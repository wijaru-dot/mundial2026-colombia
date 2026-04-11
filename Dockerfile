FROM eclipse-temurin:17-jdk-alpine AS builder
WORKDIR /app

# Copiar todo
COPY . .

# Dar permisos y compilar
RUN chmod +x ./mvnw
RUN ./mvnw clean package -DskipTests -B --no-transfer-progress

# Imagen final
FROM eclipse-temurin:17-jre-alpine
WORKDIR /app

# Buscar y copiar cualquier JAR que Maven haya creado
COPY --from=builder /app/target/*.jar app.jar

EXPOSE 8080
ENTRYPOINT ["java", "-jar", "app.jar"]