// Roda a coleção do Insomnia pelo terminal, na ordem das pastas, contra a API no ar. Serve para conferir a coleção
// depois de mudar o contrato (sem abrir o Insomnia) e para quem prefere o terminal.
//
//   node scripts/rodar-colecao-insomnia.mjs                                   → ambiente Visitante, coleção pública
//   node scripts/rodar-colecao-insomnia.mjs --ambiente equipe --arquivo .equipe/testes/rkfbank-equipe.insomnia.json
//   ... --base-url http://localhost:8081    (outra porta)    ... --pasta 03   (só as pastas que começam com "03")
//   ... --registrar saida.json   (grava cada pedido e resposta reais, com tokens cortados: base dos roteiros E2E)
//   ... --passos 01.03,02.01     (só esses passos, nessa ordem: a ordem de um roteiro; 03.05 = 5ª da pasta 03)
//
// Imita só o que a coleção usa do Insomnia: {{ _.variavel }}, {% uuid 'v4' %}, Auth Bearer e, nos scripts,
// insomnia.environment.get/set, insomnia.response.code/json()/headers.get, insomnia.test e insomnia.expect
// (to.equal, to.include, to.be.ok, to.be.an, to.be.above). Sai com código 1 se algum teste falhar.

import { existsSync, readFileSync, writeFileSync } from "node:fs";
import { dirname, isAbsolute, resolve } from "node:path";
import { fileURLToPath } from "node:url";
import { randomUUID } from "node:crypto";

const args = process.argv.slice(2);
const opcao = (nome, padrao) => (args.includes(nome) ? args[args.indexOf(nome) + 1] : padrao);
// Caminho relativo vale a partir de onde você rodou; se não existir aí, vale a partir da raiz do projeto (assim roda de qualquer pasta, IntelliJ incluso).
const RAIZ = resolve(dirname(fileURLToPath(import.meta.url)), "..");
const noProjeto = (caminho) => (isAbsolute(caminho) || existsSync(caminho) ? caminho : resolve(RAIZ, caminho));
const ARQUIVO = noProjeto(opcao("--arquivo", "backend/insomnia/rkfbank.insomnia.json"));
const AMBIENTE = opcao("--ambiente", "visitante");
const BASE_URL = opcao("--base-url", null);
const PASTA = opcao("--pasta", null);

const recursos = JSON.parse(readFileSync(ARQUIVO, "utf8")).resources;
const ambienteBase = recursos.find((r) => r._id === "env_rkfbank_base");
const subAmbiente = recursos.find((r) => r._id === `env_rkfbank_${AMBIENTE}`);
if (!subAmbiente) throw new Error(`ambiente "${AMBIENTE}" não existe na coleção (use visitante ou equipe)`);
const env = { ...ambienteBase.data, ...subAmbiente.data, ...(BASE_URL ? { base_url: BASE_URL } : {}) };

const renderizar = (texto) =>
  String(texto ?? "")
    .replace(/\{%\s*uuid\s+'v4'\s*%\}/g, () => randomUUID())
    .replace(/\{\{\s*_\.(\w+)\s*\}\}/g, (_, nome) => env[nome] ?? "");

// O pedaço do chai que os scripts da coleção usam (o Insomnia de verdade traz o chai inteiro).
function expect(atual) {
  const falha = (msg) => { throw new Error(msg); };
  const naoIgual = (esperado) => atual !== esperado || falha(`esperado diferente de ${JSON.stringify(esperado)}`);
  return {
    to: {
      equal: (esperado) => atual === esperado || falha(`esperado ${JSON.stringify(esperado)}, veio ${JSON.stringify(atual)}`),
      include: (trecho) => String(atual).includes(trecho) || falha(`"${atual}" não contém "${trecho}"`),
      match: (regex) => regex.test(String(atual)) || falha(`"${atual}" não casa com ${regex}`),
      not: { equal: naoIgual },
      have: { length: (n) => atual?.length === n || falha(`esperado tamanho ${n}, veio ${atual?.length}`) },
      be: {
        get ok() { return atual || falha(`esperado valor, veio ${JSON.stringify(atual)}`); },
        an: (tipo) => (tipo === "array" ? Array.isArray(atual) : typeof atual === tipo) || falha(`não é ${tipo}`),
        above: (n) => atual > n || falha(`${atual} não é maior que ${n}`),
      },
    },
  };
}

function rodarScript(codigo, resposta, testes) {
  if (!codigo) return;
  const insomnia = {
    environment: { get: (k) => env[k], set: (k, v) => { env[k] = v; } },
    response: resposta && {
      code: resposta.status,
      json: () => JSON.parse(resposta.corpo),
      headers: { get: (nome) => resposta.headers.get(nome) },
    },
    expect,
    test: (nome, fn) => {
      try { fn(); testes.push({ nome, ok: true }); } catch (e) { testes.push({ nome, ok: false, erro: e.message }); }
    },
  };
  new Function("insomnia", "console", codigo)(insomnia, { log: () => {} });
}

