FROM maven:3.9-eclipse-temurin-17 AS builder
WORKDIR /app

# Копируем pom.xml родительского проекта и подмодуля backend
COPY pom.xml .
COPY backend/pom.xml backend/pom.xml

# Устанавливаем родительский POM в локальный репозиторий (чтобы разрешить наследование)
RUN mvn install -N

# Скачиваем зависимости (учитывая multi-module структуру)
RUN mvn dependency:go-offline -pl backend -am

# Копируем исходники backend
COPY backend/src backend/src

# Собираем только модуль backend (и его зависимости)
RUN mvn clean package -DskipTests -pl backend -am

# Runtime stage
FROM eclipse-temurin:17-jre-alpine
WORKDIR /app
RUN apk add --no-cache curl
COPY --from=builder /app/backend/target/*.jar app.jar
EXPOSE 8081
HEALTHCHECK --interval=30s --timeout=3s --start-period=10s --retries=3 \
  CMD curl -f http://localhost:8081/actuator/health || exit 1
ENTRYPOINT ["java", "-jar", "app.jar"]
