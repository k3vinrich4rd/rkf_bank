// Engenharia reversa e ataques ao JWT do RKF Bank, contra a API rodando de verdade.
// Uso: node scripts/engenharia-reversa-jwt.mjs [baseUrl]      (padrão: http://localhost:8080)
//
// Faz o que um atacante faria com um token na mão, SEM conhecer o JWT_SEGREDO:
//   1. abre o token (header e payload são só Base64) e mostra o que tem dentro;
//   2. tenta forjar acesso: payload adulterado, "alg":"none", assinatura removida, chave chutada,
//      expiração esticada e clienteId de outra pessoa.
// A API tem que recusar TODOS com 401. Sai com código 1 se algum ataque for aceito.
// Cria um cliente novo a cada execução (precisa de acesso ao ViaCEP). Ver backend/README_TESTES_E2E.md, seção 7.8.

import { createHmac } from "node:crypto";

const BASE = (process.argv[2] ?? "http://localhost:8080").replace(/\/$/, "");
const resultados = [];

const b64url = (texto) => Buffer.from(texto).toString("base64url");
const abrir = (parte) => JSON.parse(Buffer.from(parte, "base64url").toString("utf8"));

function cpfValido() {
  const n = Array.from({ length: 9 }, () => Math.floor(Math.random() * 10));
  if (n.every((d) => d === n[0])) n[0] = (n[0] + 1) % 10;
  for (const tamanho of [9, 10]) {
    const soma = n.slice(0, tamanho).reduce((acc, d, i) => acc + d * (tamanho + 1 - i), 0);
    const resto = (soma * 10) % 11;
    n.push(resto === 10 ? 0 : resto);
  }
  return n.join("");
}

async function chamar(metodo, caminho, corpo, token) {
  const headers = { "Content-Type": "application/json", "X-Request-Id": "eng-reversa-" + Date.now() };
  if (token !== undefined) headers.Authorization = `Bearer ${token}`;
  const r = await fetch(BASE + caminho, { method: metodo, headers, body: corpo ? JSON.stringify(corpo) : undefined });
  const texto = await r.text();
  let json = null;
  try { json = texto ? JSON.parse(texto) : null; } catch { /* corpo não é JSON */ }
  return { status: r.status, json };
}

function verificar(nome, condicao, detalhe = "") {
  resultados.push({ nome, ok: Boolean(condicao) });
  console.log(`${condicao ? "✅" : "❌"} ${nome}${condicao || !detalhe ? "" : `  →  ${detalhe}`}`);
}

// Monta um token com header/payload escolhidos e assina com a chave que o atacante "chutou".
function forjar(header, payload, chave) {
  const corpo = `${b64url(JSON.stringify(header))}.${b64url(JSON.stringify(payload))}`;
  const assinatura = chave === null ? "" : createHmac("sha256", chave).update(corpo).digest("base64url");
  return `${corpo}.${assinatura}`;
}

