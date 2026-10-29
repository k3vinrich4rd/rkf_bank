---
name: revisar-migracao
description: Revisa migrações Flyway novas ou alteradas do RKF Bank (backend/src/main/resources/db/migration) antes do commit ou do PR - nome e versão, edição de migração já aplicada, operações destrutivas, compatibilidade MySQL x H2, constraints e coerência com as entidades JPA. Use quando o usuário pedir "/revisar-migracao", ao criar ou mudar um arquivo V__*.sql, ou antes de abrir PR que mexe no banco.
---

# /revisar-migracao

Pastas: `backend/src/main/resources/db/migration` (schema, `V<n>__*.sql`, roda em todo ambiente) e `backend/src/main/resources/db/seed/{equipe,visitante}` (callbacks `afterMigrate__*.sql`: contas da equipe e de visitante, rodam a cada subida, fora do perfil `test` e de produção). Se `db/migration` não existir, diga que o Flyway ainda não foi implementado e pare.

> A reestruturação de 20/10/2026 (V1–V3 antigos → V1–V5, uma tabela por versão; V6 = idempotência) foi decidida e registrada em `planing/decisoes.md`; não aponte como edição indevida. Daqui para frente a regra do item 1 vale normalmente.

Modo **revisão**: aponte problemas e sugira a correção; só edite se o usuário pedir.

## 1. O que mudou (barato)
- `git diff --name-status origin/develop...HEAD -- backend/src/main/resources/db/migration` + arquivos não commitados da pasta.
- Liste as versões existentes (`ls`) para checar sequência e duplicidade.

## 2. Checklist de revisão

| # | Verificação | Gravidade se falhar |
|:-:|---|:-:|
| 1 | **Migração já mergeada na `develop` foi editada?** (arquivo aparece como `M` no diff contra a `develop`). Isso quebra o checksum em todo banco que já aplicou. Correção: reverter e criar um `V<n+1>__`. | 🔴 |
| 2 | Nome no padrão `V<número>__<descricao_em_snake_case>.sql` (dois underlines), versão maior que a última e sem duplicar. | 🔴 |
| 3 | Operação destrutiva (`DROP TABLE`, `DROP COLUMN`, `TRUNCATE`, `DELETE` sem `WHERE`, mudança de tipo que perde dado)? Exige justificativa no PR e plano para os dados existentes. | 🔴 |
| 4 | `NOT NULL` adicionado em tabela com dados sem `DEFAULT` ou sem `UPDATE` antes? Falha em produção, passa no banco vazio do teste. | 🔴 |
| 5 | Compatível com MySQL 8.4 **e** com o H2 dos testes (modo MySQL)? Evitar sintaxe exclusiva de um dos dois; se for inevitável, apontar. | 🟡 |
| 6 | Dinheiro em `DECIMAL(19,2)`; UUID no mesmo tipo das tabelas existentes; nomes de constraint no padrão `uk_<tabela>_<coluna>`, `fk_<tabela>_<referencia>`. | 🟡 |
| 7 | A migração bate com as entidades JPA (classes `@Entity` nos modulos: `backend/src/main/java/com/br/rkfbank/<clientes|contas|autenticacao>/`)? Coluna, tipo, tamanho e nulabilidade. Com `ddl-auto=validate`, divergência derruba a aplicação na subida. | 🔴 |
| 8 | Migração de **dados** (ex.: e-mail em minúsculas) é idempotente e não depende da ordem das linhas? | 🟡 |
| 9 | Índices para colunas usadas em busca (`WHERE`, `JOIN`, `ORDER BY` do extrato)? | 🟢 |
| 10 | Mudou tabela/coluna usada pelo `db/seed`? O seed foi ajustado no mesmo PR e continua **idempotente** (`INSERT ... SELECT ... WHERE NOT EXISTS` pelo id e pela chave natural) e sem senha real? O seed da **equipe** não pode ter `tb_usuario` (login vem do `.env`); só o de **visitante** tem hash, e da senha pública `Visitante@2026`. | 🔴 |
| 11 | Algum dado de demonstração foi parar em `db/migration`? Lá ele iria para produção. Dado de demo só em `db/seed`. | 🔴 |

## 3. Validar de verdade
- `scripts/validar.sh backend` (testes em H2 aplicam as migrações; o `DadosEquipeIntegracaoTest` aplica o `db/seed/equipe` e roda o `migrate` duas vezes para provar a idempotência; o `PerfilVisitanteIntegracaoTest` sobe o perfil `visitante`).
- Se o usuário permitir apagar o MySQL local: `./run-test.sh`, depois subir a API e conferir `select version, description, success from flyway_schema_history;`.

## 4. Saída
Tabela `arquivo | verificação | gravidade | o que fazer`, seguida de um veredito: **pronto para commitar quando você mandar** / **corrigir antes**. Esta skill nunca commita. Para cada item 🔴, explique o conceito em 2 linhas (por que aquilo quebra).
