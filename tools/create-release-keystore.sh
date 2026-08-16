#!/usr/bin/env bash
set -euo pipefail

alias_name="opra-eq-for-uapp-release"
output_directory="$HOME/OPRA-EQ-release-signing"
keystore_path="$output_directory/opra-eq-for-uapp-release.p12"
base64_path="$output_directory/opra-eq-for-uapp-release.p12.base64.txt"

find_keytool() {
  if command -v keytool >/dev/null 2>&1; then
    command -v keytool
    return
  fi

  if [[ -x /usr/libexec/java_home ]]; then
    local java_home
    java_home="$(/usr/libexec/java_home 2>/dev/null || true)"
    if [[ -n "$java_home" && -x "$java_home/bin/keytool" ]]; then
      printf '%s\n' "$java_home/bin/keytool"
      return
    fi
  fi

  local android_studio_keytool="/Applications/Android Studio.app/Contents/jbr/Contents/Home/bin/keytool"
  if [[ -x "$android_studio_keytool" ]]; then
    printf '%s\n' "$android_studio_keytool"
    return
  fi

  echo "keytool was not found. Install Android Studio or a JDK 17+ and run this script again." >&2
  exit 1
}

keytool_path="$(find_keytool)"

if [[ -e "$keystore_path" ]]; then
  echo "Refusing to overwrite the existing release keystore at $keystore_path." >&2
  echo "Preserve that file as the permanent signing identity." >&2
  exit 1
fi

mkdir -p "$output_directory"

cat <<'EOF'

Creating the permanent OPRA EQ for UAPP GitHub release-signing key.
The password prompt comes from keytool and stays on this Mac.
Choose a strong unique password and store it in your password manager.
Do not post the password, keystore, or Base64 file in GitHub Issues or chat.

EOF

"$keytool_path" \
  -genkeypair \
  -v \
  -keystore "$keystore_path" \
  -storetype PKCS12 \
  -alias "$alias_name" \
  -keyalg RSA \
  -keysize 4096 \
  -validity 10000 \
  -dname "CN=OPRA EQ for UAPP,O=weekssa"

chmod 600 "$keystore_path"
base64 < "$keystore_path" | tr -d '\n' > "$base64_path"
chmod 600 "$base64_path"

cat <<EOF

Keystore created successfully.
Private keystore: $keystore_path
Base64 copy for GitHub Actions secret: $base64_path
Key alias: $alias_name

Back up the .p12 file in at least two secure locations before publishing any APK.
The Base64 file is just another representation of the same private key and must also be treated as secret.

Next, keytool will ask for the keystore password again and display the certificate fingerprints.
The SHA-256 certificate fingerprint is PUBLIC information; copy only that fingerprint for the release record.

EOF

"$keytool_path" -list -v -keystore "$keystore_path" -alias "$alias_name"

cat <<'EOF'

Signing identity generation is complete.
EOF
