FROM europe-north1-docker.pkg.dev/cgr-nav/pull-through/nav.no/jdk:openjdk-21@sha256:5f5dbd0edfccb4e0616253da3d4454438d8191ccc4b4778e6f01bf946f47b3fb
# Healtcheck lokalt/test
COPY --from=busybox:stable-musl /bin/wget /usr/bin/wget

# Working dir for RUN, CMD, ENTRYPOINT, COPY and ADD (required because of nonroot user cannot run commands in root)
WORKDIR /app

COPY target/*.jar app.jar
COPY content content

CMD ["java", "-jar", "app.jar"]