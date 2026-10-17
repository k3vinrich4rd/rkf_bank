#!/bin/bash
# Roda os testes de integracao do Spring (@SpringBootTest) contra o MySQL do Docker, em vez do H2.
# Uso: scripts/testar-no-mysql.sh [ClasseDeTeste,OutraClasse]
#      (padrao: SegurancaIntegracaoTest,RkfbankApplicationTests)
#
# Por que existe: scripts/validar.sh e ./run-test.sh usam o perfil "test" (H2 em memoria), entao nenhum teste
# toca o MySQL. Aqui o perfil continua "test", mas o datasource e trocado por -D (propriedade de sistema ganha
# do application-test.properties). Testes de repositorio (@DataJpaTest) continuam no H2: o Spring troca o banco
# deles sempre; MySQL real para eles e a feature 3 (Testcontainers).
# NAO apaga dados: usa o MySQL como esta (precisa estar no ar: docker compose up -d) e cria clientes com CPF aleatorio.

set -e
cd "$(dirname "$0")/.."

if [ ! -f .env ]; then
  echo "Falta o .env (copie do .env.example)." >&2
  exit 1
fi
# O .env do Windows tem CRLF: sem o tr, cada valor ganharia um \r invisivel no fim.
set -a
# shellcheck disable=SC1090
source <(tr -d '\r' < .env)
set +a

if ! docker inspect --format='{{.State.Health.Status}}' rkfbank-mysql 2>/dev/null | grep -q healthy; then
  echo "MySQL fora do ar. Suba com: docker compose up -d" >&2
  exit 1
fi

CLASSES="${1:-SegurancaIntegracaoTest,RkfbankApplicationTests}"
mkdir -p .validar
LOG=.validar/mysql.log

echo ">>> Testes de integracao contra o MySQL ($CLASSES)"
if ./mvnw -B -ntp test -Dspring.profiles.active=test -Dtest="$CLASSES" -Djacoco.skip=true \
    "-Dspring.datasource.url=jdbc:mysql://localhost:${MYSQL_PORT:-5441}/${MYSQL_DATABASE:-rkfbank}?useSSL=false&serverTimezone=UTC&allowPublicKeyRetrieval=true" \
    -Dspring.datasource.driver-class-name=com.mysql.cj.jdbc.Driver \
    "-Dspring.datasource.username=${MYSQL_USER}" \
    "-Dspring.datasource.password=${MYSQL_PASSWORD}" >"$LOG" 2>&1; then
  resultado="OK"
else
  resultado="FALHOU"
fi

# Prova de qual banco foi usado + resumo, sem despejar o log inteiro no terminal.
sed -E 's/\x1b\[[0-9;]*m//g' "$LOG" | grep -o -E "Database dialect: [A-Za-z]+|\(MySQL [0-9.]+\)" | sort -u | sed 's/^/    /'
grep -E "^\[(INFO|ERROR)\] Tests run:" "$LOG" | tail -1 | sed 's/^/   /'
if [ "$resultado" = "OK" ]; then
  echo "    ✅ verde no MySQL (log completo: $LOG)"
else
  grep -E "^\[ERROR\]" "$LOG" | head -15
  echo "    ❌ falhou (log completo: $LOG)"
  exit 1
fi
