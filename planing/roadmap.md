# Roadmap

**Meta:** MVP no ar até **31/10/2026**.

Marque `[x]` quando o PR for aceito na `develop`.

## O que é o MVP

O cliente consegue:

- se cadastrar (com endereço buscado pelo CEP)
- abrir uma conta
- depositar, sacar e transferir
- ver saldo e extrato

## Como estamos (19/10)

- **Backend:** tudo do MVP pronto e testado (261 testes), com Flyway, login JWT, idempotência nas movimentações, gestão de usuários e troca de senha. Falta buscar uma conta específica.
- **Frontend:** landing page, cadastro, login, abertura de conta, tela da conta, movimentações e extrato prontos na `develop` (PR #27).
- **Antes de ir ao ar:** LGPD mínimo (política de privacidade, aceite dos termos e encarregado) — ver `debitos-tecnicos.md`.

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
- [ ] Backend: `GET /contas/{contaId}` (saldo)
- [ ] Backend: `GET /clientes/{clienteId}/contas`
- [x] Frontend: abrir conta depois do cadastro
- [x] Frontend: tela da conta (saldo)

## Semana 3 (19/10 a 25/10) — Movimentações

- [x] Backend: depósito, saque e transferência
- [x] Backend: extrato
- [x] Frontend: telas de depósito, saque e transferência
- [x] Frontend: tela de extrato
- [ ] Decidir onde publicar o projeto

## Semana 4 (26/10 a 31/10) — Testar e lançar

- [ ] LGPD mínimo: política de privacidade, aceite dos termos (migração nova) e encarregado definido
- [ ] Felipe (idealizador e primeiro cliente) testa tudo (roteiro em `backend/README_TESTES_E2E.md`)
- [ ] Corrigir bugs
- [ ] Colocar no ar
- [ ] Atualizar o README com link e prints

## Depois do MVP

- Login: feito no backend (PR #24) e no frontend (PR #27)
- Testcontainers e Swagger (features 3 e 4 do checklist)
- Pix
- Cartão

Melhorias técnicas que já sabemos que precisam ser feitas: [debitos-tecnicos.md](debitos-tecnicos.md).
