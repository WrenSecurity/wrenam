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
# Copyright 2026 Wren Security. All rights reserved.
#

# Wren:AM configuration directory (location of the bootstrap file)
export CATALINA_OPTS="${CATALINA_OPTS:-} -Dcom.sun.identity.configuration.directory=$WRENAM_BASE_DIR"

# Run background configuration script unless disabled
if [[ -z "${WRENAM_INIT_DISABLE:-}" ]]; then
  /opt/wrenam/config.sh &
fi

# The admin password is needed only by the configuration script, keep it away from Tomcat
if [[ -n "${WRENAM_ADMIN_PASSWORD:-}" ]]; then
  echo "[INIT] WARNING: WRENAM_ADMIN_PASSWORD is visible in the container configuration," \
    "use WRENAM_ADMIN_PASSWORD_FILE with a mounted secret in production" >&2
  unset WRENAM_ADMIN_PASSWORD
fi

# Continue with the standard Tomcat startup (replacing this shell as PID 1)
exec "$@"
