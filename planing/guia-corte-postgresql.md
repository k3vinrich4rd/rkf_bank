# Guia de corte: Aiven MySQL → Aiven PostgreSQL (produção do RKF Bank)

| | |
|---|---|
| 🎯 **Para que serve** | tirar o RKF Bank do Aiven MySQL Free (desligado e **apagado em 21/12/2026**) e colocá-lo no Aiven PostgreSQL Free, com os dados de portfólio e com volta garantida |
| 👤 **Quem faz** | **Kevin** (API e banco). O **Renan** repete o mesmo caminho na conta Aiven/Render dele: texto pronto em [`prompt-renan-aiven-postgresql.md`](prompt-renan-aiven-postgresql.md) |
| 📅 **Prazo** | terminar **antes de 21/12/2026**; sugestão: corte na semana de 07/12, para sobrar folga de rollback |
| 🚫 **Este guia não foi executado** | nada foi feito contra a produção. O código foi validado só em Docker local (ver "O que já foi provado") |
| 📎 **Decisão** | [`decisoes.md` → "Migração do MySQL para PostgreSQL"](decisoes.md): baseline novo, V1 a V15 reescritas |

## O que muda e o que não muda

| | Antes | Depois |
|---|---|---|
| Banco | Aiven MySQL Free | **Aiven PostgreSQL Free** |
| Variáveis do Render | `DB_HOST` `DB_PORT` `DB_NAME` `DB_USER` `DB_PASSWORD` `DB_SSL_MODE` | **as mesmas**, com valores novos |
| `DB_SSL_MODE` | `REQUIRED` | **`require`** (minúsculo; vazio não cai no padrão) |
| `DB_PORT` | a do Aiven MySQL | a do Aiven PostgreSQL (veja no painel) |
| `DB_NAME` | o do MySQL | `defaultdb` (padrão do Aiven) ou um banco novo |
| Vercel (site) | `API_URL` do Render | **não muda nada** |
| `JWT_SEGREDO`, `CORS_ORIGENS`, `ADMIN_*`, `SWAGGER_ATIVO` | iguais | **iguais** (não mexa) |

## O que já foi provado (23/11/2026, Docker local)

- `./mvnw verify` verde: **368 testes, 0 falhas**, incluindo os que sobem um **PostgreSQL 17 de verdade** (Testcontainers): migrações V1 a V15, fluxo do cadastro ao extrato, concorrência, seeds e limite de login.
- A **imagem Docker do perfil `prod`** subiu contra um PostgreSQL 17 vazio e passou o smoke ponta a ponta: **42/42**.
- O perfil padrão (equipe) com `docker compose` + seeds: smoke **44/44**.
- **Ensaio da cópia dos dados:** um MySQL 8.4 com o schema antigo foi populado, passou por `mysqldump` → `converter-dump-mysql-para-postgres.mjs` → `psql` no schema novo, e saldo, texto com aspas/acento/quebra de linha, `BIT` e datas chegaram certos.

## Quem faz o quê

| Só o **Kevin** | Pode pedir ao Claude |
|---|---|
| Entrar no Aiven, criar o serviço no plano **Free**, definir/guardar senha | conferir as consultas de validação e o resultado delas |
| Colar as variáveis no Render e escolher **Save only** | ler o log do deploy e achar o erro |
| Rodar o `mysqldump` e o `psql` (as senhas são digitadas por ele, no terminal) | ajustar o script de conversão se aparecer um valor novo |
| Combinar com o Renan o merge `develop → main` (**só Kevin e Renan mergeiam**) | |

> 🔒 Nunca cole senha, URI do Aiven ou `.env` no chat, no Git, em PR ou em recado. O Claude não precisa deles para ajudar.

---

## Parte 0 · Antes de qualquer coisa (sem pressa, dá para fazer já)

