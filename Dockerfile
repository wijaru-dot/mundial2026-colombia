FROM eclipse-temurin:17-jdk-alpine AS builder
WORKDIR /app

COPY . .
RUN chmod +x ./mvnw

# Compilar con Maven Wrapper
RUN ./mvnw clean package -DskipTests

# Imagen final
FROM eclipse-temurin:17-jre-alpine
WORKDIR /app

# Copiar cualquier archivo JAR que se haya generado (más flexible)
COPY --from=builder /app/target/*-1.0.jar app.jar

EXPOSE 8080
ENTRYPOINT ["java", "-jar", "app.jar"]