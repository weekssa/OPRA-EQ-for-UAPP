#!/usr/bin/env bash
set -euo pipefail
umask 077

SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
HELPER="$SCRIPT_DIR/phone-session.sh"
TMP_ROOT="$(mktemp -d "${TMPDIR:-/tmp}/ja11-phone-helper.XXXXXX")"
trap 'rm -rf "$TMP_ROOT"' EXIT
FAKE_ADB="$TMP_ROOT/adb"
FAKE_APKSIGNER="$TMP_ROOT/apksigner"
PREVIOUS_APK="$TMP_ROOT/previous.apk"
CANDIDATE_APK="$TMP_ROOT/candidate.apk"
INSTALL_LOG="$TMP_ROOT/install.log"
INSTALLED_FLAG="$TMP_ROOT/installed.flag"

fail() { echo "FAIL: $*" >&2; exit 1; }

cat > "$FAKE_ADB" <<'EOF'
#!/usr/bin/env bash
set -euo pipefail
if [[ "${1-}" == "devices" ]]; then echo "List of devices attached"; exit 0; fi
if [[ "${1-}" == "-s" ]]; then shift 2; fi
case "${1-}" in
  get-state) echo device ;;
  shell)
    shift
    case "${1-}" in
      getprop)
        case "${2-}" in
          ro.product.manufacturer) echo Google ;;
          ro.product.model) echo "Pixel 9" ;;
          ro.product.device) echo tokay ;;
          ro.build.version.release) echo 16 ;;
          *) echo unknown ;;
        esac
        ;;
      pm)
        case "${2-}" in
          list)
            case "${JA11_TEST_STATE:-absent}" in
              absent)
                if [[ -f "$JA11_TEST_INSTALLED_FLAG" ]]; then
                  echo "package:com.weekssa.opraeqforuapp.ja11diag"
                fi
                ;;
              installed) echo "package:com.weekssa.opraeqforuapp.ja11diag" ;;
              ambiguous)
                echo "package:com.weekssa.opraeqforuapp.ja11diag"
                echo "package:com.weekssa.opraeqforuapp.ja11diag.test"
                ;;
              query-failure) echo "simulated package query failure" >&2; exit 29 ;;
              *) echo "unknown fixture" >&2; exit 30 ;;
            esac
            ;;
          path) echo "package:/data/app/~~fixture/base.apk" ;;
          *) echo "unexpected pm command" >&2; exit 31 ;;
        esac
        ;;
      dumpsys)
        if [[ -f "$JA11_TEST_INSTALLED_FLAG" ]]; then
          printf 'versionCode=%s\nversionName=%s\ndebuggable=true\n' "$JA11_CANDIDATE_VERSION_CODE" "$JA11_CANDIDATE_VERSION"
        else
          printf 'versionCode=%s\nversionName=%s\ndebuggable=true\n' \
            "${JA11_TEST_PREVIOUS_VERSION_CODE:-11}" "${JA11_TEST_PREVIOUS_VERSION:-0.8.0-ja11diag}"
        fi
        ;;
      am) exit 0 ;;
      *) echo "unexpected shell command" >&2; exit 32 ;;
    esac
    ;;
  pull)
    if [[ -f "$JA11_TEST_INSTALLED_FLAG" ]]; then cp "$JA11_TEST_CANDIDATE_APK" "${3:?destination required}"
    else cp "$JA11_TEST_PREVIOUS_APK" "${3:?destination required}"
    fi
    ;;
  install)
    if [[ "${2-}" == "-r" ]]; then printf 'install -r %s\n' "${3:?APK required}" >> "$JA11_TEST_INSTALL_LOG"
    else printf 'install %s\n' "${2:?APK required}" >> "$JA11_TEST_INSTALL_LOG"
    fi
    : > "$JA11_TEST_INSTALLED_FLAG"
    echo Success
    ;;
  *) echo "unexpected adb command: $*" >&2; exit 33 ;;
esac
EOF

cat > "$FAKE_APKSIGNER" <<'EOF'
#!/usr/bin/env bash
set -euo pipefail
apk="${@: -1}"
case "$(cat "$apk")" in
  candidate*) digest="$JA11_TEST_CANDIDATE_SIGNER" ;;
  previous*) digest="$JA11_TEST_PREVIOUS_SIGNER" ;;
  *) echo "unknown fixture APK" >&2; exit 40 ;;
esac
formatted="$(printf '%s' "$digest" | sed 's/../&:/g;s/:$//')"
printf 'Signer #1 certificate SHA-256 digest: %s\n' "$formatted"
EOF
chmod 700 "$FAKE_ADB" "$FAKE_APKSIGNER"
printf 'previous diagnostic APK fixture\n' > "$PREVIOUS_APK"
printf 'candidate diagnostic APK fixture\n' > "$CANDIDATE_APK"
CANDIDATE_SHA="$(shasum -a 256 "$CANDIDATE_APK" | awk '{print $1}')"
PREVIOUS_SHA="$(shasum -a 256 "$PREVIOUS_APK" | awk '{print $1}')"
SIGNER="0123456789abcdef0123456789abcdef0123456789abcdef0123456789abcdef"

