# RKF Bank — Contrato de API

Este arquivo é o **acordo entre backend e frontend**. Ele é a fonte da verdade: se o código e o contrato
discordarem, um dos dois está errado e precisa ser corrigido.

**Regras do contrato**

1. Endpoint novo ou alterado → muda **aqui primeiro**, no mesmo PR do código.
2. No mesmo PR, atualize os tipos em [`frontend/lib/api/types.ts`](../frontend/lib/api/types.ts).
3. Mudança que quebra o frontend (renomear campo, mudar status) → combine antes com o outro dev
   e registre em [planing/decisoes.md](../planing/decisoes.md).
4. Atualize a versão e o histórico no fim do arquivo.

| | |
|---|---|
| **Backend** | Kevin |
| **Frontend** | Renan |
| **Versão** | 2.0 (16/10/2026) — login com JWT; **quebra o frontend** (ver [histórico](#8-histórico-de-versões)) |

---

## Sumário

1. [Convenções](#1-convenções)
   - [Autenticação (JWT)](#autenticação-jwt)
2. [Endpoints](#2-endpoints)
3. [Clientes](#3-clientes)
4. [Contas e movimentações](#4-contas-e-movimentações)
5. [Regras de negócio](#5-regras-de-negócio)
6. [Erros](#6-erros)
7. [Próximos endpoints (propostas)](#7-próximos-endpoints-propostas)
8. [Histórico de versões](#8-histórico-de-versões)

---

## 1) Convenções

| Item | Regra | Exemplo |
|---|---|---|
| Base URL (backend direto) | `http://localhost:8080`, **sem** prefixo `/api` | `http://localhost:8080/clientes` |
| Formato | corpo sempre `application/json` | |
| IDs | UUID em texto | `"1a89861c-70a0-4b7c-947a-d16cdfe2dd08"` |
| Dinheiro | número JSON com até 2 casas decimais (`BigDecimal` no Java) | `150.00` |
| Data/hora | ISO-8601 em UTC | `"2026-10-12T11:19:53.038985Z"` |
| Enums | texto em maiúsculas | `"CORRENTE"`, `"DEPOSITO"` |
| Nomes de campo | `camelCase`, em português | `nomeCompleto`, `contaOrigemId` |

### Como o frontend chama a API

```text
Navegador ──► http://localhost:3000/api/clientes ──(proxy do Next)──► http://localhost:8080/clientes
```

O prefixo `/api` **só existe no frontend** (rewrite em `frontend/next.config.ts`). No Postman, Insomnia ou curl,
chame o backend direto, sem `/api`.

### Autenticação (JWT)

```text
1. POST /clientes   {..., "senha": "..."}       → 201 (cadastro é público)
2. POST /auth/login {"cpf": "...", "senha": "..."} → 200 {"token": "eyJ...", "expiraEmSegundos": 900, ...}
3. Todas as outras chamadas:  Authorization: Bearer eyJ...
```

- O token vale **15 minutos** (`expiraEmSegundos`; o servidor pode mudar com `JWT_EXPIRACAO`). Vencido ou inválido → `401`: mande o usuário para o login de novo.
  Não há *refresh token* (renovação automática) ainda.
- O token é **stateless**: o backend não guarda sessão, então "sair" é só o front apagar o token.
- Não guarde o token em lugar que outro site leia; não coloque o token na URL nem no log.
- O payload do token é só Base64 (não é segredo). Use os campos da **resposta do login**, não decodifique o token.
- **Perfis:** `CLIENTE` (todo cadastro) e `ADMIN` (criado pela configuração do servidor, não por rota).

---

## 2) Endpoints

| Método | Rota | O que faz | Quem pode | Sucesso | Seção |
|---|---|---|---|---|---|
| `POST` | `/clientes` | cadastra cliente (com senha) | **público** | `201` + cliente | [3.1](#31-cadastrar-cliente) |
| `POST` | `/auth/login` | login com CPF + senha | **público** | `200` + token | [3.3](#33-login) |
| `GET` | `/clientes` | lista clientes | só `ADMIN` | `200` | [3.2](#32-listar-clientes) |
| `POST` | `/contas/clientes/{clienteId}` | abre conta para o cliente | o próprio cliente | `201` + conta | [4.1](#41-abrir-conta) |
| `GET` | `/contas` | lista contas | `ADMIN`: todas · `CLIENTE`: só as dele | `200` | [4.2](#42-listar-contas) |
| `POST` | `/contas/{contaId}/deposito` | deposita | dono da conta | `204` | [4.3](#43-depositar-e-sacar) |
| `POST` | `/contas/{contaId}/saque` | saca | dono da conta | `204` | [4.3](#43-depositar-e-sacar) |
| `POST` | `/contas/transferencia` | transfere entre contas | dono da conta de **origem** | `204` | [4.4](#44-transferir) |
| `GET` | `/contas/{contaId}/lancamentos` | extrato da conta | dono da conta | `200` | [4.5](#45-extrato) |

> Fora as duas rotas públicas, **toda chamada sem token válido → `401`**. Token válido, mas recurso de outro
> cliente ou perfil sem acesso → `403`. O `ADMIN` lista, mas não movimenta conta de ninguém.
>
> O Spring Security 7 também publica sozinho `GET /.well-known/oauth-protected-resource` (público, padrão RFC 9728):
> devolve só `{"resource": "<url da API>", "bearer_methods_supported": ["header"], ...}`, e é para ele que aponta o
> `resource_metadata` do header `WWW-Authenticate` no `401`. O frontend não precisa usar.

---

## 3) Clientes

### 3.1 Cadastrar cliente

`POST /clientes`

```json
{
  "nomeCompleto": "Joao Silva",
  "cpf": "52998224725",
  "email": "joao.silva@rkfbank.com",
  "telefone": "11999998888",
  "cep": "01001000",
  "numero": "123",
  "complemento": "Apto 45",
  "senha": "minha-senha-forte"
}
```

| Campo | Obrigatório | Regra |
|---|:---:|---|
| `senha` | ✅ | 8 a 72 caracteres (e no máximo 72 **bytes**: acento conta 2). **Nunca volta na resposta** |
| `nomeCompleto` | ✅ | até 120 caracteres |
| `cpf` | ✅ | CPF válido (dígitos verificadores) e único |
| `email` | ✅ | formato de e-mail válido e único, **sem diferenciar maiúsculas** (`Joao@x.com` = `joao@x.com`) |
| `telefone` | ✅ | não vazio (o backend **não** valida a quantidade de dígitos — o front envia 10 ou 11) |
| `cep` | ✅ | precisa existir no ViaCEP |
| `numero` | ✅ | texto (aceita "S/N") |
| `complemento` | — | até 80 caracteres |

- **Máscaras:** o front envia CPF, telefone e CEP **só com números**. O backend também aceita com máscara —
  ele remove tudo que não é dígito antes de salvar.
- **E-mail:** o backend salva e devolve o e-mail **em minúsculas e sem espaços nas pontas** (`" Joao@X.com "` → `"joao@x.com"`).
  O front pode mostrar o valor da resposta como está; não precisa normalizar antes de enviar.
- **Endereço:** logradouro, bairro, cidade, UF e IBGE vêm do **ViaCEP**, não do usuário.
- **Tempo:** a consulta ao ViaCEP tem timeout de **3 segundos** — mostre um *loading* no botão.

**Resposta `201 Created`:**

```json
{
  "id": "1a89861c-70a0-4b7c-947a-d16cdfe2dd08",
  "nomeCompleto": "Joao Silva",
  "cpf": "529.982.247-25",
  "email": "joao.silva@rkfbank.com",
  "telefone": "(11) 99999-8888",
  "endereco": {
    "cep": "01001-000",
    "logradouro": "Praça da Sé",
    "bairro": "Sé",
    "cidade": "São Paulo",
    "uf": "SP",
    "ibge": "3550308",
    "numero": "123",
    "complemento": "Apto 45"
  }
}
```

Na resposta, **CPF, telefone e CEP já vêm formatados** para exibição. `complemento` pode vir `null`.
O cadastro **não devolve token**: depois do `201`, chame o [login](#33-login).

**Erros possíveis:** `400` (validação), `409` (CPF ou e-mail já cadastrado), `422` (CEP não encontrado),
`503` (ViaCEP indisponível). Detalhes na [seção 6](#6-erros).

### 3.2 Listar clientes

🔒 **Só `ADMIN`.** Com token de `CLIENTE` → `403`.

| Chamada | Retorno |
|---|---|
| `GET /clientes` | **array** com todos os clientes (mesmo formato da 3.1) |
| `GET /clientes?paginado=true&page=0&size=10` | **página** (formato abaixo) |

- `page` começa em `0`; `size` vai de `1` a `100`. Fora disso → `400` com o campo em `campos`.
- `page` e `size` são ignorados quando `paginado` não é `true`.

Formato da página (campos que o frontend usa):

```json
{
  "content": [ /* clientes */ ],
  "totalElements": 2,
  "totalPages": 1,
  "number": 0,
  "size": 10,
  "first": true,
  "last": true
}
```

> ⚠️ Esse formato é o `Page` do Spring serializado direto, que **não é estável** entre versões do Spring Data.
> Está registrado em [débitos técnicos](../planing/debitos-tecnicos.md). Se mudar, este bloco muda junto.

### 3.3 Login

`POST /auth/login` (público)

```json
{
  "cpf": "52998224725",
  "senha": "minha-senha-forte"
}
```

| Campo | Obrigatório | Regra |
|---|:---:|---|
| `cpf` | ✅ | com ou sem máscara (o backend remove o que não é dígito) |
| `senha` | ✅ | até 72 caracteres |

**Resposta `200 OK`:**

```json
{
  "token": "eyJhbGciOiJIUzI1NiJ9.eyJpc3MiOiJya2ZiYW5rIiwic3ViIjoi...",
  "tipo": "Bearer",
  "expiraEmSegundos": 900,
  "clienteId": "1a89861c-70a0-4b7c-947a-d16cdfe2dd08",
  "perfil": "CLIENTE"
}
```

- Envie o token em toda chamada protegida: `Authorization: Bearer <token>`.
- `clienteId` é o que vai na URL de [abrir conta](#41-abrir-conta). Para o `ADMIN` vem `null`.
- Calcule quando o token vence com `expiraEmSegundos` (a partir do momento do login).

**Erros possíveis:** `400` (campo vazio), `401` com `CPF ou senha invalidos`. A mensagem é **a mesma** para CPF sem
cadastro e senha errada, de propósito: não revela quais CPFs têm conta.

---

## 4) Contas e movimentações

🔒 Todas as rotas desta seção exigem `Authorization: Bearer <token>`.

### 4.1 Abrir conta

`POST /contas/clientes/{clienteId}` — o `clienteId` precisa ser **o do usuário logado** (o `clienteId` da resposta do login).

```json
{
  "agencia": "0001",
  "numeroConta": "12345678",
  "tipoContaEnum": "CORRENTE"
}
```

| Campo | Obrigatório | Regra |
|---|:---:|---|
| `agencia` | ✅ | exatamente 4 dígitos |
| `numeroConta` | ✅ | 5 a 12 dígitos, único no banco |
| `tipoContaEnum` | ✅ | `CORRENTE` ou `POUPANCA` |

**Resposta `201 Created`:**

```json
{
  "id": "407d727e-4e43-4c93-b01c-331cbaba42fb",
  "clienteId": "1a89861c-70a0-4b7c-947a-d16cdfe2dd08",
  "agencia": "0001",
  "numeroConta": "12345678",
  "tipoContaEnum": "CORRENTE",
  "saldo": 0.00,
  "ativa": true
}
```

- Toda conta nasce com **saldo zero** e **ativa**.
- Um cliente pode ter **mais de uma conta**.
- Hoje quem escolhe `agencia` e `numeroConta` é quem chama a API (ver [débitos técnicos](../planing/debitos-tecnicos.md)).

**Erros possíveis:** `400` (validação ou `clienteId` não é UUID), `401` (sem token), `403` (`clienteId` de outro
cliente), `404` (cliente não encontrado), `409` (número de conta já existe).

### 4.2 Listar contas

Igual à listagem de clientes: `GET /contas` (array) ou `GET /contas?paginado=true&page=0&size=10` (página).
Cada item tem o formato da resposta da 4.1.

- Token de `CLIENTE` → **só as contas dele** (é o jeito de achar as contas do usuário logado).
- Token de `ADMIN` → todas as contas.

### 4.3 Depositar e sacar

`POST /contas/{contaId}/deposito` · `POST /contas/{contaId}/saque`

```json
{
  "valor": 150.00,
  "descricao": "Deposito inicial"
}
```

| Campo | Obrigatório | Regra |
|---|:---:|---|
| `valor` | ✅ | mínimo `0.01`, no máximo 2 casas decimais |
| `descricao` | — | até 140 caracteres |

**Resposta `204 No Content`** (sem corpo). Para mostrar o novo saldo, busque a conta de novo.

**Erros possíveis:** `400` (validação), `401` (sem token), `403` (conta de outro cliente), `404` (conta não encontrada),
`409` (conta alterada ao mesmo tempo), `422` (conta inativa ou, no saque, saldo insuficiente).
A ordem de checagem é `404` → `403` → `422`: conta de outro cliente dá `403` antes de qualquer regra de saldo.

### 4.4 Transferir

`POST /contas/transferencia`

```json
{
  "contaOrigemId": "407d727e-4e43-4c93-b01c-331cbaba42fb",
  "contaDestinoId": "566dd28f-b08f-41e4-ac55-cc69ecfea4cd",
  "valor": 50.00,
  "descricao": "Transferencia aluguel"
}
```

| Campo | Obrigatório | Regra |
|---|:---:|---|
| `contaOrigemId` | ✅ | UUID de conta existente, ativa e **do usuário logado** |
| `contaDestinoId` | ✅ | UUID de conta existente e ativa, diferente da origem (pode ser de outro cliente) |
| `valor` | ✅ | mínimo `0.01`, no máximo 2 casas decimais |
| `descricao` | — | até 140 caracteres |

**Resposta `204 No Content`.** Débito e crédito acontecem **na mesma transação**: ou os dois acontecem, ou nenhum.

**Erros possíveis:** `400`, `401`, `403` (origem de outro cliente), `404`, `409`, `422` (mesma conta, conta inativa,
saldo insuficiente).

### 4.5 Extrato

`GET /contas/{contaId}/lancamentos`

**Resposta `200 OK`**, do mais recente para o mais antigo:

```json
[
  {
    "id": "59798941-269a-406c-8783-9be456bf1f5c",
    "tipoLancamento": "TRANSFERENCIA",
    "valor": 50.00,
    "contaOrigemId": "407d727e-4e43-4c93-b01c-331cbaba42fb",
    "contaDestinoId": "566dd28f-b08f-41e4-ac55-cc69ecfea4cd",
    "descricao": "Transferencia aluguel",
    "dataHora": "2026-10-12T11:19:53.038985Z"
  },
  {
    "id": "cc56735d-6a5d-4246-8f7b-da161e3a10e4",
    "tipoLancamento": "DEPOSITO",
    "valor": 150.00,
    "contaOrigemId": null,
    "contaDestinoId": "407d727e-4e43-4c93-b01c-331cbaba42fb",
    "descricao": "Deposito inicial",
    "dataHora": "2026-10-12T11:19:52.338068Z"
  }
]
```

| `tipoLancamento` | `contaOrigemId` | `contaDestinoId` |
|---|---|---|
| `DEPOSITO` | `null` | conta que recebeu |
| `SAQUE` | conta que sacou | `null` |
| `TRANSFERENCIA` | quem enviou | quem recebeu |

- `valor` é **sempre positivo**. Para saber se é **entrada** ou **saída** na tela da conta X:
  se `contaDestinoId === X` → entrada (verde); senão → saída (vermelho).
- `descricao` pode vir `null`.
- Conta sem movimentação → `[]`.
- O enum do backend também tem `CREDITO` e `DEBITO`, mas **nenhum endpoint gera esses tipos hoje**.

**Erros possíveis:** `400` (`contaId` não é UUID), `401` (sem token), `403` (conta de outro cliente),
`404` (conta não encontrada), `422` (conta inativa).

---

## 5) Regras de negócio

| # | Regra | Erro |
|---|---|---|
| R1 | CPF não se repete | `409` |
| R2 | E-mail não se repete, sem diferenciar maiúsculas (é salvo em minúsculas) | `409` |
| R3 | CEP precisa existir no ViaCEP | `422` |
| R4 | Toda conta pertence a um cliente existente | `404` |
| R5 | Número da conta não se repete | `409` |
| R6 | Só conta **ativa** movimenta e tem extrato | `422` |
| R7 | Valor sempre maior que zero | `400` |
| R8 | Saque e transferência precisam de saldo suficiente | `422` |
| R9 | Não dá para transferir para a mesma conta | `422` |
| R10 | Duas operações simultâneas na mesma conta não se sobrescrevem (lock otimista) | `409` |
| R11 | Fora `POST /clientes` e `POST /auth/login`, toda rota exige token válido | `401` |
| R12 | Só o dono movimenta, vê extrato e abre conta (na transferência, o dono da **origem**) | `403` |
| R13 | `GET /clientes` é só do `ADMIN`; em `GET /contas` o `CLIENTE` vê só as dele | `403` |
| R14 | Senha guardada só como hash BCrypt; nunca volta em resposta nem aparece em log | — |

---

## 6) Erros

### Formato padrão

Todo erro **tratado** volta assim:

```json
{
  "timestamp": "2026-10-12T11:19:54.090042Z",
  "status": 400,
  "error": "Bad Request",
  "message": "Requisicao invalida",
  "path": "/clientes",
  "campos": [
    { "campo": "cpf", "mensagem": "cpf inválido" }
  ]
}
```

- `campos` vem preenchido em erro de validação; nos outros casos vem `[]`.
- O nome em `campo` é **o mesmo do JSON**, então o front consegue marcar o input certo.
- **No front, decida pelo `status`.** O `message` serve para exibir ao usuário em `409` e `422`.

### Tabela de status

| Status | Quando | `message` (texto real do backend) |
|---|---|---|
| `400` | campo inválido no corpo | `Requisicao invalida` (+ `campos`) |
| `400` | JSON mal formado, tipo errado ou enum desconhecido | `JSON invalido ou tipo de campo incompativel` |
| `400` | ID da URL não é UUID | `Parametro de URL invalido` (+ `campos`) |
| `400` | `page`/`size` fora do limite | `Requisicao invalida` (+ `campos`) |
| `400` | senha acima de 72 bytes (acentos) | `Requisicao invalida` (+ `campos` com `senha`) |
| `401` | sem header `Authorization` | `Autenticacao necessaria: envie o token no header Authorization (Bearer)` |
| `401` | token mal formado, de outra chave ou vencido | `Token invalido ou expirado; faca login novamente` |
| `401` | login com CPF ou senha errados | `CPF ou senha invalidos` |
| `403` | conta (ou `clienteId`) de outro cliente | `Conta nao pertence ao usuario logado` / `Voce so pode abrir conta no seu proprio cadastro` |
| `403` | perfil sem acesso à rota (`CLIENTE` em `GET /clientes`) | `Acesso negado para o seu perfil` |
| `404` | cliente não existe | `Cliente não encontrado` |
| `404` | conta não existe | `Conta nao encontrada` |
| `409` | CPF repetido | `CPF ja cadastrado` |
| `409` | e-mail repetido | `Email ja cadastrado` |
| `409` | número de conta repetido | `Numero de conta ja existe` |
| `409` | conflito de concorrência | `A conta foi alterada por outra operacao; tente novamente` |
| `422` | CEP inexistente | `CEP não encontrado` |
| `422` | conta inativa | `Conta inativa` |
| `422` | saque sem saldo | `Saldo insuficiente` |
| `422` | transferência sem saldo | `Saldo insuficiente para transferencia` |
| `422` | transferência para a mesma conta | `Conta de origem e destino nao podem ser iguais` |
| `503` | ViaCEP fora do ar / timeout | `ViaCEP indisponível` ou `Falha ao consultar ViaCEP` |

> `401` e `403` também vêm no **formato padrão** (com `campos: []`); o `401` traz ainda o header `WWW-Authenticate: Bearer`.
> Rota inexistente sem token responde `401` (o filtro de segurança barra antes de saber que a rota não existe).
>
> Erros **não tratados** (bug → `500`, rota inexistente → `404`) vêm no formato padrão do Spring, **sem** `campos`.
> O `apiRequest` do front já lida com isso (vira `ApiError` com o status HTTP).

---

## 7) Próximos endpoints (propostas)

Ainda **não implementados**. Quando um deles for feito, ele sai daqui e entra nas seções 3/4.

| Método | Rota | Para quê | Prioridade |
|---|---|---|---|
| `GET` | `/contas/{contaId}` | tela de saldo | **MVP** |
| `GET` | `/clientes/{clienteId}/contas` | listar as contas de um cliente | **MVP** |
| `GET` | `/clientes/{clienteId}` | dados do cliente logado | depois do MVP |
| `POST` | `/auth/refresh` | renovar o token sem pedir a senha de novo (*refresh token*) | depois do MVP |

Proposta de resposta do `GET /contas/{contaId}`: mesmo formato da [4.1](#41-abrir-conta).

---

## 8) Histórico de versões

| Versão | Data | Mudança |
|---|---|---|
| 2.0 | 16/10/2026 | **Quebra o frontend.** Login com JWT: `senha` obrigatória no cadastro, `POST /auth/login` novo, `Authorization: Bearer` em todas as outras rotas (`401`), regra de dono (`403`), `GET /clientes` só `ADMIN`, `GET /contas` do `CLIENTE` traz só as dele. Regras R11–R14 |
| 1.3 | 15/10/2026 | e-mail salvo e devolvido em minúsculas; R2 sem diferenciar maiúsculas. Sem mudança de campo ou status (não quebra o frontend) |
| 1.2 | 13/10/2026 | tabela de mensagens de erro reais, regras numeradas, erros por endpoint, propostas priorizadas |
| 1.1 | 12/10/2026 | stack, convenções e endpoints revisados |
| — | 11/10/2026 | rotas passam a seguir o backend: sem `/api`, `POST /contas/clientes/{id}` e `POST /contas/transferencia` |
| — | 05/10/2026 | contrato alinhado com o guia de implementação e o fluxo E2E |
| 1.0 | 02/10/2026 | primeira versão do contrato |