| # | O que fazer | Pronto quando |
|:-:|---|---|
| 0.1 | **Backup completo do MySQL atual** (com schema), guardado fora do Git (Drive): `docker run --rm -it -v "$PWD:/saida" mysql:8.4 mysqldump --ssl-mode=REQUIRED -h <HOST_MYSQL> -P <PORTA_MYSQL> -u <USUARIO> -p --single-transaction --no-tablespaces --result-file=/saida/rkfbank-mysql-completo.sql <BANCO>` (a senha é pedida no terminal) | arquivo com alguns KB/MB, terminando em `-- Dump completed` |
| 0.2 | **Anotar os valores atuais** das variáveis `DB_*` do Render (print do painel, guardado fora do Git). É o seu rollback | print salvo |
| 0.3 | Ver quantos registros existem hoje (consultas da Parte 3, no MySQL) | números anotados |
| 0.4 | Confirmar com o Renan a data do corte (o merge `develop → main` é dos dois) | data combinada |

## Parte 1 · Criar o Aiven PostgreSQL Free

| # | Onde | O que fazer | Cuidado |
|:-:|---|---|---|
| 1.1 | Aiven (console) | *Create service* → **PostgreSQL** → plano **Free** → nuvem/região perto do Render (Oregon: escolha uma região dos EUA oeste, se existir no Free) → nome `rkfbank-pg` | ⚠️ **O painel abre no plano pago.** Troque para **Free** antes de criar e confira que aparece US$ 0 |
| 1.2 | Aiven | esperar *Running*; abrir *Overview* e anotar **Host**, **Port**, **Database** (`defaultdb`), **User** (`avnadmin`) e **Password** | a senha fica só com você |
| 1.3 | Aiven | **Limite do Free:** é permitido **1 serviço Free de cada tipo por organização**. Se a Barbearia já usa o PostgreSQL Free na mesma organização, escolha **uma** das saídas: (a) criar uma **segunda organização** no Aiven só para o RKF Bank e o serviço nela; ou (b) usar o **mesmo serviço** com um banco e um usuário separados (`CREATE DATABASE rkfbank;` e `CREATE USER rkfbank_app PASSWORD '...';` com `GRANT` só nesse banco) | (b) divide 1 GB de disco e 20 conexões entre os dois projetos; prefira (a). Plano B de provedor: **Neon** (também grátis), só mudam os valores `DB_*` |
| 1.4 | Aiven | (opcional) em *Allowed IP addresses*, deixe `0.0.0.0/0` (o Render Free não tem IP fixo) | senha forte compensa |

Limites do Free (documentação do Aiven, conferida em 23/11/2026): 1 CPU, 1 GB de RAM, 1 GB de disco, `max_connections` = 20 (a API usa pool de 5), sem pool de conexões, backups incluídos, pode ser desligado sem atividade (avisa antes; religa pelo painel).

## Parte 2 · Ensaio da cópia (na sua máquina, sem tocar em produção)

Já foi feito em Docker local (veja acima), mas repita com **os seus dados reais** para ver o resultado antes do dia:

1. `docker run --rm -it -v "$PWD:/saida" mysql:8.4 mysqldump --ssl-mode=REQUIRED -h <HOST_MYSQL> -P <PORTA_MYSQL> -u <USUARIO> -p --no-create-info --complete-insert --skip-extended-insert --hex-blob --skip-add-locks --skip-disable-keys --no-tablespaces --skip-triggers --compact --result-file=/saida/dados-mysql.sql <BANCO> tb_endereco tb_cliente tb_usuario tb_conta tb_lancamento`
   (só **lê**; use `--result-file`, não `>`: no PowerShell o `>` grava em UTF-16 e quebra o acento)
2. `node scripts/converter-dump-mysql-para-postgres.mjs dados-mysql.sql > dados-postgres.sql` (o script para com erro se achar algo que não conhece: não ignore)
3. Suba um PostgreSQL descartável com o schema novo e importe lá:
   `docker compose up -d` (banco local) → `./mvnw spring-boot:run` (o Flyway cria o schema) → pare a API → `docker exec -i rkfbank-postgres psql -U <POSTGRES_USER> -v ON_ERROR_STOP=1 rkfbank < dados-postgres.sql`
   (antes: `DELETE FROM tb_usuario WHERE cliente_id IS NULL;` se a API tiver criado um ADMIN; veja o passo 4.3)
4. Rode as consultas da Parte 3 nos dois bancos: os números têm que ser iguais.

