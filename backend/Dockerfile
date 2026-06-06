# Stage 1: Build
FROM maven:3.9-eclipse-temurin-17 AS builder
WORKDIR /app
COPY pom.xml .
RUN mvn dependency:go-offline -B
# pom.xml has <sourceDirectory>backend/src/main/java</sourceDirectory>
# so we must preserve the backend/ prefix inside the container
COPY backend/src ./backend/src
RUN mvn clean package -DskipTests

# Stage 2: Runtime
FROM eclipse-temurin:17-jre-alpine
WORKDIR /app

# Install FFmpeg for video processing (before switching user)
RUN apk add --no-cache ffmpeg

# Create user and group FIRST, then create logs directory
RUN addgroup -S platform && adduser -S platform -G platform
RUN mkdir -p /app/logs && chown -R platform:platform /app/logs

USER platform

ENV JAVA_OPTS="-Xms512m -Xmx1024m -XX:+UseG1GC -XX:+UseStringDeduplication"

COPY --from=builder /app/target/platform-1.0.0.jar app.jar

EXPOSE 8081
ENTRYPOINT ["sh", "-c", "java $JAVA_OPTS -jar app.jar"]
