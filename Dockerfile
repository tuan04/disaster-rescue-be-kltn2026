# syntax=docker/dockerfile:1
FROM maven:3.9.9-eclipse-temurin-21 AS build
WORKDIR /workspace
COPY pom.xml ./
COPY core-service/ core-service/
COPY infrastructure/ infrastructure/

# GitHub Actions can override MODULE to select another Spring Boot application.
ARG MODULE=core-service/dispatch-service
RUN --mount=type=cache,target=/root/.m2/repository \
    test -f "$MODULE/pom.xml" && test "$MODULE" != "core-service/common-lib" && \
    mvn -B -ntp -pl "$MODULE" -am -DskipTests package && \
    cp "$MODULE"/target/*.jar /tmp/app.jar

FROM eclipse-temurin:21-jre
WORKDIR /app
COPY --from=build /tmp/app.jar app.jar
USER 10001:10001
ENTRYPOINT ["java", "-jar", "/app/app.jar"]
