#!/usr/bin/env bash
set -euo pipefail
umask 077

PACKAGE="${JA11_PACKAGE:-com.weekssa.opraeqforuapp.ja11diag}"
CANDIDATE_VERSION="${JA11_CANDIDATE_VERSION:-0.8.1-ja11diag}"
CANDIDATE_VERSION_CODE="${JA11_CANDIDATE_VERSION_CODE:-12}"
PREVIOUS_VERSION="${JA11_PREVIOUS_VERSION:-0.8.0-ja11diag}"
PREVIOUS_VERSION_CODE="${JA11_PREVIOUS_VERSION_CODE:-11}"

ADB_BIN="${JA11_ADB_BIN:-}"
if [[ -z "$ADB_BIN" && -n "${ANDROID_SDK_ROOT:-}" ]]; then
  ADB_BIN="$ANDROID_SDK_ROOT/platform-tools/adb"
fi
if [[ -z "$ADB_BIN" && -n "${ANDROID_HOME:-}" ]]; then
  ADB_BIN="$ANDROID_HOME/platform-tools/adb"
fi
if [[ -z "$ADB_BIN" ]]; then ADB_BIN="$(command -v adb || true)"; fi
if [[ -z "$ADB_BIN" || ! -x "$ADB_BIN" ]]; then
  echo "Set JA11_ADB_BIN to the Android SDK platform-tools/adb binary." >&2
  exit 2
fi

SERIAL="${1-}"
ACTION="${2-}"
EVIDENCE_DIR="${JA11_EVIDENCE_DIR:-}"
APK="${JA11_CANDIDATE_APK:-}"
ROLLBACK_APK="${JA11_ROLLBACK_APK:-}"
APKSIGNER_BIN="${JA11_APKSIGNER_BIN:-$(command -v apksigner || true)}"

usage() {
  cat <<'EOF'
Usage: phone-session.sh <current-adb-serial> <action>

Required for every action except list:
  JA11_EVIDENCE_DIR          private local directory for captured APKs/screenshots

Required for install and verify-install:
  JA11_CANDIDATE_APK         frozen .ja11diag APK
  JA11_CANDIDATE_APK_SHA     exact SHA-256 from the frozen candidate tuple
  JA11_CANDIDATE_SIGNER_SHA  exact signer certificate SHA-256

Required for a guarded update of an installed diagnostic package:
  JA11_PREVIOUS_APK_SHA      exact prior diagnostic APK SHA-256
  JA11_PREVIOUS_SIGNER_SHA   exact prior signer certificate SHA-256

Actions:
  list                 list ADB targets
  inspect              verify the online target is a Pixel 9 and show OS details
  inspect-install-state report absent/installed package state without installing
  install              verify, preserve a known prior install, then guarded install/update
  verify-install       pull the installed APK and verify its hash, signer, and version
  launch               launch the exact diagnostic package
  capture-screen       save one screenshot to the private evidence directory
  rollback             restore the verified prior diagnostic APK preserved during install
EOF
}

utc_stamp() { date -u +%Y%m%dT%H%M%SZ; }

if [[ -z "$ACTION" ]]; then usage; exit 2; fi
if [[ "$ACTION" == "list" ]]; then "$ADB_BIN" devices -l; exit 0; fi
if [[ -z "$SERIAL" || "$SERIAL" == "all" ]]; then
  echo "A single explicit current Pixel ADB serial is required." >&2
  exit 2
fi
if [[ -z "$EVIDENCE_DIR" ]]; then
  echo "Set JA11_EVIDENCE_DIR to a private local evidence directory." >&2
  exit 2
fi
mkdir -p "$EVIDENCE_DIR"
chmod 700 "$EVIDENCE_DIR"
if ! "$ADB_BIN" -s "$SERIAL" get-state 2>/dev/null | tr -d '\r' | grep -qx device; then
  echo "The selected ADB target is not online; stop and refresh discovery." >&2
  exit 3
fi

verify_pixel() {
  local manufacturer model
  manufacturer="$("$ADB_BIN" -s "$SERIAL" shell getprop ro.product.manufacturer | tr -d '\r')"
  model="$("$ADB_BIN" -s "$SERIAL" shell getprop ro.product.model | tr -d '\r')"
  printf 'manufacturer=%s\nmodel=%s\n' "$manufacturer" "$model"
  if [[ "$manufacturer" != "Google" || "$model" != "Pixel 9" ]]; then
    echo "Target identity is not the expected Pixel 9; stop." >&2
    return 4
  fi
}

