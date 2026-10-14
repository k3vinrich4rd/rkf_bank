#!/bin/bash
# Executa os testes funcionais e E2E do RKF Bank com ambiente limpo.
# Uso: ./run-tests.sh [NomeDaClasse]
# Exemplos:
# ./run-tests.sh # roda todos os testes
# ./run-tests.sh RkfBankApplicationTests # roda so essa classe de teste

set -e
cd "$(dirname "$0")"

TEST_CLASS="${1:-}"

echo ">>> Derrubando ambiente anterior do banco..."
docker compose down -v 2>/dev/null || true

echo ">>> Subindo banco de dados limpo (MySQL)..."
docker compose up -d

echo ">>> Aguardando rkfbank-mysql ficar healthy..."
while [ "$(docker inspect --format='{{.State.Health.Status}}' rkfbank-mysql 2>/dev/null)" != "healthy" ]; do
  sleep 3
done
echo ">>> Banco MySQL healthy!"

# JDK 25 da maquina (mesmo exigido pelo maven-enforcer-plugin no pom.xml)
export JAVA_HOME='/c/Users/Estudo$/.jdks/ms-25.0.4'
export PATH="$JAVA_HOME/bin:$PATH"

if [ -n "$TEST_CLASS" ]; then
  echo ">>> Rodando teste especifico: $TEST_CLASS"
  ./mvnw clean test -Dtest="$TEST_CLASS" -Dspring.profiles.active=test
else
  echo ">>> Rodando todos os testes do projeto"
  ./mvnw clean test -Dspring.profiles.active=test
fi
