FROM openjdk:16-alpine

ARG JAR_FILE=build/libs/rol-automizer.jar
COPY ${JAR_FILE} app.jar

ENTRYPOINT ["java", "-jar", "/app.jar"]
