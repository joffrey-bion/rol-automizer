FROM amazoncorretto:17

ARG DIST_DIR=build/install
COPY ${DIST_DIR} /app

ENTRYPOINT ["/app/rol-automizer/bin/rol-automizer"]