require_pin() {
  local name="$1" value="$2"
  if [[ -z "$value" ]]; then echo "Set $name from the frozen candidate tuple." >&2; return 5; fi
}

verify_apk() {
  local file="$1" expected_sha="$2" expected_signer="$3" label="$4"
  local actual_sha cert_output actual_cert expected_normalized
  require_pin JA11_APKSIGNER_BIN "$APKSIGNER_BIN"
  require_pin "$label SHA-256" "$expected_sha"
  require_pin "$label signer SHA-256" "$expected_signer"
  if [[ ! -f "$file" ]]; then echo "$label APK is unavailable; stop." >&2; return 6; fi
  actual_sha="$(shasum -a 256 "$file" | awk '{print $1}')"
  if [[ "$actual_sha" != "$expected_sha" ]]; then echo "$label APK checksum mismatch; stop." >&2; return 7; fi
  cert_output="$("$APKSIGNER_BIN" verify --print-certs "$file")"
  actual_cert="$(printf '%s\n' "$cert_output" | awk -F': ' '/Signer #1 certificate SHA-256 digest:/ {print $2; exit}' | tr -d ':' | tr -d '[:space:]' | tr '[:upper:]' '[:lower:]')"
  expected_normalized="$(printf '%s' "$expected_signer" | tr -d ':' | tr '[:upper:]' '[:lower:]')"
  if [[ -z "$actual_cert" || "$actual_cert" != "$expected_normalized" ]]; then
    echo "$label APK signer mismatch; stop." >&2
    return 8
  fi
}

installed_package_state() {
  local package_list package_count
  if ! package_list="$("$ADB_BIN" -s "$SERIAL" shell pm list packages "$PACKAGE" | tr -d '\r')"; then
    echo "Could not query the installed diagnostic package state; stop." >&2
    return 16
  fi
  package_count="$(printf '%s\n' "$package_list" | awk '/^package:/ {n++} END {print n+0}')"
  if [[ "$package_count" -eq 0 && -z "$package_list" ]]; then printf 'absent\n'; return 0; fi
  if [[ "$package_count" -eq 1 ]] && printf '%s\n' "$package_list" | grep -Fqx "package:$PACKAGE"; then
    printf 'installed\n'
    return 0
  fi
  echo "Installed diagnostic package listing is unexpected or ambiguous; stop." >&2
  return 17
}

save_package_dump() {
  local path
  reserve_package_dump || return $?
  path="$PACKAGE_DUMP_PATH"
  "$ADB_BIN" -s "$SERIAL" shell dumpsys package "$PACKAGE" > "$path"
  chmod 600 "$path"
  grep -E 'versionCode=|versionName=' "$path" || true
}

verify_package_version() {
  local dump="$1" expected_name="$2" expected_code="$3"
  if ! grep -Eq "versionName=$expected_name([[:space:]]|$)" "$dump" || \
     ! grep -Eq "versionCode=$expected_code([[:space:]]|$)" "$dump"; then
    echo "Installed diagnostic package version does not match the exact expected candidate; stop." >&2
    return 10
  fi
}

pull_single_installed_apk() {
  local destination="$1" paths count installed_path
  if ! paths="$("$ADB_BIN" -s "$SERIAL" shell pm path "$PACKAGE" | tr -d '\r')"; then
    echo "Could not resolve the installed diagnostic APK path; stop." >&2
    return 11
  fi
  count="$(printf '%s\n' "$paths" | awk '/^package:/ {n++} END {print n+0}')"
  if [[ "$count" -ne 1 ]]; then echo "Expected exactly one installed diagnostic APK path; stop." >&2; return 12; fi
  installed_path="$(printf '%s\n' "$paths" | sed -n 's/^package://p')"
  "$ADB_BIN" -s "$SERIAL" pull "$installed_path" "$destination" >/dev/null
  chmod 600 "$destination"
}

reserve_new_evidence_file() {
  local path="$1" label="${2:-evidence file}"
  # noclobber uses an exclusive create so existing evidence cannot be replaced by a capture.
  if ! (set -o noclobber; : > "$path") 2>/dev/null; then
    echo "Preserving existing $label; use a new evidence directory." >&2
    return 9
  fi
}

reserve_package_dump() {
  if [[ -z "${PACKAGE_DUMP_PATH:-}" ]]; then
    PACKAGE_DUMP_PATH="$EVIDENCE_DIR/package-dump-$(utc_stamp).txt"
  fi
  if [[ "${PACKAGE_DUMP_RESERVED:-}" == yes ]]; then return 0; fi
  reserve_new_evidence_file "$PACKAGE_DUMP_PATH" "package dump" || return $?
  PACKAGE_DUMP_RESERVED=yes
}

