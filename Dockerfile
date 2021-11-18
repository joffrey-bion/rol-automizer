FROM openjdk:17-jdk-alpine as packager

ENV JAVA_MINIMAL="/opt/java-minimal"

# build minimal JRE
RUN jlink \
    --verbose \
    --add-modules java.base \
    --add-modules java.desktop \
    --add-modules java.naming \
    --add-modules java.net.http \
    --compress 2 --strip-java-debug-attributes --no-header-files --no-man-pages \
    --output "$JAVA_MINIMAL"

FROM alpine:latest

ENV JAVA_HOME=/opt/java-minimal
ENV PATH="$PATH:$JAVA_HOME/bin"

COPY --from=packager "$JAVA_HOME" "$JAVA_HOME"

ARG JAR_FILE=build/libs/rol-automizer.jar
COPY ${JAR_FILE} app.jar

ENTRYPOINT ["java", "-jar", "/app.jar"]
