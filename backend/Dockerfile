FROM eclipse-temurin:17-jdk

WORKDIR /app

# Install FFmpeg for video processing
RUN apt-get update && apt-get install -y ffmpeg && rm -rf /var/lib/apt/lists/*

COPY pom.xml .
COPY src ./src
COPY src/main/resources ./src/main/resources

RUN apt-get update && apt-get install -y maven && \
    mvn clean package -DskipTests && \
    mv target/*.jar app.jar && \
    apt-get remove -y maven && apt-get autoremove -y

EXPOSE 8080

ENTRYPOINT ["java", "-jar", "app.jar"]