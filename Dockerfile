FROM eclipse-temurin:17-jdk-alpine
WORKDIR /app

COPY . .

RUN echo "cache-bust-v3"
RUN chmod +x ./mvnw
RUN ./mvnw clean package -DskipTests --no-transfer-progress

CMD ["sh", "-c", "java -jar target/app.jar"]