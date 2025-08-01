FROM --platform=$BUILDPLATFORM debian:bullseye-slim AS project-build

# Install build dependencies
RUN \
  apt-get update && \
  apt-get install -y --no-install-recommends openjdk-21-jdk maven unzip chromium git && \
  # Workaround Chromium binary path for arm64 (see https://github.com/puppeteer/puppeteer/blob/v4.0.0/src/Launcher.ts#L110)
  ln -s /usr/bin/chromium /usr/bin/chromium-browser

# Configure headless Chromium for Puppeteer
ENV PUPPETEER_SKIP_CHROMIUM_DOWNLOAD=true
ENV PUPPETEER_EXECUTABLE_PATH=/usr/bin/chromium

# Copy project files
WORKDIR /project
COPY . .

# Perform actual Wren:AM build
ARG MAVEN_BUILD_ARGS
RUN \
  --mount=type=cache,target=/root/.m2 \
  --mount=type=cache,target=/root/.npm \
  mvn package ${MAVEN_BUILD_ARGS}

# Copy built artifacts into target directory
RUN \
  mkdir /build && \
  mvn -Dexpression=project.version -q -DforceStdout help:evaluate > /build/version.txt && \
  unzip openam-server/target/WrenAM-$(cat /build/version.txt).war -d /build/wrenam && \
  unzip openam-distribution/openam-distribution-ssoadmintools/target/SSOAdminTools-$(cat /build/version.txt).zip -d /build/ssoadm && \
  unzip openam-distribution/openam-distribution-ssoconfiguratortools/target/SSOConfiguratorTools-$(cat /build/version.txt).zip -d /build/ssoconf


FROM tomcat:10-jdk17-temurin

# Set environment variables
ENV \
  WRENAM_HOME="/srv/wrenam" \
  JAVA_OPTS=" \
    --add-exports=java.base/sun.security.tools.keytool=ALL-UNNAMED \
    --add-exports=java.base/sun.security.x509=ALL-UNNAMED \
    --add-exports=java.management/sun.management=ALL-UNNAMED \
    --add-exports=java.xml/com.sun.org.apache.xerces.internal.dom=ALL-UNNAMED \
    --add-opens=java.base/java.io=ALL-UNNAMED \
    --add-opens=java.base/java.lang.reflect=ALL-UNNAMED \
    --add-opens=java.base/java.net=ALL-UNNAMED \
    --add-opens=java.base/java.util.regex=ALL-UNNAMED \
    --add-opens=java.base/sun.nio.ch=ALL-UNNAMED \
    -Dcom.sun.identity.configuration.directory=/srv/wrenam \
  " \
  CATALINA_OPTS="-server -Xmx2g -XX:MetaspaceSize=256m -XX:MaxMetaspaceSize=256m"

ARG WRENAM_UID=1000

# Deploy wrenam project
ARG WRENAM_CONTEXT=auth
COPY --chown=$WRENAM_UID:root --from=project-build /build/wrenam /usr/local/tomcat/webapps/${WRENAM_CONTEXT}
COPY --chown=$WRENAM_UID:root --from=project-build /build/ssoadm /opt/ssoadm
COPY --chown=$WRENAM_UID:root --from=project-build /build/ssoconf /opt/ssoconf

USER ${WRENAM_UID}
WORKDIR ${WRENAM_HOME}

# Prepare Wren:AM configuration directory
RUN mkdir -p $WRENAM_HOME
VOLUME $WRENAM_HOME

CMD ["catalina.sh", "run"]
