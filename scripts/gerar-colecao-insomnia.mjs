// Gera a coleção do Insomnia do RKF Bank (formato de exportação v4, que o Insomnia 9 a 13 importa).
//
//   node scripts/gerar-colecao-insomnia.mjs              → backend/insomnia/rkfbank.insomnia.json (pública)
//   node scripts/gerar-colecao-insomnia.mjs --equipe     → também .equipe/testes/rkfbank-equipe.insomnia.json
//                                                          (privada: lê EQUIPE_SENHA, ADMIN_CPF e ADMIN_SENHA do .env)
//
// Por que gerar em vez de editar o JSON na mão: o arquivo exportado é enorme e cheio de ids; aqui cada requisição
// ocupa poucas linhas e mudou o contrato, muda aqui e roda de novo. O JSON gerado é o que vai para o Insomnia.
//
// Como a coleção evita copiar e colar: cada login guarda o token no ambiente (script "after-response"), e as
// requisições seguintes usam {{ _.token_origem }}, {{ _.conta_origem_id }}... O mesmo vale para o cliente novo
// (CPF válido sorteado antes do cadastro) e para o id do usuário na gestão do ADMIN.
// scripts/rodar-colecao-insomnia.mjs roda esta mesma coleção pelo terminal, para conferir que tudo funciona.

import { mkdirSync, readFileSync, writeFileSync, existsSync } from "node:fs";
import { dirname, join } from "node:path";
import { fileURLToPath } from "node:url";

const RAIZ = join(dirname(fileURLToPath(import.meta.url)), "..");
const SAIDA_PUBLICA = join(RAIZ, "backend", "insomnia", "rkfbank.insomnia.json");
const SAIDA_EQUIPE = join(RAIZ, ".equipe", "testes", "rkfbank-equipe.insomnia.json");

// ── Trechos de script reaproveitados (rodam dentro do Insomnia, na aba "Scripts") ─────────────────────────────

const status = (codigo) =>
  `insomnia.test("status ${codigo}", () => insomnia.expect(insomnia.response.code).to.equal(${codigo}));`;

// Erros vêm no formato padrão do contrato: confere a mensagem para quem está aprendendo ver o texto real.
const mensagem = (texto) =>
  `insomnia.test("mensagem: ${texto}", () => insomnia.expect(insomnia.response.json().message).to.include(${JSON.stringify(texto)}));`;

const guardaLogin = (variavelToken, variavelCliente) =>
  [
    status(200),
    "if (insomnia.response.code === 200) {",
    "  const login = insomnia.response.json();",
    `  insomnia.environment.set("${variavelToken}", login.token);`,
    variavelCliente ? `  insomnia.environment.set("${variavelCliente}", login.clienteId ?? "");` : "",
    '  console.log("perfil:", login.perfil, "· token vale", login.expiraEmSegundos, "s");',
    "}",
  ].filter(Boolean).join("\n");

// Grava no ambiente só quando a resposta veio com o status esperado: um erro não pode apagar o que já estava certo.
const guardaSe = (codigo, linha) => [status(codigo), `if (insomnia.response.code === ${codigo}) ${linha}`].join("\n");

// CPF válido (dígitos verificadores) e e-mail únicos: o cadastro pode rodar quantas vezes quiser no mesmo banco.
const SORTEIA_CLIENTE_NOVO = `
const n = Array.from({ length: 9 }, () => Math.floor(Math.random() * 10));
if (n.every((d) => d === n[0])) n[0] = (n[0] + 1) % 10;
for (const tamanho of [9, 10]) {
  const soma = n.slice(0, tamanho).reduce((acc, d, i) => acc + d * (tamanho + 1 - i), 0);
  const resto = (soma * 10) % 11;
  n.push(resto === 10 ? 0 : resto);
}
const sufixo = Date.now().toString(36);
insomnia.environment.set("novo_cpf", n.join(""));
insomnia.environment.set("novo_email", "cliente.novo." + sufixo + "@exemplo.test");
insomnia.environment.set("novo_senha", "senha-do-novo-" + sufixo);
console.log("CPF sorteado:", n.join(""));
`.trim();

