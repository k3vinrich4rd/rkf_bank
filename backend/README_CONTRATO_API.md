# RKF Bank - Contrato de API (implementado)

## 1) Tecnologias e arquitetura

Tecnologias em uso:

- Java 21 (LTS)
- Spring Boot 4.1.1
- Spring Web MVC
- Spring Data JPA + Hibernate
- Jakarta Validation
- MySQL 8.4 (Docker Compose)
- RestClient para integracao ViaCEP

Arquitetura adotada: **camadas (Layered Architecture)**.

- `controllers`: expoe HTTP e status (`ResponseEntity`)
- `services`: centraliza regra de negocio e transacao
- `repositories`: acesso ao banco via JPA
- `entities`: modelo persistente
- `dto`: contrato de entrada/saida
- `clients/viacep`: integracao externa isolada
- `controllers/handlers`: padrao de erro
- `validation`: validacoes customizadas (CPF)

Por que encaixa no projeto:

- baixo acoplamento entre HTTP, regra e persistencia
- facil evolucao de regras bancarias
- melhor testabilidade por camada

## 2) Convencoes gerais

- Base URL: `http://localhost:8080/api`
- Conteudo: `application/json`
- Valores monetarios: `BigDecimal` (2 casas)
- UUID na API: texto

### UUID no MySQL

- Persistencia: `binary(16)`
- API: continua texto

Workbench:

- leitura: `BIN_TO_UUID(id)`
- filtro: `UUID_TO_BIN('550e8400-e29b-41d4-a716-446655440000')`

## 3) Integracao ViaCEP

Fluxo implementado:

1. `ClienteService` normaliza CEP (somente digitos)
2. chama `ClienteViaCep.buscarPorCep(cep)`
3. `ClienteViaCep` usa `RestClient` com timeout configuravel
4. `erro=true` da API ViaCEP -> `NaoProcessavelException` (`422`)
5. falha 5xx/timeout -> `ServicoExternoException` (`503`)
6. endereco oficial e combinado com `numero`/`complemento` da request

## 4) Endpoints implementados

## 4.1 Clientes

### `POST /api/clientes`

Request (`CadastroClienteRequestDto`):

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

Response `201` (`ClienteResponseDto`):

```json
{
  "id": "d0ca7cf7-3ec4-4d3a-8f77-79d89cf45f9e",
  "nomeCompleto": "Joao Silva",
  "cpf": "52998224725",
  "email": "joao.silva@rkfbank.com",
  "telefone": "11999998888",
  "endereco": {
    "cep": "01001-000",
    "logradouro": "Praca da Se",
    "bairro": "Se",
    "cidade": "Sao Paulo",
    "uf": "SP",
    "ibge": "3550308",
    "numero": "123",
    "complemento": "Apto 45"
  }
}
```

### `GET /api/clientes?paginado=false`

Response `200`: `List<ClienteResponseDto>`

### `GET /api/clientes?paginado=true&page=0&size=10`

Response `200`: `Page<ClienteResponseDto>`

## 4.2 Contas e movimentacoes

### `POST /api/clientes/{clienteId}/contas`

Request (`AbrirContaRequestDto`):

```json
{
  "agencia": "0001",
  "numeroConta": "12345678",
  "tipoContaEnum": "CORRENTE"
}
```

Response `201` (`ContaResponseDto`):

```json
{
  "id": "a4fbcfa4-f93f-4f0e-83ca-0db7bf2fe9f9",
  "clienteId": "d0ca7cf7-3ec4-4d3a-8f77-79d89cf45f9e",
  "agencia": "0001",
  "numeroConta": "12345678",
  "tipoContaEnum": "CORRENTE",
  "saldo": 0.00,
  "ativa": true
}
```

### `GET /api/contas?paginado=false`

Response `200`: `List<ContaResponseDto>`

### `GET /api/contas?paginado=true&page=0&size=10`

Response `200`: `Page<ContaResponseDto>`

### `POST /api/contas/{contaId}/deposito`

Request (`DepositarRequestDto`):

```json
{
  "valor": 150.00,
  "descricao": "Deposito inicial"
}
```

Response `204` (sem corpo)

### `POST /api/contas/{contaId}/saque`

Request (`SaqueRequestDto`):

```json
{
  "valor": 20.00,
  "descricao": "Saque teste"
}
```

Response `204` (sem corpo)

### `POST /api/transferencias`

Request (`TransferirRequestDto`):

```json
{
  "contaOrigemId": "a4fbcfa4-f93f-4f0e-83ca-0db7bf2fe9f9",
  "contaDestinoId": "13c2690e-ef87-4f5b-b0fe-c98c006edc1e",
  "valor": 50.00,
  "descricao": "Transferencia aluguel"
}
```

Response `204` (sem corpo)

### `GET /api/contas/{contaId}/lancamentos`

Response `200` (`List<LancamentoResponseDto>`):

```json
[
  {
    "id": "a4cc0d3e-13ac-4778-a577-60efd4fd92eb",
    "tipoLancamento": "TRANSFERENCIA",
    "valor": 50.00,
    "contaOrigemId": "a4fbcfa4-f93f-4f0e-83ca-0db7bf2fe9f9",
    "contaDestinoId": "13c2690e-ef87-4f5b-b0fe-c98c006edc1e",
    "descricao": "Transferencia aluguel",
    "dataHora": "2026-10-05T15:32:14Z"
  }
]
```

## 5) Regras de negocio implementadas

- CPF unico
- email unico
- numeroConta unico
- conta deve estar ativa para movimentar
- valor deve ser maior que zero
- saque e transferencia exigem saldo
- transferencia exige contas diferentes
- tipo de lancamento:
  - `DEPOSITO`
  - `SAQUE`
  - `TRANSFERENCIA`

## 6) Erros HTTP padrao

Payload (`ErroResponse`):

```json
{
  "timestamp": "2026-10-05T15:40:00Z",
  "status": 400,
  "error": "Bad Request",
  "message": "Requisicao invalida",
  "path": "/api/clientes",
  "campos": [
    { "campo": "cpf", "mensagem": "cpf invalido" }
  ]
}
```

Mapeamento:

- `400`: validacao/request invalida
- `404`: cliente ou conta nao encontrado
- `409`: conflito de negocio (duplicidade)
- `422`: regra nao processavel
- `503`: indisponibilidade/falha ViaCEP
