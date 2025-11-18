FROM europe-north1-docker.pkg.dev/cgr-nav/pull-through/nav.no/jdk:openjdk-21@sha256:ae53f1a9248c3b4c8addd13f1eae6824b76a0ca15a173a71cc3df010c8bccaee
# Healtcheck lokalt/test
COPY --from=busybox:stable-musl /bin/wget /usr/bin/wget

# Working dir for RUN, CMD, ENTRYPOINT, COPY and ADD (required because of nonroot user cannot run commands in root)
WORKDIR /app

COPY target/*.jar app.jar
COPY content content

CMD ["java", "-jar", "app.jar"]