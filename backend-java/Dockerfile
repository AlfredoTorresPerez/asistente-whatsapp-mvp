# syntax=docker/dockerfile:1.7
# La etapa de compilacion usa una imagen con Maven preinstalado para evitar que
# Maven Wrapper tenga que descargar apache-maven durante el build de Docker.
FROM maven:3.9.9-eclipse-temurin-21 AS build

WORKDIR /app

ENV MAVEN_OPTS="-Djava.net.preferIPv4Stack=true -Dmaven.wagon.http.retryHandler.count=5 -Dmaven.wagon.httpconnectionManager.ttlSeconds=120"

COPY pom.xml pom.xml

RUN --mount=type=cache,target=/root/.m2 \
    for attempt in 1 2 3; do \
      mvn -B -ntp -q -DskipTests dependency:go-offline && break; \
      if [ "$attempt" = "3" ]; then exit 1; fi; \
      echo "Maven dependency resolution failed. Retrying in 15 seconds..."; \
      sleep 15; \
    done

COPY src src

RUN --mount=type=cache,target=/root/.m2 \
    for attempt in 1 2 3; do \
      mvn -B -ntp -q -DskipTests -Dmaven.test.skip=true package && break; \
      if [ "$attempt" = "3" ]; then exit 1; fi; \
      echo "Maven package failed. Retrying in 15 seconds..."; \
      sleep 15; \
    done

FROM eclipse-temurin:21-jre-jammy

WORKDIR /app

COPY --from=build /app/target/backend-java-0.0.1-SNAPSHOT.jar app.jar

EXPOSE 8080

CMD ["sh", "-c", "java ${JAVA_OPTS} -jar /app/app.jar"]
