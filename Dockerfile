FROM eclipse-temurin:17-jdk-alpine
WORKDIR /app

COPY . .

RUN chmod +x ./mvnw
RUN ./mvnw clean package -DskipTests --no-transfer-progress

# Línea clave recomendada por Railway: ejecuta cualquier JAR en target/
CMD ["sh", "-c", "java -jar target/*.jar"]