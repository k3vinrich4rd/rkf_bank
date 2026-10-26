#!/bin/bash
# Executa os testes funcionais e E2E do RKF Bank com ambiente limpo.
# Uso: ./run-test.sh [NomeDaClasse]  (APAGA os dados do MySQL local: docker compose down -v)
# Exemplos:
# ./run-test.sh # roda todos os testes
# ./run-test.sh RkfbankApplicationTests # roda so essa classe de teste

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

# JDK 25 (mesmo exigido pelo maven-enforcer-plugin no pom.xml): o baixado pelo IntelliJ, se existir nesta maquina;
# senao, o JAVA_HOME/java que ja estiver configurado.
JDK_INTELLIJ="$HOME/.jdks/ms-25.0.4"
if [ -d "$JDK_INTELLIJ" ]; then
  export JAVA_HOME="$JDK_INTELLIJ"
  export PATH="$JAVA_HOME/bin:$PATH"
fi

if [ -n "$TEST_CLASS" ]; then
  echo ">>> Rodando teste especifico: $TEST_CLASS"
  ./mvnw clean test -Dtest="$TEST_CLASS" -Dspring.profiles.active=test
else
  echo ">>> Rodando todos os testes do projeto"
  ./mvnw clean test -Dspring.profiles.active=test
fi
