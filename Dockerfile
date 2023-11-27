FROM ghcr.io/navikt/baseimages/temurin:21
COPY target/*.jar app.jar
COPY content content
