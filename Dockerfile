FROM eclipse-temurin:17-jdk-alpine
WORKDIR /app

COPY . .

RUN chmod +x ./mvnw
RUN ./mvnw clean package -DskipTests --no-transfer-progress

# Esta es la línea recomendada por Railway
CMD ["sh", "-c", "java -jar target/*.jar"]