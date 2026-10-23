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
Pronto quando o log mostrar `Tomcat started on port 8080`.

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

curl -s $API/contas -H "Authorization: Bearer $TOKEN"                     # saldo 1000.00

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
curl -s -i $API/contas/$BRUNO/lancamentos -H "Authorization: Bearer $TOKEN" | head -1   # 403: a conta é do Bruno
```

## 3½. Prefere clicar? Insomnia e o banco no navegador

- **Insomnia:** *Import* → arquivo [`backend/insomnia/rkfbank.insomnia.json`](backend/insomnia/rkfbank.insomnia.json) →
  ambiente **Visitante (H2, Ana e Bruno)** → mande as requisições de cima para baixo. O login guarda o token sozinho:
  nada de copiar e colar.
- **Ver as tabelas:** abra `http://localhost:8080/h2-console`, troque a JDBC URL para `jdbc:h2:mem:rkfbank-visitante`
  (usuário `sa`, senha vazia) e rode `SELECT numero_conta, saldo FROM tb_conta;` antes e depois da transferência.

## 4. O que tentar depois

| Experimento | O que deve acontecer |
|---|---|
| Chamar `GET /contas` sem token | `401` |
| Errar a senha da Ana 5 vezes seguidas | a 6ª tentativa dá `429` (proteção contra força bruta; libera em 15 min ou reiniciando) |
| Sacar mais do que tem | `422 Saldo insuficiente` |
| Abrir uma poupança: `POST /contas` com `{"tipoConta":"POUPANCA"}` (a conta nasce no cadastro de quem está logado) | `201` com número de conta **gerado pelo banco** |
| Trocar uma letra do token | `401`: a assinatura não bate |

Guia completo (rodar pela IDE, H2 ou MySQL, permissões de cada rota, como tornar alguém ADMIN, todos os payloads, os
roteiros com cada resposta esperada e a explicação de Spring Security, OAuth2 e JWT):
[backend/README_TESTES_E2E.md](backend/README_TESTES_E2E.md).

## 5. O que este perfil garante (e por quê)

- **Nada fica gravado:** banco H2 em memória; parou a API, sumiu tudo. Na próxima subida, saldos iniciais de novo.
- **Ninguém forja token:** o segredo do JWT é sorteado a cada subida e não está escrito em lugar nenhum.
- **Não existe admin nem conta da equipe:** só a Ana e o Bruno. Seu `.env`, se tiver um, é ignorado para segredos.
- **Não use em servidor:** é para a sua máquina. Para colaborar no projeto, veja o [CONTRIBUTING.md](CONTRIBUTING.md).

Encontrou um bug ou tem sugestão? Abra uma [issue](https://github.com/k3vinrich4rd/rkf_bank/issues). 🧡
