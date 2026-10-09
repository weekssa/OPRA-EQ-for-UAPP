#!/usr/bin/env bash
set -euo pipefail
umask 077

SOURCE_SHA="1d19067c9150aae1b09e01fafb8af3647bf65f71"
EXPECTED_APK_SHA="23adf9f4955b056f110562362717c706767b7e3ecc50225c07439a3ff5c23711"
PREVIOUS_APK_SHA="ee0fe4fbfaae7b3f959d4122f0c21c128dffdf21d47586376ee383e2534ceb0d"
DEBUG_SIGNER_SHA="73aa7581c8dc7dcc8ccea7586771119a98f9a74d7d8cf23716e09c557c9f6b41"
PACKAGE="com.weekssa.opraeqforuapp.ja11diag"
ACTIVITY="com.weekssa.opraeqforuapp.MainActivity"
REMOTE_PROFILE="/sdcard/Download/ja11-v081-baseline-1d19067c-20261009.txt"
ROLLBACK_APK="${JA11_ROLLBACK_APK:-/private/tmp/ja11-v0.8.1-acceptance-61695803/owner-phone-session-20261009-resume/installed-ja11diag-base.apk}"

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
  JA11_ROLLBACK_APK   exact prior J024 APK used only for safe software rollback

Actions:
  list             show current adb devices
  inspect          verify Pixel 9 and report basic OS properties
  verify-ja11-session  require one current JA11 candidate and a complete source-bound baseline
  start-logcat     capture full logs in the foreground; stop with Ctrl-C in that terminal
  install          install only the frozen JA11 diagnostic APK
  rollback         restore the exact prior J024 diagnostic APK; no hardware action
  launch           start only the diagnostic package
  verify           save and verify exact APP_BUILD_INFO diagnostics
  stage-profile    push a generated baseline profile to Downloads
  capture-screen   save one screenshot locally
  collect-logs     save full and JA11_DIAG-only logs locally
  remove-profile   remove only the named staged baseline profile
  uninstall        uninstall only the JA11 diagnostic package
EOF
}

utc_stamp() {
  printf '%s-%s' "$(date -u +%Y%m%dT%H%M%SZ)" "$$"
}

if [ -z "$ACTION" ]; then usage; exit 2; fi
if [ -z "$EVIDENCE_DIR" ]; then
  echo "Set JA11_EVIDENCE_DIR to a private local evidence directory." >&2
  exit 2
fi
mkdir -p "$EVIDENCE_DIR"
chmod 700 "$EVIDENCE_DIR"
LIVE_LOG="$EVIDENCE_DIR/logcat-live.txt"
LIVE_PID="$EVIDENCE_DIR/logcat-live.pid"

verify_live_capture() {
  local capture_pid capture_command
  if [ ! -s "$LIVE_LOG" ] || [ ! -s "$LIVE_PID" ]; then
    echo "Start foreground logcat capture before launching the candidate; no bound live capture is available." >&2
    return 15
  fi
  capture_pid="$(cat "$LIVE_PID")"
  if [[ ! "$capture_pid" =~ ^[0-9]+$ ]] || ! kill -0 "$capture_pid" 2>/dev/null; then
    echo "The foreground logcat capture for this evidence session is no longer running." >&2
    return 15
  fi
  capture_command="$(ps -ww -p "$capture_pid" -o command= 2>/dev/null || true)"
  if [[ "$capture_command" != *"$ADB_BIN"* || "$capture_command" != *" -s $SERIAL logcat -v threadtime"* ]]; then
    echo "The recorded foreground process is not this Pixel's ADB logcat capture." >&2
    return 15
  fi
  if ! kill -0 "$capture_pid" 2>/dev/null; then
    echo "The foreground logcat capture stopped during verification." >&2
    return 15
  fi
}

capture_diag_events() {
  local destination="$1"
  verify_live_capture || return $?
  awk 'index($0, "JA11_DIAG:") && index($0, "event=") { print }' "$LIVE_LOG" > "$destination"
  chmod 600 "$destination"
  verify_live_capture || return $?
}

