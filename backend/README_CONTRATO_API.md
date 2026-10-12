# RKF Bank - Contrato de API

Este arquivo é o acordo entre backend e frontend. Se um endpoint mudar, ele muda aqui no mesmo PR,
junto com os tipos em `frontend/lib/api/types.ts`.

- Backend: Kevin
- Frontend: Renan
- Versão: 1.1 (12/10/2026)

## 1) Stack e arquitetura

**Backend:** Java 21, Spring Boot 4, Spring Data JPA, Jakarta Validation, MySQL 8.4 (Docker), RestClient para o ViaCEP.

**Frontend:** Next.js 16, React 19, TypeScript, Tailwind CSS 4.

O backend segue arquitetura em camadas:

| Pacote | Responsabilidade |
|---|---|
| `controllers` | recebe a requisição HTTP e devolve o status |
| `services` | regras de negócio e transações |
| `repositories` | acesso ao banco (Spring Data JPA) |
| `entities` | modelo persistido |
| `dto` | formato de entrada e saída da API |
| `clients/viacep` | integração com o ViaCEP |
| `controllers/handlers` | padroniza as respostas de erro |
| `validation` | validações customizadas (CPF) |

## 2) Convenções

- Base URL: `http://localhost:8080` (sem prefixo `/api`)
- Corpo sempre em `application/json`
- IDs são UUID em texto
- Dinheiro é número com 2 casas decimais: `150.00`
- Data/hora em ISO-8601 UTC: `2026-10-12T11:19:53.038985Z`
- Enums em maiúsculas: `CORRENTE`, `POUPANCA`

**Como o frontend chama:** o navegador acessa `/api/...` no próprio Next, que repassa para o backend
(`next.config.ts`). Por isso o `/api` só existe no front. No Postman ou Insomnia, chame o backend direto.

## 3) Endpoints

| Método | Rota | O que faz | Sucesso |
|---|---|---|---|
| `POST` | `/clientes` | cadastra cliente | `201` |
| `GET` | `/clientes` | lista clientes | `200` |
| `POST` | `/contas/clientes/{clienteId}` | abre conta para o cliente | `201` |
| `GET` | `/contas` | lista contas | `200` |
| `POST` | `/contas/{contaId}/deposito` | deposita | `204` |
| `POST` | `/contas/{contaId}/saque` | saca | `204` |
| `POST` | `/contas/transferencia` | transfere entre contas | `204` |
| `GET` | `/contas/{contaId}/lancamentos` | extrato da conta | `200` |

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

| Campo | Regra |
|---|---|
| `nomeCompleto` | obrigatório, até 120 caracteres |
| `cpf` | obrigatório, válido e único |
| `email` | obrigatório, válido e único |
| `telefone` | obrigatório |
| `cep` | obrigatório, precisa existir no ViaCEP |
| `numero` | obrigatório |
| `complemento` | opcional, até 80 caracteres |

O front envia CPF, telefone e CEP só com números. O backend também aceita com máscara.

O endereço (logradouro, bairro, cidade, UF e IBGE) vem do ViaCEP. A consulta tem timeout de 3 segundos,
então vale mostrar um loading no cadastro.

Resposta `201`:

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

Na resposta, CPF, telefone e CEP já vêm formatados. `complemento` pode vir `null`.

### 3.2 Listar clientes

`GET /clientes` devolve a lista completa.

`GET /clientes?paginado=true&page=0&size=10` devolve paginado. `page` começa em 0 e `size` vai de 1 a 100.

Campos da página que usamos:

```json
{
  "content": [],
  "totalElements": 2,
  "totalPages": 1,
  "number": 0,
  "size": 10,
  "first": true,
  "last": true
}
```

### 3.3 Abrir conta

`POST /contas/clientes/{clienteId}`

```json
{
  "agencia": "0001",
  "numeroConta": "12345678",
  "tipoContaEnum": "CORRENTE"
}
```

| Campo | Regra |
|---|---|
| `agencia` | 4 dígitos |
| `numeroConta` | 5 a 12 dígitos, único |
| `tipoContaEnum` | `CORRENTE` ou `POUPANCA` |

Resposta `201`:

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

Toda conta começa com saldo zero e ativa. Um cliente pode ter mais de uma conta.

### 3.4 Listar contas

Igual à listagem de clientes: `GET /contas` ou `GET /contas?paginado=true&page=0&size=10`.

### 3.5 Depositar e sacar

`POST /contas/{contaId}/deposito` e `POST /contas/{contaId}/saque`

```json
{
  "valor": 150.00,
  "descricao": "Deposito inicial"
}
```

- `valor`: obrigatório, maior que zero, até 2 casas decimais
- `descricao`: opcional, até 140 caracteres

Resposta `204` sem corpo. O saque precisa de saldo suficiente.

### 3.6 Transferir

`POST /contas/transferencia`

```json
{
  "contaOrigemId": "407d727e-4e43-4c93-b01c-331cbaba42fb",
  "contaDestinoId": "566dd28f-b08f-41e4-ac55-cc69ecfea4cd",
  "valor": 50.00,
  "descricao": "Transferencia aluguel"
}
```

Resposta `204` sem corpo. Débito e crédito acontecem na mesma transação.

### 3.7 Extrato

`GET /contas/{contaId}/lancamentos`

Resposta `200`, do mais recente para o mais antigo:

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

| Tipo | `contaOrigemId` | `contaDestinoId` |
|---|---|---|
| `DEPOSITO` | `null` | conta que recebeu |
| `SAQUE` | conta que sacou | `null` |
| `TRANSFERENCIA` | quem enviou | quem recebeu |

