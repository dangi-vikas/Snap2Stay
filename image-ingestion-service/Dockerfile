FROM maven:3.9-eclipse-temurin-21 AS build
WORKDIR /workspace
COPY pom.xml .
COPY image-ingestion-service/pom.xml image-ingestion-service/pom.xml
COPY content-server/pom.xml content-server/pom.xml
COPY visual-search-api/pom.xml visual-search-api/pom.xml
COPY image-ingestion-service/src image-ingestion-service/src
RUN mvn -pl image-ingestion-service -am -DskipTests package

FROM eclipse-temurin:21-jre
WORKDIR /app
COPY --from=build /workspace/image-ingestion-service/target/*.jar app.jar
ENTRYPOINT ["java", "-jar", "app.jar"]
