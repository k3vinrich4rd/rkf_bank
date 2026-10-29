# Roadmap

**Meta:** MVP no ar até **31/10/2026**.

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
- **Antes de ir ao ar:** decidir onde publicar (proposta em `backend/README_DEPLOY.md`), e o teste do Fellipe. Encarregados LGPD: Kevin e Renan (26/10). Perfil `prod` e `Dockerfile` prontos (26/10). Análise completa em [analise-mvp.md](analise-mvp.md).

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
- [ ] Decidir onde publicar o projeto

## Semana 4 (26/10 a 31/10) — Testar e lançar

- [x] LGPD mínimo: política de privacidade e aceite dos termos (V10, contrato 3.1)
- [x] LGPD: encarregados Kevin e Renan, publicados na política (26/10)
- [ ] Fellipe (idealizador e primeiro cliente) testa tudo (roteiros em `.equipe/testes/`: `BACKEND_E2E.md` e `FRONTEND_E2E.md`)
- [ ] Corrigir bugs
- [ ] Colocar no ar
- [ ] Atualizar o README com link e prints

## Depois do MVP

- Pix
- Cartão

Melhorias técnicas que já sabemos que precisam ser feitas: [debitos-tecnicos.md](debitos-tecnicos.md).
