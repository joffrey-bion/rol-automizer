FROM openjdk:16-alpine as packager

COPY . project/

RUN cd project && ./gradlew jlink

FROM alpine:3.7

COPY --from=packager project/build/jlink dist/
ENTRYPOINT ["dist/bin/rolAutomizer"]
