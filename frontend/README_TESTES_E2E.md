# RKF Bank — Como testar as telas, do zero

| | |
|---|---|
| 🎯 **Para que serve** | testar **as telas do site** na sua máquina, com as contas de visitante |
| 👥 **Área responsável** | 🟩 **Frontend** (o site) · dono: Renan · público |
| ⏰ **Quando usar** | quer ver o banco pelo navegador, como um cliente |
| 🧰 **Como** | navegador, com a API no perfil `visitante` e o site rodando |
| 🚫 **Não cobre** | as regras da API sozinha → [backend/README_TESTES_E2E.md](../backend/README_TESTES_E2E.md) · o essencial em 15 min → [VISITANTES.md](../VISITANTES.md) |


> **Para quem é:** qualquer pessoa que queira usar o site do RKF Bank na própria máquina e conferir cada tela, sem
> ajuda: visitantes, recrutadores, quem está chegando no projeto. Usa só as **contas de visitante** (senha pública).
> Cada caso tem o que **fazer**, o que o navegador **manda por baixo** (pedido e resposta, como aparecem no DevTools →
> **Network**) e o que tem que **aparecer** na tela.
> A API sozinha, sem o site: [guia de testes do backend](../backend/README_TESTES_E2E.md) (a Parte 0 dele explica,
> do zero, o que é um pedido, um status `200`/`403`/`422` e um token).
>
> 🔒 Os casos de ADMIN não aparecem aqui: o perfil de visitante não tem administrador, de propósito (a senha é
> pública). A equipe testa o ADMIN no roteiro privado (`.equipe/testes/FRONTEND_E2E.md`).

Conferido em 23/10/2026 com o contrato **3.1** da API. O que muda ao repetir: ids, o número da conta (gerado pelo
banco), datas e horários.

---

## ⚡ Em 3 passos

| # | Onde | Como | Pronto quando |
|:-:|---|---|---|
| 1 | **IntelliJ** (raiz do repositório aberta, JDK 25) | escolha **API · Visitante (H2)** ao lado do ▶️ e clique ▶️ · ou, no terminal: `./mvnw spring-boot:run -Dspring-boot.run.profiles=visitante` | `Started RkfbankApplication` (API em `http://localhost:8080`) |
| 2 | terminal na pasta `frontend` | `corepack enable && pnpm install && pnpm dev` | `Ready` (site em `http://localhost:3000`) |
| 3 | navegador | abra `http://localhost:3000` | a landing com o cartão RKF em 3D |