reserve_candidate_verification_evidence() {
  if [[ -z "${CANDIDATE_VERIFIED_APK_PATH:-}" ]]; then
    CANDIDATE_VERIFIED_APK_PATH="$EVIDENCE_DIR/verified-ja11diag-$(utc_stamp).apk"
  fi
  if [[ -z "${CANDIDATE_VERIFIED_DUMP_PATH:-}" ]]; then
    CANDIDATE_VERIFIED_DUMP_PATH="$EVIDENCE_DIR/verified-ja11diag-package-$(utc_stamp).txt"
  fi
  if [[ "${CANDIDATE_VERIFICATION_RESERVED:-}" == yes ]]; then return 0; fi
  reserve_new_evidence_file "$CANDIDATE_VERIFIED_APK_PATH" "verified candidate APK" || return $?
  reserve_new_evidence_file "$CANDIDATE_VERIFIED_DUMP_PATH" "verified candidate package dump" || return $?
  CANDIDATE_VERIFICATION_RESERVED=yes
}

reserve_rollback_evidence() {
  if [[ -z "${ROLLBACK_VERIFIED_APK_PATH:-}" ]]; then
    ROLLBACK_VERIFIED_APK_PATH="$EVIDENCE_DIR/verified-rollback-ja11diag-$(utc_stamp).apk"
  fi
  if [[ -z "${ROLLBACK_VERIFIED_DUMP_PATH:-}" ]]; then
    ROLLBACK_VERIFIED_DUMP_PATH="$EVIDENCE_DIR/verified-rollback-ja11diag-package-$(utc_stamp).txt"
  fi
  if [[ "${ROLLBACK_VERIFICATION_RESERVED:-}" == yes ]]; then return 0; fi
  reserve_new_evidence_file "$ROLLBACK_VERIFIED_APK_PATH" "verified rollback APK" || return $?
  reserve_new_evidence_file "$ROLLBACK_VERIFIED_DUMP_PATH" "verified rollback package dump" || return $?
  ROLLBACK_VERIFICATION_RESERVED=yes
}

verify_previous_installed_candidate() {
  local backup="$EVIDENCE_DIR/previous-ja11diag-$(utc_stamp).apk" dump="$EVIDENCE_DIR/previous-ja11diag-package-$(utc_stamp).txt"
  local candidate_signer previous_signer
  verify_apk "$APK" "$JA11_CANDIDATE_APK_SHA" "$JA11_CANDIDATE_SIGNER_SHA" "Candidate" || return $?
  require_pin JA11_PREVIOUS_APK_SHA "${JA11_PREVIOUS_APK_SHA:-}" || return $?
  require_pin JA11_PREVIOUS_SIGNER_SHA "${JA11_PREVIOUS_SIGNER_SHA:-}" || return $?
  candidate_signer="$(printf '%s' "$JA11_CANDIDATE_SIGNER_SHA" | tr -d ':' | tr '[:upper:]' '[:lower:]')"
  previous_signer="$(printf '%s' "$JA11_PREVIOUS_SIGNER_SHA" | tr -d ':' | tr '[:upper:]' '[:lower:]')"
  if [[ "$candidate_signer" != "$previous_signer" ]]; then
    echo "Candidate and installed package signers differ; stop before update." >&2
    return 13
  fi
  if [[ "$CANDIDATE_VERSION_CODE" -le "$PREVIOUS_VERSION_CODE" ]]; then
    echo "Candidate version code is not newer than the installed package; stop before update." >&2
    return 14
  fi
  reserve_new_evidence_file "$dump" "prior package dump" || return $?
  reserve_new_evidence_file "$backup" "prior APK backup" || return $?
  pull_single_installed_apk "$backup" || return $?
  verify_apk "$backup" "$JA11_PREVIOUS_APK_SHA" "$JA11_PREVIOUS_SIGNER_SHA" "Previously installed" || return $?
  "$ADB_BIN" -s "$SERIAL" shell dumpsys package "$PACKAGE" > "$dump"
  chmod 600 "$dump"
  verify_package_version "$dump" "$PREVIOUS_VERSION" "$PREVIOUS_VERSION_CODE" || return $?
  echo "Exact prior diagnostic APK and signer verified; rollback copy preserved at: $backup"
  JA11_ROLLBACK_APK="$backup"
  export JA11_ROLLBACK_APK
}

