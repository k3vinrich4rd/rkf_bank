# Roadmap

**Meta:** MVP no ar até **31/10/2026**.

Marque `[x]` quando o PR for aceito na `develop`.

## O que é o MVP

O cliente consegue:

- se cadastrar (com endereço buscado pelo CEP)
- abrir uma conta
- depositar, sacar e transferir
- ver saldo e extrato

## Como estamos (13/10)

- **Backend:** quase tudo pronto e testado. Falta buscar uma conta específica.
- **Frontend:** landing page e cadastro prontos. Faltam as telas da conta.
- **Login (16/10):** backend com CPF + senha e JWT pronto na `feature/spring-security-jwt`; falta a tela de login no frontend (recado para o Renan).

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
- [ ] Frontend: abrir conta depois do cadastro
- [ ] Frontend: tela da conta (saldo)

## Semana 3 (19/10 a 25/10) — Movimentações

- [x] Backend: depósito, saque e transferência
- [x] Backend: extrato
- [ ] Frontend: telas de depósito, saque e transferência
- [ ] Frontend: tela de extrato
- [ ] Decidir onde publicar o projeto

## Semana 4 (26/10 a 31/10) — Testar e lançar

- [ ] Fellipe testa tudo (roteiro em `backend/README_TESTES_E2E.md`)
- [ ] Corrigir bugs
- [ ] Colocar no ar
- [ ] Atualizar o README com link e prints

## Depois do MVP

- Login: backend feito (feature 2); falta o frontend
- Testes automáticos rodando em todo PR (CI)
- Pix
- Cartão

Melhorias técnicas que já sabemos que precisam ser feitas: [debitos-tecnicos.md](debitos-tecnicos.md).