> O que **não** vai na cópia, de propósito: `tb_idempotencia`, `tb_refresh_token`, `tb_tentativa_login`, `tb_shedlock` (descartáveis: as pessoas só entram de novo) e `flyway_schema_history` (o PostgreSQL cria o dele).

## Parte 3 · Consultas de validação (rode nos DOIS bancos e compare)

```sql
SELECT 'clientes' AS t, COUNT(*) FROM tb_cliente
UNION ALL SELECT 'enderecos', COUNT(*) FROM tb_endereco
UNION ALL SELECT 'usuarios', COUNT(*) FROM tb_usuario
UNION ALL SELECT 'contas', COUNT(*) FROM tb_conta
UNION ALL SELECT 'lancamentos', COUNT(*) FROM tb_lancamento;

SELECT SUM(saldo) AS saldo_total FROM tb_conta;
SELECT SUM(valor) AS valor_total_lancamentos FROM tb_lancamento;

-- saldo x extrato: tem que dar 0 linhas nos dois
SELECT c.numero_conta FROM tb_conta c
WHERE c.saldo <> COALESCE((SELECT SUM(valor) FROM tb_lancamento WHERE conta_destino_id = c.id), 0)
                - COALESCE((SELECT SUM(valor) FROM tb_lancamento WHERE conta_origem_id = c.id), 0);
```

Também vale conferir pela API depois do corte: `GET /admin/conciliacao` (ADMIN) deve devolver lista vazia.

## Parte 4 · O corte (janela curta; avise o Renan e a equipe)

| # | Onde | O que fazer | Pronto quando |
|:-:|---|---|---|
| 4.1 | Render | *Settings → Build & Deploy* → **Auto-Deploy: Off** (para o merge não publicar sozinho antes da hora) | desligado |
| 4.2 | Render | *Environment*: trocar `DB_HOST`, `DB_PORT`, `DB_NAME`, `DB_USER`, `DB_PASSWORD` pelos do **PostgreSQL** e `DB_SSL_MODE` = `require`. Escolha **Save only** (sem deploy!). Não mexa nas outras | variáveis salvas, nenhum deploy novo |
| 4.3 | GitHub | **Kevin e Renan** mergeiam a PR `develop → main` (o Claude nunca mergeia na `main`) | `main` com a migração |
| 4.4 | Render | *Manual Deploy → Deploy latest commit* (ou *Clear build cache & deploy* se o build falhar por cache) | deploy *Live*; `https://rkfbank-api-zncu.onrender.com/actuator/health` → `UP`. O Flyway criou as 15 tabelas e o ADMIN do `ADMIN_CPF` no PostgreSQL vazio |
| 4.5 | Render | *Suspend Service* (a API para; ninguém grava durante a cópia) | serviço suspenso |
| 4.6 | sua máquina | repita a Parte 2 passos 1 e 2 **contra o MySQL de produção** (dados frescos) | `dados-postgres.sql` gerado sem erro |
| 4.7 | sua máquina | limpar o ADMIN que a API acabou de criar (o do dump traz o mesmo ADMIN com a mesma senha) e importar, numa conexão só:<br>`docker run --rm -it -v "$PWD:/saida" postgres:17 psql "host=<HOST_PG> port=<PORTA_PG> dbname=<NOME_PG> user=<USUARIO_PG> sslmode=require" -v ON_ERROR_STOP=1 -c "DELETE FROM tb_usuario WHERE cliente_id IS NULL;" -f /saida/dados-postgres.sql`<br>(a senha é pedida no terminal) | terminou sem `ERROR`; o arquivo tem `BEGIN`/`COMMIT`: se der erro, nada é gravado e você repete |
| 4.8 | sua máquina | rodar as consultas da Parte 3 no PostgreSQL: **iguais** às do MySQL | números batem, divergência = 0 linhas |
| 4.9 | Render | *Resume Service* e esperar `UP` (a API reinicia; a primeira chamada leva ~1 min) | `UP` |
| 4.10 | Render | *Auto-Deploy: On* de novo | ligado |

## Parte 5 · Validação em produção

