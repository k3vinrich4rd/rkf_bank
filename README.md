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
[![Flyway](https://img.shields.io/badge/Flyway-V1→V8_%2B_seed-0A0F1C?logo=flyway&logoColor=CC0200)](backend/src/main/resources/db/migration)
[![MySQL](https://img.shields.io/badge/MySQL-8.4-0A0F1C?logo=mysql&logoColor=white)](https://dev.mysql.com/doc/refman/8.4/en/)
[![Next.js](https://img.shields.io/badge/Next.js-16-0A0F1C?logo=nextdotjs&logoColor=white)](https://nextjs.org/)
[![Testes](https://img.shields.io/badge/testes-264_✔-22C55E)](#-qualidade-e-testes)

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

<img src="assets/readme/numeros.svg" width="100%" alt="RKF Bank em números: 266 testes, 17 rotas REST, 9 migrações Flyway, JWT de 15 minutos, 3 workflows de CI e 3 pessoas de Osasco"/>

</div>

<img src="assets/readme/divisor.svg" width="100%" alt=""/>

## 🏦 Conheça o RKF Bank

> **Mais que um banco, um parceiro para o seu amanhã.**
> O RKF Bank é um **banco digital full stack** (API REST + site) idealizado e construído por três amigos de **Osasco, SP** — **Kevin Richard**, **Renan Barros** e **Fellipe Santana**, com processo de time de verdade: contrato de API, Git Flow, Pull Requests revisados, decisões registradas, testes automatizados, CI e um MVP com data marcada.

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
| Landing page com o cartão RKF em 3D e a seção do time | — | ✅ |
| Cadastro com senha, CPF/CEP higienizados e endereço automático pelo CEP (ViaCEP) | ✅ | ✅ |
| Login com CPF + senha (JWT) e sessão em cookie `httpOnly` | ✅ | ✅ |
| Abertura de conta corrente ou poupança | ✅ | ✅ |
| Tela da conta com saldo | ✅ (via `GET /contas`) | ✅ |
| Depósito, saque e transferência (uma vez só, com `Idempotency-Key`) | ✅ | ✅ |
| Extrato | ✅ | ✅ |
| Trocar a própria senha | ✅ | ✅ |
| Meus dados (LGPD): ver, corrigir e encerrar o cadastro | ✅ | ✅ |
| Perfis `CLIENTE` e `ADMIN`, regra de dono da conta e gestão de usuários | ✅ | — |
| Erros com `codigo` estável e mensagem para exibir | ✅ | ✅ |
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
| Quem fez o quê? | os três são **idealizadores** do banco · **Kevin Richard**: idealizador e responsável pelo backend Java/Spring · **Renan Barros**: idealizador e responsável pelo frontend Next.js · **Fellipe Santana**: idealizador, PO & QA e primeiro cliente |
| Nível técnico? | transação atômica, lock otimista, Flyway, Spring Security + JWT (OAuth2 Resource Server), BCrypt, regra de dono, logs rastreáveis, **266 testes**, CI com CodeQL |
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
| [`AutenticacaoService.java`](backend/src/main/java/com/br/rkfbank/services/AutenticacaoService.java) | login sem enumeração de CPF (mesma resposta e mesmo tempo) |
| [`db/migration/`](backend/src/main/resources/db/migration) | `V1` a `V5`: uma tabela por versão (endereço, cliente, conta, lançamento, usuário) |
| [`db/seed/`](backend/src/main/resources/db/seed) | callbacks `afterMigrate`: `equipe/` (Fellipe e Renan, login só pelo `.env`) e `visitante/` (Ana e Bruno, contas públicas) |
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
5. **Fellipe:** o roteiro de aceite completo, com os acessos da equipe, está no guia privado da equipe (fora do repositório); a conferência automática é `node scripts/rodar-colecao-insomnia.mjs` (91 requisições).
6. Recados entre o time: [planing/recados.md](planing/recados.md).

</details>

<img src="assets/readme/divisor.svg" width="100%" alt=""/>

## 🎮 Aventura: sua primeira semana no RKF Bank

> Você é o Fellipe, um dos idealizadores e o primeiro cliente do RKF Bank. Faça as escolhas e veja o que a API responde. 👇 Clique para decidir.

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

<details>
<summary>🔥 <b>Fase bônus: o fim de semana</b> (desbloqueada! clique)</summary>
<br/>

**Sábado.** Você está no ônibus, manda **R$ 30,00** para o Renan e o 4G cai no meio. O app não sabe se deu certo.

<details>
<summary>📶 Apertar "Transferir" de novo quando o sinal voltar</summary>
<br/>

✅ **`204`** com o header **`Idempotency-Replayed: true`**: o banco reconheceu a mesma `Idempotency-Key` e **não debitou de novo**. Saldo: **R$ 70,00**, não R$ 40,00. A chave foi gravada na mesma transação do dinheiro da primeira tentativa.

</details>

<details>
<summary>✏️ Mudar o valor para R$ 35,00 e reenviar com a mesma chave</summary>
<br/>

❌ **`422`**: `Idempotency-Key ja usada em outra requisicao`. A chave pertence à operação de R$ 30,00; para outra operação, outra chave. O site faz isso sozinho: editou o campo, chave nova.

</details>

**Domingo.** Você desconfia que alguém viu você digitando a senha.

<details>
<summary>🔑 Trocar a senha pelo app</summary>
<br/>

✅ **`204`**: `PATCH /usuarios/me/senha` pediu a senha atual (um token roubado não basta) e gravou a nova. **Todo token emitido antes** caiu na hora: quem estivesse com o seu acesso recebe **`401`** na próxima chamada. Você entra de novo com a senha nova.

</details>

<details>
<summary>🎲 Errar a senha atual cinco vezes seguidas</summary>
<br/>

🧱 **`429 Too Many Requests`** com `Retry-After`: o mesmo freio do login. Quem tenta adivinhar a sua senha espera 15 minutos a cada 5 chutes.

</details>

**Segunda de novo.** O Kevin te promove a **ADMIN** para você aprovar o MVP.

<details>
<summary>🛡️ Continuar usando o app com o token de antes</summary>
<br/>

🔄 **`401`** `Seu acesso foi alterado ou desativado; faca login novamente`: o perfil no banco mudou e o token antigo dizia `CLIENTE`. Você entra de novo e o token novo já vem com `ADMIN`, abrindo `GET /clientes` e a gestão de usuários. 🏆 **Zerou o RKF Bank!**

</details>

</details>

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

`409 Conflict`. E-mail é comparado sem diferenciar maiúsculas (`Joao@x.com` = `joao@x.com`: o `ClienteService` guarda tudo em minúsculas).

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
| 🌍 qualquer pessoa | `POST /clientes` (cadastro) · `POST /auth/login` · `GET /actuator/health` |
| 👤 cliente logado | só as **próprias** contas: abrir, listar, depositar, sacar, transferir (origem), extrato |
| 🛠️ `ADMIN` | `GET /clientes`, todas as contas em `GET /contas` e a gestão de usuários em `/admin/usuarios` (promover, rebaixar, desativar, redefinir senha; vale na hora). O primeiro admin nasce do `.env` |

<details open>
<summary><b>🛡️ Spring Security por dentro: toda requisição passa por 4 portões</b></summary>
<br/>

```mermaid
flowchart LR
    R(["📨 Requisição<br/>Bearer eyJ..."]) --> P1{"🔏 Portão 1<br/>assinatura HS256<br/>e validade"}
    P1 -- "adulterado, alg:none,<br/>vencido" --> X1["401<br/>Token invalido"]
    P1 -- ok --> P2{"🗄️ Portão 2<br/>usuário existe, ativo,<br/>mesmo perfil e<br/>mesma versão de senha?"}
    P2 -- "desativado, rebaixado,<br/>trocou a senha" --> X2["401<br/>acesso alterado"]
    P2 -- ok --> P3{"🎫 Portão 3<br/>a rota exige ADMIN?"}
    P3 -- "CLIENTE em /admin/**" --> X3["403<br/>perfil sem acesso"]
    P3 -- ok --> P4{"🏠 Portão 4<br/>a conta é sua?"}
    P4 -- "conta de outro" --> X4["403<br/>não é dono"]
    P4 -- ok --> OK(["✅ 200 / 201 / 204"])
```

| Portão | Quem faz | Por que existe |
|---|---|---|
| 1 | `BearerTokenAuthenticationFilter` (OAuth2 Resource Server do Spring) | ninguém fabrica token sem o `JWT_SEGREDO` |
| 2 | `ConversorTokenUsuario` (nosso) | JWT é *stateless*: sem este portão, rebaixar ou desativar alguém só valeria quando o token vencesse |
| 3 | `AuthorizationFilter` + `SecurityFilterChain` | perfis `CLIENTE` e `ADMIN` |
| 4 | `ContaService` | o `clienteId` vem do **token assinado**, nunca da URL ou do corpo |

**E antes de tudo, no login:** 5 senhas erradas para o mesmo CPF em 15 min → **`429`** (nem a senha certa entra), e
CPF inexistente e senha errada dão a **mesma** resposta, no mesmo tempo.

<details>
<summary>🎛️ O que o admin muda e quando passa a valer (clique)</summary>
<br/>

| Ação | Rota | Token antigo da pessoa | Novo login |
|---|---|:-:|---|
| Promover a ADMIN | `PATCH /admin/usuarios/{id}/perfil` | ❌ `401` na hora | ✅ já vem como ADMIN |
| Rebaixar a CLIENTE | `PATCH /admin/usuarios/{id}/perfil` | ❌ `401` na hora | ✅ volta a CLIENTE |
| Desativar ("remover") | `PATCH /admin/usuarios/{id}/acesso` | ❌ `401` na hora | ❌ `CPF ou senha invalidos` |
| Redefinir a senha de quem esqueceu | `PATCH /admin/usuarios/{id}/senha` | ❌ `401` na hora | ✅ com a senha nova |
| Trocar a **própria** senha | `PATCH /usuarios/me/senha` (pede a atual) | ❌ `401` na hora | ✅ com a senha nova |

Ninguém mexe no **próprio** perfil ou acesso: o admin não se tranca para fora e sempre sobra um ADMIN ativo.

</details>

</details>

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
<summary><b>🧪 Laboratório: tente quebrar o banco (adivinhe a resposta antes de abrir)</b></summary>
<br/>

Suba o perfil `visitante` ([VISITANTES.md](VISITANTES.md)), entre como a Ana e tente cada ataque. Pense na resposta, depois clique.

<details>
<summary>1. Trocar <code>"perfil":"CLIENTE"</code> por <code>"ADMIN"</code> dentro do token</summary>
<br/>

**`401`**: o payload do JWT é só Base64 (qualquer um lê e edita), mas a assinatura HMAC-SHA256 deixa de bater. Sem o segredo, não existe token de admin.

</details>

<details>
<summary>2. Mandar um token com <code>"alg":"none"</code> e sem assinatura</summary>
<br/>

**`401`**: o decoder aceita **só** HS256. É um ataque clássico que derrubou muitas APIs; aqui virou teste automatizado permanente.

</details>

<details>
<summary>3. Ver o extrato do Bruno trocando o id na URL</summary>
<br/>

**`403`**: a regra de dono usa o `clienteId` do **token assinado**, nunca o da URL ou do corpo. E o `403` vem antes de qualquer regra que revele se a conta tem saldo.

</details>

<details>
<summary>4. Repetir uma transferência dez vezes com a mesma <code>Idempotency-Key</code></summary>
<br/>

**Uma** transferência e nove `204` com `Idempotency-Replayed: true`. Duas chamadas exatamente ao mesmo tempo? O `UNIQUE (usuario_id, chave)` deixa só uma gravar; a outra leva `409`.

</details>

<details>
<summary>5. Chutar a senha do Bruno até acertar</summary>
<br/>

**`429`** na sexta tentativa, e nem a senha certa entra por 15 minutos. CPF inexistente e senha errada dão a **mesma** resposta: não dá nem para descobrir quem é cliente.

</details>

<details>
<summary>6. Sacar mais do que o saldo, ou com três casas decimais</summary>
<br/>

**`422`** `Saldo insuficiente` e **`400`** `valor deve ter no maximo 2 casas decimais`. Dinheiro é `BigDecimal` com 2 casas e lock otimista: nenhum centavo aparece ou some.

</details>

**Placar:** 6 de 6 ataques barrados. Quer o roteiro completo, com cada resposta? [Guia de testes](backend/README_TESTES_E2E.md).

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
- ✅ Limite de tentativas de login (`429` depois de 5 erros) e movimentações idempotentes (`Idempotency-Key`).
- ✅ Nenhuma senha real no repositório: contas da equipe e admin ganham login só pelo `.env`; o perfil público `visitante` tem contas fictícias e segredo do JWT sorteado.
- ✅ Direitos do titular: **ver** (`GET /clientes/me`), **corrigir** (`PATCH /clientes/me`) e **encerrar** o cadastro (`POST /clientes/me/encerramento`, com senha e saldo zero). O cadastro encerrado fica guardado pelo prazo legal (5 anos, Lei 9.613/1998) e uma rotina diária o **anonimiza** depois.

**Falta antes de colocar no ar** (bloqueia o deploy, não o desenvolvimento):

- ⏳ Política de privacidade e termos de uso de verdade (o cadastro já cita os dois).
- ⏳ Registrar o aceite dos termos (versão + data) numa migração nova do Flyway.
- ⏳ Encarregado e canal de contato do titular.

**Depois do MVP:** auditoria (quem fez o quê, quando e de onde).
Detalhes e prioridades em [planing/debitos-tecnicos.md](planing/debitos-tecnicos.md) e [planing/decisoes.md](planing/decisoes.md).

</details>

<img src="assets/readme/divisor.svg" width="100%" alt=""/>

## 🔌 API REST

Base URL: `http://localhost:8080` · JSON · IDs em UUID · dinheiro com 2 casas (`150.00`) · datas ISO-8601 UTC · rotas privadas com `Authorization: Bearer <token>`.
Contrato completo (com a versão atual e o histórico de mudanças): [backend/README_CONTRATO_API.md](backend/README_CONTRATO_API.md).

| Método | Rota | O que faz | Quem pode | Sucesso |
|---|---|---|---|---|
| `POST` | `/clientes` | cadastra cliente (com senha) | público | `201` |
| `POST` | `/auth/login` | login com CPF + senha (5 erros em 15 min → `429`) | público | `200` + token |
| `PATCH` | `/usuarios/me/senha` | troca a própria senha (pede a atual; derruba os tokens antigos) | logado | `204` |
| `GET` | `/clientes/me` | meus dados (LGPD) | o próprio cliente | `200` |
| `PATCH` | `/clientes/me` | corrige meus dados (LGPD; CPF não muda) | o próprio cliente | `200` |
| `POST` | `/clientes/me/encerramento` | encerra o cadastro (senha + saldo zero; anonimiza depois de 5 anos) | o próprio cliente | `200` |
| `GET` | `/clientes` | lista clientes (`?page=0&size=10`) | `ADMIN` | `200` |
| `POST` | `/contas` | abre conta no próprio cadastro (cliente vem do token) | o próprio cliente | `201` |
| `GET` | `/contas` | lista contas | `ADMIN`: todas · `CLIENTE`: as dele | `200` |
| `POST` | `/contas/{contaId}/depositos` | deposita | dono da conta | `204` |
| `POST` | `/contas/{contaId}/saques` | saca | dono da conta | `204` |
| `POST` | `/transferencias` | transfere | dono da conta de origem | `204` |
| `GET` | `/contas/{contaId}/lancamentos` | extrato (mais recente primeiro) | dono da conta | `200` |
| `GET` | `/admin/usuarios` | lista os usuários (perfil, ativo) | `ADMIN` | `200` |
| `PATCH` | `/admin/usuarios/{usuarioId}/perfil` | promove a `ADMIN` ou rebaixa a `CLIENTE` | `ADMIN` (não em si mesmo) | `200` |
| `PATCH` | `/admin/usuarios/{usuarioId}/acesso` | desativa ou reativa o acesso | `ADMIN` (não em si mesmo) | `200` |
| `PATCH` | `/admin/usuarios/{usuarioId}/senha` | redefine a senha de quem esqueceu | `ADMIN` | `204` |

<details>
<summary><b>👤 <code>POST /clientes</code> — cadastrar</b></summary>
<br/>

```json
{
  "nomeCompleto": "Cliente Novo do Insomnia",
  "cpf": "529.982.247-25",
  "email": "cliente.novo@exemplo.test",
  "telefone": "(11) 99999-8888",
  "cep": "06020-010",
  "numero": "123",
  "complemento": "Apto 45",
  "senha": "minha-senha-forte"
}
```

CPF, telefone e CEP podem ir **com máscara**, do jeito que a pessoa digita: a API higieniza (fica só com os dígitos),
valida o CPF pelos dígitos verificadores e busca logradouro, bairro, cidade e UF no **ViaCEP** (aqui, Osasco/SP).
Resposta `201`: o cliente com o endereço completo. A senha **nunca** volta.

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
// POST /contas (o cliente vem do token; agência e número são gerados pelo banco)
{ "tipoConta": "CORRENTE" }

// POST /contas/{contaId}/depositos  e  /contas/{contaId}/saques   (header Idempotency-Key obrigatório)
{ "valor": 150.00, "descricao": "Deposito pelo Insomnia" }

// POST /transferencias   (header Idempotency-Key obrigatório)
{ "contaOrigemId": "407d727e-...", "contaDestinoId": "566dd28f-...", "valor": 25.00, "descricao": "Transferencia pelo Insomnia" }
```

A `Idempotency-Key` é um UUID novo **por operação**: repetir o mesmo pedido com a mesma chave (rede caiu, clique
duplo) responde `204` com `Idempotency-Replayed: true` e não move o dinheiro de novo.

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
  "timestamp": "2026-10-23T11:19:54.090042Z",
  "status": 422,
  "error": "Unprocessable Content",
  "codigo": "SALDO_INSUFICIENTE",
  "message": "Saldo insuficiente",
  "path": "/contas/407d727e-.../saques",
  "campos": []
}
```

**Decida pelo `status` e pelo `codigo`; o `message` é só para exibir.** Código e mensagem moram juntos no enum
[`CodigoErroEnum`](backend/src/main/java/com/br/rkfbank/exceptions/CodigoErroEnum.java) (a tabela completa está no
[contrato](backend/README_CONTRATO_API.md#6-erros)).

| Status | Quando | Alguns `codigo` |
|---|---|---|
| `400` | dados inválidos, JSON mal formado ou parâmetro inválido | `CAMPOS_INVALIDOS` (+ `campos`), `JSON_INVALIDO` |
| `401` | sem token, token inválido ou vencido, acesso alterado, CPF/senha errados | `TOKEN_INVALIDO`, `ACESSO_ALTERADO`, `CREDENCIAIS_INVALIDAS` |
| `403` | logado, mas sem permissão | `CONTA_DE_OUTRO_CLIENTE`, `PERFIL_SEM_PERMISSAO` |
| `404` | cliente, conta ou usuário não encontrado | `CONTA_NAO_ENCONTRADA` |
| `409` | dado repetido ou operação simultânea | `CPF_JA_CADASTRADO`, `CONCORRENCIA`, `IDEMPOTENCIA_EM_ANDAMENTO` |
| `422` | regra de negócio recusou | `SALDO_INSUFICIENTE`, `MESMA_CONTA`, `CEP_NAO_ENCONTRADO`, `SENHA_ATUAL_INCORRETA` |
| `429` | 5 logins errados seguidos | `MUITAS_TENTATIVAS` (+ `Retry-After`) |
| `503` | ViaCEP fora do ar | `SERVICO_INDISPONIVEL` |

</details>

<details>
<summary><b>🧪 Fluxo E2E completo com <code>curl</code> (o mesmo da pasta 02 do Insomnia)</b></summary>
<br/>

Cada passo tem o **mesmo nome, rota e corpo** da pasta **"02 · Fluxo E2E: cliente novo, do cadastro ao extrato"** da
[coleção do Insomnia](backend/insomnia/rkfbank.insomnia.json). Com a API no ar (`./mvnw spring-boot:run -Dspring-boot.run.profiles=visitante`):

```bash
API=http://localhost:8080
CPF=529.982.247-25    # com máscara de propósito: a API higieniza

# 1. Cadastrar cliente (público): CPF, telefone e CEP com máscara; endereço vem do ViaCEP
curl -X POST $API/clientes -H "Content-Type: application/json" -d '{
  "nomeCompleto":"Cliente Novo do Insomnia","cpf":"'$CPF'","email":"cliente.novo@exemplo.test",
  "telefone":"(11) 99999-8888","cep":"06020-010","numero":"123","complemento":"Apto 45","senha":"minha-senha-forte"}'

# 2. Login · cliente novo: guarde o token
curl -X POST $API/auth/login -H "Content-Type: application/json" -d '{"cpf":"'$CPF'","senha":"minha-senha-forte"}'
TOKEN=cole-o-token-aqui
AUTH="Authorization: Bearer $TOKEN"; JSON="Content-Type: application/json"

# 3. Abrir conta ORIGEM (corrente) e conta DESTINO (poupança): guarde os dois "id"
curl -X POST $API/contas -H "$AUTH" -H "$JSON" -d '{"tipoConta":"CORRENTE"}'
curl -X POST $API/contas -H "$AUTH" -H "$JSON" -d '{"tipoConta":"POUPANCA"}'
ORIGEM=cole-o-id-da-corrente; DESTINO=cole-o-id-da-poupanca

# 4. Depositar 150,00 · sacar 50,00 · transferir 25,00 (cada operação com uma Idempotency-Key nova)
curl -X POST $API/contas/$ORIGEM/depositos -H "$AUTH" -H "$JSON" -H "Idempotency-Key: $(uuidgen)" \
  -d '{"valor":150.00,"descricao":"Deposito pelo Insomnia"}'
curl -X POST $API/contas/$ORIGEM/saques -H "$AUTH" -H "$JSON" -H "Idempotency-Key: $(uuidgen)" \
  -d '{"valor":50.00,"descricao":"Saque pelo Insomnia"}'
curl -X POST $API/transferencias -H "$AUTH" -H "$JSON" -H "Idempotency-Key: $(uuidgen)" \
  -d '{"contaOrigemId":"'$ORIGEM'","contaDestinoId":"'$DESTINO'","valor":25.00,"descricao":"Transferencia pelo Insomnia"}'

# 5. Saldos (ORIGEM 75,00 · DESTINO 25,00) e extratos
curl $API/contas -H "$AUTH"
curl $API/contas/$ORIGEM/lancamentos -H "$AUTH"     # 3 lançamentos: transferência, saque, depósito
curl $API/contas/$DESTINO/lancamentos -H "$AUTH"    # 1 lançamento: a transferência recebida
```

Sem `uuidgen`? Use qualquer texto único de 8 a 100 caracteres (`deposito-001`, `saque-001`...).
CPF já cadastrado? Troque por outro válido: `70483726052`, `39053344705`, `11144477735`.
A coleção inteira (91 requisições, com os erros esperados) roda pelo terminal: `node scripts/rodar-colecao-insomnia.mjs`.

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
        C -. erros .-> EH[TratadorGlobalDeErros<br/>status + CodigoErroEnum]
    end

    R --> DB[(MySQL 8.4<br/>schema pelo Flyway)]
    V -->|HTTPS| VC[[viacep.com.br]]
```

<div align="center">
  <img src="assets/readme/fluxo-transferencia.svg" width="100%" alt="Animação: uma transferência atravessa Next.js, Spring Security, controller, service e MySQL e volta com 204"/>
</div>

<details>
<summary><b>🧩 Cada camada, ponto a ponto: o que faz, por que existe e onde está</b> (clique)</summary>
<br/>

| # | Camada | Papel | Por que importa | No código |
|:-:|---|---|---|---|
| 1 | **Filtros** | dão um `X-Request-Id` a cada chamada e registram método, rota, status e tempo | rastrear uma requisição em todos os logs | [`FiltroLogRequisicao`](backend/src/main/java/com/br/rkfbank/config/FiltroLogRequisicao.java) |
| 2 | **Segurança** | valida o JWT, confere no banco se o usuário ainda vale e barra `401`/`403` por perfil | ninguém chega ao controller sem identidade | [`ConfiguracaoSeguranca`](backend/src/main/java/com/br/rkfbank/config/ConfiguracaoSeguranca.java) · [`ConversorTokenUsuario`](backend/src/main/java/com/br/rkfbank/security/ConversorTokenUsuario.java) |
| 3 | **Controllers** | traduzem HTTP ↔ Java: rota, `@Valid` no DTO, quem está logado (`UsuarioLogado`) e o status de sucesso | a camada web fica fina; regra nenhuma mora aqui | [`controllers/`](backend/src/main/java/com/br/rkfbank/controllers) |
| 4 | **DTOs** (`record`) | o formato de entrada e saída da API, com as validações de formato | a entidade JPA nunca vaza; o contrato não muda quando o banco muda | [`dto/`](backend/src/main/java/com/br/rkfbank/dto) |
| 5 | **Services** | a regra de negócio: dono da conta, saldo, conta ativa, idempotência, LGPD; abrem a transação (`@Transactional`) | tudo-ou-nada no dinheiro; testável sem HTTP | [`services/`](backend/src/main/java/com/br/rkfbank/services) |
| 6 | **Mapeamento** | métodos `paraResponse`/`paraContaResponse` convertem entidade → DTO | só o que a tela usa sai da API (LGPD) | dentro de cada service |
| 7 | **Repositories** | consultas ao banco pelo Spring Data JPA | o service pede "contas do cliente", não escreve SQL | [`repositories/`](backend/src/main/java/com/br/rkfbank/repositories) |
| 8 | **Entities** | as tabelas como classes; `Conta` tem `@Version` (lock otimista) | duas operações na mesma conta não se sobrescrevem | [`entities/`](backend/src/main/java/com/br/rkfbank/entities) |
| 9 | **Clients** | conversam com sistemas de fora (ViaCEP) com timeout | falha externa vira `503`, não `500` | [`ViaCepClient`](backend/src/main/java/com/br/rkfbank/clients/viacep/ViaCepClient.java) |
| 10 | **Exceções + `CodigoErroEnum`** | o service lança `new RegraNegocioException(CodigoErroEnum.SALDO_INSUFICIENTE)`; o enum guarda o código e a mensagem | um lugar só para o texto de cada erro; o front decide pelo `codigo` | [`exceptions/`](backend/src/main/java/com/br/rkfbank/exceptions) |
| 11 | **Tratador de erros** | escolhe o status HTTP de cada exceção e monta o JSON padrão | todo erro sai no mesmo formato | [`TratadorGlobalDeErros`](backend/src/main/java/com/br/rkfbank/controllers/handlers/TratadorGlobalDeErros.java) |
| 12 | **Flyway** | cria e evolui o schema (V1 a V9) e carrega as contas de teste | o banco de todo mundo é igual ao de produção | [`db/migration`](backend/src/main/resources/db/migration) |

</details>

<details>
<summary><b>🛣️ Uma transferência do clique ao banco, passo a passo</b> (clique)</summary>
<br/>

```mermaid
sequenceDiagram
    autonumber
    participant T as Tela (Next.js)
    participant F as Filtros + Security
    participant C as ContaController
    participant I as IdempotenciaService
    participant S as ContaService
    participant R as ContaRepository
    participant DB as MySQL
    T->>F: POST /transferencias + Bearer + Idempotency-Key
    F->>F: X-Request-Id, JWT válido? usuário ativo?
    alt token ausente ou inválido
        F-->>T: 401 (TOKEN_INVALIDO...)
    end
    F->>C: requisição autenticada
    C->>C: @Valid no TransferenciaRequestDto (400 CAMPOS_INVALIDOS)
    C->>I: executarUmaVez(chave, assinatura)
    alt mesma chave e mesmo corpo
        I-->>T: 204 + Idempotency-Replayed: true (nada se move)
    end
    I->>S: transferir(request, usuario)
    S->>R: busca origem (dona?) e destino (ativa?)
    S->>S: saldo suficiente? (422 SALDO_INSUFICIENTE)
    S->>R: debita origem, credita destino, grava lançamento
    R->>DB: UPDATE + INSERT na mesma transação (@Version)
    DB-->>T: COMMIT → 204 No Content
```

Mais diagramas (telas, containers, rastreabilidade tela → tabela, sessão): **[🗺️ Atlas de fluxos](FLUXOS.md)**.

</details>

<details>
<summary><b>🆚 Antes × depois: decisões que mudaram o projeto</b> (clique em cada uma)</summary>
<br/>

<details><summary>🗄️ Banco: <code>ddl-auto=update</code> → Flyway</summary>

| Antes | Depois |
|---|---|
| o Hibernate "adivinhava" o schema: FKs com nome aleatório e sem índice no extrato | `V1` a `V5` versionados (uma tabela por versão), revisados em PR e iguais em todo ambiente |
| renomear um campo criava uma coluna nova e deixava os dados na velha | `ALTER TABLE ... RENAME COLUMN` numa migração nova |

</details>
<details><summary>🔐 Acesso: qualquer um movimentava qualquer conta → JWT + regra de dono</summary>

| Antes | Depois |
|---|---|
| `POST /contas/{id}/saques` funcionava sem login, para qualquer id | sem token dá `401`; conta de outra pessoa dá `403` |
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

<details open>
<summary><b>🛫 Flyway: do banco vazio à API no ar</b></summary>
<br/>

```mermaid
timeline
    title O banco do RKF Bank, versão a versão
    V1 a V5 : endereço, cliente, conta, lançamento, usuário : uma tabela por versão
    V6 : idempotência : o dinheiro nunca sai duas vezes
    V7 : usuário ativo : desativar alguém vale na hora
    V8 : versão da senha : trocar a senha derruba os tokens antigos
    a cada subida : callback afterMigrate : contas da equipe ou de visitante, sem duplicar
```

```mermaid
sequenceDiagram
    autonumber
    participant API as Spring Boot
    participant F as Flyway
    participant DB as MySQL
    participant H as Hibernate
    API->>F: subir
    F->>DB: o que já rodou? (flyway_schema_history)
    F->>DB: aplica só as versões que faltam (V1…V8)
    F->>DB: afterMigrate: contas prontas, só o que falta
    API->>H: ddl-auto=validate
    H->>DB: as entidades batem com as tabelas?
    alt alguém mudou a entidade e esqueceu a migração
        H-->>API: ❌ a API nem sobe
    else tudo bate
        API->>DB: InicializadorAdmin e InicializadorAcessoEquipe (logins do .env)
        API-->>API: ✅ Tomcat no ar
    end
```

<details>
<summary>🤔 E se... (clique em cada um)</summary>
<br/>

<details><summary>...alguém editar a <code>V1</code> depois de aplicada?</summary>

❌ `Migration checksum mismatch`: o Flyway guarda o checksum de cada versão e a API recusa subir. Mudou algo? **Versão nova** (`V9__...`).

</details>
<details><summary>...alguém criar um campo na entidade e esquecer a migração?</summary>

❌ O Hibernate em `validate` compara entidade × tabela e a API **não sobe**: o erro aparece no seu computador, não em produção.

</details>
<details><summary>...a API reiniciar dez vezes?</summary>

✅ As versões rodam uma vez só; o callback `afterMigrate` roda nas dez, mas só insere o que falta. Nada duplica e saldo movimentado nunca é resetado.

</details>
<details><summary>...subir em produção?</summary>

✅ `FLYWAY_LOCATIONS=classpath:db/migration`: só o schema, **nenhuma** conta de teste.

</details>

</details>

</details>

<details>
<summary><b>🗃️ Modelo de dados (migrações V1 a V9)</b></summary>
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
<summary><b>⚡ Visitante: API em 1 comando, sem Docker (perfil <code>visitante</code>)</b></summary>
<br/>

```bash
./mvnw spring-boot:run -Dspring-boot.run.profiles=visitante   # PowerShell: .\mvnw.cmd spring-boot:run "-Dspring-boot.run.profiles=visitante"
```

Sobe com H2 em memória, sem `.env`, com duas contas fictícias prontas e segredo do JWT sorteado (ninguém forja token).
Não existe admin nem conta da equipe nesse perfil. Guia de 5 minutos: **[VISITANTES.md](VISITANTES.md)**.

| Quem | CPF (login) | Senha | Conta |
|---|---|---|---|
| **Ana Visitante (Origem)** | `246.813.579-28` | `Visitante@2026` | 0001 / 900001 · R$ 1.000,00 |
| **Bruno Visitante (Destino)** | `975.318.642-82` | `Visitante@2026` | 0001 / 900002 · R$ 500,00 |

Roteiro completo (H2, MySQL, Insomnia, permissões, caminho feliz, idempotência, 401/403/429, erros, Spring Security + OAuth2):
[como testar o projeto](backend/README_TESTES_E2E.md). Quando usar cada perfil:
[backend/README.md → Perfis](backend/README.md#-perfis-qual-usar-e-quando).

</details>

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

O Spring **lê o `.env` da raiz sozinho** (desde 20/10/2026): basta rodar da raiz do repositório.

```bash
./mvnw spring-boot:run        # PowerShell: .\mvnw.cmd spring-boot:run
```

As tabelas nascem pelo **Flyway** (`V1` a `V9`) e, a cada subida, o callback `afterMigrate` garante as contas da
equipe (Fellipe = origem, Renan = destino). O login delas vem da `EQUIPE_SENHA` do `.env`: os acessos ficam no guia
privado da equipe, fora do repositório. As migrações foram reorganizadas em 20/10/2026: se a API recusar com
*Migration checksum mismatch* ou *applied migration not resolved locally*, rode
`docker compose down -v && docker compose up -d` **uma vez** (apaga os dados locais).

</details>

<details open>
<summary><b>🧪 Testar: pela IDE, no Insomnia e olhando o banco</b></summary>
<br/>

| Quero... | Como |
|---|---|
| rodar pela IntelliJ | o projeto traz 3 configurações prontas (pasta `.run/`): **API · Visitante (H2)**, **API · Equipe (H2)** e **API · MySQL (Docker)** |
| testar sem copiar e colar | importe [`backend/insomnia/rkfbank.insomnia.json`](backend/insomnia/rkfbank.insomnia.json) no Insomnia: 91 requisições em ordem (a pasta 02 é o roteiro E2E completo), e o login guarda o token sozinho |
| ver as tabelas no H2 | `http://localhost:8080/h2-console` · JDBC URL `jdbc:h2:mem:rkfbank-visitante` (ou `rkfbank-equipe`) · usuário `sa`, senha vazia |
| ver as tabelas no MySQL | `localhost:5441` com o usuário do `.env` (IntelliJ *Database*, DBeaver) ou `docker exec` no container `rkfbank-mysql` |
| tornar alguém ADMIN | pelo `.env` (primeiro admin), pela API (`PATCH /admin/usuarios/{id}/perfil`) ou por SQL num banco local |

Passo a passo de tudo isso, com as permissões de cada rota e todos os payloads:
**[Como testar o projeto, do zero](backend/README_TESTES_E2E.md)**.

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

Abra <http://localhost:3000> e entre em `/login` com uma conta de teste (visitante, equipe) ou crie uma em `/cadastro`.

</details>

<details>
<summary><b>🧯 Problemas comuns</b></summary>
<br/>

| Sintoma | Causa provável | Solução |
|---|---|---|
| `Rule 0: RequireJavaVersion failed` | JDK diferente do 25 | instale o JDK 25 e aponte o `JAVA_HOME` |
| `JWT_SEGREDO ausente ou curto` | `.env` sem segredo, ou rodou fora da raiz | gere com `openssl rand -base64 48` e rode da raiz |
| `Access denied for user` | rodou fora da raiz (o `.env` não foi achado) | rode da raiz do repositório |
| `Communications link failure` | MySQL desligado ou subindo | `docker compose ps` e espere o `healthy` |
| `Validate failed: Migration checksum mismatch` | MySQL de antes da reorganização de 20/10, ou uma migração aplicada foi editada | 1x por máquina: `docker compose down -v && docker compose up -d`; fora isso, nunca edite `V<n>__`, crie uma nova versão |
| Login da Ana dá `401` | a API não está no perfil `visitante` | suba com `-Dspring-boot.run.profiles=visitante` |
| Toda movimentação dá `400` | faltou o header `Idempotency-Key` | gere um UUID por operação ([contrato](backend/README_CONTRATO_API.md#idempotência-idempotency-key)) |
| Login dá `429` | 5 senhas erradas seguidas | espere 15 min ou reinicie a API |
| Cadastro devolve `503` | ViaCEP fora do ar | tente de novo em alguns segundos |
| Volta para o login toda hora | token de 15 min venceu | esperado: ainda não há refresh token |

</details>

<img src="assets/readme/divisor.svg" width="100%" alt=""/>

## 🧪 Qualidade e testes

<div align="center">
  <img src="assets/readme/terminal.svg" width="100%" alt="Terminal: 266 testes, 0 falhas, BUILD SUCCESS"/>
</div>

```bash
scripts/validar.sh backend          # testes em H2, resumo curto (sem Docker)
scripts/validar.sh frontend         # lint + typecheck + build
scripts/testar-no-mysql.sh          # testes de integração contra o MySQL do Docker
node scripts/smoke-e2e.mjs          # fluxo real contra a API no ar
node scripts/rodar-colecao-insomnia.mjs  # a coleção do Insomnia inteira pelo terminal
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
| `TokenServiceTest` / `AutenticacaoServiceTest` | JWT (claims, 15 min, adulteração, chave errada) e login sem enumeração |
| `UsuarioServiceTest` / `InicializadorAdminTest` | primeiro `ADMIN` pelo `.env` |
| `SegurancaIntegracaoTest` | Spring inteiro com tokens reais: `401`, `403`, `429`, perfis, `alg: none` |
| `IdempotenciaServiceTest` / `DadosEquipeIntegracaoTest` | dinheiro nunca sai duas vezes (`Idempotency-Key`) e saldo sempre bate com o extrato |
| `GestaoUsuariosIntegracaoTest` | promover, rebaixar, desativar e trocar senha valendo na hora |
| `PerfilVisitanteIntegracaoTest` | o perfil público não expõe nada da equipe |
| `ClienteControllerTest` / `ContaControllerTest` | status de cada rota e limites da paginação |
| `TratadorGlobalDeErrosTest` | cada exceção no formato de erro |
| `ViaCepClientTest` | CEP encontrado, inexistente (`422`) e ViaCEP fora (`503`) |
| `CpfValidoValidatorTest` | dígitos verificadores e máscara |
| `FiltroLogRequisicaoTest` | `X-Request-Id` reaproveitado, gerado e rejeitado contra log forjado |
| `ConsoleH2LigadoTest` / `ConsoleH2DesligadoTest` | console do H2 aberto só nos perfis com H2, sem afrouxar a segurança da API |
| `*RepositoryTest` | consultas, constraints, cascata, `@Version` e `tb_usuario` (`@DataJpaTest` com as migrações do Flyway) |

Relatório de cobertura: `target/site/jacoco/index.html`. Teste manual de aceite: [roteiro E2E](backend/README_TESTES_E2E.md) + [coleção do Insomnia](backend/insomnia/rkfbank.insomnia.json).

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
INFO  [http-nio-8080-exec-1] [e2e-transf-001] c.b.r.c.FiltroLogRequisicao : POST /transferencias -> 204 (15 ms)
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
    Telas da conta e login               :done,   f0, 2026-10-14, 4d
    Flyway V1 a V3                       :done,   f1, 2026-10-15, 2d
    Spring Security + JWT                :done,   f2, 2026-10-16, 2d
    Idempotência, gestão de usuários, Flyway V1 a V8 :done, f3, 2026-10-20, 1d
    section MVP
    Testcontainers (MySQL real)          :active, m0, 2026-10-21, 2d
    GET conta e contas do cliente        :        m1, after m0, 1d
    LGPD mínimo                          :        m2, after m1, 2d
    Deploy (Docker, perfil prod, banco)  :crit,   m4, after m2, 3d
    Testes do Fellipe e correções         :        m3, after m4, 2d
    section Depois
    Swagger / OpenAPI                    :        p2, 2026-11-02, 2d
    Pix, boleto e cartão (simulados)     :        p3, 2026-11-05, 20d
```

<details open>
<summary><b>🚀 MVP — até 31/10/2026</b></summary>
<br/>

- [x] Cadastro com ViaCEP, abertura de conta, depósito, saque, transferência e extrato
- [x] Logs rastreáveis, CI, CodeQL e Dependabot
- [x] Banco versionado com Flyway (V1 a V9), contas da equipe (Fellipe e Renan) e perfil público de visitante
- [x] Idempotência nas movimentações, número de conta gerado pelo banco, limite de tentativas no login e health check
- [x] Login com Spring Security + JWT, perfis e regra de dono
- [x] Frontend: landing, cadastro, login, conta, movimentações e extrato
- [x] Gestão de usuários pelo ADMIN (promover, rebaixar, desativar) e troca de senha, valendo na hora
- [ ] Testes de integração com Testcontainers + MySQL real (protege o deploy)
- [ ] Backend: `GET /contas/{id}` e `GET /clientes/{id}/contas`
- [ ] LGPD mínimo: política de privacidade, aceite dos termos e encarregado
- [ ] Fellipe testa tudo como primeiro cliente e os bugs são corrigidos
- [ ] Colocar no ar (Docker, perfil `prod` sem dados de teste, banco gerenciado) e atualizar este README com link e prints

</details>

<details>
<summary><b>🔭 Depois do MVP</b></summary>
<br/>

- [ ] `MockMvc` em todos os controllers e teste de concorrência ([checklist](planing/checklist-features.md))
- [ ] Swagger UI com o botão *Authorize* (springdoc-openapi)
- [ ] Refresh token e 2FA (limite de tentativas e revogação já existem)
- [x] "Meus dados", editar perfil e encerrar conta (direitos do titular)
- [ ] CI do frontend
- [ ] Pix, boleto, QR Code e cartão virtual (simulados) — [funcionalidades futuras](planing/debitos-tecnicos.md#funcionalidades-futuras-)
- [ ] 🤖 Assistente do extrato com Spring AI (ideia)

</details>

Planejamento detalhado: [planing/roadmap.md](planing/roadmap.md) · [checklist de features](planing/checklist-features.md) · [decisões](planing/decisoes.md).

<img src="assets/readme/divisor.svg" width="100%" alt=""/>

## 👥 Quem faz o RKF Bank

> Os três são **idealizadores** do RKF Bank: tiveram a ideia juntos, em Osasco, e dividiram o banco em três frentes, cada um com a sua função: **API** (Kevin), **site** (Renan) e **produto** (Fellipe).

<div align="center">
  <img src="assets/readme/time.svg" width="100%" alt="Os três idealizadores do RKF Bank, todos de Osasco: Kevin Richard, idealizador e backend; Fellipe Santana, idealizador, PO e QA e primeiro cliente; Renan Barros, idealizador e frontend"/>
</div>

<table>
  <tr>
    <td align="center" valign="top" width="33%">
      <a href="https://www.linkedin.com/in/kevin-richard-b68a54203/"><img src="https://github.com/k3vinrich4rd.png" width="96" alt="Kevin Richard"/></a>
      <h3>Kevin Richard</h3>
      <a href="https://www.linkedin.com/in/kevin-richard-b68a54203/"><img src="https://img.shields.io/badge/LinkedIn-Kevin_Richard-0A66C2?style=for-the-badge&logo=linkedin&logoColor=white" alt="LinkedIn de Kevin Richard"/></a>
      <br/>
      <img src="https://img.shields.io/badge/Idealizador-Backend-3B82F6?logo=springboot&logoColor=white" alt="Idealizador e responsável pelo backend"/>
      <p align="left"><sub><i>“Conhecimento nunca é demais.”</i> Movido pela vontade de aprender e de acumular experiências, quer crescer como pessoa e como profissional de forma <b>honesta e constante</b>, com determinação, foco, persistência, ambição, espírito de time e empatia.</sub></p>
      <p align="left"><sub><b>No RKF Bank:</b> <b>idealizador</b> e responsável pelo <b>backend</b>: API REST em Java e Spring Boot, MySQL com Flyway, Spring Security + JWT, ViaCEP, logs e testes. Objetivo: o dinheiro de cada cliente sempre certo e nenhum dado pessoal vazando.</sub></p>
      <a href="mailto:kevinrichardve@gmail.com"><img src="https://img.shields.io/badge/E--mail-kevinrichardve-3B82F6?logo=gmail&logoColor=white" alt="E-mail de Kevin"/></a>
      <a href="https://github.com/k3vinrich4rd"><img src="https://img.shields.io/badge/GitHub-k3vinrich4rd-181717?logo=github&logoColor=white" alt="GitHub de Kevin"/></a>
    </td>
    <td align="center" valign="top" width="33%">
      <h3>Fellipe Santana</h3>
      <img src="https://img.shields.io/badge/LinkedIn-em_breve-6B7280?style=for-the-badge&logo=linkedin&logoColor=white" alt="LinkedIn de Fellipe Santana: em breve"/>
      <br/>
      <img src="https://img.shields.io/badge/Idealizador-PO_%26_QA-E2C98A" alt="Idealizador, PO e QA"/>
      <p align="left"><sub><i>“Se não funciona para o cliente, não está pronto.”</i> Olha o banco pelos olhos de quem usa: transforma ideia em prioridade, prioridade em critério de aceite e só dá o ok quando o fluxo funciona de ponta a ponta.</sub></p>
      <p align="left"><sub><b>No RKF Bank:</b> <b>idealizador</b> e <b>PO & QA</b>: define o que o produto precisa e é o <b>primeiro cliente</b> — a conta de origem que nasce pronta em todo ambiente é a dele. Valida o MVP pelo guia de testes antes do lançamento.</sub></p>
      <img src="https://img.shields.io/badge/1º_cliente-conta_0001%2F100001-0A0F1C" alt="Primeiro cliente: agência 0001, conta 100001"/>
    </td>
    <td align="center" valign="top" width="33%">
      <a href="https://www.linkedin.com/in/renan-costa-barros/"><img src="https://github.com/renanbarrostech-cmd.png" width="96" alt="Renan Barros"/></a>
      <h3>Renan Barros</h3>
      <a href="https://www.linkedin.com/in/renan-costa-barros/"><img src="https://img.shields.io/badge/LinkedIn-Renan_Barros-0A66C2?style=for-the-badge&logo=linkedin&logoColor=white" alt="LinkedIn de Renan Barros"/></a>
      <br/>
      <img src="https://img.shields.io/badge/Idealizador-Frontend-06B6D4?logo=nextdotjs&logoColor=white" alt="Idealizador e responsável pelo frontend"/>
      <p align="left"><sub><i>“Transformar problemas em soluções.”</i> Do administrativo para a tecnologia: de Jovem Aprendiz a Assistente Financeiro em três anos, com <b>quatro promoções</b>, e já entregou na empresa um sistema em Next.js + PostgreSQL que automatizou a triagem de notas fiscais. Cursa GTI e a formação Full Stack da DevClub.</sub></p>
      <p align="left"><sub><b>No RKF Bank:</b> <b>idealizador</b> e responsável pelo <b>frontend</b>: site em Next.js, React e TypeScript, identidade visual RKF, telas da conta, login com cookie <code>httpOnly</code> e logs com máscara. Objetivo: parecer e funcionar como um banco digital de verdade.</sub></p>
      <a href="mailto:renanbarros.tech@gmail.com"><img src="https://img.shields.io/badge/E--mail-renanbarros.tech-06B6D4?logo=gmail&logoColor=white" alt="E-mail de Renan"/></a>
      <a href="https://github.com/renanbarrostech-cmd"><img src="https://img.shields.io/badge/GitHub-renanbarrostech--cmd-181717?logo=github&logoColor=white" alt="GitHub de Renan"/></a>
    </td>
  </tr>
</table>

<details>
<summary><b>📖 A história de cada um, em detalhe</b></summary>
<br/>

**Kevin Richard — backend.** Acredita que conhecimento nunca é demais e vive buscando informação nova e momentos de
aprendizado para evoluir no pessoal e no profissional. O jeito de crescer é o de quem não pula etapa: determinação,
foco, persistência e ambição, sem perder a coletividade e a empatia com quem está do lado. No RKF Bank isso aparece em
cada PR: teste antes de entregar, decisão registrada e documentação que ensina quem vem depois.

**Renan Barros — frontend.** Desde criança queria entender como as coisas funcionavam, mas a carreira começou no
administrativo: entrou como Jovem Aprendiz na Expresso Salomé em 2023 e, em três anos, conquistou quatro promoções
(Auxiliar de Escritório, Auxiliar Financeiro, Assistente Financeiro). Dali vieram organização, análise de processos e
melhoria contínua. Em transição para tecnologia, cursa Gestão da Tecnologia da Informação na Estácio e a formação Full
Stack da DevClub (140h+), e já gerou resultado real: um sistema de triagem de notas fiscais (Next.js, React,
TypeScript, Tailwind, Drizzle e PostgreSQL) que trocou um processo manual e sujeito a erro por um automatizado. Também
faz freelas de sites, landing pages, dashboards e automações para pequenas empresas. Meta: primeira vaga como Dev Full
Stack Júnior, rumo a Engenheiro de Software.

**Fellipe Santana — produto e qualidade.** É quem garante que o banco resolve o problema de quem usa: decide prioridade,
escreve o que "pronto" significa e testa como cliente — por isso a conta de origem que nasce em todo ambiente é a dele.
<sub>(Texto provisório, montado a partir do papel dele no time; será trocado pelo "Sobre" do LinkedIn assim que o Fellipe enviar o link.)</sub>

</details>

<div align="center">

Dúvida sobre a API ou o banco? **Kevin**. Telas e site? **Renan**. Produto, prioridade e testes de aceite? **Fellipe**.
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
        "description": "Kevin, Renan e Fellipe: o time inteiro é de Osasco",
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

Com `ddl-auto=update` o Hibernate "adivinha" o schema, não remove nem renomeia nada e não deixa histórico. Com Flyway, cada mudança é um SQL versionado (`V1__` a `V8__`), revisado no PR e igual em todo ambiente; o Hibernate só **valida**. Migração aplicada nunca se edita: o checksum muda e a API não sobe.

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

**4. A rede cai no meio da transferência e o app manda a mesma de novo. O dinheiro sai duas vezes?**

<details><summary>A) Sai: a API não tem como saber</summary>

❌ Tem sim: o app manda um `Idempotency-Key` por operação.

</details>
<details><summary>B) Não: a repetição volta 204 com <code>Idempotency-Replayed: true</code></summary>

✅ **Isso!** A chave foi gravada na mesma transação do dinheiro (`tb_idempotencia`, V6). A repetição só confirma.

</details>

**5. O admin rebaixa alguém que está logado com um token de ADMIN válido por mais 50 minutos. E aí?**

<details><summary>A) Continua admin até o token vencer</summary>

❌ Seria assim com JWT puro, que é *stateless*.

</details>
<details><summary>B) A próxima requisição dele dá 401</summary>

✅ **Isso!** O `ConversorTokenUsuario` confere o banco a cada requisição: o perfil do token não bate mais.

</details>

**6. Qual versão do Flyway criou a coluna que faz a troca de senha derrubar os tokens antigos?**

<details><summary>A) V5</summary>

❌ A V5 criou a `tb_usuario`.

</details>
<details><summary>B) V8</summary>

✅ **Isso!** `versao_credencial`: sobe a cada troca e vai dentro do token.

</details>

<img src="assets/readme/divisor.svg" width="100%" alt=""/>

## 📚 Documentação

| Área | Documento | O que tem |
|---|---|---|
| Time | [CONTRIBUTING.md](CONTRIBUTING.md) | branches, commits, PRs e revisão |
| Time e IA | [CLAUDE.md](CLAUDE.md) | regras do projeto para pessoas e agentes de IA |
| Todos | [🗺️ Atlas de fluxos](FLUXOS.md) | como cada parte se conecta: contexto, telas, árvore da transferência, containers, componentes, rastreabilidade tela → tabela, sessão e fluxo do time |
| API | [Contrato da API](backend/README_CONTRATO_API.md) | rotas, autenticação, JSON, regras e erros |
| Segurança | [Spring Security + OAuth2 + JWT](backend/README_SEGURANCA.md) | como o login, o token, os filtros, a autorização e o cookie do front funcionam, e por que é assim |
| Backend | [backend/README.md](backend/README.md) | como rodar, testes, banco, Flyway e problemas comuns |
| Backend | [Arquitetura e implementação](backend/README_IMPLEMENTACAO.md) | camadas e trechos reais |
| Backend | [Como testar o projeto](backend/README_TESTES_E2E.md) · [Coleção do Insomnia](backend/insomnia/rkfbank.insomnia.json) · [Visitantes](VISITANTES.md) | rodar (IDE, H2 ou MySQL), ver o banco, permissões, payloads e roteiros; aceite do MVP pela coleção |
| Frontend | [frontend/README.md](frontend/README.md) · [Logs](frontend/lib/logger/README.md) | como rodar e logs com máscara |
| Planejamento | [planing/](planing/README.md) · [Padrões de projeto](planing/padroes-de-projeto.md) | roadmap, decisões, checklist de features, débitos, recados e padrões |

<br/>

<div align="center">

<a href="planing/roadmap.md"><img src="assets/readme/chamada.svg" width="100%" alt="Mais que um banco, um parceiro para o seu amanhã. Ver o roadmap."/></a>

<sub>📍 RKF Banco Digital — idealizado e feito em Osasco, SP, por Kevin Richard, Renan Barros e Fellipe Santana.</sub>
<br/>
<a href="#readme">⬆️ voltar ao topo</a>

</div>
