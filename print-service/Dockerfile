# syntax=docker/dockerfile:1
# Build context is the repository root (see docker-compose.yml), because the module needs the parent pom.

FROM maven:3.9-eclipse-temurin-21 AS build
WORKDIR /workspace
COPY pom.xml .
COPY order-service/pom.xml order-service/
COPY processing-service/pom.xml processing-service/
COPY print-service/pom.xml print-service/
COPY print-service/src print-service/src
RUN --mount=type=cache,target=/root/.m2 mvn -B -q -pl print-service -am package -DskipTests

FROM eclipse-temurin:21-jre-alpine
WORKDIR /app
RUN addgroup -S app && adduser -S app -G app
COPY --from=build /workspace/print-service/target/print-service.jar app.jar
USER app
EXPOSE 8083
ENTRYPOINT ["java", "-XX:MaxRAMPercentage=75", "-jar", "app.jar"]
