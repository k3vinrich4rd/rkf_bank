---
name: testar-e2e
description: Sobe a API do RKF Bank contra o MySQL do Docker e roda o smoke test ponta a ponta (cadastro com ViaCEP, contas, depósito, saque, transferência, extrato e cenários de erro), interpretando qualquer falha. Use quando o usuário pedir "/testar-e2e", "testa o fluxo completo", ou depois de mudar regra de negócio, rota, banco ou dependências do servidor.
---

# /testar-e2e

## 1. Ambiente
- `docker compose ps`: o `rkfbank-mysql` precisa estar `healthy`. Se não estiver, `docker compose up -d` e espere.
- Se a porta 8080 já responder, pergunte se pode usar a API que está no ar (pode ser de outra branch).
- Sem Docker? `--spring.profiles.active=equipe` (H2 + contas da equipe; segredos do `.env`) ou `visitante` (H2 + contas públicas Ana e Bruno, sem admin).
- Caso contrário, suba a API em segundo plano com as variáveis do `.env` carregadas:
  `set -a && . ./.env && set +a && ./mvnw -q -DskipTests package && java -jar target/rkfbank-0.0.1-SNAPSHOT.jar`
  e espere `GET /clientes` responder.

## 2. Rodar (script, não IA)
`node scripts/smoke-e2e.mjs` (ou `node scripts/smoke-e2e.mjs <baseUrl>`). Ele cria dados novos a cada execução, então não precisa apagar o banco.

O smoke já confere as contas do seed: no `visitante`, a Ana; no padrão/`equipe`, Fellipe e Renan **se** `EQUIPE_SENHA` estiver no ambiente do terminal (`set -a && source <(tr -d '\r' < .env) && set +a`). Esperado: 32/32 no `visitante`, 35/35 no `equipe` com o `.env` carregado. Nunca escreva a `EQUIPE_SENHA` na resposta ao usuário.

Depois, a coleção do Insomnia (cobre ADMIN, LGPD e o roteiro E2E da pasta 02, que o smoke não cobre): `node scripts/rodar-colecao-insomnia.mjs` (visitante: 83/83, pula o ADMIN) ou, no `equipe`/padrão, `node scripts/rodar-colecao-insomnia.mjs --ambiente equipe --arquivo .equipe/testes/rkfbank-equipe.insomnia.json` (121/121; se o arquivo não existir, gere com `node scripts/gerar-colecao-insomnia.mjs --equipe`).

## 3. Interpretar
- Tudo ✅: informe `X/Y verificações OK` em uma linha.
- Alguma ❌: para cada falha, leia o trecho do log da API pelo `X-Request-Id` que começa com `smoke-e2e-`, explique a causa em linguagem simples e indique o arquivo provável (`arquivo:linha`). Corrija só se o usuário pedir.
- Falha de cadastro com `503`: o ViaCEP pode estar fora ou sem internet; não é bug do RKF Bank.

## 4. Encerrar
Pare a API que você subiu (no Windows: `Get-NetTCPConnection -LocalPort 8080` → `Stop-Process`). Não derrube o MySQL.
Se o resultado mudou algo do roteiro manual, sugira atualizar `backend/README_TESTES_E2E.md` (guia passo a passo) e, se mudou rota ou payload, `scripts/gerar-colecao-insomnia.mjs`.
