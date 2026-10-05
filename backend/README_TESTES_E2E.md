# RKF Bank - Testes E2E (versao final)

## 1) Subida do ambiente

```powershell
Set-Location "C:\Users\Estudo$\Documents\rkfbank"
docker compose up -d
.\mvnw.cmd spring-boot:run
```

Base URL:

- `http://localhost:8080/api`

## 2) Ordem real de testes

1. criar cliente origem
2. criar conta origem
3. criar cliente destino
4. criar conta destino
5. depositar
6. sacar
7. transferir
8. listar lancamentos
9. listar clientes (com e sem paginacao)
10. listar contas (com e sem paginacao)

## 3) Requests e responses esperados

## 3.1 Criar cliente origem

`POST /clientes`

Request:

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

Response esperado (`201`):

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

Guarde `id` como `clienteOrigemId`.

## 3.2 Criar conta origem

`POST /clientes/{clienteOrigemId}/contas`

Request:

```json
{
  "agencia": "0001",
  "numeroConta": "12345678",
  "tipoContaEnum": "CORRENTE"
}
```

Response esperado (`201`):

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

Guarde `id` como `contaOrigemId`.

## 3.3 Criar cliente destino

`POST /clientes`

Request:

```json
{
  "nomeCompleto": "Maria Souza",
  "cpf": "16899535009",
  "email": "maria.souza@rkfbank.com",
  "telefone": "11999997777",
  "cep": "30140071",
  "numero": "500",
  "complemento": "Sala 10"
}
```

Response esperado (`201`): mesma estrutura do cliente origem.

Guarde `id` como `clienteDestinoId`.

## 3.4 Criar conta destino

`POST /clientes/{clienteDestinoId}/contas`

Request:

```json
{
  "agencia": "0001",
  "numeroConta": "87654321",
  "tipoContaEnum": "CORRENTE"
}
```

Response esperado (`201`): mesma estrutura da conta origem.

Guarde `id` como `contaDestinoId`.

## 3.5 Depositar

`POST /contas/{contaOrigemId}/deposito`

Request:

```json
{
  "valor": "150.00",
  "descricao": "Deposito inicial"
}
```

Response esperado: `204 No Content` (sem body).

## 3.6 Sacar

`POST /contas/{contaOrigemId}/saque`

Request:

```json
{
  "valor": "20.00",
  "descricao": "Saque teste"
}
```

Response esperado: `204 No Content` (sem body).

## 3.7 Transferir

`POST /transferencias`

Request:

```json
{
  "contaOrigemId": "a4fbcfa4-f93f-4f0e-83ca-0db7bf2fe9f9",
  "contaDestinoId": "13c2690e-ef87-4f5b-b0fe-c98c006edc1e",
  "valor": "50.00",
  "descricao": "Transferencia aluguel"
}
```

Response esperado: `204 No Content` (sem body).

## 3.8 Listar lancamentos da conta

`GET /contas/{contaOrigemId}/lancamentos`

Response esperado (`200`):

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
  },
  {
    "id": "8dc61f72-df0a-4f70-9a2d-fb81c0b5f18d",
    "tipoLancamento": "SAQUE",
    "valor": 20.00,
    "contaOrigemId": "a4fbcfa4-f93f-4f0e-83ca-0db7bf2fe9f9",
    "contaDestinoId": null,
    "descricao": "Saque teste",
    "dataHora": "2026-10-05T15:30:02Z"
  },
  {
    "id": "6f43c817-e362-4fcd-9f23-9475bddf6d60",
    "tipoLancamento": "DEPOSITO",
    "valor": 150.00,
    "contaOrigemId": null,
    "contaDestinoId": "a4fbcfa4-f93f-4f0e-83ca-0db7bf2fe9f9",
    "descricao": "Deposito inicial",
    "dataHora": "2026-10-05T15:28:45Z"
  }
]
```

## 3.9 Listar clientes (sem paginacao)

`GET /clientes?paginado=false`

Response esperado (`200`):

```json
[
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
]
```

## 3.10 Listar clientes (paginado)

`GET /clientes?paginado=true&page=0&size=10`

Response esperado (`200`):

```json
{
  "content": [
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
  ],
  "number": 0,
  "size": 10,
  "totalElements": 2,
  "totalPages": 1,
  "first": true,
  "last": true
}
```

## 3.11 Listar contas (sem paginacao)

`GET /contas?paginado=false`

Response esperado (`200`):

```json
[
  {
    "id": "a4fbcfa4-f93f-4f0e-83ca-0db7bf2fe9f9",
    "clienteId": "d0ca7cf7-3ec4-4d3a-8f77-79d89cf45f9e",
    "agencia": "0001",
    "numeroConta": "12345678",
    "tipoContaEnum": "CORRENTE",
    "saldo": 80.00,
    "ativa": true
  }
]
```

## 3.12 Listar contas (paginado)

`GET /contas?paginado=true&page=0&size=10`

Response esperado (`200`):

```json
{
  "content": [
    {
      "id": "a4fbcfa4-f93f-4f0e-83ca-0db7bf2fe9f9",
      "clienteId": "d0ca7cf7-3ec4-4d3a-8f77-79d89cf45f9e",
      "agencia": "0001",
      "numeroConta": "12345678",
      "tipoContaEnum": "CORRENTE",
      "saldo": 80.00,
      "ativa": true
    }
  ],
  "number": 0,
  "size": 10,
  "totalElements": 2,
  "totalPages": 1,
  "first": true,
  "last": true
}
```

## 4) Cenários de erro obrigatórios

## 4.1 CPF invalido

`POST /clientes`

```json
{
  "nomeCompleto": "Teste CPF",
  "cpf": "11111111111",
  "email": "cpf.invalido@rkfbank.com",
  "telefone": "11999996666",
  "cep": "01001000",
  "numero": "10",
  "complemento": "Casa"
}
```

Response esperado (`400`):

```json
{
  "timestamp": "2026-10-05T15:40:00Z",
  "status": 400,
  "error": "Bad Request",
  "message": "Requisicao invalida",
  "path": "/api/clientes",
  "campos": [
    {
      "campo": "cpf",
      "mensagem": "cpf invalido"
    }
  ]
}
```

## 4.2 Saldo insuficiente

`POST /contas/{contaOrigemId}/saque` com valor maior que saldo.

Esperado: `422`.

## 4.3 Conta inexistente

`POST /contas/{uuidInexistente}/deposito`

Esperado: `404`.

## 4.4 Cliente inexistente

`POST /clientes/{uuidInexistente}/contas`

Esperado: `404`.

## 5) Conferencia no Workbench

```sql
SELECT BIN_TO_UUID(id) AS id, nome_completo, cpf, email
FROM tb_cliente;

SELECT BIN_TO_UUID(id) AS id, numero_conta, saldo
FROM tb_conta;

SELECT BIN_TO_UUID(id) AS id,
       tipo_lancamento_enum,
       valor,
       BIN_TO_UUID(conta_origem_id) AS conta_origem_id,
       BIN_TO_UUID(conta_destino_id) AS conta_destino_id,
       data_hora
FROM tb_lancamento
ORDER BY data_hora DESC;
```
