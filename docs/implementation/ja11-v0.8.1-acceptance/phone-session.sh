#!/usr/bin/env bash
set -euo pipefail
umask 077

SOURCE_SHA="3f5e0c3a39687e27d962dd7f7f80d2667ff396ae"
EXPECTED_APK_SHA="85e06ca0db818586a7eb2eab3378a1b21949b3c8593e1318536ec651d8369305"
PACKAGE="com.weekssa.opraeqforuapp.ja11diag"
ACTIVITY="com.weekssa.opraeqforuapp.MainActivity"
REMOTE_PROFILE="/sdcard/Download/ja11-v081-baseline-3f5e0c3a-20261008.txt"

ADB_BIN="${JA11_ADB_BIN:-}"
if [ -z "$ADB_BIN" ] && [ -n "${ANDROID_SDK_ROOT:-}" ]; then
  ADB_BIN="$ANDROID_SDK_ROOT/platform-tools/adb"
fi
if [ -z "$ADB_BIN" ] && [ -n "${ANDROID_HOME:-}" ]; then
  ADB_BIN="$ANDROID_HOME/platform-tools/adb"
fi
if [ -z "$ADB_BIN" ]; then
  ADB_BIN="$(command -v adb || true)"
fi
if [ -z "$ADB_BIN" ] || [ ! -x "$ADB_BIN" ]; then
  echo "Set JA11_ADB_BIN to the Android SDK platform-tools/adb binary." >&2
  exit 2
fi

SERIAL="${1-}"
ACTION="${2-}"
PROFILE="${3-}"
EVIDENCE_DIR="${JA11_EVIDENCE_DIR:-}"
APK="${JA11_CANDIDATE_APK:-}"

usage() {
  cat <<'EOF'
Usage: phone-session.sh <current-adb-serial> <action> [profile-file]

Required environment:
  JA11_EVIDENCE_DIR   private local directory for evidence
  JA11_CANDIDATE_APK  frozen diagnostic APK path (install only)

Optional environment:
  JA11_ADB_BIN        exact Android SDK adb binary (otherwise SDK roots/PATH are checked)

Actions:
  list             show current adb devices
  inspect          verify Pixel 9 and report basic OS properties
  start-logcat     start private local full-logcat capture
  stop-logcat      stop the local capture process (serial may be blank)
  install          install only the frozen JA11 diagnostic APK
  launch           start only the diagnostic package
  verify           save and verify exact APP_BUILD_INFO diagnostics
  stage-profile    push a generated baseline profile to Downloads
  capture-screen   save one screenshot locally
  collect-logs     save full and JA11_DIAG-only logs locally
  remove-profile   remove only the named staged baseline profile
  uninstall        uninstall only the JA11 diagnostic package
EOF
}

if [ -z "$ACTION" ]; then usage; exit 2; fi
if [ -z "$EVIDENCE_DIR" ]; then
  echo "Set JA11_EVIDENCE_DIR to a private local evidence directory." >&2
  exit 2
fi
mkdir -p "$EVIDENCE_DIR"
chmod 700 "$EVIDENCE_DIR"
PID_FILE="$EVIDENCE_DIR/logcat-capture.pid"
LIVE_LOG="$EVIDENCE_DIR/logcat-live.txt"

if [ "$ACTION" = "list" ]; then "$ADB_BIN" devices -l; exit 0; fi
if [ "$ACTION" = "stop-logcat" ]; then
  if [ -f "$PID_FILE" ]; then
    PID="$(sed -n '1p' "$PID_FILE")"
    CAPTURE_SERIAL="$(sed -n '2p' "$PID_FILE")"
    COMMAND="$(ps -p "$PID" -o command= 2>/dev/null || true)"
    if [ -n "$CAPTURE_SERIAL" ] && [[ "$COMMAND" == *"$CAPTURE_SERIAL"* && "$COMMAND" == *"logcat"* ]]; then
      kill -INT "$PID"
      rm -f "$PID_FILE"
    else
      echo "The saved PID does not identify this adb logcat capture; it was left untouched." >&2
      exit 10
    fi
  fi
  exit 0
fi
if [ -z "$SERIAL" ] || [ "$SERIAL" = "all" ]; then
  echo "A single explicit current Pixel ADB serial is required." >&2
  exit 2
fi
if ! "$ADB_BIN" -s "$SERIAL" get-state 2>/dev/null | tr -d '\r' | grep -qx device; then
  echo "The selected ADB target is not online; stop and refresh discovery." >&2
  exit 3
fi

verify_pixel() {
  local manufacturer model
  manufacturer=$("$ADB_BIN" -s "$SERIAL" shell getprop ro.product.manufacturer | tr -d '\r')
  model=$("$ADB_BIN" -s "$SERIAL" shell getprop ro.product.model | tr -d '\r')
  printf 'manufacturer=%s\nmodel=%s\n' "$manufacturer" "$model"
  if [ "$manufacturer" != "Google" ] || [ "$model" != "Pixel 9" ]; then
    echo "Target identity is not the expected Pixel 9; stop." >&2
    return 4
  fi
}
save_package_dump() {
  local dump
  dump="$EVIDENCE_DIR/package-dump-$(date -u +%Y%m%dT%H%M%SZ).txt"
  if [ -e "$dump" ]; then
    echo "Preserving existing package dump; use a new evidence directory." >&2
    return 14
  fi
  "$ADB_BIN" -s "$SERIAL" shell dumpsys package "$PACKAGE" > "$dump"
  chmod 600 "$dump"
  grep -E 'versionCode=|versionName=|debuggable=' "$dump" || true
}

