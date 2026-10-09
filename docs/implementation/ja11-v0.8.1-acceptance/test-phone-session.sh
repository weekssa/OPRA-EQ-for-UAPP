#!/usr/bin/env bash
set -euo pipefail
umask 077

HERE="$(cd "$(dirname "$0")" && pwd)"
HELPER="$HERE/phone-session.sh"
SOURCE_SHA="$(sed -n 's/^SOURCE_SHA="\([0-9a-f]*\)"/\1/p' "$HELPER")"
PACKAGE="com.weekssa.opraeqforuapp.ja11diag"
TMP_ROOT="$(mktemp -d "${TMPDIR:-/tmp}/ja11-phone-session-test.XXXXXX")"
CAPTURE_PID=""

cleanup() {
  if [[ -n "$CAPTURE_PID" ]]; then
    kill "$CAPTURE_PID" 2>/dev/null || true
    wait "$CAPTURE_PID" 2>/dev/null || true
  fi
  rm -rf "$TMP_ROOT"
}
trap cleanup EXIT

fail() {
  echo "FAIL: $*" >&2
  exit 1
}

ADB="$TMP_ROOT/fake-adb"
cat > "$ADB" <<'EOF'
#!/usr/bin/env bash
set -euo pipefail
[[ "$1" == "-s" ]] || { echo "unexpected adb target arguments" >&2; exit 90; }
shift 2
case "${1-}" in
  get-state)
    echo device
    ;;
  shell)
    shift
    case "${1-}" in
      getprop)
        case "${2-}" in
          ro.product.manufacturer) echo Google ;;
          ro.product.model) echo "Pixel 9" ;;
          *) echo "unexpected getprop" >&2; exit 91 ;;
        esac
        ;;
      dumpsys)
        printf 'versionCode=11\nversionName=0.8.0-ja11diag\ndebuggable=true\n'
        ;;
      pidof)
        [[ "${2-}" == "com.weekssa.opraeqforuapp.ja11diag" ]] || exit 92
        echo 7943
        ;;
      *) echo "unexpected shell command" >&2; exit 93 ;;
    esac
    ;;
  logcat)
    [[ "${2-}" == "-v" && "${3-}" == "threadtime" && -n "${JA11_TEST_EVENT_SOURCE:-}" ]] || {
      echo "unexpected logcat mode" >&2
      exit 94
    }
    cat "$JA11_TEST_EVENT_SOURCE"
    while :; do sleep 1; done
    ;;
  *) echo "unexpected adb command" >&2; exit 95 ;;
esac
EOF
chmod 700 "$ADB"

FAKE_BIN="$TMP_ROOT/fake-bin"
mkdir -m 700 "$FAKE_BIN"
cat > "$FAKE_BIN/ps" <<'EOF'
#!/usr/bin/env bash
set -euo pipefail
if [[ -n "${JA11_TEST_PS_CALL_COUNT:-}" ]]; then
  count=0
  if [[ -f "$JA11_TEST_PS_CALL_COUNT" ]]; then read -r count < "$JA11_TEST_PS_CALL_COUNT"; fi
  count=$((count + 1))
  printf '%s\n' "$count" > "$JA11_TEST_PS_CALL_COUNT"
  if [[ "$count" == "${JA11_TEST_KILL_ON_PS_CALL:-never}" ]]; then
    /bin/kill "${JA11_TEST_CAPTURE_TO_KILL:?}"
  fi
fi
exec /bin/ps "$@"
EOF
chmod 700 "$FAKE_BIN/ps"

