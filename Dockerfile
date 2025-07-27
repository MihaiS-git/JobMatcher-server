#Build Stage
FROM eclipse-temurin:21-jdk-alpine AS builder

WORKDIR /app

COPY gradlew settings.gradle build.gradle /app/
COPY gradle /app/gradle
COPY src /app/src

RUN chmod +x gradlew && ./gradlew build --no-daemon -x test

#Run Stage
FROM eclipse-temurin:21-jdk-alpine

WORKDIR /app

# Install libwebp
RUN apk add --no-cache libwebp-tools

COPY --from=builder /app/build/libs/server-0.0.1-SNAPSHOT.jar /app/app.jar

EXPOSE 8080

ENTRYPOINT ["java", "-XX:+UseContainerSupport", "-XX:MaxRAMPercentage=75", "-XX:+UseG1GC", "-Dfile.encoding=UTF-8", "-jar", "/app/app.jar"]
