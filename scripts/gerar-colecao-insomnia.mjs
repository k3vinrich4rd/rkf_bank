// Gera a coleção do Insomnia do RKF Bank (formato de exportação v4, que o Insomnia 9 a 13 importa).
//
//   node scripts/gerar-colecao-insomnia.mjs              → backend/insomnia/rkfbank.insomnia.json e .yaml (pública)
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

// Erros vêm no formato padrão do contrato (3.0): confere o "codigo", que é estável. O "message" é só para ler
// (aparece na aba Preview da resposta) e pode mudar de texto sem quebrar ninguém.
const codigo = (valor) =>
  `insomnia.test("codigo ${valor}", () => insomnia.expect(insomnia.response.json().codigo).to.equal("${valor}"));`;

const guardaLogin = (variavelToken, variavelCliente) =>
  [
    status(200),
    "if (insomnia.response.code === 200) {",
    "  const login = insomnia.response.json();",
    `  insomnia.environment.set("${variavelToken}", login.token);`,
    `  insomnia.environment.set("${variavelToken.replace("token_", "refresh_")}", login.refreshToken);`,
    variavelCliente ? `  insomnia.environment.set("${variavelCliente}", login.clienteId ?? "");` : "",
    '  console.log("perfil:", login.perfil, "· token vale", login.expiraEmSegundos, "s · renovação vale", login.refreshExpiraEmSegundos, "s");',
    "}",
  ].filter(Boolean).join("\n");

