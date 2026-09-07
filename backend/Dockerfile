# --- Build stage ---
FROM eclipse-temurin:21-jdk AS build
WORKDIR /app

# Copy wrapper + pom first so dependency resolution is cached across builds when only src/ changes.
COPY mvnw pom.xml ./
COPY .mvn .mvn
RUN chmod +x mvnw && ./mvnw -q -B dependency:go-offline

COPY src src
RUN ./mvnw -q -B -DskipTests package && cp target/backend-*.jar app.jar

# --- Runtime stage ---
FROM eclipse-temurin:21-jre
WORKDIR /app
COPY --from=build /app/app.jar app.jar

# Railway injects PORT dynamically; application.properties reads it via ${PORT:${SERVER_PORT:8080}}.
EXPOSE 8080
ENTRYPOINT ["java", "-jar", "app.jar"]
