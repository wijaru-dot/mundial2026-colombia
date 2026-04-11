FROM eclipse-temurin:17-jdk-alpine
WORKDIR /app

# Copiar todo el código fuente
COPY . .

# Dar permisos al Maven Wrapper y compilar (esto genera el JAR dentro del contenedor)
RUN chmod +x ./mvnw
RUN ./mvnw clean package -DskipTests --no-transfer-progress

# Ejecutar el JAR que genera Spring Boot (busca cualquier .jar en target/)
CMD ["sh", "-c", "java -jar target/*.jar"]