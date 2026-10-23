# RKF Bank — Como testar as telas, do zero

> **Para quem é:** qualquer pessoa que queira usar o site do RKF Bank na própria máquina e conferir cada tela, sem
> ajuda: visitantes, recrutadores, quem está chegando no projeto. Usa só as **contas de visitante** (senha pública).
> A API sozinha, sem o site, está no [guia de testes do backend](../backend/README_TESTES_E2E.md).
>
> 🔒 A equipe tem o mesmo roteiro com as contas internas (Felipe e Renan) e os casos de ADMIN no repositório privado
> (`.equipe/testes/FRONTEND_E2E.md`).

Conferido em 23/10/2026 com o contrato **3.0** da API. O que muda ao repetir: o número da conta (gerado pelo banco),
datas e horários.

---

## ⚡ Em 3 passos

| # | Terminal | Comando | Pronto quando |
|:-:|---|---|---|
| 1 | 1 · raiz do repositório | `./mvnw spring-boot:run -Dspring-boot.run.profiles=visitante` | `Started RkfbankApplication` (API em `http://localhost:8080`) |
| 2 | 2 · pasta `frontend` | `corepack enable && pnpm install && pnpm dev` | `Ready` (site em `http://localhost:3000`) |
| 3 | navegador | abra `http://localhost:3000` | a landing com o cartão RKF em 3D |

Pré-requisitos: **JDK 25** e **Node 20+**. Sem Docker e sem `.env`: o perfil `visitante` usa um banco H2 em memória
que some quando a API para.

| Conta pronta | CPF (login) | Senha | Saldo inicial | ID da conta (para receber transferências) |
|---|---|---|---|---|
| 🟠 Ana Visitante (Origem) | `246.813.579-28` | `Visitante@2026` | R$ 1.000,00 | `a0000000-0000-4000-8000-000000000114` |
| 🔵 Bruno Visitante (Destino) | `975.318.642-82` | `Visitante@2026` | R$ 500,00 | `a0000000-0000-4000-8000-000000000124` |

---

## 🧪 Roteiro principal: um cliente novo, do cadastro ao extrato

É o mesmo caminho da pasta **02 · Fluxo E2E** da coleção do Insomnia, agora pelas telas. Marque conforme for:

- [ ] **T01 · Cadastro** (`/cadastro`, botão **Abrir minha conta** na landing)
- [ ] **T02 · Conta ORIGEM** (`/conta/abrir`)
- [ ] **T03 · Depósito, saque e transferência para a conta DESTINO** (a do Bruno)
- [ ] **T04 · Extrato** dos dois lados
- [ ] **T05 · Meus dados e troca de senha**

