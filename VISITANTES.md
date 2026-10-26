# 👋 Teste o RKF Bank em 15 minutos

| | |
|---|---|
| 🎯 **Para que serve** | ver o banco funcionando em 15 minutos: **só o essencial** de um cliente |
| 👥 **Área responsável** | 🟦 **Backend** (a API) · para visitantes, recrutadores e curiosos |
| ⏰ **Quando usar** | primeira vez no projeto |
| 🧰 **Como** | Insomnia + coleção pública, perfil `visitante` (H2, sem Docker e sem `.env`) |
| 🚫 **Não cobre** | o ADMIN (só a equipe testa) · as telas → [frontend/README_TESTES_E2E.md](frontend/README_TESTES_E2E.md) · tudo o que o cliente faz → [backend/README_TESTES_E2E.md](backend/README_TESTES_E2E.md) |


Você chegou pelo GitHub e quer ver o banco funcionando? Aqui está **só o necessário**: você entra como a **Ana**,
manda dinheiro para o **Bruno**, confere o extrato e vê três proteções do banco em ação. Sem Docker, sem `.env`,
sem cadastrar nada, e você **não digita nenhum dado**: a coleção do Insomnia já vem preenchida.

| Você vai precisar | Para quê |
|---|---|
| **JDK 25** (`java -version` mostra `25.x`) | rodar a API |
| **Insomnia** 10 ou mais novo ([download](https://insomnia.rest/download)) | mandar os pedidos com um clique |
| **IntelliJ** (opcional) | rodar a API com um clique em vez do terminal |

---

## Antes de começar (5 min, uma vez)

### A. Suba a API

```bash
git clone https://github.com/k3vinrich4rd/rkf_bank.git
cd rkf_bank
./mvnw spring-boot:run -Dspring-boot.run.profiles=visitante
```

- **PowerShell:** `.\mvnw.cmd spring-boot:run "-Dspring-boot.run.profiles=visitante"`
- **IntelliJ:** **File → Open** na pasta `rkf_bank` → escolha **API · Visitante (H2)** ao lado do ▶️ e clique ▶️.
  JDK 25 em **File → Project Structure → SDK** (*Download JDK* se não tiver).

<details>
<summary>⚙️ A configuração <b>API · Visitante (H2)</b> não apareceu? Crie na mão (VM options)</summary>

1. **Run → Edit Configurations… → + → Application**.
2. **Name** `API · Visitante (H2)` · **JDK** 25 · **Main class** `com.br.rkfbank.RkfbankApplication`.
3. **Modify options → Add VM options** → cole `-Dspring.profiles.active=visitante`.
   (A 8080 já está em uso? Acrescente ` -Dserver.port=8081` e troque `base_url` no ambiente do Insomnia.)
4. **Working directory:** `$PROJECT_DIR$` (a pasta `rkf_bank`) → **OK** → ▶️.
5. No log tem que aparecer `The following 1 profile is active: "visitante"`.

Não precisa de Docker nem de `.env`: o perfil `visitante` usa um banco em memória (H2) e sorteia o segredo do login.
</details>

✅ **Pronto quando** o log mostra `Started RkfbankApplication` e http://localhost:8080/actuator/health mostra
`{"groups":["liveness","readiness"],"status":"UP"}`.

### B. Importe a coleção no Insomnia

1. Insomnia → **Import** → arquivo [`backend/insomnia/rkfbank.insomnia.yaml`](backend/insomnia/rkfbank.insomnia.yaml).
2. No topo, escolha o ambiente **Visitante (H2, Ana e Bruno)**.
3. Pronto: à esquerda aparecem as pastas `00` a `09`. Cada passo abaixo diz **qual pasta e qual requisição** clicar.

> 💡 **O que é cada coisa:** uma *requisição* é um pedido para o banco (ex.: "deposite R$ 150"). O banco devolve um
> **status** (um número: `200`/`201`/`204` = deu certo; `4xx` = o banco recusou de propósito) e, às vezes, um JSON com
> os dados. Você só precisa comparar o **status** e os campos principais com o que está em "Tem que voltar".
> Ids e datas mudam a cada execução, e o Insomnia mostra `1000.00` onde aqui aparece `1000`: é o mesmo valor.

| Método | Quer dizer | Nos passos |
|:-:|---|---|
| `GET` | **ler**, sem mudar nada (sem corpo) | ver contas e extrato |
| `POST` | **criar** ou **fazer uma ação** (corpo em JSON) | login, depósito, saque, transferência |
| `PATCH` | **mudar uma parte** (corpo em JSON) | corrigir dados e trocar senha (pasta 06) |

Os headers que aparecem: `Authorization: Bearer …` é o crachá do login (a coleção põe sozinha);
`Idempotency-Key` é o número único de cada depósito, saque ou transferência (a coleção sorteia).
Status: `2xx` deu certo · `400` pedido mal escrito · `401` sem login · `403` não pode · `422` a regra do banco não deixa.

### Quem é quem

| | Ana Visitante (Origem) | Bruno Visitante (Destino) |
|---|---|---|
| Papel no teste | quem **manda** o dinheiro | quem **recebe** |
| CPF / senha | `246.813.579-28` / `Visitante@2026` | `975.318.642-82` / `Visitante@2026` |
| Conta (agência / número) | `0001` / `900001` | `0001` / `900002` |
| Saldo quando a API sobe | **R$ 1.000,00** | **R$ 500,00** |

Dados fictícios. A senha é pública de propósito: só existe dentro do perfil `visitante`, na sua máquina.

---

## Os 13 passos

Faça na ordem: cada passo usa o que o anterior guardou (o login guarda o "crachá", o token, sozinho).
Errou algo ou quer começar de novo? Pare a API e suba outra vez: os saldos voltam ao início.

| Passos | O que você prova |
|:-:|---|
| 1–2 | a Ana e o Bruno entram no banco |
| 3–8 | o dinheiro anda: saldo, depósito, saque, transferência e os dois extratos |
| 9–10 | clicar duas vezes **não** cobra duas vezes |
| 11–13 | o banco barra quem não deve: sem login, conta dos outros e saldo insuficiente |

<!-- roteiro-gerado:trilha:inicio -->
> 🤖 Pedidos e respostas reais (ambiente **visitante**), gerados por `scripts/gerar-roteiro-e2e.mjs`. Ids, números de conta e datas mudam a cada execução; o **status** e o `codigo` não.

**A ordem, de uma olhada** (o detalhe de cada passo vem logo abaixo):

| Passo | Método | Rota | Crachá (token) | O que faz | Tem que voltar |
|:-:|:-:|---|---|---|:-:|
| 1 | `POST` | `/auth/login` | — (público) | Login · cliente ORIGEM (Ana / Fellipe) | **200** |
| 2 | `POST` | `/auth/login` | — (público) | Login · cliente DESTINO (Bruno / Renan) | **200** |
| 3 | `GET` | `/contas` | `token_origem` | Minhas contas (ORIGEM) | **200** |
| 4 | `POST` | `/contas/{conta_origem_id}/depositos` | `token_origem` | Depositar R$ 150,00 | **204** |
| 5 | `POST` | `/contas/{conta_origem_id}/saques` | `token_origem` | Sacar R$ 50,00 | **204** |
| 6 | `POST` | `/transferencias` | `token_origem` | Transferir R$ 25,00 ORIGEM → DESTINO | **204** |
| 7 | `GET` | `/contas/{conta_origem_id}/lancamentos` | `token_origem` | Extrato da ORIGEM | **200** |
| 8 | `GET` | `/contas/{conta_destino_id}/lancamentos` | `token_destino` | Extrato do DESTINO (token do destino) | **200** |
| 9 | `POST` | `/contas/{conta_origem_id}/depositos` | `token_origem` | 1ª vez: depósito com chave fixa | **204** |
| 10 | `POST` | `/contas/{conta_origem_id}/depositos` | `token_origem` | 2ª vez: mesma chave, mesmo corpo → repetição | **204** |
| 11 | `GET` | `/contas` | — (público) | Sem token → 401 | **401** |
| 12 | `GET` | `/contas/{conta_origem_id}/lancamentos` | `token_destino` | Ver o extrato de outro cliente → 403 | **403** |
| 13 | `POST` | `/contas/{conta_origem_id}/saques` | `token_origem` | Sacar mais que o saldo → 422 | **422** |

### Passo 1 · Login · cliente ORIGEM (Ana / Fellipe)

📂 Insomnia: **01 · Login (guarda o token sozinho)** → **Login · cliente ORIGEM (Ana / Fellipe)** → **Send**

💡 Entra como o cliente que vai MANDAR dinheiro. Na resposta, o `token` é o crachá de acesso; o Insomnia guarda ele em `token_origem` e as próximas requisições usam sozinhas.

**Você manda**

```http
POST /auth/login
Content-Type: application/json

{
  "cpf": "24681357928",
  "senha": "Visitante@2026"
}
```

**Tem que voltar**

✅ **200**

```json
{
  "token": "eyJhbGciOiJIUzI1NiJ9...",
  "tipo": "Bearer",
  "expiraEmSegundos": 3600,
  "clienteId": "a0000000-0000-4000-8000-000000000112",
  "perfil": "CLIENTE",
  "deveTrocarSenha": false,
  "refreshToken": "q3Jd0kX4...",
  "refreshExpiraEmSegundos": 604800
}
```

- [ ] voltou igual

### Passo 2 · Login · cliente DESTINO (Bruno / Renan)

📂 Insomnia: **01 · Login (guarda o token sozinho)** → **Login · cliente DESTINO (Bruno / Renan)** → **Send**

💡 Entra como o cliente que vai RECEBER dinheiro. Ter dois clientes logados permite provar que um não enxerga a conta do outro.

**Você manda**

```http
POST /auth/login
Content-Type: application/json

{
  "cpf": "97531864282",
  "senha": "Visitante@2026"
}
```

**Tem que voltar**

✅ **200**

```json
{
  "token": "eyJhbGciOiJIUzI1NiJ9...",
  "tipo": "Bearer",
  "expiraEmSegundos": 3600,
  "clienteId": "a0000000-0000-4000-8000-000000000122",
  "perfil": "CLIENTE",
  "deveTrocarSenha": false,
  "refreshToken": "q3Jd0kX4...",
  "refreshExpiraEmSegundos": 604800
}
```

- [ ] voltou igual

### Passo 3 · Minhas contas (ORIGEM)

📂 Insomnia: **03 · Dinheiro entre clientes (contas prontas do seed)** → **Minhas contas (ORIGEM)** → **Send**

💡 Lista as contas da Ana (ou do Fellipe, na equipe), que já vêm com dinheiro do banco de teste. O Insomnia guarda o id da primeira conta para os próximos passos.

**Você manda**

```http
GET /contas
Authorization: Bearer {{ token_origem }}
```

**Tem que voltar**

✅ **200**

```json
[
  {
    "id": "a0000000-0000-4000-8000-000000000114",
    "clienteId": "a0000000-0000-4000-8000-000000000112",
    "agencia": "0001",
    "numeroConta": "900001",
    "tipoConta": "CORRENTE",
    "saldo": 1000,
    "ativa": true
  }
]
```

- [ ] voltou igual

### Passo 4 · Depositar R$ 150,00

📂 Insomnia: **03 · Dinheiro entre clientes (contas prontas do seed)** → **Depositar R$ 150,00** → **Send**

💡 Depósito na conta da ORIGEM. `204`, sem corpo, é sucesso.

**Você manda**

```http
POST /contas/a0000000-0000-4000-8000-000000000114/depositos
Authorization: Bearer {{ token_origem }}
Content-Type: application/json
Idempotency-Key: <UUID novo a cada envio>

{
  "valor": 150,
  "descricao": "Deposito pelo Insomnia"
}
```

**Tem que voltar**

✅ **204** · sem corpo

- [ ] voltou igual

### Passo 5 · Sacar R$ 50,00

📂 Insomnia: **03 · Dinheiro entre clientes (contas prontas do seed)** → **Sacar R$ 50,00** → **Send**

💡 Saque da conta da ORIGEM. `204` de novo.

**Você manda**

```http
POST /contas/a0000000-0000-4000-8000-000000000114/saques
Authorization: Bearer {{ token_origem }}
Content-Type: application/json
Idempotency-Key: <UUID novo a cada envio>

{
  "valor": 50,
  "descricao": "Saque pelo Insomnia"
}
```

**Tem que voltar**

✅ **204** · sem corpo

- [ ] voltou igual

### Passo 6 · Transferir R$ 25,00 ORIGEM → DESTINO

📂 Insomnia: **03 · Dinheiro entre clientes (contas prontas do seed)** → **Transferir R$ 25,00 ORIGEM → DESTINO** → **Send**

💡 Agora de uma PESSOA para OUTRA: Ana manda para o Bruno. Só o dono da conta de origem pode mandar.

**Você manda**

```http
POST /transferencias
Authorization: Bearer {{ token_origem }}
Content-Type: application/json
Idempotency-Key: <UUID novo a cada envio>

{
  "contaOrigemId": "a0000000-0000-4000-8000-000000000114",
  "contaDestinoId": "a0000000-0000-4000-8000-000000000124",
  "valor": 25,
  "descricao": "Transferencia pelo Insomnia"
}
```

**Tem que voltar**

✅ **204** · sem corpo

- [ ] voltou igual

### Passo 7 · Extrato da ORIGEM

📂 Insomnia: **03 · Dinheiro entre clientes (contas prontas do seed)** → **Extrato da ORIGEM** → **Send**

💡 O extrato da Ana mostra o depósito, o saque e a transferência que saiu.

**Você manda**

```http
GET /contas/a0000000-0000-4000-8000-000000000114/lancamentos
Authorization: Bearer {{ token_origem }}
```

**Tem que voltar**

✅ **200**

```json
[
  {
    "id": "22ba5e1d-2e68-4d6e-9b89-c5126acdf1be",
    "tipoLancamento": "TRANSFERENCIA",
    "valor": 25,
    "contaOrigemId": "a0000000-0000-4000-8000-000000000114",
    "contaDestinoId": "a0000000-0000-4000-8000-000000000124",
    "descricao": "Transferencia pelo Insomnia",
    "dataHora": "2026-10-26T13:14:08.012722Z"
  },
  {
    "id": "9d064e76-dc53-46a1-bd09-4a8e923b2a59",
    "tipoLancamento": "SAQUE",
    "valor": 50,
    "contaOrigemId": "a0000000-0000-4000-8000-000000000114",
    "contaDestinoId": null,
    "descricao": "Saque pelo Insomnia",
    "dataHora": "2026-10-26T13:14:08.000726Z"
  },
  "… mais 2 itens no mesmo formato"
]
```

- [ ] voltou igual

### Passo 8 · Extrato do DESTINO (token do destino)

📂 Insomnia: **03 · Dinheiro entre clientes (contas prontas do seed)** → **Extrato do DESTINO (token do destino)** → **Send**

💡 O Bruno entra com o crachá DELE e vê a transferência que chegou. Com o crachá da Ana, este mesmo pedido daria `403` (proibido).

**Você manda**

```http
GET /contas/a0000000-0000-4000-8000-000000000124/lancamentos
Authorization: Bearer {{ token_destino }}
```

**Tem que voltar**

✅ **200**

```json
[
  {
    "id": "22ba5e1d-2e68-4d6e-9b89-c5126acdf1be",
    "tipoLancamento": "TRANSFERENCIA",
    "valor": 25,
    "contaOrigemId": "a0000000-0000-4000-8000-000000000114",
    "contaDestinoId": "a0000000-0000-4000-8000-000000000124",
    "descricao": "Transferencia pelo Insomnia",
    "dataHora": "2026-10-26T13:14:08.012722Z"
  },
  {
    "id": "a0000000-0000-4000-8000-000000000125",
    "tipoLancamento": "DEPOSITO",
    "valor": 500,
    "contaOrigemId": null,
    "contaDestinoId": "a0000000-0000-4000-8000-000000000124",
    "descricao": "Saldo de boas-vindas do visitante",
    "dataHora": "2026-09-25T12:00:00Z"
  }
]
```

- [ ] voltou igual

### Passo 9 · 1ª vez: depósito com chave fixa

📂 Insomnia: **04 · Idempotência (o mesmo pedido não movimenta duas vezes)** → **1ª vez: depósito com chave fixa** → **Send**

💡 Um depósito com uma "senha de pedido" (`Idempotency-Key`) que fica guardada. É como o número do pedido de uma compra.

**Você manda**

```http
POST /contas/a0000000-0000-4000-8000-000000000114/depositos
Authorization: Bearer {{ token_origem }}
Content-Type: application/json
Idempotency-Key: {{ chave_fixa }}

{
  "valor": 10,
  "descricao": "Idempotencia"
}
```

**Tem que voltar**

✅ **204** · sem corpo

- [ ] voltou igual

### Passo 10 · 2ª vez: mesma chave, mesmo corpo → repetição

📂 Insomnia: **04 · Idempotência (o mesmo pedido não movimenta duas vezes)** → **2ª vez: mesma chave, mesmo corpo → repetição** → **Send**

💡 Manda o MESMO pedido de novo, como se a internet tivesse caído e o app tentasse outra vez. O banco reconhece o número, responde igual e NÃO deposita de novo. Repare no header `Idempotency-Replayed: true`.

**Você manda**

```http
POST /contas/a0000000-0000-4000-8000-000000000114/depositos
Authorization: Bearer {{ token_origem }}
Content-Type: application/json
Idempotency-Key: {{ chave_fixa }}

{
  "valor": 10,
  "descricao": "Idempotencia"
}
```

**Tem que voltar**

✅ **204** · sem corpo · header `Idempotency-Replayed: true`

- [ ] voltou igual

### Passo 11 · Sem token → 401

📂 Insomnia: **07 · Erros que DEVEM acontecer** → **Sem token → 401** → **Send**

💡 Pedir as contas sem o crachá: `401` ("quem é você?").

**Você manda**

```http
GET /contas
```

**Tem que voltar**

✅ **401**

```json
{
  "timestamp": "2026-10-26T13:14:09.100821900Z",
  "status": 401,
  "error": "Unauthorized",
  "codigo": "AUTENTICACAO_NECESSARIA",
  "message": "Autenticação necessária: envie o token no header Authorization (Bearer)",
  "path": "/contas",
  "campos": []
}
```

- [ ] voltou igual

### Passo 12 · Ver o extrato de outro cliente → 403

📂 Insomnia: **07 · Erros que DEVEM acontecer** → **Ver o extrato de outro cliente → 403** → **Send**

💡 O Bruno tentando ver o extrato da Ana: `403`.

**Você manda**

```http
GET /contas/a0000000-0000-4000-8000-000000000114/lancamentos
Authorization: Bearer {{ token_destino }}
```

**Tem que voltar**

✅ **403**

```json
{
  "timestamp": "2026-10-26T13:14:09.198146300Z",
  "status": 403,
  "error": "Forbidden",
  "codigo": "CONTA_DE_OUTRO_CLIENTE",
  "message": "Conta não pertence ao usuário logado",
  "path": "/contas/a0000000-0000-4000-8000-000000000114/lancamentos",
  "campos": []
}
```

- [ ] voltou igual

### Passo 13 · Sacar mais que o saldo → 422

📂 Insomnia: **07 · Erros que DEVEM acontecer** → **Sacar mais que o saldo → 422** → **Send**

💡 Sacar mais do que tem: recusado com o `codigo` `SALDO_INSUFICIENTE`. Nenhum centavo sai.

**Você manda**

```http
POST /contas/a0000000-0000-4000-8000-000000000114/saques
Authorization: Bearer {{ token_origem }}
Content-Type: application/json
Idempotency-Key: <UUID novo a cada envio>

{
  "valor": 999999
}
```

**Tem que voltar**

✅ **422**

```json
{
  "timestamp": "2026-10-26T13:14:09.217664300Z",
  "status": 422,
  "error": "Unprocessable Entity",
  "codigo": "SALDO_INSUFICIENTE",
  "message": "Saldo insuficiente",
  "path": "/contas/a0000000-0000-4000-8000-000000000114/saques",
  "campos": []
}
```

- [ ] voltou igual

<!-- roteiro-gerado:trilha:fim -->

🎉 **Terminou os 13?** Você testou o coração do banco. A conta final: a Ana começou com R$ 1.000,00, depositou 150,
sacou 50, transferiu 25 e depositou 10 uma vez só (o passo 10 foi a repetição): **R$ 1.085,00**. Confira em
**03 · Dinheiro entre clientes → Minhas contas (ORIGEM)**.

---

## Quer ir além? (opcional)

| Pasta da coleção | O que testa | Tempo |
|---|---|:-:|
| **02 · Fluxo E2E** | você cria **o seu** cliente (o CEP vira endereço pelo ViaCEP), abre duas contas e movimenta | 5 min |
| **06 · Meus dados (LGPD)** | ver e corrigir os próprios dados, trocar a senha e encerrar o cadastro (rode a 02 antes) | 5 min |
| **07 · Erros que DEVEM acontecer** | 25 recusas do banco, cada uma com o `codigo` certo | 3 min |
| **09 · Sessão** | continuar logado sem digitar a senha (refresh token) e sair de verdade | 2 min |
| **Tudo de uma vez** | clique com o botão direito na coleção → **Run** (Runner do Insomnia) ou `node scripts/rodar-colecao-insomnia.mjs` → `83/83` | 1 min |

Cada requisição tem um **💡 Em palavras simples** na aba **Docs** do Insomnia. O roteiro completo, com o pedido e a
resposta de todas as 83 requisições e a explicação de cada conceito, fica em
[backend/README_TESTES_E2E.md](backend/README_TESTES_E2E.md). Todas as rotas no navegador:
http://localhost:8080/swagger-ui.html.

<details>
<summary>⌨️ Prefere o terminal? Os mesmos passos com <code>curl</code> (Git Bash, Linux ou macOS)</summary>

```bash
API=http://localhost:8080; H='Content-Type: application/json'
ANA=a0000000-0000-4000-8000-000000000114; BRUNO=a0000000-0000-4000-8000-000000000124
TOKEN=$(curl -s -X POST $API/auth/login -H "$H" -d '{"cpf":"24681357928","senha":"Visitante@2026"}' | sed -E 's/.*"token":"([^"]+)".*/\1/')

curl -s $API/contas -H "Authorization: Bearer $TOKEN"                     # saldo 1000.00

CHAVE=$(node -e 'console.log(crypto.randomUUID())')                       # uma chave por operação
curl -s -i -X POST $API/transferencias -H "$H" -H "Authorization: Bearer $TOKEN" -H "Idempotency-Key: $CHAVE" \
  -d "{\"contaOrigemId\":\"$ANA\",\"contaDestinoId\":\"$BRUNO\",\"valor\":200.00,\"descricao\":\"Aluguel\"}" | head -1
# HTTP/1.1 204

# DE NOVO com a mesma chave (como se a rede tivesse caído): o banco não debita duas vezes.
curl -s -i -X POST $API/transferencias -H "$H" -H "Authorization: Bearer $TOKEN" -H "Idempotency-Key: $CHAVE" \
  -d "{\"contaOrigemId\":\"$ANA\",\"contaDestinoId\":\"$BRUNO\",\"valor\":200.00,\"descricao\":\"Aluguel\"}" | grep -E "HTTP|Idempotency"
# HTTP/1.1 204
# Idempotency-Replayed: true

curl -s $API/contas -H "Authorization: Bearer $TOKEN"                     # saldo 800.00 (não 600.00)
curl -s $API/contas/$ANA/lancamentos -H "Authorization: Bearer $TOKEN"    # extrato
curl -s $API/contas/$BRUNO/lancamentos -H "Authorization: Bearer $TOKEN"  # 403 CONTA_DE_OUTRO_CLIENTE
```

</details>

<details>
<summary>🗄️ Ver as tabelas por dentro (banco H2 no navegador)</summary>

Abra http://localhost:8080/h2-console, troque a JDBC URL para `jdbc:h2:mem:rkfbank-visitante` (usuário `sa`, senha
vazia) e rode `SELECT numero_conta, saldo FROM tb_conta;` antes e depois de uma transferência.
</details>

## 🛡️ E o administrador?

O banco tem um perfil `ADMIN` (gestão de usuários e auditoria dos saldos), mas ele **não existe** no perfil
`visitante`, de propósito: a senha dos visitantes é pública, e um ADMIN com senha pública seria um ADMIN de qualquer
um. Os testes de ADMIN ficam com a equipe. Daqui você vê o outro lado: a Ana tentando uma rota de ADMIN recebe `403`
(pasta **07**). Por isso a coleção pula da pasta `04` para a `06`.

## O que este perfil garante

- **Nada fica gravado:** banco em memória; parou a API, sumiu tudo. Na próxima subida, saldos iniciais de novo.
- **Ninguém forja o login:** o segredo do token é sorteado a cada subida e não está escrito em lugar nenhum.
- **Só a Ana e o Bruno existem:** sem admin e sem conta da equipe. Seu `.env`, se tiver um, é ignorado.
- **É para a sua máquina**, não para servidor. Quer colaborar? Veja o [CONTRIBUTING.md](CONTRIBUTING.md).

Achou um bug ou tem sugestão? Abra uma [issue](https://github.com/k3vinrich4rd/rkf_bank/issues). 🧡
