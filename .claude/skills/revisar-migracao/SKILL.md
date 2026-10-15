---
name: revisar-migracao
description: Revisa migrações Flyway novas ou alteradas do RKF Bank (backend/src/main/resources/db/migration) antes do commit ou do PR - nome e versão, edição de migração já aplicada, operações destrutivas, compatibilidade MySQL x H2, constraints e coerência com as entidades JPA. Use quando o usuário pedir "/revisar-migracao", ao criar ou mudar um arquivo V__*.sql, ou antes de abrir PR que mexe no banco.
---

# /revisar-migracao

Pasta das migrações: `backend/src/main/resources/db/migration`. Se ela não existir, diga que o Flyway ainda não foi implementado (feature 1 do `planing/checklist-features.md`) e pare.

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
| 7 | A migração bate com as entidades JPA (`backend/src/main/java/com/br/rkfbank/entities`)? Coluna, tipo, tamanho e nulabilidade. Com `ddl-auto=validate`, divergência derruba a aplicação na subida. | 🔴 |
| 8 | Migração de **dados** (ex.: e-mail em minúsculas) é idempotente e não depende da ordem das linhas? | 🟡 |
| 9 | Índices para colunas usadas em busca (`WHERE`, `JOIN`, `ORDER BY` do extrato)? | 🟢 |

## 3. Validar de verdade
- `scripts/validar.sh backend` (testes em H2 aplicam as migrações).
- Se o usuário permitir apagar o MySQL local: `./run-test.sh`, depois subir a API e conferir `select version, description, success from flyway_schema_history;`.

## 4. Saída
Tabela `arquivo | verificação | gravidade | o que fazer`, seguida de um veredito: **pode commitar** / **corrigir antes**. Para cada item 🔴, explique o conceito em 2 linhas (por que aquilo quebra).