// Chave de idempotência fixa para mostrar a repetição (o {% uuid %} gera uma nova a cada envio).
const SORTEIA_CHAVE_FIXA = `
insomnia.environment.set("chave_fixa", "chave-fixa-" + Date.now() + "-" + Math.random().toString(36).slice(2, 8));
`.trim();

// ── Requisições ───────────────────────────────────────────────────────────────────────────────────────────────

const JSON_CT = { name: "Content-Type", value: "application/json" };
const CHAVE_NOVA = { name: "Idempotency-Key", value: "{% uuid 'v4' %}" };
const token = (variavel) => ({ type: "bearer", token: `{{ _.${variavel} }}`, prefix: "" });

// Atalho: req(nome, método, caminho, { corpo, auth, headers, antes, depois, descricao })
function req(nome, metodo, caminho, opcoes = {}) {
  const headers = [...(opcoes.headers ?? [])];
  if (opcoes.corpo !== undefined) headers.unshift(JSON_CT);
  return {
    nome, metodo, caminho, headers,
    corpo: opcoes.corpo === undefined ? undefined
      : typeof opcoes.corpo === "string" ? opcoes.corpo : JSON.stringify(opcoes.corpo, null, 2),
    auth: opcoes.auth ? token(opcoes.auth) : { type: "none" },
    antes: opcoes.antes ?? "",
    depois: opcoes.depois ?? "",
    descricao: opcoes.descricao ?? "",
  };
}

