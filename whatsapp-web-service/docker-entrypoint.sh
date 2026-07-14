#!/bin/bash
set -euo pipefail

SESSION_DATA_PATH="${WHATSAPP_WEB_SESSION_DATA_PATH:-/app/.wwebjs_auth}"
CACHE_PATH="${WHATSAPP_WEB_CACHE_PATH:-/app/.wwebjs_cache}"
CLEAN_PROFILE_LOCKS="${WHATSAPP_WEB_CLEAN_PROFILE_LOCKS_ON_START:-true}"
KILL_ORPHAN_CHROMIUM="${WHATSAPP_WEB_KILL_ORPHAN_CHROMIUM_ON_START:-true}"

log() {
  echo "[$(date -u +'%Y-%m-%dT%H:%M:%SZ')] [entrypoint] $*"
}

backup_session_auth() {
  local dir="$1"
  if [ ! -d "$dir" ]; then
    return 0
  fi
  local backup_dir="${dir}.backup"
  if [ -d "$backup_dir" ]; then
    rm -rf "$backup_dir"
  fi
  cp -r "$dir" "$backup_dir" 2>/dev/null || true
  log "Backed up session auth data from $dir to $backup_dir"
}

clean_chromium_locks() {
  local dir="$1"
  if [ ! -d "$dir" ]; then
    return 0
  fi
  
  local lock_files=("SingletonLock" "SingletonSocket" "SingletonCookie" "DevToolsActivePort")
  for lock in "${lock_files[@]}"; do
    find "$dir" -name "$lock" -type f -delete 2>/dev/null || true
  done
  
  log "Cleaned Chromium profile locks in $dir"
}

kill_orphan_chromium() {
  pkill -f "chromium" 2>/dev/null || true
  pkill -f "chrome" 2>/dev/null || true
  log "Killed orphan Chromium processes"
}

main() {
  log "Starting whatsapp-web-service entrypoint"
  log "SESSION_DATA_PATH=$SESSION_DATA_PATH"
  log "CACHE_PATH=$CACHE_PATH"
  log "CLEAN_PROFILE_LOCKS=$CLEAN_PROFILE_LOCKS"
  log "KILL_ORPHAN_CHROMIUM=$KILL_ORPHAN_CHROMIUM"
  
  if [ "$KILL_ORPHAN_CHROMIUM" = "true" ]; then
    kill_orphan_chromium
  fi
  
  if [ "$CLEAN_PROFILE_LOCKS" = "true" ]; then
    backup_session_auth "$SESSION_DATA_PATH"
    clean_chromium_locks "$SESSION_DATA_PATH"
    clean_chromium_locks "$CACHE_PATH"
    clean_chromium_locks "/tmp/whatsapp-web-profile"
  fi
  
  log "Entrypoint complete, starting application"
  exec "$@"
}

main "$@"