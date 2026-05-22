# Multi-stage build keeps the final image slim.
FROM maven:3.9-eclipse-temurin-21 AS build
WORKDIR /workspace
COPY pom.xml .
COPY content-server/pom.xml content-server/pom.xml
COPY visual-search-api/pom.xml visual-search-api/pom.xml
COPY image-ingestion-service/pom.xml image-ingestion-service/pom.xml
COPY content-server/src content-server/src
COPY content-server/seed-images content-server/seed-images
RUN mvn -pl content-server -am -DskipTests package

FROM eclipse-temurin:21-jre
WORKDIR /app
COPY --from=build /workspace/content-server/target/*.jar app.jar
COPY content-server/seed-images /app/seed-images
EXPOSE 8083
ENV SNAP2STAY_CONTENTSERVER_IMAGESDIR=/app/seed-images
ENTRYPOINT ["java", "-jar", "app.jar"]
