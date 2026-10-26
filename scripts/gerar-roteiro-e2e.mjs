// Gera o passo a passo dos roteiros E2E a partir da coleção do Insomnia e de uma execução real dela.
//
//   1. API no ar + node scripts/rodar-colecao-insomnia.mjs --registrar registro.json   (grava pedidos e respostas)
//   2. node scripts/gerar-roteiro-e2e.mjs --registro registro.json --saida backend/README_TESTES_E2E.md
//        [--colecao .equipe/testes/rkfbank-equipe.insomnia.json] [--env .env]
//
// O texto entre <!-- roteiro-gerado:inicio --> e <!-- roteiro-gerado:fim --> na saída é substituído; o resto do
// arquivo (preparação, conceitos, problemas comuns) é escrito à mão e fica como está. Assim o roteiro e a coleção
// nunca divergem: cada passo é uma requisição da coleção, com o pedido e a resposta que a API deu de verdade.
//
// --env: valores de EQUIPE_SENHA, ADMIN_CPF e ADMIN_SENHA viram marcadores (<EQUIPE_SENHA do .env>): nenhuma senha
// fica escrita no roteiro, nem no privado. Tokens já chegam cortados do executor.

import { existsSync, readFileSync, writeFileSync } from "node:fs";
import { dirname, join } from "node:path";
import { fileURLToPath } from "node:url";

const RAIZ = join(dirname(fileURLToPath(import.meta.url)), "..");
const args = process.argv.slice(2);
const opcao = (nome, padrao) => (args.includes(nome) ? args[args.indexOf(nome) + 1] : padrao);

const REGISTRO = opcao("--registro", null);
const SAIDA = opcao("--saida", null);
const COLECAO = opcao("--colecao", join(RAIZ, "backend", "insomnia", "rkfbank.insomnia.json"));
const ENV = opcao("--env", null);
if (!REGISTRO || !SAIDA) {
  console.error("uso: node scripts/gerar-roteiro-e2e.mjs --registro registro.json --saida arquivo.md [--colecao c.json] [--env .env]");
  process.exit(1);
}

// --bloco admin --pastas 01,05: preenche só <!-- roteiro-gerado:admin:inicio --> ... :fim --> com essas pastas.
// Um roteiro pode ter vários blocos (ex.: o ADMIN primeiro e o cliente depois); sem --bloco, o bloco único de sempre.
const BLOCO = opcao("--bloco", null);
const SO_PASTAS = opcao("--pastas", null)?.split(",");
// --passos 01.01,03.03,...: modo "trilha" — só esses passos (pasta.posição na execução), na ordem dada, em cartões
// numerados e curtos (onde clicar, o que mandar, o que volta e uma caixinha). É o formato dos guias de primeira vez.
const SO_PASSOS = opcao("--passos", null)?.split(",");
const INICIO = BLOCO ? `<!-- roteiro-gerado:${BLOCO}:inicio -->` : "<!-- roteiro-gerado:inicio -->";
const FIM = BLOCO ? `<!-- roteiro-gerado:${BLOCO}:fim -->` : "<!-- roteiro-gerado:fim -->";

// ── Segredos que nunca entram no roteiro ──────────────────────────────────────────────────────────────────────

