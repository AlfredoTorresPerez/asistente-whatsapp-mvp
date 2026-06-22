#!/bin/sh
set -eu

VISUAL_MODE="${WHATSAPP_WEB_VISUAL_MODE:-true}"
KILL_ORPHAN_CHROMIUM="${WHATSAPP_WEB_KILL_ORPHAN_CHROMIUM_ON_START:-true}"

if [ "$KILL_ORPHAN_CHROMIUM" = "true" ]; then
  pkill -f chromium >/dev/null 2>&1 || true
fi

rm -f /tmp/.X99-lock /tmp/.X11-unix/X99 >/dev/null 2>&1 || true

if [ "$VISUAL_MODE" = "true" ]; then
  export DISPLAY="${DISPLAY:-:99}"
  Xvfb "$DISPLAY" -screen 0 "${WHATSAPP_WEB_SCREEN_SIZE:-1366x768x24}" >/tmp/xvfb.log 2>&1 &
  sleep 1
  fluxbox >/tmp/fluxbox.log 2>&1 &
  x11vnc -display "$DISPLAY" -forever -shared -nopw -rfbport 5900 >/tmp/x11vnc.log 2>&1 &
  websockify --web=/usr/share/novnc/ 6080 localhost:5900 >/tmp/novnc.log 2>&1 &
fi

exec node src/server.js
