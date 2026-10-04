Perfeito — vou alinhar o contrato com o que já está definido no guia de implementação (rotas reais, status de erro e formato de `ErroResponse` com `campos`).

- [x] Validar rotas e DTOs usados no guia
- [x] Ajustar seção de erros (`400/404/409/422/503`) com exemplos consistentes
- [x] Atualizar o contrato para refletir abertura de conta em `/clientes/{clienteId}/contas`
- [x] Entregar versão pronta para substituir o conteúdo de `backend/README_CONTRATO_API.md`

Use este conteúdo no `backend/README_CONTRATO_API.md`:

```markdown
# RKF Bank - Contrato de API (MVP atual)

> Este arquivo e o contrato oficial entre backend e frontend ate o momento.
>
> Sempre que um endpoint mudar, atualize este documento para manter os dois lados alinhados.

## 1) Convencoes gerais

- Base local: `http://localhost:8080/api`
- Conteudo: `application/json`
- Moeda: decimal com `BigDecimal` (2 casas)
- DTOs:
  - entrada: `...Request`
  - saida: `...Response`

Padrao de envio recomendado no JSON:

- enviar como string decimal para evitar problemas de precisao no frontend JavaScript.
- exemplo: `"valor": "50.00"`

Padrao de recepcao no backend:

- DTOs Java usam `BigDecimal` no campo `valor`.
- Exemplo: `public record DepositarRequest(BigDecimal valor, String descricao) {}`

## 2) Entidades e responsabilidade de cada uma

- `Cliente`: guarda dados pessoais.
- `Endereco`: guarda endereco oficial + numero/complemento.
- `Conta`: guarda saldo atual da conta.
- `Lancamento`: guarda historico das movimentacoes.

Relacoes:

```text
Cliente (1) ----- (1) Endereco
Cliente (1) ----- (N) Conta
Conta (1) ------- (N) Lancamento
```

### Ponto mais importante sobre `Lancamento`

`Lancamento` e criado internamente pelo backend no `ServicoConta`.

- Deposito gera `DEPOSITO`
- Saque gera `SAQUE`
- Transferencia gera `TRANSFERENCIA`

Sem `Lancamento`, nao existe extrato confiavel para frontend.

## 3) Endpoints e contratos

## 3.1 Criar cliente

Endpoint:

`POST /clientes`

### Request - `CadastroClienteRequest`

```json
{
  "nomeCompleto": "Joao Silva",
  "cpf": "12345678901",
  "email": "joao@email.com",
  "telefone": "11999999999",
  "cep": "01001000",
  "numero": "123",
  "complemento": "Apto 45"
}
```

### Response 201 - `ClienteResponse`

```json
{
  "id": "a7fd1fa2-7d8f-4f89-bf89-c4e4fc8ce6e3",
  "nomeCompleto": "Joao Silva",
  "cpf": "12345678901",
  "email": "joao@email.com",
  "telefone": "11999999999",
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

Regras de negocio:

- CPF e email devem ser unicos.
- CEP deve ter 8 digitos numericos.
- Endereco e montado por composicao:
    - ViaCEP fornece dados oficiais.
    - usuario fornece `numero` e `complemento`.

## 3.2 Abrir conta para cliente

Endpoint:

`POST /clientes/{clienteId}/contas`

### Request - `AbrirContaRequest`

```json
{
  "agencia": "0001",
  "numeroConta": "12345678",
  "tipoContaEnum": "CORRENTE"
}
```

### Response 201 - `ContaResponse`

```json
{
  "id": "0f77d728-e8f8-4449-b8ac-2cb95a66c3a6",
  "clienteId": "a7fd1fa2-7d8f-4f89-bf89-c4e4fc8ce6e3",
  "agencia": "0001",
  "numeroConta": "12345678",
  "tipoContaEnum": "CORRENTE",
  "saldo": "0.00",
  "ativa": true
}
```

Regras de negocio:

- `clienteId` deve existir.
- `numeroConta` deve ser unico.
- `tipoContaEnum` deve ser `CORRENTE` ou `POUPANCA`.

## 3.3 Deposito

Endpoint:

`POST /contas/{contaId}/deposito`

### Request - `DepositarRequest`

```json
{
  "valor": "50.00",
  "descricao": "Deposito inicial"
}
```

### Response 204

Sem corpo.

Efeito interno:

- soma `valor` no saldo;
- cria `Lancamento` com tipo `DEPOSITO`.

## 3.4 Saque

Endpoint:

`POST /contas/{contaId}/saque`

### Request - `SacarRequest`

```json
{
  "valor": "15.00",
  "descricao": "Saque caixa 24h"
}
```

### Response 204

Sem corpo.

Efeito interno:

- valida saldo suficiente;
- subtrai valor do saldo;
- cria `Lancamento` com tipo `SAQUE`.

## 3.5 Transferencia

Endpoint:

`POST /transferencias`

### Request - `TransferirRequest`

```json
{
  "contaOrigemId": "0f77d728-e8f8-4449-b8ac-2cb95a66c3a6",
  "contaDestinoId": "8fdf7fa3-9965-40ca-a7e1-f8d2f9d96f4d",
  "valor": "25.00",
  "descricao": "Transferencia aluguel"
}
```

### Response 204

Sem corpo.

Efeito interno:

- valida contas diferentes;
- valida contas ativas;
- valida saldo da origem;
- debita origem, credita destino;
- cria `Lancamento` com tipo `TRANSFERENCIA`.

## 3.6 Extrato da conta

Endpoint:

`GET /contas/{contaId}/lancamentos`

### Response 200 - `List<LancamentoResponse>`

```json
[
  {
    "id": "8f3f1f83-577a-4d5a-8d22-d10f4312bfe0",
    "tipoLancamento": "DEPOSITO",
    "valor": "50.00",
    "contaOrigemId": null,
    "contaDestinoId": "0f77d728-e8f8-4449-b8ac-2cb95a66c3a6",
    "descricao": "Deposito inicial",
    "dataHora": "2026-10-02T12:30:00Z"
  }
]
```

## 4) Fluxo completo ViaCEP (entrada -> resposta)

1. Usuario digita CEP no frontend.
2. Front envia `CadastroClienteRequest` para `POST /api/clientes`.
3. Backend valida campos (`@Valid`).
4. Backend normaliza CEP (`replaceAll("\\D", "")`).
5. Backend valida regex `^\\d{8}$`.
6. Backend chama `GET https://viacep.com.br/ws/{cep}/json/`.
7. ViaCEP responde com endereco ou `{"erro": true}`.
8. Backend compoe o endereco:
    - ViaCEP: `logradouro`, `bairro`, `localidade`, `uf`, `ibge`
    - usuario: `numero`, `complemento`
