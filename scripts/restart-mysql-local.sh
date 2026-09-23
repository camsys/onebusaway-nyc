#!/usr/bin/env bash
set -uo pipefail

# Recovers local MySQL when it's stuck — most commonly an orphaned
# --skip-grant-tables/--skip-networking safe-mode instance left behind by an
# interrupted password reset (see setup-bustime-dev.sh), which blocks port
# 3306 until it's killed. Safe to run anytime bustime's local MySQL seems
# unreachable; a no-op if nothing is stuck.
#
# No 'set -e' here on purpose — this script's whole job is to get MySQL back
# into a good state and report clearly, so it should keep going and tell you
# what it found rather than bailing out on the first failed check.

log()  { printf '\n\033[1;34m==>\033[0m %s\n' "$1"; }
warn() { printf '\033[1;33mWARNING:\033[0m %s\n' "$1"; }

log "Looking for a stuck safe-mode MySQL process"
STRAY_PIDS="$(pgrep -f -- "--skip-grant-tables --skip-networking" || true)"
if [[ -n "$STRAY_PIDS" ]]; then
  warn "Found stuck safe-mode process(es), PID(s): $STRAY_PIDS"
  kill $STRAY_PIDS 2>/dev/null || true
  sleep 2
  STILL_ALIVE="$(pgrep -f -- "--skip-grant-tables --skip-networking" || true)"
  if [[ -n "$STILL_ALIVE" ]]; then
    warn "Still alive after a normal kill, forcing: $STILL_ALIVE"
    kill -9 $STILL_ALIVE 2>/dev/null || true
    sleep 1
  fi
  echo "Cleared."
else
  echo "None found."
fi

log "Restarting MySQL via Homebrew"
brew services restart mysql >/dev/null 2>&1 || true

log "Waiting for MySQL to accept connections on 127.0.0.1:3306"
tries=0
until nc -z 127.0.0.1 3306 >/dev/null 2>&1 || [[ $tries -ge 20 ]]; do
  sleep 1
  tries=$((tries + 1))
done

if nc -z 127.0.0.1 3306 >/dev/null 2>&1; then
  echo "MySQL is up and reachable on 127.0.0.1:3306."
else
  warn "Still not reachable over TCP after restarting."
  warn "Check 'brew services list' and the error log under /opt/homebrew/var/mysql/*.err (or /usr/local/var/mysql/*.err on Intel Macs)."
  exit 1
fi