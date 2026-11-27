FROM eclipse-temurin:21-jdk
EXPOSE 5000

COPY build/libs/app.jar app.jar
CMD ["java", "-jar", "app.jar"]