| # | O que fazer | O que deve aparecer | Por quê |
|---|---|---|---|
| T01 | Em `/cadastro`, digite: nome `Cliente Novo do Insomnia`, CPF `52998224725`, celular `11999998888`, e-mail `cliente.novo@exemplo.test`, CEP `06020010`, número `123`, senha `minha-senha-forte` (duas vezes) → **Criar meu cadastro** | enquanto você digita, o CPF vira `529.982.247-25` e a força da senha aparece ("Razoável"). Depois: **"Bem-vindo ao RKF, Cliente."** com **Avenida dos Autonomistas, 123 · Vila Yara · Osasco/SP · 06020-010** | a tela mascara para ler melhor, a API **higieniza** (guarda só os dígitos), valida o CPF pelos dígitos verificadores e busca o endereço no **ViaCEP**. O login acontece sozinho |
| T02 | **Abrir minha conta** → **Conta corrente** → **Abrir conta corrente** | **"Sua conta está aberta."** · Tipo Conta corrente · Agência `0001` · Conta com **8 dígitos** · Saldo R$ 0,00 | o número é gerado pelo banco (o último dígito é verificador); o dono vem do token, não da tela |
| T03a | **Ir para minha conta** → **Depositar** `150,00`, descrição `Deposito pelo site` | **"Depósito concluído."** R$ 150,00 · Saldo atual R$ 150,00 | — |
| T03b | **Sacar** `50,00` | **"Saque concluído."** · saldo R$ 100,00 | — |
| T03c | **Transferir** `25,00` para o ID do Bruno (`a0000000-0000-4000-8000-000000000124`) | transferência concluída · saldo R$ 75,00 | débito e crédito na mesma transação |
| T04 | **Extrato** | do mais novo ao mais antigo: transferência 25,00 (saída), saque 50,00 (saída), depósito 150,00 (entrada) | `valor` vem sempre positivo; a tela decide entrada × saída pela conta de destino |
| T04b | **Sair** e entre como **Bruno** | saldo **R$ 525,00**; no extrato, a transferência de 25,00 como **entrada** | — |
| T05a | Como Bruno, **Meus dados** (`/conta/meus-dados`) | CPF mascarado `***.***.642-82` ("É o seu login: não muda"), endereço do ViaCEP e o botão **Nada para salvar** até você mudar algo | LGPD: o titular vê o que o banco guarda; só o que mudou vai para a API |
| T05b | Mude o celular → salve | "Dados atualizados." | — |
| T05c | **Quero encerrar meu cadastro** → senha `chute-errado-1` → **Encerrar de vez** | **"Senha incorreta."** no campo | a tela decide pelo `codigo` `SENHA_INCORRETA` da API, nunca pelo texto |
| T05d | Repita com a senha certa (`Visitante@2026`) | **"Transfira ou saque o saldo das suas contas antes de encerrar o cadastro."** | `codigo` `SALDO_NAO_ZERADO`: dinheiro não some |
| T05e | **Trocar senha** (`/conta/senha`) com a senha atual `chute-errado-2` | **"Senha atual incorreta."** no campo da senha atual | `codigo` `SENHA_ATUAL_INCORRETA` |

> Quer encerrar de verdade? Faça com o cliente novo do T01: saque os R$ 75,00, volte em **Meus dados** e encerre. A
> sessão é apagada e o login dele não entra mais. Reiniciar a API traz tudo de volta.

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
chamada. Um ID que não existe dá "Não encontramos uma conta com esse ID."
</details>

<details>
<summary>🎲 <b>Errar a senha seis vezes</b></summary>
<br/>

**Sair** e tente entrar 6 vezes com o CPF do Bruno e a senha `errada-123`: 5 × "CPF ou senha inválidos." e, na 6ª,
**"Muitas tentativas seguidas. Por segurança, aguarde alguns minutos e tente de novo."** (vale até com a senha certa;
reinicie a API para liberar).
</details>

<details>
<summary>🍪 <b>O token não aparece para o JavaScript</b></summary>
<br/>

DevTools → **Application** → **Cookies** → `localhost:3000`: o `rkf_token` está marcado como **HttpOnly**. No
**Console**, `document.cookie` não mostra o token. Quem coloca o `Authorization: Bearer` nas chamadas é o servidor do
Next (`proxy.ts`), então um script injetado na página não consegue roubá-lo.
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
| "Não foi possível conectar ao servidor" em tudo | a API não está no ar | confira o terminal 1; `curl http://localhost:8080/actuator/health` tem que dar `UP` |
| "Não conseguimos consultar o CEP agora" | sem internet (o ViaCEP é externo) | conecte e tente de novo |
| Cadastro diz que o CPF já existe | você já rodou o T01 nesta subida | reinicie a API (o H2 zera) ou use outro CPF válido: `70483726052` |
| Volta para o login no meio do teste | o token venceu (60 min no visitante) ou a API reiniciou | entre de novo: a tela volta para onde você estava |
| `pnpm` não encontrado | o Corepack está desligado | `corepack enable` (vem com o Node) |

Encontrou um bug? Abra uma [issue](https://github.com/k3vinrich4rd/rkf_bank/issues) com o passo (ex.: `T03c`) e o que
apareceu. 🧡
