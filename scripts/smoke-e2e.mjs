// Smoke test ponta a ponta contra a API rodando de verdade (MySQL ou H2).
// Uso: node scripts/smoke-e2e.mjs [baseUrl]      (padrão: http://localhost:8080)
// Cria dados novos a cada execução (CPF, e-mail e número de conta aleatórios), então pode rodar
// várias vezes no mesmo banco. Segue o roteiro de backend/README_TESTES_E2E.md em versão resumida.
// Login: cada cliente se cadastra com senha e entra em POST /auth/login; as rotas de conta vão com o token.
// Contas do seed (só leitura): visitantes Ana e Bruno no perfil visitante; Felipe e Renan com EQUIPE_SENHA no ambiente.
// Admin (opcional): com ADMIN_CPF e ADMIN_SENHA no ambiente (os mesmos da API), também confere GET /clientes.
// Sai com código 0 se todas as verificações passaram e 1 se alguma falhou.

const BASE = (process.argv[2] ?? "http://localhost:8080").replace(/\/$/, "");
const SENHA = "smoke-senha-" + Math.random().toString(36).slice(2, 10);
const resultados = [];

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

const aleatorio = (digitos) => String(Math.floor(Math.random() * 10 ** digitos)).padStart(digitos, "0");

const MOVIMENTACAO = /\/(deposito|saque|transferencia)$/;

// Movimentações levam Idempotency-Key (contrato 2.1): uma chave nova por chamada, salvo quando o teste passa
// a própria em `chave` (para repetir de propósito) ou `chave: null` (para mandar sem).
async function chamar(metodo, caminho, corpo, token, { chave } = {}) {
  const headers = { "Content-Type": "application/json", "X-Request-Id": "smoke-e2e-" + aleatorio(6) };
  if (token) headers.Authorization = `Bearer ${token}`;
  if (metodo === "POST" && MOVIMENTACAO.test(caminho) && chave !== null) headers["Idempotency-Key"] = chave ?? crypto.randomUUID();
  const resposta = await fetch(BASE + caminho, {
    method: metodo,
    headers,
    body: corpo === undefined ? undefined : typeof corpo === "string" ? corpo : JSON.stringify(corpo),
  });
  const texto = await resposta.text();
  let json = null;
  try { json = texto ? JSON.parse(texto) : null; } catch { /* corpo não é JSON */ }
  return { status: resposta.status, json, headers: resposta.headers };
}

function verificar(nome, condicao, detalhe = "") {
  resultados.push({ nome, ok: Boolean(condicao) });
  console.log(`${condicao ? "✅" : "❌"} ${nome}${condicao || !detalhe ? "" : `  →  ${detalhe}`}`);
  return Boolean(condicao);
}

async function login(cpf, senha) {
  const r = await chamar("POST", "/auth/login", { cpf, senha });
  return r;
}

// Cadastra e já faz login: devolve { ...cliente, cpf, token }.
async function novoCliente(nome) {
  const cpf = cpfValido();
  const r = await chamar("POST", "/clientes", {
    nomeCompleto: nome, cpf, email: `smoke.${aleatorio(8)}@rkfbank.com`,
    telefone: "11999999999", cep: "01001000", numero: "123", senha: SENHA,
  });
  verificar(`Cadastrar ${nome} → 201 com endereço do ViaCEP`, r.status === 201 && r.json?.endereco?.logradouro, `status ${r.status} ${JSON.stringify(r.json)}`);
  verificar(`Cadastro de ${nome} não devolve senha`, r.json && !("senha" in r.json) && !JSON.stringify(r.json).includes(SENHA));
  const l = await login(cpf, SENHA);
  verificar(`Login de ${nome} → 200 com token Bearer e clienteId`, l.status === 200 && l.json?.token && l.json?.tipo === "Bearer" && l.json?.clienteId === r.json?.id, `status ${l.status}`);
  return { ...r.json, cpf, token: l.json?.token };
}

async function novaConta(cliente) {
  // Sem agência nem número: a API gera (contrato 2.1).
  const r = await chamar("POST", `/contas/clientes/${cliente.id}`, { tipoContaEnum: "CORRENTE" }, cliente.token);
  verificar("Abrir conta sem número → 201, número gerado (8 dígitos), saldo 0.00 e ativa",
      r.status === 201 && /^\d{8}$/.test(r.json?.numeroConta ?? "") && r.json?.agencia === "0001"
      && Number(r.json?.saldo) === 0 && r.json?.ativa === true, `status ${r.status} ${JSON.stringify(r.json)}`);
  return r.json;
}

// GET /contas devolve só as contas do dono do token.
async function saldoDe(contaId, token) {
  const r = await chamar("GET", "/contas", undefined, token);
  return Number((r.json ?? []).find((c) => c.id === contaId)?.saldo);
}

