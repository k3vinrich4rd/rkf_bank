#!/usr/bin/env node
// Converte o dump de DADOS do MySQL do RKF Bank em SQL que o PostgreSQL entende (migracao MySQL -> PostgreSQL).
// Guia completo: planing/guia-corte-postgresql.md. Nao conecta em banco nenhum: le um arquivo e escreve na tela.
//
// Uso:
//   mysqldump --no-create-info --complete-insert --skip-extended-insert --hex-blob --skip-add-locks --skip-disable-keys \
//     --no-tablespaces --skip-triggers --compact -h <HOST> -P <PORTA> -u <USUARIO> -p --ssl-mode=REQUIRED <BANCO> \
//     tb_endereco tb_cliente tb_usuario tb_conta tb_lancamento > dados-mysql.sql
//   node scripts/converter-dump-mysql-para-postgres.mjs dados-mysql.sql > dados-postgres.sql
//
// O que converte: crases dos nomes, 0x<hex> (BINARY(16)) em UUID, b'1' e 0x01/0x00 (BIT) em true/false, escapes de texto do MySQL
// (barra + aspas, barra + n...) em texto padrao, e DATETIME (que o prod grava em UTC) em timestamptz (+00).
// Recusa (e sai com erro) o que nao conhece: melhor parar do que importar torto.
import { readFileSync } from 'node:fs';

const arquivo = process.argv[2];
if (!arquivo) {
  console.error('Uso: node scripts/converter-dump-mysql-para-postgres.mjs <dump-de-dados.sql>');
  process.exit(2);
}

const BARRA = String.fromCharCode(92);
// Ordem das chaves estrangeiras. Ficam de fora, de proposito: tb_idempotencia, tb_refresh_token, tb_tentativa_login e
// tb_shedlock (descartaveis: o pessoal so precisa entrar de novo) e flyway_schema_history (o PostgreSQL cria o seu).
const TABELAS = ['tb_endereco', 'tb_cliente', 'tb_usuario', 'tb_conta', 'tb_lancamento'];
const ESCAPES = { 0: '\0', b: '\b', n: '\n', r: '\r', t: '\t', Z: '\x1a', "'": "'", '"': '"', [BARRA]: BARRA };

function converterValores(texto, linha) {
  const saida = [];
  let i = 0;
  while (i < texto.length) {
    const c = texto[i];
    if (c === ',' || c === ' ') { i++; continue; }
    if (c === "'") {
      let valor = '';
      i++;
      while (i < texto.length && texto[i] !== "'") {
        if (texto[i] === BARRA) {
          const proximo = texto[i + 1];
          valor += proximo in ESCAPES ? ESCAPES[proximo] : BARRA + proximo; // \% e \_ ficam como estao
          i += 2;
        } else valor += texto[i++];
      }
      i++;
      if (/^\d{4}-\d{2}-\d{2} \d{2}:\d{2}:\d{2}(\.\d+)?$/.test(valor)) valor += '+00';
      saida.push("'" + valor.replaceAll("'", "''") + "'");
      continue;
    }
    const resto = texto.slice(i);
    let m;
    if ((m = /^0x([0-9a-fA-F]{32})/.exec(resto))) {
      const h = m[1].toLowerCase();
      saida.push(`'${h.slice(0, 8)}-${h.slice(8, 12)}-${h.slice(12, 16)}-${h.slice(16, 20)}-${h.slice(20)}'`);
      i += m[0].length;
    } else if ((m = /^0x0([01])(?![0-9a-fA-F])/.exec(resto))) {
      saida.push(m[1] === '1' ? 'true' : 'false'); // BIT(1) sai como 0x01 / 0x00 com --hex-blob
      i += m[0].length;
    } else if ((m = /^b'([01])'/.exec(resto))) {
      saida.push(m[1] === '1' ? 'true' : 'false');
      i += m[0].length;
    } else if (resto.startsWith('NULL')) {
      saida.push('NULL');
      i += 4;
    } else if ((m = /^-?\d+(\.\d+)?([eE][-+]?\d+)?/.exec(resto))) {
      saida.push(m[0]);
      i += m[0].length;
    } else {
      console.error(`Valor desconhecido na linha ${linha}: ${resto.slice(0, 40)}`);
      process.exit(1);
    }
  }
  return saida.join(', ');
}

const porTabela = Object.fromEntries(TABELAS.map((t) => [t, []]));
readFileSync(arquivo, 'utf8').split(/\r?\n/).forEach((linha, idx) => {
  if (!linha.startsWith('INSERT INTO')) return;
  const m = /^INSERT INTO `(\w+)` \(([^)]*)\) VALUES \((.*)\);$/.exec(linha);
  if (!m) {
    console.error(`INSERT que nao reconheco na linha ${idx + 1}. Gere o dump com --skip-extended-insert --complete-insert.`);
    process.exit(1);
  }
  const [, tabela, colunas, valores] = m;
  if (!(tabela in porTabela)) {
    console.error(`Tabela fora da lista de migracao: ${tabela}`);
    process.exit(1);
  }
  porTabela[tabela].push(`INSERT INTO ${tabela} (${colunas.replaceAll('`', '')}) VALUES (${converterValores(valores, idx + 1)});`);
});

console.log('-- Gerado por scripts/converter-dump-mysql-para-postgres.mjs. Rode com: psql -v ON_ERROR_STOP=1 -f <este arquivo>');
console.log('BEGIN;');
for (const t of TABELAS) {
  console.log(`-- ${t}: ${porTabela[t].length} linha(s)`);
  porTabela[t].forEach((l) => console.log(l));
}
console.log('COMMIT;');
