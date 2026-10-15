// Smoke test ponta a ponta contra a API rodando de verdade (MySQL ou H2).
// Uso: node scripts/smoke-e2e.mjs [baseUrl]      (padrão: http://localhost:8080)
// Cria dados novos a cada execução (CPF, e-mail e número de conta aleatórios), então pode rodar
// várias vezes no mesmo banco. Segue o roteiro de backend/README_TESTES_E2E.md em versão resumida.
// Sai com código 0 se todas as verificações passaram e 1 se alguma falhou.

const BASE = (process.argv[2] ?? "http://localhost:8080").replace(/\/$/, "");
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

async function chamar(metodo, caminho, corpo) {
  const resposta = await fetch(BASE + caminho, {
    method: metodo,
    headers: { "Content-Type": "application/json", "X-Request-Id": "smoke-e2e-" + aleatorio(6) },
    body: corpo === undefined ? undefined : typeof corpo === "string" ? corpo : JSON.stringify(corpo),
  });
  const texto = await resposta.text();
  let json = null;
  try { json = texto ? JSON.parse(texto) : null; } catch { /* corpo não é JSON */ }
  return { status: resposta.status, json };
}

function verificar(nome, condicao, detalhe = "") {
  resultados.push({ nome, ok: Boolean(condicao) });
  console.log(`${condicao ? "✅" : "❌"} ${nome}${condicao || !detalhe ? "" : `  →  ${detalhe}`}`);
  return Boolean(condicao);
}

async function novoCliente(nome) {
  const r = await chamar("POST", "/clientes", {
    nomeCompleto: nome, cpf: cpfValido(), email: `smoke.${aleatorio(8)}@rkfbank.com`,
    telefone: "11999999999", cep: "01001000", numero: "123",
  });
  verificar(`Cadastrar ${nome} → 201 com endereço do ViaCEP`, r.status === 201 && r.json?.endereco?.logradouro, `status ${r.status} ${JSON.stringify(r.json)}`);
  return r.json;
}

async function novaConta(clienteId) {
  const r = await chamar("POST", `/contas/clientes/${clienteId}`, { agencia: "0001", numeroConta: aleatorio(8), tipoContaEnum: "CORRENTE" });
  verificar("Abrir conta → 201, saldo 0.00 e ativa", r.status === 201 && Number(r.json?.saldo) === 0 && r.json?.ativa === true, `status ${r.status} ${JSON.stringify(r.json)}`);
  return r.json;
}

async function saldoDe(contaId) {
  const r = await chamar("GET", "/contas");
  return Number((r.json ?? []).find((c) => c.id === contaId)?.saldo);
}

try {
  console.log(`Smoke test E2E em ${BASE}\n`);
  const origem = await novoCliente("Smoke Origem");
  const destino = await novoCliente("Smoke Destino");
  if (!origem?.id || !destino?.id) throw new Error("cadastro falhou; a API está no ar e com acesso ao ViaCEP?");
  const contaOrigem = await novaConta(origem.id);
  const contaDestino = await novaConta(destino.id);

  let r = await chamar("POST", `/contas/${contaOrigem.id}/deposito`, { valor: 150.0, descricao: "smoke deposito" });
  verificar("Depósito de 150.00 → 204", r.status === 204, `status ${r.status}`);
  r = await chamar("POST", `/contas/${contaOrigem.id}/saque`, { valor: 20.0, descricao: "smoke saque" });
  verificar("Saque de 20.00 → 204", r.status === 204, `status ${r.status}`);
  r = await chamar("POST", "/contas/transferencia", { contaOrigemId: contaOrigem.id, contaDestinoId: contaDestino.id, valor: 50.0, descricao: "smoke transferencia" });
  verificar("Transferência de 50.00 → 204", r.status === 204, `status ${r.status}`);

  verificar("Saldo da origem = 80.00", (await saldoDe(contaOrigem.id)) === 80);
  verificar("Saldo do destino = 50.00", (await saldoDe(contaDestino.id)) === 50);

  r = await chamar("GET", `/contas/${contaOrigem.id}/lancamentos`);
  const tipos = (r.json ?? []).map((l) => l.tipoLancamento);
  verificar("Extrato da origem: 3 lançamentos, do mais novo ao mais antigo", JSON.stringify(tipos) === JSON.stringify(["TRANSFERENCIA", "SAQUE", "DEPOSITO"]), JSON.stringify(tipos));
  const [transf, saque, deposito] = r.json ?? [];
  verificar("Transferência com origem e destino preenchidos", transf?.contaOrigemId === contaOrigem.id && transf?.contaDestinoId === contaDestino.id);
  verificar("Saque com destino nulo e depósito com origem nula", saque?.contaDestinoId === null && deposito?.contaOrigemId === null);

  console.log("\nCenários de erro");
  r = await chamar("POST", "/clientes", { nomeCompleto: "X", cpf: "11111111111", email: "x@x.com", telefone: "11999999999", cep: "01001000", numero: "1" });
  verificar("CPF inválido → 400 com campo cpf", r.status === 400 && (r.json?.campos ?? []).some((c) => c.campo === "cpf"), `status ${r.status}`);
  r = await chamar("POST", `/contas/${contaOrigem.id}/saque`, { valor: 1000000, descricao: "smoke" });
  verificar("Saque maior que o saldo → 422", r.status === 422, `status ${r.status}`);
  r = await chamar("POST", "/contas/transferencia", { contaOrigemId: contaOrigem.id, contaDestinoId: contaOrigem.id, valor: 1, descricao: "smoke" });
  verificar("Transferência para a mesma conta → 422", r.status === 422, `status ${r.status}`);
  r = await chamar("POST", "/contas/00000000-0000-0000-0000-000000000000/deposito", { valor: 10, descricao: "smoke" });
  verificar("Depósito em conta inexistente → 404", r.status === 404, `status ${r.status}`);
  r = await chamar("POST", "/clientes", "{invalido");
  verificar("JSON inválido → 400", r.status === 400, `status ${r.status}`);
} catch (erro) {
  verificar("Execução do smoke test", false, erro.cause?.code === "ECONNREFUSED" ? `API fora do ar em ${BASE}` : erro.message);
}

const falhas = resultados.filter((x) => !x.ok).length;
console.log(`\n${resultados.length - falhas}/${resultados.length} verificações OK`);
process.exit(falhas ? 1 : 0);
