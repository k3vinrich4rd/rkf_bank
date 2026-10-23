#!/usr/bin/env bash
# Valida o projeto e imprime só o resumo (o log completo fica em .validar/*.log).
# Uso: scripts/validar.sh [backend|frontend|tudo]   (padrão: backend)
#   backend  -> ./mvnw clean verify com o perfil "test" (H2, não precisa de Docker)
#   frontend -> pnpm lint + typecheck + test + build dentro de frontend/
# Sai com código 0 se tudo passou e 1 se algo falhou.

set -u
cd "$(dirname "$0")/.."
RAIZ="$(pwd)"
ALVO="${1:-backend}"
mkdir -p .validar
FALHOU=0

validar_backend() {
  local log="$RAIZ/.validar/backend.log"
  echo ">>> Backend: ./mvnw clean verify (perfil test)"
  if ./mvnw -B -ntp clean verify -Dspring.profiles.active=test >"$log" 2>&1; then
    grep -E "Tests run: [0-9]+, Failures" "$log" | tail -1 | sed 's/^\[INFO\] /    /'
    echo "    ✅ BUILD SUCCESS (relatório de cobertura: target/site/jacoco/index.html)"
  else
    FALHOU=1
    grep -E "Tests run: [0-9]+, Failures" "$log" | tail -1 | sed 's/^\[[A-Z]*\] /    /'
    echo "    ❌ BUILD FAILURE. Testes e erros que falharam:"
    grep -E "<<< (FAILURE|ERROR)!|\[ERROR\] (Failures|Errors|  )|COMPILATION ERROR|cannot find symbol|\.java:\[[0-9]+" "$log" \
      | grep -v "^\[ERROR\] *$" | sed 's/^/    /' | head -25
    echo "    Log completo: $log"
  fi
}

pnpm_cmd() {
  # Usa a versão de pnpm fixada no package.json ("packageManager"), sem precisar instalar nada global.
  local versao
  versao="$(node -p "require('./package.json').packageManager.split('@')[1]")"
  npx -y "pnpm@$versao" "$@"
}

validar_frontend() {
  local log="$RAIZ/.validar/frontend.log"
  : >"$log"
  echo ">>> Frontend: pnpm lint, typecheck, test e build"
  (
    cd frontend || exit 1
    if [ ! -d node_modules ]; then
      echo "    instalando dependências (pnpm install --frozen-lockfile)..."
      pnpm_cmd install --frozen-lockfile >>"$log" 2>&1 || exit 1
    fi
    for etapa in lint typecheck test build; do
      if pnpm_cmd run "$etapa" >>"$log" 2>&1; then
        echo "    ✅ $etapa"
      else
        echo "    ❌ $etapa falhou:"
        tail -25 "$log" | sed 's/^/       /'
        exit 1
      fi
    done
  ) || { FALHOU=1; echo "    Log completo: $log"; }
}

case "$ALVO" in
  backend)  validar_backend ;;
  frontend) validar_frontend ;;
  tudo)     validar_backend; validar_frontend ;;
  *) echo "Uso: scripts/validar.sh [backend|frontend|tudo]"; exit 2 ;;
esac

exit $FALHOU
