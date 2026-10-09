#!/usr/bin/env bash
set -euo pipefail
umask 077

SOURCE_SHA="da1f8e25918065667648d676cb669fed4c803f17"
EXPECTED_APK_SHA="767b42591adc92f0e1662480112bf9efe87ce15f51060d20a7aa39118b8d8c24"
PREVIOUS_APK_SHA="7beb5bcebbc0dc40a68b33de911cc8722d76d3f0ff2e98685b1fa25e17caed61"
DEBUG_SIGNER_SHA="73aa7581c8dc7dcc8ccea7586771119a98f9a74d7d8cf23716e09c557c9f6b41"
PACKAGE="com.weekssa.opraeqforuapp.ja11diag"
ACTIVITY="com.weekssa.opraeqforuapp.MainActivity"
REMOTE_PROFILE="/sdcard/Download/ja11-v081-baseline-da1f8e25-20261008.txt"
ROLLBACK_APK="${JA11_ROLLBACK_APK:-/private/tmp/ja11-v0.8.1-acceptance-a7880844/opra-eq-ja11diag-0.8.0-source-a7880844.apk}"

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
APKSIGNER_BIN="${JA11_APKSIGNER_BIN:-$(command -v apksigner || true)}"

usage() {
  cat <<'EOF'
Usage: phone-session.sh <current-adb-serial> <action> [profile-file]

Required environment:
  JA11_EVIDENCE_DIR   private local directory for evidence
  JA11_CANDIDATE_APK  frozen diagnostic APK path (install only)

Optional environment:
  JA11_ADB_BIN        exact Android SDK adb binary (otherwise SDK roots/PATH are checked)
  JA11_ROLLBACK_APK   exact superseded J020 APK used only for safe software rollback

Actions:
  list             show current adb devices
  inspect          verify Pixel 9 and report basic OS properties
  verify-identity  require a current candidate-process snapshot and stable JA11 identity boolean
  start-logcat     start private local full-logcat capture
  stop-logcat      stop the local capture process (serial may be blank)
  install          install only the frozen JA11 diagnostic APK
  rollback         restore the exact prior J020 diagnostic APK; no hardware action
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
verify_apk() {
  local file="$1" expected_sha="$2" label="$3" actual_sha cert_output actual_cert
  if [ -z "$APKSIGNER_BIN" ] || [ ! -x "$APKSIGNER_BIN" ]; then
    echo "Set JA11_APKSIGNER_BIN to the Android SDK apksigner binary." >&2
    return 6
  fi
  actual_sha="$(shasum -a 256 "$file" | awk '{print $1}')"
  if [ "$actual_sha" != "$expected_sha" ]; then
    echo "$label APK checksum mismatch; stop." >&2
    return 7
  fi
  cert_output="$("$APKSIGNER_BIN" verify --print-certs "$file")"
  actual_cert="$(printf '%s\n' "$cert_output" | awk -F': ' '/Signer #1 certificate SHA-256 digest:/ {print $2; exit}' | tr -d ':' | tr -d '[:space:]' | tr '[:upper:]' '[:lower:]')"
  if [ "$actual_cert" != "$DEBUG_SIGNER_SHA" ]; then
    echo "$label APK signer mismatch; stop." >&2
    return 8
  fi
}
verify_previous_installed_candidate() {
  local paths count installed_path stamp pulled actual_sha dump_path dump
  paths="$("$ADB_BIN" -s "$SERIAL" shell pm path "$PACKAGE" | tr -d '\r')"
  count="$(printf '%s\n' "$paths" | awk '/^package:/ {n++} END {print n+0}')"
  if [ "$count" -ne 1 ]; then
    echo "The installed diagnostic package does not have exactly one APK path; stop." >&2
    return 9
  fi
  installed_path="$(printf '%s\n' "$paths" | sed -n 's/^package://p')"
  stamp="$(date -u +%Y%m%dT%H%M%SZ)"
  pulled="$EVIDENCE_DIR/previous-ja11diag-base-$stamp.apk"
  dump_path="$EVIDENCE_DIR/previous-ja11diag-package-$stamp.txt"
  if [ -e "$pulled" ] || [ -e "$dump_path" ]; then
    echo "Preserving existing prior-candidate evidence; use a new private evidence directory." >&2
    return 10
  fi
  "$ADB_BIN" -s "$SERIAL" pull "$installed_path" "$pulled" >/dev/null
  chmod 600 "$pulled"
  verify_apk "$pulled" "$PREVIOUS_APK_SHA" "Previously installed"
  dump="$("$ADB_BIN" -s "$SERIAL" shell dumpsys package "$PACKAGE" | tr -d '\r')"
  printf '%s\n' "$dump" > "$dump_path"
  chmod 600 "$dump_path"
  if ! printf '%s\n' "$dump" | grep -Fq 'versionName=0.8.0-ja11diag' || \
     ! printf '%s\n' "$dump" | grep -Eq 'versionCode=11([[:space:]]|$)'; then
    echo "Installed diagnostic package version is not the exact prior J020 candidate; stop." >&2
    return 11
  fi
  echo "Exact prior diagnostic APK and signer verified; its app data will be preserved by in-place update."
  echo "Verified rollback APK saved at: $pulled"
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
    verify_apk "$APK" "$EXPECTED_APK_SHA" "Frozen candidate"
    PACKAGE_PATHS="$("$ADB_BIN" -s "$SERIAL" shell pm path "$PACKAGE" | tr -d '\r')"
    if printf '%s\n' "$PACKAGE_PATHS" | grep -q '^package:'; then
      verify_previous_installed_candidate
      "$ADB_BIN" -s "$SERIAL" install -r "$APK"
    else
      "$ADB_BIN" -s "$SERIAL" install "$APK"
    fi
    save_package_dump
    ;;
  rollback)
    verify_pixel
    if [ ! -f "$ROLLBACK_APK" ]; then
      echo "The exact J020 rollback APK is unavailable; stop without changing the installed app." >&2
      exit 6
    fi
    verify_apk "$ROLLBACK_APK" "$PREVIOUS_APK_SHA" "J020 rollback"
    PACKAGE_PATHS="$("$ADB_BIN" -s "$SERIAL" shell pm path "$PACKAGE" | tr -d '\r')"
    if [ "$(printf '%s\n' "$PACKAGE_PATHS" | awk '/^package:/ {n++} END {print n+0}')" -ne 1 ]; then
      echo "Rollback requires exactly one installed diagnostic APK path; stop." >&2
      exit 9
    fi
    INSTALLED_PATH="$(printf '%s\n' "$PACKAGE_PATHS" | sed -n 's/^package://p')"
    STAMP="$(date -u +%Y%m%dT%H%M%SZ)"
    CURRENT_APK="$EVIDENCE_DIR/current-ja11diag-before-rollback-$STAMP.apk"
    if [ -e "$CURRENT_APK" ]; then
      echo "Preserving existing pre-rollback APK evidence; use a new private evidence directory." >&2
      exit 10
    fi
    "$ADB_BIN" -s "$SERIAL" pull "$INSTALLED_PATH" "$CURRENT_APK" >/dev/null
    chmod 600 "$CURRENT_APK"
    verify_apk "$CURRENT_APK" "$EXPECTED_APK_SHA" "Installed corrected candidate"
    "$ADB_BIN" -s "$SERIAL" install -r "$ROLLBACK_APK"
    verify_previous_installed_candidate
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
    BUILD_LINE="$(grep -F 'event=APP_BUILD_INFO' "$EVENTS" | tail -n 1 || true)"
    printf '%s\n' "$BUILD_LINE"
    if [ -z "$BUILD_LINE" ] || [[ "$BUILD_LINE" != *"package=$PACKAGE"* || "$BUILD_LINE" != *"versionName=0.8.0-ja11diag"* || "$BUILD_LINE" != *"versionCode=11"* || "$BUILD_LINE" != *"debuggable=true"* || "$BUILD_LINE" != *"sourceSha=$SOURCE_SHA"* || "$BUILD_LINE" != *"ja11DiagnosticsEnabled=true"* ]]; then
      echo "The latest diagnostic build-info event does not match the frozen candidate; stop." >&2
      exit 14
    fi
    ;;
  verify-identity)
    verify_pixel
    EVENTS="$EVIDENCE_DIR/ja11-identity-availability-$(date -u +%Y%m%dT%H%M%SZ).txt"
    if [ -e "$EVENTS" ]; then echo "Preserving existing identity event log; use a new evidence directory." >&2; exit 12; fi
    "$ADB_BIN" -s "$SERIAL" logcat -d -v threadtime JA11_DIAG:I '*:S' > "$EVENTS"
    chmod 600 "$EVENTS"
    BUILD_ENTRY="$(awk '/event=APP_BUILD_INFO/ {position = NR; pid = $3; line = $0} END {if (position) printf "%d\t%s\t%s", position, pid, line}' "$EVENTS")"
    BUILD_POSITION=""
    BUILD_PID=""
    BUILD_LINE=""
    IFS="$(printf '\t')" read -r BUILD_POSITION BUILD_PID BUILD_LINE <<< "$BUILD_ENTRY"
    if [ -z "$BUILD_POSITION" ] || [ -z "$BUILD_PID" ] || \
       [[ "$BUILD_LINE" != *"package=$PACKAGE"* || "$BUILD_LINE" != *"versionName=0.8.0-ja11diag"* || "$BUILD_LINE" != *"versionCode=11"* || "$BUILD_LINE" != *"debuggable=true"* || "$BUILD_LINE" != *"sourceSha=$SOURCE_SHA"* || "$BUILD_LINE" != *"ja11DiagnosticsEnabled=true"* ]]; then
      echo "The latest diagnostic build-info event does not identify the frozen candidate; stop." >&2
      exit 14
    fi
    SNAPSHOT_ENTRY="$(awk -v build="$BUILD_POSITION" -v pid="$BUILD_PID" 'NR > build && $3 == pid && index($0, "event=SNAPSHOT_READ_COMPLETE") {position = NR; line = $0} END {if (position) printf "%d\t%s", position, line}' "$EVENTS")"
    IDENTITY_ENTRY="$(awk -v build="$BUILD_POSITION" -v pid="$BUILD_PID" 'NR > build && $3 == pid && index($0, "event=RESTART_IDENTITY_AVAILABILITY") {position = NR; line = $0} END {if (position) printf "%d\t%s", position, line}' "$EVENTS")"
    SNAPSHOT_POSITION=""
    SNAPSHOT_LINE=""
    IDENTITY_POSITION=""
    IDENTITY_LINE=""
    IFS="$(printf '\t')" read -r SNAPSHOT_POSITION SNAPSHOT_LINE <<< "$SNAPSHOT_ENTRY"
    IFS="$(printf '\t')" read -r IDENTITY_POSITION IDENTITY_LINE <<< "$IDENTITY_ENTRY"
    SNAPSHOT_GENERATION="$(printf '%s\n' "$SNAPSHOT_LINE" | sed -n 's/.*sessionGeneration=\([0-9][0-9]*\).*/\1/p')"
    IDENTITY_GENERATION="$(printf '%s\n' "$IDENTITY_LINE" | sed -n 's/.*sessionGeneration=\([0-9][0-9]*\).*/\1/p')"
    SNAPSHOT_SOURCE="$(printf '%s\n' "$SNAPSHOT_LINE" | sed -n 's/.*sourceSha=\([0-9a-f][0-9a-f]*\).*/\1/p')"
    printf '%s\n' "$IDENTITY_LINE"
    if [ -z "$SNAPSHOT_GENERATION" ] || [ -z "$IDENTITY_GENERATION" ] || [ -z "$SNAPSHOT_SOURCE" ] || \
       [ "$SNAPSHOT_SOURCE" != "$SOURCE_SHA" ] || \
       [ "$SNAPSHOT_GENERATION" != "$IDENTITY_GENERATION" ] || \
       [[ "$IDENTITY_LINE" != *"identityAvailable=true"* || "$IDENTITY_LINE" != *"sessionCurrent=true"* ]]; then
      echo "The latest complete snapshot does not have a current stable restart identity; stop before any restart-control write." >&2
      exit 15
    fi
    echo "Stable restart identity is available for snapshot session $SNAPSHOT_GENERATION; no serial or fingerprint was recorded."
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
