FROM eclipse-temurin:17-jdk-alpine
WORKDIR /app

COPY . .

RUN chmod +x ./mvnw
RUN ./mvnw clean package -DskipTests --no-transfer-progress

CMD ["java", "-jar", "target/app.jar"]