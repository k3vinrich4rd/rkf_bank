# Decisões

Quando o time decidir algo importante, anote aqui. Assim ninguém precisa lembrar "por que fizemos assim".

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
Decisão: backend em Java + Spring Boot; frontend em Next.js + TypeScript.
Por quê: é a stack que o time quer aprender e usar no mercado.

## Banco de dados: MySQL
Data: 02/10
Decisão: usar MySQL 8.4 rodando no Docker (e não PostgreSQL, como pensado no começo).
Por quê: foi o banco usado na implementação do backend. Trocar agora atrasaria o MVP.

## Contrato da API
Data: 02/10
Decisão: o arquivo `backend/README_CONTRATO_API.md` é a regra entre o backend e o frontend.
Por quê: assim um lado não fica adivinhando o que o outro faz. Se mudar um endpoint, atualize o arquivo no mesmo PR.

## Rotas da API seguem o backend atual
Data: 11/10
Decisão: as rotas não têm prefixo `/api`. Abrir conta é `POST /contas/clientes/{clienteId}` e transferir é `POST /contas/transferencia`.
Por quê: o backend já estava pronto e testado assim; ajustar o frontend e o contrato era mais barato perto do prazo.

## Frontend chama o backend por um proxy
Data: 11/10
Decisão: o navegador chama `/api/...` no próprio Next, que repassa para o backend.
Por quê: evita problemas de CORS e não expõe o endereço do backend.

## Logs do frontend com dados mascarados
Data: 12/10
Decisão: todo log do frontend passa por `lib/logger`, que esconde CPF, e-mail, telefone e nome.
Por quê: ajuda a investigar erros sem expor dados pessoais (LGPD).

---

## Ainda falta decidir

- [ ] **Acesso à conta sem login:** depois do cadastro, como o usuário volta para a conta dele? (prazo: 15/10)
- [ ] **Dinheiro:** a API manda o valor em reais (`50.00`) ou em centavos (`5000`)? Hoje a API usa reais e o frontend converte para centavos.
- [ ] **Login:** fazer no MVP ou depois?
- [ ] **Onde publicar:** onde o site, a API e o banco vão ficar no ar?