if [ "$ACTION" = "list" ]; then "$ADB_BIN" devices -l; exit 0; fi
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
  dump="$EVIDENCE_DIR/package-dump-$(utc_stamp).txt"
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
  stamp="$(utc_stamp)"
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
    echo "Installed diagnostic package version is not the exact prior J024 candidate; stop." >&2
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
    if [ -e "$LIVE_LOG" ] || [ -e "$LIVE_PID" ]; then
      echo "This evidence directory already contains a log capture; preserve it and use a new directory." >&2
      exit 5
    fi
    : > "$LIVE_LOG"
    chmod 600 "$LIVE_LOG"
    printf '%s\n' "$$" > "$LIVE_PID"
    chmod 600 "$LIVE_PID"
    echo "Full logcat is capturing to the private evidence directory; stop with Ctrl-C in this terminal."
    exec "$ADB_BIN" -s "$SERIAL" logcat -v threadtime > "$LIVE_LOG" 2>&1
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
      echo "The exact J024 rollback APK is unavailable; stop without changing the installed app." >&2
      exit 6
    fi
    verify_apk "$ROLLBACK_APK" "$PREVIOUS_APK_SHA" "J024 rollback"
    PACKAGE_PATHS="$("$ADB_BIN" -s "$SERIAL" shell pm path "$PACKAGE" | tr -d '\r')"
    if [ "$(printf '%s\n' "$PACKAGE_PATHS" | awk '/^package:/ {n++} END {print n+0}')" -ne 1 ]; then
      echo "Rollback requires exactly one installed diagnostic APK path; stop." >&2
      exit 9
    fi
    INSTALLED_PATH="$(printf '%s\n' "$PACKAGE_PATHS" | sed -n 's/^package://p')"
    STAMP="$(utc_stamp)"
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
    EVENTS="$EVIDENCE_DIR/ja11-diag-events-$(utc_stamp).txt"
    if [ -e "$EVENTS" ]; then echo "Preserving existing event log; use a new evidence directory." >&2; exit 11; fi
    capture_diag_events "$EVENTS"
    BUILD_LINE="$(grep -F 'event=APP_BUILD_INFO' "$EVENTS" | tail -n 1 || true)"
    printf '%s\n' "$BUILD_LINE"
    if [ -z "$BUILD_LINE" ] || [[ "$BUILD_LINE" != *"package=$PACKAGE"* || "$BUILD_LINE" != *"versionName=0.8.0-ja11diag"* || "$BUILD_LINE" != *"versionCode=11"* || "$BUILD_LINE" != *"debuggable=true"* || "$BUILD_LINE" != *"sourceSha=$SOURCE_SHA"* || "$BUILD_LINE" != *"ja11DiagnosticsEnabled=true"* ]]; then
      echo "The latest diagnostic build-info event does not match the frozen candidate; stop." >&2
      exit 14
    fi
    verify_live_capture || exit $?
    ;;
  verify-ja11-session)
    verify_pixel
    EVENTS="$EVIDENCE_DIR/ja11-session-baseline-$(utc_stamp).txt"
    if [ -e "$EVENTS" ]; then echo "Preserving existing JA11 session event log; use a new evidence directory." >&2; exit 12; fi
    capture_diag_events "$EVENTS"
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
    SESSION_ENTRY="$(awk -v build="$BUILD_POSITION" -v pid="$BUILD_PID" 'NR > build && $3 == pid && index($0, "event=USB_SESSION_OPENED") {position = NR; line = $0} END {if (position) printf "%d\t%s", position, line}' "$EVENTS")"
    SESSION_POSITION=""
    SESSION_LINE=""
    IFS="$(printf '\t')" read -r SESSION_POSITION SESSION_LINE <<< "$SESSION_ENTRY"
    DESCRIPTOR_ENTRY="$(awk -v build="$BUILD_POSITION" -v pid="$BUILD_PID" -v session="$SESSION_POSITION" 'NR > build && NR < session && $3 == pid && index($0, "event=USB_IDENTITY_DESCRIPTOR_STATUS") {position = NR; line = $0} END {if (position) printf "%d\t%s", position, line}' "$EVENTS")"
    DESCRIPTOR_POSITION=""
    DESCRIPTOR_LINE=""
    IFS="$(printf '\t')" read -r DESCRIPTOR_POSITION DESCRIPTOR_LINE <<< "$DESCRIPTOR_ENTRY"
    LATEST_DESCRIPTOR_POSITION="$(awk -v build="$BUILD_POSITION" -v pid="$BUILD_PID" 'NR > build && $3 == pid && index($0, "event=USB_IDENTITY_DESCRIPTOR_STATUS") {position = NR} END {print position+0}' "$EVENTS")"
    DESCRIPTOR_PRODUCT_ID="$(printf '%s\n' "$DESCRIPTOR_LINE" | sed -n 's/.*productId=\([0-9][0-9]*\).*/\1/p')"
    OPENED_PRODUCT_ID="$(printf '%s\n' "$SESSION_LINE" | sed -n 's/.* pid=\([0-9][0-9]*\).*/\1/p')"
    SESSION_GENERATION="$(printf '%s\n' "$SESSION_LINE" | sed -n 's/.*sessionGeneration=\([0-9][0-9]*\).*/\1/p')"
    OPENED_CANDIDATE_COUNT="$(printf '%s\n' "$SESSION_LINE" | sed -n 's/.*supportedCandidateCount=\([0-9][0-9]*\).*/\1/p')"
    DEVICE_SERIAL_STATUS="$(printf '%s\n' "$DESCRIPTOR_LINE" | sed -n 's/.*serialStatus=\([A-Z_]*\).*/\1/p')"
    CONNECTION_SERIAL_STATUS="$(printf '%s\n' "$DESCRIPTOR_LINE" | sed -n 's/.*connectionSerialStatus=\([A-Z_]*\).*/\1/p')"
    SERIAL_SOURCE="$(printf '%s\n' "$DESCRIPTOR_LINE" | sed -n 's/.*serialSource=\([A-Z_]*\).*/\1/p')"
    if [ -z "$SESSION_POSITION" ] || [ -z "$SESSION_GENERATION" ] || \
       [ -z "$DESCRIPTOR_POSITION" ] || [ "$LATEST_DESCRIPTOR_POSITION" != "$DESCRIPTOR_POSITION" ] || \
       [ -z "$DESCRIPTOR_PRODUCT_ID" ] || [ "$DESCRIPTOR_PRODUCT_ID" != "$OPENED_PRODUCT_ID" ] || \
       { [ "$DESCRIPTOR_PRODUCT_ID" != "257" ] && [ "$DESCRIPTOR_PRODUCT_ID" != "258" ]; } || \
       [ "$OPENED_CANDIDATE_COUNT" != "1" ] || \
       [ -z "$DEVICE_SERIAL_STATUS" ] || [ -z "$CONNECTION_SERIAL_STATUS" ] || \
       [ -z "$SERIAL_SOURCE" ] || \
       [[ "$DESCRIPTOR_LINE" != *"permissionGranted=true"* ]]; then
      echo "The latest opened JA11 session lacks a current permissioned descriptor or unique candidate; stop before any write." >&2
      exit 15
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
    IDENTITY_CANDIDATE_COUNT="$(printf '%s\n' "$IDENTITY_LINE" | sed -n 's/.*supportedCandidateCount=\([0-9][0-9]*\).*/\1/p')"
    SERIAL_AVAILABLE="$(printf '%s\n' "$IDENTITY_LINE" | sed -n 's/.*serialAvailable=\([^ ]*\).*/\1/p')"
    if [ -z "$SNAPSHOT_GENERATION" ] || [ -z "$IDENTITY_GENERATION" ] || [ -z "$SNAPSHOT_SOURCE" ] || \
       [ -z "$SNAPSHOT_POSITION" ] || [ -z "$IDENTITY_POSITION" ] || \
       [ "$SNAPSHOT_POSITION" -le "$SESSION_POSITION" ] || [ "$IDENTITY_POSITION" -le "$SESSION_POSITION" ] || \
       [ "$IDENTITY_POSITION" -le "$SNAPSHOT_POSITION" ] || \
       [ "$SNAPSHOT_SOURCE" != "$SOURCE_SHA" ] || \
       [ "$SNAPSHOT_GENERATION" != "$SESSION_GENERATION" ] || \
       [ "$SNAPSHOT_GENERATION" != "$IDENTITY_GENERATION" ] || \
       [ "$IDENTITY_CANDIDATE_COUNT" != "1" ] || \
       [ -z "$SERIAL_AVAILABLE" ] || \
       [[ "$IDENTITY_LINE" != *"sessionCurrent=true"* ]]; then
      echo "The latest complete baseline does not match one current supported JA11 session; stop before any write." >&2
      exit 16
    fi
    LATER_AMBIGUOUS_ATTACH_POSITION="$(awk -v session="$SESSION_POSITION" -v pid="$BUILD_PID" 'NR > session && $3 == pid && index($0, "event=USB_ATTACH") { for (i = 1; i <= NF; i++) if (index($i, "supportedCandidateCount=") == 1) { value = $i; sub(/^supportedCandidateCount=/, "", value); if (value + 0 > 1) position = NR } } END {print position+0}' "$EVENTS")"
    if [ "$LATER_AMBIGUOUS_ATTACH_POSITION" -gt 0 ]; then
      echo "A later JA11 attach made candidate selection ambiguous; stop before any write." >&2
      exit 17
    fi
    TERMINAL_SESSION_POSITION="$(awk -v session="$SESSION_POSITION" -v pid="$BUILD_PID" -v generation="$SESSION_GENERATION" 'NR > session && $3 == pid && ((index($0, "event=USB_DETACH ") && index($0, "sessionGeneration=" generation " ")) || (index($0, "event=USB_SESSION_CLOSED ") && index($0, "sessionGeneration=" generation " "))) {position = NR} END {print position+0}' "$EVENTS")"
    if [ "$TERMINAL_SESSION_POSITION" -gt 0 ]; then
      echo "The verified JA11 USB session has a later detach or close event; stop before any write." >&2
      exit 18
    fi
    if ! CURRENT_PACKAGE_PIDS="$("$ADB_BIN" -s "$SERIAL" shell pidof "$PACKAGE" 2>/dev/null | tr -d '\r' | awk '{$1=$1; print}')"; then
      echo "Cannot confirm that the verified candidate app process is still running; stop before any write." >&2
      exit 19
    fi
    case " $CURRENT_PACKAGE_PIDS " in
      *" $BUILD_PID "*) ;;
      *)
        echo "The process that emitted the verified candidate identity is no longer running; stop before any write." >&2
        exit 20
        ;;
    esac
    verify_live_capture || exit $?
    printf 'USB_IDENTITY_DESCRIPTOR_STATUS permissionGranted=true serialStatus=%s connectionSerialStatus=%s serialSource=%s productId=%s\n' "$DEVICE_SERIAL_STATUS" "$CONNECTION_SERIAL_STATUS" "$SERIAL_SOURCE" "$DESCRIPTOR_PRODUCT_ID"
    printf 'USB_SESSION_OPENED productId=%s sessionGeneration=%s supportedCandidateCount=%s\n' "$OPENED_PRODUCT_ID" "$SESSION_GENERATION" "$OPENED_CANDIDATE_COUNT"
    printf 'RESTART_IDENTITY_AVAILABILITY sessionGeneration=%s serialAvailable=%s sessionCurrent=true supportedCandidateCount=%s\n' "$SNAPSHOT_GENERATION" "$SERIAL_AVAILABLE" "$IDENTITY_CANDIDATE_COUNT"
    echo "The exact candidate has a complete current baseline from the sole attached supported JA11. A readable serial is optional continuity evidence; without one, this check makes no claim that the same physical unit was identified. No serial or fingerprint was recorded."
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
    SCREEN="$EVIDENCE_DIR/screen-$(utc_stamp).png"
    if [ -e "$SCREEN" ]; then echo "Preserving existing screenshot; use a new evidence directory." >&2; exit 12; fi
    "$ADB_BIN" -s "$SERIAL" exec-out screencap -p > "$SCREEN"
    chmod 600 "$SCREEN"
    echo "$SCREEN"
    ;;
  collect-logs)
    verify_pixel
    STAMP="$(utc_stamp)"
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
