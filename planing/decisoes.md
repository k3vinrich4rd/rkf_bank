# Decisões

Quando o time decidir algo importante, anote aqui.

**Modelo:**

```
## Título
Data:
Decisão:
Por quê:
```

---

## Stack do projeto
Data: 25/09
Decisão: backend em Java + Spring Boot e PostgreSQL; frontend em Next.js + TypeScript.
Por quê: é a stack que o time quer aprender e usar no mercado.

## Contrato da API
Data: 02/10
Decisão: o arquivo `backend/README_CONTRATO_API.md` é a regra entre o backend e o frontend.
Por quê: assim um lado não fica adivinhando o que o outro faz. Se mudar um endpoint, atualize o arquivo.

## Rotas da API seguem o backend atual
Data: 11/10
Decisão: o contrato passa a usar as rotas que o backend já expõe — sem prefixo `/api`, abertura de conta em `POST /contas/clientes/{clienteId}` e transferência em `POST /contas/transferencia`.
Por quê: o backend já estava pronto e testado assim; ajustar o frontend e o contrato é mais barato perto do prazo do MVP.

---

## Ainda falta decidir

- [ ] **Dinheiro:** a API manda o valor como texto (`"50.00"`) ou em centavos (`5000`)? Hoje cada parte faz de um jeito.
- [ ] **Login:** Fazer area de login para o banco digital.
- [ ] **Onde publicar:** onde o site e a API vão ficar no ar?
