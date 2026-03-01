FROM eclipse-temurin:20-jdk-alpine

WORKDIR /app
COPY build/libs/*.jar app.jar
EXPOSE 8080 50051
ENTRYPOINT ["java","-jar","app.jar"]