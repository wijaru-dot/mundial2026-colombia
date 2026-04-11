FROM eclipse-temurin:17-jdk-alpine
WORKDIR /app

COPY . .

RUN chmod +x ./mvnw
RUN ./mvnw clean package -DskipTests --no-transfer-progress

# Esta línea es la clave: ejecuta cualquier JAR que se genere en target/
CMD ["sh", "-c", "java -jar target/*.jar"]