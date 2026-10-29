# RKF Bank — contexto para agentes de IA

Banco digital full stack idealizado por três amigos de Osasco, SP. **Backend:** Kevin. **Frontend:** Renan. **PO & QA e primeiro cliente:** Fellipe Santana.
Este arquivo vale para o projeto inteiro; o frontend tem regras próprias em [frontend/CLAUDE.md](frontend/CLAUDE.md).

## Premissas do time

- **Idioma:** sempre português. Termos técnicos em inglês vêm traduzidos na primeira vez (`rollback` = desfazer a transação).
- **Ensinar enquanto faz:** quem pede uma feature quer aprender. Explique o **conceito** (o que é, por que existe, como funciona por dentro) e o **porquê de cada decisão**, em pedaços curtos entre os passos. Sem quiz e sem pedir que a pessoa prove que entendeu antes de implementar.
- **Escopo:** mexa só na parte pedida. Backend é do Kevin, frontend é do Renan: mudança no outro lado só quando pedida, e sempre avisando.
- **Testes 100% verdadeiros:** nunca afrouxe ou apague um teste para ele passar. Teste que falha é informação.
- **Barato e eficiente:** script determinístico antes de IA. Use `scripts/validar.sh` em vez de ler o log inteiro do Maven.

## Estrutura

| Pasta | O que tem |
|---|---|
| `backend/src/main/java/com/br/rkfbank` | API Spring Boot 4.1 / Java 25, **monólito modular**: um pacote por assunto (`clientes`, `contas`, `autenticacao`, `titular`) com controller → service → repository, entidades e `dto/` de cada um, e o `compartilhado` (`config`, `erros`, `web`, `validacao`) |
| `backend/src/test/java/...` | JUnit 5 + Mockito (services, controllers), `@DataJpaTest` (repositories) e o `ArquiteturaModulosTest` (ArchUnit: as fronteiras dos módulos) |
| `backend/*.md` | contrato da API, arquitetura, segurança (`README_SEGURANCA.md`), roteiro e checklist E2E |
| `frontend/` | Next.js 16 + React 19 + TypeScript + Tailwind 4, pnpm (ver `frontend/CLAUDE.md`) |
| `planing/` | roadmap, decisões, débitos técnicos, recados e o **checklist de features** |
| `scripts/` | validação, smoke test E2E, varredura de CVEs e hooks de Git |
| `.claude/skills/` | skills do projeto (lista abaixo) |

`pom.xml` e `mvnw` ficam na **raiz** (apontam para `backend/src`).

## Comandos

```bash
scripts/validar.sh backend          # testes do backend em H2, resumo curto (não precisa de Docker)
scripts/validar.sh frontend         # lint + typecheck + build do frontend
scripts/validar.sh tudo             # os dois
./run-test.sh                       # ⚠️ APAGA o MySQL local (docker compose down -v) e roda tudo com o perfil test
scripts/testar-no-mysql.sh          # testes @SpringBootTest contra o MySQL do Docker (os outros scripts usam H2); não apaga dados
./mvnw spring-boot:run -Dspring-boot.run.profiles=visitante  # API pública sem Docker e sem .env (H2 + Ana e Bruno Visitante, JWT sorteado)
./mvnw spring-boot:run -Dspring-boot.run.profiles=equipe     # equipe sem Docker (H2 + Fellipe e Renan; segredos do .env)
node scripts/smoke-e2e.mjs          # fluxo real contra a API rodando em http://localhost:8080
node scripts/rodar-colecao-insomnia.mjs  # roda a coleção do Insomnia (backend/insomnia) contra a API: 83/83 visitante, 121/121 equipe (a pública não tem o ADMIN)
node scripts/gerar-colecao-insomnia.mjs  # regera a coleção do Insomnia (mudou rota ou payload? mude aqui); --equipe gera a privada em .equipe/ com o .env padrão do guia da equipe
node scripts/gerar-roteiro-e2e.mjs       # passos dos guias de teste a partir de uma execução real (--registrar); --passos gera a trilha curta (VISITANTES.md, .equipe/testes/BACKEND_E2E.md)
node scripts/engenharia-reversa-jwt.mjs  # abre o JWT e tenta forjá-lo (alg none, payload adulterado...): todos têm que dar 401
node scripts/e2e-site.mjs           # site (Next, porta 3000) + API visitante juntos: páginas, login pelo site e conta pelo proxy
docker build -t rkfbank-api .       # imagem da API para publicar (perfil prod; variáveis em backend/README_DEPLOY.md)
node scripts/varrer-cves.js         # vulnerabilidades conhecidas (OSV.dev) no Maven e no pnpm-lock
node scripts/gerar-svgs-readme.mjs  # regera os SVGs animados do README (assets/readme/) com o número atual de testes e migrações
scripts/instalar-hooks.sh           # ativa o hook que valida mensagens de commit
```