try {
  console.log(`Smoke test E2E em ${BASE}\n`);
  const origem = await novoCliente("Smoke Origem");
  const destino = await novoCliente("Smoke Destino");
  if (!origem?.token || !destino?.token) throw new Error("cadastro ou login falhou; a API está no ar, com JWT_SEGREDO e acesso ao ViaCEP?");
  const contaOrigem = await novaConta(origem);
  const contaDestino = await novaConta(destino);

  let r = await chamar("POST", `/contas/${contaOrigem.id}/deposito`, { valor: 150.0, descricao: "smoke deposito" }, origem.token);
  verificar("Depósito de 150.00 → 204", r.status === 204, `status ${r.status}`);
  r = await chamar("POST", `/contas/${contaOrigem.id}/saque`, { valor: 20.0, descricao: "smoke saque" }, origem.token);
  verificar("Saque de 20.00 → 204", r.status === 204, `status ${r.status}`);
  r = await chamar("POST", "/contas/transferencia", { contaOrigemId: contaOrigem.id, contaDestinoId: contaDestino.id, valor: 50.0, descricao: "smoke transferencia" }, origem.token);
  verificar("Transferência de 50.00 → 204", r.status === 204, `status ${r.status}`);

  verificar("Saldo da origem = 80.00", (await saldoDe(contaOrigem.id, origem.token)) === 80);
  verificar("Saldo do destino = 50.00", (await saldoDe(contaDestino.id, destino.token)) === 50);

  r = await chamar("GET", `/contas/${contaOrigem.id}/lancamentos`, undefined, origem.token);
  const tipos = (r.json ?? []).map((l) => l.tipoLancamento);
  verificar("Extrato da origem: 3 lançamentos, do mais novo ao mais antigo", JSON.stringify(tipos) === JSON.stringify(["TRANSFERENCIA", "SAQUE", "DEPOSITO"]), JSON.stringify(tipos));
  const [transf, saque, deposito] = r.json ?? [];
  verificar("Transferência com origem e destino preenchidos", transf?.contaOrigemId === contaOrigem.id && transf?.contaDestinoId === contaDestino.id);
  verificar("Saque com destino nulo e depósito com origem nula", saque?.contaDestinoId === null && deposito?.contaOrigemId === null);

  console.log("\nSegurança");
  r = await chamar("GET", "/contas");
  verificar("Sem token → 401 no formato padrão", r.status === 401 && r.json?.status === 401 && r.json?.path === "/contas", `status ${r.status}`);
  r = await chamar("GET", "/contas", undefined, "token.falso.qualquer");
  verificar("Token inválido → 401", r.status === 401, `status ${r.status}`);
  r = await login(origem.cpf, "senha-errada");
  verificar("Senha errada → 401 'CPF ou senha invalidos'", r.status === 401 && r.json?.message === "CPF ou senha invalidos", `status ${r.status}`);
  r = await chamar("POST", `/contas/${contaOrigem.id}/saque`, { valor: 1.0, descricao: "smoke" }, destino.token);
  verificar("Saque na conta de outro cliente → 403", r.status === 403 && r.json?.status === 403, `status ${r.status}`);
  r = await chamar("GET", `/contas/${contaOrigem.id}/lancamentos`, undefined, destino.token);
  verificar("Extrato da conta de outro cliente → 403", r.status === 403, `status ${r.status}`);
  r = await chamar("GET", "/contas", undefined, destino.token);
  verificar("GET /contas mostra só as contas do dono do token", Array.isArray(r.json) && r.json.length === 1 && r.json[0].id === contaDestino.id, JSON.stringify(r.json));
  r = await chamar("GET", "/clientes", undefined, origem.token);
  verificar("GET /clientes com perfil CLIENTE → 403", r.status === 403, `status ${r.status}`);
  // O perfil visitante nao tem admin de proposito: la, ADMIN_CPF do ambiente nao vale.
  const perfilVisitante = (await login("24681357928", "Visitante@2026")).status === 200;
  if (perfilVisitante) {
    console.log("⏭️  GET /clientes com ADMIN pulado (perfil visitante nao tem admin)");
  } else if (process.env.ADMIN_CPF && process.env.ADMIN_SENHA) {
    const admin = await login(process.env.ADMIN_CPF, process.env.ADMIN_SENHA);
    r = await chamar("GET", "/clientes?paginado=true", undefined, admin.json?.token);
    verificar("GET /clientes com perfil ADMIN → 200", admin.json?.perfil === "ADMIN" && r.status === 200, `login ${admin.status}, listagem ${r.status}`);
  } else {
    console.log("⏭️  GET /clientes com ADMIN pulado (defina ADMIN_CPF e ADMIN_SENHA para conferir)");
  }

  console.log("\nIdempotência (Idempotency-Key)");
  const chave = crypto.randomUUID();
  const antes = await saldoDe(contaOrigem.id, origem.token);
  r = await chamar("POST", `/contas/${contaOrigem.id}/saque`, { valor: 1.0, descricao: "smoke idempotente" }, origem.token, { chave });
  const repetida = await chamar("POST", `/contas/${contaOrigem.id}/saque`, { valor: 1.0, descricao: "smoke idempotente" }, origem.token, { chave });
  verificar("Repetir o saque com a mesma chave → 204 com Idempotency-Replayed, debitado uma vez só",
      r.status === 204 && repetida.status === 204 && repetida.headers.get("idempotency-replayed") === "true"
      && (await saldoDe(contaOrigem.id, origem.token)) === antes - 1, `status ${r.status}/${repetida.status}`);
  r = await chamar("POST", `/contas/${contaOrigem.id}/saque`, { valor: 5.0, descricao: "smoke idempotente" }, origem.token, { chave });
  verificar("Mesma chave com outro valor → 422", r.status === 422, `status ${r.status}`);
  r = await chamar("POST", `/contas/${contaOrigem.id}/deposito`, { valor: 1.0 }, origem.token, { chave: null });
  verificar("Movimentação sem Idempotency-Key → 400", r.status === 400 && (r.json?.campos ?? []).some((c) => c.campo === "Idempotency-Key"), `status ${r.status}`);

  // Contas que o Flyway (db/seed) cria a cada subida. Só leitura: o smoke nunca mexe no saldo delas.
  // Perfil visitante: Ana e Bruno com a senha pública. Perfil padrão/equipe: Felipe e Renan com a EQUIPE_SENHA do ambiente.
  console.log("\nContas pré-criadas pelo seed");
  const ana = await login("24681357928", "Visitante@2026");
  if (ana.status === 200) {
    r = await chamar("GET", "/contas", undefined, ana.json.token);
    verificar("Visitante Ana vê só a conta 0001/900001", r.json?.length >= 1 && r.json.every((c) => c.clienteId === ana.json.clienteId)
        && r.json.some((c) => c.numeroConta === "900001"), JSON.stringify(r.json));
  } else if (process.env.EQUIPE_SENHA) {
    const felipe = await login("12345678909", process.env.EQUIPE_SENHA);
    const renan = await login("98765432100", process.env.EQUIPE_SENHA);
    if (verificar("Login do Felipe e do Renan com a EQUIPE_SENHA → 200", felipe.status === 200 && renan.status === 200,
        `Felipe ${felipe.status}, Renan ${renan.status} (a API leu a mesma EQUIPE_SENHA? db/seed/equipe ligado?)`)) {
      r = await chamar("GET", "/contas", undefined, felipe.json.token);
      verificar("Felipe vê só as próprias contas, entre elas a 0001/100001", r.json?.length >= 1
          && r.json.every((c) => c.clienteId === felipe.json.clienteId)
          && r.json.some((c) => c.id === "5eed0000-0000-4000-8000-000000000014" && c.numeroConta === "100001"), JSON.stringify(r.json));
      r = await chamar("GET", "/contas/5eed0000-0000-4000-8000-000000000014/lancamentos", undefined, renan.json.token);
      verificar("Renan não vê o extrato do Felipe → 403", r.status === 403, `status ${r.status}`);
    }
  } else {
    console.log("⏭️  Contas do seed puladas (perfil visitante não está no ar e EQUIPE_SENHA não está no ambiente)");
  }

  console.log("\nCenários de erro");
  r = await chamar("POST", "/clientes", { nomeCompleto: "X", cpf: "11111111111", email: "x@x.com", telefone: "11999999999", cep: "01001000", numero: "1", senha: SENHA });
  verificar("CPF inválido → 400 com campo cpf", r.status === 400 && (r.json?.campos ?? []).some((c) => c.campo === "cpf"), `status ${r.status}`);
  r = await chamar("POST", `/contas/${contaOrigem.id}/saque`, { valor: 1000000, descricao: "smoke" }, origem.token);
  verificar("Saque maior que o saldo → 422", r.status === 422, `status ${r.status}`);
  r = await chamar("POST", "/contas/transferencia", { contaOrigemId: contaOrigem.id, contaDestinoId: contaOrigem.id, valor: 1, descricao: "smoke" }, origem.token);
  verificar("Transferência para a mesma conta → 422", r.status === 422, `status ${r.status}`);
  r = await chamar("POST", "/contas/00000000-0000-0000-0000-000000000000/deposito", { valor: 10, descricao: "smoke" }, origem.token);
  verificar("Depósito em conta inexistente → 404", r.status === 404, `status ${r.status}`);
  r = await chamar("POST", "/clientes", "{invalido");
  verificar("JSON inválido → 400", r.status === 400, `status ${r.status}`);
} catch (erro) {
  verificar("Execução do smoke test", false, erro.cause?.code === "ECONNREFUSED" ? `API fora do ar em ${BASE}` : erro.message);
}

const falhas = resultados.filter((x) => !x.ok).length;
console.log(`\n${resultados.length - falhas}/${resultados.length} verificações OK`);
process.exit(falhas ? 1 : 0);
