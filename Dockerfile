FROM navikt/java:17
COPY target/*.jar app.jar
COPY content content