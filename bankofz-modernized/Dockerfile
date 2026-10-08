# ---- Build stage ----
FROM maven:3.9-eclipse-temurin-21 AS build
WORKDIR /workspace
COPY pom.xml .
RUN mvn -q -B dependency:go-offline
COPY src ./src
RUN mvn -q -B clean package -DskipTests

# ---- Runtime stage ----
FROM eclipse-temurin:21-jre
WORKDIR /app
RUN useradd --system --uid 1001 bankofz
COPY --from=build /workspace/target/bankofz-modernized-*.jar app.jar
USER bankofz
EXPOSE 8080
ENTRYPOINT ["java", "-jar", "/app/app.jar"]