const PASTAS = [
  {
    nome: "00 · Comece aqui",
    descricao:
      "Escolha o **ambiente** no canto superior esquerdo (Visitante ou Equipe) e mande a primeira requisição. " +
      "`{\"status\":\"UP\"}` = a API está no ar. Sem resposta? Ela não subiu: veja o guia `backend/README_TESTES_E2E.md`.",
    itens: [
      req("Saúde da API", "GET", "/actuator/health", {
        depois: status(200) + '\ninsomnia.test("UP", () => insomnia.expect(insomnia.response.json().status).to.equal("UP"));',
        descricao: "Pública. Usada pelo Docker e pelo deploy para saber se a API respondeu.",
      }),
    ],
  },
  {
    nome: "01 · Login (guarda o token sozinho)",
    descricao:
      "Cada login roda um script que salva o token no ambiente (`token_origem`, `token_destino`, `token_admin`). " +
      "As outras pastas já usam essas variáveis na aba **Auth → Bearer**. O token vence (15 min no MySQL, 60 min no H2): " +
      "deu `401 Token invalido ou expirado`? Mande o login de novo.",
    itens: [
      req("Login · cliente ORIGEM (Ana / Felipe)", "POST", "/auth/login", {
        corpo: '{\n  "cpf": "{{ _.cpf_origem }}",\n  "senha": "{{ _.senha_clientes }}"\n}',
        depois: guardaLogin("token_origem", "cliente_origem_id"),
      }),
      req("Login · cliente DESTINO (Bruno / Renan)", "POST", "/auth/login", {
        corpo: '{\n  "cpf": "{{ _.cpf_destino }}",\n  "senha": "{{ _.senha_clientes }}"\n}',
        depois: guardaLogin("token_destino", "cliente_destino_id"),
      }),
      req("Login · ADMIN (só perfil equipe ou MySQL)", "POST", "/auth/login", {
        corpo: '{\n  "cpf": "{{ _.admin_cpf }}",\n  "senha": "{{ _.admin_senha }}"\n}',
        depois: guardaLogin("token_admin"),
        descricao:
          "O ADMIN não nasce por rota: a API cria na subida se `ADMIN_CPF` e `ADMIN_SENHA` estiverem no ambiente. " +
          "No perfil **visitante** não existe ADMIN (de propósito): este login dá `401`.",
      }),
    ],
  },
  {
    nome: "02 · Cliente novo (cadastro → login → conta)",
    descricao:
      "O cadastro sorteia um CPF válido e um e-mail novos a cada envio (aba Scripts → Pre-request), então pode repetir " +
      "à vontade. Precisa de internet: o endereço vem do ViaCEP.",
    itens: [
      req("Cadastrar cliente (público)", "POST", "/clientes", {
        antes: SORTEIA_CLIENTE_NOVO,
        corpo:
          '{\n  "nomeCompleto": "Cliente Novo do Insomnia",\n  "cpf": "{{ _.novo_cpf }}",\n  "email": "{{ _.novo_email }}",\n' +
          '  "telefone": "11999998888",\n  "cep": "06020010",\n  "numero": "123",\n  "complemento": "Apto 45",\n' +
          '  "senha": "{{ _.novo_senha }}"\n}',
        depois: guardaSe(201, 'insomnia.environment.set("novo_cliente_id", insomnia.response.json().id);'),
        descricao: "`201` com o cliente (CPF, telefone e CEP voltam formatados). O cadastro **não** devolve token: faça o login.",
      }),
      req("Login · cliente novo", "POST", "/auth/login", {
        corpo: '{\n  "cpf": "{{ _.novo_cpf }}",\n  "senha": "{{ _.novo_senha }}"\n}',
        depois: guardaLogin("token_novo"),
      }),
      req("Abrir conta corrente (cliente novo)", "POST", "/contas", {
        auth: "token_novo",
        corpo: { tipoConta: "CORRENTE" },
        depois: guardaSe(201, 'insomnia.environment.set("conta_nova_id", insomnia.response.json().id);'),
        descricao: "O dono vem do **token**, não da URL. Agência `0001` e número (com dígito verificador) são gerados pelo banco.",
      }),
      req("Abrir poupança (cliente novo)", "POST", "/contas", {
        auth: "token_novo",
        corpo: { tipoConta: "POUPANCA" },
        depois: status(201),
      }),
      req("Minhas contas (cliente novo)", "GET", "/contas", { auth: "token_novo", depois: status(200) }),
    ],
  },
  {
    nome: "03 · Dinheiro: depósito, saque, transferência, extrato",
    descricao:
      "Tudo com o token da ORIGEM. Depósito, saque e transferência levam `Idempotency-Key` (aba Headers): o " +
      "`{% uuid 'v4' %}` gera uma chave nova a cada envio. Resposta `204` não tem corpo: veja o saldo em **Minhas contas**.",
    itens: [
      req("Minhas contas (ORIGEM)", "GET", "/contas", {
        auth: "token_origem",
        depois: [
          status(200),
          'const contas = insomnia.response.code === 200 ? insomnia.response.json() : [];',
          'if (contas.length) insomnia.environment.set("conta_origem_id", contas[0].id);',
          'console.log(contas.map((c) => c.numeroConta + " saldo " + c.saldo).join("\\n"));',
        ].join("\n"),
        descricao: "`CLIENTE` vê só as dele; `ADMIN` vê todas. O script guarda a primeira conta em `conta_origem_id`.",
      }),
      req("Minhas contas paginadas", "GET", "/contas?page=0&size=10", {
        auth: "token_origem",
        depois: status(200) + '\ninsomnia.test("é página", () => insomnia.expect(insomnia.response.json().content).to.be.an("array"));',
        descricao: "Basta `page` **ou** `size` para vir a página: `content`, `totalElements`, `totalPages`, `number`, `size`, `first`, `last`.",
      }),
      req("Depositar R$ 150,00", "POST", "/contas/{{ _.conta_origem_id }}/depositos", {
        auth: "token_origem", headers: [CHAVE_NOVA], corpo: { valor: 150.0, descricao: "Deposito pelo Insomnia" }, depois: status(204),
      }),
      req("Sacar R$ 50,00", "POST", "/contas/{{ _.conta_origem_id }}/saques", {
        auth: "token_origem", headers: [CHAVE_NOVA], corpo: { valor: 50.0, descricao: "Saque pelo Insomnia" }, depois: status(204),
      }),
      req("Transferir R$ 25,00 ORIGEM → DESTINO", "POST", "/transferencias", {
        auth: "token_origem",
        headers: [CHAVE_NOVA],
        corpo:
          '{\n  "contaOrigemId": "{{ _.conta_origem_id }}",\n  "contaDestinoId": "{{ _.conta_destino_id }}",\n' +
          '  "valor": 25.00,\n  "descricao": "Transferencia pelo Insomnia"\n}',
        depois: status(204),
        descricao: "Débito e crédito na mesma transação: ou os dois acontecem, ou nenhum.",
      }),
      req("Extrato da ORIGEM", "GET", "/contas/{{ _.conta_origem_id }}/lancamentos", {
        auth: "token_origem", depois: status(200), descricao: "Do mais recente para o mais antigo. `valor` sempre positivo.",
      }),
      req("Extrato da ORIGEM paginado", "GET", "/contas/{{ _.conta_origem_id }}/lancamentos?page=0&size=5", {
        auth: "token_origem", depois: status(200),
      }),
      req("Extrato do DESTINO (token do destino)", "GET", "/contas/{{ _.conta_destino_id }}/lancamentos", {
        auth: "token_destino",
        depois: status(200) + '\ninsomnia.test("recebeu a transferência", () => insomnia.expect(insomnia.response.json()[0].tipoLancamento).to.equal("TRANSFERENCIA"));',
      }),
    ],
  },
  {
    nome: "04 · Idempotência (o mesmo pedido não movimenta duas vezes)",
    descricao:
      "Mande na ordem. A 1ª sorteia uma chave e guarda em `chave_fixa`; a 2ª repete a MESMA chave com o MESMO corpo " +
      "(como se a rede tivesse caído): `204` com o header `Idempotency-Replayed: true` e o saldo **não** muda.",
    itens: [
      req("1ª vez: depósito com chave fixa", "POST", "/contas/{{ _.conta_origem_id }}/depositos", {
        auth: "token_origem", antes: SORTEIA_CHAVE_FIXA,
        headers: [{ name: "Idempotency-Key", value: "{{ _.chave_fixa }}" }],
        corpo: { valor: 10.0, descricao: "Idempotencia" }, depois: status(204),
      }),
      req("2ª vez: mesma chave, mesmo corpo → repetição", "POST", "/contas/{{ _.conta_origem_id }}/depositos", {
        auth: "token_origem",
        headers: [{ name: "Idempotency-Key", value: "{{ _.chave_fixa }}" }],
        corpo: { valor: 10.0, descricao: "Idempotencia" },
        depois: status(204) + '\ninsomnia.test("Idempotency-Replayed", () => insomnia.expect(insomnia.response.headers.get("Idempotency-Replayed")).to.equal("true"));',
      }),
      req("Mesma chave, outro valor → 422", "POST", "/contas/{{ _.conta_origem_id }}/depositos", {
        auth: "token_origem",
        headers: [{ name: "Idempotency-Key", value: "{{ _.chave_fixa }}" }],
        corpo: { valor: 99.0, descricao: "Idempotencia" },
        depois: [status(422), mensagem("Idempotency-Key ja usada")].join("\n"),
      }),
      req("Sem a chave → 400", "POST", "/contas/{{ _.conta_origem_id }}/depositos", {
        auth: "token_origem", corpo: { valor: 10.0 }, depois: status(400),
      }),
    ],
  },
  {
    nome: "05 · ADMIN: gestão de clientes e usuários",
    descricao:
      "Precisa do **Login · ADMIN** (pasta 01) e do **cliente novo** (pasta 02): é nele que o ADMIN mexe, para não " +
      "estragar as contas fixas. No perfil visitante não há ADMIN: tudo aqui dá `401`.",
    itens: [
      req("Listar clientes (paginado)", "GET", "/clientes?page=0&size=10", { auth: "token_admin", depois: status(200) }),
      req("Listar todas as contas", "GET", "/contas?page=0&size=20", { auth: "token_admin", depois: status(200) }),
      req("Listar usuários (guarda o id do cliente novo)", "GET", "/admin/usuarios", {
        auth: "token_admin",
        depois: [
          status(200),
          'const usuarios = insomnia.response.code === 200 ? insomnia.response.json() : [];',
          'const alvo = usuarios.find((u) => u.clienteId === insomnia.environment.get("novo_cliente_id"));',
          'if (alvo) insomnia.environment.set("usuario_novo_id", alvo.id);',
          'insomnia.test("achou o cliente novo", () => insomnia.expect(alvo).to.be.ok);',
        ].join("\n"),
        descricao: "CPF vem mascarado. O `id` daqui (tb_usuario) é o que vai nas rotas abaixo, **não** o `clienteId`.",
      }),
      req("Promover cliente novo a ADMIN", "PATCH", "/admin/usuarios/{{ _.usuario_novo_id }}/perfil", {
        auth: "token_admin", corpo: { perfil: "ADMIN" },
        depois: status(200) + '\ninsomnia.test("virou ADMIN", () => insomnia.expect(insomnia.response.json().perfil).to.equal("ADMIN"));',
        descricao: "Vale na próxima requisição: o token antigo do cliente novo passa a dar `401`. Ele faz login de novo e já entra como ADMIN.",
      }),
      req("Rebaixar de volta a CLIENTE", "PATCH", "/admin/usuarios/{{ _.usuario_novo_id }}/perfil", {
        auth: "token_admin", corpo: { perfil: "CLIENTE" }, depois: status(200),
      }),
      req("Desativar o acesso", "PATCH", "/admin/usuarios/{{ _.usuario_novo_id }}/acesso", {
        auth: "token_admin", corpo: { ativo: false }, depois: status(200),
      }),
      req("Login do desativado → 401", "POST", "/auth/login", {
        corpo: '{\n  "cpf": "{{ _.novo_cpf }}",\n  "senha": "{{ _.novo_senha }}"\n}', depois: status(401),
      }),
      req("Reativar o acesso", "PATCH", "/admin/usuarios/{{ _.usuario_novo_id }}/acesso", {
        auth: "token_admin", corpo: { ativo: true }, depois: status(200),
      }),
      req("Redefinir a senha (quem esqueceu)", "PATCH", "/admin/usuarios/{{ _.usuario_novo_id }}/senha", {
        auth: "token_admin",
        antes: 'insomnia.environment.set("senha_redefinida", "redefinida-" + Date.now().toString(36));',
        corpo: '{\n  "novaSenha": "{{ _.senha_redefinida }}"\n}',
        depois: guardaSe(204, 'insomnia.environment.set("novo_senha", insomnia.environment.get("senha_redefinida"));'),
        descricao: "O script passa a senha nova para `novo_senha`: o próximo login do cliente novo já usa ela.",
      }),
      req("Login · cliente novo (de novo)", "POST", "/auth/login", {
        corpo: '{\n  "cpf": "{{ _.novo_cpf }}",\n  "senha": "{{ _.novo_senha }}"\n}',
        depois: guardaLogin("token_novo"),
      }),
    ],
  },
  {
    nome: "06 · Meus dados (LGPD) e senha",
    descricao:
      "O titular vê, corrige e encerra o próprio cadastro (`/clientes/me`: o `me` é sempre o dono do token). " +
      "Troca de senha e encerramento são feitos no **cliente novo**, para as contas fixas continuarem entrando.",
    itens: [
      req("Ver meus dados (ORIGEM)", "GET", "/clientes/me", { auth: "token_origem", depois: status(200) }),
      req("Corrigir meu telefone (ORIGEM)", "PATCH", "/clientes/me", {
        auth: "token_origem", corpo: { telefone: "11912345678" }, depois: status(200),
        descricao: "Só muda o que vier no corpo. CPF não muda. CEP novo exige `numero` (o endereço vem do ViaCEP).",
      }),
      req("Trocar a própria senha (cliente novo)", "PATCH", "/usuarios/me/senha", {
        auth: "token_novo",
        antes: 'insomnia.environment.set("senha_trocada", "trocada-" + Date.now().toString(36));',
        corpo: '{\n  "senhaAtual": "{{ _.novo_senha }}",\n  "novaSenha": "{{ _.senha_trocada }}"\n}',
        depois: guardaSe(204, 'insomnia.environment.set("novo_senha", insomnia.environment.get("senha_trocada"));'),
        descricao: "Depois do `204`, todo token antigo (inclusive este) dá `401`. O script já guarda a senha nova.",
      }),
      req("Token antigo depois da troca → 401", "GET", "/contas", {
        auth: "token_novo", depois: [status(401), mensagem("Seu acesso foi alterado")].join("\n"),
      }),
      req("Login · cliente novo com a senha nova", "POST", "/auth/login", {
        corpo: '{\n  "cpf": "{{ _.novo_cpf }}",\n  "senha": "{{ _.novo_senha }}"\n}',
        depois: guardaLogin("token_novo"),
      }),
      req("⚠️ Encerrar o cadastro (cliente novo)", "POST", "/clientes/me/encerramento", {
        auth: "token_novo", corpo: '{\n  "senha": "{{ _.novo_senha }}"\n}', depois: status(200),
        descricao:
          "Exige saldo zero em todas as contas (as do cliente novo estão zeradas). Contas ficam inativas, o login é " +
          "desativado e os dados são anonimizados depois de 5 anos. Para repetir, cadastre outro cliente novo (pasta 02).",
      }),
    ],
  },
  {
    nome: "07 · Erros que DEVEM acontecer",
    descricao: "Cada uma testa uma proteção. Se alguma passar com 2xx, achamos um bug.",
    itens: [
      req("Sem token → 401", "GET", "/contas", { depois: [status(401), mensagem("Autenticacao necessaria")].join("\n") }),
      req("CLIENTE em rota de ADMIN → 403", "GET", "/clientes", {
        auth: "token_origem", depois: [status(403), mensagem("Acesso negado")].join("\n"),
      }),
      req("Depositar na conta de outro cliente → 403", "POST", "/contas/{{ _.conta_origem_id }}/depositos", {
        auth: "token_destino", headers: [CHAVE_NOVA], corpo: { valor: 1.0 },
        depois: [status(403), mensagem("Conta nao pertence")].join("\n"),
      }),
      req("Sacar mais que o saldo → 422", "POST", "/contas/{{ _.conta_origem_id }}/saques", {
        auth: "token_origem", headers: [CHAVE_NOVA], corpo: { valor: 999999.0 },
        depois: [status(422), mensagem("Saldo insuficiente")].join("\n"),
      }),
      req("Transferir para a mesma conta → 422", "POST", "/transferencias", {
        auth: "token_origem", headers: [CHAVE_NOVA],
        corpo: '{\n  "contaOrigemId": "{{ _.conta_origem_id }}",\n  "contaDestinoId": "{{ _.conta_origem_id }}",\n  "valor": 1.00\n}',
        depois: [status(422), mensagem("nao podem ser iguais")].join("\n"),
      }),
      req("Conta que não existe → 404", "GET", "/contas/00000000-0000-4000-8000-000000000000/lancamentos", {
        auth: "token_origem", depois: status(404),
      }),
      req("Id que não é UUID → 400", "GET", "/contas/abc/lancamentos", { auth: "token_origem", depois: status(400) }),
      req("Cadastro inválido → 400 com os campos", "POST", "/clientes", {
        corpo: { nomeCompleto: "", cpf: "11111111111", email: "nao-e-email", telefone: "", cep: "", numero: "", senha: "curta" },
        depois: status(400) + '\ninsomnia.test("lista os campos", () => insomnia.expect(insomnia.response.json().campos.length).to.be.above(0));',
      }),
      req("Valor com 3 casas → 400", "POST", "/contas/{{ _.conta_origem_id }}/depositos", {
        auth: "token_origem", headers: [CHAVE_NOVA], corpo: '{\n  "valor": 10.001\n}', depois: status(400),
      }),
    ],
  },
  {
    nome: "08 · Rotas obsoletas (ainda funcionam até o contrato 3.0)",
    descricao:
      "Os nomes antigos continuam respondendo e avisam com `Deprecation: true` e `Link: <rota nova>; rel=\"successor-version\"` " +
      "(aba Headers da resposta). Use sempre as rotas novas das outras pastas.",
    itens: [
      req("POST /contas/{id}/deposito (antiga)", "POST", "/contas/{{ _.conta_origem_id }}/deposito", {
        auth: "token_origem", headers: [CHAVE_NOVA], corpo: { valor: 5.0 },
        depois: status(204) + '\ninsomnia.test("Deprecation", () => insomnia.expect(insomnia.response.headers.get("Deprecation")).to.equal("true"));',
      }),
      req("POST /contas/transferencia (antiga)", "POST", "/contas/transferencia", {
        auth: "token_origem", headers: [CHAVE_NOVA],
        corpo: '{\n  "contaOrigemId": "{{ _.conta_origem_id }}",\n  "contaDestinoId": "{{ _.conta_destino_id }}",\n  "valor": 1.00\n}',
        depois: status(204) + '\ninsomnia.test("Deprecation", () => insomnia.expect(insomnia.response.headers.get("Deprecation")).to.equal("true"));',
      }),
      req("GET /contas?paginado=true (antiga)", "GET", "/contas?paginado=true", { auth: "token_origem", depois: status(200) }),
    ],
  },
];