write_log() {
  local path="$1" serial_mode="${2:-serialless}" candidate_count="${3:-1}" include_build="${4:-yes}"
  local session_current="${5:-true}" later_ambiguous_attach="${6:-no}" identity_count="${7:-$candidate_count}"
  local device_status connection_status serial_source serial_available
  case "$serial_mode" in
    serialless)
      device_status=READABLE_NULL
      connection_status=READABLE_NULL
      serial_source=NONE
      serial_available=false
      ;;
    connection)
      device_status=READABLE_NULL
      connection_status=READABLE_NONBLANK
      serial_source=USB_CONNECTION
      serial_available=true
      ;;
    device)
      device_status=READABLE_NONBLANK
      connection_status=NOT_CHECKED
      serial_source=USB_DEVICE
      serial_available=true
      ;;
    blank)
      device_status=READABLE_BLANK
      connection_status=NOT_CHECKED
      serial_source=NONE
      serial_available=false
      ;;
    *) fail "unknown serial fixture: $serial_mode" ;;
  esac
  : > "$path"
  if [[ "$include_build" == yes ]]; then
    printf '10-09 00:13:37.859 7943 7943 I JA11_DIAG: event=APP_BUILD_INFO package=%s versionName=0.8.0-ja11diag versionCode=11 debuggable=true sourceSha=%s ja11DiagnosticsEnabled=true\n' "$PACKAGE" "$SOURCE_SHA" >> "$path"
  fi
  printf '10-09 00:14:10.100 7943 7943 I JA11_DIAG: event=USB_IDENTITY_DESCRIPTOR_STATUS permissionGranted=true serialStatus=%s connectionSerialStatus=%s serialSource=%s productId=258 serial=SERIAL_SENTINEL fingerprint=FINGERPRINT_SENTINEL\n' "$device_status" "$connection_status" "$serial_source" >> "$path"
  printf '10-09 00:14:10.200 7943 7943 I JA11_DIAG: event=USB_SESSION_OPENED pid=258 sessionGeneration=3 supportedCandidateCount=%s serial=SERIAL_SENTINEL\n' "$candidate_count" >> "$path"
  printf '10-09 00:14:10.300 7943 7943 I JA11_DIAG: event=SNAPSHOT_READ_COMPLETE sourceSha=%s sessionGeneration=3\n' "$SOURCE_SHA" >> "$path"
  printf '10-09 00:14:10.400 7943 7943 I JA11_DIAG: event=RESTART_IDENTITY_AVAILABILITY serialAvailable=%s supportedCandidateCount=%s sessionGeneration=3 sessionCurrent=%s fingerprint=FINGERPRINT_SENTINEL\n' "$serial_available" "$identity_count" "$session_current" >> "$path"
  if [[ "$later_ambiguous_attach" == yes ]]; then
    printf '10-09 00:14:10.500 7943 7943 I JA11_DIAG: event=USB_ATTACH pid=257 supportedCandidateCount=2\n' >> "$path"
  fi
}

start_capture() {
  local evidence_dir="$1" event_source="$2" target_serial="${3:-fixture}" attempt recorded_pid
  JA11_ADB_BIN="$ADB" JA11_EVIDENCE_DIR="$evidence_dir" JA11_TEST_EVENT_SOURCE="$event_source" \
    "$HELPER" "$target_serial" start-logcat > "$TMP_ROOT/start-logcat.out" 2>&1 &
  CAPTURE_PID=$!
  for ((attempt=0; attempt<50; attempt++)); do
    if [[ -s "$evidence_dir/logcat-live.txt" && -s "$evidence_dir/logcat-live.pid" ]]; then break; fi
    sleep 0.1
  done
  [[ -s "$evidence_dir/logcat-live.txt" && -s "$evidence_dir/logcat-live.pid" ]] || {
    cat "$TMP_ROOT/start-logcat.out" >&2
    fail "foreground capture did not start"
  }
  recorded_pid="$(cat "$evidence_dir/logcat-live.pid")"
  [[ "$recorded_pid" == "$CAPTURE_PID" ]] || fail "capture PID was not bound to the foreground process"
}

stop_capture() {
  if [[ -n "$CAPTURE_PID" ]]; then
    kill "$CAPTURE_PID" 2>/dev/null || true
    wait "$CAPTURE_PID" 2>/dev/null || true
    CAPTURE_PID=""
  fi
}

POSITIVE="$TMP_ROOT/positive"
mkdir -m 700 "$POSITIVE"
write_log "$TMP_ROOT/positive.events"
start_capture "$POSITIVE" "$TMP_ROOT/positive.events"
JA11_ADB_BIN="$ADB" JA11_EVIDENCE_DIR="$POSITIVE" "$HELPER" fixture verify > "$TMP_ROOT/verify.out" 2>&1 || fail "verify rejected matching APP_BUILD_INFO from retained live capture"
grep -q 'event=APP_BUILD_INFO' "$TMP_ROOT/verify.out" || fail "verify did not report the retained APP_BUILD_INFO"
JA11_ADB_BIN="$ADB" JA11_EVIDENCE_DIR="$POSITIVE" "$HELPER" fixture verify-ja11-session > "$TMP_ROOT/session.out" 2>&1 || fail "verify-ja11-session rejected a sole serialless JA11 with a complete current baseline"
grep -q 'serialAvailable=false' "$TMP_ROOT/session.out" || fail "verify-ja11-session did not preserve the serialless status"
grep -q 'supportedCandidateCount=1' "$TMP_ROOT/session.out" || fail "verify-ja11-session did not report unique JA11 selection"
if grep -Eq 'SERIAL_SENTINEL|FINGERPRINT_SENTINEL' "$TMP_ROOT/session.out"; then
  fail "verify-ja11-session exposed a raw serial or fingerprint"
