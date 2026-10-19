---
name: notas-de-versao
description: Gera as notas de versão (release notes) do RKF Bank a partir dos commits e PRs entre a main e a develop, agrupadas por tipo e escritas para o time e para o PO, com sugestão de número de versão. Use quando pedirem "/notas-de-versao", "o que vai entrar na main", "prepara o release" ou similar.
---

# /notas-de-versao [base] [alvo]

Padrão: `base = origin/main`, `alvo = origin/develop`. Rode `git fetch --prune origin` antes.

## 1. Coletar (barato, só Git)
- Commits: `git log --no-merges --format="%h|%s|%an" <base>..<alvo>`.
- PRs mergeados: `git log --merges --format="%h|%s" <base>..<alvo>` (assunto `Merge pull request #N from .../branch`).
- Arquivos tocados por área: `git diff --stat <base>...<alvo> -- backend pom.xml`, `-- frontend`, `-- planing backend/*.md`.
- Última versão: `git tag --sort=-v:refname | head -1` (o projeto ainda pode não ter tags).

## 2. Agrupar pelos prefixos do Conventional Commits

| Seção | Prefixos |
|---|---|
| ✨ Novidades | `feat` |
| 🐛 Correções | `fix` (inclusive `fix(deps)`, destacando CVEs corrigidas) |
| 🧪 Testes | `test` |
| 🔧 Build, CI e dependências | `build`, `ci`, `chore` |
| 📝 Documentação | `docs` |
| ♻️ Melhorias internas | `refactor`, `perf`, `style` |

Separe **Backend** e **Frontend** dentro de cada seção quando houver os dois. Commits fora do padrão vão para "Outros".

## 3. Escrever
- Primeiro um **resumo de 3 linhas para o PO (Felipe)**: o que o usuário final ganha, sem jargão.
- Depois as seções técnicas, uma linha por item: `descrição (#PR ou hash) — autor`.
- **⚠️ Atenção na implantação:** migrações de banco, variáveis de ambiente novas, mudanças de contrato da API, versões sobrescritas no `pom.xml`.
- **Sugestão de versão** (SemVer): `feat` → minor; só `fix` → patch; `!` ou mudança incompatível de contrato → major. Sem tag anterior, sugira `v0.1.0`.

## 4. Entregar
Mostre o texto em Markdown pronto para colar no PR `develop → main` ou numa release do GitHub. Só crie a tag ou o arquivo `CHANGELOG.md` se o usuário pedir.