try {
  console.log(`Engenharia reversa do JWT em ${BASE}\n`);

  // Vítima: um cliente comum que se cadastra e faz login.
  const cpf = cpfValido();
  const senha = "eng-reversa-" + Math.random().toString(36).slice(2, 10);
  const cadastro = await chamar("POST", "/clientes", {
    nomeCompleto: "Alvo Engenharia Reversa", cpf, email: `alvo.${Date.now()}@rkfbank.com`,
    telefone: "11999999999", cep: "01001000", numero: "1", senha,
  });
  if (cadastro.status !== 201) throw new Error(`cadastro falhou (status ${cadastro.status}); a API está no ar e com acesso ao ViaCEP?`);
  const login = await chamar("POST", "/auth/login", { cpf, senha });
  const token = login.json?.token;
  if (!token) throw new Error(`login falhou (status ${login.status})`);

  // ---------- 1. Abrir o token sem a chave ----------
  const [h, p, assinatura] = token.split(".");
  const header = abrir(h);
  const payload = abrir(p);
  console.log("1) O que qualquer um lê no token, sem saber o segredo:");
  console.log("   header :", JSON.stringify(header));
  console.log("   payload:", JSON.stringify(payload));
  console.log(`   assinatura: ${assinatura.slice(0, 12)}... (${Buffer.from(assinatura, "base64url").length} bytes = HMAC-SHA256)\n`);

  verificar("Algoritmo é HS256", header.alg === "HS256", JSON.stringify(header));
  verificar("Payload não traz CPF, e-mail nem senha", !p.includes(b64url(cpf).slice(0, 8)) && !JSON.stringify(payload).includes(cpf) && !JSON.stringify(payload).includes(senha));
  verificar("Expira em 15 min (exp - iat = 900 s)", payload.exp - payload.iat === 900, `${payload.exp - payload.iat} s`);
  verificar("Perfil CLIENTE e clienteId igual ao do cadastro", payload.perfil === "CLIENTE" && payload.clienteId === cadastro.json.id);

  // Controle: o token verdadeiro funciona e não dá acesso de admin.
  let r = await chamar("GET", "/contas", undefined, token);
  verificar("Controle: token verdadeiro → 200 em GET /contas", r.status === 200, `status ${r.status}`);
  r = await chamar("GET", "/clientes", undefined, token);
  verificar("Controle: token verdadeiro de CLIENTE → 403 em GET /clientes", r.status === 403, `status ${r.status}`);

  // ---------- 2. Ataques: todos têm que dar 401 ----------
  console.log("\n2) Tentativas de forjar acesso (a API tem que recusar todas com 401):");
  const comoAdmin = { ...payload, perfil: "ADMIN" };
  const ataques = [
    ["Payload adulterado (perfil CLIENTE → ADMIN) mantendo a assinatura original",
      `${h}.${b64url(JSON.stringify(comoAdmin))}.${assinatura}`],
    ['"alg":"none" (token sem assinatura, ataque clássico)',
      forjar({ alg: "none" }, comoAdmin, null)],
    ["Assinatura removida (header.payload.)",
      `${h}.${p}.`],
    ['Assinado com chave chutada ("secret")',
      forjar(header, comoAdmin, "secret")],
    ["Assinado com chave chutada (o valor de exemplo do .env.example)",
      forjar(header, comoAdmin, "troque-por-um-valor-aleatorio-com-pelo-menos-32-bytes")],
    ["Expiração esticada para daqui a 10 anos",
      `${h}.${b64url(JSON.stringify({ ...payload, exp: payload.exp + 10 * 365 * 24 * 3600 }))}.${assinatura}`],
  ];
  for (const [nome, falso] of ataques) {
    r = await chamar("GET", "/clientes", undefined, falso);
    verificar(`${nome} → 401`, r.status === 401, `status ${r.status} (ATAQUE ACEITO!)`);
  }

  // Trocar o clienteId pelo de outra pessoa para mexer na conta dela.
  const outroCliente = "00000000-0000-4000-8000-000000000001";
  r = await chamar("GET", "/contas", undefined, `${h}.${b64url(JSON.stringify({ ...payload, clienteId: outroCliente }))}.${assinatura}`);
  verificar("clienteId trocado pelo de outra pessoa → 401", r.status === 401, `status ${r.status} (ATAQUE ACEITO!)`);

  verificar("Recusa vem no formato padrão de erro", r.json?.status === 401 && r.json?.message && Array.isArray(r.json?.campos));

  // ---------- 3. Prova de que a forja funciona (e de que o segredo é a chave mestra) ----------
  // Com o JWT_SEGREDO verdadeiro no ambiente, o mesmo forjar() TEM que ser aceito: assim os 401 acima vieram
  // da assinatura errada, e não de um token mal montado pelo script.
  if (process.env.JWT_SEGREDO) {
    console.log("\n3) Com o JWT_SEGREDO verdadeiro (o que um atacante ganharia se o segredo vazasse):");
    const agora = Math.floor(Date.now() / 1000);
    const adminForjado = forjar(header, { iss: "rkfbank", sub: payload.sub, perfil: "ADMIN", iat: agora, exp: agora + 300 }, process.env.JWT_SEGREDO);
    r = await chamar("GET", "/clientes", undefined, adminForjado);
    verificar("Token de ADMIN forjado com o segredo real → 200 (por isso o segredo nunca vai para o Git)", r.status === 200, `status ${r.status}`);
  } else {
    console.log("\n⏭️  Prova com o segredo real pulada (carregue o .env no terminal para incluir)");
  }
} catch (erro) {
  verificar("Execução do script", false, erro.cause?.code === "ECONNREFUSED" ? `API fora do ar em ${BASE}` : erro.message);
}

const falhas = resultados.filter((x) => !x.ok).length;
console.log(`\n${resultados.length - falhas}/${resultados.length} verificações OK`);
process.exit(falhas ? 1 : 0);