// ── Ambientes ─────────────────────────────────────────────────────────────────────────────────────────────────

// Variáveis que os scripts preenchem: começam vazias e aparecem no ambiente para quem quiser olhar.
const PREENCHIDAS_PELOS_SCRIPTS = {
  token_origem: "", token_destino: "", token_admin: "", token_novo: "",
  cliente_origem_id: "", cliente_destino_id: "",
  novo_cpf: "", novo_email: "", novo_senha: "", novo_cliente_id: "", conta_nova_id: "", usuario_novo_id: "",
  chave_fixa: "", senha_redefinida: "", senha_trocada: "",
};

function ambientes({ equipeSenha, adminCpf, adminSenha }) {
  return {
    base: { base_url: "http://localhost:8080", ...PREENCHIDAS_PELOS_SCRIPTS },
    visitante: {
      // Contas do db/seed/visitante (perfil visitante, H2). Senha pública: está no VISITANTES.md.
      cpf_origem: "24681357928", cpf_destino: "97531864282", senha_clientes: "Visitante@2026",
      conta_origem_id: "a0000000-0000-4000-8000-000000000114",
      conta_destino_id: "a0000000-0000-4000-8000-000000000124",
      admin_cpf: "", admin_senha: "",
    },
    equipe: {
      // Contas do db/seed/equipe (perfil padrão MySQL e perfil equipe H2). Senhas: só no .env de cada um.
      cpf_origem: "12345678909", cpf_destino: "98765432100", senha_clientes: equipeSenha,
      conta_origem_id: "5eed0000-0000-4000-8000-000000000014",
      conta_destino_id: "5eed0000-0000-4000-8000-000000000024",
      admin_cpf: adminCpf, admin_senha: adminSenha,
    },
  };
}

