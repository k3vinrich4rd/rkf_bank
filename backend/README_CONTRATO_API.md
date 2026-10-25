# RKF Bank — Contrato de API

Este arquivo é o **acordo entre backend e frontend**. Ele é a fonte da verdade: se o código e o contrato
discordarem, um dos dois está errado e precisa ser corrigido.

**Regras do contrato**

1. Endpoint novo ou alterado → muda **aqui primeiro**, no mesmo PR do código.
2. No mesmo PR, atualize os tipos em [`frontend/lib/api/types.ts`](../frontend/lib/api/types.ts).
3. Mudança que quebra o frontend (renomear campo, mudar status) → combine antes com o outro dev
   e registre em [planing/decisoes.md](../planing/decisoes.md).
4. Atualize a versão e o histórico no fim do arquivo.

| | |
|---|---|
| **Backend** | Kevin |
| **Frontend** | Renan |
| **Versão** | 3.1 (23/10/2026) — *refresh token* e logout, aceite dos termos no cadastro (`GET /termos`), CPF mascarado nas respostas, senha temporária depois da redefinição pelo ADMIN, `GET /contas/{contaId}` e conciliação; o frontend foi ajustado no mesmo PR (ver [histórico](#8-histórico-de-versões)) |

---

## Sumário

1. [Convenções](#1-convenções)
   - [Autenticação (JWT)](#autenticação-jwt)
2. [Endpoints](#2-endpoints)
3. [Clientes](#3-clientes)
4. [Contas e movimentações](#4-contas-e-movimentações)
5. [Regras de negócio](#5-regras-de-negócio)
6. [Erros](#6-erros)
7. [Próximos endpoints (propostas)](#7-próximos-endpoints-propostas)
8. [Histórico de versões](#8-histórico-de-versões)

---

## 1) Convenções

| Item | Regra | Exemplo |
|---|---|---|
| Base URL (backend direto) | `http://localhost:8080`, **sem** prefixo `/api` | `http://localhost:8080/clientes` |
| Formato | corpo sempre `application/json` | |
| IDs | UUID em texto | `"1a89861c-70a0-4b7c-947a-d16cdfe2dd08"` |
| Dinheiro | número JSON com até 2 casas decimais (`BigDecimal` no Java) | `150.00` |
| Data/hora | ISO-8601 em UTC | `"2026-10-12T11:19:53.038985Z"` |
| Enums | texto em maiúsculas | `"CORRENTE"`, `"DEPOSITO"` |
| Nomes de campo | `camelCase`, em português | `nomeCompleto`, `contaOrigemId` |
| Documentação viva | Swagger UI gerado do código (springdoc); o token se cola no botão **Authorize**. Em produção, desligue com `springdoc.api-docs.enabled=false` e `springdoc.swagger-ui.enabled=false` (ver [análise do MVP](../planing/analise-mvp.md)) | `http://localhost:8080/swagger-ui.html` · especificação em `/v3/api-docs` |

### Como o frontend chama a API

```text
Navegador ──► http://localhost:3000/api/clientes ──(proxy do Next)──► http://localhost:8080/clientes
```

O prefixo `/api` **só existe no frontend** (rewrite em `frontend/next.config.ts`). No Postman, Insomnia ou curl,
chame o backend direto, sem `/api`.

**CORS** (navegador chamando o backend direto, sem o proxy): vale para todas as rotas e só para as origens de
`CORS_ORIGENS` (padrão `http://localhost:3000`). O *preflight* (`OPTIONS`) é respondido sem token; origem fora da
lista recebe `403`. Headers aceitos: `Authorization`, `Content-Type`, `Idempotency-Key`, `X-Request-Id`. Headers que o
JavaScript consegue ler na resposta: `X-Request-Id`, `Idempotency-Replayed`, `Retry-After`.

### Autenticação (JWT)

```text
1. POST /clientes     {..., "senha": "...", "aceiteTermos": true} → 201 (cadastro é público)
2. POST /auth/login   {"cpf": "...", "senha": "..."} → 200 {"token": "eyJ...", "refreshToken": "...", ...}
3. Todas as outras chamadas:  Authorization: Bearer eyJ...
4. Antes de o token vencer:  POST /auth/refresh {"refreshToken": "..."} → 200 com um par novo
5. Sair:                     POST /auth/logout  {"refreshToken": "..."} → 204
```

- O token de acesso vale **15 minutos** (`expiraEmSegundos`; o servidor pode mudar com `JWT_EXPIRACAO`). Vencido ou inválido → `401`.
- O **token de renovação** (`refreshToken`) vale **7 dias** e troca o par sem pedir a senha ([3.3](#33-login)). Ele é
  de **uso único**: cada renovação devolve um par novo e o usado deixa de valer. Usar de novo um já trocado é sinal de
  roubo: a API derruba **todas** as sessões da pessoa (`401` `SESSAO_EXPIRADA`).
- O token de acesso é **stateless** (o backend não guarda sessão); o de renovação fica no banco só como hash SHA-256, e
  o logout o revoga; a troca de senha, a redefinição pelo ADMIN e o usuário desativado o invalidam (a renovação confere
  a versão da senha e se o usuário está ativo).
- Não guarde o token em lugar que outro site leia; não coloque o token na URL nem no log.
- O payload do token é só Base64 (não é segredo). Use os campos da **resposta do login**, não decodifique o token.
- **Perfis:** `CLIENTE` (todo cadastro) e `ADMIN` (criado pela configuração do servidor, não por rota).
- **Senha temporária:** depois que o ADMIN redefine a senha de alguém, o login devolve `deveTrocarSenha: true` e o
  token só abre `PATCH /usuarios/me/senha`; qualquer outra rota responde `403` `TROCA_DE_SENHA_OBRIGATORIA`.
- **Limite de tentativas:** 5 logins errados para o mesmo CPF em 15 min → `429` com header `Retry-After` (segundos).
  Enquanto durar, **nem a senha certa entra**. Mostre "aguarde alguns minutos".

### Idempotência (`Idempotency-Key`)

Depósito, saque e transferência **exigem** o header `Idempotency-Key`: um valor que o cliente gera **por operação**
(use `crypto.randomUUID()`; aceita 8 a 100 caracteres `A-Z a-z 0-9 - _`).

| Situação | Resposta |
|---|---|
| chave nova | movimenta e responde `204` |
| **mesma chave e mesmo corpo** (repetição: rede caiu, clique duplo, retry) | `204` com header `Idempotency-Replayed: true` e **nada é movimentado de novo** |
| mesma chave, corpo diferente (outro valor, conta ou descrição) | `422` com `codigo` `IDEMPOTENCIA_CHAVE_REUSADA` |
| duas chamadas iguais ao mesmo tempo | uma movimenta; a outra recebe `409` `IDEMPOTENCIA_EM_ANDAMENTO` e, repetida com a mesma chave, vira `204` com `Idempotency-Replayed` |
| sem o header ou fora do formato | `400` com `campos[0].campo = "Idempotency-Key"` |

Regra para o front: gere a chave ao enviar o formulário e **reuse a mesma** se o usuário tentar de novo depois de erro
de rede; troque quando ele editar um campo ou quando a operação der certo. A chave vale por usuário. Uma chamada
recusada (ex.: `422` de saldo) não grava a chave.

---

## 2) Endpoints

| Método | Rota | O que faz | Quem pode | Sucesso | Seção |
|---|---|---|---|---|---|
| `POST` | `/clientes` | cadastra cliente (com senha) | **público** | `201` + cliente | [3.1](#31-cadastrar-cliente) |
| `GET` | `/termos` | versão vigente dos termos e da política (o cadastro manda a que a pessoa aceitou) | **público** | `200` | [3.1](#31-cadastrar-cliente) |
| `POST` | `/auth/login` | login com CPF + senha | **público** | `200` + tokens | [3.3](#33-login) |
| `POST` | `/auth/refresh` | troca o token de renovação por um par novo | **público** (o token vai no corpo) | `200` + tokens | [3.3](#33-login) |
| `POST` | `/auth/logout` | revoga o token de renovação | **público** (o token vai no corpo) | `204` | [3.3](#33-login) |
| `GET` | `/clientes` | lista clientes | só `ADMIN` | `200` | [3.2](#32-listar-clientes) |
| `POST` | `/contas` | abre conta **no próprio cadastro** (cliente vem do token) | o próprio cliente | `201` + conta | [4.1](#41-abrir-conta) |
| `GET` | `/contas` | lista contas | `ADMIN`: todas · `CLIENTE`: só as dele | `200` | [4.2](#42-listar-contas) |
| `GET` | `/contas/{contaId}` | uma conta (saldo, tipo, ativa) | dono da conta ou `ADMIN` | `200` + conta | [4.2](#42-listar-contas) |
| `POST` | `/contas/{contaId}/depositos` | deposita | dono da conta | `204` | [4.3](#43-depositar-e-sacar) |
| `POST` | `/contas/{contaId}/saques` | saca | dono da conta | `204` | [4.3](#43-depositar-e-sacar) |
| `POST` | `/transferencias` | transfere entre contas | dono da conta de **origem** | `204` | [4.4](#44-transferir) |
| `GET` | `/contas/{contaId}/lancamentos` | extrato da conta | dono da conta | `200` | [4.5](#45-extrato) |
| `GET` | `/admin/usuarios` | lista usuários (CPF mascarado) | só `ADMIN` | `200` | [3.4](#34-gestão-de-usuários-admin) |
| `PATCH` | `/admin/usuarios/{usuarioId}/perfil` | promove a `ADMIN` ou rebaixa a `CLIENTE` | só `ADMIN` | `200` | [3.4](#34-gestão-de-usuários-admin) |
| `PATCH` | `/admin/usuarios/{usuarioId}/acesso` | desativa ou reativa o login | só `ADMIN` | `200` | [3.4](#34-gestão-de-usuários-admin) |
| `PATCH` | `/admin/usuarios/{usuarioId}/senha` | redefine a senha de quem esqueceu (vira senha temporária) | só `ADMIN` | `204` | [3.4](#34-gestão-de-usuários-admin) |
| `GET` | `/admin/conciliacao` | contas cujo saldo não bate com o extrato | só `ADMIN` | `200` | [3.7](#37-conciliação-admin) |
| `PATCH` | `/usuarios/me/senha` | troca a **própria** senha (com a atual) | qualquer usuário logado | `204` | [3.5](#35-trocar-a-própria-senha) |
| `GET` | `/clientes/me` | o titular vê os próprios dados (LGPD) | o próprio cliente | `200` + cliente | [3.6](#36-meus-dados-lgpd) |
| `PATCH` | `/clientes/me` | o titular corrige os próprios dados (LGPD) | o próprio cliente | `200` + cliente | [3.6](#36-meus-dados-lgpd) |
| `POST` | `/clientes/me/encerramento` | o titular encerra o cadastro (LGPD) | o próprio cliente | `200` + prazos | [3.6](#36-meus-dados-lgpd) |
| `GET` | `/actuator/health` | a API está no ar? (`{"status":"UP"}`, sem detalhes) | **público** | `200` | — |
| `GET` | `/swagger-ui.html` · `/v3/api-docs` | documentação viva (desligar em produção) | **público** | `200` | [1](#1-convenções) |

**Por que as rotas têm esse formato:** recurso no plural (cada `POST /contas/{id}/depositos` *cria* um depósito), a
transferência fora de `/contas/{id}` (envolve duas contas) e `me` no lugar de um id na URL (o dono vem do token, então
não existe "conta ou cadastro de outro" para pedir). Rotas que não estão nesta tabela respondem `404`.

> Fora as rotas públicas, **toda chamada sem token válido → `401`**. Token válido, mas recurso de outro
> cliente ou perfil sem acesso → `403`. O `ADMIN` lista, mas não movimenta conta de ninguém.
>
> O Spring Security 7 também publica sozinho `GET /.well-known/oauth-protected-resource` (público, padrão RFC 9728):
> devolve só `{"resource": "<url da API>", "bearer_methods_supported": ["header"], ...}`, e é para ele que aponta o
> `resource_metadata` do header `WWW-Authenticate` no `401`. O frontend não precisa usar.

---

## 3) Clientes

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
  "complemento": "Apto 45",
  "senha": "minha-senha-forte",
  "aceiteTermos": true,
  "termosVersao": "2026-10-23"
}
```

| Campo | Obrigatório | Regra |
|---|:---:|---|
| `aceiteTermos` | ✅ | precisa ser `true`: a pessoa aceitou os Termos de Uso e a Política de Privacidade (LGPD). `false` ou ausente → `400` |
| `termosVersao` | — | a versão que a tela mostrou (de `GET /termos`). Diferente da vigente → `422` `TERMOS_DESATUALIZADOS`: recarregue os termos e peça o aceite de novo. Sem o campo, vale a vigente |
| `senha` | ✅ | 8 a 72 caracteres (e no máximo 72 **bytes**: acento conta 2). **Nunca volta na resposta** |
| `nomeCompleto` | ✅ | até 120 caracteres |
| `cpf` | ✅ | CPF válido (dígitos verificadores) e único |
| `email` | ✅ | formato de e-mail válido e único, **sem diferenciar maiúsculas** (`Joao@x.com` = `joao@x.com`) |
| `telefone` | ✅ | não vazio (o backend **não** valida a quantidade de dígitos — o front envia 10 ou 11) |
| `cep` | ✅ | precisa existir no ViaCEP |
| `numero` | ✅ | texto (aceita "S/N") |
| `complemento` | — | até 80 caracteres |

- **Higienização:** CPF, telefone e CEP podem vir **com ou sem máscara** (`529.982.247-25`, `(11) 99999-8888`,
  `06020-010`): o backend remove tudo que não é dígito, valida o CPF pelos dígitos verificadores e consulta o ViaCEP
  com os 8 dígitos. No banco fica só o número.
- **E-mail:** o backend salva e devolve o e-mail **em minúsculas e sem espaços nas pontas** (`" Joao@X.com "` → `"joao@x.com"`).
  O front pode mostrar o valor da resposta como está; não precisa normalizar antes de enviar.
- **Endereço:** logradouro, bairro, cidade, UF e IBGE vêm do **ViaCEP**, não do usuário.
- **Tempo:** a consulta ao ViaCEP tem timeout de **3 segundos** — mostre um *loading* no botão.
- **Termos:** `GET /termos` (público) → `200` `{"versao": "2026-10-23"}`. O backend grava a versão e o momento do
  aceite no cadastro (`termos_versao`, `termos_aceitos_em`); a versão muda pela configuração `rkfbank.lgpd.termos-versao`.

**Resposta `201 Created`:**

```json
{
  "id": "1a89861c-70a0-4b7c-947a-d16cdfe2dd08",
  "nomeCompleto": "Joao Silva",
  "cpf": "***.982.247-**",
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
  },
  "termosVersao": "2026-10-23",
  "termosAceitosEm": "2026-10-23T14:02:11.483Z"
}
```

Na resposta, **telefone e CEP já vêm formatados** para exibição e o **CPF vem mascarado** (`***.982.247-**`, desde a
3.1: a resposta não precisa expor o documento inteiro). `complemento` pode vir `null`; cadastro anterior à 3.1 traz
`termosVersao` e `termosAceitosEm` nulos.
O cadastro **não devolve token**: depois do `201`, chame o [login](#33-login).

**Erros possíveis:** `400` (validação, inclusive `aceiteTermos`), `409` (CPF ou e-mail já cadastrado), `422` (CEP não
encontrado ou `TERMOS_DESATUALIZADOS`), `503` (ViaCEP indisponível). Detalhes na [seção 6](#6-erros).

### 3.2 Listar clientes

🔒 **Só `ADMIN`.** Com token de `CLIENTE` → `403`.

| Chamada | Retorno |
|---|---|
| `GET /clientes` | **array** com todos os clientes (mesmo formato da 3.1) |
| `GET /clientes?page=0&size=10` | **página** (formato abaixo) |

- `page` começa em `0`; `size` vai de `1` a `100`. Fora disso → `400` com o campo em `campos`.
- Basta `page` **ou** `size` na URL para a resposta vir paginada (sem eles: array). O que faltar usa o padrão (`page=0`, `size=10`).

Formato da página (campos que o frontend usa):

```json
{
  "content": [ /* clientes */ ],
  "totalElements": 2,
  "totalPages": 1,
  "number": 0,
  "size": 10,
  "first": true,
  "last": true
}
```

São exatamente esses sete campos, nem mais nem menos: a API devolve um DTO próprio (`PaginaResponse`), e não o
`Page` do Spring, então o formato não muda quando o Spring Data muda (antes da versão 2.4 vazavam `pageable`,
`sort`, `numberOfElements`...). Os nomes ficaram em inglês de propósito: são os que o frontend já lia.

### 3.3 Login

`POST /auth/login` (público)

```json
{
  "cpf": "52998224725",
  "senha": "minha-senha-forte"
}
```

| Campo | Obrigatório | Regra |
|---|:---:|---|
| `cpf` | ✅ | com ou sem máscara (o backend remove o que não é dígito) |
| `senha` | ✅ | até 72 caracteres |

**Resposta `200 OK`:**

```json
{
  "token": "eyJhbGciOiJIUzI1NiJ9.eyJpc3MiOiJya2ZiYW5rIiwic3ViIjoi...",
  "tipo": "Bearer",
  "expiraEmSegundos": 900,
  "clienteId": "1a89861c-70a0-4b7c-947a-d16cdfe2dd08",
  "perfil": "CLIENTE",
  "deveTrocarSenha": false,
  "refreshToken": "q3Jd0kX4...43 caracteres",
  "refreshExpiraEmSegundos": 604800
}
```

- Envie o token em toda chamada protegida: `Authorization: Bearer <token>`.
- `deveTrocarSenha: true` → leve a pessoa direto para a troca de senha ([3.5](#35-trocar-a-própria-senha)); as outras
  rotas respondem `403` `TROCA_DE_SENHA_OBRIGATORIA` até ela trocar.
- Guarde o `refreshToken` onde nenhum JavaScript da página leia (o front usa cookie `httpOnly` no servidor do Next).
- `clienteId` identifica o cadastro do titular (o front usa para separar as contas dele quando o perfil é `ADMIN`,
  que recebe todas em `GET /contas`). Para o `ADMIN` criado pelo `.env` vem `null`.
- Calcule quando o token vence com `expiraEmSegundos` (a partir do momento do login).

**Erros possíveis:** `400` (campo vazio), `401` `CREDENCIAIS_INVALIDAS`, `429` (5 erros seguidos para o mesmo CPF; ver [limite](#autenticação-jwt)). A mensagem é **a mesma** para CPF sem
cadastro e senha errada, de propósito: não revela quais CPFs têm conta.

**Renovar:** `POST /auth/refresh` com `{"refreshToken": "..."}` → `200` no **mesmo formato do login**, com um par novo
(o token de renovação usado deixa de valer). Renove pouco antes do de acesso vencer. `401` `SESSAO_EXPIRADA` quando o
token de renovação venceu, foi revogado (logout, troca ou redefinição de senha, usuário desativado) ou foi **reusado**
(nesse caso todas as sessões da pessoa caem): mande para o login.

**Sair:** `POST /auth/logout` com `{"refreshToken": "..."}` → `204` sempre, mesmo com token desconhecido ou já
revogado (sair nunca falha). O token de acesso continua válido até vencer (no máximo 15 min): apague-o no front.

### 3.4 Gestão de usuários (ADMIN)

🔒 Só `ADMIN` (`CLIENTE` → `403`). Sem tela no frontend: uso pelo Postman/curl.

`GET /admin/usuarios` → `200`, admins primeiro:

```json
[{"id":"9744fa24-8034-4c38-b20f-ce3a0cfe40c7","cpf":"***.792.468-**","nome":null,"perfil":"ADMIN","ativo":true,"clienteId":null},
 {"id":"f3155e34-57f9-410e-8900-a1139202fe2f","cpf":"***.456.789-**","nome":"Fellipe Santana (Teste Origem)","perfil":"CLIENTE","ativo":true,"clienteId":"5eed0000-0000-4000-8000-000000000012"}]
```

`PATCH /admin/usuarios/{usuarioId}/perfil` com `{"perfil":"ADMIN"}` ou `{"perfil":"CLIENTE"}` · `PATCH /admin/usuarios/{usuarioId}/acesso`
com `{"ativo":false}` ou `{"ativo":true}` → `200` com o usuário no formato acima.

- `usuarioId` é o `id` da lista (de `tb_usuario`), **não** o `clienteId`.
- A mudança vale na **próxima requisição**: o token emitido antes dela responde `401` `ACESSO_ALTERADO`.
  Desativado não faz login (`401` `CREDENCIAIS_INVALIDAS`, a mesma resposta de senha errada).
- "Remover" = desativar: usuário não é apagado (o histórico aponta para ele).
- `PATCH /admin/usuarios/{usuarioId}/senha` com `{"novaSenha":"..."}` (8 a 72) → `204`. Os tokens da pessoa caem na hora
  (inclusive os de renovação) e a senha nova é **temporária**: no próximo login vem `deveTrocarSenha: true` e só a troca
  de senha funciona até ela escolher outra. O ADMIN
  não redefine a própria senha por aqui (`422` `PROPRIA_SENHA_PELA_GESTAO`: ele usa `PATCH /usuarios/me/senha`).

**Erros possíveis:** `400` (perfil fora de `CLIENTE`/`ADMIN`, `ativo` ausente, id não é UUID), `401`, `403` (não é ADMIN),
`404` `USUARIO_NAO_ENCONTRADO`, `422` `PROPRIO_ACESSO` (ninguém altera o próprio perfil ou acesso) ou
`PERFIL_CLIENTE_SEM_CADASTRO` (o ADMIN do `.env` não tem cadastro para virar CLIENTE; desative o acesso).

### 3.5 Trocar a própria senha

`PATCH /usuarios/me/senha` (🔒 qualquer usuário logado)

```json
{ "senhaAtual": "senha-de-hoje", "novaSenha": "senha-nova-123" }
```

| Campo | Obrigatório | Regra |
|---|:---:|---|
| `senhaAtual` | ✅ | a senha de agora (prova que é o dono, e não alguém com um token roubado) |
| `novaSenha` | ✅ | 8 a 72 caracteres (até 72 bytes), diferente da atual |

**Resposta `204 No Content`.** Todo token emitido antes da troca, **inclusive o usado nesta chamada**, passa a responder
`401` `ACESSO_ALTERADO`: o front deve mandar para o login.
Por dentro: o token leva a claim `versaoCredencial`; a troca sobe a versão no banco e o token antigo deixa de bater.

**Erros possíveis:** `400` (validação; `novaSenha` acima de 72 bytes), `401`, `422` `SENHA_ATUAL_INCORRETA` (marque o
campo da senha atual) ou `SENHA_REPETIDA` (marque o da nova), `429` (senha atual errada conta no mesmo limite do login).

### 3.6 Meus dados (LGPD)

Direitos do titular (LGPD, art. 18): **ver**, **corrigir** e **pedir o encerramento** do próprio cadastro. `me` é
sempre o cliente do token: não existe `/clientes/{id}` de propósito, para nenhum id na URL abrir o cadastro de outra
pessoa. `ADMIN` criado pelo `.env` não tem cadastro de cliente → `422` `SEM_CADASTRO_CLIENTE`.

**`GET /clientes/me`** → `200` no mesmo formato da [3.1](#31-cadastrar-cliente) (o que o banco guarda dele, sem senha).

**`PATCH /clientes/me`** → `200` com o cadastro atualizado. Só muda o que vier no corpo:

```json
{ "email": "joao.novo@exemplo.com", "telefone": "11912345678", "cep": "01001000", "numero": "200", "complemento": "" }
```

| Campo | Regra |
|---|---|
| `nomeCompleto` | 1 a 120 caracteres |
| `email` | formato de e-mail; salvo em minúsculas; não pode ser de outro cliente (`409`) |
| `telefone` | DDD + 8 ou 9 dígitos (pontuação é ignorada) |
| `cep` | 8 dígitos; o endereço é buscado de novo no ViaCEP (`422`/`503` como no cadastro); exige `numero` |
| `numero` | 1 a 20 caracteres |
| `complemento` | até 80; `""` apaga |
| `cpf` | **não muda**: é o login e a chave do cadastro (correção pelo atendimento) |

Corpo vazio (`{}`) → `400` com `campos` = `corpo`. O log registra **quais** campos mudaram, nunca os valores.

**`POST /clientes/me/encerramento`** com `{ "senha": "..." }` → `200`:

```json
{
  "encerradoEm": "2026-10-22T18:02:11.483Z",
  "anonimizacaoPrevistaPara": "2031-10-22",
  "motivoDaRetencao": "Lei 9.613/1998, art. 10: cadastro e operacoes guardados pelo prazo legal depois do fim do relacionamento; em seguida, os dados pessoais sao anonimizados"
}
```

| Regra | Erro |
|---|---|
| a senha confirma que é o titular | `422` `SENHA_INCORRETA` |
| todas as contas com saldo zero (dinheiro não some: transfira antes) | `422` `SALDO_NAO_ZERADO` |

Na hora: todas as contas viram `ativa = false` (não recebem nem movimentam), o login é desativado e **todo token
dele passa a dar `401`**; o `ADMIN` não consegue reativar (`422` `CADASTRO_ENCERRADO`). O cadastro **não é apagado agora**: a lei de prevenção à lavagem de dinheiro manda guardar por 5 anos
(`LGPD_RETENCAO`, padrão `5y`). Uma rotina diária (03:00, horário de São Paulo) troca nome, CPF, e-mail, telefone,
endereço e login por valores sem identificação quando o prazo vence; contas, lançamentos e cidade/UF ficam.

**Erros possíveis:** `400` (validação), `401`, `409` `EMAIL_JA_CADASTRADO`, `422` (acima), `503` (ViaCEP).

### 3.7 Conciliação (ADMIN)

🔒 Só `ADMIN`. `GET /admin/conciliacao` → `200` com as contas cujo saldo **não bate** com o extrato (entradas − saídas):

```json
[{ "contaId": "407d727e-4e43-4c93-b01c-331cbaba42fb", "numeroConta": "12345678", "saldo": 150.00, "saldoPeloExtrato": 100.00 }]
```

Lista vazia é o normal. Qualquer linha é bug ou alteração manual no banco: investigar antes de mexer no saldo.

---

## 4) Contas e movimentações

🔒 Todas as rotas desta seção exigem `Authorization: Bearer <token>`.

### 4.1 Abrir conta

`POST /contas` — a conta é aberta **no cadastro do usuário logado** (o cliente vem do token).

```json
{
  "tipoConta": "CORRENTE"
}
```

| Campo | Obrigatório | Regra |
|---|:---:|---|
| `tipoConta` | ✅ | `CORRENTE` ou `POUPANCA` |

Agência e número **não vão no pedido**: a agência é sempre `0001` e o número tem 8 dígitos gerados pelo banco
(7 aleatórios + dígito verificador módulo 11). Número de conta é identidade do banco, não escolha de quem abre.

**Resposta `201 Created`:**

```json
{
  "id": "407d727e-4e43-4c93-b01c-331cbaba42fb",
  "clienteId": "1a89861c-70a0-4b7c-947a-d16cdfe2dd08",
  "agencia": "0001",
  "numeroConta": "12345678",
  "tipoConta": "CORRENTE",
  "saldo": 0.00,
  "ativa": true
}
```

- Toda conta nasce com **saldo zero** e **ativa**.
- Um cliente pode ter **mais de uma conta**.

**Erros possíveis:** `400` (`tipoConta` ausente ou fora de `CORRENTE`/`POUPANCA`), `401` (sem token),
`409` `NUMERO_CONTA_INDISPONIVEL` (raro: 5 sorteios seguidos ocupados; tente de novo), `422` `SEM_CADASTRO_CLIENTE`
(o ADMIN do `.env`).

### 4.2 Listar contas

Igual à listagem de clientes: `GET /contas` (array) ou `GET /contas?page=0&size=10` (página).
Cada item tem o formato da resposta da 4.1.

- Token de `CLIENTE` → **só as contas dele** (é o jeito de achar as contas do usuário logado).
- Token de `ADMIN` → todas as contas.

**Uma conta:** `GET /contas/{contaId}` → `200` no formato da 4.1. Serve para atualizar o saldo depois de uma
movimentação sem buscar a lista inteira. **Erros:** `400` (id não é UUID), `401`, `403` `CONTA_DE_OUTRO_CLIENTE`,
`404` `CONTA_NAO_ENCONTRADA`. O `ADMIN` vê qualquer conta.

### 4.3 Depositar e sacar

`POST /contas/{contaId}/depositos` · `POST /contas/{contaId}/saques`

```json
{
  "valor": 150.00,
  "descricao": "Deposito inicial"
}
```

| Campo | Obrigatório | Regra |
|---|:---:|---|
| `valor` | ✅ | mínimo `0.01`, no máximo 2 casas decimais |
| `descricao` | — | até 140 caracteres |
| header `Idempotency-Key` | ✅ | uma por operação ([idempotência](#idempotência-idempotency-key)) |

**Resposta `204 No Content`** (sem corpo; repetição com a mesma chave traz `Idempotency-Replayed: true`). Para mostrar o
novo saldo, busque a conta de novo.

**Erros possíveis:** `400` (validação), `401` (sem token), `403` (conta de outro cliente), `404` (conta não encontrada),
`409` (conta alterada ao mesmo tempo ou mesma chave em uso), `422` (conta inativa, no saque saldo insuficiente, ou chave
reaproveitada com outro corpo).
A ordem de checagem é `404` → `403` → `422`: conta de outro cliente dá `403` antes de qualquer regra de saldo.

### 4.4 Transferir

`POST /transferencias`

```json
{
  "contaOrigemId": "407d727e-4e43-4c93-b01c-331cbaba42fb",
  "contaDestinoId": "566dd28f-b08f-41e4-ac55-cc69ecfea4cd",
  "valor": 50.00,
  "descricao": "Transferencia aluguel"
}
```

| Campo | Obrigatório | Regra |
|---|:---:|---|
| `contaOrigemId` | ✅ | UUID de conta existente, ativa e **do usuário logado** |
| `contaDestinoId` | ✅ | UUID de conta existente e ativa, diferente da origem (pode ser de outro cliente) |
| `valor` | ✅ | mínimo `0.01`, no máximo 2 casas decimais |
| `descricao` | — | até 140 caracteres |
| header `Idempotency-Key` | ✅ | uma por operação ([idempotência](#idempotência-idempotency-key)) |

**Resposta `204 No Content`** (com `Idempotency-Replayed: true` quando for repetição). Débito e crédito acontecem **na mesma transação**: ou os dois acontecem, ou nenhum.

**Erros possíveis:** `400`, `401`, `403` (origem de outro cliente), `404`, `409`, `422` (mesma conta, conta inativa,
saldo insuficiente).

### 4.5 Extrato

`GET /contas/{contaId}/lancamentos`

**Resposta `200 OK`**, do mais recente para o mais antigo:

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

| `tipoLancamento` | `contaOrigemId` | `contaDestinoId` |
|---|---|---|
| `DEPOSITO` | `null` | conta que recebeu |
| `SAQUE` | conta que sacou | `null` |
| `TRANSFERENCIA` | quem enviou | quem recebeu |

- `valor` é **sempre positivo**. Para saber se é **entrada** ou **saída** na tela da conta X:
  se `contaDestinoId === X` → entrada (verde); senão → saída (vermelho).
- `descricao` pode vir `null`.
- Conta sem movimentação → `[]`.
- `tipoLancamento` só tem esses três valores (desde a 3.1 a coluna aceita só eles: V13).

**Erros possíveis:** `400` (`contaId` não é UUID), `401` (sem token), `403` (conta de outro cliente),
`404` (conta não encontrada), `422` (conta inativa).

---

## 5) Regras de negócio

| # | Regra | Erro |
|---|---|---|
| R1 | CPF não se repete | `409` |
| R2 | E-mail não se repete, sem diferenciar maiúsculas (é salvo em minúsculas) | `409` |
| R3 | CEP precisa existir no ViaCEP | `422` |
| R4 | Toda conta pertence a um cliente existente | `404` |
| R5 | Número da conta não se repete | `409` |
| R6 | Só conta **ativa** movimenta e tem extrato | `422` |
| R7 | Valor sempre maior que zero | `400` |
| R8 | Saque e transferência precisam de saldo suficiente | `422` |
| R9 | Não dá para transferir para a mesma conta | `422` |
| R10 | Duas operações simultâneas na mesma conta não se sobrescrevem (lock otimista) | `409` |
| R11 | Fora `POST /clientes`, `POST /auth/login`, `/auth/refresh`, `/auth/logout`, `GET /termos`, o Swagger e `GET /actuator/health`, toda rota exige token válido | `401` |
| R12 | Só o dono movimenta, vê extrato e abre conta (na transferência, o dono da **origem**) | `403` |
| R13 | `GET /clientes` é só do `ADMIN`; em `GET /contas` o `CLIENTE` vê só as dele | `403` |
| R14 | Senha guardada só como hash BCrypt; nunca volta em resposta nem aparece em log | — |
| R15 | Cada movimentação acontece **uma vez** por `Idempotency-Key`; repetir a chave não movimenta de novo | `400` sem chave · `422` chave com outro corpo |
| R16 | Agência e número da conta são sempre definidos pelo banco (`0001` + 8 dígitos com DV) | `409` se não houver número livre |
| R17 | 5 logins errados para o mesmo CPF em 15 min bloqueiam aquele CPF até a janela acabar | `429` |
| R18 | Conta não é apagada: cliente com conta não pode ser excluído; conta é encerrada (`ativa = false`) | — |
| R19 | Só `ADMIN` promove, rebaixa, desativa e reativa usuários; ninguém altera o próprio perfil ou acesso | `403` / `422` |
| R20 | Token de usuário desativado ou com perfil alterado depois do login deixa de valer na hora | `401` |
| R21 | Usuário desativado não faz login (mesma resposta de senha errada) | `401` |
| R22 | Trocar a própria senha exige a atual; erro conta no limite de tentativas | `422` / `429` |
| R23 | Troca ou redefinição de senha derruba na hora todos os tokens emitidos antes | `401` |
| R24 | O titular vê e corrige só o **próprio** cadastro (`/clientes/me`, do token); CPF não muda | `422` sem cadastro · `409` e-mail |
| R25 | Encerrar o cadastro exige a senha e saldo zero em todas as contas | `422` |
| R26 | Cadastro encerrado: contas inativas, login desativado na hora e sem reativação pelo `ADMIN` | `401` / `422` |
| R27 | Cadastro encerrado é anonimizado depois do prazo de retenção (5 anos); lançamentos ficam, com a descrição apagada | — |
| R28 | Cadastro exige o aceite dos termos vigentes; versão e momento do aceite ficam gravados | `400` / `422` |
| R29 | Token de renovação é de uso único; reusar um já trocado derruba todas as sessões da pessoa | `401` |
| R30 | Senha redefinida pelo ADMIN é temporária: só a troca de senha funciona até a pessoa escolher outra | `403` |

---

## 6) Erros

### Formato padrão

Todo erro **tratado** volta assim:

```json
{
  "timestamp": "2026-10-23T11:19:54.090042Z",
  "status": 400,
  "error": "Bad Request",
  "codigo": "CAMPOS_INVALIDOS",
  "message": "Requisição inválida",
  "path": "/clientes",
  "campos": [
    { "campo": "cpf", "mensagem": "cpf inválido" }
  ]
}
```

| Campo | Para quê |
|---|---|
| `status` | decida primeiro por ele |
| `codigo` | **identificador estável da causa** (desde a 3.0). Use quando o mesmo status tem causas diferentes, ex.: `409` `IDEMPOTENCIA_EM_ANDAMENTO` × `CONCORRENCIA`, `422` `SENHA_ATUAL_INCORRETA` × `SENHA_REPETIDA`. Código novo pode surgir; um existente não muda de nome |
| `message` | texto em português para **exibir**. Pode mudar (um acento, uma vírgula) sem aviso: **nunca compare o texto** |
| `campos` | erros por campo (nome igual ao do JSON, para o front marcar o input). Vem `[]` fora da validação |

No backend, código e mensagem moram juntos no enum
[`CodigoErroEnum`](src/main/java/com/br/rkfbank/exceptions/CodigoErroEnum.java): o service lança a exceção com o
código (`new RegraNegocioException(CodigoErroEnum.SALDO_INSUFICIENTE)`) e o
[`TratadorGlobalDeErros`](src/main/java/com/br/rkfbank/controllers/handlers/TratadorGlobalDeErros.java) só escolhe o
status. No front, o tipo `CodigoErro` de [`lib/api/types.ts`](../frontend/lib/api/types.ts) espelha o enum e o
`ApiError` traz `codigo`.

### Tabela de códigos

| Status | `codigo` | Quando | `message` |
|---|---|---|---|
| `400` | `CAMPOS_INVALIDOS` | campo inválido no corpo (Bean Validation), `page`/`size` fora do limite, senha acima de 72 bytes, `Idempotency-Key` ausente ou fora do formato; o detalhe de cada campo vem em `campos` | Requisição inválida |
| `400` | `JSON_INVALIDO` | JSON mal formado, tipo errado ou enum desconhecido | JSON inválido ou tipo de campo incompatível |
| `400` | `PARAMETRO_INVALIDO` | id da URL não é UUID (o parâmetro vem em `campos`) | Parâmetro de URL inválido |
| `401` | `AUTENTICACAO_NECESSARIA` | sem header `Authorization` | Autenticação necessária: envie o token no header Authorization (Bearer) |
| `401` | `TOKEN_INVALIDO` | token mal formado, de outra chave ou vencido | Token inválido ou expirado; faça login novamente |
| `401` | `ACESSO_ALTERADO` | token válido, mas o usuário foi desativado, mudou de perfil, trocou a senha ou encerrou o cadastro depois do login | Seu acesso foi alterado ou desativado; faça login novamente |
| `401` | `CREDENCIAIS_INVALIDAS` | login com CPF ou senha errados (mesma resposta para CPF sem cadastro) | CPF ou senha inválidos |
| `401` | `SESSAO_EXPIRADA` | `POST /auth/refresh` com token de renovação vencido, revogado ou reusado | Sua sessão expirou; entre de novo com CPF e senha |
| `403` | `PERFIL_SEM_PERMISSAO` | perfil sem acesso à rota (`CLIENTE` em `GET /clientes` ou `/admin/**`) | Acesso negado para o seu perfil |
| `403` | `CONTA_DE_OUTRO_CLIENTE` | ver, movimentar ou ver o extrato da conta de outro cliente | Conta não pertence ao usuário logado |
| `403` | `TROCA_DE_SENHA_OBRIGATORIA` | senha temporária (redefinida pelo ADMIN) chamando qualquer rota além da troca de senha | Sua senha foi redefinida: troque-a antes de continuar |
| `404` | `CLIENTE_NAO_ENCONTRADO` | cliente não existe | Cliente não encontrado |
| `404` | `CONTA_NAO_ENCONTRADA` | conta não existe | Conta não encontrada |
| `404` | `USUARIO_NAO_ENCONTRADO` | usuário não existe (gestão do ADMIN) | Usuário não encontrado |
| `409` | `CPF_JA_CADASTRADO` | cadastro com CPF repetido | CPF já cadastrado |
| `409` | `EMAIL_JA_CADASTRADO` | cadastro ou correção com e-mail de outro cliente | E-mail já cadastrado |
| `409` | `CADASTRO_DUPLICADO` | dois cadastros iguais ao mesmo tempo (a constraint do banco pegou) | CPF ou e-mail já cadastrado |
| `409` | `NUMERO_CONTA_INDISPONIVEL` | o banco não achou número livre em 5 sorteios (raro; repetir resolve) | Não foi possível gerar um número de conta livre; tente novamente |
| `409` | `CONCORRENCIA` | duas operações mexeram na mesma conta ao mesmo tempo (lock otimista); nada foi gravado | A conta foi alterada por outra operação; tente novamente |
| `409` | `IDEMPOTENCIA_EM_ANDAMENTO` | a mesma `Idempotency-Key` em duas chamadas simultâneas | Esta operação já está sendo processada; repita com a mesma Idempotency-Key para ver o resultado |
| `422` | `CEP_NAO_ENCONTRADO` | CEP que o ViaCEP não conhece | CEP não encontrado |
| `422` | `TERMOS_DESATUALIZADOS` | cadastro com `termosVersao` diferente da vigente | Os termos de uso mudaram: recarregue a página e aceite a versão atual |
| `422` | `CONTA_INATIVA` | movimentar ou ver o extrato de conta inativa | Conta inativa |
| `422` | `VALOR_INVALIDO` | valor zero ou negativo que passou da validação | Valor deve ser maior que zero |
| `422` | `SALDO_INSUFICIENTE` | saque ou transferência maior que o saldo | Saldo insuficiente |
| `422` | `MESMA_CONTA` | transferência com origem igual ao destino | Conta de origem e destino não podem ser iguais |
| `422` | `SENHA_ATUAL_INCORRETA` | troca de senha com a senha atual errada | Senha atual incorreta |
| `422` | `SENHA_REPETIDA` | troca de senha com a nova igual à atual | A nova senha precisa ser diferente da atual |
| `422` | `SENHA_INCORRETA` | encerramento do cadastro com a senha errada | Senha incorreta |
| `422` | `SEM_CADASTRO_CLIENTE` | `/clientes/me` ou `POST /contas` com usuário sem cadastro de cliente (ADMIN do `.env`) | Seu usuário não tem cadastro de cliente |
| `422` | `SALDO_NAO_ZERADO` | encerramento com saldo em alguma conta | Transfira o saldo das suas contas antes de encerrar o cadastro |
| `422` | `PERFIL_CLIENTE_SEM_CADASTRO` | ADMIN tentando rebaixar a `CLIENTE` quem não tem cadastro | Usuário sem cadastro de cliente não pode virar CLIENTE; desative o acesso |
| `422` | `CADASTRO_ENCERRADO` | ADMIN tentando reativar quem encerrou o cadastro | O titular encerrou o cadastro; o acesso não pode ser reativado |
| `422` | `PROPRIA_SENHA_PELA_GESTAO` | ADMIN redefinindo a própria senha pela gestão | Para trocar a sua própria senha use PATCH /usuarios/me/senha |
| `422` | `PROPRIO_ACESSO` | ADMIN alterando o próprio perfil ou acesso | Você não pode alterar o seu próprio perfil ou acesso; peça a outro ADMIN |
| `422` | `IDEMPOTENCIA_CHAVE_REUSADA` | `Idempotency-Key` reaproveitada com outro corpo | Idempotency-Key já usada em outra requisição; gere uma chave nova para esta operação |
| `429` | `MUITAS_TENTATIVAS` | 5 logins errados seguidos para o mesmo CPF (+ header `Retry-After`) | Muitas tentativas de login; aguarde alguns minutos e tente de novo |
| `503` | `SERVICO_INDISPONIVEL` | ViaCEP fora do ar ou lento (timeout de 3 s) | ViaCEP indisponível; tente novamente em instantes |

> `401` e `403` também vêm no **formato padrão**; o `401` traz ainda o header `WWW-Authenticate: Bearer`.
> Rota inexistente sem token responde `401` (o filtro de segurança barra antes de saber que a rota não existe);
> com token, `404`.
>
> Erros **não tratados** (bug → `500`, rota inexistente → `404`) vêm no formato padrão do Spring, **sem** `codigo`
> nem `campos`. O `apiRequest` do front já lida com isso (vira `ApiError` com o status HTTP e `codigo: null`).

---

## 7) Próximos endpoints (propostas)

Ainda **não implementados**. Quando um deles for feito, ele sai daqui e entra nas seções 3/4.

Nenhuma proposta aberta para o MVP: `GET /contas/{contaId}` e `POST /auth/refresh` entraram na 3.1, e
`GET /clientes/{clienteId}/contas` foi descartada (o cliente vem do token: `GET /contas` já lista as dele, e um id de
cliente na URL reabriria a porta para pedir as contas de outra pessoa). As funcionalidades futuras (Pix, boleto,
comprovante) estão em [planing/debitos-tecnicos.md](../planing/debitos-tecnicos.md#funcionalidades-futuras-).

---

## 8) Histórico de versões

| Versão | Data | Mudança |
|---|---|---|
| 3.1 | 23/10/2026 | `POST /auth/refresh` e `POST /auth/logout` (token de renovação de 7 dias, uso único, reuso derruba todas as sessões; V15); login devolve `refreshToken`, `refreshExpiraEmSegundos` e `deveTrocarSenha`. Aceite dos termos: `GET /termos`, `aceiteTermos` obrigatório e `termosVersao` no cadastro (V10), `termosVersao`/`termosAceitosEm` na resposta. **CPF mascarado** nas respostas de cliente. Senha redefinida pelo ADMIN vira temporária (`403` `TROCA_DE_SENHA_OBRIGATORIA`; V14). `GET /contas/{contaId}` e `GET /admin/conciliacao`. Códigos `SESSAO_EXPIRADA`, `TROCA_DE_SENHA_OBRIGATORIA` e `TERMOS_DESATUALIZADOS`. Swagger em `/swagger-ui.html`. Regras R28–R30. Quebra o cadastro de quem não manda `aceiteTermos` (frontend ajustado no mesmo PR) |
| 3.0 | 23/10/2026 | **Quebra o frontend (ajustado no mesmo PR).** Saem as rotas do contrato 2.x (`POST /contas/clientes/{id}`, `/contas/{id}/deposito` e `/saque`, `POST /contas/transferencia`, `PATCH /auth/senha`) e o `?paginado`: respondem `404`/são ignorados. `tipoConta` no pedido e na resposta (sai `tipoContaEnum`); `agencia` e `numeroConta` saem do pedido. Todo erro ganha `codigo` (`CodigoErroEnum`) e as mensagens passam a ter acento |
| 2.6 | 22/10/2026 | Rotas no padrão de mercado: `POST /contas` (cliente do token), `/contas/{id}/depositos` e `/saques`, `POST /transferencias`, `PATCH /usuarios/me/senha`; paginação só com `page`/`size`; pedido de abertura aceita `tipoConta`. As antigas seguem funcionando com `Deprecation` + `Link` até a 3.0. Não quebra o frontend |
| 2.5 | 22/10/2026 | Direitos do titular (LGPD): `GET /clientes/me`, `PATCH /clientes/me` e `POST /clientes/me/encerramento`; `tb_cliente.encerrado_em` e `anonimizado_em` (V9); rotina diária de anonimização depois de 5 anos (`LGPD_RETENCAO`). Regras R24–R27. Não quebra o frontend |
| 2.4 | 22/10/2026 | Página (`?paginado=true`) passa a ser um DTO próprio com os mesmos campos de antes (`content`, `totalElements`, `totalPages`, `number`, `size`, `first`, `last`), sem os campos internos do `Page` do Spring. CORS valendo para todas as rotas, com origens em `CORS_ORIGENS`. Não quebra o frontend |
| 2.3 | 20/10/2026 | `PATCH /usuarios/me/senha` (troca da própria senha) e `PATCH /admin/usuarios/{id}/senha` (redefinição pelo ADMIN); claim `versaoCredencial` no token e `tb_usuario.versao_credencial` (V8). Regras R22–R23. Não quebra o frontend |
| 2.2 | 20/10/2026 | Gestão de usuários: `GET /admin/usuarios`, `PATCH .../perfil` e `.../acesso` (só `ADMIN`); `tb_usuario.ativo` (V7); token de quem foi desativado ou mudou de perfil → `401` com mensagem própria. Regras R19–R21. Não quebra o frontend |
| 2.1 | 20/10/2026 | **Quebra o frontend (ajustado no mesmo PR).** `Idempotency-Key` obrigatório em depósito, saque e transferência (`400` sem ele, `422` reaproveitada, header `Idempotency-Replayed` na repetição); `agencia` e `numeroConta` opcionais na abertura (o banco gera); `429` no login após 5 erros; `GET /actuator/health` público |
| 2.0 | 16/10/2026 | **Quebra o frontend.** Login com JWT: `senha` obrigatória no cadastro, `POST /auth/login` novo, `Authorization: Bearer` em todas as outras rotas (`401`), regra de dono (`403`), `GET /clientes` só `ADMIN`, `GET /contas` do `CLIENTE` traz só as dele. Regras R11–R14 |
| 1.3 | 15/10/2026 | e-mail salvo e devolvido em minúsculas; R2 sem diferenciar maiúsculas. Sem mudança de campo ou status (não quebra o frontend) |
| 1.2 | 13/10/2026 | tabela de mensagens de erro reais, regras numeradas, erros por endpoint, propostas priorizadas |
| 1.1 | 12/10/2026 | stack, convenções e endpoints revisados |
| — | 11/10/2026 | rotas passam a seguir o backend: sem `/api`, `POST /contas/clientes/{id}` e `POST /transferencias` |
| — | 05/10/2026 | contrato alinhado com o guia de implementação e o fluxo E2E |
| 1.0 | 02/10/2026 | primeira versão do contrato |
