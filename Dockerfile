FROM navikt/java:11
COPY target/*.jar app.jar
COPY content content