fi
stop_capture

CONNECTION_SERIAL="$TMP_ROOT/connection-serial"
mkdir -m 700 "$CONNECTION_SERIAL"
write_log "$TMP_ROOT/connection-serial.events" connection
start_capture "$CONNECTION_SERIAL" "$TMP_ROOT/connection-serial.events"
JA11_ADB_BIN="$ADB" JA11_EVIDENCE_DIR="$CONNECTION_SERIAL" "$HELPER" fixture verify-ja11-session > "$TMP_ROOT/connection-serial.out" 2>&1 || fail "verify-ja11-session rejected optional connection-serial evidence"
grep -q 'serialSource=USB_CONNECTION' "$TMP_ROOT/connection-serial.out" || fail "connection-serial fixture lost its diagnostic category"
stop_capture

MID_VERIFY_STOP="$TMP_ROOT/mid-verify-stop"
mkdir -m 700 "$MID_VERIFY_STOP"
write_log "$TMP_ROOT/mid-verify-stop.events"
start_capture "$MID_VERIFY_STOP" "$TMP_ROOT/mid-verify-stop.events"
if PATH="$FAKE_BIN:$PATH" JA11_TEST_PS_CALL_COUNT="$TMP_ROOT/ps-calls" \
   JA11_TEST_KILL_ON_PS_CALL=3 JA11_TEST_CAPTURE_TO_KILL="$CAPTURE_PID" \
   JA11_ADB_BIN="$ADB" JA11_EVIDENCE_DIR="$MID_VERIFY_STOP" \
   "$HELPER" fixture verify-ja11-session > "$TMP_ROOT/mid-verify-stop.out" 2>&1; then
  fail "verify-ja11-session reported success after logcat stopped at the final capture check"
else
  status=$?
  [[ "$status" -eq 15 ]] || fail "mid-verification capture stop returned $status instead of 15"
fi
stop_capture
if JA11_ADB_BIN="$ADB" JA11_EVIDENCE_DIR="$POSITIVE" "$HELPER" fixture verify-ja11-session > "$TMP_ROOT/stale.out" 2>&1; then
  fail "verify-ja11-session accepted a nonempty capture after its foreground process stopped"
else
  status=$?
  [[ "$status" -eq 15 ]] || fail "stopped capture returned $status instead of 15"
fi

NO_CAPTURE="$TMP_ROOT/no-capture"
mkdir -m 700 "$NO_CAPTURE"
if JA11_ADB_BIN="$ADB" JA11_EVIDENCE_DIR="$NO_CAPTURE" "$HELPER" fixture verify > "$TMP_ROOT/no-capture.out" 2>&1; then
  fail "verify accepted a missing retained capture"
else
  status=$?
  [[ "$status" -eq 15 ]] || fail "missing capture returned $status instead of 15"
fi

NO_BUILD="$TMP_ROOT/no-build"
mkdir -m 700 "$NO_BUILD"
write_log "$TMP_ROOT/no-build.events" serialless 1 no
start_capture "$NO_BUILD" "$TMP_ROOT/no-build.events"
if JA11_ADB_BIN="$ADB" JA11_EVIDENCE_DIR="$NO_BUILD" "$HELPER" fixture verify > "$TMP_ROOT/no-build.out" 2>&1; then
  fail "verify accepted a capture without APP_BUILD_INFO"
else
  status=$?
  [[ "$status" -eq 14 ]] || fail "missing APP_BUILD_INFO returned $status instead of 14"
fi
stop_capture