// ── Montagem no formato de exportação v4 do Insomnia ─────────────────────────────────────────────────────────

function montar(valores) {
  const amb = ambientes(valores);
  const recursos = [
    {
      _id: "wrk_rkfbank", _type: "workspace", parentId: null, scope: "collection", name: "RKF Bank API",
      description: "Coleção gerada por scripts/gerar-colecao-insomnia.mjs. Guia: backend/README_TESTES_E2E.md.",
    },
    { _id: "env_rkfbank_base", _type: "environment", parentId: "wrk_rkfbank", name: "Base Environment", data: amb.base, metaSortKey: 0 },
    {
      _id: "env_rkfbank_visitante", _type: "environment", parentId: "env_rkfbank_base", name: "Visitante (H2, Ana e Bruno)",
      data: amb.visitante, color: "#2e7d32", metaSortKey: 1,
    },
    {
      _id: "env_rkfbank_equipe", _type: "environment", parentId: "env_rkfbank_base", name: "Equipe (MySQL ou H2, Felipe e Renan)",
      data: amb.equipe, color: "#1565c0", metaSortKey: 2,
    },
  ];
  PASTAS.forEach((pasta, p) => {
    const idPasta = `fld_rkfbank_${String(p).padStart(2, "0")}`;
    recursos.push({ _id: idPasta, _type: "request_group", parentId: "wrk_rkfbank", name: pasta.nome, description: pasta.descricao, metaSortKey: p });
    pasta.itens.forEach((r, i) => {
      recursos.push({
        _id: `req_rkfbank_${String(p).padStart(2, "0")}_${String(i).padStart(2, "0")}`,
        _type: "request", parentId: idPasta, name: r.nome, description: r.descricao, method: r.metodo,
        url: `{{ _.base_url }}${r.caminho}`,
        body: r.corpo === undefined ? {} : { mimeType: "application/json", text: r.corpo },
        headers: r.headers, authentication: r.auth, parameters: [],
        preRequestScript: r.antes, afterResponseScript: r.depois, metaSortKey: i,
        settingEncodeUrl: true, settingSendCookies: false, settingStoreCookies: false,
      });
    });
  });
  return { _type: "export", __export_format: 4, __export_source: "rkfbank.gerar-colecao-insomnia", resources: recursos };
}