// Contrato 3.1: o cadastro exige o aceite dos termos (LGPD). Sem `termosVersao`, vale a versão vigente.
const ACEITE = '  "senha": "senha-forte-1",\n  "aceiteTermos": true\n}';

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
const d = n.join("");
insomnia.environment.set("novo_cpf_mascara", d.slice(0, 3) + "." + d.slice(3, 6) + "." + d.slice(6, 9) + "-" + d.slice(9));
insomnia.environment.set("novo_email", "cliente.novo." + sufixo + "@exemplo.test");
insomnia.environment.set("novo_senha", "senha-do-novo-" + sufixo);
console.log("CPF sorteado:", n.join(""));
`.trim();

// Lê o "sub" (id do usuário) do payload do JWT, que é só Base64URL: dá para ler, não para alterar (a assinatura não bate).
const ID_DO_TOKEN = (variavel) => `
if (insomnia.response.code === 200) {
  const payload = insomnia.response.json().token.split(".")[1].replace(/-/g, "+").replace(/_/g, "/");
  insomnia.environment.set("${variavel}", JSON.parse(atob(payload)).sub);
}`.trim();

// Um segundo CPF válido, para o cadastro que deve falhar pelo CEP (e não pelo CPF repetido).
const SORTEIA_CPF_EXTRA = SORTEIA_CLIENTE_NOVO.split("\nconst sufixo")[0] + '\ninsomnia.environment.set("cpf_extra", n.join(""));';

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
      req("Versão dos termos (pública)", "GET", "/termos", {
        depois: guardaSe(200, 'insomnia.environment.set("termos_versao", insomnia.response.json().versao);'),
        descricao: "Versão vigente dos Termos de Uso e da Política de Privacidade (contrato 3.1). A tela de cadastro mostra estes textos e manda a versão aceita em `termosVersao`.",
      }),
    ],
  },
  {
    nome: "01 · Login (guarda o token sozinho)",
    descricao:
      "Cada login roda um script que salva no ambiente o token de acesso (`token_origem`, `token_destino`, `token_admin`) e o " +
      "de renovação (`refresh_origem`...). As outras pastas já usam essas variáveis na aba **Auth → Bearer**. O token de " +
      "acesso vence (15 min no MySQL, 60 min no H2): deu `401 Token inválido ou expirado`? Mande o login de novo, ou " +
      "renove pela pasta **09 · Sessão**.",
    itens: [
      req("Login · cliente ORIGEM (Ana / Fellipe)", "POST", "/auth/login", {
        corpo: '{\n  "cpf": "{{ _.cpf_origem }}",\n  "senha": "{{ _.senha_clientes }}"\n}',
        depois: guardaLogin("token_origem", "cliente_origem_id"),
      }),
      req("Login · cliente DESTINO (Bruno / Renan)", "POST", "/auth/login", {
        corpo: '{\n  "cpf": "{{ _.cpf_destino }}",\n  "senha": "{{ _.senha_clientes }}"\n}',
        depois: guardaLogin("token_destino", "cliente_destino_id"),
      }),
      req("Login · ADMIN (só perfil equipe ou MySQL)", "POST", "/auth/login", {
        corpo: '{\n  "cpf": "{{ _.admin_cpf }}",\n  "senha": "{{ _.admin_senha }}"\n}',
        // O "sub" do token é o id do usuário: é assim que a pasta 05 acha o PRÓPRIO admin sem adivinhar pela lista.
        depois: guardaLogin("token_admin") + "\n" + ID_DO_TOKEN("usuario_admin_id"),
        descricao:
          "O ADMIN não nasce por rota: a API cria na subida se `ADMIN_CPF` e `ADMIN_SENHA` estiverem no ambiente. " +
          "No perfil **visitante** não existe ADMIN (de propósito): este login dá `401`.",
      }),
    ],
  },
  {
    nome: "02 · Fluxo E2E: cliente novo, do cadastro ao extrato",
    descricao:
      "O roteiro E2E principal, na ordem: cadastro (CPF, CEP e telefone com máscara, endereço pelo ViaCEP) → login → " +
      "contas ORIGEM e DESTINO → depósito, saque e transferência → extratos. O cadastro sorteia CPF e e-mail novos a cada envio, então pode repetir " +
      "à vontade. Precisa de internet: o endereço vem do ViaCEP.",
    itens: [
      req("Cadastrar cliente (público)", "POST", "/clientes", {
        antes: SORTEIA_CLIENTE_NOVO,
        corpo:
          '{\n  "nomeCompleto": "Cliente Novo do Insomnia",\n  "cpf": "{{ _.novo_cpf_mascara }}",\n  "email": "{{ _.novo_email }}",\n' +
          '  "telefone": "(11) 99999-8888",\n  "cep": "06020-010",\n  "numero": "123",\n  "complemento": "Apto 45",\n' +
          '  "senha": "{{ _.novo_senha }}",\n  "aceiteTermos": true\n}',
        depois: [
          guardaSe(201, 'insomnia.environment.set("novo_cliente_id", insomnia.response.json().id);'),
          'const c = insomnia.response.code === 201 ? insomnia.response.json() : null;',
          'insomnia.test("ViaCEP preencheu a cidade", () => insomnia.expect(c.endereco.cidade).to.equal("Osasco"));',
          'insomnia.test("CEP higienizado", () => insomnia.expect(c.endereco.cep.replace(/\\D/g, "")).to.equal("06020010"));',
          'insomnia.test("CPF mascarado na resposta", () => insomnia.expect(c.cpf).to.match(/^\\*\\*\\*\\.\\d{3}\\.\\d{3}-\\*\\*$/));',
          'insomnia.test("aceite gravado", () => insomnia.expect(c.termosAceitosEm).to.be.ok);',
        ].join("\n"),
        descricao:
          "`aceiteTermos: true` é obrigatório (LGPD, contrato 3.1). Manda CPF, CEP e telefone **com máscara**, como a pessoa digita: a API higieniza (fica só com os dígitos), " +
          "valida o CPF pelos dígitos verificadores e busca o endereço no **ViaCEP** (Osasco/SP). `201` com o cliente. " +
          "A resposta traz o CPF **mascarado** e a versão dos termos aceita. O cadastro **não** devolve token: faça o login.",
      }),
      req("Login · cliente novo", "POST", "/auth/login", {
        corpo: '{\n  "cpf": "{{ _.novo_cpf }}",\n  "senha": "{{ _.novo_senha }}"\n}',
        depois: guardaLogin("token_novo"),
      }),
      req("Abrir conta ORIGEM: corrente", "POST", "/contas", {
        auth: "token_novo",
        corpo: { tipoConta: "CORRENTE" },
        depois: guardaSe(201, 'insomnia.environment.set("conta_nova_id", insomnia.response.json().id);'),
        descricao: "Corpo só com `tipoConta`: o dono vem do **token**, a agência é `0001` e o número (com dígito verificador) é gerado pelo banco.",
      }),
      req("Abrir conta DESTINO: poupança", "POST", "/contas", {
        auth: "token_novo",
        corpo: { tipoConta: "POUPANCA" },
        depois: guardaSe(201, 'insomnia.environment.set("conta_nova_destino_id", insomnia.response.json().id);'),
        descricao: "Um cliente pode ter mais de uma conta. Esta recebe a transferência logo abaixo.",
      }),
      req("Minhas contas (as duas com saldo 0,00)", "GET", "/contas", {
        auth: "token_novo",
        depois: status(200) + '\ninsomnia.test("duas contas", () => insomnia.expect(insomnia.response.json().length).to.equal(2));',
      }),
      req("Depositar R$ 150,00 na ORIGEM", "POST", "/contas/{{ _.conta_nova_id }}/depositos", {
        auth: "token_novo", headers: [CHAVE_NOVA], corpo: { valor: 150.0, descricao: "Deposito pelo Insomnia" }, depois: status(204),
        descricao: "`204` sem corpo. O header `Idempotency-Key` (aba Headers) gera um UUID novo a cada envio.",
      }),
      req("Sacar R$ 50,00 da ORIGEM", "POST", "/contas/{{ _.conta_nova_id }}/saques", {
        auth: "token_novo", headers: [CHAVE_NOVA], corpo: { valor: 50.0, descricao: "Saque pelo Insomnia" }, depois: status(204),
      }),
      req("Transferir R$ 25,00 ORIGEM → DESTINO", "POST", "/transferencias", {
        auth: "token_novo",
        headers: [CHAVE_NOVA],
        corpo:
          '{\n  "contaOrigemId": "{{ _.conta_nova_id }}",\n  "contaDestinoId": "{{ _.conta_nova_destino_id }}",\n' +
          '  "valor": 25.00,\n  "descricao": "Transferencia pelo Insomnia"\n}',
        depois: status(204),
        descricao: "Débito e crédito na mesma transação: ou os dois acontecem, ou nenhum.",
      }),
      req("Saldos: ORIGEM 75,00 · DESTINO 25,00", "GET", "/contas", {
        auth: "token_novo",
        depois: [
          status(200),
          'const contas = insomnia.response.code === 200 ? insomnia.response.json() : [];',
          'const saldo = (id) => Number(contas.find((c) => c.id === insomnia.environment.get(id))?.saldo);',
          'insomnia.test("origem 75", () => insomnia.expect(saldo("conta_nova_id")).to.equal(75));',
          'insomnia.test("destino 25", () => insomnia.expect(saldo("conta_nova_destino_id")).to.equal(25));',
        ].join("\n"),
      }),
      req("Ver só a conta ORIGEM", "GET", "/contas/{{ _.conta_nova_id }}", {
        auth: "token_novo",
        depois: status(200) + '\ninsomnia.test("saldo 75", () => insomnia.expect(Number(insomnia.response.json().saldo)).to.equal(75));',
        descricao: "`GET /contas/{contaId}` (contrato 3.1): uma conta só, para atualizar o saldo sem buscar a lista inteira.",
      }),
      req("Extrato da ORIGEM (3 lançamentos)", "GET", "/contas/{{ _.conta_nova_id }}/lancamentos", {
        auth: "token_novo",
        depois: status(200) + '\ninsomnia.test("transferência, saque e depósito", () => insomnia.expect(insomnia.response.json().length).to.equal(3));',
        descricao: "Do mais recente para o mais antigo. `valor` sempre positivo: se `contaDestinoId` é esta conta, é entrada.",
      }),
      req("Extrato do DESTINO (1 lançamento)", "GET", "/contas/{{ _.conta_nova_destino_id }}/lancamentos", {
        auth: "token_novo",
        depois: status(200) + '\ninsomnia.test("recebeu a transferência", () => insomnia.expect(insomnia.response.json()[0].tipoLancamento).to.equal("TRANSFERENCIA"));',
      }),
    ],
  },
  {
    nome: "03 · Dinheiro entre clientes (contas prontas do seed)",
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
        depois: [status(422), codigo("IDEMPOTENCIA_CHAVE_REUSADA")].join("\n"),
      }),
      req("Sem a chave → 400", "POST", "/contas/{{ _.conta_origem_id }}/depositos", {
        auth: "token_origem", corpo: { valor: 10.0 }, depois: status(400),
      }),
    ],
  },
  {
    nome: "05 · ADMIN: gestão de clientes, contas e usuários",
    descricao:
      "Tudo o que o ADMIN faz e tudo o que ele **não** pode fazer. Precisa do **Login · ADMIN** (pasta 01) e do " +
      "**cliente novo** (pasta 02): é nele que o ADMIN mexe, para não estragar as contas fixas. No perfil visitante não " +
      "há ADMIN: tudo aqui dá `401` (use o ambiente Equipe com a API no perfil `equipe` ou MySQL).",
    itens: [
      req("Listar clientes (array)", "GET", "/clientes", {
        auth: "token_admin", depois: status(200),
        descricao: "Só ADMIN. Sem `page`/`size` vem um array com todos; com eles, uma página.",
      }),
      req("Listar clientes (paginado)", "GET", "/clientes?page=0&size=10", {
        auth: "token_admin",
        depois: status(200) + '\ninsomnia.test("é página", () => insomnia.expect(insomnia.response.json().content).to.be.an("array"));',
      }),
      req("Listar TODAS as contas (paginado)", "GET", "/contas?page=0&size=20", {
        auth: "token_admin", depois: status(200),
        descricao: "A mesma rota do cliente, mas o ADMIN recebe as contas de todo mundo.",
      }),
      req("Listar usuários (guarda o id do cliente novo)", "GET", "/admin/usuarios", {
        auth: "token_admin",
        depois: [
          status(200),
          'const usuarios = insomnia.response.code === 200 ? insomnia.response.json() : [];',
          'const alvo = usuarios.find((u) => u.clienteId === insomnia.environment.get("novo_cliente_id"));',
          'if (alvo) insomnia.environment.set("usuario_novo_id", alvo.id);',
          'insomnia.test("achou o cliente novo", () => insomnia.expect(alvo).to.be.ok);',
          'insomnia.test("CPF mascarado", () => insomnia.expect(usuarios[0].cpf).to.include("***"));',
        ].join("\n"),
        descricao: "CPF vem mascarado. O `id` daqui (tb_usuario) é o que vai nas rotas abaixo, **não** o `clienteId`.",
      }),
      req("Promover o cliente novo a ADMIN", "PATCH", "/admin/usuarios/{{ _.usuario_novo_id }}/perfil", {
        auth: "token_admin", corpo: { perfil: "ADMIN" },
        depois: status(200) + '\ninsomnia.test("virou ADMIN", () => insomnia.expect(insomnia.response.json().perfil).to.equal("ADMIN"));',
        descricao: "Vale na próxima requisição: o token antigo do promovido passa a dar `401`.",
      }),
      req("Token antigo do promovido → 401", "GET", "/contas", {
        auth: "token_novo", depois: [status(401), codigo("ACESSO_ALTERADO")].join("\n"),
      }),
      req("Login do promovido (já vem ADMIN)", "POST", "/auth/login", {
        corpo: '{\n  "cpf": "{{ _.novo_cpf }}",\n  "senha": "{{ _.novo_senha }}"\n}',
        depois: guardaLogin("token_novo") + '\ninsomnia.test("perfil ADMIN", () => insomnia.expect(insomnia.response.json().perfil).to.equal("ADMIN"));',
      }),
      req("Promovido lista clientes → 200", "GET", "/clientes?page=0&size=5", {
        auth: "token_novo", depois: status(200),
        descricao: "Agora ele é ADMIN e continua dono das próprias contas (o cadastro de cliente não some).",
      }),
      req("Promovido tenta rebaixar o ADMIN do .env → 422", "PATCH", "/admin/usuarios/{{ _.usuario_admin_id }}/perfil", {
        auth: "token_novo", corpo: { perfil: "CLIENTE" },
        depois: [status(422), codigo("PERFIL_CLIENTE_SEM_CADASTRO")].join("\n"),
        descricao: "O ADMIN do `.env` não é cliente: virar CLIENTE não faz sentido. Para tirá-lo, desative o acesso.",
      }),
      req("Rebaixar de volta a CLIENTE", "PATCH", "/admin/usuarios/{{ _.usuario_novo_id }}/perfil", {
        auth: "token_admin", corpo: { perfil: "CLIENTE" }, depois: status(200),
      }),
      req("Desativar o acesso", "PATCH", "/admin/usuarios/{{ _.usuario_novo_id }}/acesso", {
        auth: "token_admin", corpo: { ativo: false },
        depois: status(200) + '\ninsomnia.test("ativo false", () => insomnia.expect(insomnia.response.json().ativo).to.equal(false));',
        descricao: "\"Remover\" = desativar: o usuário nunca é apagado (o histórico de movimentações aponta para ele).",
      }),
      req("Login do desativado → 401", "POST", "/auth/login", {
        corpo: '{\n  "cpf": "{{ _.novo_cpf }}",\n  "senha": "{{ _.novo_senha }}"\n}',
        depois: [status(401), codigo("CREDENCIAIS_INVALIDAS")].join("\n"),
      }),
      req("Reativar o acesso", "PATCH", "/admin/usuarios/{{ _.usuario_novo_id }}/acesso", {
        auth: "token_admin", corpo: { ativo: true }, depois: status(200),
      }),
      req("Redefinir a senha (quem esqueceu)", "PATCH", "/admin/usuarios/{{ _.usuario_novo_id }}/senha", {
        auth: "token_admin",
        antes: 'insomnia.environment.set("senha_redefinida", "redefinida-" + Date.now().toString(36));',
        corpo: '{\n  "novaSenha": "{{ _.senha_redefinida }}"\n}',
        depois: guardaSe(204, 'insomnia.environment.set("novo_senha", insomnia.environment.get("senha_redefinida"));'),
        descricao: "O script passa a senha nova para `novo_senha`. Ela é **temporária** (contrato 3.1): no próximo login a pessoa precisa trocá-la.",
      }),
      req("Login com a senha temporária (deveTrocarSenha)", "POST", "/auth/login", {
        corpo: '{\n  "cpf": "{{ _.novo_cpf }}",\n  "senha": "{{ _.novo_senha }}"\n}',
        depois: guardaLogin("token_novo") + '\ninsomnia.test("deveTrocarSenha", () => insomnia.expect(insomnia.response.json().deveTrocarSenha).to.equal(true));',
      }),
      req("Senha temporária em outra rota → 403", "GET", "/contas", {
        auth: "token_novo", depois: [status(403), codigo("TROCA_DE_SENHA_OBRIGATORIA")].join("\n"),
        descricao: "Com a senha temporária, o token só abre `PATCH /usuarios/me/senha`.",
      }),
      req("Trocar a senha temporária", "PATCH", "/usuarios/me/senha", {
        auth: "token_novo",
        antes: 'insomnia.environment.set("senha_escolhida", "escolhida-" + Date.now().toString(36));',
        corpo: '{\n  "senhaAtual": "{{ _.novo_senha }}",\n  "novaSenha": "{{ _.senha_escolhida }}"\n}',
        depois: guardaSe(204, 'insomnia.environment.set("novo_senha", insomnia.environment.get("senha_escolhida"));'),
      }),
      req("Login · cliente novo (de novo, senha escolhida)", "POST", "/auth/login", {
        corpo: '{\n  "cpf": "{{ _.novo_cpf }}",\n  "senha": "{{ _.novo_senha }}"\n}',
        depois: guardaLogin("token_novo") + '\ninsomnia.test("sem troca pendente", () => insomnia.expect(insomnia.response.json().deveTrocarSenha).to.equal(false));',
      }),
      req("Trava: ADMIN mexe no próprio perfil → 422", "PATCH", "/admin/usuarios/{{ _.usuario_admin_id }}/perfil", {
        auth: "token_admin", corpo: { perfil: "ADMIN" },
        depois: [status(422), codigo("PROPRIO_ACESSO")].join("\n"),
        descricao: "Ninguém mexe em si mesmo: o ADMIN não se tranca para fora e sempre sobra um ADMIN ativo.",
      }),
      req("Trava: ADMIN redefine a própria senha pela gestão → 422", "PATCH", "/admin/usuarios/{{ _.usuario_admin_id }}/senha", {
        auth: "token_admin", corpo: { novaSenha: "nao-vai-mudar-123" },
        depois: [status(422), codigo("PROPRIA_SENHA_PELA_GESTAO")].join("\n"),
        descricao: "A própria senha se troca em `PATCH /usuarios/me/senha`, que exige a atual (um token roubado não basta).",
      }),
      req("Perfil que não existe → 400", "PATCH", "/admin/usuarios/{{ _.usuario_novo_id }}/perfil", {
        auth: "token_admin", corpo: { perfil: "GERENTE" }, depois: status(400),
      }),
      req("Usuário que não existe → 404", "PATCH", "/admin/usuarios/00000000-0000-4000-8000-000000000000/perfil", {
        auth: "token_admin", corpo: { perfil: "ADMIN" }, depois: [status(404), codigo("USUARIO_NAO_ENCONTRADO")].join("\n"),
      }),
      req("Acesso sem o campo → 400", "PATCH", "/admin/usuarios/{{ _.usuario_novo_id }}/acesso", {
        auth: "token_admin", corpo: {}, depois: status(400),
      }),
      req("Senha curta na redefinição → 400", "PATCH", "/admin/usuarios/{{ _.usuario_novo_id }}/senha", {
        auth: "token_admin", corpo: { novaSenha: "curta" }, depois: status(400),
      }),
      req("ADMIN do .env abre conta → 422", "POST", "/contas", {
        auth: "token_admin", corpo: { tipoConta: "CORRENTE" },
        depois: [status(422), codigo("SEM_CADASTRO_CLIENTE")].join("\n"),
      }),
      req("ADMIN do .env em /clientes/me → 422", "GET", "/clientes/me", {
        auth: "token_admin", depois: [status(422), codigo("SEM_CADASTRO_CLIENTE")].join("\n"),
      }),
      req("ADMIN deposita na conta de um cliente → 403", "POST", "/contas/{{ _.conta_origem_id }}/depositos", {
        auth: "token_admin", headers: [CHAVE_NOVA], corpo: { valor: 1.0 },
        depois: [status(403), codigo("CONTA_DE_OUTRO_CLIENTE")].join("\n"),
        descricao: "O ADMIN **vê** todas as contas, mas não **mexe** no dinheiro dos outros: a regra de dono vale para todos.",
      }),
      req("ADMIN vê o extrato de um cliente → 403", "GET", "/contas/{{ _.conta_origem_id }}/lancamentos", {
        auth: "token_admin", depois: status(403),
      }),
      req("ADMIN vê uma conta de cliente → 200", "GET", "/contas/{{ _.conta_origem_id }}", {
        auth: "token_admin", depois: status(200),
        descricao: "Ver pode (como em `GET /contas`); movimentar e ver o extrato, não.",
      }),
      req("Conciliação: saldo × extrato", "GET", "/admin/conciliacao", {
        auth: "token_admin",
        depois: status(200) + '\ninsomnia.test("nenhuma conta divergente", () => insomnia.expect(insomnia.response.json()).to.have.length(0));',
        descricao: "Contas cujo saldo não bate com a soma do extrato. Lista vazia é o normal; qualquer linha é bug ou alteração manual no banco.",
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
        descricao: "Só muda o que vier no corpo. CPF não muda.",
      }),
      req("Mudar de endereço: CEP + número (ORIGEM)", "PATCH", "/clientes/me", {
        auth: "token_origem", corpo: { cep: "01001000", numero: "10" },
        depois: status(200) + '\ninsomnia.test("endereço do ViaCEP", () => insomnia.expect(insomnia.response.json().endereco.numero).to.equal("10"));',
        descricao: "O endereço vem do ViaCEP de novo (precisa de internet). CEP novo exige `numero`.",
      }),
      req("CEP sem número → 400", "PATCH", "/clientes/me", { auth: "token_origem", corpo: { cep: "01001000" }, depois: status(400) }),
      req("Corpo vazio → 400", "PATCH", "/clientes/me", { auth: "token_origem", corpo: {}, depois: status(400) }),
      req("E-mail de outra pessoa → 409", "PATCH", "/clientes/me", {
        auth: "token_origem", corpo: '{\n  "email": "{{ _.email_destino }}"\n}',
        depois: [status(409), codigo("EMAIL_JA_CADASTRADO")].join("\n"),
      }),
      req("Trocar senha com a atual errada → 422", "PATCH", "/usuarios/me/senha", {
        auth: "token_novo", corpo: { senhaAtual: "chute-errado-1", novaSenha: "outra-senha-123" },
        depois: [status(422), codigo("SENHA_ATUAL_INCORRETA")].join("\n"),
      }),
      req("Nova senha igual à atual → 422", "PATCH", "/usuarios/me/senha", {
        auth: "token_novo", corpo: '{\n  "senhaAtual": "{{ _.novo_senha }}",\n  "novaSenha": "{{ _.novo_senha }}"\n}',
        depois: [status(422), codigo("SENHA_REPETIDA")].join("\n"),
      }),
      req("Trocar a própria senha (cliente novo)", "PATCH", "/usuarios/me/senha", {
        auth: "token_novo",
        antes: 'insomnia.environment.set("senha_trocada", "trocada-" + Date.now().toString(36));',
        corpo: '{\n  "senhaAtual": "{{ _.novo_senha }}",\n  "novaSenha": "{{ _.senha_trocada }}"\n}',
        depois: guardaSe(204, 'insomnia.environment.set("novo_senha", insomnia.environment.get("senha_trocada"));'),
        descricao: "Depois do `204`, todo token antigo (inclusive este) dá `401`. O script já guarda a senha nova.",
      }),
      req("Token antigo depois da troca → 401", "GET", "/contas", {
        auth: "token_novo", depois: [status(401), codigo("ACESSO_ALTERADO")].join("\n"),
      }),
      req("Login · cliente novo com a senha nova", "POST", "/auth/login", {
        corpo: '{\n  "cpf": "{{ _.novo_cpf }}",\n  "senha": "{{ _.novo_senha }}"\n}',
        depois: guardaLogin("token_novo"),
      }),
      req("Encerrar com a senha errada → 422", "POST", "/clientes/me/encerramento", {
        auth: "token_novo", corpo: { senha: "chute-errado-2" }, depois: [status(422), codigo("SENHA_INCORRETA")].join("\n"),
      }),
      req("Encerrar com saldo (ORIGEM) → 422", "POST", "/clientes/me/encerramento", {
        auth: "token_origem", corpo: '{\n  "senha": "{{ _.senha_clientes }}"\n}',
        depois: [status(422), codigo("SALDO_NAO_ZERADO")].join("\n"),
        descricao: "Dinheiro não some: com saldo, o encerramento é recusado (e nada muda na conta de origem).",
      }),
      req("Zerar a ORIGEM (saque de R$ 75,00)", "POST", "/contas/{{ _.conta_nova_id }}/saques", {
        auth: "token_novo", headers: [CHAVE_NOVA], corpo: { valor: 75.0, descricao: "Saque antes de encerrar" }, depois: status(204),
        descricao: "Dinheiro não some: antes de encerrar, o titular tira o saldo das contas (as da pasta 02).",
      }),
      req("Zerar o DESTINO (saque de R$ 25,00)", "POST", "/contas/{{ _.conta_nova_destino_id }}/saques", {
        auth: "token_novo", headers: [CHAVE_NOVA], corpo: { valor: 25.0, descricao: "Saque antes de encerrar" }, depois: status(204),
      }),
      req("⚠️ Encerrar o cadastro (cliente novo)", "POST", "/clientes/me/encerramento", {
        auth: "token_novo", corpo: '{\n  "senha": "{{ _.novo_senha }}"\n}',
        depois: status(200) + '\ninsomnia.test("prazo de anonimização", () => insomnia.expect(insomnia.response.json().anonimizacaoPrevistaPara).to.be.ok);',
        descricao:
          "Exige saldo zero em todas as contas (as do cliente novo estão zeradas). Contas ficam inativas, o login é " +
          "desativado e os dados são anonimizados depois de 5 anos. Para repetir, cadastre outro cliente novo (pasta 02).",
      }),
      req("Token do encerrado → 401", "GET", "/clientes/me", { auth: "token_novo", depois: status(401) }),
      req("Login do encerrado → 401", "POST", "/auth/login", {
        corpo: '{\n  "cpf": "{{ _.novo_cpf }}",\n  "senha": "{{ _.novo_senha }}"\n}', depois: status(401),
      }),
      req("ADMIN tenta reativar o encerrado → 422", "PATCH", "/admin/usuarios/{{ _.usuario_novo_id }}/acesso", {
        auth: "token_admin", corpo: { ativo: true },
        depois: [status(422), codigo("CADASTRO_ENCERRADO")].join("\n"),
        descricao: "Só com ADMIN (ambiente Equipe). Quem encerrou não volta pelas mãos do ADMIN: a decisão é do titular.",
      }),
    ],
  },
  {
    nome: "07 · Erros que DEVEM acontecer",
    descricao: "Cada uma testa uma proteção. Se alguma passar com 2xx, achamos um bug.",
    itens: [
      req("Sem token → 401", "GET", "/contas", { depois: [status(401), codigo("AUTENTICACAO_NECESSARIA")].join("\n") }),
      req("Token adulterado → 401", "GET", "/contas", {
        auth: "token_falso", depois: [status(401), codigo("TOKEN_INVALIDO")].join("\n"),
        descricao: "Um JWT com payload trocado e assinatura que não bate: o Resource Server recusa.",
      }),
      req("Senha errada no login → 401", "POST", "/auth/login", {
        corpo: { cpf: "39053344705", senha: "errada-123" }, depois: [status(401), codigo("CREDENCIAIS_INVALIDAS")].join("\n"),
        descricao: "CPF sem cadastro e senha errada dão a MESMA resposta: não dá para descobrir quem é cliente. " +
          "5 erros seguidos no mesmo CPF → `429` (mande esta 6 vezes para ver; o CPF fica 15 min bloqueado).",
      }),
      req("CLIENTE em rota de ADMIN → 403", "GET", "/clientes", {
        auth: "token_origem", depois: [status(403), codigo("PERFIL_SEM_PERMISSAO")].join("\n"),
      }),
      req("CLIENTE na gestão de usuários → 403", "GET", "/admin/usuarios", { auth: "token_origem", depois: status(403) }),
      req("Depositar na conta de outro cliente → 403", "POST", "/contas/{{ _.conta_origem_id }}/depositos", {
        auth: "token_destino", headers: [CHAVE_NOVA], corpo: { valor: 1.0 },
        depois: [status(403), codigo("CONTA_DE_OUTRO_CLIENTE")].join("\n"),
      }),
      req("Ver o extrato de outro cliente → 403", "GET", "/contas/{{ _.conta_origem_id }}/lancamentos", {
        auth: "token_destino", depois: status(403),
      }),
      req("Ver a conta de outro cliente → 403", "GET", "/contas/{{ _.conta_origem_id }}", {
        auth: "token_destino", depois: [status(403), codigo("CONTA_DE_OUTRO_CLIENTE")].join("\n"),
      }),
      req("CLIENTE na conciliação → 403", "GET", "/admin/conciliacao", { auth: "token_origem", depois: status(403) }),
      req("Sacar mais que o saldo → 422", "POST", "/contas/{{ _.conta_origem_id }}/saques", {
        auth: "token_origem", headers: [CHAVE_NOVA], corpo: { valor: 999999.0 },
        depois: [status(422), codigo("SALDO_INSUFICIENTE")].join("\n"),
      }),
      req("Transferir mais que o saldo → 422", "POST", "/transferencias", {
        auth: "token_origem", headers: [CHAVE_NOVA],
        corpo: '{\n  "contaOrigemId": "{{ _.conta_origem_id }}",\n  "contaDestinoId": "{{ _.conta_destino_id }}",\n  "valor": 999999.00\n}',
        depois: [status(422), codigo("SALDO_INSUFICIENTE")].join("\n"),
      }),
      req("Transferir para a mesma conta → 422", "POST", "/transferencias", {
        auth: "token_origem", headers: [CHAVE_NOVA],
        corpo: '{\n  "contaOrigemId": "{{ _.conta_origem_id }}",\n  "contaDestinoId": "{{ _.conta_origem_id }}",\n  "valor": 1.00\n}',
        depois: [status(422), codigo("MESMA_CONTA")].join("\n"),
      }),
      req("Conta que não existe → 404", "GET", "/contas/00000000-0000-4000-8000-000000000000/lancamentos", {
        auth: "token_origem", depois: status(404),
      }),
      req("Ver conta que não existe → 404", "GET", "/contas/00000000-0000-4000-8000-000000000000", {
        auth: "token_origem", depois: [status(404), codigo("CONTA_NAO_ENCONTRADA")].join("\n"),
      }),
      req("Id que não é UUID → 400", "GET", "/contas/abc/lancamentos", { auth: "token_origem", depois: status(400) }),
      req("Paginação inválida → 400", "GET", "/contas?size=0", { auth: "token_origem", depois: status(400) }),
      req("Tipo de conta inválido → 400", "POST", "/contas", {
        auth: "token_origem", corpo: { tipoConta: "SALARIO" }, depois: status(400),
      }),
      req("JSON quebrado → 400", "POST", "/contas/{{ _.conta_origem_id }}/depositos", {
        auth: "token_origem", headers: [CHAVE_NOVA], corpo: '{ "valor": ', depois: status(400),
      }),
      req("Valor com 3 casas → 400", "POST", "/contas/{{ _.conta_origem_id }}/depositos", {
        auth: "token_origem", headers: [CHAVE_NOVA], corpo: '{\n  "valor": 10.001\n}', depois: status(400),
      }),
      req("Cadastro inválido → 400 com os campos", "POST", "/clientes", {
        corpo: { nomeCompleto: "", cpf: "11111111111", email: "nao-e-email", telefone: "", cep: "", numero: "", senha: "curta", aceiteTermos: true },
        depois: status(400) + '\ninsomnia.test("lista os campos", () => insomnia.expect(insomnia.response.json().campos.length).to.be.above(0));',
      }),
      req("CPF já cadastrado → 409", "POST", "/clientes", {
        corpo:
          '{\n  "nomeCompleto": "CPF Repetido",\n  "cpf": "{{ _.cpf_origem }}",\n  "email": "repetido.{% uuid \'v4\' %}@exemplo.test",\n' +
          '  "telefone": "11999998888",\n  "cep": "01001000",\n  "numero": "1",\n' + ACEITE,
        depois: [status(409), codigo("CPF_JA_CADASTRADO")].join("\n"),
      }),
      req("Cadastro sem aceitar os termos → 400", "POST", "/clientes", {
        antes: SORTEIA_CPF_EXTRA,
        corpo:
          '{\n  "nomeCompleto": "Sem Aceite",\n  "cpf": "{{ _.cpf_extra }}",\n  "email": "aceite.{% uuid \'v4\' %}@exemplo.test",\n' +
          '  "telefone": "11999998888",\n  "cep": "01001000",\n  "numero": "1",\n  "senha": "senha-forte-1",\n  "aceiteTermos": false\n}',
        depois: [status(400), codigo("CAMPOS_INVALIDOS"), 'insomnia.test("campo aceiteTermos", () => insomnia.expect(insomnia.response.json().campos[0].campo).to.equal("aceiteTermos"));'].join("\n"),
        descricao: "LGPD (contrato 3.1): sem o aceite dos Termos de Uso e da Política de Privacidade, o cadastro nem chega ao banco.",
      }),
      req("Cadastro com termos antigos → 422", "POST", "/clientes", {
        antes: SORTEIA_CPF_EXTRA,
        corpo:
          '{\n  "nomeCompleto": "Termos Antigos",\n  "cpf": "{{ _.cpf_extra }}",\n  "email": "termos.{% uuid \'v4\' %}@exemplo.test",\n' +
          '  "telefone": "11999998888",\n  "cep": "01001000",\n  "numero": "1",\n  "senha": "senha-forte-1",\n  "aceiteTermos": true,\n  "termosVersao": "2000-01-01"\n}',
        depois: [status(422), codigo("TERMOS_DESATUALIZADOS")].join("\n"),
        descricao: "A pessoa aceitou uma versão dos termos que não vale mais (página aberta antes de eles mudarem): recarregar e aceitar de novo.",
      }),
      req("CEP que não existe → 422", "POST", "/clientes", {
        antes: SORTEIA_CPF_EXTRA,
        corpo:
          '{\n  "nomeCompleto": "CEP Inexistente",\n  "cpf": "{{ _.cpf_extra }}",\n  "email": "cep.{% uuid \'v4\' %}@exemplo.test",\n' +
          '  "telefone": "11999998888",\n  "cep": "99999999",\n  "numero": "1",\n' + ACEITE,
        depois: [status(422), codigo("CEP_NAO_ENCONTRADO")].join("\n"),
        descricao: "O endereço vem do ViaCEP: CEP inexistente → `422`; ViaCEP fora do ar → `503`.",
      }),
    ],
  },
  {
    nome: "08 · CORS (navegador chamando a API direto)",
    descricao:
      "O site de hoje passa pelo proxy do Next e não usa CORS. Isto vale para um navegador chamando a API direto. " +
      "As origens permitidas vêm de `CORS_ORIGENS` (padrão `http://localhost:3000`).",
    itens: [
      req("Preflight de origem permitida → 200", "OPTIONS", "/transferencias", {
        headers: [
          { name: "Origin", value: "http://localhost:3000" },
          { name: "Access-Control-Request-Method", value: "POST" },
          { name: "Access-Control-Request-Headers", value: "authorization,content-type,idempotency-key" },
        ],
        depois: status(200) + '\ninsomnia.test("Allow-Origin", () => insomnia.expect(insomnia.response.headers.get("Access-Control-Allow-Origin")).to.equal("http://localhost:3000"));',
      }),
      req("Preflight de origem estranha → 403", "OPTIONS", "/transferencias", {
        headers: [
          { name: "Origin", value: "https://site-estranho.example" },
          { name: "Access-Control-Request-Method", value: "POST" },
        ],
        depois: status(403),
      }),
    ],
  },
  {
    nome: "09 · Sessão: renovar e sair (refresh token)",
    descricao:
      "Contrato 3.1. O login devolve, além do token de acesso (15 min), um **token de renovação** (7 dias) que troca o par " +
      "sem pedir a senha. Ele é de **uso único**: usar de novo um já trocado é sinal de roubo e derruba todas as sessões da " +
      "pessoa. Mande na ordem; usa o cliente DESTINO, e no fim ele entra de novo para as outras pastas continuarem.",
    itens: [
      req("Login · cliente DESTINO", "POST", "/auth/login", {
        corpo: '{\n  "cpf": "{{ _.cpf_destino }}",\n  "senha": "{{ _.senha_clientes }}"\n}',
        depois: guardaLogin("token_destino") + '\nif (insomnia.response.code === 200) insomnia.environment.set("refresh_usado", insomnia.response.json().refreshToken);',
      }),
      req("Renovar: troca o par", "POST", "/auth/refresh", {
        corpo: '{\n  "refreshToken": "{{ _.refresh_destino }}"\n}',
        depois: [
          guardaLogin("token_destino"),
          'insomnia.test("token de renovação novo", () => insomnia.expect(insomnia.response.json().refreshToken).to.not.equal(insomnia.environment.get("refresh_usado")));',
        ].join("\n"),
        descricao: "Público: o token de renovação vai no corpo (o de acesso pode já ter vencido). Mesmo formato do login.",
      }),
      req("O token novo funciona", "GET", "/contas", { auth: "token_destino", depois: status(200) }),
      req("Reusar o token já trocado → 401", "POST", "/auth/refresh", {
        corpo: '{\n  "refreshToken": "{{ _.refresh_usado }}"\n}',
        depois: [status(401), codigo("SESSAO_EXPIRADA")].join("\n"),
        descricao: "Reuso = alguém copiou o token. A API derruba **todas** as renovações da pessoa, inclusive a legítima.",
      }),
      req("Depois do reuso, a renovação legítima também caiu → 401", "POST", "/auth/refresh", {
        corpo: '{\n  "refreshToken": "{{ _.refresh_destino }}"\n}',
        depois: [status(401), codigo("SESSAO_EXPIRADA")].join("\n"),
      }),
      req("Login · cliente DESTINO (de novo)", "POST", "/auth/login", {
        corpo: '{\n  "cpf": "{{ _.cpf_destino }}",\n  "senha": "{{ _.senha_clientes }}"\n}',
        depois: guardaLogin("token_destino") + '\nif (insomnia.response.code === 200) insomnia.environment.set("refresh_usado", insomnia.response.json().refreshToken);',
      }),
      req("Sair (logout)", "POST", "/auth/logout", {
        corpo: '{\n  "refreshToken": "{{ _.refresh_usado }}"\n}', depois: status(204),
        descricao: "Revoga o token de renovação. O de acesso continua valendo até vencer (no máximo 15 min): o front o apaga.",
      }),
      req("Renovar depois de sair → 401", "POST", "/auth/refresh", {
        corpo: '{\n  "refreshToken": "{{ _.refresh_usado }}"\n}', depois: [status(401), codigo("SESSAO_EXPIRADA")].join("\n"),
      }),
      req("Sair com token desconhecido → 204", "POST", "/auth/logout", {
        corpo: { refreshToken: "token-que-nunca-existiu" }, depois: status(204),
        descricao: "Sair nunca falha (nem revela se o token existia).",
      }),
      req("Renovar sem o token → 400", "POST", "/auth/refresh", { corpo: {}, depois: status(400) }),
      req("Login · cliente DESTINO (para as outras pastas)", "POST", "/auth/login", {
        corpo: '{\n  "cpf": "{{ _.cpf_destino }}",\n  "senha": "{{ _.senha_clientes }}"\n}',
        depois: guardaLogin("token_destino", "cliente_destino_id"),
      }),
    ],
  },
];

