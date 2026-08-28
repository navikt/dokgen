FROM europe-north1-docker.pkg.dev/cgr-nav/pull-through/nav.no/jdk:openjdk-21@sha256:74265fab0bc107e6e0d509d80406217e69db30174cbf2628cd57598611efb52e
# Healtcheck lokalt/test
COPY --from=busybox:stable-musl /bin/wget /usr/bin/wget

# Working dir for RUN, CMD, ENTRYPOINT, COPY and ADD (required because of nonroot user cannot run commands in root)
WORKDIR /app

COPY target/*.jar app.jar
COPY content content

CMD ["java", "-jar", "app.jar"]