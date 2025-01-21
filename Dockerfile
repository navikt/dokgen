FROM ghcr.io/navikt/fp-baseimages/distroless:21

COPY target/*.jar app.jar
COPY content content

CMD ["app.jar"]