function segredos() {
  if (!ENV) return [];
  const valores = {};
  for (const linha of readFileSync(ENV, "utf8").split(/\r?\n/)) {
    const m = linha.match(/^\s*([A-Z_]+)\s*=\s*(.*)\s*$/);
    if (m) valores[m[1]] = m[2].replace(/^["']|["']$/g, "");
  }
  const lista = ["EQUIPE_SENHA", "ADMIN_SENHA", "ADMIN_CPF"]
    .filter((nome) => valores[nome])
    .map((nome) => ({ valor: valores[nome], marcador: `<${nome} do .env>` }));
  // O CPF mascarado (***.456.789-**) ainda mostra 6 dígitos: o do ADMIN também sai do roteiro.
  const cpf = (valores.ADMIN_CPF ?? "").replace(/\D/g, "");
  if (cpf.length === 11) lista.push({ valor: `${cpf.slice(3, 6)}.${cpf.slice(6, 9)}`, marcador: "xxx.xxx" });
  return lista;
}
const SEGREDOS = segredos();
const limpar = (texto) => SEGREDOS.reduce((t, s) => t.split(s.valor).join(s.marcador), String(texto ?? ""));

// ── Coleção: descrição, variáveis usadas e guardadas, testes de cada requisição ──────────────────────────────

const recursos = JSON.parse(readFileSync(COLECAO, "utf8")).resources;
const pastas = recursos.filter((r) => r._type === "request_group").sort((a, b) => a.metaSortKey - b.metaSortKey);
const requisicoes = recursos.filter((r) => r._type === "request");

const variaveisUsadas = (r) =>
  [...new Set([...JSON.stringify([r.url, r.body, r.headers, r.authentication]).matchAll(/\{\{\s*_\.(\w+)\s*\}\}/g)].map((m) => m[1]))]
    .filter((v) => v !== "base_url");
const variaveisGuardadas = (r) =>
  [...new Set([...(r.afterResponseScript ?? "").matchAll(/environment\.set\("(\w+)"/g)].map((m) => m[1]))];
const variaveisSorteadas = (r) =>
  [...new Set([...(r.preRequestScript ?? "").matchAll(/environment\.set\("(\w+)"/g)].map((m) => m[1]))];
const testes = (r) => [...(r.afterResponseScript ?? "").matchAll(/insomnia\.test\("([^"]+)"/g)].map((m) => m[1]);

// ── Registro: o que a API respondeu de verdade ───────────────────────────────────────────────────────────────

const registro = JSON.parse(readFileSync(REGISTRO, "utf8")).filter((item) => !SO_PASTAS || SO_PASTAS.includes(item.pasta.slice(0, 2)));
const ambiente = opcao("--ambiente", REGISTRO.includes("equipe") ? "equipe" : "visitante");

function formatarJson(texto) {
  if (!texto) return null;
  let valor;
  try {
    valor = JSON.parse(texto);
  } catch {
    return limpar(texto);
  }
  // Lista grande vira as duas primeiras + aviso: o formato importa, não o tamanho do banco na hora do teste.
  const encurtar = (v) => {
    if (Array.isArray(v)) {
      const resto = v.length - 2;
      const itens = v.slice(0, 2).map(encurtar);
      return resto > 0 ? [...itens, `… mais ${resto} ${resto === 1 ? "item" : "itens"} no mesmo formato`] : itens;
    }
    if (v && typeof v === "object") return Object.fromEntries(Object.entries(v).map(([k, x]) => [k, encurtar(x)]));
    return v;
  };
  return limpar(JSON.stringify(encurtar(valor), null, 2));
}

function blocoPedido(item, req) {
  const linhas = [`${item.metodo} ${limpar(item.caminho)}`];
  const auth = req?.authentication?.type === "bearer" ? req.authentication.token.match(/_\.(\w+)/)?.[1] : null;
  if (auth) linhas.push(`Authorization: Bearer {{ ${auth} }}`);
  for (const h of req?.headers ?? []) {
    if (h.name === "Idempotency-Key") {
      linhas.push(`Idempotency-Key: ${h.value.includes("uuid") ? "<UUID novo a cada envio>" : "{{ " + (h.value.match(/_\.(\w+)/)?.[1] ?? h.value) + " }}"}`);
    } else {
      linhas.push(`${h.name}: ${h.value}`);
    }
  }
  const corpo = item.corpo ? formatarJson(item.corpo) : null;
  return ["```http", ...linhas, ...(corpo ? ["", corpo] : []), "```"].join("\n");
}

function blocoResposta(item) {
  const json = formatarJson(item.resposta);
  const extras = item.repetida ? ` · header \`Idempotency-Replayed: ${item.repetida}\`` : "";
  const cabeca = `✅ **${item.status}**${json ? "" : " · sem corpo"}${extras}`;
  return json ? `${cabeca}\n\n\`\`\`json\n${json}\n\`\`\`` : cabeca;
}

// O que cada pasta prova, em uma linha (a descrição da pasta na coleção é o texto longo).
const RESUMOS = {
  "00": "a API está no ar e qual versão dos termos o cadastro aceita",
  "01": "login dos clientes ORIGEM e DESTINO (e, na coleção da equipe, do ADMIN); tokens de acesso e de renovação guardados",
  "02": "cliente novo do zero: cadastro com aceite e ViaCEP, contas ORIGEM e DESTINO, depósito, saque, transferência, saldo e extrato",
  "03": "dinheiro entre dois clientes diferentes, com as contas prontas do seed",
  "04": "a mesma movimentação repetida não sai duas vezes (`Idempotency-Key`)",
  "05": "ADMIN: listar, promover, rebaixar, desativar, redefinir senha (temporária), conciliação e as travas",
  "06": "direitos do titular (LGPD): ver, corrigir e encerrar o cadastro; troca de senha",
  "07": "as proteções: 401, 403, 404, 409, 422 e 400, cada uma com o `codigo` do contrato",
  "08": "CORS para um navegador que chama a API direto",
  "09": "sessão: renovar sem senha, reuso derrubando tudo, logout",
};

function gerar() {
  const saida = [];
  const porPasta = new Map();
  for (const item of registro) {
    if (!porPasta.has(item.pasta)) porPasta.set(item.pasta, []);
    porPasta.get(item.pasta).push(item);
  }

  saida.push(
    `> 🤖 Gerado por \`scripts/gerar-roteiro-e2e.mjs\` a partir da coleção do Insomnia e de uma execução real dela ` +
      `(ambiente **${ambiente}**, ${registro.length} requisições, todas com o resultado esperado). Ids, números de conta, ` +
      "datas e CPFs sorteados mudam a cada execução; status, campos e `codigo` não. Tokens aparecem cortados.\n",
    "| Pasta da coleção | Passos | O que prova |",
    "|---|:-:|---|",
  );
  for (const pasta of pastas) {
    const itens = porPasta.get(pasta.name);
    if (!itens) continue;
    const prefixo = pasta.name.slice(0, 2);
    // Mesmo slug do GitHub: minúsculas, acentos ficam, pontuação sai e cada espaço vira um hífen ("00 · Comece" → "00--comece").
    const ancora = pasta.name.toLowerCase().replace(/[^\p{L}\p{N} _-]/gu, "").replace(/ /g, "-");
    const resumo = RESUMOS[prefixo] ?? (pasta.description ?? "").split(/(?<=\.)\s/)[0];
    saida.push(`| [${pasta.name}](#${ancora}) | ${prefixo}.01–${prefixo}.${String(itens.length).padStart(2, "0")} | ${limpar(resumo)} |`);
  }
  saida.push("");

  for (const pasta of pastas) {
    const itens = porPasta.get(pasta.name);
    if (!itens) continue;
    const prefixo = pasta.name.slice(0, 2);
    saida.push(`### ${pasta.name}`, "", limpar(pasta.description ?? ""), "");
    const daPasta = requisicoes.filter((r) => r.parentId === pasta._id);
    itens.forEach((item, i) => {
      const req = daPasta.find((r) => r.name === item.nome);
      const id = `${prefixo}.${String(i + 1).padStart(2, "0")}`;
      saida.push(`#### ${id} · ${item.nome}`, "");
      if (req?.description) saida.push(limpar(req.description), "");
      const usa = variaveisUsadas(req ?? {});
      const sorteia = variaveisSorteadas(req ?? {});
      const guarda = variaveisGuardadas(req ?? {});
      const meta = [];
      if (sorteia.length) meta.push(`🎲 sorteia antes: ${sorteia.map((v) => `\`${v}\``).join(", ")}`);
      if (usa.length) meta.push(`📥 usa: ${usa.map((v) => `\`${v}\``).join(", ")}`);
      if (guarda.length) meta.push(`📤 guarda: ${guarda.map((v) => `\`${v}\``).join(", ")}`);
      if (meta.length) saida.push(meta.join(" · "), "");
      saida.push("**Pedido**", "", blocoPedido(item, req), "", "**Resposta esperada**", "", blocoResposta(item), "");
      const confere = testes(req ?? {});
      if (confere.length) saida.push(`🔎 A coleção confere: ${confere.map((t) => `*${t}*`).join(" · ")}`, "");
    });
  }
  return saida.join("\n");
}

const atual = existsSync(SAIDA) ? readFileSync(SAIDA, "utf8") : "";
const crlf = atual.includes("\r\n");
const texto = atual.replace(/\r\n/g, "\n");
const ini = texto.indexOf(INICIO);
const fim = texto.indexOf(FIM);
if (ini < 0 || fim < ini) {
  console.error(`${SAIDA} precisa ter os marcadores ${INICIO} e ${FIM} onde o passo a passo entra`);
  process.exit(1);
}
// Modo trilha: cada passo vira um cartão curto. Só o 💡 da descrição entra; o texto técnico fica no roteiro completo.
function gerarTrilha() {
  const porId = new Map();
  const contagem = new Map();
  for (const item of registro) {
    const prefixo = item.pasta.slice(0, 2);
    const n = (contagem.get(prefixo) ?? 0) + 1;
    contagem.set(prefixo, n);
    porId.set(`${prefixo}.${String(n).padStart(2, "0")}`, item);
  }
  const passos = SO_PASSOS.map((id) => {
    const item = porId.get(id);
    if (!item) throw new Error(`passo ${id} não existe nesta execução`);
    const pasta = pastas.find((p) => p.name === item.pasta);
    return { item, req: requisicoes.find((r) => r.parentId === pasta?._id && r.name === item.nome) };
  });
  // Mapa da trilha: a ordem dos métodos HTTP, com a rota em forma de molde ({conta_origem_id}) e quem está logado.
  const rota = (item, req) =>
    (req?.url ?? item.caminho).replace(/\{\{\s*_\.base_url\s*\}\}/, "").replace(/\{\{\s*_\.(\w+)\s*\}\}/g, "{$1}");
  const quem = (req) => req?.authentication?.type === "bearer" ? `\`${req.authentication.token.match(/_\.(\w+)/)?.[1]}\`` : "— (público)";
  const saida = [
    `> 🤖 Pedidos e respostas reais (ambiente **${ambiente}**), gerados por \`scripts/gerar-roteiro-e2e.mjs\`. ` +
      "Ids, números de conta e datas mudam a cada execução; o **status** e o `codigo` não.\n",
    "**A ordem, de uma olhada** (o detalhe de cada passo vem logo abaixo):\n",
    "| Passo | Método | Rota | Crachá (token) | O que faz | Tem que voltar |",
    "|:-:|:-:|---|---|---|:-:|",
    ...passos.map(({ item, req }, i) =>
      `| ${i + 1} | \`${item.metodo}\` | \`${rota(item, req)}\` | ${quem(req)} | ${item.nome.replace(/\|/g, "\\|")} | **${item.status}** |`),
    "",
  ];
  passos.forEach(({ item, req }, i) => {
    const simples = (req?.description ?? "").split(/\n\s*\n/)[0].replace(/^💡 \*\*Em palavras simples:\*\*\s*/, "");
    saida.push(
      `### Passo ${i + 1} · ${item.nome}`, "",
      `📂 Insomnia: **${item.pasta}** → **${item.nome}** → **Send**`, "",
      ...(simples ? [`💡 ${limpar(simples)}`, ""] : []),
      "**Você manda**", "", blocoPedido(item, req), "",
      "**Tem que voltar**", "", blocoResposta(item), "",
      "- [ ] voltou igual", "",
    );
  });
  return saida.join("\n");
}

const novo = texto.slice(0, ini + INICIO.length) + "\n" + (SO_PASSOS ? gerarTrilha() : gerar()) + "\n" + texto.slice(fim);
writeFileSync(SAIDA, crlf ? novo.replace(/\n/g, "\r\n") : novo);
console.log(`✅ ${registro.length} passos → ${SAIDA}`);
