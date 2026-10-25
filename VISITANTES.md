# 👋 RKF Bank para visitantes

Você chegou pelo GitHub e quer ver o banco funcionando? Este é o caminho feito para você: **um comando**, sem Docker,
sem `.env` e sem cadastrar nada. A API sobe na sua máquina com duas contas prontas e um banco que some quando você
para a aplicação.

## 1. Suba a API (perfil `visitante`)

Pré-requisito: **JDK 25** (`java -version` mostra `25.x`).

```bash
git clone https://github.com/k3vinrich4rd/rkf_bank.git
cd rkf_bank
./mvnw spring-boot:run -Dspring-boot.run.profiles=visitante
```

No PowerShell: `.\mvnw.cmd spring-boot:run "-Dspring-boot.run.profiles=visitante"`.
**Pelo IntelliJ:** **File → Open** na pasta `rkf_bank` → JDK 25 em **File → Project Structure → SDK** (o próprio
IntelliJ baixa: *Download JDK*) → escolha **API · Visitante (H2)** ao lado do ▶️ e clique ▶️. Passo a passo, com o que
aparece no log: [guia, Parte 1](backend/README_TESTES_E2E.md#parte-1--prepare-o-ambiente-pelo-intellij).

Pronto quando o log mostrar `Started RkfbankApplication` e http://localhost:8080/actuator/health responder
`{"groups":["liveness","readiness"],"status":"UP"}`. Todas as rotas, com os campos: http://localhost:8080/swagger-ui.html.

## 2. Contas de visitante

| | Ana Visitante (Origem) | Bruno Visitante (Destino) |
|---|---|---|
| CPF (login) | `246.813.579-28` | `975.318.642-82` |
| Senha | `Visitante@2026` | `Visitante@2026` |
| Conta (`id`) | `a0000000-0000-4000-8000-000000000114` | `a0000000-0000-4000-8000-000000000124` |
| Agência / número | `0001` / `900001` | `0001` / `900002` |
| Saldo ao subir | R$ 1.000,00 | R$ 500,00 |

Nomes, CPFs e endereços são fictícios. A senha é pública de propósito: só vale **dentro** do perfil `visitante`.

## 3. Cinco minutos de teste (Git Bash, Linux ou macOS)

```bash
API=http://localhost:8080; H='Content-Type: application/json'
ANA=a0000000-0000-4000-8000-000000000114; BRUNO=a0000000-0000-4000-8000-000000000124
TOKEN=$(curl -s -X POST $API/auth/login -H "$H" -d '{"cpf":"24681357928","senha":"Visitante@2026"}' | sed -E 's/.*"token":"([^"]+)".*/\1/')

# o login respondeu: {"token":"eyJ...","tipo":"Bearer","expiraEmSegundos":3600,"clienteId":"a0000000-...0112",
#                     "perfil":"CLIENTE","deveTrocarSenha":false,"refreshToken":"...","refreshExpiraEmSegundos":604800}

curl -s $API/contas -H "Authorization: Bearer $TOKEN"                     # saldo 1000.00
# [{"id":"a0000000-...0114","clienteId":"a0000000-...0112","agencia":"0001","numeroConta":"900001","tipoConta":"CORRENTE","saldo":1000.00,"ativa":true}]

CHAVE=$(node -e 'console.log(crypto.randomUUID())')                       # uma chave por operação
curl -s -i -X POST $API/transferencias -H "$H" -H "Authorization: Bearer $TOKEN" -H "Idempotency-Key: $CHAVE" \
  -d "{\"contaOrigemId\":\"$ANA\",\"contaDestinoId\":\"$BRUNO\",\"valor\":200.00,\"descricao\":\"Aluguel\"}" | head -1
# HTTP/1.1 204

# Mande DE NOVO com a mesma chave (como se a rede tivesse caído): o banco não debita duas vezes.
curl -s -i -X POST $API/transferencias -H "$H" -H "Authorization: Bearer $TOKEN" -H "Idempotency-Key: $CHAVE" \
  -d "{\"contaOrigemId\":\"$ANA\",\"contaDestinoId\":\"$BRUNO\",\"valor\":200.00,\"descricao\":\"Aluguel\"}" | grep -E "HTTP|Idempotency"
# HTTP/1.1 204
# Idempotency-Replayed: true

curl -s $API/contas -H "Authorization: Bearer $TOKEN"                     # saldo 800.00 (não 600.00)
curl -s $API/contas/$ANA/lancamentos -H "Authorization: Bearer $TOKEN"    # extrato
curl -s $API/contas/$BRUNO/lancamentos -H "Authorization: Bearer $TOKEN"  # 403: a conta é do Bruno
# {"status":403,"error":"Forbidden","codigo":"CONTA_DE_OUTRO_CLIENTE","message":"Conta não pertence ao usuário logado",...}
```

## 3½. Prefere clicar? Insomnia e o banco no navegador

- **Insomnia:** *Import* → arquivo [`backend/insomnia/rkfbank.insomnia.yaml`](backend/insomnia/rkfbank.insomnia.yaml)
  (Insomnia 10+; para o 9, o [`.json`](backend/insomnia/rkfbank.insomnia.json)) → ambiente **Visitante (H2, Ana e
  Bruno)** → mande as requisições de cima para baixo. O login guarda o token sozinho: nada de copiar e colar. O pedido
  e a resposta esperada de cada uma estão no [guia, Parte 2](backend/README_TESTES_E2E.md#parte-2--teste-como-cliente), cada uma com um **💡 Em palavras simples** do que ela faz.
- **Ver as tabelas:** abra `http://localhost:8080/h2-console`, troque a JDBC URL para `jdbc:h2:mem:rkfbank-visitante`
  (usuário `sa`, senha vazia) e rode `SELECT numero_conta, saldo FROM tb_conta;` antes e depois da transferência.

## 🎯 Missão completa: do seu cadastro ao extrato

No Insomnia, a pasta **02 · Fluxo E2E** faz o caminho inteiro de um cliente novo, na ordem. Marque conforme for:

- [ ] **Cadastro** com CPF, CEP e telefone **com máscara** (`529.982.247-25`, `06020-010`) e o **aceite dos termos** (`"aceiteTermos": true`): a cidade volta **Osasco**, direto do ViaCEP, e o CPF volta **mascarado** (`***.982.247-**`)
- [ ] **Login** com o CPF do cadastro
- [ ] **Conta ORIGEM** (corrente) e **conta DESTINO** (poupança), as duas com saldo `0.00`
- [ ] **Depósito** de 150,00, **saque** de 50,00 e **transferência** de 25,00 da origem para o destino
- [ ] **Saldos** 75,00 e 25,00, e os **extratos** com 3 e 1 lançamentos

Prefere o terminal? O mesmo roteiro, comando por comando, está no [README → API REST](README.md#-api-rest).

## 4. O que tentar depois (adivinhe a resposta antes de abrir)

<details>
<summary>🔓 Chamar <code>GET /contas</code> sem token</summary>

`401` com `"codigo": "AUTENTICACAO_NECESSARIA"`. Fora o cadastro, o login e o health, toda rota pede o token.
</details>

<details>
<summary>🎲 Errar a senha da Ana 5 vezes seguidas</summary>

A 6ª tentativa dá `429` `MUITAS_TENTATIVAS` com o header `Retry-After`, **mesmo com a senha certa** (proteção contra
força bruta; libera em 15 min ou reiniciando a API).
</details>

<details>
<summary>💸 Sacar mais do que tem</summary>

`422` com `"codigo": "SALDO_INSUFICIENTE"`, e o saldo não muda. O front decide pelo `codigo`; o `message` é só para
exibir.
</details>

<details>
<summary>🏦 Abrir uma poupança: <code>POST /contas</code> com <code>{"tipoConta":"POUPANCA"}</code></summary>

`201` com agência `0001` e número de 8 dígitos **gerado pelo banco** (o último é dígito verificador). A conta nasce no
cadastro de quem está logado: não existe id de cliente na URL para trocar.
</details>

<details>
<summary>✂️ Trocar uma letra do token</summary>

`401` `TOKEN_INVALIDO`: a assinatura não bate. Tente também o `node scripts/engenharia-reversa-jwt.mjs`, que tenta
forjar o token de vários jeitos.
</details>

Guia completo (o que é uma API e como ler cada resposta, rodar pela IDE com as VM options, o Swagger, todos os
payloads, os roteiros com cada resposta esperada e a explicação de Spring Security, OAuth2 e JWT):
[backend/README_TESTES_E2E.md](backend/README_TESTES_E2E.md).

## 🛡️ E o administrador?

O banco tem um perfil `ADMIN` (gestão de usuários e auditoria dos saldos), mas o perfil `visitante` **não tem ADMIN**,
de propósito: a senha dos visitantes é pública, e um ADMIN com senha pública seria um ADMIN de qualquer um. Os testes
de ADMIN são feitos pela equipe. Daqui você vê o outro lado: a Ana tentando entrar numa rota de ADMIN recebe `403`
(pasta **07 · Erros** da coleção). Por isso a coleção pula da pasta `04` para a `06`.

## 5. O que este perfil garante (e por quê)

- **Nada fica gravado:** banco H2 em memória; parou a API, sumiu tudo. Na próxima subida, saldos iniciais de novo.
- **Ninguém forja token:** o segredo do JWT é sorteado a cada subida e não está escrito em lugar nenhum.
- **Não existe admin nem conta da equipe:** só a Ana e o Bruno. Seu `.env`, se tiver um, é ignorado para segredos.
- **Não use em servidor:** é para a sua máquina. Para colaborar no projeto, veja o [CONTRIBUTING.md](CONTRIBUTING.md).

Encontrou um bug ou tem sugestão? Abra uma [issue](https://github.com/k3vinrich4rd/rkf_bank/issues). 🧡