DEVICE_SERIAL="$TMP_ROOT/device-serial"
mkdir -m 700 "$DEVICE_SERIAL"
write_log "$TMP_ROOT/device-serial.events" device
start_capture "$DEVICE_SERIAL" "$TMP_ROOT/device-serial.events"
JA11_ADB_BIN="$ADB" JA11_EVIDENCE_DIR="$DEVICE_SERIAL" "$HELPER" fixture verify-ja11-session > "$TMP_ROOT/device-serial.out" 2>&1 || fail "verify-ja11-session rejected optional device-serial evidence"
grep -q 'serialSource=USB_DEVICE' "$TMP_ROOT/device-serial.out" || fail "device-serial fixture lost its diagnostic category"
stop_capture

BLANK_SERIAL="$TMP_ROOT/blank-serial"
mkdir -m 700 "$BLANK_SERIAL"
write_log "$TMP_ROOT/blank-serial.events" blank
start_capture "$BLANK_SERIAL" "$TMP_ROOT/blank-serial.events"
JA11_ADB_BIN="$ADB" JA11_EVIDENCE_DIR="$BLANK_SERIAL" "$HELPER" fixture verify-ja11-session > "$TMP_ROOT/blank-serial.out" 2>&1 || fail "verify-ja11-session treated an unavailable serial as a support failure"
grep -q 'serialAvailable=false' "$TMP_ROOT/blank-serial.out" || fail "blank serial fixture lost its unavailable status"
stop_capture

for candidate_count in 0 2; do
  CANDIDATES="$TMP_ROOT/candidates-$candidate_count"
  mkdir -m 700 "$CANDIDATES"
  write_log "$TMP_ROOT/candidates-$candidate_count.events" serialless "$candidate_count"
  start_capture "$CANDIDATES" "$TMP_ROOT/candidates-$candidate_count.events"
  if JA11_ADB_BIN="$ADB" JA11_EVIDENCE_DIR="$CANDIDATES" "$HELPER" fixture verify-ja11-session > "$TMP_ROOT/candidates-$candidate_count.out" 2>&1; then
    fail "verify-ja11-session accepted candidate count $candidate_count"
  else
    status=$?
    [[ "$status" -eq 15 ]] || fail "candidate count $candidate_count returned $status instead of 15"
  fi
  stop_capture
done

STALE_SESSION="$TMP_ROOT/stale-session"
mkdir -m 700 "$STALE_SESSION"
write_log "$TMP_ROOT/stale-session.events" serialless 1 yes false
start_capture "$STALE_SESSION" "$TMP_ROOT/stale-session.events"
if JA11_ADB_BIN="$ADB" JA11_EVIDENCE_DIR="$STALE_SESSION" "$HELPER" fixture verify-ja11-session > "$TMP_ROOT/stale-session.out" 2>&1; then
  fail "verify-ja11-session accepted a non-current session"
else
  status=$?
  [[ "$status" -eq 16 ]] || fail "non-current session returned $status instead of 16"
fi
stop_capture

LATER_AMBIGUITY="$TMP_ROOT/later-ambiguity"
mkdir -m 700 "$LATER_AMBIGUITY"
write_log "$TMP_ROOT/later-ambiguity.events" serialless 1 yes true yes
start_capture "$LATER_AMBIGUITY" "$TMP_ROOT/later-ambiguity.events"
if JA11_ADB_BIN="$ADB" JA11_EVIDENCE_DIR="$LATER_AMBIGUITY" "$HELPER" fixture verify-ja11-session > "$TMP_ROOT/later-ambiguity.out" 2>&1; then
  fail "verify-ja11-session accepted a later ambiguous JA11 attach"
else
  status=$?
  [[ "$status" -eq 17 ]] || fail "later ambiguity returned $status instead of 17"
fi
stop_capture

WRONG_PIXEL="$TMP_ROOT/wrong-pixel"
mkdir -m 700 "$WRONG_PIXEL"
write_log "$TMP_ROOT/wrong-pixel.events"
start_capture "$WRONG_PIXEL" "$TMP_ROOT/wrong-pixel.events" fixture-other
if JA11_ADB_BIN="$ADB" JA11_EVIDENCE_DIR="$WRONG_PIXEL" "$HELPER" fixture verify-ja11-session > "$TMP_ROOT/wrong-pixel.out" 2>&1; then
  fail "verify-ja11-session accepted a capture bound to a different ADB serial"
else
  status=$?
  [[ "$status" -eq 15 ]] || fail "wrong-Pixel capture returned $status instead of 15"
fi
stop_capture

echo "PHONE SESSION HELPER FIXTURES PASS"