verify_candidate_installed() {
  local installed dump
  require_pin JA11_CANDIDATE_APK_SHA "${JA11_CANDIDATE_APK_SHA:-}" || return $?
  require_pin JA11_CANDIDATE_SIGNER_SHA "${JA11_CANDIDATE_SIGNER_SHA:-}" || return $?
  reserve_candidate_verification_evidence || return $?
  installed="$CANDIDATE_VERIFIED_APK_PATH"
  dump="$CANDIDATE_VERIFIED_DUMP_PATH"
  pull_single_installed_apk "$installed" || return $?
  verify_apk "$installed" "$JA11_CANDIDATE_APK_SHA" "$JA11_CANDIDATE_SIGNER_SHA" "Installed candidate" || return $?
  "$ADB_BIN" -s "$SERIAL" shell dumpsys package "$PACKAGE" > "$dump"
  chmod 600 "$dump"
  verify_package_version "$dump" "$CANDIDATE_VERSION" "$CANDIDATE_VERSION_CODE" || return $?
  echo "Installed diagnostic candidate hash, signer, package, and version verified."
}

verify_pixel
case "$ACTION" in
  inspect)
    "$ADB_BIN" -s "$SERIAL" shell getprop ro.product.device
    "$ADB_BIN" -s "$SERIAL" shell getprop ro.build.version.release
    ;;
  inspect-install-state)
    PACKAGE_STATE="$(installed_package_state)"
    printf 'diagnostic_package_state=%s\n' "$PACKAGE_STATE"
    ;;
  install)
    require_pin JA11_CANDIDATE_APK "$APK"
    verify_apk "$APK" "${JA11_CANDIDATE_APK_SHA:-}" "${JA11_CANDIDATE_SIGNER_SHA:-}" "Candidate"
    reserve_candidate_verification_evidence
    reserve_package_dump
    PACKAGE_STATE="$(installed_package_state)"
    if [[ "$PACKAGE_STATE" == installed ]]; then
      verify_previous_installed_candidate
      "$ADB_BIN" -s "$SERIAL" install -r "$APK"
    elif [[ "$PACKAGE_STATE" == absent ]]; then
      "$ADB_BIN" -s "$SERIAL" install "$APK"
    else
      echo "Installed diagnostic package state is not safe for installation; stop." >&2
      exit 17
    fi
    verify_candidate_installed
    save_package_dump
    ;;
  verify-install)
    reserve_candidate_verification_evidence
    reserve_package_dump
    verify_candidate_installed
    save_package_dump
    ;;
  launch)
    "$ADB_BIN" -s "$SERIAL" shell am force-stop "$PACKAGE"
    "$ADB_BIN" -s "$SERIAL" shell am start -W -n "$PACKAGE/com.weekssa.opraeqforuapp.MainActivity"
    ;;
  capture-screen)
    destination="$EVIDENCE_DIR/screen-$(utc_stamp).png"
    if [[ -e "$destination" ]]; then echo "Preserving existing screenshot; use a new evidence directory." >&2; exit 18; fi
    "$ADB_BIN" -s "$SERIAL" exec-out screencap -p > "$destination"
    chmod 600 "$destination"
    echo "Screen capture saved at: $destination"
    ;;
  rollback)
    require_pin JA11_ROLLBACK_APK "$ROLLBACK_APK"
    reserve_candidate_verification_evidence
    reserve_package_dump
    reserve_rollback_evidence
    verify_candidate_installed
    verify_apk "$ROLLBACK_APK" "${JA11_PREVIOUS_APK_SHA:-}" "${JA11_PREVIOUS_SIGNER_SHA:-}" "Rollback"
    "$ADB_BIN" -s "$SERIAL" install -r "$ROLLBACK_APK"
    pull_single_installed_apk "$ROLLBACK_VERIFIED_APK_PATH"
    verify_apk "$ROLLBACK_VERIFIED_APK_PATH" "$JA11_PREVIOUS_APK_SHA" "$JA11_PREVIOUS_SIGNER_SHA" "Restored rollback"
    "$ADB_BIN" -s "$SERIAL" shell dumpsys package "$PACKAGE" > "$ROLLBACK_VERIFIED_DUMP_PATH"
    chmod 600 "$ROLLBACK_VERIFIED_DUMP_PATH"
    verify_package_version "$ROLLBACK_VERIFIED_DUMP_PATH" "$PREVIOUS_VERSION" "$PREVIOUS_VERSION_CODE"
    ;;
  *) usage; exit 2 ;;
esac
