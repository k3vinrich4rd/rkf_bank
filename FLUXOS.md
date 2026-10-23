# 🗺️ RKF Bank — Atlas de fluxos

Como cada parte do RKF Bank se conecta, do clique na tela até a linha no banco. Os diagramas são **código**
([Mermaid](https://mermaid.js.org/)): o GitHub desenha sozinho, eles mudam no mesmo PR que o código e passam por revisão
como qualquer arquivo. A organização segue o **modelo C4** (contexto → containers → componentes), o padrão de mercado
para documentar arquitetura em níveis de zoom.

| Sou... | Comece por |
|---|---|
| 👀 visitante, recrutador(a), curioso(a) | [1. O RKF Bank em uma imagem](#1-o-rkf-bank-em-uma-imagem) → [2. As telas](#2-as-telas-e-o-que-cada-uma-chama) → [3. Uma transferência por dentro](#3-uma-transferência-por-dentro-árvore-de-decisão) |
| 🧑‍💻 do time (ou chegando nele) | [4. Containers](#4-containers-o-que-roda-onde) → [5. Componentes](#5-componentes-do-backend) → [6. Rastreabilidade](#6-rastreabilidade-da-tela-à-tabela) → [7. Sessão](#7-sessão-login-cookie-e-proxy) → [8. Transferência](#8-transferência-ponta-a-ponta) → [10. Como o time trabalha](#10-como-o-time-trabalha) |
| 🧪 vou testar | [9. Qual perfil subir?](#9-qual-perfil-subir) e o [guia de testes](backend/README_TESTES_E2E.md) |

Legenda das cores: 🟦 o que é nosso · 🟩 banco de dados · 🟧 serviço de terceiros · ⬜ pessoas.

---

## 1. O RKF Bank em uma imagem

**C4 nível 1 (contexto):** quem usa o sistema e com quem ele conversa. Nada de tecnologia ainda.

```mermaid
flowchart LR
    classDef pessoa fill:#f4f4f5,stroke:#71717a,color:#18181b
    classDef nosso fill:#dbeafe,stroke:#2563eb,color:#1e3a8a
    classDef terceiro fill:#ffedd5,stroke:#ea580c,color:#7c2d12

    C(["👤 Cliente<br/>abre conta, movimenta,<br/>vê o extrato"]):::pessoa
    A(["🛡️ Admin<br/>valida usuários,<br/>promove, desativa"]):::pessoa
    V(["🧪 Visitante<br/>testa a API na própria<br/>máquina (perfil visitante)"]):::pessoa

    RKF["🏦 RKF Bank<br/>banco digital: cadastro, contas,<br/>depósito, saque, transferência, extrato"]:::nosso

    VC["📮 ViaCEP<br/>endereço oficial a partir do CEP"]:::terceiro
    GH["🐙 GitHub<br/>código, PRs, CI, Dependabot"]:::terceiro

    C -->|"site (Next.js)"| RKF
    A -->|"API (Postman/curl)"| RKF
    V -->|"API REST"| RKF
    RKF -->|"HTTPS, timeout 3 s"| VC
    RKF -. "build, testes, CodeQL<br/>a cada PR" .- GH
```

---

## 2. As telas e o que cada uma chama

Mapa de navegação do site, com a rota da API que cada tela usa. 🔒 = precisa estar logado (o `proxy.ts` do Next manda
para o login, e volta para a mesma tela depois).

```mermaid
flowchart TB
    classDef publica fill:#f4f4f5,stroke:#71717a,color:#18181b
    classDef privada fill:#dbeafe,stroke:#2563eb,color:#1e3a8a
    classDef api fill:#ecfdf5,stroke:#059669,color:#064e3b

    L["/ · landing"]:::publica --> CAD["/cadastro"]:::publica
    L --> LOG["/login"]:::publica
    CAD -->|"cadastrou: login automático"| ABR["🔒 /conta/abrir"]:::privada
    LOG -->|"volta para ?voltar= ou /conta"| CONTA["🔒 /conta<br/>saldo e ações"]:::privada
    CONTA -->|"ainda sem conta:<br/>Falta abrir sua conta"| ABR
    ABR --> CONTA
    CONTA --> DEP["🔒 /conta/depositar"]:::privada
    CONTA --> SAQ["🔒 /conta/sacar"]:::privada
    CONTA --> TRF["🔒 /conta/transferir"]:::privada
    CONTA --> EXT["🔒 /conta/extrato"]:::privada
    CONTA --> SEN["🔒 /conta/senha"]:::privada
    SEN -->|"204: todo token antigo cai"| LOG
    CONTA -->|"sair"| L

    CAD -.-> A1["POST /clientes<br/>POST /auth/login"]:::api
    LOG -.-> A2["POST /auth/login"]:::api
    ABR -.-> A3["POST /contas/clientes/{id}"]:::api
    CONTA -.-> A4["GET /contas"]:::api
    DEP -.-> A5["POST /contas/{id}/depositos<br/>+ Idempotency-Key"]:::api
    SAQ -.-> A6["POST /contas/{id}/saques<br/>+ Idempotency-Key"]:::api
    TRF -.-> A7["POST /transferencias<br/>+ Idempotency-Key"]:::api
    EXT -.-> A8["GET /contas/{id}/lancamentos"]:::api
    SEN -.-> A9["PATCH /usuarios/me/senha"]:::api
```

O admin não tem tela (decisão do time): usa `GET/PATCH /admin/usuarios` pelo Postman ou curl
([guia de testes, seção 8](backend/README_TESTES_E2E.md)).

---

## 3. Uma transferência por dentro (árvore de decisão)

Toda transferência passa por estas perguntas, **nesta ordem**. A primeira que falhar decide a resposta, e nada é
movimentado. Por que nessa ordem: o barato e o que não revela informação vem primeiro (quem é você? o pedido está bem
escrito?), o que depende do banco vem depois.

```mermaid
flowchart TD
    S(["POST /transferencias"]) --> Q1{"Tem token válido<br/>e o usuário ainda está ativo?"}
    Q1 -- não --> R401["401<br/>faça login de novo"]
    Q1 -- sim --> Q2{"Corpo bem formado?<br/>valor > 0, 2 casas, UUIDs"}
    Q2 -- não --> R400["400<br/>+ campos com erro"]
    Q2 -- sim --> Q3{"Tem Idempotency-Key<br/>no formato certo?"}
    Q3 -- não --> R400b["400<br/>Idempotency-Key obrigatória"]
    Q3 -- sim --> Q4{"Essa chave já foi usada?"}
    Q4 -- "sim, mesma operação" --> R204r["204 + Idempotency-Replayed<br/>nada movimenta de novo"]
    Q4 -- "sim, outra operação" --> R422k["422<br/>gere uma chave nova"]
    Q4 -- não --> Q5{"Origem e destino<br/>são a mesma conta?"}
    Q5 -- sim --> R422m["422<br/>mesma conta"]
    Q5 -- não --> Q6{"A conta de origem<br/>é sua?"}
    Q6 -- não --> R403["403<br/>conta de outro cliente"]
    Q6 -- sim --> Q7{"As duas existem<br/>e estão ativas?"}
    Q7 -- não --> R404["404 não existe<br/>422 inativa"]
    Q7 -- sim --> Q8{"Saldo suficiente?"}
    Q8 -- não --> R422s["422<br/>saldo insuficiente"]
    Q8 -- sim --> Q9{"Alguém mexeu na conta<br/>no mesmo instante?<br/>(@Version)"}
    Q9 -- sim --> R409["409<br/>tente de novo"]
    Q9 -- não --> OK(["✅ 204<br/>débito + crédito + lançamento + chave<br/>na MESMA transação"])
```

---

## 4. Containers: o que roda onde

**C4 nível 2.** Cada caixa é algo que roda separado (um processo, um banco). O navegador **nunca** fala direto com o
backend e **nunca** vê o token.

```mermaid
flowchart LR
    classDef pessoa fill:#f4f4f5,stroke:#71717a,color:#18181b
    classDef nosso fill:#dbeafe,stroke:#2563eb,color:#1e3a8a
    classDef dados fill:#dcfce7,stroke:#16a34a,color:#14532d
    classDef terceiro fill:#ffedd5,stroke:#ea580c,color:#7c2d12

    U(["👤 Navegador"]):::pessoa

    subgraph FE ["Frontend · Next.js 16 + React 19 · :3000"]
        PG["Telas (Server Components)<br/>+ screens (Client Components)"]:::nosso
        RH["Route Handlers<br/>/api/auth/login · logout · senha · sessao"]:::nosso
        PX["proxy.ts<br/>cookie httpOnly → Authorization: Bearer"]:::nosso
    end

    subgraph BE ["Backend · Spring Boot 4.1 + Java 25 · :8080"]
        API["API REST<br/>14 rotas · JWT · regras do banco"]:::nosso
    end

    DB[("MySQL 8.4<br/>schema pelo Flyway")]:::dados
    H2[("H2 em memória<br/>testes e perfis visitante/equipe")]:::dados
    VC["ViaCEP"]:::terceiro

    U -->|"HTML + JS"| PG
    U -->|"fetch /api/..."| PX
    U -->|"login / sair"| RH
    RH -->|"POST /auth/login<br/>guarda o token no cookie"| API
    PX -->|"rewrite /api/x → /x<br/>+ Bearer"| API
    API -->|"JPA / JDBC"| DB
    API -.->|"testes"| H2
    API -->|"HTTPS"| VC
```

---

## 5. Componentes do backend

**C4 nível 3.** As camadas do Spring e quem pode chamar quem. A seta só desce: controller não fala com repository,
repository não conhece HTTP. O porquê de cada camada está no [guia de testes, seção 9](backend/README_TESTES_E2E.md).

```mermaid
flowchart TB
    classDef borda fill:#fef9c3,stroke:#ca8a04,color:#713f12
    classDef web fill:#dbeafe,stroke:#2563eb,color:#1e3a8a
    classDef regra fill:#ede9fe,stroke:#7c3aed,color:#4c1d95
    classDef dados fill:#dcfce7,stroke:#16a34a,color:#14532d
    classDef terceiro fill:#ffedd5,stroke:#ea580c,color:#7c2d12

    subgraph BORDA ["Borda (roda antes dos controllers)"]
        FLOG["FiltroLogRequisicao<br/>X-Request-Id"]:::borda
        SEC["ConfiguracaoSeguranca<br/>Resource Server JWT"]:::borda
        CONV["ConversorTokenUsuario<br/>usuário ativo? mesmo perfil?"]:::borda
        RES["TratadorErrosSeguranca<br/>401 / 403"]:::borda
    end

    subgraph WEB ["controllers + dto"]
        AUTHC["AuthController<br/>/auth"]:::web
        CLIC["ClienteController<br/>/clientes"]:::web
        CONC["ContaController<br/>/contas"]:::web
        ADMC["AdminUsuarioController<br/>/admin/usuarios"]:::web
        EH["TratadorGlobalDeErros<br/>formato único de erro"]:::web
    end

    subgraph REGRA ["services"]
        AUTHS["AutenticacaoService + TokenService<br/>+ LimitadorTentativasLogin"]:::regra
        CLIS["ClienteService"]:::regra
        CONS["ContaService"]:::regra
        IDEM["IdempotenciaService"]:::regra
        GES["GestaoUsuarioService"]:::regra
        USU["UsuarioService<br/>admin e equipe na subida"]:::regra
    end

    subgraph DADOS ["repositories + entities"]
        RCLI["ClienteRepository"]:::dados
        RCON["ContaRepository<br/>Conta @Version"]:::dados
        RLAN["LancamentoRepository"]:::dados
        RUSU["UsuarioRepository"]:::dados
        RIDE["IdempotenciaRepository"]:::dados
    end

    VIA["ViaCepClient<br/>RestClient + timeout"]:::terceiro

    FLOG --> SEC --> CONV
    SEC -.-> RES
    CONV --> RUSU
    SEC --> AUTHC & CLIC & CONC & ADMC
    AUTHC --> AUTHS
    CLIC --> CLIS
    CONC --> IDEM --> CONS
    ADMC --> GES
    AUTHS --> RUSU
    CLIS --> RCLI & RUSU & VIA
    CONS --> RCON & RLAN
    IDEM --> RIDE
    GES --> RUSU
    USU --> RUSU
    AUTHC & CLIC & CONC & ADMC -.->|"exceções"| EH
```

---

## 6. Rastreabilidade: da tela à tabela

Para achar rápido **onde mexer**. Cada linha é um caminho completo; a coluna "regra" diz onde mora a decisão.

| Tela | Hook (frontend) | `lib/api` | Rota | Controller | Service | Tabelas | Regra principal |
|---|---|---|---|---|---|---|---|
| `/cadastro` | `use-cadastro-form` | `clientesApi.cadastrar` → `authApi.login` | `POST /clientes` | `ClienteController.cadastrar` | `ClienteService.cadastrar` (+ ViaCEP) | `tb_endereco`, `tb_cliente`, `tb_usuario` | CPF e e-mail únicos (409), CEP existe (422) |
| `/login` | `use-login` | `authApi.login` (Route Handler do Next) | `POST /auth/login` | `AuthController.login` | `AutenticacaoService.login` + `TokenService` | `tb_usuario` | mesma resposta para CPF inexistente e senha errada; 5 erros → 429 |
| `/conta/abrir` | `use-abrir-conta` | `contasApi.abrir` | `POST /contas/clientes/{id}` | `ContaController.abrir` | `ContaService.abrir` | `tb_conta` | só no próprio cadastro (403); número com DV gerado |
| `/conta` | `use-conta` | `contasApi.minhas` | `GET /contas` | `ContaController.listar` | `ContaService.listarTodas` | `tb_conta` | cliente vê só as dele; admin vê todas |
| `/conta/depositar` · `/sacar` | `use-movimentacao` | `contasApi.depositar` · `sacar` | `POST /contas/{id}/depositos` · `/saque` | `ContaController.depositar` · `sacar` | `IdempotenciaService` → `ContaService` | `tb_conta`, `tb_lancamento`, `tb_idempotencia` | dono (403), ativa (422), saldo (422), uma vez por chave |
| `/conta/transferir` | `use-movimentacao` | `contasApi.transferir` | `POST /transferencias` | `ContaController.transferir` | `IdempotenciaService` → `ContaService.transferir` | idem | [árvore da seção 3](#3-uma-transferência-por-dentro-árvore-de-decisão) |
| `/conta/extrato` | `use-extrato` | `contasApi.extrato` | `GET /contas/{id}/lancamentos` | `ContaController.extrato` | `ContaService.listarLancamentos` | `tb_lancamento` | dono (403); mais novo primeiro |
| `/conta/senha` | `use-trocar-senha` | `authApi.trocarSenha` (Route Handler) | `PATCH /usuarios/me/senha` | `AuthController.trocarSenha` | `AutenticacaoService.trocarSenha` | `tb_usuario` | pede a atual; derruba todos os tokens |
| — (admin) | — | — | `/admin/usuarios/**` | `AdminUsuarioController` | `GestaoUsuarioService` | `tb_usuario` | ninguém mexe em si mesmo (422) |

---

## 7. Sessão: login, cookie e proxy

Por que tanta volta: o token num **cookie `httpOnly`** não pode ser lido por JavaScript, então um script injetado na
página (XSS) não consegue roubá-lo. Quem coloca o token na chamada é o servidor do Next.

```mermaid
sequenceDiagram
    autonumber
    actor U as Navegador
    participant RH as Next · /api/auth/login
    participant PX as Next · proxy.ts
    participant API as Spring · API
    participant DB as MySQL

    U->>RH: POST {cpf, senha}
    RH->>API: POST /auth/login
    API->>DB: busca tb_usuario e confere o BCrypt
    API-->>RH: 200 {token, perfil, clienteId}
    RH-->>U: Set-Cookie rkf_token (httpOnly, sameSite) + sessão sem o token
    Note over U: o JavaScript da página nunca vê o token

    U->>PX: GET /api/contas (cookie vai sozinho)
    PX->>PX: lê o cookie, apaga cookies do pedido, põe Authorization: Bearer
    PX->>API: GET /contas
    API->>API: valida assinatura e validade + ConversorTokenUsuario confere tb_usuario
    alt token ok
        API-->>U: 200 contas
    else vencido, desativado ou senha trocada
        API-->>U: 401
        U->>U: tela manda para /login?voltar=/conta
    end
```

---

## 8. Transferência ponta a ponta

O caminho feliz por dentro do backend, com as duas proteções que um banco precisa: **não repetir** (idempotência) e
**não sobrescrever** (lock otimista), tudo numa transação só.

```mermaid
sequenceDiagram
    autonumber
    participant C as ContaController
    participant I as IdempotenciaService
    participant S as ContaService
    participant R as ContaRepository
    participant DB as MySQL

    C->>I: executarUmaVez(usuário, chave, TRANSFERENCIA, assinatura)
    Note over I,DB: @Transactional começa aqui: tudo abaixo é tudo ou nada
    I->>DB: chave já existe para este usuário?
    DB-->>I: não
    I->>S: transferir(...)
    S->>R: busca origem (dono?) e destino (ativa?)
    S->>S: saldo suficiente?
    S->>R: save(origem − valor) · save(destino + valor)
    S->>DB: INSERT tb_lancamento
    I->>DB: INSERT tb_idempotencia (UNIQUE usuario_id + chave)
    Note over I,DB: commit: UPDATE ... WHERE versao = ? nas duas contas
    alt outra operação mudou a conta antes
        DB-->>C: 0 linhas → rollback → 409
    else tudo certo
        DB-->>C: commit
        C-->>C: 204 No Content
    end
```

---

## 9. Qual perfil subir?

```mermaid
flowchart TD
    Q{"O que eu quero?"} -->|"conhecer a API sem instalar nada<br/>além do JDK 25"| VIS["perfil visitante<br/>H2 · Ana e Bruno · segredo sorteado<br/>sem admin"]
    Q -->|"testar como admin<br/>ou com as contas da equipe"| EQ["perfil equipe<br/>H2 · Felipe e Renan · admin do .env<br/>(ou das variáveis do terminal)"]
    Q -->|"desenvolver com o banco real"| PAD["perfil padrão<br/>MySQL 8.4 no Docker · .env completo"]
    Q -->|"rodar a suíte automática"| TST["perfil test<br/>scripts/validar.sh backend<br/>H2 · sem seeds"]
    VIS --> G1["backend/README_TESTES_E2E.md · seções 1 a 7"]
    EQ --> G2["backend/README_TESTES_E2E.md · seção 8"]
    PAD --> G3["backend/README.md · Como rodar"]
```

---

## 10. Como o time trabalha

Do item no checklist até a `main`. As *skills* do Claude Code (`.claude/skills/`) automatizam os passos repetitivos;
os scripts determinísticos rodam antes de qualquer IA.

```mermaid
flowchart LR
    classDef plano fill:#fef9c3,stroke:#ca8a04,color:#713f12
    classDef faz fill:#dbeafe,stroke:#2563eb,color:#1e3a8a
    classDef valida fill:#dcfce7,stroke:#16a34a,color:#14532d
    classDef entrega fill:#ede9fe,stroke:#7c3aed,color:#4c1d95

    CK["planing/checklist-features.md<br/>débitos · decisões · recados"]:::plano --> BR["branch nova da origin/develop<br/>feature/ fix/ docs/ chore/"]:::faz
    BR --> IMP["/implementar-feature · /nova-tela<br/>/explicar-codigo"]:::faz
    IMP --> VAL["scripts/validar.sh<br/>/revisar-migracao · /verificar-contrato<br/>/varrer-vulnerabilidades · /testar-e2e"]:::valida
    VAL --> ENT["/entregar<br/>commits semânticos + PR"]:::entrega
    ENT --> CI["GitHub Actions<br/>Backend CI · CodeQL · Dependency Review"]:::valida
    CI --> REV["revisão do outro dev"]:::entrega
    REV --> DEV["develop"]:::entrega
    DEV -->|"/notas-de-versao"| MAIN["main"]:::entrega
    ENT -.->|"/recado"| CK
```

```mermaid
gitGraph
    commit id: "main estável"
    branch develop
    checkout develop
    commit id: "base"
    branch feature/x
    checkout feature/x
    commit id: "feat: regra"
    commit id: "test: cenários"
    commit id: "docs: contrato"
    checkout develop
    merge feature/x id: "PR revisado + CI verde"
    branch fix/y
    checkout fix/y
    commit id: "fix: correção"
    checkout develop
    merge fix/y
    checkout main
    merge develop id: "release + notas de versão"
```

---

## Outros diagramas do projeto

| Diagrama | Onde |
|---|---|
| Jornada do cliente (do cadastro ao extrato) e saldo a cada lançamento | [README → A experiência](README.md#-a-experiência) |
| Os portões de segurança do JWT e o ataque de força bruta | [README → Segurança e LGPD](README.md#-segurança-e-lgpd) |
| Arquitetura resumida e animação da transferência | [README → Arquitetura](README.md#-arquitetura) |
| Modelo de dados (ER) e linha do tempo das migrações | [README → Arquitetura](README.md#-arquitetura) |
| Caminho de uma requisição autenticada (sequência) | [Guia de testes → seção 3.3](backend/README_TESTES_E2E.md) |
| Onde cada cenário de teste é barrado (camadas) | [Guia de testes → seção 9](backend/README_TESTES_E2E.md) |
| Ciclo de vida de um usuário (promover, rebaixar, desativar) | [Guia de testes → seção 8.1](backend/README_TESTES_E2E.md) |
