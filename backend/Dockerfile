FROM maven:3.9-eclipse-temurin-17 AS builder
WORKDIR /app

RUN mkdir -p /app/.. && echo '<?xml version="1.0" encoding="UTF-8"?><project xmlns="http://maven.apache.org/POM/4.0.0" xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance" xsi:schemaLocation="http://maven.apache.org/POM/4.0.0 https://maven.apache.org/xsd/maven-4.0.0.xsd"><modelVersion>4.0.0</modelVersion><groupId>com.platform</groupId><artifactId>platform</artifactId><version>1.0.0</version><packaging>pom</packaging><properties><java.version>17</java.version><spring-boot.version>3.2.5</spring-boot.version><spring-cloud.version>2023.0.1</spring-cloud.version></properties><dependencyManagement><dependencies><dependency><groupId>org.springframework.boot</groupId><artifactId>spring-boot-dependencies</artifactId><version>${spring-boot.version}</version><type>pom</type><scope>import</scope></dependency><dependency><groupId>org.springframework.cloud</groupId><artifactId>spring-cloud-dependencies</artifactId><version>${spring-cloud.version}</version><type>pom</type><scope>import</scope></dependency></dependencies></dependencyManagement><build><pluginManagement><plugins><plugin><groupId>org.springframework.boot</groupId><artifactId>spring-boot-maven-plugin</artifactId><version>${spring-boot.version}</version></plugin></plugins></pluginManagement></build></project>' > /app/../pom.xml

COPY pom.xml .
COPY backend/src backend/src

RUN mvn dependency:go-offline

RUN mvn clean package -Dmaven.test.skip=true

FROM eclipse-temurin:17-jre-alpine
WORKDIR /app
RUN apk add --no-cache curl ffmpeg
COPY --from=builder /app/target/*.jar app.jar
EXPOSE 8081
HEALTHCHECK --interval=30s --timeout=3s --start-period=10s --retries=3   CMD curl -f http://localhost:8081/api/health || exit 1
ENV JAVA_TOOL_OPTIONS="-Xms512m -Xmx512m"
ENTRYPOINT ["java", "-jar", "app.jar"]
