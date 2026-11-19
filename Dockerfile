FROM europe-north1-docker.pkg.dev/cgr-nav/pull-through/nav.no/jdk:openjdk-21@sha256:f84a3524ce1dca226cecb5354e87cb44900e2a3d2f30ac4476bda0657eb90a93
# Healtcheck lokalt/test
COPY --from=busybox:stable-musl /bin/wget /usr/bin/wget

# Working dir for RUN, CMD, ENTRYPOINT, COPY and ADD (required because of nonroot user cannot run commands in root)
WORKDIR /app

COPY target/*.jar app.jar
COPY content content

CMD ["java", "-jar", "app.jar"]