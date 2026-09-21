#!/usr/bin/env bash
set -euo pipefail

# Automates the scriptable portions of local BusTime dev setup on macOS.
# Source of truth (update there first, this script should follow it):
#   https://camsys.atlassian.net/wiki/spaces/~623178576a6824006970af77/pages/3235020829
#
# Automates: MySQL install (Homebrew) + db/table, *.xml path rewrites,
#            Tomcat download + workspace dirs, context.xml datasource block, maven build.
#
# Stays manual (IntelliJ UI only — see checklist printed at the end):
#   IDE license activation, suggested plugins, Corretto 11 SDK, maven profile
#   checkboxes, Tomcat run configuration + deployment artifacts, TDM VPC access.

REPO_ROOT="$(cd "$(dirname "${BASH_SOURCE[0]}")/.." && pwd)"
TOMCAT_VERSION="8.5.91"
TOMCAT_DIR="$REPO_ROOT/src/iworkspace-servers/bustime/app"
TOMCAT_HOME="$TOMCAT_DIR/apache-tomcat-${TOMCAT_VERSION}"
DB_NAME="bustime"
DB_PASSWORD=""

log()  { printf '\n\033[1;34m==>\033[0m %s\n' "$1"; }
warn() { printf '\033[1;33mWARNING:\033[0m %s\n' "$1"; }

wait_for_mysql() {
  local tries=0
  until nc -z 127.0.0.1 3306 >/dev/null 2>&1 || [[ $tries -ge 15 ]]; do
    sleep 1
    tries=$((tries + 1))
  done
}

# Onboarding devs won't know a root password that was set before they got the
# machine — so instead of asking for the existing one, we force-set whatever
# password they choose, resetting via --skip-grant-tables if needed.
kill_stray_safe_mode_mysqld() {
  # A previous run that died mid-reset (or set -e aborting before cleanup ran)
  # can leave this behind indefinitely — it holds the datadir lock and the
  # socket, so the brew-managed instance can never bind again until it's gone.
  pkill -f -- "--skip-grant-tables --skip-networking" >/dev/null 2>&1 || true
  sleep 1
}

# Safety net: if this script exits for ANY reason while a safe-mode mysqld is
# running — normal completion, a 'set -e' abort, or you hitting Ctrl-C out of
# impatience — clean it up rather than leaving it to block port 3306 forever.
# A background '&' job doesn't get your terminal's Ctrl-C, only the foreground
# script does, so without this trap an interrupt orphans it every time.
trap kill_stray_safe_mode_mysqld EXIT

reset_root_password_unknown() {
  warn "Root already has a password we don't know — resetting it via MySQL safe mode"

  # Idempotent: clean up any orphan left by a previous failed reset before
  # starting a new one, so re-running the script after a bad run self-heals
  # instead of piling up stuck processes.
  kill_stray_safe_mode_mysqld

  log "Stopping the regular MySQL service"
  brew services stop mysql >/dev/null 2>&1 || true
  sleep 2

  log "Starting MySQL in safe mode (this can take up to ~20s)"
  mysqld_safe --skip-grant-tables --skip-networking >/tmp/mysqld_safe_reset.log 2>&1 &
  local safe_pid=$!

  local tries=0
  until mysql -u root -e "SELECT 1" >/dev/null 2>&1 || [[ $tries -ge 20 ]]; do
    sleep 1
    tries=$((tries + 1))
  done

  local reset_failed=false
  log "Safe mode is up — setting the new root password"
  if ! mysql -u root -e "FLUSH PRIVILEGES; ALTER USER 'root'@'localhost' IDENTIFIED BY '${DB_PASSWORD}'; FLUSH PRIVILEGES;"; then
    warn "Could not set the new password against the safe-mode instance."
    reset_failed=true
  fi

  # Cleanup always runs, whether or not the ALTER USER above succeeded —
  # this is what was missing before: a failure here used to trip 'set -e'
  # and exit the script before this teardown ever ran, orphaning mysqld.
  log "Shutting down safe mode"
  kill "$safe_pid" >/dev/null 2>&1 || true
  wait "$safe_pid" 2>/dev/null || true
  kill_stray_safe_mode_mysqld

  if [[ "$reset_failed" == true ]]; then
    exit 1
  fi

  log "Restarting MySQL normally"
  brew services start mysql >/dev/null 2>&1 || true
  wait_for_mysql
  if ! nc -z 127.0.0.1 3306 >/dev/null 2>&1; then
    warn "MySQL restarted but isn't reachable over TCP (127.0.0.1:3306)."
    warn "Check 'brew services list' and 'ps aux | grep mysqld' for a leftover --skip-networking process, or run scripts/restart-mysql-local.sh."
    exit 1
  fi
  log "MySQL is back up with the new password"
}

