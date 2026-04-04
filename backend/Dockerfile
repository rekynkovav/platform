FROM maven:3.9-eclipse-temurin-21 AS builder
WORKDIR /app
# Копируем pom.xml из корня проекта
COPY pom.xml .
RUN mvn dependency:go-offline
# Копируем исходники из правильных путей
COPY backend/src src
RUN mvn clean package -DskipTests

FROM eclipse-temurin:21-jre-alpine
WORKDIR /app
RUN apk add --no-cache curl
COPY --from=builder /app/target/*.jar app.jar
EXPOSE 8081
ENTRYPOINT ["java", "-jar", "app.jar"]