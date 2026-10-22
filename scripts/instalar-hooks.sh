#!/usr/bin/env sh
# Faz o Git usar os hooks versionados em scripts/hooks (vale só para este clone e os worktrees dele).
#
# O caminho gravado é ABSOLUTO e aponta para o checkout principal. Com o caminho relativo (scripts/hooks), o Git não
# acha os hooks dentro de um worktree (git worktree add), e o commit-msg e o post-commit deixam de rodar lá.
# O checkout principal vem de --git-common-dir: rodar este script de dentro de um worktree ainda grava o caminho
# do principal, que não some quando o worktree é apagado.
set -e
cd "$(dirname "$0")/.."
GIT_COMUM=$(git rev-parse --path-format=absolute --git-common-dir)
HOOKS="$(dirname "$GIT_COMUM")/scripts/hooks"
git config core.hooksPath "$HOOKS"
chmod +x "$HOOKS"/* 2>/dev/null || true
echo "✅ Hooks ativados em $HOOKS (vale também nos worktrees): o commit-msg valida o padrão Conventional Commits."