const pastas = recursos.filter((r) => r._type === "request_group").sort((a, b) => a.metaSortKey - b.metaSortKey);
let falhas = 0;
const semAdmin = !env.admin_cpf || env.admin_cpf.startsWith("COLE_AQUI");
let total = 0;
// --registrar saida.json: grava cada pedido e resposta (base dos roteiros E2E). Tokens saem cortados, nunca inteiros.
const REGISTRAR = opcao("--registrar", null);
const registro = [];
const cortarTokens = (texto) =>
  String(texto ?? "")
    .replace(/eyJ[\w-]+\.[\w-]+\.[\w-]+/g, "eyJhbGciOiJIUzI1NiJ9...")
    .replace(/("refreshToken"\s*:\s*")[^"]{12,}"/g, '$1q3Jd0kX4..."');

// Cada requisição ganha um id "pasta.posição" (03.05 = 5ª da pasta 03), o mesmo dos roteiros gerados.
const doCatalogo = pastas.flatMap((pasta) =>
  recursos.filter((r) => r._type === "request" && r.parentId === pasta._id)
    .sort((a, b) => a.metaSortKey - b.metaSortKey)
    .map((r, i) => ({ pasta, r, id: `${pasta.name.slice(0, 2)}.${String(i + 1).padStart(2, "0")}` })));
// --passos 01.03,02.01,...: só esses, nesta ordem (a ordem de um roteiro), para o registro mostrar o que quem segue
// o roteiro vai ver de verdade (saldos e ids dependem do que veio antes).
const PASSOS = opcao("--passos", null)?.split(",");
const fila = PASSOS
  ? PASSOS.map((id) => doCatalogo.find((p) => p.id === id) ?? (() => { throw new Error(`passo ${id} não existe na coleção`); })())
  : doCatalogo;

let pastaAtual = null;
for (const { pasta, r, id } of fila) {
  if (PASTA && !pasta.name.startsWith(PASTA)) continue;
  if (pasta !== pastaAtual) { console.log(`\n📁 ${pasta.name}`); pastaAtual = pasta; }
  // Sem ADMIN no ambiente (o perfil visitante não tem, de propósito): a gestão de usuários não tem como rodar.
  if (semAdmin && pasta.name.includes("ADMIN")) { console.log(`  ⏭️  ${r.name}: este ambiente não tem ADMIN`); continue; }
  {
    if (semAdmin && /admin_cpf|token_admin/.test(JSON.stringify(r))) { console.log(`  ⏭️  ${r.name}: este ambiente não tem ADMIN`); continue; }
    const testes = [];
    rodarScript(r.preRequestScript, null, testes);
    const headers = Object.fromEntries(r.headers.map((h) => [h.name, renderizar(h.value)]));
    if (r.authentication?.type === "bearer") headers.Authorization = `Bearer ${renderizar(r.authentication.token)}`;
    const url = renderizar(r.url);
    // Renderiza o corpo uma vez só: o script de depois pode mudar as variáveis (ex.: a senha trocada).
    const corpo = r.body?.text ? renderizar(r.body.text) : undefined;
    let resposta;
    try {
      const http = await fetch(url, { method: r.method, headers, body: corpo });
      resposta = { status: http.status, headers: http.headers, corpo: await http.text() };
    } catch (e) {
      console.log(`  ❌ ${r.name}: API fora do ar em ${url} (${e.cause?.code ?? e.message})`);
      process.exit(1);
    }
    try { rodarScript(r.afterResponseScript, resposta, testes); }
    catch (e) { testes.push({ nome: "script", ok: false, erro: e.message }); }
    const ruins = testes.filter((t) => !t.ok);
    total += 1;
    falhas += ruins.length ? 1 : 0;
    console.log(`  ${ruins.length ? "❌" : "✅"} ${resposta.status} ${r.name}`);
    if (REGISTRAR) {
      registro.push({
        id, ambiente: AMBIENTE, pasta: pasta.name, nome: r.name, metodo: r.method, caminho: url.replace(env.base_url, ""),
        headers: Object.keys(headers), corpo: cortarTokens(corpo ?? ""),
        status: resposta.status, repetida: resposta.headers.get("Idempotency-Replayed"), resposta: cortarTokens(resposta.corpo),
      });
    }
    for (const t of ruins) console.log(`       ↳ ${t.nome}: ${t.erro}${resposta.corpo ? ` · corpo: ${resposta.corpo.slice(0, 160)}` : ""}`);
  }
}

if (REGISTRAR) writeFileSync(REGISTRAR, JSON.stringify(registro, null, 2));
console.log(`\n${falhas ? "❌" : "✅"} ${total - falhas}/${total} requisições como o esperado (ambiente ${AMBIENTE})`);
process.exit(falhas ? 1 : 0);