usage() {
  cat <<EOF
Usage: $(basename "$0") [options]

  --skip-mysql       Skip MySQL install + db/table creation
  --skip-tomcat      Skip Tomcat download + workspace dirs
  --skip-build       Skip the maven build
  --skip-opt-perms   Skip 'sudo chmod 777 /opt' (asked interactively otherwise)
  --yes              Don't prompt before the /opt chmod (assume yes)
  -h, --help         Show this help
EOF
}

SKIP_MYSQL=false
SKIP_TOMCAT=false
SKIP_BUILD=false
SKIP_OPT_PERMS=false
ASSUME_YES=false

for arg in "$@"; do
  case "$arg" in
    --skip-mysql) SKIP_MYSQL=true ;;
    --skip-tomcat) SKIP_TOMCAT=true ;;
    --skip-build) SKIP_BUILD=true ;;
    --skip-opt-perms) SKIP_OPT_PERMS=true ;;
    --yes) ASSUME_YES=true ;;
    -h|--help) usage; exit 0 ;;
    *) echo "Unknown option: $arg"; usage; exit 1 ;;
  esac
done

if [[ "$(uname)" != "Darwin" ]]; then
  warn "This script assumes macOS + Homebrew, per the onboarding doc. Proceeding anyway, but expect to hand-fix things."
fi

# 1. Prereqs -------------------------------------------------------------------
log "Checking prerequisites"
command -v brew >/dev/null || { echo "Homebrew not found. Install from https://brew.sh first."; exit 1; }
command -v mvn  >/dev/null || { echo "Maven not found. Install it (brew install maven) first."; exit 1; }
command -v java >/dev/null || warn "No 'java' on PATH — make sure Corretto 11 is set as your project SDK in IntelliJ."

# 2. MySQL -----------------------------------------------------------------------
if ! $SKIP_MYSQL; then
  log "Installing MySQL via Homebrew (if needed)"
  if ! brew list mysql >/dev/null 2>&1; then
    brew install mysql
  else
    echo "mysql already installed via brew"
  fi
  brew services start mysql >/dev/null 2>&1 || true
  wait_for_mysql

  read -r -s -p "Choose a local MySQL root password (works whether root has no password yet, or an old one you don't know): " DB_PASSWORD
  echo

  if mysql -u root -e "SELECT 1" >/dev/null 2>&1; then
    log "Root has no password yet — setting it"
    mysql -u root -e "ALTER USER 'root'@'localhost' IDENTIFIED BY '${DB_PASSWORD}'; FLUSH PRIVILEGES;"
  elif mysql -u root -p"${DB_PASSWORD}" -e "SELECT 1" >/dev/null 2>&1; then
    echo "That password already works — nothing to reset"
  else
    reset_root_password_unknown
  fi

  log "Creating '${DB_NAME}' database + obanyc_psas table (idempotent)"
  mysql -u root -p"${DB_PASSWORD}" -e "
    CREATE DATABASE IF NOT EXISTS ${DB_NAME};
    CREATE TABLE IF NOT EXISTS ${DB_NAME}.obanyc_psas (
      id bigint(20) NOT NULL AUTO_INCREMENT,
      text varchar(1024) NOT NULL,
      PRIMARY KEY (id)
    );
  "
else
  log "Skipping MySQL setup (--skip-mysql)"
fi

# 3. Global path replacements -----------------------------------------------------
log "Rewriting tomcat log + TDS bundle paths in *.xml"
find "$REPO_ROOT" -name "*.xml" -not -path "*/target/*" -not -path "*/.git/*" -print0 \
  | xargs -0 sed -i '' \
      -e 's#/var/log/tomcat8/#/tmp/logs/#g' \
      -e 's#/var/lib/obanyc/oba-tds-bundle#/opt/bustime/oba/tds-bundle#g'

