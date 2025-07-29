#!/bin/bash -eu
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

# Wren:AM server instance base URL (SERVER_URL configuration parameter)
WRENAM_SERVER_URL=${WRENAM_SERVER_URL:-http://wrenam.wrensecurity.local:8080}

# Wren:AM servlet context path (DEPLOYMENT_URI configuration parameter)
WRENAM_DEPLOYMENT_URI=${WRENAM_DEPLOYMENT_URI:-/auth}

# Wren:AM base configuration directory (BASE_DIR configuration parameter)
WRENAM_BASE_DIR=${WRENAM_BASE_DIR:-/srv/wrenam}

# Wren:AM initial configuration directory
WRENAM_INIT_DIR=${WRENAM_INIT_DIR:-/srv/wrenam-init}

# Wren:AM amAdmin password file (used when WRENAM_ADMIN_PASSWORD is not set)
WRENAM_ADMIN_PASSWORD_FILE=${WRENAM_ADMIN_PASSWORD_FILE:-WRENAM_BASE_DIR/amadmin.pwd}

# Log initialization debug message
log_init() {
  echo "[INIT] $@"
}

# Fetch server status (response for the isAlive.jsp request)
check_am() {
  local response=$(
    curl -si --connect-timeout 5 $WRENAM_SERVER_URL$WRENAM_DEPLOYMENT_URI/isAlive.jsp 2> /dev/null
  )
  [[ ! -z "$response" ]] || exit 1
  echo "$response"
}

# Check initialization process halt condition
check_stop() {
  if ! ps 1 > /dev/null; then
    log_init "Missing server process, aborting initialization"
    exit 1
  fi
}

# Wait until the server is up and running
wait_startup() {
  while ! $(check_am > /dev/null); do
    log_init "Waiting Tomcat initialization..."
    sleep 2
    check_stop
  done
}

# Wait until the Wren:AM instance is fully configured
wait_alive() {
  while true; do
    $(check_am | grep "Server is ALIVE:" > /dev/null) && break
    log_init "Waiting for Wren:AM initialization..."
    sleep 2
    check_stop
  done
}

# Run SSO Configuration Tools
run_ssoconf() {
  local config_properties="$1"
  cd /opt/ssoconf
  java \
    -jar ./openam-configurator-tool.jar \
    --file "$config_properties"
  wait_alive
}

# Run SSO Admin Tools
run_ssoadm() {
  local config_batch="$1"
  local admin_password=$(
    ([[ ! -z "${WRENAM_ADMIN_PASSWORD:-}" ]] && echo -n "$WRENAM_ADMIN_PASSWORD") ||
    ([[ -f "$WRENAM_ADMIN_PASSWORD_FILE" ]] && cat "$WRENAM_ADMIN_PASSWORD_FILE")
  )
  if [[ -z "$admin_password" ]]; then
    log_init "Missing admin user password...\n" \
      "Make sure WRENAM_ADMIN_PASSWORD_FILE points to an existing password file " \
      "or that WRENAM_ADMIN_PASSWORD is set."
    exit 1
  fi
  cd "$WRENAM_INIT_DIR"
  ssoadm \
    do-batch \
    --adminid amadmin \
    --password-file <(echo -n "$admin_password") \
    --batchfile "$config_batch"
}

# Wait for Wren:AM startup
wait_startup

# Check if any configuration is needed
if [[ $(check_am | grep "Server is ALIVE:" > /dev/null) ]]; then
  log_init "Server is ALIVE, no configuration needed"
  exit
fi

# Check for configuration page redirect
if [[ ! $(check_am | grep 'Location: .*/config/options.htm') ]]; then
  log_init "Unexpected server response, aborting initialization"
  exit 1
fi

# Distinguish between first start and upgrade
if [[ ! -d "$WRENAM_BASE_DIR$WRENAM_DEPLOYMENT_URI" ]]; then
  init_config="$WRENAM_INIT_DIR/init.properties"
  if [[ -f "$init_config" ]]; then
    log_init "Initializing new Wren:AM server instance..."
    run_ssoconf "$WRENAM_INIT_DIR/init.properties"
    if [[ -f "$WRENAM_INIT_DIR/config.batch" ]]; then
      log_init "Configuring Wren:AM services..."
      run_ssoadm "$WRENAM_INIT_DIR/config.batch"
    fi
  else
    log_init "Ignoring init state (no $init_config)"
  fi
else
  upgrade_config="$WRENAM_INIT_DIR/upgrade.properties"
  if [[ -f "$upgrade_config" ]]; then
    log_init "Running Wren:AM upgrade..."
    run_ssoconf "$WRENAM_INIT_DIR/upgrade.properties"
    log_init "Reloading Wren:AM web application..."
    touch /usr/local/tomcat/webapps$WRENAM_DEPLOYMENT_URI/WEB-INF/web.xml
  else
    log_init "Ignoring upgrade state (no $upgrade_config)"
  fi
fi

log_init "Server configuration finished"