1. `https://rkfbank-api-zncu.onrender.com/actuator/health` → `UP`.
2. No site (`https://rkfbank.vercel.app`): entrar com um cliente que já existia → ver contas e **extrato com os mesmos valores**.
3. Fazer um depósito pequeno e uma transferência entre duas contas suas → saldos e extrato coerentes.
4. Entrar como ADMIN → `GET /admin/conciliacao` vazio; listar usuários.
5. Logs do Render sem `ERROR` (procure `flyway`, `HikariPool`, `SSL`).
6. Cadastrar um cliente novo (ViaCEP) e fazer login nele.
7. Só depois de 1 a 6: avise o Renan e a equipe. **Não apague o MySQL antes de passar alguns dias**; ele some sozinho em 21/12.

## Parte 6 · Rollback (a qualquer momento antes de 21/12)

O MySQL **não foi alterado** em nenhum passo (só leitura), então a volta é simples:

| # | Onde | O que fazer |
|:-:|---|---|
| R.1 | Render | *Environment*: devolver `DB_HOST`, `DB_PORT`, `DB_NAME`, `DB_USER`, `DB_PASSWORD` e `DB_SSL_MODE=REQUIRED` aos valores do print do passo 0.2 (**Save only**) |
| R.2 | Render | *Deploys* → escolher o último deploy **antes do corte** (a imagem com MySQL) → **Redeploy** (ou *Rollback*) |
| R.3 | conferir | `/actuator/health` → `UP` e login no site |

O que se perde no rollback: o que foi gravado **depois** do corte (cadastros, depósitos). Em portfólio, o aceitável; se precisar levar para o MySQL, peça ajuda (o caminho inverso exige outro script).
Se o problema for **só** o deploy (build ou subida), antes de tudo leia o log: erro de `SSL`/`password` costuma ser variável digitada errada (`DB_SSL_MODE=require` minúsculo), não o banco.

## Parte 7 · Depois do corte

- Atualize o `backend/README_DEPLOY.md` (já descreve PostgreSQL) com a data real do corte e o nome do serviço no Aiven.
- Dê um tempo (uma semana) e então **apague o Aiven MySQL** (ou deixe o Aiven apagar em 21/12). Guarde o backup completo da Parte 0.1 no Drive.
- Se for criar CI/ambiente novo, lembre: o **PostgreSQL com `sslmode=require`**; no CI e no Docker local o banco não tem SSL e usa `DB_SSL_MODE=disable`.
- `.equipe/` (guia privado e roteiros E2E) ainda cita `docker exec rkfbank-mysql mysql ...` e `MYSQL_*`: troque por `docker exec rkfbank-postgres psql -U "$POSTGRES_USER" "$POSTGRES_DB"` e `POSTGRES_*`.
- O `.env` local do Kevin e do Renan: troque `MYSQL_*` por `POSTGRES_*` (copie do `.env.example`) e rode `docker compose down -v && docker compose up -d` (o volume do MySQL antigo não serve).

## Se der errado: sintomas comuns

| Sintoma no log do Render | Causa provável | O que fazer |
|---|---|---|
| `The server requested SCRAM-SHA-256 authentication ... / password authentication failed` | senha ou usuário errados | rever `DB_USER`/`DB_PASSWORD` (sem espaço no fim) |
| `Connection refused` / `timed out` | `DB_HOST`/`DB_PORT` errados, ou serviço desligado no Aiven | conferir o Overview; ligar (*Power on*) |
| `SSL error` / `sslmode` inválido | `DB_SSL_MODE` em maiúsculo ou vazio | `require`, minúsculo |
| `FATAL: remaining connection slots are reserved` | passou das 20 conexões do Free (outro projeto no mesmo serviço) | separar serviços ou baixar `DB_POOL` |
| `Schema-validation: wrong column type` | imagem antiga rodando contra o PostgreSQL | confirmar que o deploy é do commit novo |
| `ERROR: duplicate key ... uk_usuario_login` na importação | esqueceu o `DELETE ... cliente_id IS NULL` (passo 4.7) | rode o `DELETE` e importe de novo (a transação desfez tudo) |
