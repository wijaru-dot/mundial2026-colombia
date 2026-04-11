FROM eclipse-temurin:17-jre-alpine
WORKDIR /app

# Copiamos el archivo 'app' que generó Spring Boot
COPY target/app app.jar

EXPOSE 8080
ENTRYPOINT ["java", "-jar", "app.jar"]