FROM maven:3.9.9-eclipse-temurin-17 AS build
WORKDIR /workspace

COPY pom.xml pom.xml
COPY platform-common/pom.xml platform-common/pom.xml
COPY admin-was/pom.xml admin-was/pom.xml
COPY user-was/pom.xml user-was/pom.xml

COPY platform-common/src platform-common/src
COPY user-was/src user-was/src

RUN mvn -pl user-was -am -DskipTests package

FROM eclipse-temurin:17-jre-jammy
WORKDIR /app

RUN apt-get update \
    && apt-get install -y --no-install-recommends curl \
    && rm -rf /var/lib/apt/lists/*

RUN groupadd --system --gid 10001 app \
    && useradd --system --uid 10001 --gid app --create-home --home-dir /home/app app

COPY --from=build --chown=app:app /workspace/user-was/target/excel-import-user-was-0.0.1-SNAPSHOT.jar app.jar

USER app:app

EXPOSE 8080
ENTRYPOINT ["java", "-jar", "/app/app.jar"]
