FROM europe-north1-docker.pkg.dev/cgr-nav/pull-through/nav.no/jdk:openjdk-21@sha256:2b2b44c00d9dd92dbc50b32c5ebb441e2725cfaba432be14ab16ba6f09651611
# Healtcheck lokalt/test
COPY --from=busybox:stable-musl /bin/wget /usr/bin/wget

# Working dir for RUN, CMD, ENTRYPOINT, COPY and ADD (required because of nonroot user cannot run commands in root)
WORKDIR /app

COPY target/*.jar app.jar
COPY content content

CMD ["java", "-jar", "app.jar"]