<div align="center">

<!-- Os SVGs de assets/readme/ são gerados por scripts/gerar-svgs-readme.mjs (rode de novo quando mudar testes ou migrações). -->
<a href="#-conheça-o-rkf-bank"><img src="assets/readme/banner.svg" width="100%" alt="RKF Banco Digital: conta digital com depósito, saque, transferência e extrato. Confiança, tecnologia e liberdade."/></a>

[![Backend CI](https://github.com/k3vinrich4rd/rkf_bank/actions/workflows/backend-ci.yml/badge.svg?branch=develop)](https://github.com/k3vinrich4rd/rkf_bank/actions/workflows/backend-ci.yml)
[![Backend CodeQL](https://github.com/k3vinrich4rd/rkf_bank/actions/workflows/backend-codeql.yml/badge.svg?branch=develop)](https://github.com/k3vinrich4rd/rkf_bank/actions/workflows/backend-codeql.yml)
[![Último commit](https://img.shields.io/github/last-commit/k3vinrich4rd/rkf_bank/develop?color=3B82F6&label=último%20commit)](https://github.com/k3vinrich4rd/rkf_bank/commits/develop)
[![PRs fechados](https://img.shields.io/github/issues-pr-closed/k3vinrich4rd/rkf_bank?color=06B6D4&label=PRs%20fechados)](https://github.com/k3vinrich4rd/rkf_bank/pulls?q=is%3Apr+is%3Aclosed)
[![Status](https://img.shields.io/badge/MVP-31%2F10%2F2026-3B82F6)](planing/roadmap.md)
[![Feito em Osasco](https://img.shields.io/badge/📍_feito_em-Osasco,_SP-06B6D4)](#-feito-em-osasco)

[![Java](https://img.shields.io/badge/Java-25_LTS-0A0F1C?logo=openjdk&logoColor=white)](https://openjdk.org/projects/jdk/25/)
[![Spring Boot](https://img.shields.io/badge/Spring_Boot-4.1-0A0F1C?logo=springboot&logoColor=6DB33F)](https://spring.io/projects/spring-boot)
[![Spring Security](https://img.shields.io/badge/Spring_Security-JWT-0A0F1C?logo=springsecurity&logoColor=6DB33F)](https://docs.spring.io/spring-security/reference/)
[![Flyway](https://img.shields.io/badge/Flyway-V1→V3-0A0F1C?logo=flyway&logoColor=CC0200)](backend/src/main/resources/db/migration)
[![MySQL](https://img.shields.io/badge/MySQL-8.4-0A0F1C?logo=mysql&logoColor=white)](https://dev.mysql.com/doc/refman/8.4/en/)
[![Next.js](https://img.shields.io/badge/Next.js-16-0A0F1C?logo=nextdotjs&logoColor=white)](https://nextjs.org/)
[![Testes](https://img.shields.io/badge/testes-163_✔-22C55E)](#-qualidade-e-testes)

<!-- Botões gerados por scripts/gerar-svgs-readme.mjs. Cada um leva a uma seção. -->
<a href="#-conheça-o-rkf-bank"><img src="assets/readme/botoes/abrir-conta.svg" alt="🏦 Conheça o banco" height="46"/></a>
<a href="#-a-experiência"><img src="assets/readme/botoes/experiencia.svg" alt="📱 Experiência" height="46"/></a>
<a href="#-segurança-e-lgpd"><img src="assets/readme/botoes/seguranca.svg" alt="🛡️ Segurança" height="46"/></a>
<a href="#-api-rest"><img src="assets/readme/botoes/api.svg" alt="🔌 API" height="46"/></a>
<br/>
<a href="#-rodando-o-projeto"><img src="assets/readme/botoes/rodar.svg" alt="🚀 Rodar local" height="46"/></a>
<a href="#-qualidade-e-testes"><img src="assets/readme/botoes/qualidade.svg" alt="🧪 Qualidade" height="46"/></a>
<a href="#-roadmap"><img src="assets/readme/botoes/roadmap.svg" alt="🗺️ Roadmap" height="46"/></a>
<a href="#-quem-faz-o-rkf-bank"><img src="assets/readme/botoes/time.svg" alt="👥 Time" height="46"/></a>
<br/>
<a href="#-aventura-sua-primeira-semana-no-rkf-bank"><img src="assets/readme/botoes/aventura.svg" alt="🎮 Aventura" height="46"/></a>
<a href="#-feito-em-osasco"><img src="assets/readme/botoes/osasco.svg" alt="📍 Osasco" height="46"/></a>

<img src="assets/readme/numeros.svg" width="100%" alt="RKF Bank em números: 163 testes, 9 rotas REST, 3 migrações Flyway, JWT de 15 minutos, 3 workflows de CI e 3 pessoas de Osasco"/>

</div>

<img src="assets/readme/divisor.svg" width="100%" alt=""/>

## 🏦 Conheça o RKF Bank

> **Mais que um banco, um parceiro para o seu amanhã.**
> O RKF Bank é um **banco digital full stack** (API REST + site) idealizado e construído por três amigos de **Osasco, SP** — **Kevin Richard**, **Renan Barros** e **Felipe Santana**, com processo de time de verdade: contrato de API, Git Flow, Pull Requests revisados, decisões registradas, testes automatizados, CI e um MVP com data marcada.

<table>
  <tr>
    <td align="center" width="25%"><h3>💳</h3><b>Conta em minutos</b><br/><sub>cadastro com CPF validado e endereço pelo CEP</sub></td>
    <td align="center" width="25%"><h3>💸</h3><b>Dinheiro em movimento</b><br/><sub>depósito, saque e transferência tudo-ou-nada</sub></td>
    <td align="center" width="25%"><h3>📜</h3><b>Extrato na hora</b><br/><sub>cada lançamento aparece assim que acontece</sub></td>
    <td align="center" width="25%"><h3>🔐</h3><b>Login seguro</b><br/><sub>CPF + senha, JWT e cookie <code>httpOnly</code></sub></td>
  </tr>
</table>

| Funcionalidade | Backend | Frontend |
|---|:---:|:---:|
| Landing page (benefícios, como funciona, cartão, segurança, FAQ) | — | ✅ |
| Cadastro com senha e endereço automático pelo CEP (ViaCEP) | ✅ | ✅ |
| Login com CPF + senha (JWT) e sessão em cookie `httpOnly` | ✅ | ✅ |
| Abertura de conta corrente ou poupança | ✅ | ✅ |
| Tela da conta com saldo | ✅ (via `GET /contas`) | ✅ |
| Depósito, saque e transferência | ✅ | ✅ |
| Extrato | ✅ | ✅ |
| Perfis `CLIENTE` e `ADMIN` + regra de dono da conta | ✅ | — |
| Logs rastreáveis por requisição (`X-Request-Id`) | ✅ | ✅ |
| `GET /contas/{id}` e `GET /clientes/{id}/contas` | ⏳ | — |
| Política de privacidade e aceite dos termos (LGPD) | ⏳ | ⏳ |

<sub>✅ pronto na <code>develop</code> · ⏳ a fazer antes do lançamento</sub>

<img src="assets/readme/divisor.svg" width="100%" alt=""/>

## 🎮 Escolha a sua trilha

> Cada botão abre um caminho diferente pelo projeto. 👇 Eles abrem e fecham.

<details>
<summary><img src="assets/readme/botoes/trilha-cliente.svg" height="64" alt="💳 Quero ser cliente: o que o RKF Bank faz por mim?"/></summary>
<br/>

| Você quer... | No RKF Bank |
|---|---|
| abrir uma conta sem fila | cadastro com nome, CPF, e-mail, telefone, CEP e senha; o endereço se completa sozinho |
| não pagar mensalidade | conta sem tarifa (o MVP não cobra nada) |
| mandar dinheiro para alguém | transferência entre contas RKF, debitada e creditada na mesma operação |
| saber para onde foi o dinheiro | extrato com entradas e saídas, do mais novo para o mais antigo |
| ter certeza de que é seguro | senha guardada só como hash, sessão que expira em 15 min e ninguém mexe na sua conta além de você |

**Jornada em 3 passos:** `Cadastre-se` → `Abra a conta` → `Movimente com liberdade`. Veja a [experiência completa](#-a-experiência).

</details>

<details>
<summary><img src="assets/readme/botoes/trilha-recrutador.svg" height="64" alt="🧑‍💼 Sou recrutador(a): o que esse projeto prova?"/></summary>
<br/>

| Pergunta | Resposta rápida |
|---|---|
| O que é? | banco digital full stack com prazo de MVP (31/10/2026), feito por um time de três |
| Quem fez o quê? | os três idealizaram o banco juntos · **Kevin Richard**: backend Java/Spring · **Renan Barros**: frontend Next.js · **Felipe Santana**: PO & QA e primeiro cliente |
| Nível técnico? | transação atômica, lock otimista, Flyway, Spring Security + JWT (OAuth2 Resource Server), BCrypt, regra de dono, logs rastreáveis, **163 testes**, CI com CodeQL |
| Processo? | Git Flow (`feature/*` → `develop` → `main`), PR revisado, Conventional Commits, decisões e débitos documentados |
| Onde ver? | [segurança](#-segurança-e-lgpd) · [arquitetura](#-arquitetura) · [qualidade](#-qualidade-e-testes) · [roadmap](#-roadmap) |

</details>

<details>
<summary><img src="assets/readme/botoes/trilha-dev.svg" height="64" alt="👩‍💻 Sou dev: quero ver código, testes e decisões"/></summary>
<br/>

Sugestão de leitura, do mais simples ao mais interessante:

| Arquivo | Por que olhar |
|---|---|
| [`ContaService.java`](backend/src/main/java/com/br/rkfbank/services/ContaService.java) | transferência atômica, regra de dono (404 → 403 → 422) e validação de saldo |
| [`Conta.java`](backend/src/main/java/com/br/rkfbank/entities/Conta.java) | `@Version` para lock otimista |
| [`ConfiguracaoSeguranca.java`](backend/src/main/java/com/br/rkfbank/config/ConfiguracaoSeguranca.java) | `SecurityFilterChain`, JWT HS256 com algoritmo fixo, BCrypt |
| [`AuthService.java`](backend/src/main/java/com/br/rkfbank/services/AuthService.java) | login sem enumeração de CPF (mesma resposta e mesmo tempo) |
| [`db/migration/`](backend/src/main/resources/db/migration) | `V1` schema, `V2` migração de dados, `V3` usuários |
| [`FiltroLogRequisicao.java`](backend/src/main/java/com/br/rkfbank/config/FiltroLogRequisicao.java) | `X-Request-Id` em todas as linhas de log, com proteção contra log forjado |
| [`SegurancaIntegracaoTest.java`](backend/src/test/java/com/br/rkfbank/SegurancaIntegracaoTest.java) | ataques ao token (`alg: none`, payload adulterado) |
| [`planing/decisoes.md`](planing/decisoes.md) | por que cada escolha foi feita |

</details>

<details>
<summary><img src="assets/readme/botoes/trilha-time.svg" height="64" alt="🤝 Sou do time: como rodo, testo e entrego?"/></summary>
<br/>

1. Leia o [CONTRIBUTING.md](CONTRIBUTING.md) e o [CLAUDE.md](CLAUDE.md) (regras do time, também para agentes de IA).
2. Suba tudo: [rodando o projeto](#-rodando-o-projeto).
3. Branch nova **sempre** da `origin/develop` atualizada: `git switch -c feature/minha-tarefa origin/develop`.
4. Antes do PR: `scripts/validar.sh backend` (ou `frontend` / `tudo`).
5. **Felipe:** o roteiro de testes de aceite está em [README_TESTES_E2E.md](backend/README_TESTES_E2E.md) e o checklist em [CHECKLIST_TESTES_E2E.md](backend/CHECKLIST_TESTES_E2E.md).
6. Recados entre o time: [planing/recados.md](planing/recados.md).

</details>

<img src="assets/readme/divisor.svg" width="100%" alt=""/>

## 🎮 Aventura: sua primeira semana no RKF Bank

> Você é o Felipe, primeiro cliente do RKF Bank. Faça as escolhas e veja o que a API responde. 👇 Clique para decidir.

**Segunda-feira.** Você abre o site para criar a conta. O seu CEP é de Osasco.

<details>
<summary>🅰️ Cadastrar com o CPF <code>111.111.111-11</code> (fácil de lembrar)</summary>
<br/>

❌ **`400 Bad Request`**: `cpf inválido`. O `@CpfValido` confere os dígitos verificadores, e CPF com todos os números iguais não passa. Volte e tente a outra opção. 🔁

</details>

<details>
<summary>🅱️ Cadastrar com o seu CPF de verdade, o CEP e uma senha de 8+ caracteres</summary>
<br/>

✅ **`201 Created`**: o endereço se completou sozinho pelo ViaCEP e a sua senha foi guardada só como hash BCrypt.

**Terça-feira.** Você entra com CPF e senha e abre uma conta corrente. Hora do primeiro dinheiro.

<details>
<summary>💵 Depositar R$ 150,00</summary>
<br/>

✅ **`204 No Content`**: saldo de **R$ 150,00**, e o extrato já mostra o depósito.

**Quarta-feira.** O aluguel vence e você precisa transferir **R$ 200,00** para o Renan.

<details>
<summary>💸 Transferir os R$ 200,00 mesmo assim</summary>
<br/>

❌ **`422 Unprocessable Entity`**: `Saldo insuficiente`. Nada foi debitado: a transferência é tudo ou nada. Volte e escolha a outra opção. 🔁

</details>

<details>
<summary>🤝 Transferir R$ 50,00 agora e o resto depois</summary>
<br/>

✅ **`204`**: saldo de **R$ 100,00**. Débito, crédito e lançamento foram gravados na mesma transação.

**Quinta-feira.** Bate a curiosidade: e se você olhasse o extrato da conta do Kevin?

<details>
<summary>🕵️ Trocar o id da conta na URL pelo da conta do Kevin</summary>
<br/>

❌ **`403 Forbidden`**: você está logado, mas a conta não é sua. A resposta não revela nem se o Kevin tem saldo.

</details>

<details>
<summary>⏰ Deixar o app aberto por 20 minutos e clicar em "Extrato"</summary>
<br/>

🔐 **`401 Unauthorized`**: o JWT vence em 15 minutos. O site leva você ao login e depois **volta para o extrato**.

**Sexta-feira.** 🏁 Você fechou a semana com R$ 100,00, um extrato certinho e nenhum dado pessoal nos logs. **Missão cumprida!** Agora é a vez do [roteiro E2E de verdade](backend/README_TESTES_E2E.md).

</details>

</details>

</details>

</details>

<img src="assets/readme/divisor.svg" width="100%" alt=""/>

## 📱 A experiência

```mermaid
journey
    title Do cadastro ao extrato no RKF Bank
    section Abrir a conta
      Preenche nome, CPF, e-mail e senha: 5: Cliente
      Digita o CEP e o endereço aparece sozinho: 5: Cliente, API
      Escolhe conta corrente ou poupança: 4: Cliente
    section Entrar
      Faz login com CPF e senha: 5: Cliente
      Recebe a sessão em cookie seguro: 5: Next.js
    section Movimentar
      Deposita 150,00: 5: Cliente
      Saca 20,00: 4: Cliente
      Transfere 50,00 numa única transação: 5: API
    section Acompanhar
      Vê o saldo e o extrato atualizados: 5: Cliente
```

<div align="center">
  <img src="assets/readme/extrato.svg" width="100%" alt="Extrato animado: cada lançamento entra na lista e o saldo da conta se atualiza"/>
</div>

<details>
<summary><b>📈 O saldo dessa semana em gráfico</b></summary>
<br/>

```mermaid
xychart-beta
    title "Saldo da conta a cada lançamento (R$)"
    x-axis ["abertura", "depósito", "saque", "transf. enviada", "transf. recebida"]
    y-axis "Saldo" 0 --> 160
    bar [0, 150, 130, 80, 115]
    line [0, 150, 130, 80, 115]
```

</details>

<details>
<summary><b>🖥️ As telas do site</b></summary>
<br/>

| Rota | Tela |
|---|---|
| `/` | landing page: benefícios, como funciona, cartão, segurança e dúvidas |
| `/cadastro` | cadastro com CEP automático e senha |
| `/login` | entrar com CPF e senha |
| `/conta/abrir` | abrir conta corrente ou poupança |
| `/conta` | saldo e atalhos |
| `/conta/depositar` · `/conta/sacar` · `/conta/transferir` | movimentações |
| `/conta/extrato` | extrato |

</details>

<details>
<summary><b>🔄 O ciclo de vida de uma conta</b></summary>
<br/>

```mermaid
stateDiagram-v2
    [*] --> Ativa: abrir conta (saldo 0,00)
    Ativa --> Ativa: depósito / saque / transferência
    Ativa --> Conflito: duas operações ao mesmo tempo
    Conflito --> Ativa: 409, cliente tenta de novo
    Ativa --> Inativa: encerrar (🔮 futuro)
    Inativa --> [*]
    note right of Ativa
        só conta ativa movimenta
        saldo nunca fica negativo
    end note
```

</details>

<details>
<summary><b>🎲 Simulador: e se eu tentar...?</b> (clique em cada cenário)</summary>
<br/>

<details><summary>💸 ...sacar mais do que tenho?</summary>

`422 Unprocessable Entity` com `Saldo insuficiente`. Nada é debitado e a tentativa fica no log com o `X-Request-Id`.

</details>
<details><summary>🕵️ ...ver o extrato da conta de outra pessoa?</summary>

`403 Forbidden`. A regra de dono fica no `ContaService`: o `clienteId` vem do token assinado, não do que você manda na URL. E a ordem 404 → 403 → 422 garante que ninguém descubra o saldo alheio pela mensagem de erro.

</details>
<details><summary>⏰ ...usar o app depois de 15 minutos parado?</summary>

O JWT expira e a API responde `401`. O site leva você ao login (`?expirou=1`) e depois **de volta para a tela onde estava**.

</details>
<details><summary>⚡ ...fazer duas transferências ao mesmo tempo da mesma conta?</summary>

Uma passa e a outra recebe `409 Conflict`: a `Conta` tem `@Version` (lock otimista). O saldo nunca é sobrescrito.

</details>
<details><summary>🔁 ...cadastrar o mesmo CPF ou e-mail duas vezes?</summary>

`409 Conflict`. E-mail é comparado sem diferenciar maiúsculas (`Joao@x.com` = `joao@x.com`, migração `V2`).

</details>
<details><summary>📮 ...cadastrar com o ViaCEP fora do ar?</summary>

Timeout de 3 s e `503 Service Unavailable` com mensagem clara, em vez de travar. CEP que não existe vira `422`.

</details>
<details><summary>🧪 ...adulterar o token (trocar o perfil para ADMIN)?</summary>

`401`. A assinatura HS256 não bate mais. Token com `alg: none` também é recusado (algoritmo fixo no `JwtDecoder`). Teste você mesmo: `node scripts/engenharia-reversa-jwt.mjs`.

</details>

</details>

<img src="assets/readme/divisor.svg" width="100%" alt=""/>

## 🔐 Segurança e LGPD

<div align="center">
  <img src="assets/readme/seguranca.svg" width="100%" alt="Seis camadas de segurança: BCrypt, JWT, cookie httpOnly, regra de dono, lock otimista e logs sem dados pessoais"/>
</div>

| Quem pode o quê | Rota |
|---|---|
| 🌍 qualquer pessoa | `POST /clientes` (cadastro) · `POST /auth/login` |
| 👤 cliente logado | só as **próprias** contas: abrir, listar, depositar, sacar, transferir (origem), extrato |
| 🛠️ `ADMIN` | `GET /clientes` e todas as contas em `GET /contas` (o primeiro admin nasce do `.env`) |

<details>
<summary><b>🔑 Como funciona o login, passo a passo</b></summary>
<br/>

```mermaid
sequenceDiagram
    autonumber
    actor C as Cliente
    participant N as Next.js (BFF)
    participant A as API /auth/login
    participant DB as MySQL (tb_usuario)
    C->>N: CPF + senha
    N->>A: POST /auth/login
    A->>DB: busca o usuário pelo CPF
    A->>A: BCrypt confere a senha (mesmo tempo se o CPF não existir)
    A-->>N: JWT HS256 de 15 min
    N-->>C: cookie httpOnly rkf_token (o JavaScript não lê)
    C->>N: abre o extrato
    N->>A: GET /contas/{id}/lancamentos + Bearer
    A->>A: assinatura e validade ok? (401) · a conta é sua? (403)
    A-->>C: 200 com o extrato
```

</details>

<details>
<summary><b>🛡️ Status da LGPD no backend (análise de 19/10/2026)</b></summary>
<br/>

**Já atende:**

- ✅ Senha só como hash BCrypt; nunca volta em resposta nem em log.
- ✅ Logs sem CPF, e-mail, telefone, senha ou token (até o `toString` dos DTOs esconde os dados).
- ✅ Autenticação em todas as rotas privadas e regra de dono (`403`).
- ✅ Dados de todos os clientes só para `ADMIN`.
- ✅ O `sub` do JWT é o id do usuário, não o CPF (o payload do JWT é só Base64).
- ✅ Frontend mascara CPF, e-mail e telefone nos logs; token fora do alcance do JavaScript.

**Falta antes de colocar no ar** (bloqueia o deploy, não o desenvolvimento):

- ⏳ Política de privacidade e termos de uso de verdade (o cadastro já cita os dois).
- ⏳ Registrar o aceite dos termos (versão + data) numa migração `V4`.
- ⏳ Encarregado e canal de contato do titular.

**Depois do MVP:** rota "meus dados" (`GET /clientes/me`), correção de dados, encerramento de conta com retenção e anonimização, limite de tentativas de login e auditoria.
Detalhes e prioridades em [planing/debitos-tecnicos.md](planing/debitos-tecnicos.md) e [planing/decisoes.md](planing/decisoes.md).

</details>

<img src="assets/readme/divisor.svg" width="100%" alt=""/>

## 🔌 API REST

Base URL: `http://localhost:8080` · JSON · IDs em UUID · dinheiro com 2 casas (`150.00`) · datas ISO-8601 UTC · rotas privadas com `Authorization: Bearer <token>`.
Contrato completo (versão 2.0): [backend/README_CONTRATO_API.md](backend/README_CONTRATO_API.md).

| Método | Rota | O que faz | Quem pode | Sucesso |
|---|---|---|---|---|
| `POST` | `/clientes` | cadastra cliente (com senha) | público | `201` |
| `POST` | `/auth/login` | login com CPF + senha | público | `200` + token |
| `GET` | `/clientes` | lista clientes (`?paginado=true&page=0&size=10`) | `ADMIN` | `200` |
| `POST` | `/contas/clientes/{clienteId}` | abre conta | o próprio cliente | `201` |
| `GET` | `/contas` | lista contas | `ADMIN`: todas · `CLIENTE`: as dele | `200` |
| `POST` | `/contas/{contaId}/deposito` | deposita | dono da conta | `204` |
| `POST` | `/contas/{contaId}/saque` | saca | dono da conta | `204` |
| `POST` | `/contas/transferencia` | transfere | dono da conta de origem | `204` |
| `GET` | `/contas/{contaId}/lancamentos` | extrato (mais recente primeiro) | dono da conta | `200` |

<details>
<summary><b>👤 <code>POST /clientes</code> — cadastrar</b></summary>
<br/>

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

Resposta `201`: o cliente com o endereço completo vindo do ViaCEP. A senha **nunca** volta.

</details>

<details>
<summary><b>🔑 <code>POST /auth/login</code> — entrar</b></summary>
<br/>

```json
{ "cpf": "52998224725", "senha": "minha-senha-forte" }
```

```json
{
  "token": "eyJhbGciOiJIUzI1NiJ9...",
  "tipo": "Bearer",
  "expiraEmSegundos": 900,
  "clienteId": "1a89861c-70a0-4b7c-947a-d16cdfe2dd08",
  "perfil": "CLIENTE"
}
```

CPF sem cadastro e senha errada recebem **a mesma** resposta `401`, de propósito.

</details>

<details>
<summary><b>💳 Abrir conta, movimentar e ver o extrato</b></summary>
<br/>

```json
// POST /contas/clientes/{clienteId}
{ "agencia": "0001", "numeroConta": "12345678", "tipoContaEnum": "CORRENTE" }

// POST /contas/{contaId}/deposito  e  /saque
{ "valor": 150.00, "descricao": "Deposito inicial" }

// POST /contas/transferencia
{ "contaOrigemId": "407d727e-...", "contaDestinoId": "566dd28f-...", "valor": 50.00, "descricao": "Aluguel" }
```

Extrato (`GET /contas/{contaId}/lancamentos`):

```json
[
  {
    "id": "59798941-269a-406c-8783-9be456bf1f5c",
    "tipoLancamento": "TRANSFERENCIA",
    "valor": 50.00,
    "contaOrigemId": "407d727e-4e43-4c93-b01c-331cbaba42fb",
    "contaDestinoId": "566dd28f-b08f-41e4-ac55-cc69ecfea4cd",
    "descricao": "Aluguel",
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
  "campos": [{ "campo": "cpf", "mensagem": "cpf inválido" }]
}
```

| Status | Quando |
|---|---|
| `400` | dados inválidos, JSON mal formado ou parâmetro inválido |
| `401` | sem token, token inválido ou vencido, ou CPF/senha errados no login |
| `403` | logado, mas sem permissão (conta de outro cliente, rota de `ADMIN`) |
| `404` | cliente ou conta não encontrado |
| `409` | CPF, e-mail ou número de conta repetido, ou conta alterada ao mesmo tempo |
| `422` | regra de negócio: CEP inexistente, saldo insuficiente, mesma conta na transferência |
| `503` | ViaCEP fora do ar |

</details>

<details>
<summary><b>🧪 Fluxo completo com <code>curl</code></b></summary>
<br/>

```bash
API=http://localhost:8080

# 1. cadastro (público)
curl -X POST $API/clientes -H "Content-Type: application/json" -d '{
  "nomeCompleto":"Joao Silva","cpf":"52998224725","email":"joao@rkfbank.com",
  "telefone":"11999998888","cep":"01001000","numero":"123","senha":"minha-senha-forte"}'

# 2. login: guarde o token e o clienteId
curl -X POST $API/auth/login -H "Content-Type: application/json" \
  -d '{"cpf":"52998224725","senha":"minha-senha-forte"}'
TOKEN=cole-o-token-aqui

# 3. conta (use o clienteId do login)
curl -X POST $API/contas/clientes/$CLIENTE_ID -H "Authorization: Bearer $TOKEN" \
  -H "Content-Type: application/json" -d '{"agencia":"0001","numeroConta":"12345678","tipoContaEnum":"CORRENTE"}'

# 4. movimentações e extrato (use o id da conta)
curl -X POST $API/contas/$CONTA_ID/deposito -H "Authorization: Bearer $TOKEN" -H "Content-Type: application/json" -d '{"valor":150.00}'
curl $API/contas/$CONTA_ID/lancamentos -H "Authorization: Bearer $TOKEN"
```

CPFs válidos para teste: `52998224725`, `16899535009`, `39053344705`, `11144477735`.
Ou rode tudo de uma vez com a API no ar: `node scripts/smoke-e2e.mjs`.

</details>

<img src="assets/readme/divisor.svg" width="100%" alt=""/>

## 🧱 Arquitetura

```mermaid
flowchart LR
    U([📱 Cliente]) -->|navegador| FE

    subgraph FE [Frontend · Next.js :3000]
        P[Telas] --> H[Hooks]
        H --> API["lib/api"]
        RH["Route Handlers<br/>/api/auth/*"] --> CK[(cookie httpOnly<br/>rkf_token)]
    end

    API -->|"/api/... + Bearer (proxy)"| BE

    subgraph BE [Backend · Spring Boot :8080]
        F["FiltroLogRequisicao<br/>X-Request-Id"] --> SEC["SecurityFilterChain<br/>JWT · 401/403"]
        SEC --> C[Controllers]
        C --> S[Services<br/>regra de dono · @Transactional]
        S --> R[Repositories]
        S --> V[Client ViaCEP]
        C -. erros .-> EH[ExceptionHandler]
    end

    R --> DB[(MySQL 8.4<br/>schema pelo Flyway)]
    V -->|HTTPS| VC[[viacep.com.br]]
```

<div align="center">
  <img src="assets/readme/fluxo-transferencia.svg" width="100%" alt="Animação: uma transferência atravessa Next.js, Spring Security, controller, service e MySQL e volta com 204"/>
</div>

<details>
<summary><b>🆚 Antes × depois: decisões que mudaram o projeto</b> (clique em cada uma)</summary>
<br/>

<details><summary>🗄️ Banco: <code>ddl-auto=update</code> → Flyway</summary>

| Antes | Depois |
|---|---|
| o Hibernate "adivinhava" o schema: FKs com nome aleatório e sem índice no extrato | `V1`, `V2` e `V3` versionados, revisados em PR e iguais em todo ambiente |
| renomear um campo criava uma coluna nova e deixava os dados na velha | `ALTER TABLE ... RENAME COLUMN` numa migração nova |

</details>
<details><summary>🔐 Acesso: qualquer um movimentava qualquer conta → JWT + regra de dono</summary>

| Antes | Depois |
|---|---|
| `POST /contas/{id}/saque` funcionava sem login, para qualquer id | sem token dá `401`; conta de outra pessoa dá `403` |
| `GET /clientes` mostrava o CPF de todo mundo | só o `ADMIN` lista clientes |

</details>
<details><summary>🍪 Token: <code>localStorage</code> → cookie <code>httpOnly</code></summary>

| Com `localStorage` (descartado) | Com cookie `httpOnly` |
|---|---|
| qualquer script da página (um XSS) leria o token | o Next grava o cookie e o JavaScript não alcança |

</details>
<details><summary>💰 Dinheiro: <code>double</code> → <code>BigDecimal</code></summary>

| Com `double` | Com `BigDecimal` |
|---|---|
| `0.1 + 0.2 = 0.30000000000000004` | `0.30` exato, e `DECIMAL(19,2)` no banco |

</details>

</details>

<details>
<summary><b>🗃️ Modelo de dados (migrações V1, V2 e V3)</b></summary>
<br/>

```mermaid
erDiagram
    tb_cliente ||--|| tb_endereco : "mora em"
    tb_cliente ||--o{ tb_conta : "possui"
    tb_cliente ||--o| tb_usuario : "entra com"
    tb_conta ||..o{ tb_lancamento : "origem ou destino"
    tb_cliente {
        BINARY16 id PK
        string nome_completo
        string cpf UK
        string email UK "minúsculas (V2)"
        string telefone
    }
    tb_endereco {
        BINARY16 id PK
        string cep
        string logradouro
        string cidade
        string uf
    }
    tb_conta {
        BINARY16 id PK
        string numero_conta UK
        string agencia
        string tipo_conta_enum "CORRENTE | POUPANCA"
        decimal saldo "DECIMAL(19,2)"
        bit ativa
        bigint versao "lock otimista"
    }
    tb_usuario {
        BINARY16 id PK
        string login UK "CPF"
        string senha_hash "BCrypt (60)"
        string perfil "CLIENTE | ADMIN"
        BINARY16 cliente_id UK
    }
    tb_lancamento {
        BINARY16 id PK
        string tipo_lancamento_enum
        decimal valor
        BINARY16 conta_origem_id
        BINARY16 conta_destino_id
        datetime data_hora
    }
```

</details>

<details>
<summary><b>📦 Pacotes do backend</b></summary>
<br/>

| Pacote | Responsabilidade |
|---|---|
| [`config`](backend/src/main/java/com/br/rkfbank/config) | segurança (filter chain, JWT, erros 401/403), CORS, filtro de log, criação do admin |
| [`controllers`](backend/src/main/java/com/br/rkfbank/controllers) | HTTP: `AuthController`, `ClienteController`, `ContaController` |
| [`services`](backend/src/main/java/com/br/rkfbank/services) | regras de negócio, transações, login e token |
| [`security`](backend/src/main/java/com/br/rkfbank/security) | `UsuarioLogado`, montado a partir do JWT |
| [`repositories`](backend/src/main/java/com/br/rkfbank/repositories) | Spring Data JPA |
| [`entities`](backend/src/main/java/com/br/rkfbank/entities) | `Cliente`, `Endereco`, `Conta`, `Lancamento`, `Usuario` |
| [`dto`](backend/src/main/java/com/br/rkfbank/dto) | entrada e saída da API (`record`) |
| [`clients/viacep`](backend/src/main/java/com/br/rkfbank/clients/viacep) | integração com o ViaCEP |
| [`validation`](backend/src/main/java/com/br/rkfbank/validation) | `@CpfValido` |
| [`exceptions`](backend/src/main/java/com/br/rkfbank/exceptions) | exceções de negócio que viram status HTTP |

Mais detalhes em [backend/README_IMPLEMENTACAO.md](backend/README_IMPLEMENTACAO.md).

</details>

<details>
<summary><b>🧰 Stack completa</b></summary>
<br/>

<div align="center">
<img src="https://skillicons.dev/icons?i=java,spring,maven,mysql,docker,nextjs,react,ts,tailwind,githubactions&perline=10" alt="Stack do RKF Bank"/>
</div>

| Lado | Tecnologias |
|---|---|
| Backend | Java 25 · Spring Boot 4.1 · Spring Web MVC · Spring Data JPA (Hibernate 7) · Spring Security + OAuth2 Resource Server · Bean Validation · `RestClient` · Flyway · Log4j2 |
| Banco | MySQL 8.4 (Docker Compose) · H2 2.3.232 nos testes |
| Testes | JUnit 5 · Mockito · `@DataJpaTest` · `@SpringBootTest` · JaCoCo |
| Frontend | Next.js 16 (App Router) · React 19 · TypeScript 5 · Tailwind CSS 4 · shadcn/ui · Lucide · pnpm 10 |
| Esteira | GitHub Actions · CodeQL · Dependency Review · Dependabot · Conventional Commits |

</details>

<img src="assets/readme/divisor.svg" width="100%" alt=""/>

## 🚀 Rodando o projeto

| Ferramenta | Versão |
|---|---|
| [JDK](https://adoptium.net/temurin/releases/?version=25) | **25** (o Maven Enforcer bloqueia outras) |
| [Docker Desktop](https://www.docker.com/products/docker-desktop/) | recente |
| [Node.js](https://nodejs.org/) + [pnpm](https://pnpm.io/installation) | 20+ e 10 (`corepack enable`) |

<details open>
<summary><b>1️⃣ Banco (MySQL no Docker)</b></summary>
<br/>

```bash
cp .env.example .env          # preencha senhas e gere o JWT_SEGREDO: openssl rand -base64 48
docker compose up -d          # MySQL na porta 5441
docker compose ps             # espere "healthy"
```

</details>

<details open>
<summary><b>2️⃣ Backend (porta 8080)</b></summary>
<br/>

O Spring **não lê o `.env` sozinho**: carregue as variáveis no terminal.

```bash
# Git Bash / Linux / macOS
set -a && source <(tr -d '\r' < .env) && set +a
./mvnw spring-boot:run
```

```powershell
# PowerShell
Get-Content .env | Where-Object { $_ -match '^\s*[^#].*=' } | ForEach-Object {
  $nome, $valor = $_ -split '=', 2; Set-Item "env:$nome" $valor
}
.\mvnw.cmd spring-boot:run
```

As tabelas nascem pelo **Flyway** na primeira subida. Se a API recusar com *Found non-empty schema(s)*, o seu MySQL é anterior ao Flyway: rode `docker compose down -v && docker compose up -d` uma vez (apaga os dados locais).

</details>

<details open>
<summary><b>3️⃣ Frontend (porta 3000)</b></summary>
<br/>

```bash
cd frontend
cp .env.example .env.local
pnpm install
pnpm dev
```

Abra <http://localhost:3000>, crie a conta em `/cadastro` e entre em `/login`.

</details>

<details>
<summary><b>🧯 Problemas comuns</b></summary>
<br/>

| Sintoma | Causa provável | Solução |
|---|---|---|
| `Rule 0: RequireJavaVersion failed` | JDK diferente do 25 | instale o JDK 25 e aponte o `JAVA_HOME` |
| `JWT_SEGREDO ausente ou curto` | `.env` sem segredo ou não carregado | gere com `openssl rand -base64 48` e carregue o `.env` |
| `Access denied for user` | variáveis do `.env` não carregadas | carregue o `.env` antes do `mvnw` |
| `Communications link failure` | MySQL desligado ou subindo | `docker compose ps` e espere o `healthy` |
| `Validate failed: Migration checksum mismatch` | uma migração já aplicada foi editada | nunca edite `V<n>__`; crie uma nova versão |
| Cadastro devolve `503` | ViaCEP fora do ar | tente de novo em alguns segundos |
| Volta para o login toda hora | token de 15 min venceu | esperado: ainda não há refresh token |

</details>

<img src="assets/readme/divisor.svg" width="100%" alt=""/>

## 🧪 Qualidade e testes

<div align="center">
  <img src="assets/readme/terminal.svg" width="100%" alt="Terminal: 163 testes, 0 falhas, BUILD SUCCESS"/>
</div>

```bash
scripts/validar.sh backend          # testes em H2, resumo curto (sem Docker)
scripts/validar.sh frontend         # lint + typecheck + build
scripts/testar-no-mysql.sh          # testes de integração contra o MySQL do Docker
node scripts/smoke-e2e.mjs          # fluxo real contra a API no ar
node scripts/engenharia-reversa-jwt.mjs  # tenta forjar o JWT: tudo tem que dar 401
node scripts/varrer-cves.js         # vulnerabilidades conhecidas no Maven e no pnpm
```

<details>
<summary><b>📋 O que cada suíte cobre</b></summary>
<br/>

| Suíte | O que cobre |
|---|---|
| `ClienteServiceTest` | cadastro, CPF/e-mail duplicado, ViaCEP, senha em BCrypt |
| `ContaServiceTest` | abertura, depósito, saque, transferência, extrato e regra de dono (`403`) |
| `TokenServiceTest` / `AuthServiceTest` | JWT (claims, 15 min, adulteração, chave errada) e login sem enumeração |
| `UsuarioServiceTest` / `CriadorAdminTest` | primeiro `ADMIN` pelo `.env` |
| `SegurancaIntegracaoTest` | Spring inteiro com tokens reais: `401`, `403`, perfis, `alg: none` |
| `ClienteControllerTest` / `ContaControllerTest` | status de cada rota e limites da paginação |
| `ControllerExceptionHandlerTest` | cada exceção no formato de erro |
| `ClienteViaCepTest` | CEP encontrado, inexistente (`422`) e ViaCEP fora (`503`) |
| `CpfValidoValidatorTest` | dígitos verificadores e máscara |
| `FiltroLogRequisicaoTest` | `X-Request-Id` reaproveitado, gerado e rejeitado contra log forjado |
| `*RepositoryTest` | consultas, constraints, cascata, `@Version` e `tb_usuario` (`@DataJpaTest` com as migrações do Flyway) |

Relatório de cobertura: `target/site/jacoco/index.html`. Teste manual de aceite: [roteiro E2E](backend/README_TESTES_E2E.md) + [checklist](backend/CHECKLIST_TESTES_E2E.md).

</details>

<details>
<summary><b>⚙️ Esteira no GitHub Actions</b></summary>
<br/>

```mermaid
flowchart LR
    PR([Pull Request<br/>para develop/main]) --> CI[Backend CI<br/>JDK 25 · mvnw verify · JaCoCo]
    PR --> CQ[CodeQL<br/>análise de segurança]
    PR --> DR{Dependency Review<br/>vulnerabilidade alta?}
    DR -->|sim| X[❌ bloqueia]
    DR -->|não| OK[✅ libera]
    DB([Dependabot semanal]) -.-> PR
```

| Workflow | Arquivo | O que faz |
|---|---|---|
| Backend CI | [`backend-ci.yml`](.github/workflows/backend-ci.yml) | compila, testa e publica relatórios |
| Backend CodeQL | [`backend-codeql.yml`](.github/workflows/backend-codeql.yml) | procura vulnerabilidades no código Java |
| Dependency Review | [`backend-dependency-review.yml`](.github/workflows/backend-dependency-review.yml) | barra dependência vulnerável ou com licença AGPL |
| Dependabot | [`dependabot.yml`](.github/dependabot.yml) | PRs de atualização para a `develop` |

</details>

<details>
<summary><b>🔎 Logs rastreáveis</b></summary>
<br/>

```text
INFO  [http-nio-8080-exec-1] [e2e-transf-001] c.b.r.s.ContaService : Transferencia de 50.00 da conta 672e4ddf-... para 83af15fb-...
INFO  [http-nio-8080-exec-1] [e2e-transf-001] c.b.r.c.FiltroLogRequisicao : POST /contas/transferencia -> 204 (15 ms)
```

Cada requisição ganha um `X-Request-Id` que aparece em todas as linhas, até no SQL. CPF, e-mail, senha e token **nunca** entram no log. Configuração: [`log4j2-spring.xml`](backend/src/main/resources/log4j2-spring.xml).

</details>

<img src="assets/readme/divisor.svg" width="100%" alt=""/>

## 🧭 Roadmap

```mermaid
gantt
    title Do MVP ao próximo nível
    dateFormat YYYY-MM-DD
    axisFormat %d/%m
    section Feito
    Flyway V1 a V3                 :done,   f1, 2026-10-15, 2d
    Spring Security + JWT          :done,   f2, 2026-10-16, 2d
    Telas da conta e login         :done,   f0, 2026-10-14, 4d
    section MVP
    GET conta e contas do cliente  :active, m1, 2026-10-20, 3d
    LGPD mínimo                    :        m2, 2026-10-22, 2d
    Testes do Felipe              :        m3, 2026-10-26, 3d
    Colocar no ar                  :crit,   m4, 2026-10-29, 3d
    section Depois
    Testcontainers                 :        p1, 2026-10-20, 4d
    Swagger / OpenAPI              :        p2, after p1, 2d
    Pix, boleto e cartão (simulados) :      p3, 2026-11-05, 20d
```

<details open>
<summary><b>🚀 MVP — até 31/10/2026</b></summary>
<br/>

- [x] Cadastro com ViaCEP, abertura de conta, depósito, saque, transferência e extrato
- [x] Logs rastreáveis, CI, CodeQL e Dependabot
- [x] Banco versionado com Flyway (V1, V2, V3)
- [x] Login com Spring Security + JWT, perfis e regra de dono
- [x] Frontend: landing, cadastro, login, conta, movimentações e extrato
- [ ] Backend: `GET /contas/{id}` e `GET /clientes/{id}/contas`
- [ ] LGPD mínimo: política de privacidade, aceite dos termos e encarregado
- [ ] Felipe testa tudo como primeiro cliente e os bugs são corrigidos
- [ ] Colocar no ar e atualizar este README com link e prints

</details>

<details>
<summary><b>🔭 Depois do MVP</b></summary>
<br/>

- [ ] Testes de integração com Testcontainers + MySQL e `MockMvc` ([checklist](planing/checklist-features.md))
- [ ] Swagger UI com o botão *Authorize* (springdoc-openapi)
- [ ] Refresh token, limite de tentativas de login e 2FA
- [ ] "Meus dados", editar perfil e encerrar conta (direitos do titular)
- [ ] CI do frontend
- [ ] Pix, boleto, QR Code e cartão virtual (simulados) — [funcionalidades futuras](planing/debitos-tecnicos.md#funcionalidades-futuras-)
- [ ] 🤖 Assistente do extrato com Spring AI (ideia)

</details>

Planejamento detalhado: [planing/roadmap.md](planing/roadmap.md) · [checklist de features](planing/checklist-features.md) · [decisões](planing/decisoes.md).

<img src="assets/readme/divisor.svg" width="100%" alt=""/>

## 👥 Quem faz o RKF Bank

> Três amigos de Osasco tiveram a ideia juntos e dividiram o banco em três frentes: **API**, **site** e **produto**.

<div align="center">
  <img src="assets/readme/time.svg" width="100%" alt="Os três idealizadores do RKF Bank, todos de Osasco: Kevin Richard no backend, Felipe Santana no produto e como primeiro cliente, Renan Barros no frontend"/>
</div>

<table>
  <tr>
    <td align="center" valign="top" width="33%">
      <a href="https://github.com/k3vinrich4rd"><img src="https://github.com/k3vinrich4rd.png" width="96" alt="Kevin Richard"/></a>
      <h3>Kevin Richard</h3>
      <img src="https://img.shields.io/badge/Idealizador-Backend-3B82F6?logo=springboot&logoColor=white" alt="Idealizador e responsável pelo backend"/>
      <p align="left"><sub>API REST em Java e Spring Boot, banco MySQL e Flyway, Spring Security e JWT, integração com o ViaCEP, logs e testes. Objetivo: o dinheiro de cada cliente sempre certo e nenhum dado pessoal vazando.</sub></p>
      <a href="mailto:kevinrichardve@gmail.com"><img src="https://img.shields.io/badge/E--mail-kevinrichardve-3B82F6?logo=gmail&logoColor=white" alt="E-mail de Kevin"/></a>
      <a href="https://github.com/k3vinrich4rd"><img src="https://img.shields.io/badge/GitHub-k3vinrich4rd-181717?logo=github&logoColor=white" alt="GitHub de Kevin"/></a>
    </td>
    <td align="center" valign="top" width="33%">
      <h3>💡<br/>Felipe Santana</h3>
      <img src="https://img.shields.io/badge/Idealizador-PO_%26_QA-E2C98A" alt="Idealizador, PO e QA"/>
      <p align="left"><sub>Idealizou o RKF Bank com Kevin e Renan, define o que o produto precisa e é o <b>primeiro cliente</b>: tem acesso antes de todo mundo para os primeiros testes e valida o MVP pelo roteiro E2E antes do lançamento.</sub></p>
      <img src="https://img.shields.io/badge/1º_cliente-acesso_antecipado-0A0F1C" alt="Primeiro cliente"/>
    </td>
    <td align="center" valign="top" width="33%">
      <a href="https://github.com/renanbarrostech-cmd"><img src="https://github.com/renanbarrostech-cmd.png" width="96" alt="Renan Barros"/></a>
      <h3>Renan Barros</h3>
      <img src="https://img.shields.io/badge/Idealizador-Frontend-06B6D4?logo=nextdotjs&logoColor=white" alt="Idealizador e responsável pelo frontend"/>
      <p align="left"><sub>Site em Next.js, React e TypeScript, identidade visual RKF, telas da conta, login com cookie <code>httpOnly</code> e logs com máscara. Objetivo: parecer e funcionar como um banco digital de verdade.</sub></p>
      <a href="mailto:renanbarros.tech@gmail.com"><img src="https://img.shields.io/badge/E--mail-renanbarros.tech-06B6D4?logo=gmail&logoColor=white" alt="E-mail de Renan"/></a>
      <a href="https://github.com/renanbarrostech-cmd"><img src="https://img.shields.io/badge/GitHub-renanbarrostech--cmd-181717?logo=github&logoColor=white" alt="GitHub de Renan"/></a>
    </td>
  </tr>
</table>

<div align="center">

Dúvida sobre a API ou o banco? **Kevin**. Telas e site? **Renan**. Produto, prioridade e testes de aceite? **Felipe**.
Bug ou sugestão? Abra uma [issue](https://github.com/k3vinrich4rd/rkf_bank/issues).

</div>

### 📍 Feito em Osasco

<div align="center">
  <img src="assets/readme/osasco.svg" width="100%" alt="Feito em Osasco, SP: onde três amigos tiveram a ideia do RKF Bank"/>
</div>

O RKF Bank nasceu em **Osasco**, na Grande São Paulo, da ideia de três amigos que queriam aprender construindo algo de verdade. Arraste e dê zoom no mapa:

```geojson
{
  "type": "FeatureCollection",
  "features": [
    {
      "type": "Feature",
      "properties": {
        "title": "RKF Bank · Osasco, SP",
        "description": "Kevin, Renan e Felipe: o time inteiro é de Osasco",
        "marker-color": "#3B82F6",
        "marker-size": "large",
        "marker-symbol": "bank"
      },
      "geometry": { "type": "Point", "coordinates": [-46.7917, -23.5329] }
    }
  ]
}
```

<details>
<summary><b>🤝 Como o time trabalha</b></summary>
<br/>

```mermaid
gitGraph
    commit id: "main"
    branch develop
    checkout develop
    commit id: "integração"
    branch feature/flyway
    checkout feature/flyway
    commit id: "feat(db): V1, V2"
    checkout develop
    merge feature/flyway id: "PR #23"
    branch feature/spring-security-jwt
    checkout feature/spring-security-jwt
    commit id: "feat: login JWT"
    commit id: "test: ataques ao token"
    checkout develop
    merge feature/spring-security-jwt id: "PR #24"
    branch feature/login-frontend
    checkout feature/login-frontend
    commit id: "feat: cookie httpOnly"
    checkout develop
    merge feature/login-frontend id: "PR #27"
    checkout main
    merge develop id: "release do MVP"
```

- **Branches:** `main` é o que vai ao ar, `develop` é a integração; toda tarefa sai da `origin/develop` atualizada.
- **Commits:** [Conventional Commits](https://www.conventionalcommits.org/pt-br) em português, validados por hook.
- **PRs:** revisão do outro dev + CI verde. **Contrato primeiro:** mudou rota, muda o [contrato](backend/README_CONTRATO_API.md) no mesmo PR.
- **IA no time:** o [CLAUDE.md](CLAUDE.md) e as skills em [`.claude/skills/`](.claude/skills) (`/implementar-feature`, `/entregar`, `/revisar-migracao`, `/testar-e2e`...) dão a qualquer agente o mesmo contexto e as mesmas regras do time.

Passo a passo completo: [CONTRIBUTING.md](CONTRIBUTING.md).

</details>

<img src="assets/readme/divisor.svg" width="100%" alt=""/>

## 💬 Pergunte ao RKF

> Perguntas que um cliente, um recrutador ou um dev costumam fazer. Clique para abrir.

<details>
<summary><b>Por que a transferência nunca deixa o dinheiro "no meio do caminho"?</b></summary>
<br/>

Débito, crédito e lançamento do extrato acontecem dentro de um único `@Transactional`: ou os três são gravados, ou nenhum. E o `@Version` na `Conta` impede que duas operações simultâneas sobrescrevam o saldo (a segunda recebe `409`).

</details>

<details>
<summary><b>Por que o token fica num cookie e não no <code>localStorage</code>?</b></summary>
<br/>

Qualquer script que rode na página (um XSS, uma dependência comprometida) consegue ler o `localStorage`. Com cookie `httpOnly`, gravado pelo servidor do Next, o JavaScript não alcança o token. `SameSite=Lax` protege contra CSRF. Decisão registrada em [decisoes.md](planing/decisoes.md).

</details>

<details>
<summary><b>Por que Flyway em vez de deixar o Hibernate criar as tabelas?</b></summary>
<br/>

Com `ddl-auto=update` o Hibernate "adivinha" o schema, não remove nem renomeia nada e não deixa histórico. Com Flyway, cada mudança é um SQL versionado (`V1__`, `V2__`, `V3__`), revisado no PR e igual em todo ambiente; o Hibernate só **valida**. Migração aplicada nunca se edita: o checksum muda e a API não sobe.

</details>

<details>
<summary><b>O RKF Bank é um banco de verdade?</b></summary>
<br/>

Ainda não: é um projeto real de engenharia, com processo de time, mas sem autorização do Banco Central. Pix e boleto, quando vierem, serão **simulados** entre as contas do próprio RKF Bank, para aprender o domínio sem depender de terceiros.

</details>

### 🎯 Desafio relâmpago

**1. Um cliente logado tenta sacar da conta de outra pessoa. Qual status a API devolve?**

<details><summary>A) 401 Unauthorized</summary>

❌ **Quase.** `401` é para quem **não** está autenticado (sem token, token vencido ou adulterado).

</details>
<details><summary>B) 403 Forbidden</summary>

✅ **Isso!** Ele está autenticado, mas não é dono da conta. A regra fica no `ContaService`, com o `clienteId` vindo do token assinado.

</details>
<details><summary>C) 422 Unprocessable Entity</summary>

❌ **Ainda não.** `422` é regra de negócio (saldo insuficiente). Responder `422` aqui revelaria o saldo de outra pessoa.

</details>

**2. Alguém troca `"perfil":"CLIENTE"` por `"ADMIN"` no payload do JWT. O que acontece?**

<details><summary>A) Vira admin, o payload é só Base64</summary>

❌ **Não.** O payload é legível, mas a **assinatura** HS256 deixa de bater.

</details>
<details><summary>B) 401: assinatura inválida</summary>

✅ **Isso!** O `JwtDecoder` confere assinatura e algoritmo em toda requisição. Rode `node scripts/engenharia-reversa-jwt.mjs` para ver.

</details>

**3. Como a senha do cliente fica guardada no banco?**

<details><summary>A) Criptografada, para poder ser recuperada</summary>

❌ **Não.** Se dá para descriptografar, quem rouba a chave lê todas as senhas.

</details>
<details><summary>B) Como hash BCrypt com salt</summary>

✅ **Isso!** Hash de mão única, com salt embutido e lento de propósito contra força bruta. Nem o time consegue ver a sua senha.

</details>

<img src="assets/readme/divisor.svg" width="100%" alt=""/>

## 📚 Documentação

| Área | Documento | O que tem |
|---|---|---|
| Time | [CONTRIBUTING.md](CONTRIBUTING.md) | branches, commits, PRs e revisão |
| Time e IA | [CLAUDE.md](CLAUDE.md) | regras do projeto para pessoas e agentes de IA |
| API | [Contrato da API (2.0)](backend/README_CONTRATO_API.md) | rotas, autenticação, JSON, regras e erros |
| Backend | [backend/README.md](backend/README.md) | como rodar, testes, banco, Flyway e problemas comuns |
| Backend | [Arquitetura e implementação](backend/README_IMPLEMENTACAO.md) | camadas e trechos reais |
| Backend | [Roteiro E2E](backend/README_TESTES_E2E.md) · [Checklist E2E](backend/CHECKLIST_TESTES_E2E.md) | testes de aceite do Felipe |
| Frontend | [frontend/README.md](frontend/README.md) · [Logs](frontend/lib/logger/README.md) | como rodar e logs com máscara |
| Planejamento | [planing/](planing/README.md) | roadmap, decisões, checklist de features, débitos e recados |

<br/>

<div align="center">

<a href="planing/roadmap.md"><img src="assets/readme/chamada.svg" width="100%" alt="Mais que um banco, um parceiro para o seu amanhã. Ver o roadmap."/></a>

<sub>📍 RKF Banco Digital — idealizado e feito em Osasco, SP, por Kevin Richard, Renan Barros e Felipe Santana.</sub>
<br/>
<a href="#readme">⬆️ voltar ao topo</a>

</div>