Pré-requisitos: **JDK 25** e **Node 20+**. Sem Docker e sem `.env`: o perfil `visitante` usa um banco H2 em memória
que some quando a API para. Primeira vez no IntelliJ? [Guia do backend, Parte 1](../backend/README_TESTES_E2E.md#parte-1--prepare-o-ambiente-pelo-intellij)
(inclui como criar a configuração com as **VM options** na mão).

**Pela IDE, em vez do terminal (passo 2):**

- **IntelliJ / WebStorm:** abra `frontend/package.json` e clique no ▶️ ao lado do script `dev` (ou **Run → Edit
  Configurations… → + → npm**, *Command* `run`, *Scripts* `dev`, *package.json* `frontend/package.json`). Rodando a API
  e o site juntos, cada um aparece numa aba da janela **Run**.
- **VS Code:** abra a pasta `frontend`, painel **NPM Scripts** (no Explorer) → ▶️ em `dev`.
- A API está em outra porta? Crie `frontend/.env.local` a partir do exemplo (`cp .env.example .env.local`) e troque o
  `API_URL`. Sem esse arquivo, o site usa `http://localhost:8080`.

**Testes automáticos do front** (não precisam da API no ar), na pasta `frontend`:

| Comando | O que confere |
|---|---|
| `pnpm test` | testes do Vitest: sessão, renovação do token no `proxy.ts`, força da senha, seletor de conta |
| `pnpm lint` e `pnpm typecheck` | padrão de código e tipos do TypeScript (o contrato da API em `lib/api/types.ts`) |
| `pnpm build` | o build de produção do Next |

Ou tudo de uma vez, da raiz: `scripts/validar.sh frontend`. Na IDE, os testes do Vitest também rodam pelo ▶️ ao lado de
cada `describe`/`it` (IntelliJ/WebStorm já reconhecem; no VS Code, com a extensão oficial do Vitest).

| Conta pronta | CPF (login) | Senha | Saldo inicial | ID da conta (para receber transferências) |
|---|---|---|---|---|
| 🟠 Ana Visitante (Origem) | `246.813.579-28` | `Visitante@2026` | R$ 1.000,00 | `a0000000-0000-4000-8000-000000000114` |
| 🔵 Bruno Visitante (Destino) | `975.318.642-82` | `Visitante@2026` | R$ 500,00 | `a0000000-0000-4000-8000-000000000124` |

### Como o site fala com a API (para ler a aba Network)

```text
Navegador ──► http://localhost:3000/api/...  ──(proxy.ts do Next)──► http://localhost:8080/...
```

O navegador **nunca** vê os tokens: o login guarda três cookies `httpOnly` (`rkf_token`, o de acesso; `rkf_refresh`,
o de renovação, 7 dias; e `rkf_sessao`, só com perfil e validade), e o servidor do Next coloca o
`Authorization: Bearer` em cada chamada para a API.

### Primeira vez no DevTools? Como ver o que o site manda "por baixo"

Cada clique na tela vira um **pedido** para a API, e o navegador mostra todos eles:

1. Com o site aberto, aperte **F12** (ou botão direito → **Inspecionar**) e abra a aba **Network** (Rede).
2. No filtro, digite `api` para ver só os pedidos do banco (sem imagens e scripts).
3. Faça a ação na tela (ex.: **Depositar**). Aparece uma linha nova: clique nela.
   - **Headers:** o método e o endereço (`POST /api/contas/.../depositos`) e o **Status** (`204`).
   - **Payload:** o que o site mandou (`{"valor":150.00,...}`).
   - **Response:** o que a API respondeu (vazio num `204`, que quer dizer "feito, nada para mostrar").
4. Erro de propósito (ex.: sacar mais do que tem)? A linha fica vermelha com `422`, e a **Response** traz o `codigo`
   (`SALDO_INSUFICIENTE`) que a tela usou para escolher a mensagem. Vermelho aqui é o teste **passando**.

Nos casos abaixo, a coluna **Por baixo** é exatamente o que essa aba mostra.

---

## 🧪 Roteiro principal: um cliente novo, do cadastro ao extrato

É o mesmo caminho da pasta **02 · Fluxo E2E** da coleção do Insomnia, agora pelas telas. Marque conforme for:

- [ ] **T01 · Cadastro com o aceite dos termos** (`/cadastro`, botão **Abrir minha conta** na landing)
- [ ] **T02 · Conta ORIGEM** (`/conta/abrir`)
- [ ] **T03 · Depósito, saque e transferência para a conta DESTINO** (a do Bruno)
- [ ] **T04 · Extrato** dos dois lados
- [ ] **T05 · Meus dados e troca de senha**
- [ ] **T06 · Sessão: renovar sozinha e sair**

### T01 · Cadastro com o aceite dos termos

Em `/cadastro`, digite: nome `Cliente Novo`, CPF `52998224725`, celular `11999998888`, e-mail
`cliente.novo@exemplo.test`, CEP `06020010`, número `123`, senha `minha-senha-forte` (duas vezes).

1. Clique **Criar meu cadastro** **sem** marcar a caixa dos termos. ✅ Nenhum pedido sai; aparece "Para criar a
   conta, aceite os Termos de Uso e a Política de Privacidade." embaixo da caixa.
2. Abra os links **Termos de Uso** e **Política de Privacidade** (outra aba, `/termos` e `/privacidade`, com a versão
   `2026-10-23` no topo).
3. Marque **Li e aceito os Termos de Uso e a Política de Privacidade do RKF.** e clique **Criar meu cadastro**.

Enquanto você digita, o CPF vira `529.982.247-25` e a força da senha aparece ("Razoável"). Por baixo:

```http
POST /api/clientes
Content-Type: application/json

{
  "aceiteTermos": true,
  "termosVersao": "2026-10-23",
  "nomeCompleto": "Cliente Novo",
  "cpf": "52998224725",
  "email": "cliente.novo@exemplo.test",
  "telefone": "11999998888",
  "cep": "06020010",
  "numero": "123",
  "senha": "minha-senha-forte"
}
```

✅ **201**

```json
{
  "id": "1a3de7a4-602b-4eb2-ab92-91bc0e5209f6",
  "nomeCompleto": "Cliente Novo",
  "cpf": "***.982.247-**",
  "email": "cliente.novo@exemplo.test",
  "telefone": "(11) 99999-8888",
  "endereco": { "cep": "06020-010", "logradouro": "Avenida dos Autonomistas", "bairro": "Vila Yara", "cidade": "Osasco", "uf": "SP", "ibge": "3534401", "numero": "123", "complemento": null },
  "termosVersao": "2026-10-23",
  "termosAceitosEm": "2026-10-23T17:46:07.012Z"
}
```

Em seguida o site entra sozinho: `POST /api/auth/login` `{"cpf":"52998224725","senha":"minha-senha-forte"}` → **200**
com `{"clienteId":"1a3de7a4-...","perfil":"CLIENTE","expiraEm":"...","deveTrocarSenha":false}` (sem token nenhum no
corpo: ele vai nos cookies).

Na tela: ✅ **"Bem-vindo ao RKF, Cliente."** com **Avenida dos Autonomistas, 123 · Vila Yara · Osasco/SP · 06020-010**.
A tela mascara para ler melhor, a API **higieniza** (guarda só os dígitos), valida o CPF pelos dígitos verificadores,
busca o endereço no **ViaCEP**, grava a versão e o momento do aceite e devolve o CPF **mascarado**.

### T02 a T05 · Conta, dinheiro, extrato, meus dados e senha

| # | O que fazer | Por baixo (pedido → resposta) | O que deve aparecer |
|---|---|---|---|
| T02 | **Abrir minha conta** → **Conta corrente** → **Abrir conta corrente** | `POST /api/contas` `{"tipoConta":"CORRENTE"}` → **201** `{"id":"...","agencia":"0001","numeroConta":"<8 dígitos>","tipoConta":"CORRENTE","saldo":0.00,"ativa":true}` | **"Sua conta está aberta."** · Tipo Conta corrente · Agência `0001` · Conta com **8 dígitos** · Saldo R$ 0,00 (o número é gerado pelo banco; o dono vem do token) |
| T03a | **Ir para minha conta** → **Depositar** `150,00`, descrição `Deposito pelo site` | `POST /api/contas/{id}/depositos` · header `Idempotency-Key: <UUID>` · `{"valor":150.00,"descricao":"Deposito pelo site"}` → **204** sem corpo | **"Depósito concluído."** R$ 150,00 · Saldo atual R$ 150,00 |
| T03b | **Sacar** `50,00` | `POST /api/contas/{id}/saques` `{"valor":50.00}` → **204** | **"Saque concluído."** · saldo R$ 100,00 |
| T03c | **Transferir** `25,00` para o ID do Bruno (`a0000000-0000-4000-8000-000000000124`) | `POST /api/transferencias` `{"contaOrigemId":"{id}","contaDestinoId":"a0000000-…0124","valor":25.00}` → **204** | transferência concluída · saldo R$ 75,00 (débito e crédito na mesma transação) |
| T04 | **Extrato** | `GET /api/contas/{id}/lancamentos` → **200** com 3 lançamentos, do mais novo ao mais antigo | transferência 25,00 (saída), saque 50,00 (saída), depósito 150,00 (entrada): `valor` vem sempre positivo, e a tela decide entrada × saída pela conta de destino |
| T04b | **Sair** e entre como **Bruno** | `GET /api/contas` → **200** `[{"numeroConta":"900002","saldo":525.00,...}]` | saldo **R$ 525,00**; no extrato, a transferência de 25,00 como **entrada** |
| T05a | Como Bruno, **Meus dados** (`/conta/meus-dados`) | `GET /api/clientes/me` → **200** com `"cpf":"***.318.642-**"` e o endereço | CPF mascarado ("É o seu login: não muda"), endereço do ViaCEP e **Nada para salvar** até você mudar algo (LGPD: o titular vê o que o banco guarda; só o que mudou vai para a API) |
| T05b | Mude o celular → salve | `PATCH /api/clientes/me` `{"telefone":"..."}` → **200** | "Dados atualizados." |
| T05c | **Quero encerrar meu cadastro** → senha `chute-errado-1` → **Encerrar de vez** | `POST /api/clientes/me/encerramento` `{"senha":"chute-errado-1"}` → **422** `{"codigo":"SENHA_INCORRETA",...}` | **"Senha incorreta."** no campo: a tela decide pelo `codigo`, nunca pelo texto |
| T05d | Repita com a senha certa (`Visitante@2026`) | → **422** `{"codigo":"SALDO_NAO_ZERADO",...}` | **"Transfira ou saque o saldo das suas contas antes de encerrar o cadastro."** (dinheiro não some) |
| T05e | **Trocar senha** (`/conta/senha`) com a senha atual `chute-errado-2` | `PATCH /api/auth/senha` → **422** `{"codigo":"SENHA_ATUAL_INCORRETA",...}` | **"Senha atual incorreta."** no campo da senha atual |

> Quer encerrar de verdade? Faça com o cliente novo do T01: saque os R$ 75,00, volte em **Meus dados** e encerre. A
> sessão é apagada e o login dele não entra mais. Reiniciar a API traz tudo de volta.

### T06 · Sessão: renova sozinha e "sair" vale de verdade

**Renovar:** o token de acesso vence (60 min no perfil `visitante`), mas você não cai no login. Para ver acontecer sem
esperar uma hora: DevTools → **Application** → **Cookies** → `localhost:3000` → apague só o `rkf_token` e clique em
**Extrato**. Na aba **Network**:

```text
GET /api/contas/{id}/lancamentos   → 307 Temporary Redirect   (location: a mesma URL)
    set-cookie: rkf_token=eyJhbGci…;   HttpOnly
    set-cookie: rkf_refresh=oBB65u4e…; HttpOnly       ← um token de renovação NOVO (o antigo deixou de valer)
    set-cookie: rkf_sessao=%7B%22cl…;  HttpOnly
GET /api/contas/{id}/lancamentos   → 200
```

✅ O extrato abre direto. O servidor do Next trocou o par na API (`POST /auth/refresh`) e mandou o navegador repetir a
chamada já com os cookies novos. Cada token de renovação vale **uma vez**: se alguém reusar um já trocado, a API
derruba todas as sessões da pessoa.

**Sair:** clique **Sair** → `POST /api/auth/logout` → **204**, com os três cookies apagados. Por baixo o Next chamou
`POST /auth/logout` na API: o token de renovação deixou de valer. Depois disso, `GET /api/auth/sessao` → **401**
`{"codigo":"TOKEN_INVALIDO","message":"Sessão expirada; faça login novamente",...}`.

---

## 🛡️ Proteções que você consegue ver pela tela

<details>
<summary>🔁 <b>A mesma transferência não sai duas vezes</b> (idempotência)</summary>
<br/>

1. Abra o DevTools (`F12`) → **Network**.
2. Faça uma transferência de `10,00` para o Bruno. Na requisição `transferencias` → **Headers**, aparece
   `Idempotency-Key: <um UUID>`.
3. Nova transferência de `10,00`: antes de clicar, mude **No throttling** para **Offline** e clique **Transferir** →
   "Não foi possível conectar ao servidor". Volte para **No throttling** e clique de novo **sem mexer nos campos**.

Esperado: o dinheiro sai **uma vez só**, e as duas tentativas têm o **mesmo** `Idempotency-Key`. Se você editar um
campo antes de reenviar, a chave muda (é outra operação).
</details>

<details>
<summary>🚫 <b>Transferir para a própria conta</b></summary>
<br/>

Cole o ID da sua própria conta em **Transferir**: "Esse é o ID da sua própria conta." aparece no campo, e a API nem é
chamada. Um ID que não existe dá "Não encontramos uma conta com esse ID." (a API respondeu `404` `CONTA_NAO_ENCONTRADA`).
</details>

<details>
<summary>🎲 <b>Errar a senha seis vezes</b></summary>
<br/>

**Sair** e tente entrar 6 vezes com o CPF do Bruno e a senha `errada-123`: 5 × "CPF ou senha inválidos." (`401`
`CREDENCIAIS_INVALIDAS`) e, na 6ª, **"Muitas tentativas seguidas. Por segurança, aguarde alguns minutos e tente de
novo."** (`429` `MUITAS_TENTATIVAS`; vale até com a senha certa; reinicie a API para liberar).
</details>

<details>
<summary>🗂️ <b>Mais de uma conta: o seletor</b></summary>
<br/>

Quem tem duas contas (por exemplo, corrente e poupança abertas pela API: `POST /contas` `{"tipoConta":"POUPANCA"}`
com o token da pessoa, na [coleção](../backend/insomnia/rkfbank.insomnia.yaml)) vê, em `/conta`, um botão por conta
acima do título. Clicar troca saldo, número e ID na hora e vale para o extrato e as movimentações (a escolha fica
guardada neste navegador). Com uma conta só, o seletor não aparece.
</details>

<details>
<summary>🍪 <b>O token não aparece para o JavaScript</b></summary>
<br/>

DevTools → **Application** → **Cookies** → `localhost:3000`: `rkf_token` e `rkf_refresh` estão marcados como
**HttpOnly**. No **Console**, `document.cookie` não mostra nenhum dos dois. Quem coloca o `Authorization: Bearer` nas
chamadas é o servidor do Next (`proxy.ts`), então um script injetado na página não consegue roubá-los.
</details>

<details>
<summary>📝 <b>Logs da tela sem dado sensível</b></summary>
<br/>

DevTools → **Console** durante o cadastro e a troca de senha: aparecem os logs da tela e das chamadas à API, com CPF,
e-mail, endereço e senhas **mascarados** (`***`). Nunca o valor (regras em [`lib/logger/mascara.ts`](lib/logger/mascara.ts)).
</details>

---

## 🧯 Deu diferente?

| Sintoma | Causa provável | O que fazer |
|---|---|---|
| "Não foi possível conectar ao servidor" em tudo | a API não está no ar | confira o IntelliJ (ou o terminal 1); `http://localhost:8080/actuator/health` tem que dar `UP` |
| **Criar meu cadastro** não faz nada | a caixa dos termos não está marcada | marque o aceite (aparece a mensagem embaixo dela) |
| "Não conseguimos consultar o CEP agora" | sem internet (o ViaCEP é externo) | conecte e tente de novo |
| Cadastro diz que o CPF já existe | você já rodou o T01 nesta subida | reinicie a API (o H2 zera) ou use outro CPF válido: `70483726052` |
| Volta para o login no meio do teste | a API reiniciou (o perfil `visitante` sorteia o segredo do JWT a cada subida) ou você saiu | entre de novo: a tela volta para onde você estava |
| Volta para o login a cada hora | o cookie `rkf_refresh` não existe (DevTools → Application → Cookies) | apague os cookies `rkf_*` e entre de novo |
| `pnpm` não encontrado | o Corepack está desligado | `corepack enable` (vem com o Node) |

Encontrou um bug? Abra uma [issue](https://github.com/k3vinrich4rd/rkf_bank/issues) com o passo (ex.: `T03c`) e o que
apareceu. 🧡
