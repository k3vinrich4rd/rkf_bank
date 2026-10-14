<div align="center">

# 🏦 RKF Bank

**Banco digital de estudo, construído ponta a ponta por um time pequeno — com processo de time de verdade.**

![Java](https://img.shields.io/badge/Java-25-EC7000?logo=openjdk&logoColor=white)
![Spring Boot](https://img.shields.io/badge/Spring_Boot-4.1-003D74?logo=springboot&logoColor=white)
![MySQL](https://img.shields.io/badge/MySQL-8.4-003D74?logo=mysql&logoColor=white)
![Next.js](https://img.shields.io/badge/Next.js-16-EC7000?logo=nextdotjs&logoColor=white)
![TypeScript](https://img.shields.io/badge/TypeScript-5-003D74?logo=typescript&logoColor=white)
![E2E](https://img.shields.io/badge/E2E-38%2F38_✓-003D74)
![Status](https://img.shields.io/badge/status-em_desenvolvimento-EC7000)

**Responsáveis pelo projeto:** [Kevin Richard](https://github.com/k3vinrich4rd) (backend) ·
[Renan Barros](https://github.com/renanbarrostech-cmd) (frontend)

[Sobre](#-sobre-o-projeto) · [Arquitetura](#️-arquitetura-em-1-minuto) · [Rodando](#-rodando-o-projeto-do-zero) ·
[Testando](#-testando-o-projeto) · [Documentação](#-documentação) · [Contribuidores](#-contribuidores)

</div>

<img src="https://capsule-render.vercel.app/api?type=rect&color=0:003D74,100:EC7000&height=3&section=header" width="100%"/>

## 🧭 Sobre o projeto

O **RKF Bank** é uma aplicação bancária completa (API + site) criada para praticar, na vida real,
o que um time de engenharia faz no dia a dia: combinar um **contrato de API**, trabalhar em **branches e
Pull Requests**, registrar **decisões**, escrever **testes** e entregar um **MVP com prazo**.

> *Conhecimento não tem teto. Código não tem limite. Só tem próximo nível.*

**Meta atual:** 🚀 MVP no ar até **31/10/2026** — ver [roadmap](planing/roadmap.md).

### O que o MVP entrega

| Funcionalidade | Backend | Frontend |
|---|:---:|:---:|
| Landing page institucional | — | ✅ |
| Cadastro de cliente com endereço via CEP (ViaCEP) | ✅ | ✅ |
| Abertura de conta (corrente ou poupança) | ✅ | ⏳ |
| Depósito, saque e transferência | ✅ | ⏳ |
| Extrato da conta | ✅ | ⏳ |
| Tela de saldo da conta | ⏳ (falta `GET /contas/{id}`) | ⏳ |
| Login | ⏳ (pós-MVP / a decidir) | 🟡 placeholder |

✅ pronto · 🟡 parcial · ⏳ a fazer

### ⭐ Destaques técnicos

- **Dinheiro tratado com cuidado:** `BigDecimal`, transações (tudo ou nada) e proteção contra operações
  simultâneas na mesma conta (*lock* otimista).
- **Contrato de API primeiro:** frontend e backend trabalham em paralelo a partir do mesmo documento.
- **Arquitetura em camadas:** Controller → Service → Repository, com DTOs e erros padronizados.
- **Integração externa resiliente:** consulta de CEP com *timeout* e tratamento de falhas do ViaCEP.
- **Logs rastreáveis:** Log4j2 em todas as camadas do backend; cada requisição ganha um `X-Request-Id` que
  aparece em todas as linhas de log dela, inclusive no SQL.
- **LGPD:** o backend nunca registra CPF nem e-mail em log; o frontend mascara CPF, e-mail e telefone.
- **Testes automatizados** em todas as camadas do backend, com relatório de cobertura (JaCoCo), e roteiro
  E2E manual com [checklist de validação](backend/CHECKLIST_TESTES_E2E.md).
- **Processo de time:** Git Flow, Conventional Commits, Pull Requests com revisão e decisões registradas.

<img src="https://capsule-render.vercel.app/api?type=rect&color=0:003D74,100:EC7000&height=3&section=header" width="100%"/>

## 🏗️ Arquitetura em 1 minuto

```mermaid
flowchart LR
    U([Usuário]) -->|navegador| FE

    subgraph FE [Frontend · Next.js :3000]
        P[Páginas / telas] --> H[Hooks de tela]
        H --> API["lib/api<br/>(clientesApi, contasApi)"]
    end

    API -->|"/api/... (proxy do Next)"| BE

    subgraph BE [Backend · Spring Boot :8080]
        F[Filtro de log<br/>X-Request-Id] --> C[Controllers]
        C --> S[Services]
        S --> R[Repositories]
        S --> V[Client ViaCEP]
    end

    R --> DB[(MySQL 8.4<br/>Docker :5441)]
    V -->|HTTPS| VC[[viacep.com.br]]
```

- O **navegador nunca fala direto com o backend**: ele chama `/api/...` no próprio Next, que repassa para
  `http://localhost:8080` (configurado em [`frontend/next.config.ts`](frontend/next.config.ts)).
- O **backend é um monólito em camadas** (Controller → Service → Repository). Detalhes em
  [backend/README_IMPLEMENTACAO.md](backend/README_IMPLEMENTACAO.md).
- A **fronteira entre os dois lados** é o [contrato de API](backend/README_CONTRATO_API.md). Mudou um endpoint?
  Muda o contrato **no mesmo PR**.

<img src="https://capsule-render.vercel.app/api?type=rect&color=0:003D74,100:EC7000&height=3&section=header" width="100%"/>

## 🚀 Rodando o projeto do zero

### Pré-requisitos

| Ferramenta | Versão | Para quê |
|---|---|---|
| [JDK](https://learn.microsoft.com/java/openjdk/download) | **25** (exatamente — o Maven bloqueia outras versões) | backend |
| [Docker Desktop](https://www.docker.com/products/docker-desktop/) | recente | banco MySQL |
| [Node.js](https://nodejs.org/) | 20 ou superior | frontend |
| [pnpm](https://pnpm.io/installation) | 10 (`corepack enable` já resolve) | frontend |

> Maven **não** precisa ser instalado: o projeto usa o Maven Wrapper (`mvnw` / `mvnw.cmd`).
> Confira o Java com `java -version` (deve mostrar `25.x`) e garanta que o `JAVA_HOME` aponta para o JDK 25.

### 1. Banco de dados

Na **raiz** do repositório:

```bash
cp .env.example .env          # PowerShell: Copy-Item .env.example .env
docker compose up -d          # sobe o MySQL na porta 5441
docker compose ps             # espere o status "healthy"
```

### 2. Backend (porta 8080)

O Spring **não lê o arquivo `.env` sozinho** — as variáveis precisam estar no ambiente do terminal.
Ainda na raiz:

```powershell
# PowerShell: carrega o .env no terminal atual e sobe a API
Get-Content .env | Where-Object { $_ -match '^\s*[^#].*=' } | ForEach-Object {
  $nome, $valor = $_ -split '=', 2; Set-Item "env:$nome" $valor
}
.\mvnw.cmd spring-boot:run
```

```bash
# Git Bash / Linux / macOS
set -a && source .env && set +a
./mvnw spring-boot:run
```

Teste rápido: `curl http://localhost:8080/clientes` deve responder `200` com um array (`[]` no banco vazio).

> Sem Docker? Suba com H2 em memória: `./mvnw spring-boot:run -Dspring-boot.run.profiles=test`
> (os dados somem ao parar a API). No IntelliJ, dá para usar o plugin **EnvFile** apontando para o `.env`.

### 3. Frontend (porta 3000)

```bash
cd frontend
cp .env.example .env.local    # PowerShell: Copy-Item .env.example .env.local
pnpm install
pnpm dev
```

Abra <http://localhost:3000>. A tela de cadastro fica em <http://localhost:3000/cadastro>.

Algo deu errado? Veja a seção **Problemas comuns** em [backend/README.md](backend/README.md#-problemas-comuns)
e [frontend/README.md](frontend/README.md#-problemas-comuns).

<img src="https://capsule-render.vercel.app/api?type=rect&color=0:003D74,100:EC7000&height=3&section=header" width="100%"/>

## 🧪 Testando o projeto

### Testes automatizados (backend)

Na raiz do repositório:

```bash
./mvnw clean verify -Dspring.profiles.active=test   # sem Docker (H2) — PowerShell: .\mvnw.cmd clean verify "-Dspring.profiles.active=test"
./mvnw clean verify                                 # com MySQL no ar e .env carregado
./run-test.sh                                       # recria o MySQL (APAGA os dados) e roda os testes
```

Cobertura: `target/site/jacoco/index.html`. Detalhes de cada classe de teste em
[backend/README.md](backend/README.md#-testes).

### Teste manual ponta a ponta

1. Suba banco e API (seção anterior).
2. Siga o [roteiro E2E](backend/README_TESTES_E2E.md): caminho feliz (cliente → conta → depósito → saque →
   transferência → extrato) e 18 cenários de erro.
3. Registre o resultado no [checklist](backend/CHECKLIST_TESTES_E2E.md).

**Última execução completa:** 13/10/2026 · Java 25 + MySQL 8.4 · **38/38 itens aprovados**.

### Frontend

```bash
cd frontend
pnpm lint
```

<img src="https://capsule-render.vercel.app/api?type=rect&color=0:003D74,100:EC7000&height=3&section=header" width="100%"/>

## 🗂️ Estrutura do repositório

```text
rkf_bank/
├── backend/                     # código e docs do backend (Java/Spring)
│   ├── src/main/java/...        # aplicação
│   ├── src/main/resources/      # application*.properties e log4j2-spring.xml
│   ├── src/test/java/...        # testes unitários
│   └── *.md                     # docs do backend (contrato, arquitetura, roteiro e checklist E2E)
├── frontend/                    # aplicação Next.js (projeto independente, com package.json próprio)
├── planing/                     # roadmap, decisões, recados e débitos técnicos do time
├── pom.xml                      # build do backend (fica na raiz — ver planing/debitos-tecnicos.md)
├── mvnw, mvnw.cmd, .mvn/        # Maven Wrapper
├── run-test.sh                  # recria o MySQL e roda os testes
├── docker-compose.yml           # MySQL local
├── .env.example                 # variáveis do banco (copie para .env)
└── CONTRIBUTING.md              # como trabalhamos: branches, commits, PRs, revisão
```

<img src="https://capsule-render.vercel.app/api?type=rect&color=0:003D74,100:EC7000&height=3&section=header" width="100%"/>

## 📚 Documentação

| Área | Documento | O que tem |
|---|---|---|
| Time | [CONTRIBUTING.md](CONTRIBUTING.md) | branches, commits, Pull Requests e revisão |
| API | [Contrato da API](backend/README_CONTRATO_API.md) | rotas, exemplos de JSON, regras e erros |
| Backend | [backend/README.md](backend/README.md) | como rodar, testes e banco de dados |
| Backend | [Arquitetura](backend/README_IMPLEMENTACAO.md) | camadas, decisões técnicas e como criar um endpoint |
| Backend | [Testes manuais](backend/README_TESTES_E2E.md) | roteiro completo para Postman/Insomnia/curl |
| Backend | [Checklist E2E](backend/CHECKLIST_TESTES_E2E.md) | itens de validação do MVP e resultado da última execução |
| Frontend | [frontend/README.md](frontend/README.md) | como rodar, estrutura e como criar uma tela |
| Frontend | [Logs](frontend/lib/logger/README.md) | como registrar logs com dados mascarados |
| Planejamento | [planing/](planing/README.md) | roadmap, decisões e débitos técnicos |

<img src="https://capsule-render.vercel.app/api?type=rect&color=0:003D74,100:EC7000&height=3&section=header" width="100%"/>

## 👥 Contribuidores

O RKF Bank tem dois responsáveis, um para cada lado da aplicação: **Kevin Richard** responde pelo **backend**
e **Renan Barros** responde pelo **frontend**.

<table>
  <tr>
    <td align="center" width="200">
      <a href="https://github.com/k3vinrich4rd">
        <img src="https://github.com/k3vinrich4rd.png" width="96" style="border-radius:50%" alt="Kevin Richard"/><br/>
        <b>Kevin Richard</b>
      </a><br/>
      <sub><b>Responsável pelo backend</b></sub><br/>
      <sub>API, regras de negócio, banco, logs e testes</sub>
    </td>
    <td align="center" width="200">
      <a href="https://github.com/renanbarrostech-cmd">
        <img src="https://github.com/renanbarrostech-cmd.png" width="96" style="border-radius:50%" alt="Renan Barros"/><br/>
        <b>Renan Barros</b>
      </a><br/>
      <sub><b>Responsável pelo frontend</b></sub><br/>
      <sub>landing page, identidade visual, tela de cadastro, cliente HTTP da API, logs do frontend, planejamento e revisão de PRs</sub>
    </td>
    <td align="center" width="200">
      <b>Fellipe</b><br/>
      <sub>PO &amp; QA</sub><br/>
      <sub>nosso primeiro "cliente" — valida o MVP pelo <a href="backend/CHECKLIST_TESTES_E2E.md">checklist</a></sub>
    </td>
  </tr>
</table>

Quer contribuir? Leia o [CONTRIBUTING.md](CONTRIBUTING.md) antes de abrir a primeira branch.

---

<div align="center">
  <small>RKF Bank — projeto colaborativo de estudo em engenharia de software.</small>
</div>
