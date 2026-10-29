# Roadmap

**MVP concluído em 29/10/2026** (PR #76 na `main`). O RKF Bank é um **projeto de portfólio**: roda na máquina de quem
avalia (`VISITANTES.md`, 1 comando) e **não tem fase de deploy**. A imagem Docker e o perfil `prod` seguem testados no CI.

Marque `[x]` quando o PR for aceito na `develop`.

## O que é o MVP

O cliente consegue:

- se cadastrar (com endereço buscado pelo CEP)
- abrir uma conta
- depositar, sacar e transferir
- ver saldo e extrato

## Como estamos (23/10)

- **Backend:** tudo do MVP pronto e testado (307 testes, 7 no MySQL real com Testcontainers), com refresh token, aceite LGPD, `GET /contas/{contaId}` e Swagger.
- **Frontend:** todas as telas do MVP, renovação de sessão, aceite dos termos, seletor de conta e 31 testes (Vitest) com CI próprio.
- **Fechamento (29/10):** MVP na `main` (PR #76). Sem deploy: o projeto é de portfólio. Encarregados LGPD: Kevin e Renan (26/10). Perfil `prod` e `Dockerfile` testados no CI. Análise em [analise-mvp.md](analise-mvp.md).

---

## Semana 1 (05/10 a 11/10) — Arrumar a base ✅

- [x] Backend compilando e rodando
- [x] Tirar o `.env` do git
- [x] Responder as dúvidas do `decisoes.md`

## Semana 2 (12/10 a 18/10) — Cliente e conta

- [x] Backend: cadastrar cliente + ViaCEP
- [x] Backend: abrir conta
- [x] Frontend: tela de cadastro
- [x] Frontend: logs
- [x] Decidir como o usuário acessa a conta sem login (resolvido com o login: feature 2)
- [x] Backend: `GET /contas/{contaId}` (saldo)
- [x] ~~Backend: `GET /clientes/{clienteId}/contas`~~ substituída por `GET /contas` para o cliente (o cliente vem do token, contrato 2.6); volta **só para o ADMIN** no contrato 3.2 (27/10)
- [x] Frontend: abrir conta depois do cadastro
- [x] Frontend: tela da conta (saldo)

## Semana 3 (19/10 a 25/10) — Movimentações

- [x] Backend: depósito, saque e transferência
- [x] Backend: extrato
- [x] Frontend: telas de depósito, saque e transferência
- [x] Frontend: tela de extrato
- [x] ~~Decidir onde publicar o projeto~~ → sem deploy: projeto de portfólio (29/10)

## Semana 4 (26/10 a 31/10) — Testar e lançar

- [x] LGPD mínimo: política de privacidade e aceite dos termos (V10, contrato 3.1)
- [x] LGPD: encarregados Kevin e Renan, publicados na política (26/10)
- [x] Roteiros de aceite prontos para o Fellipe (`.equipe/testes/`: `BACKEND_E2E.md` e `FRONTEND_E2E.md`) e para quem avalia (`VISITANTES.md`)
- [x] Release na `main` (PR #76) — MVP concluído (29/10)
- [x] ~~Colocar no ar~~ → fora do escopo: o projeto é de portfólio e roda localmente

## Depois do MVP

- Pix
- Cartão

Melhorias técnicas que já sabemos que precisam ser feitas: [debitos-tecnicos.md](debitos-tecnicos.md).