O `valor` é sempre positivo. Para saber se é entrada ou saída, compare com a conta da tela:
se ela é o `contaDestinoId`, é entrada.

## 4) Regras de negócio

- CPF e e-mail não se repetem
- CEP precisa existir no ViaCEP
- Toda conta pertence a um cliente
- Número da conta não se repete
- Só conta ativa movimenta e tem extrato
- Valor sempre maior que zero
- Saque e transferência precisam de saldo
- Não dá para transferir para a mesma conta
- Operações simultâneas na mesma conta não se sobrescrevem (lock otimista)

## 5) Respostas de erro

Todo erro tratado volta no mesmo formato:

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

`campos` só vem preenchido em erro de validação, e o nome do campo é o mesmo do JSON.
O front usa isso para marcar o input certo no formulário.

| Status | Quando |
|---|---|
| `400` | dados inválidos, JSON mal formado ou parâmetro de URL inválido |
| `404` | cliente ou conta não encontrado |
| `409` | CPF, e-mail ou número de conta já existe, ou a conta foi alterada ao mesmo tempo |
| `422` | regra de negócio: CEP inexistente, saldo insuficiente, mesma conta na transferência |
| `503` | ViaCEP fora do ar |

No front, a decisão é sempre pelo `status`. O `message` serve para mostrar ao usuário.

## 6) Testando o fluxo completo

```bash
API=http://localhost:8080

# cliente
curl -X POST $API/clientes -H "Content-Type: application/json" -d '{
  "nomeCompleto":"Joao Silva","cpf":"52998224725","email":"joao@rkfbank.com",
  "telefone":"11999998888","cep":"01001000","numero":"123"}'

# conta (use o id do cliente)
curl -X POST $API/contas/clientes/$CLIENTE_ID -H "Content-Type: application/json" \
  -d '{"agencia":"0001","numeroConta":"12345678","tipoContaEnum":"CORRENTE"}'

# movimentações (use o id da conta)
curl -X POST $API/contas/$CONTA_ID/deposito -H "Content-Type: application/json" -d '{"valor":150.00}'
curl -X POST $API/contas/$CONTA_ID/saque    -H "Content-Type: application/json" -d '{"valor":20.00}'

# extrato
curl $API/contas/$CONTA_ID/lancamentos
```

CPFs válidos para teste: `52998224725`, `16899535009`, `39053344705`, `11144477735`.

## 7) Próximos endpoints

Para fechar o MVP, a tela de conta precisa buscar uma conta específica. A ideia é seguir este formato:

| Método | Rota | Para quê |
|---|---|---|
| `GET` | `/contas/{contaId}` | tela de saldo |
| `GET` | `/clientes/{clienteId}/contas` | listar as contas do cliente |
| `POST` | `/auth/login` | login (junto com o Spring Security) |

Quando um deles for implementado, ele sai daqui e entra na seção 3.

## 8) Como vamos construir

A ideia é que o projeto funcione e também mostre como um time trabalha de verdade.
Isto é o que seguimos (ou vamos adotar):

### Fluxo de trabalho

- **Branches:** `main` é o que está no ar, `develop` é a integração. Cada tarefa vira uma branch
  `feature/...`, `fix/...` ou `docs/...` saindo de `develop`.
- **Commits:** padrão [Conventional Commits](https://www.conventionalcommits.org/pt-br) (`feat:`, `fix:`, `docs:`, `refactor:`, `test:`, `chore:`).
- **Pull Requests:** todo código entra por PR, com descrição do que mudou e revisão do outro.
- **Contrato primeiro:** endpoint novo é combinado aqui antes de virar código, assim front e back trabalham em paralelo.
- **Decisões:** escolhas importantes ficam registradas em `planing/decisoes.md`.

### Qualidade

- **Testes no backend:** unitários com JUnit 5 e Mockito em cada camada, cobertura com JaCoCo.
- **Testes de integração:** Testcontainers com MySQL real, para testar repository e fluxo HTTP de ponta a ponta.
- **Frontend:** ESLint e `tsc` sem erros; testes de componente com Vitest e Testing Library; fluxo principal com Playwright.
- **CI:** GitHub Actions rodando `mvnw verify` e `pnpm lint`, `pnpm typecheck` e `pnpm build` em todo PR. PR com CI vermelho não entra.

### Backend

- **Migrations:** trocar o `ddl-auto=update` por Flyway, com o histórico do banco versionado no Git.
- **Documentação viva:** Swagger UI com springdoc-openapi, gerado a partir dos controllers.
- **Segurança:** Spring Security com JWT para o login; senha com BCrypt; nenhum segredo no repositório.
- **Logs:** logs estruturados com um id por requisição, para seguir uma operação do início ao fim.
- **Configuração:** tudo que muda por ambiente vem de variável de ambiente (`.env.example` mostra quais são).

### Frontend

- **Camada de API isolada:** componentes não chamam `fetch`; usam `clientesApi` e `contasApi` em `lib/api`.
- **Tipos espelhando o contrato:** `lib/api/types.ts` é a versão TypeScript deste arquivo.
- **Lógica fora do JSX:** cada tela tem um hook (`use-cadastro-form.ts`) e os componentes só desenham.
- **Logs por camada:** `lib/logger` para a API e `useLogDeTela` / `<LogDeTela />` para as telas.
- **Acessibilidade:** labels em todos os inputs, navegação por teclado e contraste conferido.

### Entrega

- **Ambiente local:** `docker compose up` sobe o banco; backend e frontend rodam com um comando cada.
- **Deploy:** frontend na Vercel e backend com banco em um serviço com plano gratuito (Render ou Railway).
- **README do projeto:** descrição, prints das telas, link do deploy, stack e como rodar.
  É a primeira coisa que um recrutador abre.
