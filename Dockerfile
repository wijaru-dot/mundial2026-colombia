FROM eclipse-temurin:17-jdk-alpine AS builder
WORKDIR /app

COPY . .
RUN chmod +x ./mvnw
RUN ./mvnw clean package -DskipTests --no-transfer-progress

FROM eclipse-temurin:17-jre-alpine
WORKDIR /app

COPY --from=builder /app/target/mundial2026-orquesta-1.0* app.jar

EXPOSE 8080
ENTRYPOINT ["java", "-jar", "app.jar"]