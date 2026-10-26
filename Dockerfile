# Imagem da API do RKF Bank para publicar (perfil prod). Fica na raiz porque o pom.xml e o mvnw moram aqui.
#
#   docker build -t rkfbank-api .
#   docker run -p 8080:8080 --env-file deploy.env rkfbank-api      (variaveis em backend/README_DEPLOY.md)
#
# Dois estagios: o primeiro compila com o JDK e o Maven; o segundo leva so o .jar e um JRE, sem codigo-fonte,
# Maven nem cache: imagem menor e com menos coisa para atacar. Os testes nao rodam aqui (o CI ja roda os 307).

FROM eclipse-temurin:25-jdk AS build
WORKDIR /app

# Primeiro so o que define as dependencias: enquanto o pom.xml nao mudar, o Docker reaproveita esta camada
# e nao baixa a internet de novo a cada mudanca no codigo.
COPY mvnw pom.xml ./
COPY .mvn .mvn
RUN chmod +x mvnw && ./mvnw -B -ntp dependency:go-offline

COPY backend/src/main backend/src/main
RUN ./mvnw -B -ntp package -DskipTests -Djacoco.skip=true \
    && cp target/rkfbank-*.jar app.jar

FROM eclipse-temurin:25-jre
WORKDIR /app

# Usuario sem privilegio: se alguem explorar a API, nao vira root dentro do container.
RUN groupadd --system rkfbank && useradd --system --gid rkfbank --no-create-home rkfbank
COPY --from=build --chown=rkfbank:rkfbank /app/app.jar app.jar
USER rkfbank

# Planos baratos tem 512 MB: o Java usa ate 75% da memoria do container (o resto fica para o sistema).
ENV SPRING_PROFILES_ACTIVE=prod \
    JAVA_TOOL_OPTIONS="-XX:MaxRAMPercentage=75 -XX:+ExitOnOutOfMemoryError" \
    TZ=America/Sao_Paulo

EXPOSE 8080
# O provedor confere a saude por HTTP em /actuator/health (publico, so UP/DOWN).
ENTRYPOINT ["java", "-jar", "/app/app.jar"]
