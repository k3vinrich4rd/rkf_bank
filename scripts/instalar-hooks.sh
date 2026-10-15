#!/usr/bin/env sh
# Faz o Git usar os hooks versionados em scripts/hooks (vale só para este clone).
set -e
cd "$(dirname "$0")/.."
git config core.hooksPath scripts/hooks
chmod +x scripts/hooks/* 2>/dev/null || true
echo "✅ Hooks ativados: o commit-msg agora valida o padrão Conventional Commits."