// ── Ambientes ─────────────────────────────────────────────────────────────────────────────────────────────────

// Variáveis que os scripts preenchem: começam vazias e aparecem no ambiente para quem quiser olhar.
const PREENCHIDAS_PELOS_SCRIPTS = {
  token_origem: "", token_destino: "", token_admin: "", token_novo: "",
  refresh_origem: "", refresh_destino: "", refresh_admin: "", refresh_novo: "", refresh_usado: "", termos_versao: "",
  cliente_origem_id: "", cliente_destino_id: "",
  novo_cpf: "", novo_email: "", novo_senha: "", novo_cliente_id: "", conta_nova_id: "", conta_nova_destino_id: "", usuario_novo_id: "",
  chave_fixa: "", novo_cpf_mascara: "", senha_redefinida: "", senha_trocada: "", senha_escolhida: "", usuario_admin_id: "", cpf_extra: "",
  // JWT de mentira (payload com perfil ADMIN e assinatura falsa) para o teste "Token adulterado".
  token_falso: "eyJhbGciOiJIUzI1NiJ9.eyJzdWIiOiJxdWFscXVlci11bSIsInBlcmZpbCI6IkFETUlOIn0.assinatura-que-nao-bate",
};

function ambientes({ equipeSenha, adminCpf, adminSenha }) {
  return {
    base: { base_url: "http://localhost:8080", ...PREENCHIDAS_PELOS_SCRIPTS },
    visitante: {
      // Contas do db/seed/visitante (perfil visitante, H2). Senha pública: está no VISITANTES.md.
      cpf_origem: "24681357928", cpf_destino: "97531864282", senha_clientes: "Visitante@2026",
      email_destino: "bruno.visitante@exemplo.test",
      conta_origem_id: "a0000000-0000-4000-8000-000000000114",
      conta_destino_id: "a0000000-0000-4000-8000-000000000124",
      admin_cpf: "", admin_senha: "",
    },
    equipe: {
      // Contas do db/seed/equipe (perfil padrão MySQL e perfil equipe H2). Senhas: só no .env de cada um.
      cpf_origem: "12345678909", cpf_destino: "98765432100", senha_clientes: equipeSenha,
      email_destino: "renan.barros@rkfbank.test",
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
      _id: "env_rkfbank_equipe", _type: "environment", parentId: "env_rkfbank_base", name: "Equipe (MySQL ou H2, Fellipe e Renan)",
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

// ── Exportação v5 (YAML), o formato que o Insomnia 10+ exporta e importa ─────────────────────────────────────

// YAML sem biblioteca: texto de várias linhas vira bloco "|-"; o resto vai entre aspas duplas (JSON é YAML válido).
function yaml(valor, nivel = 0) {
  const pad = "  ".repeat(nivel);
  if (Array.isArray(valor)) {
    if (!valor.length) return " []";
    return "\n" + valor.map((item) => `${pad}-${yaml(item, nivel + 1).replace(/^\n\s*/, " ")}`).join("\n");
  }
  if (valor && typeof valor === "object") {
    const entradas = Object.entries(valor).filter(([, v]) => v !== undefined);
    if (!entradas.length) return " {}";
    return "\n" + entradas.map(([k, v]) => `${pad}${k}:${yaml(v, nivel + 1)}`).join("\n");
  }
  if (typeof valor === "string" && valor.includes("\n")) {
    return " |-\n" + valor.split("\n").map((linha) => (linha ? `${pad}${linha}` : "")).join("\n");
  }
  return " " + JSON.stringify(valor ?? "");
}

function montarV5(valores) {
  const amb = ambientes(valores);
  const agora = Date.now();
  const meta = (id, extra = {}) => ({ id, created: agora, modified: agora, ...extra });
  const pastas = PASTAS.map((pasta, p) => ({
    name: pasta.nome,
    meta: meta(`fld_rkfbank_${String(p).padStart(2, "0")}`, { sortKey: p, description: pasta.descricao }),
    children: pasta.itens.map((r, i) => ({
      url: `{{ _.base_url }}${r.caminho}`,
      name: r.nome,
      meta: meta(`req_rkfbank_${String(p).padStart(2, "0")}_${String(i).padStart(2, "0")}`, {
        isPrivate: false, description: r.descricao, sortKey: i,
      }),
      method: r.metodo,
      body: r.corpo === undefined ? undefined : { mimeType: "application/json", text: r.corpo },
      headers: r.headers.length ? r.headers : undefined,
      authentication: r.auth,
      scripts: r.antes || r.depois ? { preRequest: r.antes || undefined, afterResponse: r.depois || undefined } : undefined,
      settings: {
        renderRequestBody: true, encodeUrl: true, followRedirects: "global",
        cookies: { send: false, store: false }, rebuildPath: true,
      },
    })),
  }));
  const colecao = {
    type: "collection.insomnia.rest/5.0",
    schema_version: "5.1",
    name: "RKF Bank API",
    meta: meta("wrk_rkfbank", { description: "Coleção gerada por scripts/gerar-colecao-insomnia.mjs. Guia: backend/README_TESTES_E2E.md." }),
    collection: pastas,
    environments: {
      name: "Base Environment",
      meta: meta("env_rkfbank_base", { isPrivate: false }),
      data: amb.base,
      subEnvironments: [
        { name: "Visitante (H2, Ana e Bruno)", meta: meta("env_rkfbank_visitante", { isPrivate: false, sortKey: 1 }), data: amb.visitante, color: "#2e7d32" },
        { name: "Equipe (MySQL ou H2, Fellipe e Renan)", meta: meta("env_rkfbank_equipe", { isPrivate: false, sortKey: 2 }), data: amb.equipe, color: "#1565c0" },
      ],
    },
  };
  return yaml(colecao).replace(/^\n/, "") + "\n";
}

function salvar(caminho, colecao, valores) {
  mkdirSync(dirname(caminho), { recursive: true });
  writeFileSync(caminho, JSON.stringify(colecao, null, 2) + "\n");
  const total = colecao.resources.filter((r) => r._type === "request").length;
  console.log(`✅ ${total} requisições → ${caminho}`);
  // A mesma coleção no formato v5 (YAML), ao lado: Import → File aceita os dois.
  const caminhoYaml = caminho.replace(/\.json$/, ".yaml");
  writeFileSync(caminhoYaml, montarV5(valores));
  console.log(`✅ ${total} requisições → ${caminhoYaml}`);
}

// Pública: senhas da equipe como instrução, nunca o valor (o repositório é público).
const PUBLICOS = {
  equipeSenha: "COLE_AQUI_A_EQUIPE_SENHA_DO_SEU_.env",
  adminCpf: "COLE_AQUI_O_ADMIN_CPF_DO_SEU_.env",
  adminSenha: "COLE_AQUI_A_ADMIN_SENHA_DO_SEU_.env",
};
salvar(SAIDA_PUBLICA, montar(PUBLICOS), PUBLICOS);

if (process.argv.includes("--equipe")) {
  const env = lerEnv();
  const daEquipe = { equipeSenha: env.EQUIPE_SENHA ?? "", adminCpf: env.ADMIN_CPF ?? "", adminSenha: env.ADMIN_SENHA ?? "" };
  salvar(SAIDA_EQUIPE, montar(daEquipe), daEquipe);
}