case "$ACTION" in
  inspect)
    verify_pixel
    "$ADB_BIN" -s "$SERIAL" shell getprop ro.product.device
    "$ADB_BIN" -s "$SERIAL" shell getprop ro.build.version.release
    ;;
  start-logcat)
    verify_pixel
    if [ -e "$PID_FILE" ] || [ -e "$LIVE_LOG" ]; then
      echo "This evidence directory already contains a log capture; preserve it and use a new directory." >&2
      exit 5
    fi
    : > "$LIVE_LOG"
    chmod 600 "$LIVE_LOG"
    nohup "$ADB_BIN" -s "$SERIAL" logcat -v threadtime > "$LIVE_LOG" 2>&1 < /dev/null &
    printf '%s\n%s\n' "$!" "$SERIAL" > "$PID_FILE"
    chmod 600 "$PID_FILE"
    echo "Log capture started in the private evidence directory."
    ;;
  install)
    verify_pixel
    if [ -z "$APK" ] || [ ! -f "$APK" ]; then
      echo "Set JA11_CANDIDATE_APK to the frozen diagnostic APK." >&2
      exit 6
    fi
    ACTUAL_SHA="$(shasum -a 256 "$APK" | awk '{print $1}')"
    if [ "$ACTUAL_SHA" != "$EXPECTED_APK_SHA" ]; then
      echo "Frozen APK checksum mismatch; stop." >&2
      exit 7
    fi
    if "$ADB_BIN" -s "$SERIAL" shell pm path "$PACKAGE" | grep -q '^package:'; then
      echo "The diagnostic package is already installed; stop and resume from its current state." >&2
      exit 8
    fi
    "$ADB_BIN" -s "$SERIAL" install "$APK"
    save_package_dump
    ;;
  launch)
    verify_pixel
    "$ADB_BIN" -s "$SERIAL" shell am force-stop "$PACKAGE"
    "$ADB_BIN" -s "$SERIAL" shell am start -W -n "$PACKAGE/$ACTIVITY"
    ;;
  verify)
    verify_pixel
    save_package_dump
    EVENTS="$EVIDENCE_DIR/ja11-diag-events-$(date -u +%Y%m%dT%H%M%SZ).txt"
    if [ -e "$EVENTS" ]; then echo "Preserving existing event log; use a new evidence directory." >&2; exit 11; fi
    "$ADB_BIN" -s "$SERIAL" logcat -d -v threadtime JA11_DIAG:I '*:S' > "$EVENTS"
    chmod 600 "$EVENTS"
    grep -F "event=APP_BUILD_INFO" "$EVENTS" | grep -F "package=$PACKAGE" | grep -F "versionName=0.8.0-ja11diag" | grep -F "versionCode=11" | grep -F "debuggable=true" | grep -F "sourceSha=$SOURCE_SHA" | grep -F "ja11DiagnosticsEnabled=true"
    ;;
  stage-profile)
    verify_pixel
    if [ -z "$PROFILE" ] || [ ! -f "$PROFILE" ]; then
      echo "Pass the generated local baseline profile." >&2
      exit 8
    fi
    if "$ADB_BIN" -s "$SERIAL" shell "test -e '$REMOTE_PROFILE'"; then
      echo "The unique staging filename already exists; do not overwrite it." >&2
      exit 9
    fi
    "$ADB_BIN" -s "$SERIAL" push "$PROFILE" "$REMOTE_PROFILE"
    ;;
  capture-screen)
    verify_pixel
    SCREEN="$EVIDENCE_DIR/screen-$(date -u +%Y%m%dT%H%M%SZ).png"
    if [ -e "$SCREEN" ]; then echo "Preserving existing screenshot; use a new evidence directory." >&2; exit 12; fi
    "$ADB_BIN" -s "$SERIAL" exec-out screencap -p > "$SCREEN"
    chmod 600 "$SCREEN"
    echo "$SCREEN"
    ;;
  collect-logs)
    verify_pixel
    STAMP="$(date -u +%Y%m%dT%H%M%SZ)"
    FULL_LOG="$EVIDENCE_DIR/logcat-$STAMP.txt"
    EVENTS="$EVIDENCE_DIR/ja11-diag-events-$STAMP.txt"
    if [ -e "$FULL_LOG" ] || [ -e "$EVENTS" ]; then echo "Preserving existing logs; use a new evidence directory." >&2; exit 13; fi
    "$ADB_BIN" -s "$SERIAL" logcat -d -v threadtime > "$FULL_LOG"
    chmod 600 "$FULL_LOG"
    "$ADB_BIN" -s "$SERIAL" logcat -d -v threadtime JA11_DIAG:I '*:S' > "$EVENTS"
    chmod 600 "$EVENTS"
    echo "$FULL_LOG"
    ;;
  remove-profile)
    verify_pixel
    "$ADB_BIN" -s "$SERIAL" shell rm -f "$REMOTE_PROFILE"
    ;;
  uninstall)
    verify_pixel
    "$ADB_BIN" -s "$SERIAL" uninstall "$PACKAGE"
    ;;
  *)
    usage
    exit 2
    ;;
esac