# 4. /opt permissions ---------------------------------------------------------------
if ! $SKIP_OPT_PERMS; then
  DO_CHMOD=$ASSUME_YES
  if ! $ASSUME_YES; then
    warn "The onboarding doc calls for 'sudo chmod 777 /opt' — this makes /opt world-writable for ALL apps on this machine, not just BusTime."
    read -r -p "Run it anyway? [Y/N] " REPLY
    [[ "$REPLY" =~ ^[Yy]$ ]] && DO_CHMOD=true
  fi
  if [[ "$DO_CHMOD" == true ]]; then
    log "Running sudo chmod 777 /opt"
    sudo chmod 777 /opt/
  else
    log "Skipped /opt chmod — create/own the specific bustime subdirs manually if step 5 fails on permissions"
  fi
else
  log "Skipping /opt chmod (--skip-opt-perms)"
fi

# 5. Tomcat -----------------------------------------------------------------------
if ! $SKIP_TOMCAT; then
  log "Setting up Tomcat ${TOMCAT_VERSION}"
  mkdir -p "$TOMCAT_DIR"
  mkdir -p /opt/bustime/oba/bundles/builder
  mkdir -p /opt/bustime/oba/tds-bundle
  if [[ ! -d "$TOMCAT_HOME" ]]; then
    ( cd "$TOMCAT_DIR" \
      && curl -sSLO "https://archive.apache.org/dist/tomcat/tomcat-8/v${TOMCAT_VERSION}/bin/apache-tomcat-${TOMCAT_VERSION}.tar.gz" \
      && tar zxf "apache-tomcat-${TOMCAT_VERSION}.tar.gz" )
  else
    echo "Tomcat already present at $TOMCAT_HOME"
  fi
else
  log "Skipping Tomcat setup (--skip-tomcat)"
fi

# 6. context.xml datasource block -------------------------------------------------
CONTEXT_XML="$TOMCAT_HOME/conf/context.xml"
if [[ ! -f "$CONTEXT_XML" ]]; then
  # Only checking that $TOMCAT_HOME exists (step 5) misses this: if just this
  # one file gets deleted/renamed/lost, the directory check still passes and
  # this step used to just warn and leave it missing. Recreate Tomcat's
  # stock skeleton here so injection below always has a file to work with.
  log "context.xml missing — writing Tomcat's default skeleton before injecting the datasource block"
  mkdir -p "$(dirname "$CONTEXT_XML")"
  cat > "$CONTEXT_XML" <<'CTXEOF'
<?xml version="1.0" encoding="UTF-8"?>
<!--
  Licensed to the Apache Software Foundation (ASF) under one or more
  contributor license agreements.  See the NOTICE file distributed with
  this work for additional information regarding copyright ownership.
  The ASF licenses this file to You under the Apache License, Version 2.0
  (the "License"); you may not use this file except in compliance with
  the License.  You may obtain a copy of the License at

      http://www.apache.org/licenses/LICENSE-2.0

  Unless required by applicable law or agreed to in writing, software
  distributed under the License is distributed on an "AS IS" BASIS,
  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
  See the License for the specific language governing permissions and
  limitations under the License.
-->
<!-- The contents of this file will be loaded for each web application -->
<Context>

    <!-- Default set of monitored resources. If one of these changes, the    -->
    <!-- web application will be reloaded.                                   -->
    <WatchedResource>WEB-INF/web.xml</WatchedResource>
    <WatchedResource>${catalina.base}/conf/web.xml</WatchedResource>

    <!-- Uncomment this to disable session persistence across Tomcat restarts -->
    <!--
    <Manager pathname="" />
    -->
</Context>
CTXEOF
fi

NEEDS_INJECTION=true
if grep -q "jdbc/archiveDB" "$CONTEXT_XML"; then
  # The block existing isn't enough — if a later run reset MySQL's password
  # (or someone hand-edited context.xml, as happened here), the stored
  # password silently drifts out of sync with the real one and this file
  # never gets corrected. Only trust it if it still actually authenticates.
  if ! $SKIP_MYSQL && ! mysql -h 127.0.0.1 -P 3306 -u root -p"${DB_PASSWORD}" -e "SELECT 1" >/dev/null 2>&1; then
    warn "context.xml has a datasource block, but its stored password doesn't match MySQL's current root password — updating it"
    python3 - "$CONTEXT_XML" <<'STRIPEOF'
