# RKF Bank — contexto para agentes de IA

Banco digital full stack feito em dupla. **Backend:** Kevin. **Frontend:** Renan. **PO & QA:** Fellipe.
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
| `backend/src/main/java/com/br/rkfbank` | API Spring Boot 4.1 / Java 25: `controllers` → `services` → `repositories`, `dto`, `entities`, `exceptions`, `clients/viacep`, `config`, `validation` |
| `backend/src/test/java/...` | JUnit 5 + Mockito (services, controllers) e `@DataJpaTest` (repositories) |
| `backend/*.md` | contrato da API, arquitetura, roteiro e checklist E2E |
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
node scripts/smoke-e2e.mjs          # fluxo real contra a API rodando em http://localhost:8080
node scripts/varrer-cves.js         # vulnerabilidades conhecidas (OSV.dev) no Maven e no pnpm-lock
scripts/instalar-hooks.sh           # ativa o hook que valida mensagens de commit
```

Rodar a API: `docker compose up -d`, carregar o `.env` no terminal e `./mvnw spring-boot:run`.
JDK **25** obrigatório (o `maven-enforcer-plugin` recusa outra versão).

## Regras do backend

- Camadas: controller só conhece service; service tem a regra e o `@Transactional`; repository só acesso a dados.
- Entrada e saída por DTO `record`; nunca exponha a entidade JPA.
- Erros: exceção de negócio → status pelo `ControllerExceptionHandler` (`400`, `404`, `409`, `422`, `503`) no formato padrão (`timestamp`, `status`, `error`, `message`, `path`, `campos`).
- Dinheiro em `BigDecimal` com 2 casas; `Conta` tem `@Version` (lock otimista → `409`).
- Logs com Log4j2 e `X-Request-Id`; nunca logar CPF completo, senha ou token.
- Mudou rota ou formato? Atualize `backend/README_CONTRATO_API.md` **e** avise o frontend (`frontend/lib/api/types.ts` espelha o contrato).
- **Schema só por Flyway** (`backend/src/main/resources/db/migration`, `ddl-auto=validate`): mudou entidade → crie `V<n>__descricao.sql` que rode no MySQL e no H2. **Nunca edite uma migração já mergeada** (checksum). Revise com `/revisar-migracao`.

## Armadilhas conhecidas

- H2 está fixado em **2.3.232** (o 2.4.240 quebra o `CHECK ... IN` das colunas de enum).
- Tomcat e Jackson estão **sobrescritos** no `pom.xml` por causa de CVEs; remover quando o Spring Boot alcançar.
- `@DataJpaTest` troca o datasource por H2 embutido em qualquer perfil.
- O Mockito é carregado como `-javaagent` no surefire (Java 21+); não remova o `argLine`.
- Classe de teste precisa terminar em `Test`, `Tests` ou `TestCase`; senão o Surefire **ignora em silêncio**.
- O Spring **não lê o `.env`** sozinho: carregue as variáveis no terminal antes de `spring-boot:run`.
- Flyway × H2: não use `spring.jpa.defer-datasource-initialization=true` (faz o JPA validar antes de o Flyway criar as tabelas); UUID está fixado como `BINARY` (`preferred_uuid_jdbc_type`); evite `BIT(n)` e `CAST(... AS BINARY)` sem tamanho, que o H2 trata diferente do MySQL.

## Git

> 🔒 **Commit e push só quando o Kevin mandar**, na mensagem atual e com essas palavras ("commita", "faz o push", `/entregar`).
> Sem essa ordem, deixe todas as mudanças **não commitadas** e mostre a lista de arquivos alterados (`git status --short`),
> para ele revisar na IDE. Autorização dada antes não vale para a próxima entrega. "Commita" sozinho não autoriza push.
> Nunca use `git reset`, `revert`, `rebase` ou `push --force` sem pedido explícito.

- `feature/*`, `fix/*`, `chore/*`, `docs/*` → Pull Request para a `develop`. Nunca commit direto em `main` ou `develop`.
- Conventional Commits em português (`feat:`, `fix:`, `test:`, `docs:`, `refactor:`, `chore:`, `build:`, `ci:`), um assunto por commit. O hook `scripts/hooks/commit-msg` recusa mensagem fora do padrão.
- Antes do PR: `scripts/validar.sh` verde no lado que mudou. Detalhes em [CONTRIBUTING.md](CONTRIBUTING.md).

## Skills do projeto

| Skill | Para quê |
|---|---|
| `/implementar-feature <n>` | implementa a feature `n` do `planing/checklist-features.md` explicando cada conceito na prática |
| `/entregar` | valida, atualiza a documentação afetada, faz commits semânticos, push e escreve a descrição do PR |
| `/verificar-contrato` | compara controllers/DTOs do backend, o contrato da API e os tipos do frontend, e aponta divergências |
| `/varrer-vulnerabilidades` | roda a varredura de CVEs e propõe a correção mínima |
| `/testar-e2e` | sobe o fluxo real (cadastro → extrato) contra a API e interpreta o resultado |
| `/explicar-codigo <alvo>` | explica um código que já existe (arquivo, fluxo ou conceito), sem alterar nada |
| `/revisar-migracao` | revisa migrações Flyway antes do commit (edição de migração aplicada, operação destrutiva, H2 × MySQL) |
| `/nova-tela <nome>` | cria uma tela no padrão do frontend (página + screen + hook); área do Renan |
| `/recado` | escreve, responde ou lista recados em `planing/recados.md` |
| `/notas-de-versao` | notas de versão entre `main` e `develop`, com resumo para o PO e sugestão de versão |