Rodar a API: `docker compose up -d` e `./mvnw spring-boot:run` na raiz (o Spring lê o `.env` sozinho). Console do H2 só nos perfis
`visitante`/`equipe` (`/h2-console`, JDBC URL `jdbc:h2:mem:rkfbank-<perfil>`); o perfil padrão o desliga de propósito (o DevTools
ligaria). Mudou rota ou payload? Atualize também `scripts/gerar-colecao-insomnia.mjs` e rode-o.
JDK **25** obrigatório (o `maven-enforcer-plugin` recusa outra versão).

## Regras do backend

- **Módulos:** classe nova vai para o pacote do assunto dela (`contas/`, `clientes/`...). O `compartilhado` não conhece nenhum módulo, o repository de um módulo só é usado por ele (o `titular` é a exceção da LGPD) e tecnologia de fora fica atrás de uma porta (`clientes/ConsultaCep`, `contas/comprovante/NotificadorDeMovimentacao`). Quem confere é o `ArquiteturaModulosTest`: regra quebrada = build vermelho.
- Camadas (dentro de cada módulo): controller só conhece service; service tem a regra e o `@Transactional`; repository só acesso a dados.
- Entrada e saída por DTO `record`; nunca exponha a entidade JPA.
- Erros: exceção de negócio → status pelo `TratadorGlobalDeErros` (`400`, `404`, `409`, `422`, `503`) no formato padrão (`timestamp`, `status`, `error`, `codigo`, `message`, `path`, `campos`). **Código e mensagem só no `CodigoErroEnum`**: o service lança `new RegraNegocioException(CodigoErroEnum.X)`, nunca escreve a frase. Código novo → contrato (seção 6) e `CodigoErro` em `frontend/lib/api/types.ts`; nunca renomeie um existente.
- **Carregamento:** relacionamento para-um novo nasce `fetch = FetchType.LAZY`; lista que mostra o associado busca junto com `@EntityGraph` (ou `JOIN FETCH`, que não pagina para-muitos). O `ConsultasSqlIntegracaoTest` conta as consultas das listas do ADMIN: N+1 = teste vermelho.
- Dinheiro em `BigDecimal` com 2 casas; `Conta` tem `@Version` (lock otimista → `409`).
- Logs com Log4j2 e `X-Request-Id`; nunca logar CPF completo, senha ou token.
- **LGPD:** dado pessoal novo precisa de finalidade; response devolve só o que a tela usa; nada de dado pessoal em log, exceção, URL ou prompt de IA. Pendências em `planing/debitos-tecnicos.md`.
- Mudou rota ou formato? Atualize `backend/README_CONTRATO_API.md` **e** avise o frontend (`frontend/lib/api/types.ts` espelha o contrato).
- **Schema só por Flyway** (`backend/src/main/resources/db/migration`, `ddl-auto=validate`): mudou entidade → crie `V<n>__descricao.sql` (hoje a última é `V16`) que rode no MySQL e no H2. **Nunca edite uma migração já mergeada** (checksum). Revise com `/revisar-migracao`.
- **Dados prontos** por callback `afterMigrate` do Flyway (roda a cada subida, idempotente): `db/seed/equipe` (Fellipe = origem, Renan = destino; perfis padrão e `equipe`) e `db/seed/visitante` (Ana e Bruno; só no perfil `visitante`). **Desligados no `test`** e em produção (`FLYWAY_LOCATIONS=classpath:db/migration`). Mudou tabela usada pelo seed → ajuste o seed no mesmo PR.
- 🔒 **Repositório público: nenhuma senha real no Git.** Login das contas da equipe vem de `EQUIPE_SENHA` (`InicializadorAcessoEquipe`) e o admin de `ADMIN_CPF`/`ADMIN_SENHA` (`InicializadorAdmin`), só no `.env`. Acessos e roteiro completo da equipe ficam em `.equipe/GUIA_TESTES_EQUIPE.md`, **fora do Git** (`.gitignore`): nunca copie conteúdo de lá para README, PR, issue, recado ou commit. Doc pública usa só as contas de visitante.
- **Perfil e acesso valem na hora:** o `ConversorTokenUsuario` confere `tb_usuario` (existe, `ativo`, mesmo perfil e mesma `versaoCredencial` do token) a cada requisição. Gestão de usuários só pelo ADMIN em `/admin/usuarios`; usuário nunca é apagado, é desativado.
- **Perfil `prod`** (`application-prod.properties`, ligado pelo `Dockerfile`): só `db/migration` (sem seed), sem Swagger, segredos e banco só por variável (`DB_*`, `JWT_SEGREDO`, `CORS_ORIGENS`), sem login da equipe. Garantias em `PerfilProdIntegracaoTest`; CI da imagem em `backend-docker.yml` (publica no ghcr.io na `main`) e da aplicação inteira em `e2e.yml`. Mudou o prod? Teste com `.equipe/testes/PROD_E2E.md`.
- **Comprovante por Outbox (V16):** depósito, saque e transferência gravam o evento na `tb_outbox` pelo `ComprovanteService.registrar` (`Propagation.MANDATORY`: só dentro da transação do dinheiro); o `PublicadorOutbox` entrega a cada 2 s. Nova rota que move dinheiro → registre o comprovante também. Evento sem dado pessoal.
- **Movimentações exigem `Idempotency-Key`** (`IdempotenciaService`, mesma transação do dinheiro). Nova rota que move dinheiro → passe pelo `executarUmaVez`.

