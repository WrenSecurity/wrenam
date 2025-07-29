#
# The contents of this file are subject to the terms of the Common Development and
# Distribution License (the License). You may not use this file except in compliance with the
# License.
#
# You can obtain a copy of the License at legal/CDDLv1.1.txt. See the License for the
# specific language governing permission and limitations under the License.
#
# When distributing Covered Software, include this CDDL Header Notice in each file and include
# the License file at legal/CDDLv1.1.txt. If applicable, add the following below the CDDL
# Header, with the fields enclosed by brackets [] replaced by your own identifying
# information: "Portions copyright [year] [name of copyright owner]".
#
# Copyright 2025 Wren Security
#

#
# Stage building project artifacts:
#
# - Wren:AM Server
# - Wren:AM SSO Configuration Tools
# - Wren:AM SSO Admin Tools
#
FROM --platform=$BUILDPLATFORM debian:bullseye-slim AS project-build

# Install build dependencies
RUN \
  apt-get update && \
  apt-get install -y --no-install-recommends openjdk-21-jdk maven unzip chromium git && \
  # Workaround Chromium binary path for arm64 (see https://github.com/puppeteer/puppeteer/blob/v4.0.0/src/Launcher.ts#L110)
  ln -s /usr/bin/chromium /usr/bin/chromium-browser

# Configure headless Chromium for Puppeteer
ENV PUPPETEER_SKIP_CHROMIUM_DOWNLOAD=true \
    PUPPETEER_EXECUTABLE_PATH=/usr/bin/chromium

# Copy project files
WORKDIR /project
COPY . .

# Build the project
ARG MAVEN_BUILD_ARGS
RUN \
  --mount=type=cache,target=/root/.m2 \
  --mount=type=cache,target=/root/.npm \
  mvn package ${MAVEN_BUILD_ARGS}

# Prepare output artifacts
RUN \
  mkdir /build && \
  mvn -Dexpression=project.version -q -DforceStdout help:evaluate > /build/version.txt && \
  unzip openam-server/target/WrenAM-$(cat /build/version.txt).war -d /build/wrenam && \
  unzip openam-distribution/openam-distribution-ssoadmintools/target/SSOAdminTools-$(cat /build/version.txt).zip -d /build/ssoadm && \
  unzip openam-distribution/openam-distribution-ssoconfiguratortools/target/SSOConfiguratorTools-$(cat /build/version.txt).zip -d /build/ssoconf


#
# Target runtime stage with the following artifacts:
#   
# - Wren:AM Server web application inside Tomcat servlet container
# - Wren:AM SSO Configuration Tools in `/opt/ssoconf`
# - Wren:AM SSO Admin Tools in `/opt/ssoadm` 
#
FROM tomcat:9-jdk17-temurin

# Runtime UID of the Tomcat user
ARG WRENAM_UID=1000

# Wren:AM servlet context path
ARG WRENAM_CONTEXT=auth

# Wren:AM base (work) directory
ARG WRENAM_HOME=/srv/wrenam

# Set environment variables
ENV \
  # Base directory for Wren:AM server configuration
  WRENAM_BASE_DIR="$WRENAM_HOME" \
  # Base directory for Wren:AM server init files
  WRENAM_INIT_DIR="/srv/wrenam-init" \
  # Servlet context URI for the Wren:AM server 
  WRENAM_DEPLOYMENT_URI="/$WRENAM_CONTEXT" \
  # Server instance base URL
  WRENAM_SERVER_URL="http://wrenam.wrensecurity.local:8080" \
  # Shared JVM options required by the Wren:AM server and associated tools
  JAVA_OPTS=" \
    --add-exports=java.base/sun.security.tools.keytool=ALL-UNNAMED \
    --add-exports=java.base/sun.security.x509=ALL-UNNAMED \
    --add-exports=java.management/sun.management=ALL-UNNAMED \
    --add-exports=java.xml/com.sun.org.apache.xerces.internal.dom=ALL-UNNAMED \
    --add-opens=java.base/java.lang.reflect=ALL-UNNAMED \
    --add-opens=java.base/java.net=ALL-UNNAMED \
    --add-opens=java.base/java.util.regex=ALL-UNNAMED \
    -Dcom.sun.identity.configuration.directory=/srv/wrenam \
  " \
  # JVM options for the Tomcat server
  CATALINA_OPTS="-server -Xmx2g -XX:MetaspaceSize=256m -XX:MaxMetaspaceSize=256m"

# Copy Wren:AM artifacts
COPY --chown=$WRENAM_UID:root --from=project-build /build/wrenam /usr/local/tomcat/webapps/${WRENAM_CONTEXT}
COPY --chown=$WRENAM_UID:root --from=project-build /build/ssoadm /opt/ssoadm
COPY --chown=$WRENAM_UID:root --from=project-build /build/ssoconf /opt/ssoconf

# Copy additional Wren:AM container files
COPY --chown=$WRENAM_UID:root .docker/ssoadm /usr/local/bin/ssoadm
COPY --chown=$WRENAM_UID:root .docker/wrenam /opt/wrenam
COPY --chown=$WRENAM_UID:root .docker/sample /srv/wrenam-init/sample

# Switch to the runtime user
USER $WRENAM_UID
WORKDIR $WRENAM_HOME

# Prepare Wren:AM configuration directory
RUN mkdir -p $WRENAM_HOME
VOLUME $WRENAM_HOME

ENTRYPOINT ["/opt/wrenam/entrypoint.sh"]
CMD ["catalina.sh", "run"]
