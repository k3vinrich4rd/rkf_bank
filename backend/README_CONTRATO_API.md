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
| **Versão** | 1.2 (13/10/2026) |

---

## Sumário

1. [Convenções](#1-convenções)
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

---

## 2) Endpoints

| Método | Rota | O que faz | Sucesso | Seção |
|---|---|---|---|---|
| `POST` | `/clientes` | cadastra cliente | `201` + cliente | [3.1](#31-cadastrar-cliente) |
| `GET` | `/clientes` | lista clientes | `200` | [3.2](#32-listar-clientes) |
| `POST` | `/contas/clientes/{clienteId}` | abre conta para o cliente | `201` + conta | [4.1](#41-abrir-conta) |
| `GET` | `/contas` | lista contas | `200` | [4.2](#42-listar-contas) |
| `POST` | `/contas/{contaId}/deposito` | deposita | `204` | [4.3](#43-depositar-e-sacar) |
| `POST` | `/contas/{contaId}/saque` | saca | `204` | [4.3](#43-depositar-e-sacar) |
| `POST` | `/contas/transferencia` | transfere entre contas | `204` | [4.4](#44-transferir) |
| `GET` | `/contas/{contaId}/lancamentos` | extrato da conta | `200` | [4.5](#45-extrato) |

> Ainda **não há autenticação**: qualquer chamada é aceita. Ver [seção 7](#7-próximos-endpoints-propostas).

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
  "complemento": "Apto 45"
}
```

| Campo | Obrigatório | Regra |
|---|:---:|---|
| `nomeCompleto` | ✅ | até 120 caracteres |
| `cpf` | ✅ | CPF válido (dígitos verificadores) e único |
| `email` | ✅ | formato de e-mail válido e único |
| `telefone` | ✅ | não vazio (o backend **não** valida a quantidade de dígitos — o front envia 10 ou 11) |
| `cep` | ✅ | precisa existir no ViaCEP |
| `numero` | ✅ | texto (aceita "S/N") |
| `complemento` | — | até 80 caracteres |

- **Máscaras:** o front envia CPF, telefone e CEP **só com números**. O backend também aceita com máscara —
  ele remove tudo que não é dígito antes de salvar.
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

**Erros possíveis:** `400` (validação), `409` (CPF ou e-mail já cadastrado), `422` (CEP não encontrado),
`503` (ViaCEP indisponível). Detalhes na [seção 6](#6-erros).

### 3.2 Listar clientes

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

---

## 4) Contas e movimentações

### 4.1 Abrir conta

`POST /contas/clientes/{clienteId}`

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

**Erros possíveis:** `400` (validação ou `clienteId` não é UUID), `404` (cliente não encontrado),
`409` (número de conta já existe).

### 4.2 Listar contas

Igual à listagem de clientes: `GET /contas` (array) ou `GET /contas?paginado=true&page=0&size=10` (página).
Cada item tem o formato da resposta da 4.1.

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

**Erros possíveis:** `400` (validação), `404` (conta não encontrada), `409` (conta alterada ao mesmo tempo),
`422` (conta inativa ou, no saque, saldo insuficiente).

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
| `contaOrigemId` | ✅ | UUID de conta existente e ativa |
| `contaDestinoId` | ✅ | UUID de conta existente e ativa, diferente da origem |
| `valor` | ✅ | mínimo `0.01`, no máximo 2 casas decimais |
| `descricao` | — | até 140 caracteres |

**Resposta `204 No Content`.** Débito e crédito acontecem **na mesma transação**: ou os dois acontecem, ou nenhum.

**Erros possíveis:** `400`, `404`, `409`, `422` (mesma conta, conta inativa, saldo insuficiente).

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

**Erros possíveis:** `400` (`contaId` não é UUID), `404` (conta não encontrada), `422` (conta inativa).

---

## 5) Regras de negócio

| # | Regra | Erro |
|---|---|---|
| R1 | CPF não se repete | `409` |
| R2 | E-mail não se repete | `409` |
| R3 | CEP precisa existir no ViaCEP | `422` |
| R4 | Toda conta pertence a um cliente existente | `404` |
| R5 | Número da conta não se repete | `409` |
| R6 | Só conta **ativa** movimenta e tem extrato | `422` |
| R7 | Valor sempre maior que zero | `400` |
| R8 | Saque e transferência precisam de saldo suficiente | `422` |
| R9 | Não dá para transferir para a mesma conta | `422` |
| R10 | Duas operações simultâneas na mesma conta não se sobrescrevem (lock otimista) | `409` |

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
| `POST` | `/auth/login` | login (Spring Security + JWT) | a decidir |

Proposta de resposta do `GET /contas/{contaId}`: mesmo formato da [4.1](#41-abrir-conta).

---

## 8) Histórico de versões

| Versão | Data | Mudança |
|---|---|---|
| 1.2 | 13/10/2026 | tabela de mensagens de erro reais, regras numeradas, erros por endpoint, propostas priorizadas |
| 1.1 | 12/10/2026 | stack, convenções e endpoints revisados |
| — | 11/10/2026 | rotas passam a seguir o backend: sem `/api`, `POST /contas/clientes/{id}` e `POST /contas/transferencia` |
| — | 05/10/2026 | contrato alinhado com o guia de implementação e o fluxo E2E |
| 1.0 | 02/10/2026 | primeira versão do contrato |