## Armadilhas conhecidas

- H2 está fixado em **2.5.252** (o 2.4.240 que o Boot traz quebra o `CHECK ... IN` das colunas de enum); toda URL do H2 leva `MODE=MySQL` (o 2.5 só aceita `DATETIME` assim) e o perfil de teste tem `spring.test.database.replace=none`.
- `@DataJpaTest` troca o datasource por H2 embutido em qualquer perfil.
- **IntelliJ antigo × Maven 3.10 do wrapper:** a IntelliJ Community 2024.3 não importa o projeto (`Unable to create injector ... ArtifactHandlerManager` no log da IDE) e marca em vermelho as dependências mais novas (springdoc, ShedLock), mesmo com o `mvnw` verde. Solução: IntelliJ 2025.3+ ou Maven home = **Bundled** nas configurações. Não troque o wrapper por causa disso.
- Testes Spring têm `@ActiveProfiles("test")` e o `application-test.properties` fica em `src/test/resources` (fora do jar). O `pom.xml` aponta `RKFBANK_ENV` para um arquivo inexistente: o `.env` de quem roda não entra nos testes.
- O Mockito é carregado como `-javaagent` no surefire (Java 21+); não remova o `argLine`.
- Classe de teste precisa terminar em `Test`, `Tests` ou `TestCase`; senão o Surefire **ignora em silêncio**.
- O Spring lê o `.env` sozinho (`spring.config.import`), **desde que rode da raiz**. Scripts de shell que usam as variáveis ainda carregam o `.env` por conta própria.
- Flyway × H2: não use `spring.jpa.defer-datasource-initialization=true` (faz o JPA validar antes de o Flyway criar as tabelas); UUID está fixado como `BINARY` (`preferred_uuid_jdbc_type`); evite `BIT(n)` e `CAST(... AS BINARY)` sem tamanho, que o H2 trata diferente do MySQL.

