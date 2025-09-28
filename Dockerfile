
FROM eclipse-temurin:24-jdk AS build
WORKDIR /workspace

RUN apt-get update && apt-get install -y unzip && rm -rf /var/lib/apt/lists/*

COPY gradlew ./
COPY gradle ./gradle
COPY settings.gradle.kts build.gradle.kts gradle.properties ./

COPY src ./src
COPY example-app ./example-app

RUN chmod +x gradlew

RUN ./gradlew :example-app:bootJar -x test --no-daemon

RUN JAR_FILE=$(ls example-app/build/libs/*.jar | head -n 1) && cp "$JAR_FILE" /app.jar

FROM eclipse-temurin:24-jdk
WORKDIR /app

COPY --from=build /app.jar /app/app.jar

EXPOSE 8080

ENV JAVA_OPTS=""

ENTRYPOINT ["sh", "-c", "java $JAVA_OPTS -jar /app/app.jar"]