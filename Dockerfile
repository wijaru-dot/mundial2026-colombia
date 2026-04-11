FROM eclipse-temurin:17-jdk-alpine
WORKDIR /app

# Copiar todo el código fuente
COPY . .

# Dar permisos al Maven Wrapper y compilar
RUN chmod +x ./mvnw
RUN ./mvnw clean package -DskipTests --no-transfer-progress

# Ejecutar la aplicación
CMD ["java", "-jar", "target/app.jar"]