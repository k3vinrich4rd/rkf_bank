<div align="center">

<img src="https://capsule-render.vercel.app/api?type=waving&color=0:003D74,100:EC7000&height=220&section=header&text=RKF%20Bank&fontSize=72&fontColor=ffffff&fontAlignY=36&desc=Banco%20digital%20constru%C3%ADdo%20ponta%20a%20ponta%2C%20com%20processo%20de%20time%20de%20verdade&descSize=18&descAlignY=58&animation=fadeIn" width="100%" alt="RKF Bank"/>

<a href="#-sobre-o-projeto">
  <img src="https://readme-typing-svg.demolab.com?font=Fira+Code&weight=600&size=22&duration=3000&pause=800&color=EC7000&center=true&vCenter=true&width=720&lines=Conta+digital+em+minutos+%F0%9F%8F%A6;Dep%C3%B3sito%2C+saque+e+transfer%C3%AAncia+%F0%9F%92%B8;Extrato+sempre+atualizado+%F0%9F%93%9C;Java+%2B+Spring+Boot+%2B+Next.js+%E2%9A%A1;Cada+opera%C3%A7%C3%A3o+rastreada+nos+logs+%F0%9F%94%8E" alt="O que o RKF Bank faz"/>
</a>

**Responsáveis pelo projeto:**
[**Kevin Richard**](https://github.com/k3vinrich4rd) — backend ·
[**Renan Barros**](https://github.com/renanbarrostech-cmd) — frontend

[![Backend CI](https://github.com/k3vinrich4rd/rkf_bank/actions/workflows/backend-ci.yml/badge.svg?branch=develop)](https://github.com/k3vinrich4rd/rkf_bank/actions/workflows/backend-ci.yml)
[![Backend CodeQL](https://github.com/k3vinrich4rd/rkf_bank/actions/workflows/backend-codeql.yml/badge.svg?branch=develop)](https://github.com/k3vinrich4rd/rkf_bank/actions/workflows/backend-codeql.yml)
[![Último commit](https://img.shields.io/github/last-commit/k3vinrich4rd/rkf_bank/develop?color=EC7000&label=último%20commit)](https://github.com/k3vinrich4rd/rkf_bank/commits/develop)
[![PRs abertos](https://img.shields.io/github/issues-pr/k3vinrich4rd/rkf_bank?color=003D74&label=PRs%20abertos)](https://github.com/k3vinrich4rd/rkf_bank/pulls)

[![Java](https://img.shields.io/badge/Java-25_LTS-EC7000?logo=openjdk&logoColor=white)](https://openjdk.org/projects/jdk/25/)
[![Spring Boot](https://img.shields.io/badge/Spring_Boot-4.1-003D74?logo=springboot&logoColor=white)](https://spring.io/projects/spring-boot)
[![MySQL](https://img.shields.io/badge/MySQL-8.4-003D74?logo=mysql&logoColor=white)](https://dev.mysql.com/doc/refman/8.4/en/)
[![Next.js](https://img.shields.io/badge/Next.js-16-EC7000?logo=nextdotjs&logoColor=white)](https://nextjs.org/)
[![React](https://img.shields.io/badge/React-19-003D74?logo=react&logoColor=white)](https://react.dev/)
[![TypeScript](https://img.shields.io/badge/TypeScript-5-003D74?logo=typescript&logoColor=white)](https://www.typescriptlang.org/)
[![Status](https://img.shields.io/badge/status-MVP_em_desenvolvimento-EC7000)](planing/roadmap.md)

[Sobre](#-sobre-o-projeto) ·
[Experiência](#-a-experiência-rkf-bank) ·
[Funcionalidades](#-funcionalidades) ·
[Tecnologias](#%EF%B8%8F-tecnologias) ·
[Arquitetura](#%EF%B8%8F-arquitetura) ·
[API](#-api-rest) ·
[Rodando](#-rodando-o-projeto) ·
[Testes](#-testes-e-qualidade) ·
[Logs](#-observabilidade-e-logs) ·
[CI/CD](#%EF%B8%8F-github-actions-cicd) ·
[Roadmap](#%EF%B8%8F-roadmap) ·
[Time e contato](#-quem-faz-o-rkf-bank)

</div>

<img src="https://capsule-render.vercel.app/api?type=rect&color=0:003D74,100:EC7000&height=3&section=header" width="100%"/>

## 🧭 Sobre o projeto

O **RKF Bank** é uma aplicação bancária completa (**API REST + site**) criada para praticar, na vida real,
o que um time de engenharia faz no dia a dia: combinar um **contrato de API**, trabalhar com **Git Flow e
Pull Requests**, registrar **decisões**, escrever **testes automatizados**, rodar **CI no GitHub Actions** e
entregar um **MVP com prazo**.

> *Conhecimento não tem teto. Código não tem limite. Só tem próximo nível.*

<table>
  <tr>
    <td align="center" width="25%"><h3>🏦</h3><b>Banco digital</b><br/><sub>clientes, contas e movimentações</sub></td>
    <td align="center" width="25%"><h3>🔌</h3><b>API REST</b><br/><sub>contrato combinado antes do código</sub></td>
    <td align="center" width="25%"><h3>🧪</h3><b>Testado</b><br/><sub>testes automatizados + roteiro E2E</sub></td>
    <td align="center" width="25%"><h3>🚀</h3><b>MVP</b><br/><sub>meta: no ar até 31/10/2026</sub></td>
  </tr>
</table>

Acompanhe a meta no [roadmap](planing/roadmap.md).

<img src="https://capsule-render.vercel.app/api?type=rect&color=0:003D74,100:EC7000&height=3&section=header" width="100%"/>

## 📱 A experiência RKF Bank

A jornada de quem abre uma conta no RKF Bank, do cadastro ao extrato:

```mermaid
journey
    title Jornada do cliente no RKF Bank
    section Abrir a conta
      Preenche nome, CPF e e-mail: 5: Cliente
      Digita o CEP e o endereço aparece sozinho: 5: Cliente
      Escolhe conta corrente ou poupança: 4: Cliente
    section Movimentar
      Deposita o primeiro dinheiro: 5: Cliente
      Saca quando precisa: 4: Cliente
      Transfere para outra conta: 5: Cliente
    section Acompanhar
      Confere o extrato, do mais novo para o mais antigo: 5: Cliente
```

<table>
  <tr>
    <td align="center" width="33%">
      <h3>👤 Cadastro inteligente</h3>
      CPF validado na hora, e-mail único e endereço completo preenchido pelo <b>ViaCEP</b> só com o CEP.
    </td>
    <td align="center" width="33%">
      <h3>💳 Conta digital</h3>
      Corrente ou poupança, aberta em segundos, com <b>saldo zero e conta ativa</b> desde o primeiro minuto.
    </td>
    <td align="center" width="33%">
      <h3>💸 Dinheiro em movimento</h3>
      Depósito, saque e transferência com <b>tudo-ou-nada</b>: nunca sai dinheiro de uma conta sem entrar na outra.
    </td>
  </tr>
  <tr>
    <td align="center">
      <h3>📜 Extrato</h3>
      Todos os lançamentos da conta, com entrada e saída identificadas e o mais recente primeiro.
    </td>
    <td align="center">
      <h3>🛡️ Segurança e LGPD</h3>
      CPF e e-mail <b>nunca</b> vão para os logs do backend; o frontend mascara CPF, e-mail e telefone.
    </td>
    <td align="center">
      <h3>🔎 Rastreável</h3>
      Cada operação ganha um <code>X-Request-Id</code> que segue por todas as linhas de log, até o SQL.
    </td>
  </tr>
</table>

<img src="https://capsule-render.vercel.app/api?type=rect&color=0:003D74,100:EC7000&height=3&section=header" width="100%"/>

## ✨ Funcionalidades

| Funcionalidade | Backend | Frontend |
|---|:---:|:---:|
| Landing page institucional (hero, recursos, segurança, FAQ) | — | ✅ |
| Cadastro de cliente com endereço automático pelo CEP (ViaCEP) | ✅ | ✅ |
| Listagem de clientes (completa ou paginada) | ✅ | — |
| Abertura de conta corrente ou poupança | ✅ | ⏳ |
| Listagem de contas (completa ou paginada) | ✅ | — |
| Depósito e saque | ✅ | ⏳ |
| Transferência entre contas | ✅ | ⏳ |
| Extrato da conta | ✅ | ⏳ |
| Logs rastreáveis por requisição | ✅ | ✅ |
| Tela de saldo da conta | ⏳ | ⏳ |
| Login | ⏳ | 🟡 |

✅ pronto · 🟡 parcial · ⏳ a fazer

<details>
<summary><b>🔎 Ver as regras de negócio que o backend garante</b></summary>
<br/>

- CPF (com dígito verificador validado por `@CpfValido`) e e-mail **não se repetem**.
- O **CEP precisa existir** no ViaCEP — logradouro, bairro, cidade, UF e IBGE são preenchidos automaticamente.
- Toda conta pertence a um cliente e **começa com saldo zero e ativa**. Um cliente pode ter várias contas.
- O **número da conta não se repete**.
- Só **conta ativa** movimenta e tem extrato.
- Todo valor precisa ser **maior que zero**, com até 2 casas decimais.
- Saque e transferência exigem **saldo suficiente**.
- Não dá para **transferir para a mesma conta**.
- Débito e crédito da transferência acontecem **na mesma transação** (tudo ou nada).
- Operações simultâneas na mesma conta **não se sobrescrevem** (lock otimista com `@Version`).

</details>

<details>
<summary><b>⭐ Ver os destaques técnicos</b></summary>
<br/>

- **Dinheiro tratado com cuidado:** `BigDecimal`, `@Transactional` e lock otimista contra concorrência.
- **Contrato de API primeiro:** frontend e backend trabalham em paralelo a partir do mesmo documento.
- **Arquitetura em camadas:** Controller → Service → Repository, com DTOs (`record`) e erros padronizados.
- **Integração externa resiliente:** consulta ao ViaCEP com *timeout* de 3s e erros traduzidos para `422`/`503`.
- **Erros previsíveis:** um único `@RestControllerAdvice` devolve sempre o mesmo formato, com o campo inválido.
- **Logs com Log4j2 em todas as camadas**, com `requestId` em cada linha e sem dados pessoais.
- **Testes automatizados** em todas as camadas do backend, com cobertura (JaCoCo), e roteiro E2E com checklist.
- **LGPD de ponta a ponta:** o backend não registra CPF/e-mail; o frontend mascara CPF, e-mail e telefone.
- **Esteira no GitHub Actions:** build, testes, análise de segurança e revisão de dependências em todo PR.

</details>

<img src="https://capsule-render.vercel.app/api?type=rect&color=0:003D74,100:EC7000&height=3&section=header" width="100%"/>

## 🛠️ Tecnologias

<div align="center">

<img src="https://skillicons.dev/icons?i=java,spring,maven,mysql,docker,nextjs,react,ts,tailwind,githubactions&perline=10" alt="Stack do RKF Bank"/>

</div>

<details open>
<summary><b>☕ Backend</b></summary>
<br/>

| Tecnologia | Versão | Para que usamos |
|---|---|---|
| [Java](https://openjdk.org/projects/jdk/25/) | 25 (LTS) | linguagem do backend |
| [Spring Boot](https://spring.io/projects/spring-boot) | 4.1 | base da aplicação |
| Spring Web MVC | — | controllers REST |
| [Spring Data JPA](https://spring.io/projects/spring-data-jpa) + Hibernate | 7 | acesso ao banco e repositórios |
| [Jakarta Validation](https://beanvalidation.org/) | — | validação dos DTOs + validador customizado de CPF |
| `RestClient` (Spring) | — | integração com o [ViaCEP](https://viacep.com.br/) |
| [Log4j2](https://logging.apache.org/log4j/2.x/) | 2 | logs em todas as camadas, com `requestId` por requisição |
| [MySQL](https://dev.mysql.com/doc/refman/8.4/en/) | 8.4 | banco de dados (via Docker Compose) |
| [H2](https://www.h2database.com/) | — | banco em memória para os testes |
| [JUnit 5](https://junit.org/junit5/) + [Mockito](https://site.mockito.org/) | — | testes automatizados |
| [JaCoCo](https://www.jacoco.org/jacoco/) | — | relatório de cobertura de testes |
| [Maven Wrapper](https://maven.apache.org/wrapper/) | Maven 3.9 | build sem precisar instalar o Maven |
| Spring DevTools | — | reinício automático em desenvolvimento |

</details>

<details>
<summary><b>⚛️ Frontend</b></summary>
<br/>

| Tecnologia | Versão | Para que usamos |
|---|---|---|
| [Next.js](https://nextjs.org/) (App Router) | 16 | framework do site e proxy `/api` para o backend |
| [React](https://react.dev/) | 19 | interface |
| [TypeScript](https://www.typescriptlang.org/) | 5 | tipagem, espelhando o contrato da API |
| [Tailwind CSS](https://tailwindcss.com/) | 4 | estilos |
| [shadcn/ui](https://ui.shadcn.com/) + [Radix UI](https://www.radix-ui.com/) | — | componentes acessíveis |
| [Lucide](https://lucide.dev/) | — | ícones |
| [ESLint](https://eslint.org/) | 9 | padrão de código |
| [pnpm](https://pnpm.io/) | 10 | gerenciador de pacotes |

</details>

<details>
<summary><b>🧰 Infra, qualidade e processo</b></summary>
<br/>

| Ferramenta | Para que usamos |
|---|---|
| [Docker Compose](https://docs.docker.com/compose/) | sobe o MySQL local com *healthcheck* |
| [GitHub Actions](https://docs.github.com/actions) | CI do backend, CodeQL e revisão de dependências |
| [CodeQL](https://codeql.github.com/) | análise de segurança do código Java |
| [Dependabot](https://docs.github.com/code-security/dependabot) | PRs automáticos de atualização (Maven e Actions) |
| [Conventional Commits](https://www.conventionalcommits.org/pt-br) | padrão das mensagens de commit |
| Git Flow simplificado | `feature/*` → `develop` → `main` |

</details>

<img src="https://capsule-render.vercel.app/api?type=rect&color=0:003D74,100:EC7000&height=3&section=header" width="100%"/>

## 🏗️ Arquitetura

```mermaid
flowchart LR
    U([Usuário]) -->|navegador| FE

    subgraph FE [Frontend · Next.js :3000]
        P[Páginas / telas] --> H[Hooks de tela]
        H --> API["lib/api<br/>(clientesApi, contasApi)"]
    end

    API -->|"/api/... (proxy do Next)"| BE

    subgraph BE [Backend · Spring Boot :8080]
        F["Filtro de log<br/>(X-Request-Id)"] --> C[Controllers]
        C --> S[Services]
        S --> R[Repositories]
        S --> V[Client ViaCEP]
        C -. erros .-> EH[ExceptionHandler]
    end

    R --> DB[(MySQL 8.4<br/>Docker :5441)]
    V -->|HTTPS| VC[[viacep.com.br]]
```

- O **navegador nunca fala direto com o backend**: ele chama `/api/...` no próprio Next, que repassa para
  `http://localhost:8080` (veja [`frontend/next.config.ts`](frontend/next.config.ts)).
- A **fronteira entre os dois lados** é o [contrato de API](backend/README_CONTRATO_API.md). Mudou um endpoint?
  Muda o contrato **no mesmo PR**.

<details>
<summary><b>📦 Ver as camadas do backend</b></summary>
<br/>

| Pacote | Responsabilidade |
|---|---|
| [`config`](backend/src/main/java/com/br/rkfbank/config) | CORS e o filtro que registra cada requisição com o `X-Request-Id` |
| [`controllers`](backend/src/main/java/com/br/rkfbank/controllers) | recebe a requisição HTTP e devolve o status |
| [`services`](backend/src/main/java/com/br/rkfbank/services) | regras de negócio e transações |
| [`repositories`](backend/src/main/java/com/br/rkfbank/repositories) | acesso ao banco (Spring Data JPA) |
| [`entities`](backend/src/main/java/com/br/rkfbank/entities) | modelo persistido (`Cliente`, `Endereco`, `Conta`, `Lancamento`) |
| [`dto`](backend/src/main/java/com/br/rkfbank/dto) | formato de entrada e saída da API |
| [`clients/viacep`](backend/src/main/java/com/br/rkfbank/clients/viacep) | integração com o ViaCEP |
| [`controllers/handlers`](backend/src/main/java/com/br/rkfbank/controllers/handlers) | padroniza as respostas de erro |
| [`validation`](backend/src/main/java/com/br/rkfbank/validation) | validações customizadas (`@CpfValido`) |
| [`exceptions`](backend/src/main/java/com/br/rkfbank/exceptions) | exceções de negócio que viram status HTTP |

Mais detalhes em [backend/README_IMPLEMENTACAO.md](backend/README_IMPLEMENTACAO.md).

</details>

<details>
<summary><b>🗃️ Ver o modelo de dados</b></summary>
<br/>

```mermaid
erDiagram
    CLIENTE ||--|| ENDERECO : mora_em
    CLIENTE ||--o{ CONTA : possui
    CONTA ||--o{ LANCAMENTO : "contaOrigemId / contaDestinoId"

    CLIENTE {
        uuid id
        string nomeCompleto
        string cpf "único"
        string email "único"
        string telefone
    }
    ENDERECO {
        uuid id
        string cep
        string logradouro
        string bairro
        string cidade
        string uf
        string ibge
        string numero
        string complemento
    }
    CONTA {
        uuid id
        string agencia
        string numeroConta "único"
        enum tipoContaEnum "CORRENTE | POUPANCA"
        decimal saldo
        boolean ativa
        long versao "lock otimista (@Version)"
    }
    LANCAMENTO {
        uuid id
        enum tipoLancamentoEnum "DEPOSITO | SAQUE | TRANSFERENCIA"
        decimal valor
        uuid contaOrigemId
        uuid contaDestinoId
        string descricao
        instant dataHora
    }
```

</details>

<details>
<summary><b>🔁 Ver o fluxo de uma transferência</b></summary>
<br/>

```mermaid
sequenceDiagram
    autonumber
    participant F as Frontend
    participant L as FiltroLogRequisicao
    participant C as ContaController
    participant S as ContaService
    participant DB as MySQL

    F->>L: POST /contas/transferencia
    L->>L: gera ou reaproveita o X-Request-Id
    L->>C: segue a requisição
    C->>C: valida o JSON (@Valid)
    C->>S: transferir(request)
    activate S
    Note over S,DB: @Transactional — tudo ou nada
    S->>DB: busca conta de origem e de destino
    S->>S: contas ativas? diferentes? saldo suficiente?
    S->>DB: debita origem e credita destino (@Version)
    S->>DB: grava o lançamento TRANSFERENCIA
    deactivate S
    C-->>L: 204 No Content
    L-->>F: 204 + header X-Request-Id
    Note over F,C: Erro de regra → 422 · conta alterada ao mesmo tempo → 409
```

</details>

<img src="https://capsule-render.vercel.app/api?type=rect&color=0:003D74,100:EC7000&height=3&section=header" width="100%"/>

## 🔌 API REST

Base URL: `http://localhost:8080` · JSON · IDs em UUID · dinheiro com 2 casas (`150.00`) · datas em ISO-8601 UTC.
O contrato completo está em [backend/README_CONTRATO_API.md](backend/README_CONTRATO_API.md) e as respostas
de cada rota, inclusive dos erros, estão no [roteiro E2E](backend/README_TESTES_E2E.md).

| Método | Rota | O que faz | Sucesso |
|---|---|---|---|
| `POST` | `/clientes` | cadastra cliente | `201` |
| `GET` | `/clientes` | lista clientes (`?paginado=true&page=0&size=10`) | `200` |
| `POST` | `/contas/clientes/{clienteId}` | abre conta para o cliente | `201` |
| `GET` | `/contas` | lista contas (`?paginado=true&page=0&size=10`) | `200` |
| `POST` | `/contas/{contaId}/deposito` | deposita | `204` |
| `POST` | `/contas/{contaId}/saque` | saca | `204` |
| `POST` | `/contas/transferencia` | transfere entre contas | `204` |
| `GET` | `/contas/{contaId}/lancamentos` | extrato (mais recente primeiro) | `200` |

<details>
<summary><b>👤 <code>POST /clientes</code> — cadastrar cliente</b></summary>
<br/>

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

Resposta `201` — o endereço vem do ViaCEP e CPF, telefone e CEP voltam formatados:

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

</details>

<details>
<summary><b>💳 <code>POST /contas/clientes/{clienteId}</code> — abrir conta</b></summary>
<br/>

```json
{
  "agencia": "0001",
  "numeroConta": "12345678",
  "tipoContaEnum": "CORRENTE"
}
```

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

</details>

<details>
<summary><b>💰 <code>POST /contas/{contaId}/deposito</code> e <code>/saque</code> — movimentar</b></summary>
<br/>

```json
{
  "valor": 150.00,
  "descricao": "Deposito inicial"
}
```

Resposta `204` sem corpo. O saque exige saldo suficiente.

</details>

<details>
<summary><b>🔀 <code>POST /contas/transferencia</code> — transferir</b></summary>
<br/>

```json
{
  "contaOrigemId": "407d727e-4e43-4c93-b01c-331cbaba42fb",
  "contaDestinoId": "566dd28f-b08f-41e4-ac55-cc69ecfea4cd",
  "valor": 50.00,
  "descricao": "Transferencia aluguel"
}
```

Resposta `204` sem corpo.

</details>

<details>
<summary><b>📄 <code>GET /contas/{contaId}/lancamentos</code> — extrato</b></summary>
<br/>

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
  }
]
```

O `valor` é sempre positivo: se a conta da tela é o `contaDestinoId`, é entrada; senão, saída.

</details>

<details>
<summary><b>⚠️ Formato de erro e status HTTP</b></summary>
<br/>

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

| Status | Quando |
|---|---|
| `400` | dados inválidos, JSON mal formado ou parâmetro inválido |
| `404` | cliente ou conta não encontrado |
| `409` | CPF, e-mail ou número de conta repetido, ou conta alterada ao mesmo tempo |
| `422` | regra de negócio: CEP inexistente, saldo insuficiente, mesma conta na transferência |
| `503` | ViaCEP fora do ar |

</details>

<details>
<summary><b>🧪 Testar o fluxo completo com <code>curl</code></b></summary>
<br/>

```bash
API=http://localhost:8080

# 1. cliente
curl -X POST $API/clientes -H "Content-Type: application/json" -d '{
  "nomeCompleto":"Joao Silva","cpf":"52998224725","email":"joao@rkfbank.com",
  "telefone":"11999998888","cep":"01001000","numero":"123"}'

# 2. conta (use o id do cliente)
curl -X POST $API/contas/clientes/$CLIENTE_ID -H "Content-Type: application/json" \
  -d '{"agencia":"0001","numeroConta":"12345678","tipoContaEnum":"CORRENTE"}'

# 3. movimentações (use o id da conta)
curl -X POST $API/contas/$CONTA_ID/deposito -H "Content-Type: application/json" -d '{"valor":150.00}'
curl -X POST $API/contas/$CONTA_ID/saque    -H "Content-Type: application/json" -d '{"valor":20.00}'

# 4. extrato
curl $API/contas/$CONTA_ID/lancamentos
```

CPFs válidos para teste: `52998224725`, `16899535009`, `39053344705`, `11144477735`.
Roteiro completo para Postman/Insomnia: [backend/README_TESTES_E2E.md](backend/README_TESTES_E2E.md).

</details>

<img src="https://capsule-render.vercel.app/api?type=rect&color=0:003D74,100:EC7000&height=3&section=header" width="100%"/>

## 🚀 Rodando o projeto

### Pré-requisitos

| Ferramenta | Versão | Para quê |
|---|---|---|
| [JDK](https://adoptium.net/temurin/releases/?version=25) | **25** (o Maven Enforcer bloqueia outras versões) | backend |
| [Docker Desktop](https://www.docker.com/products/docker-desktop/) | recente | banco MySQL |
| [Node.js](https://nodejs.org/) | 20 ou superior | frontend |
| [pnpm](https://pnpm.io/installation) | 10 (`corepack enable` já resolve) | frontend |

> O Maven **não** precisa ser instalado: o projeto usa o Maven Wrapper (`mvnw` / `mvnw.cmd`).
> Confira o Java com `java -version` (deve mostrar `25.x`) e garanta que o `JAVA_HOME` aponta para o JDK 25.

<details open>
<summary><b>1️⃣ Banco de dados (MySQL no Docker)</b></summary>
<br/>

Na **raiz** do repositório:

```bash
cp .env.example .env          # PowerShell: Copy-Item .env.example .env
docker compose up -d          # sobe o MySQL na porta 5441
docker compose ps             # espere o status "healthy"
```

</details>

<details open>
<summary><b>2️⃣ Backend (porta 8080)</b></summary>
<br/>

O Spring **não lê o arquivo `.env` sozinho** — as variáveis precisam estar no ambiente do terminal.

```bash
# Git Bash / Linux / macOS
set -a && source .env && set +a
./mvnw spring-boot:run
```

```powershell
# PowerShell
Get-Content .env | Where-Object { $_ -match '^\s*[^#].*=' } | ForEach-Object {
  $nome, $valor = $_ -split '=', 2; Set-Item "env:$nome" $valor
}
.\mvnw.cmd spring-boot:run
```

Teste rápido: `curl http://localhost:8080/clientes` deve responder `200` com um array (`[]` no banco vazio).

> Sem Docker? Suba com H2 em memória: `./mvnw spring-boot:run -Dspring-boot.run.profiles=test`
> (os dados somem ao parar a API). No IntelliJ, dá para usar o plugin **EnvFile** apontando para o `.env`.

</details>

<details open>
<summary><b>3️⃣ Frontend (porta 3000)</b></summary>
<br/>

```bash
cd frontend
cp .env.example .env.local    # PowerShell: Copy-Item .env.example .env.local
pnpm install
pnpm dev
```

Abra <http://localhost:3000>. O cadastro fica em <http://localhost:3000/cadastro>.

</details>

<details>
<summary><b>🧯 Problemas comuns</b></summary>
<br/>

| Sintoma | Causa provável | Solução |
|---|---|---|
| `Rule 0: RequireJavaVersion failed` | JDK diferente do 25 | instale o JDK 25 e aponte o `JAVA_HOME` para ele |
| `Access denied for user 'rkfbank'` | variáveis do `.env` não carregadas | rode o `source .env` (ou o script do PowerShell) antes do `mvnw` |
| `Communications link failure` | MySQL desligado ou ainda subindo | `docker compose ps` e espere o `healthy` |
| Cadastro devolve `503` | ViaCEP fora do ar ou sem internet | tente de novo em alguns segundos |
| Frontend não acha a API | backend parado ou `.env.local` errado | confira se a API responde em `:8080` |

</details>

<img src="https://capsule-render.vercel.app/api?type=rect&color=0:003D74,100:EC7000&height=3&section=header" width="100%"/>

## 🧪 Testes e qualidade

### Automatizados (backend)

```bash
# sem Docker: perfil "test" usa H2 em memória (+ relatório de cobertura)
./mvnw clean verify -Dspring.profiles.active=test   # PowerShell: .\mvnw.cmd clean verify "-Dspring.profiles.active=test"

# com o MySQL no ar e o .env carregado
./mvnw clean verify

# recria o MySQL (APAGA os dados) e roda os testes com o perfil "test"
./run-test.sh
```

O relatório do JaCoCo fica em `target/site/jacoco/index.html` — abra no navegador.

<details>
<summary><b>📋 Ver o que é testado no backend</b></summary>
<br/>

| Suíte | O que cobre |
|---|---|
| `ClienteServiceTest` | cadastro, CPF/e-mail duplicado, integração com ViaCEP |
| `ContaServiceTest` | abertura de conta, depósito, saque, transferência, saldo e extrato |
| `ClienteControllerTest` / `ContaControllerTest` | status HTTP de cada rota e limites da paginação |
| `ControllerExceptionHandlerTest` | tradução de cada exceção para o formato de erro |
| `ClienteViaCepTest` | CEP encontrado, CEP inexistente (`422`) e falha do ViaCEP (`503`) |
| `CpfValidoValidatorTest` | dígitos verificadores, CPFs inválidos e com máscara |
| `FiltroLogRequisicaoTest` | `X-Request-Id` reaproveitado, gerado e rejeitado quando tenta forjar linhas de log |
| `EntitiesAndEnumsTest` | entidades e enums |
| `ClienteRepositoryTest` / `ContaRepositoryTest` / `LancamentoRepositoryTest` | consultas do Spring Data, constraints, cascata, lock otimista e extrato no H2 (`@DataJpaTest`) |
| `RkfbankApplicationTests` | sobe o contexto completo do Spring |

</details>

### Ponta a ponta (manual)

| Passo | Documento |
|---|---|
| 1. Siga o roteiro: caminho feliz e cenários de erro, com o response de cada request | [README_TESTES_E2E.md](backend/README_TESTES_E2E.md) |
| 2. Marque o resultado item a item | [CHECKLIST_TESTES_E2E.md](backend/CHECKLIST_TESTES_E2E.md) |

### Frontend

```bash
cd frontend
pnpm lint
pnpm typecheck
pnpm build
```

<img src="https://capsule-render.vercel.app/api?type=rect&color=0:003D74,100:EC7000&height=3&section=header" width="100%"/>

## 🔎 Observabilidade e logs

Cada requisição ao backend recebe um **`X-Request-Id`** (reaproveitado se o cliente enviar um válido, ou gerado
na hora). Ele volta no header da resposta e aparece em **todas** as linhas de log daquela chamada:

```text
INFO  [http-nio-8080-exec-1] [e2e-transf-001] c.b.r.s.ContaService : Transferencia de 50.00 da conta 672e4ddf-... para 83af15fb-...
INFO  [http-nio-8080-exec-1] [e2e-transf-001] c.b.r.c.FiltroLogRequisicao : POST /contas/transferencia -> 204 (15 ms)
```

<details>
<summary><b>📐 Ver o que cada camada registra</b></summary>
<br/>

| Camada | Nível | O que aparece |
|---|---|---|
| Filtro (`FiltroLogRequisicao`) | `INFO` | uma linha por requisição: método, rota, status e duração |
| Controllers | `DEBUG` | parâmetros recebidos (ids, valores, paginação) |
| Services | `INFO` / `WARN` | operação concluída (cadastro, abertura, depósito, saque, transferência) ou recusada pela regra de negócio |
| Client ViaCEP | `DEBUG` / `WARN` / `ERROR` | tempo da consulta, CEP inexistente, ViaCEP indisponível (com a causa) |
| Handler de erros | `WARN` | erros de entrada (validação, JSON, parâmetro) e conflitos de concorrência |
| Hibernate (`org.hibernate.SQL`) | `DEBUG` | SQL executado, também com o `requestId` |

- **CPF e e-mail nunca são registrados**, nem quando vêm dentro da mensagem de erro do banco.
- O formato e os níveis ficam em [`log4j2-spring.xml`](backend/src/main/resources/log4j2-spring.xml).

</details>

<img src="https://capsule-render.vercel.app/api?type=rect&color=0:003D74,100:EC7000&height=3&section=header" width="100%"/>

## ⚙️ GitHub Actions (CI/CD)

Todo PR para `develop` ou `main` passa por uma esteira automática. PR com check vermelho não entra.

```mermaid
flowchart LR
    PR([Pull Request<br/>para develop/main]) --> CI
    PR --> CQ
    PR --> DR

    subgraph CI [Backend CI]
        B1[JDK 25] --> B2["mvnw verify<br/>(build + testes)"] --> B3[relatórios de<br/>testes e cobertura]
    end

    subgraph CQ [Backend CodeQL]
        Q1[análise de segurança<br/>do código Java] --> Q2[alertas em<br/>Security › Code scanning]
    end

    subgraph DR [Dependency Review]
        D1[dependências novas<br/>no pom.xml] --> D2{vulnerabilidade<br/>alta/crítica?}
        D2 -->|sim| X[❌ bloqueia o PR]
        D2 -->|não| OK[✅ libera]
    end

    DB([Dependabot<br/>toda semana]) -.->|abre PRs de atualização| PR
```

| Workflow | Arquivo | Quando roda | O que faz |
|---|---|---|---|
| **Backend CI** | [`backend-ci.yml`](.github/workflows/backend-ci.yml) | push/PR no backend | compila com JDK 25, roda os testes e publica os relatórios do JaCoCo e do Surefire |
| **Backend CodeQL** | [`backend-codeql.yml`](.github/workflows/backend-codeql.yml) | push/PR no backend e toda segunda | procura vulnerabilidades no código Java (SQL injection, dados sensíveis em log etc.) |
| **Backend Dependency Review** | [`backend-dependency-review.yml`](.github/workflows/backend-dependency-review.yml) | PR que muda o `pom.xml` | bloqueia dependência com vulnerabilidade alta/crítica ou licença AGPL |
| **Dependabot** | [`dependabot.yml`](.github/dependabot.yml) | toda semana (Maven) e todo mês (Actions) | abre PRs para a `develop` atualizando dependências |

<details>
<summary><b>🧑‍🎓 Primeira vez com GitHub Actions? Leia aqui</b></summary>
<br/>

- Um **workflow** é um arquivo `.yml` em `.github/workflows/`. O GitHub lê esse arquivo e executa os passos
  numa máquina virtual sempre que o evento configurado em `on:` acontece (push, PR, horário…).
- Os workflows do backend só rodam quando arquivos do backend mudam (filtro `paths:`), então um PR só de
  frontend não gasta minutos à toa.
- O resultado aparece na aba **Actions** do repositório e no final de cada PR, como *checks* ✅ ou ❌.
- Clique num check que falhou para ver o log do passo que quebrou. Os relatórios de testes e cobertura ficam em
  **Artifacts**, no resumo da execução.
- Para exigir CI verde antes do merge: **Settings › Branches › Add rule** em `develop` e `main`,
  marcando *Require status checks to pass*.

</details>

<img src="https://capsule-render.vercel.app/api?type=rect&color=0:003D74,100:EC7000&height=3&section=header" width="100%"/>

## 🤝 Como trabalhamos

```mermaid
gitGraph
    commit id: "main"
    branch develop
    checkout develop
    commit id: "integração"
    branch feature/minha-tarefa
    checkout feature/minha-tarefa
    commit id: "feat: ..."
    commit id: "test: ..."
    checkout develop
    merge feature/minha-tarefa id: "PR revisado + CI verde"
    checkout main
    merge develop id: "release do MVP"
```

- **Branches:** `main` é o que está no ar, `develop` é a integração. Cada tarefa vira uma branch
  `feature/...`, `fix/...` ou `docs/...` saindo da `develop`.
- **Commits:** [Conventional Commits](https://www.conventionalcommits.org/pt-br) — `feat:`, `fix:`, `docs:`,
  `refactor:`, `test:`, `build:`, `ci:`, `chore:`.
- **Pull Requests:** todo código entra por PR, com descrição, revisão do outro dev e CI verde.
- **Contrato primeiro:** endpoint novo é combinado no [contrato](backend/README_CONTRATO_API.md) antes de virar código.
- **Decisões:** escolhas importantes ficam em [planing/decisoes.md](planing/decisoes.md).

O passo a passo completo está no [CONTRIBUTING.md](CONTRIBUTING.md).

<img src="https://capsule-render.vercel.app/api?type=rect&color=0:003D74,100:EC7000&height=3&section=header" width="100%"/>

## 🗺️ Roadmap

<details open>
<summary><b>🚀 MVP — até 31/10/2026</b></summary>
<br/>

- [x] Backend compilando e rodando
- [x] Backend: cadastro de cliente + ViaCEP
- [x] Backend: abertura de conta
- [x] Backend: depósito, saque, transferência e extrato
- [x] Backend: logs rastreáveis em todas as camadas
- [x] Frontend: landing page e tela de cadastro
- [x] CI do backend no GitHub Actions
- [x] Roteiro de testes E2E com checklist
- [ ] Backend: `GET /contas/{id}` e `GET /clientes/{id}/contas`
- [ ] Frontend: telas de conta, movimentações e extrato
- [ ] Testes do Fellipe (PO & QA) e correções
- [ ] Deploy

</details>

<details>
<summary><b>🔭 Depois do MVP</b></summary>
<br/>

- [ ] Login com Spring Security + JWT (senha com BCrypt)
- [ ] Migrations com Flyway no lugar do `ddl-auto=update`
- [ ] Documentação viva com Swagger UI (springdoc-openapi)
- [ ] Testes de integração com Testcontainers + MySQL
- [ ] CI do frontend (`lint`, `typecheck`, `build`)
- [ ] Pix e cartão

</details>

O planejamento detalhado está em [planing/roadmap.md](planing/roadmap.md).

<img src="https://capsule-render.vercel.app/api?type=rect&color=0:003D74,100:EC7000&height=3&section=header" width="100%"/>

## 🗂️ Estrutura do repositório

<details>
<summary><b>Ver a árvore de pastas</b></summary>
<br/>

```text
rkf_bank/
├── .github/
│   ├── workflows/               # GitHub Actions (CI, CodeQL, Dependency Review)
│   └── dependabot.yml           # atualização automática de dependências
├── backend/
│   ├── src/main/java/...        # aplicação Spring Boot
│   ├── src/main/resources/      # application*.properties (MySQL / H2) e log4j2-spring.xml
│   ├── src/test/java/...        # testes unitários e de repositório (H2)
│   └── *.md                     # contrato da API, arquitetura, roteiro e checklist E2E
├── frontend/                    # aplicação Next.js (package.json próprio)
│   ├── app/                     # rotas: /, /cadastro, /login
│   ├── components/              # marketing, cadastro, ui, logs
│   └── lib/                     # api, logger (com máscara LGPD), formatação e máscaras
├── planing/                     # roadmap, decisões, recados e débitos técnicos do time
├── pom.xml                      # build do backend
├── mvnw, mvnw.cmd, .mvn/        # Maven Wrapper
├── run-test.sh                  # recria o MySQL e roda os testes
├── docker-compose.yml           # MySQL local
├── .env.example                 # variáveis do banco (copie para .env)
└── CONTRIBUTING.md              # como trabalhamos: branches, commits, PRs e revisão
```

</details>

## 📚 Documentação

| Área | Documento | O que tem |
|---|---|---|
| Time | [CONTRIBUTING.md](CONTRIBUTING.md) | branches, commits, Pull Requests e revisão |
| API | [Contrato da API](backend/README_CONTRATO_API.md) | rotas, exemplos de JSON, regras e erros |
| Backend | [backend/README.md](backend/README.md) | como rodar, testes, banco de dados e problemas comuns |
| Backend | [Arquitetura e implementação](backend/README_IMPLEMENTACAO.md) | camadas e trechos reais de cada uma |
| Backend | [Testes manuais](backend/README_TESTES_E2E.md) | roteiro completo, com o response de cada request |
| Backend | [Checklist E2E](backend/CHECKLIST_TESTES_E2E.md) | itens de validação do MVP, item a item |
| Frontend | [frontend/README.md](frontend/README.md) | como rodar e estrutura do frontend |
| Frontend | [Logs](frontend/lib/logger/README.md) | como registrar logs com dados mascarados |
| Planejamento | [planing/](planing/README.md) | roadmap, decisões e recados |
| Planejamento | [Débitos técnicos](planing/debitos-tecnicos.md) | o que funciona hoje, mas precisa melhorar |

<img src="https://capsule-render.vercel.app/api?type=rect&color=0:003D74,100:EC7000&height=3&section=header" width="100%"/>

## 👥 Quem faz o RKF Bank

O RKF Bank tem **dois responsáveis**, um para cada lado da aplicação.

<table>
  <tr>
    <td align="center" valign="top" width="50%">
      <a href="https://github.com/k3vinrich4rd">
        <img src="https://github.com/k3vinrich4rd.png" width="120" alt="Kevin Richard"/>
      </a>
      <h3>Kevin Richard</h3>
      <img src="https://img.shields.io/badge/Responsável_pelo-Backend-EC7000?logo=springboot&logoColor=white" alt="Responsável pelo backend"/>
      <br/><br/>
      <p align="left">
        <b>👋 Apresentação</b><br/>
        Dev backend do RKF Bank. Cuida de tudo o que roda atrás da tela: a API REST em Java e Spring Boot,
        o banco MySQL, a integração com o ViaCEP, os logs e os testes.
      </p>
      <p align="left">
        <b>🎯 Objetivo no projeto</b><br/>
        Garantir que o dinheiro de cada cliente esteja sempre certo: nenhuma operação pela metade, nenhum
        saldo errado, nenhum dado pessoal vazando — e que qualquer problema possa ser rastreado nos logs.
      </p>
      <p align="left">
        <b>🧱 Frentes</b><br/>
        Cadastro de clientes com ViaCEP · contas · depósito, saque, transferência e extrato · validação de CPF ·
        erros padronizados · logs e observabilidade · testes automatizados e roteiro E2E.
      </p>
      <p align="left"><b>📬 Contato</b></p>
      <a href="mailto:kevinrichardve@gmail.com"><img src="https://img.shields.io/badge/E--mail-kevinrichardve@gmail.com-EC7000?logo=gmail&logoColor=white" alt="E-mail de Kevin"/></a>
      <a href="https://github.com/k3vinrich4rd"><img src="https://img.shields.io/badge/GitHub-k3vinrich4rd-181717?logo=github&logoColor=white" alt="GitHub de Kevin"/></a>
    </td>
    <td align="center" valign="top" width="50%">
      <a href="https://github.com/renanbarrostech-cmd">
        <img src="https://github.com/renanbarrostech-cmd.png" width="120" alt="Renan Barros"/>
      </a>
      <h3>Renan Barros</h3>
      <img src="https://img.shields.io/badge/Responsável_pelo-Frontend-003D74?logo=nextdotjs&logoColor=white" alt="Responsável pelo frontend"/>
      <br/><br/>
      <p align="left">
        <b>👋 Apresentação</b><br/>
        Dev frontend do RKF Bank. Cuida de tudo o que o cliente vê e toca: o site em Next.js, React e
        TypeScript, a identidade visual da marca e a comunicação com a API.
      </p>
      <p align="left">
        <b>🎯 Objetivo no projeto</b><br/>
        Fazer o RKF Bank parecer e funcionar como um banco digital de verdade: telas claras, cadastro sem
        atrito, mensagens de erro que o cliente entende e dados pessoais sempre mascarados.
      </p>
      <p align="left">
        <b>🧱 Frentes</b><br/>
        Landing page institucional · identidade visual RKF · tela de cadastro · tela de login ·
        cliente HTTP da API · logs do frontend com máscara LGPD · planejamento e documentação do time.
      </p>
      <p align="left"><b>📬 Contato</b></p>
      <a href="mailto:renanbarros.tech@gmail.com"><img src="https://img.shields.io/badge/E--mail-renanbarros.tech@gmail.com-003D74?logo=gmail&logoColor=white" alt="E-mail de Renan"/></a>
      <a href="https://github.com/renanbarrostech-cmd"><img src="https://img.shields.io/badge/GitHub-renanbarrostech--cmd-181717?logo=github&logoColor=white" alt="GitHub de Renan"/></a>
    </td>
  </tr>
</table>

<div align="center">

| Papel | Pessoa | GitHub | Contato |
|---|---|---|---|
| 🧡 Responsável pelo backend | Kevin Richard | [@k3vinrich4rd](https://github.com/k3vinrich4rd) | [kevinrichardve@gmail.com](mailto:kevinrichardve@gmail.com) |
| 💙 Responsável pelo frontend | Renan Barros | [@renanbarrostech-cmd](https://github.com/renanbarrostech-cmd) | [renanbarros.tech@gmail.com](mailto:renanbarros.tech@gmail.com) |
| 🧪 PO & QA | Fellipe | — | nosso primeiro "cliente" — valida o MVP |

Dúvida sobre a API ou o banco? Fale com o **Kevin**. Sobre telas, layout ou o site? Fale com o **Renan**.
Bug ou sugestão? Abra uma [issue](https://github.com/k3vinrich4rd/rkf_bank/issues).

</div>

<details>
<summary><b>📊 Métricas de desenvolvimento</b></summary>
<br/>

<div align="center">

<b>Kevin Richard</b><br/>
<img src="https://github-readme-stats.vercel.app/api?username=k3vinrich4rd&show_icons=true&theme=tokyonight&include_all_commits=true&count_private=true&hide_border=true&rank_icon=github&bg_color=0d1117&title_color=EC7000&icon_color=003D74&text_color=ffffff" alt="GitHub Stats de Kevin" width="49%"/>
<img src="https://github-readme-streak-stats.herokuapp.com/?user=k3vinrich4rd&theme=tokyonight&hide_border=true&background=0d1117&ring=EC7000&fire=EC7000&currStreakLabel=EC7000&sideLabels=003D74" alt="GitHub Streak de Kevin" width="49%"/>

<b>Renan Barros</b><br/>
<img src="https://github-readme-stats.vercel.app/api?username=renanbarrostech-cmd&show_icons=true&theme=tokyonight&include_all_commits=true&count_private=true&hide_border=true&rank_icon=github&bg_color=0d1117&title_color=003D74&icon_color=EC7000&text_color=ffffff" alt="GitHub Stats de Renan" width="49%"/>
<img src="https://github-readme-streak-stats.herokuapp.com/?user=renanbarrostech-cmd&theme=tokyonight&hide_border=true&background=0d1117&ring=003D74&fire=003D74&currStreakLabel=003D74&sideLabels=EC7000" alt="GitHub Streak de Renan" width="49%"/>

</div>

</details>

<div align="center">

<a href="https://github.com/k3vinrich4rd/rkf_bank/graphs/contributors">
  <img src="https://contrib.rocks/image?repo=k3vinrich4rd/rkf_bank" alt="Contribuidores do RKF Bank"/>
</a>

Quer contribuir? Leia o [CONTRIBUTING.md](CONTRIBUTING.md) antes de abrir a primeira branch.

</div>

<img src="https://capsule-render.vercel.app/api?type=waving&color=0:EC7000,100:003D74&height=120&section=footer&text=S%C3%B3%20tem%20pr%C3%B3ximo%20n%C3%ADvel&fontSize=24&fontColor=ffffff&fontAlignY=70" width="100%" alt="Rodapé RKF Bank"/>

<div align="center">
  <small>RKF Bank — projeto colaborativo de estudo em engenharia de software.</small>
  <br/>
  <a href="#readme">⬆️ voltar ao topo</a>
</div>