## Git

> 🔒 **Commit e push só quando o Kevin mandar**, na mensagem atual e com essas palavras ("commita", "faz o push", `/entregar`).
> Sem essa ordem, deixe todas as mudanças **não commitadas** e mostre a lista de arquivos alterados (`git status --short`),
> para ele revisar na IDE. Autorização dada antes não vale para a próxima entrega. "Commita" sozinho não autoriza push.
> Nunca use `git reset`, `revert`, `rebase` ou `push --force` sem pedido explícito.

- `feature/*`, `fix/*`, `chore/*`, `docs/*` → Pull Request para a `develop`. Nunca commit direto em `main` ou `develop`.
- 📅 **Fluxo de entrega (Git Flow do time):** atualizar a `develop` (`git pull`) → criar a branch dela → `git pull origin develop`
  na branch e resolver os conflitos → PR para a `develop`, CI verde e merge → PR `develop → main` **aberto** (o `/entregar`
  abre ou atualiza). **O merge na `main` é só do Kevin e do Renan**, na reunião semanal, com `/notas-de-versao`: nunca mergeie nela.
- 🌱 **Branch nova nasce sempre da `origin/develop` atualizada** (`git fetch` + `git switch -c <branch> origin/develop`).
  Nunca empilhe uma branch em outra que ainda não foi mergeada: se a feature depende de algo fora da `develop`
  (confira com `git merge-base --is-ancestor <commit> origin/develop`), **pare e espere o merge**. "Vou mergear" ou
  "está em aprovação" não é merge feito: confira no remoto.
- 🔄 **Mantenha a branch atualizada com a `develop`**: no início de cada sessão de trabalho e antes do `/entregar`,
  `git fetch` + `git merge origin/develop` (merge, não rebase). Conflito → pare e mostre ao Kevin.
- Conventional Commits em português (`feat:`, `fix:`, `test:`, `docs:`, `refactor:`, `chore:`, `build:`, `ci:`), um assunto por commit. O hook `scripts/hooks/commit-msg` recusa mensagem fora do padrão.
- Antes do PR: `scripts/validar.sh` verde no lado que mudou. Detalhes em [CONTRIBUTING.md](CONTRIBUTING.md).

## Skills do projeto

| Skill | Para quê |
|---|---|
| `/implementar-feature <n>` | implementa a feature `n` do `planing/checklist-features.md` explicando cada conceito na prática |
| `/entregar` | Git Flow do time: `develop` atualizada → branch → `git pull` da `develop` e conflitos → valida, documenta, commits semânticos → PR e merge na `develop` com CI verde → PR `develop → main` aberto (sem merge) |
| `/verificar-contrato` | compara controllers/DTOs do backend, o contrato da API e os tipos do frontend, e aponta divergências |
| `/varrer-vulnerabilidades` | roda a varredura de CVEs e propõe a correção mínima |
| `/testar-e2e` | sobe o fluxo real (cadastro → extrato) contra a API e interpreta o resultado |
| `/explicar-codigo <alvo>` | explica um código que já existe (arquivo, fluxo ou conceito), sem alterar nada |
| `/revisar-migracao` | revisa migrações Flyway antes do commit (edição de migração aplicada, operação destrutiva, H2 × MySQL) |
| `/nova-tela <nome>` | cria uma tela no padrão do frontend (página + screen + hook); área do Renan |
| `/recado` | escreve, responde ou lista recados em `planing/recados.md` |
| `/notas-de-versao` | notas de versão entre `main` e `develop`, com resumo para o PO e sugestão de versão |
| `/sugerir-evolucao [área]` | sugere as próximas evoluções com mais valor de portfólio (padrões de projeto, arquitetura, testes, segurança), priorizadas pelo MVP; não altera código. Catálogo em `planing/padroes-de-projeto.md` |
