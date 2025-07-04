#Build Stage
FROM eclipse-temurin:21-jdk-alpine AS builder

WORKDIR /app

COPY gradle /app/gradle
COPY gradlew /app
COPY build.gradle /app
COPY settings.gradle /app
COPY src /app/src

RUN chmod +x gradlew

RUN ./gradlew build --no-daemon -x test

#Run Stage
FROM eclipse-temurin:21-jdk-alpine

WORKDIR /app

COPY --from=builder /app/build/libs/server-0.0.1-SNAPSHOT.jar /app/app.jar

EXPOSE 8080

ENTRYPOINT ["java", "-jar", "/app/app.jar"]