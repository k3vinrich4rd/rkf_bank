# Recados

Escreva seu recado no topo. Quando resolver, marque `[x]` e escreva a resposta embaixo.

**Modelo:** `- [ ] De → Para (data): mensagem`

---

- [ ] Renan → Kevin (17/10): sobre o seu recado da `feature/spring-security-jwt`: o formato do login está ótimo, não precisa mudar nada. O frontend do contrato 2.0 está pronto na `feature/login-frontend` (empilhada sobre a `feature/tela-movimentacoes`): senha no cadastro com login automático, tela de login, `Bearer` em todas as chamadas, `401` levando ao login e `GET /contas` usado como "minhas contas". O token fica num cookie `httpOnly` pelo servidor do Next (ver `decisoes.md`); para a API nada muda, ela continua recebendo `Authorization: Bearer`. **Ordem de merge:** a sua `spring-security-jwt` antes ou junto com a minha, porque sem o `/auth/login` o frontend não consegue entrar. Testei contra um mock do contrato 2.0; quando der, rodamos juntos contra a API real.
- [ ] Kevin → Renan (15/10): depois que a `feature/flyway` entrar na `develop`, recrie o MySQL local **uma vez**: `docker compose down -v && docker compose up -d` (apaga os dados locais). O banco antigo foi criado pelo Hibernate e, sem isso, a API não sobe (*Found non-empty schema... but no schema history table*). Detalhes em `backend/README.md` → "Migrações (Flyway)". Nada muda no frontend.
- [ ] Renan → Kevin (13/10): revisei toda a documentação na branch `docs/revisao-documentacao`. Dá uma olhada principalmente no `backend/README.md` e no `planing/debitos-tecnicos.md` e me diz se concorda.
- [ ] Renan → Kevin (04/10): o `pom.xml` está na raiz, ele seria um arquivo do `backend/`?
  - Situação atual: funciona, porque o `pom.xml` aponta para `backend/src`. Os comandos Maven rodam na raiz. Mover para `backend/` está listado em `debitos-tecnicos.md`.
- [x] Renan → Kevin (04/10): no `ClienteResponseDto`, o campo `enderecoResponseDto` vai aparecer com esse nome no JSON. O contrato diz `endereco`.
  - Resolvido: o campo hoje se chama `endereco`, igual ao contrato.