9. Backend salva `Cliente` com `Endereco`.
10. Backend devolve `ClienteResponse` com endereco completo.

Observacao de status para CEP:

- CEP mal formatado no request (`cep` fora de `\\d{8}`) => `400`.
- CEP bem formatado, mas inexistente no ViaCEP (`"erro": true`) => `422`.

## 5) Erros padronizados

### Response - `ErroResponse`

```json
{
  "timestamp": "2026-10-02T10:15:30Z",
  "status": 400,
  "error": "Bad Request",
  "message": "Requisicao invalida",
  "path": "/api/clientes",
  "campos": [
    { "campo": "cpf", "mensagem": "cpf deve ter 11 digitos numericos" },
    { "campo": "cep", "mensagem": "cep deve ter 8 digitos numericos" }
  ]
}
```

Mapeamento atual:

- `400`:
    - validacao de payload (`@Valid`, campo ausente, formato invalido);
    - JSON invalido/tipo incompativel;
    - parametro de URL invalido (ex.: UUID mal formatado).
- `404`:
    - cliente nao encontrado;
    - conta nao encontrada.
- `409`:
    - conflito de unicidade (CPF/email/numeroConta ja existente).
- `422`:
    - regra de negocio com payload valido (saldo insuficiente, conta inativa, transferencia para mesma conta, CEP inexistente no ViaCEP).
- `503`:
    - servico externo indisponivel (ViaCEP fora do ar/timeout).

### Exemplo 404

```json
{
  "timestamp": "2026-10-02T10:15:30Z",
  "status": 404,
  "error": "Not Found",
  "message": "Conta nao encontrada",
  "path": "/api/contas/0f77d728-e8f8-4449-b8ac-2cb95a66c3a6/deposito",
  "campos": []
}
```

### Exemplo 422

```json
{
  "timestamp": "2026-10-02T10:15:30Z",
  "status": 422,
  "error": "Unprocessable Entity",
  "message": "Saldo insuficiente",
  "path": "/api/contas/0f77d728-e8f8-4449-b8ac-2cb95a66c3a6/saque",
  "campos": []
}
```

## 6) Checklist de alinhamento com frontend

- [ ] Front envia valores monetarios no campo `valor` (ex.: `"50.00"`)
- [ ] Front trata `204` sem corpo em deposito/saque/transferencia
- [ ] Front trata `201` com corpo em criacao de cliente e abertura de conta
- [ ] Front mostra `message` de `ErroResponse` para o usuario
- [ ] Front, em caso de `400`, mapeia `campos[]` para mensagens por input
- [ ] Front usa `ClienteResponse.endereco` para renderizar confirmacao do cadastro
- [ ] Time mantem nomes `Request`/`Response` sem misturar com entidades
```

Se quiser, no próximo passo eu também te devolvo uma versão **“diff por seção”** (o que mudou em cada item do contrato), para facilitar explicar ao seu amigo do frontend.