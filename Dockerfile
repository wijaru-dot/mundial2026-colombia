FROM eclipse-temurin:17-jdk-alpine
WORKDIR /app

# Copiar todo el código
COPY . .

# Mostrar qué hay en la carpeta antes de compilar (para diagnóstico)
RUN ls -la

# Dar permisos y compilar
RUN chmod +x ./mvnw
RUN ./mvnw clean package -DskipTests --no-transfer-progress

# Mostrar qué se generó
RUN ls -la target/

# Copiar el JAR
COPY target/app app.jar

EXPOSE 8080
ENTRYPOINT ["java", "-jar", "app.jar"]