import re, sys
path = sys.argv[1]
with open(path) as f:
    content = f.read()
content = re.sub(r'\n?    <Resource name="jdbc/archiveDB".*?</Context>', '\n</Context>', content, flags=re.DOTALL)
with open(path, "w") as f:
    f.write(content)
STRIPEOF
  else
    echo "context.xml already has a working datasource block — leaving it alone"
    NEEDS_INJECTION=false
  fi
fi

if $NEEDS_INJECTION; then
  PW="${DB_PASSWORD:-CHANGE_ME}"
  [[ "$PW" == "CHANGE_ME" ]] && warn "No DB password known (mysql step was skipped) — writing CHANGE_ME placeholder into context.xml, edit it by hand."
  log "Injecting datasource + parameter block into context.xml"
  python3 - "$CONTEXT_XML" "$PW" <<'PYEOF'
import sys
path, pw = sys.argv[1], sys.argv[2]
block = f"""    <Resource name="jdbc/archiveDB"
              auth="Container"
              type="javax.sql.DataSource"
              maxIdle="3"
              username="root"
              password="{pw}"
              driverClassName="com.mysql.cj.jdbc.Driver"
              url="jdbc:mysql://localhost:3306/bustime?useSSL=false&amp;allowPublicKeyRetrieval=true" />
    <Resource name="jdbc/appDB"
              auth="Container"
              type="javax.sql.DataSource"
              maxIdle="3"
              username="root"
              password="{pw}"
              driverClassName="com.mysql.cj.jdbc.Driver"
              url="jdbc:mysql://localhost:3306/bustime?useSSL=false&amp;allowPublicKeyRetrieval=true" />
    <Parameter name="obanyc.environment" value="dev" override="false" />
    <Parameter name="admin.instanceId" value="localhost" override="false" />
    <Parameter name="admin.port" value="9999" override="false" />
    <Parameter name="admin.context" value="api" override="false" />
    <Parameter name="file.bundle.bucketName" value="/opt/bustime/oba/bundles/builder" override="false" />
    <Parameter name="obanyc.resource" value="onebusaway-sound" override="false" />
"""
with open(path) as f:
    content = f.read()
content = content.replace("</Context>", block + "</Context>")
with open(path, "w") as f:
    f.write(content)
PYEOF
fi

# 7. Build --------------------------------------------------------------------------
if ! $SKIP_BUILD; then
  log "Building (mvn clean install, skipping tests)"
  ( cd "$REPO_ROOT" && mvn clean install -T8 -DskipTests=true -Dlicense.skip=true -Pskip-integration-tests,local-single-port )
else
  log "Skipping build (--skip-build)"
fi

# 8. Manual checklist -----------------------------------------------------------------
cat <<'EOF'

================================================================
  Automated steps done. Finish setup manually in IntelliJ:
================================================================
  [ ] Help -> Manage Subscriptions -> Activate Subscription
  [ ] Plugins menu -> search "/suggested" -> install all
  [ ] File -> Project Structure -> SDK -> download Corretto 11 (restart IDE)
  [ ] Maven profiles panel (m icon): uncheck cloud, check eclipse-only,
      check local-single-port, uncheck include-integration-tests,
      check skip-integration-tests
  [ ] Run -> Edit Configurations -> + Tomcat Server (Local)
        Name: bustime-app
        Tomcat Home: src/iworkspace-servers/bustime/app/apache-tomcat-8.5.91
        Deployment tab: add these artifacts
          - onebusaway-nyc-transit-data-federation-webapp:war exploded
            -> context: /onebusaway-nyc-transit-data-federation-webapp
          - onebusaway-nyc-api-webapp:war -> context: /onebusaway-nyc-api-webapp
          - onebusaway-nyc-acta-webapp:war -> context: /
  [ ] Verify TDM access: http://tdm.dev.obanyc.com:80/api/config/list
      (if blocked, get your IP added to vpc_obanyc_tdm_dev security group)
  [ ] Run the "bustime-app" configuration
  [ ] Verify TDS loaded: http://localhost:8080/routes/

Full doc: https://camsys.atlassian.net/wiki/spaces/~623178576a6824006970af77/pages/3235020829
================================================================
EOF