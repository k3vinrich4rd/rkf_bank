# Prompt para o Renan (o Kevin cola no chat/WhatsApp ou no Claude do Renan)

> Texto sem segredos. O Renan **nunca** manda senha nem URI do banco por chat: ela fica só no painel do Render dele.

---

Renan, aviso importante sobre o **banco do RKF Bank**, com prazo **21/12/2026**:

**O que está acontecendo.** O banco gratuito do Aiven que usamos (**MySQL Free**) vai ser **desligado e apagado em 21/12/2026**. Decidimos trocar para **PostgreSQL grátis** (Aiven PostgreSQL Free). O código já está pronto na PR `feature/migracao-postgresql` (para a `develop`; o Kevin e você mergeiam a `develop → main`). Como você tem o **seu próprio deploy** do RKF Bank (o seu Aiven e o seu Render), você precisa repetir a troca **no seu lado**. A API e o site (Vercel) não mudam: só o banco e as variáveis `DB_*` do Render.

**O que você precisa fazer, na ordem:**

1. **Antes de tudo, faça backup do seu MySQL** (se tiver dados que queira guardar). No terminal, com o Docker aberto (a senha é pedida na hora, não cole no chat):
   `docker run --rm -it -v "$PWD:/saida" mysql:8.4 mysqldump --ssl-mode=REQUIRED -h <HOST_DO_SEU_MYSQL> -P <PORTA_DO_SEU_MYSQL> -u <SEU_USUARIO> -p --single-transaction --no-tablespaces --result-file=/saida/meu-mysql-completo.sql <SEU_BANCO>`
   Guarde o arquivo fora do Git.
2. **Anote os valores atuais** das variáveis `DB_HOST`, `DB_PORT`, `DB_NAME`, `DB_USER`, `DB_PASSWORD` e `DB_SSL_MODE` do seu Render (tire um print). É o seu caminho de volta se algo der errado.
3. **Crie o banco novo no Aiven:** *Create service* → **PostgreSQL** → plano **Free**. Atenção: o painel abre no plano pago, então confira que está escrito **Free / US$ 0** antes de criar. Espere ficar *Running* e anote, na aba *Overview*, o **Host**, a **Porta**, o **Database** (`defaultdb`), o **User** (`avnadmin`) e a **Password**. Se o Aiven disser que você já tem um serviço Free desse tipo na organização, crie uma segunda organização para o RKF Bank, ou crie um banco e um usuário separados dentro do mesmo serviço.
4. **No seu Render (serviço da API), em *Environment*, troque só estas variáveis** e clique em **Save only** (sem deploy ainda):

   | Variável | Valor |
   |---|---|
   | `DB_HOST` | o Host do **PostgreSQL** novo |
   | `DB_PORT` | a Porta do PostgreSQL novo (não é a 3306 nem a 5432: veja no painel) |
   | `DB_NAME` | `defaultdb` (ou o banco que você criou) |
   | `DB_USER` | o usuário do PostgreSQL |
   | `DB_PASSWORD` | a senha do PostgreSQL (digite só no painel) |
   | `DB_SSL_MODE` | `require` (**minúsculo**; vazio não funciona) |

   Não mexa em `JWT_SEGREDO`, `CORS_ORIGENS`, `ADMIN_CPF`, `ADMIN_SENHA` nem em `SWAGGER_ATIVO`.
5. **Espere o merge `develop → main`** da PR de migração (você e o Kevin combinam o dia). Se o seu Render faz deploy automático da `main`, o deploy novo já nasce com o PostgreSQL. Se não, use *Manual Deploy → Deploy latest commit*.
6. **Confira:** `https://<seu-render>/actuator/health` deve dar `UP` (a primeira chamada pode levar ~1 minuto). O Flyway cria as tabelas sozinho no banco novo, que começa vazio e com o ADMIN do `ADMIN_CPF`/`ADMIN_SENHA` que você já tem.
7. **Entre no seu site** e teste: login, cadastro de um cliente novo, abrir conta, depósito, transferência e extrato.
8. **Se você quiser levar os dados antigos** (clientes, contas e lançamentos), peça ao Kevin o passo a passo da cópia (`planing/guia-corte-postgresql.md`, Partes 2 e 4): é só rodar `mysqldump` → `node scripts/converter-dump-mysql-para-postgres.mjs` → `psql` (as senhas são digitadas por você no terminal). Se for só demonstração, pode começar com o banco vazio.
9. **Se algo der errado:** devolva as variáveis do print do passo 2 (**Save only**) e, em *Deploys*, faça *Redeploy* do último deploy antes da troca. O seu MySQL continua intacto até 21/12.

**Erros comuns:** `password authentication failed` (usuário ou senha errados, sem espaço no fim), `sslmode` ou `SSL error` (você escreveu `REQUIRED` maiúsculo ou deixou vazio: use `require`), `Connection refused` (host ou porta errados, ou o serviço está desligado: use *Power on* no Aiven).

**Prazo:** faça tudo **antes de 21/12/2026** (de preferência até 14/12, para sobrar folga). Me avise quando o `/actuator/health` do seu deploy der `UP` no PostgreSQL. Não apague o MySQL antigo antes de uns dias de teste: o Aiven apaga sozinho em 21/12.

Não me mande senha, URI do Aiven nem print com a senha aparecendo. O que vale é só o painel do seu Render.

---
