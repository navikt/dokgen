FROM ghcr.io/navikt/fp-baseimages/java:21
COPY target/*.jar app.jar
COPY content content
