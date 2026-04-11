FROM eclipse-temurin:17-jdk-alpine AS builder
WORKDIR /app

# Copiar todo el proyecto
COPY . .

# Dar permisos de ejecución al Maven Wrapper
RUN chmod +x ./mvnw

# Compilar el proyecto
RUN ./mvnw clean package -DskipTests

# Imagen final más ligera
FROM eclipse-temurin:17-jre-alpine
WORKDIR /app
COPY --from=builder /app/target/*.jar app.jar

EXPOSE 8080
ENTRYPOINT ["java", "-jar", "app.jar"]