run_install() {
  local state="$1" evidence="$2" previous_version="${3:-0.8.0-ja11diag}" previous_sha="${4:-$PREVIOUS_SHA}" previous_signer="${5:-$SIGNER}"
  mkdir -m 700 -p "$evidence"
  JA11_ADB_BIN="$FAKE_ADB" \
  JA11_APKSIGNER_BIN="$FAKE_APKSIGNER" \
  JA11_EVIDENCE_DIR="$evidence" \
  JA11_CANDIDATE_APK="$CANDIDATE_APK" \
  JA11_CANDIDATE_APK_SHA="$CANDIDATE_SHA" \
  JA11_CANDIDATE_SIGNER_SHA="$SIGNER" \
  JA11_CANDIDATE_VERSION=0.8.1-ja11diag \
  JA11_CANDIDATE_VERSION_CODE=12 \
  JA11_PREVIOUS_VERSION=0.8.0-ja11diag \
  JA11_PREVIOUS_VERSION_CODE=11 \
  JA11_PREVIOUS_APK_SHA="$previous_sha" \
  JA11_PREVIOUS_SIGNER_SHA="$previous_signer" \
  JA11_TEST_STATE="$state" \
  JA11_TEST_PREVIOUS_VERSION="$previous_version" \
  JA11_TEST_PREVIOUS_VERSION_CODE=11 \
  JA11_TEST_CANDIDATE_VERSION=0.8.1-ja11diag \
  JA11_TEST_CANDIDATE_VERSION_CODE=12 \
  JA11_TEST_CANDIDATE_SIGNER="$SIGNER" \
  JA11_TEST_PREVIOUS_SIGNER="$previous_signer" \
  JA11_TEST_PREVIOUS_APK="$PREVIOUS_APK" \
  JA11_TEST_CANDIDATE_APK="$CANDIDATE_APK" \
  JA11_TEST_INSTALLED_FLAG="$INSTALLED_FLAG" \
  JA11_TEST_INSTALL_LOG="$INSTALL_LOG" \
  "$HELPER" fixture install
}

reset_fixture() { rm -f "$INSTALLED_FLAG" "$INSTALL_LOG"; }

reset_fixture
run_install absent "$TMP_ROOT/absent" > "$TMP_ROOT/absent.out" 2>&1 || fail "absent package did not clean install"
grep -Fqx "install $CANDIDATE_APK" "$INSTALL_LOG" || fail "absent package did not use guarded plain adb install"
if grep -Fq 'install -r' "$INSTALL_LOG"; then fail "absent package used in-place update"; fi
grep -Fq 'Installed diagnostic candidate hash, signer, package, and version verified.' "$TMP_ROOT/absent.out" || fail "clean install was not verified"

reset_fixture
run_install installed "$TMP_ROOT/installed" > "$TMP_ROOT/installed.out" 2>&1 || fail "known prior package did not update"
grep -Fqx "install -r $CANDIDATE_APK" "$INSTALL_LOG" || fail "known prior package did not use adb install -r"
grep -Fq 'rollback copy preserved at:' "$TMP_ROOT/installed.out" || fail "known prior APK was not preserved before update"

for state in ambiguous query-failure; do
  reset_fixture
  if run_install "$state" "$TMP_ROOT/$state" > "$TMP_ROOT/$state.out" 2>&1; then
    fail "$state package query unexpectedly allowed install"
  fi
  if [[ -s "$INSTALL_LOG" ]]; then fail "$state package query reached adb install"; fi
done
grep -Fq 'ambiguous' "$TMP_ROOT/ambiguous.out" || fail "ambiguous package state was not identified"
grep -Fq 'Could not query' "$TMP_ROOT/query-failure.out" || fail "package query failure was not identified"

reset_fixture
if run_install installed "$TMP_ROOT/version-mismatch" 0.7.0-ja11diag > "$TMP_ROOT/version-mismatch.out" 2>&1; then
  fail "mismatched installed version unexpectedly allowed update"
fi
if [[ -s "$INSTALL_LOG" ]]; then fail "mismatched installed version reached adb install"; fi
grep -Fq 'does not match the exact expected candidate' "$TMP_ROOT/version-mismatch.out" || fail "version mismatch was not identified"

reset_fixture
if run_install installed "$TMP_ROOT/hash-mismatch" 0.8.0-ja11diag "$(printf '0%.0s' {1..64})" > "$TMP_ROOT/hash-mismatch.out" 2>&1; then
  fail "unknown prior APK hash unexpectedly allowed update"
fi
if [[ -s "$INSTALL_LOG" ]]; then fail "unknown prior APK hash reached adb install"; fi
grep -Fq 'Previously installed APK checksum mismatch' "$TMP_ROOT/hash-mismatch.out" || fail "prior APK hash mismatch was not identified"

reset_fixture
if run_install installed "$TMP_ROOT/signer-mismatch" 0.8.0-ja11diag "$PREVIOUS_SHA" "$(printf 'f%.0s' {1..64})" > "$TMP_ROOT/signer-mismatch.out" 2>&1; then
  fail "different installed signer unexpectedly allowed update"
fi
if [[ -s "$INSTALL_LOG" ]]; then fail "different installed signer reached adb install"; fi
grep -Fq 'signers differ' "$TMP_ROOT/signer-mismatch.out" || fail "signer mismatch was not identified"

echo "phone-session install fixtures passed"