function lerEnv() {
  const arquivo = join(RAIZ, ".env");
  if (!existsSync(arquivo)) throw new Error("--equipe precisa do .env na raiz (EQUIPE_SENHA, ADMIN_CPF, ADMIN_SENHA)");
  const valores = {};
  for (const linha of readFileSync(arquivo, "utf8").split(/\r?\n/)) {
    const m = linha.match(/^\s*([A-Z_]+)\s*=\s*(.*)\s*$/);
    if (m) valores[m[1]] = m[2].replace(/^["']|["']$/g, "");
  }
  return valores;
}

function salvar(caminho, colecao) {
  mkdirSync(dirname(caminho), { recursive: true });
  writeFileSync(caminho, JSON.stringify(colecao, null, 2) + "\n");
  const total = colecao.resources.filter((r) => r._type === "request").length;
  console.log(`✅ ${total} requisições → ${caminho}`);
}

// Pública: senhas da equipe como instrução, nunca o valor (o repositório é público).
salvar(SAIDA_PUBLICA, montar({
  equipeSenha: "COLE_AQUI_A_EQUIPE_SENHA_DO_SEU_.env",
  adminCpf: "COLE_AQUI_O_ADMIN_CPF_DO_SEU_.env",
  adminSenha: "COLE_AQUI_A_ADMIN_SENHA_DO_SEU_.env",
}));

if (process.argv.includes("--equipe")) {
  const env = lerEnv();
  salvar(SAIDA_EQUIPE, montar({ equipeSenha: env.EQUIPE_SENHA ?? "", adminCpf: env.ADMIN_CPF ?? "", adminSenha: env.ADMIN_SENHA ?? "" }));
}
