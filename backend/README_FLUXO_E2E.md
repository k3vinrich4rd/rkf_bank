# RKF Bank - Fluxo ponta a ponta (Postman/Insomnia)

## 1) Subida do ambiente

```powershell
Set-Location "C:\Users\Estudo$\Documents\rkfbank"
Copy-Item .env.example .env
docker compose up -d
.\mvnw.cmd spring-boot:run
```

Base URL:

- `http://localhost:8080/api`

## 2) Ordem correta de cadastro

1. criar cliente origem
2. criar conta origem
3. criar cliente destino
4. criar conta destino
5. depositar na conta origem
6. sacar (opcional)
7. transferir origem -> destino
8. listar lancamentos da conta origem/destino
9. listar clientes e contas (paginado ou nao)

## 3) Requests (copiar e colar)

## 3.1 Criar cliente origem

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

Salve `id` como `clienteOrigemId`.

## 3.2 Criar conta origem

`POST /clientes/{clienteOrigemId}/contas`

```json
{
  "agencia": "0001",
  "numeroConta": "12345678",
  "tipoContaEnum": "CORRENTE"
}
```

Salve `id` como `contaOrigemId`.

## 3.3 Criar cliente destino

`POST /clientes`

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

Salve `id` como `clienteDestinoId`.

## 3.4 Criar conta destino

`POST /clientes/{clienteDestinoId}/contas`

```json
{
  "agencia": "0001",
  "numeroConta": "87654321",
  "tipoContaEnum": "CORRENTE"
}
```

Salve `id` como `contaDestinoId`.

## 3.5 Depositar

`POST /contas/{contaOrigemId}/deposito`

```json
{
  "valor": "150.00",
  "descricao": "Deposito inicial"
}
```

Esperado: `204 No Content`.

## 3.6 Sacar (opcional)

`POST /contas/{contaOrigemId}/saque`

```json
{
  "valor": "20.00",
  "descricao": "Saque teste"
}
```

Esperado: `204 No Content`.

## 3.7 Transferir

`POST /transferencias`

```json
{
  "contaOrigemId": "{contaOrigemId}",
  "contaDestinoId": "{contaDestinoId}",
  "valor": "50.00",
  "descricao": "Transferencia aluguel"
}
```

Esperado: `204 No Content`.

## 3.8 Listar lancamentos

`GET /contas/{contaOrigemId}/lancamentos`

Esperado: `200 OK` com lista de `DEPOSITO`, `SAQUE` e/ou `TRANSFERENCIA`.

## 3.9 Listar clientes (sem paginacao)

`GET /clientes?paginado=false`

## 3.10 Listar clientes (paginado)

`GET /clientes?paginado=true&page=0&size=10`

## 3.11 Listar contas (sem paginacao)

`GET /contas?paginado=false`

## 3.12 Listar contas (paginado)

`GET /contas?paginado=true&page=0&size=10`

## 4) Cenarios de erro obrigatorios

## 4.1 CPF invalido

`POST /clientes` com CPF invalido:

```json
{
  "nomeCompleto": "Teste CPF",
  "cpf": "11111111111",
  "email": "teste.cpf@rkfbank.com",
  "telefone": "11999996666",
  "cep": "01001000",
  "numero": "10",
  "complemento": "Casa"
}
```

Esperado: `400` com campo `cpf` invalido.

## 4.2 Saldo insuficiente

`POST /contas/{contaOrigemId}/saque` com valor alto.

Esperado: `422`.

## 4.3 Conta inexistente

Use UUID aleatorio em deposito/saque.

Esperado: `404`.

## 4.4 Cliente inexistente

Use UUID aleatorio em abertura de conta.

Esperado: `404`.

## 5) Consulta no Workbench (opcional)

IDs estao em `binary(16)`.

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

