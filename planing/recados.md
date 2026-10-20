# Recados

Escreva seu recado no topo. Quando resolver, marque `[x]` e escreva a resposta embaixo.

**Modelo:** `- [ ] De → Para (data): mensagem`

---

- [ ] Kevin → Renan (20/10): o contrato **2.1** (`backend/README_CONTRATO_API.md` → "Idempotência" e 4.1) mexeu no frontend, e eu já fiz o ajuste mínimo no mesmo PR para nada quebrar — revisa quando puder:
  1. **Depósito, saque e transferência agora exigem o header `Idempotency-Key`** (sem ele → `400`). `contasApi.depositar/sacar/transferir` (`frontend/lib/api/contas.ts`) recebem a chave como último parâmetro; o `use-movimentacao.ts` gera `crypto.randomUUID()` ao enviar e **reusa a mesma** se o usuário clicar de novo depois de erro de rede (troca quando ele edita um campo ou dá certo). Repetição devolve `204` com `Idempotency-Replayed: true`: se quiser, dá para mostrar "essa operação já tinha sido feita".
  2. **Abrir conta sem agência e número:** o banco gera (`0001` + 8 dígitos com DV). Apaguei o sorteio e o retry do `use-abrir-conta.ts`; `AbrirContaRequest` ficou com os dois campos opcionais em `types.ts`.
  3. **Login pode dar `429`** depois de 5 senhas erradas para o mesmo CPF (15 min, header `Retry-After`). Hoje a tela de login cai na mensagem genérica; vale uma mensagem própria.
  4. Os acessos de teste do Felipe e do seu usuário (Renan, conta destino) saíram dos READMEs: estão no guia privado da equipe (`.equipe/GUIA_TESTES_EQUIPE.md`, fora do Git) que eu te mando por DM, junto com a `EQUIPE_SENHA` para o seu `.env`. Sem Docker: `./mvnw spring-boot:run -Dspring-boot.run.profiles=equipe`.
  `scripts/validar.sh frontend` passou (lint, typecheck e build).
- [x] Kevin → Renan (16/10): a `feature/spring-security-jwt` **quebra o frontend** quando entrar na `develop` (contrato **2.0**, `backend/README_CONTRATO_API.md` → "Autenticação (JWT)" e seção 3.3). O que muda para você:
  1. **Cadastro** (`POST /clientes`) agora exige `senha` (8 a 72 caracteres). A resposta continua igual e não traz token.
  2. **Login novo:** `POST /auth/login` com `{cpf, senha}` devolve `{token, tipo: "Bearer", expiraEmSegundos, clienteId, perfil}`. O `clienteId` é o que vai em `POST /contas/clientes/{clienteId}`.
  3. **Todas as outras rotas** precisam de `Authorization: Bearer <token>` (dá para colocar uma vez no `apiRequest` de `lib/api/client.ts`). Sem token ou token vencido (15 min) → `401` no formato padrão: mandar para a tela de login.
  4. **Regra de dono:** conta de outro cliente → `403`. `GET /contas` com token de cliente já devolve **só as contas dele** (o `procurarNaListagem` continua funcionando).
  5. `GET /clientes` virou **só ADMIN**: o `clientesApi.listar` vai receber `403` com token de cliente.
  6. Para rodar a API local, o `.env` precisa de `JWT_SEGREDO` (ver `.env.example`). Recrie o MySQL local (`docker compose down -v`): clientes antigos não têm senha.
  Atualizar `frontend/lib/api/types.ts` com `senha` no cadastro e os tipos do login. Me diz se prefere outro formato de resposta antes do merge.
  - Resposta (17/10): o formato do login está ótimo. O frontend do contrato 2.0 entrou com a `feature/login-frontend`: senha no cadastro com login automático, tela de login, `Bearer` em todas as chamadas, `401` levando ao login e `GET /contas` como "minhas contas" (o `procurarNaListagem` foi removido). O token fica num cookie `httpOnly` pelo servidor do Next (ver `decisoes.md`); para a API nada muda, ela continua recebendo `Authorization: Bearer`.
- [ ] Kevin → Renan (15/10): depois que a `feature/flyway` entrar na `develop`, recrie o MySQL local **uma vez**: `docker compose down -v && docker compose up -d` (apaga os dados locais). O banco antigo foi criado pelo Hibernate e, sem isso, a API não sobe (*Found non-empty schema... but no schema history table*). Detalhes em `backend/README.md` → "Migrações (Flyway)". Nada muda no frontend.
- [ ] Renan → Kevin (13/10): revisei toda a documentação na branch `docs/revisao-documentacao`. Dá uma olhada principalmente no `backend/README.md` e no `planing/debitos-tecnicos.md` e me diz se concorda.
- [ ] Renan → Kevin (04/10): o `pom.xml` está na raiz, ele seria um arquivo do `backend/`?
  - Situação atual: funciona, porque o `pom.xml` aponta para `backend/src`. Os comandos Maven rodam na raiz. Mover para `backend/` está listado em `debitos-tecnicos.md`.
- [x] Renan → Kevin (04/10): no `ClienteResponseDto`, o campo `enderecoResponseDto` vai aparecer com esse nome no JSON. O contrato diz `endereco`.
  - Resolvido: o campo hoje se chama `endereco`, igual ao contrato.
