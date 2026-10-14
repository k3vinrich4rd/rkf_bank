# Débitos técnicos

Débito técnico é algo que funciona hoje, mas que vai dar trabalho ou problema no futuro.
Anotar aqui não é reclamar do código: é não esquecer.

**Prioridade:** 🔴 resolver antes do MVP · 🟡 logo depois do MVP · 🟢 quando der

Resolveu um item? Apague a linha no mesmo PR.

---

## Backend

| Prioridade | Problema | Sugestão |
|:---:|---|---|
| 🔴 | O Spring não lê o `.env`. Se as variáveis não estiverem no terminal, ele usa usuário/senha `rkfbank`, que não batem com o Docker. | Adicionar `spring.config.import=optional:file:.env[.properties]` no `application.properties`. |
| 🔴 | O teste `RkfbankApplicationTests` precisa do MySQL ligado para rodar. | Rodar esse teste com o H2 (perfil de teste). |
| 🔴 | O arquivo `application-test.properties` está em `src/main/resources`. | Mover para `src/test/resources`. |
| 🟡 | Não existe login: qualquer pessoa pode ver ou movimentar qualquer conta. | Spring Security + JWT. |
| 🟡 | `GET /clientes` devolve CPF, e-mail e telefone de todos os clientes. | Proteger com login ou remover do uso público. |
| 🟡 | O banco é criado pelo Hibernate (`ddl-auto=update`). | Usar Flyway (scripts de banco versionados). |
| 🟡 | A configuração de CORS vale para `/api/**`, mas as rotas não têm `/api`. Hoje funciona só por causa do proxy do Next. | Ajustar para `/**` ou remover, se o proxy for mantido. |
| 🟡 | Quem chama a API escolhe a agência e o número da conta. | O backend gerar esses números. |
| 🟡 | A paginação devolve o `Page` do Spring direto, e esse formato pode mudar entre versões. | Criar um DTO de página próprio. |
| 🟢 | O `pom.xml` fica na raiz, mas o código fica em `backend/`. | Mover `pom.xml` e `mvnw` para `backend/`. |
| 🟢 | E-mail repetido com letras maiúsculas diferentes passa como novo (`Joao@x.com` × `joao@x.com`). | Salvar o e-mail em minúsculas. |
| 🟢 | O enum de lançamento tem `CREDITO` e `DEBITO`, que não são usados. | Remover ou usar. |
| 🟢 | Mensagens de erro misturam texto com e sem acento ("nao", "não"). | Padronizar. |
| 🟢 | `show-sql=true` deixa o console poluído. | Ligar só em desenvolvimento. |
| 🟢 | Os testes de controller chamam o método direto, sem testar o JSON de verdade. | Usar `MockMvc`. |

## Frontend

| Prioridade | Problema | Sugestão |
|:---:|---|---|
| 🟡 | Não há testes automáticos no frontend. | Vitest + Testing Library. |
| 🟢 | `components/balance-summary.tsx` e `types/index.ts` (`Account`) não são usados. | Apagar ou reaproveitar na tela de saldo. |

## Projeto

| Prioridade | Problema | Sugestão |
|:---:|---|---|
| 🟡 | O frontend não tem CI (o backend já tem: build, testes, CodeQL e revisão de dependências). | Workflow rodando `lint`, `typecheck` e `build` do frontend nos PRs. |
| 🟢 | A pasta `planing` está escrita errado (o certo é `planning`). | Renomear quando ninguém estiver mexendo nela. |
