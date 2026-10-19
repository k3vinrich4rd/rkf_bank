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

## Banco versionado com Flyway
Data: 15/10
Decisão: o schema passa a ser criado e alterado só por migrações do Flyway (`backend/src/main/resources/db/migration`); o Hibernate fica em `ddl-auto=validate`. Bancos locais antigos são recriados uma vez (`docker compose down -v`), sem *baseline*. UUID fixado como `BINARY(16)` no Hibernate e e-mail salvo em minúsculas (`V2`).
Por quê: histórico do banco no Git, revisado em PR e igual em todo ambiente. Sem produção ainda, recriar é mais seguro que o *baseline*, que esconderia as diferenças do schema antigo (FKs com nome aleatório, sem índices). O `BINARY` e o SQL portável fazem o H2 dos testes validar o mesmo schema do MySQL.

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

## Login com CPF + senha e JWT
Data: 16/10
Decisão: o cliente entra com **CPF + senha** e recebe um **JWT** (HS256, expira em 15 min por padrão, sem refresh token por enquanto). A API valida o token com o **OAuth2 Resource Server** do Spring (não com filtro próprio). Senha e perfil (`CLIENTE`/`ADMIN`) ficam em `tb_usuario` (V3), separada de `tb_cliente`, com a senha em **BCrypt**. O primeiro `ADMIN` é criado na subida a partir de `ADMIN_CPF`/`ADMIN_SENHA` do `.env`. Públicas só `POST /clientes` e `POST /auth/login`; `GET /clientes` é só do ADMIN; em `GET /contas` o ADMIN vê todas e o CLIENTE só as dele; movimentar, ver extrato e abrir conta só nas contas do próprio cliente (`403`).
Por quê: CPF já é único e validado no cadastro. O Resource Server é a forma oficial e testada de validar JWT (menos código nosso para errar). Tabela separada deixa credencial longe do dado cadastral e permite admin que não é cliente. Admin pelo `.env` evita senha em migração ou no Git. Também responde ao "acesso à conta sem login": agora o usuário volta pelo login.

---

## Onde o frontend guarda o token do login
Data: 17/10
Decisão: o token fica num **cookie `httpOnly`** gravado pelo servidor do Next (padrão BFF, *Backend for Frontend*). O login passa por `POST /api/auth/login` (Route Handler do Next), que chama a API, guarda o token no cookie `rkf_token` e devolve ao navegador só `{clienteId, perfil, expiraEm}`. Nas outras chamadas, o `proxy.ts` lê o cookie e envia `Authorization: Bearer` para a API. Cookies com `SameSite=Lax`, `Secure` em produção e validade igual à do token. `401` no meio do uso leva ao login (`?expirou=1`) e depois de volta para a tela.
Por quê: com `localStorage`/`sessionStorage`, qualquer script que rode na página (um XSS, uma dependência comprometida) lê e rouba o token; com `httpOnly`, o JavaScript não alcança o cookie. O proxy `/api` já existia, então o custo foi pequeno. `SameSite=Lax` evita que outro site faça POST com o cookie (CSRF). Alternativa descartada: `sessionStorage` (mais simples, mas exposta a XSS).

## Papéis do time
Data: 19/10
Decisão: **Fellipe Santana** é o idealizador do RKF Bank, PO & QA e o **primeiro cliente**: tem acesso antes de todo mundo para os primeiros testes. Kevin cuida do backend e Renan do frontend. O time inteiro é de Osasco, SP.
Por quê: deixa claro quem decide prioridade de produto e quem dá o aceite do MVP.

## LGPD: mínimo antes do deploy, resto depois do MVP
Data: 19/10
Decisão: o roadmap técnico segue (Testcontainers → Swagger). O "LGPD mínimo" (política de privacidade, registro do aceite dos termos e encarregado) **bloqueia o "Colocar no ar"**, como o login bloqueou. Direitos do titular (ver, corrigir, encerrar) entram depois do MVP, com contrato combinado com o frontend.
Por quê: hoje só há dados de teste no ambiente local, então o risco real começa no deploy. Os itens mínimos são pequenos e não competem com as features; os direitos do titular são features de produto que precisam de tela.

---

## Ainda falta decidir

- [x] **Acesso à conta sem login:** resolvido com o login (ver "Login com CPF + senha e JWT").
- [ ] **Dinheiro:** a API manda o valor em reais (`50.00`) ou em centavos (`5000`)? Hoje a API usa reais e o frontend converte para centavos.
- [x] **Login:** feito (feature 2 do checklist).
- [ ] **Onde publicar:** onde o site, a API e o banco vão ficar no ar?
