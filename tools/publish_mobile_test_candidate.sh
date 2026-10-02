#!/usr/bin/env bash
set -euo pipefail

: "${GH_TOKEN:?Missing GitHub token}"
: "${GITHUB_REPOSITORY:?Missing repository identity}"
: "${GITHUB_SHA:?Missing exact source commit}"
: "${GITHUB_RUN_ID:?Missing workflow run identity}"
: "${CANDIDATE_DIRECTORY:?Missing downloaded candidate directory}"
: "${EXPECTED_VERSION_NAME:?Missing Gradle versionName}"
: "${EXPECTED_VERSION_CODE:?Missing Gradle versionCode}"
: "${PINNED_V06_READ_ONLY_ARTIFACT_ID:?Missing pinned v0.6 artifact ID}"
: "${PINNED_V06_READ_ONLY_COMMIT:?Missing pinned v0.6 source commit}"
: "${PINNED_V06_READ_ONLY_SHA256:?Missing pinned v0.6 APK digest}"

manifest="$CANDIDATE_DIRECTORY/candidate-manifest.json"
mapping="$CANDIDATE_DIRECTORY/r8-mapping.txt"
test -s "$manifest"
test -s "$mapping"
jq -e \
  --arg sourceSha "$GITHUB_SHA" \
  --arg versionName "$EXPECTED_VERSION_NAME" \
  --arg versionCode "$EXPECTED_VERSION_CODE" \
  --arg signerSha256 "$(tr -d '[:space:]:' < release-signing-cert.sha256 | tr '[:upper:]' '[:lower:]')" \
  '.sourceSha == $sourceSha and
   .packageId == "com.weekssa.opraeqforuapp" and
   .versionName == $versionName and
   .versionCode == ($versionCode | tonumber) and
   .signerSha256 == $signerSha256 and
   .r8MinificationEnabled == true and
   (.r8MappingSha256 | test("^[a-f0-9]{64}$"))' \
  "$manifest" >/dev/null

apk_name="$(jq -er '.apk | strings' "$manifest")"
expected_name="EQ-Library-v${EXPECTED_VERSION_NAME}-beta-${GITHUB_SHA:0:7}.apk"
test "$apk_name" = "$expected_name"
source_apk="$CANDIDATE_DIRECTORY/$apk_name"
test -s "$source_apk"
apk_sha256="$(sha256sum "$source_apk" | awk '{print $1}')"
manifest_sha256="$(jq -er '.apkSha256 | strings' "$manifest")"
test "$apk_sha256" = "$manifest_sha256"
checksum_file="${source_apk}.sha256"
test -s "$checksum_file"
awk -v digest="$apk_sha256" -v name="$apk_name" '
  $1 == digest && ($2 == name || $2 == "*" name || $2 == "dist/" name || $2 == "*dist/" name) { ok = 1 }
  END { exit !ok }
' "$checksum_file"
mapping_sha256="$(sha256sum "$mapping" | awk '{print $1}')"
test "$mapping_sha256" = "$(jq -er '.r8MappingSha256' "$manifest")"
awk '
  /^com\.weekssa\.opraeqforuapp\.[^[:space:]]+[[:space:]]+->[[:space:]]+[^[:space:]]+:$/ {
    left = $1
    right = $3
    sub(/:$/, "", right)
    if (left != right) renamed = 1
  }
  END { exit !renamed }
' "$mapping"
normalized_signer="$(tr -d '[:space:]:' < release-signing-cert.sha256 | tr '[:upper:]' '[:lower:]')"
report_signer="$(sed -n 's/^Signer #1 certificate SHA-256 digest: //p' "$CANDIDATE_DIRECTORY/apksigner-verification.txt" | head -n 1 | tr -d '[:space:]:' | tr '[:upper:]' '[:lower:]')"
test "$report_signer" = "$normalized_signer"
grep -Fq 'Verified using v2 scheme (APK Signature Scheme v2): true' "$CANDIDATE_DIRECTORY/apksigner-verification.txt"
grep -Fq 'Verified using v3 scheme (APK Signature Scheme v3): true' "$CANDIDATE_DIRECTORY/apksigner-verification.txt"
grep -Fq 'Verification successful' "$CANDIDATE_DIRECTORY/zipalign-verification.txt"

publish_dir="$(mktemp -d)"
if git clone --depth 1 --branch mobile-test-apk \
  "https://x-access-token:${GH_TOKEN}@github.com/${GITHUB_REPOSITORY}.git" "$publish_dir"; then
  :
else
  rm -rf "$publish_dir"
  publish_dir="$(mktemp -d)"
  cd "$publish_dir"
  git init
  git checkout -b mobile-test-apk
  git remote add origin "https://x-access-token:${GH_TOKEN}@github.com/${GITHUB_REPOSITORY}.git"
fi

cd "$publish_dir"
git config user.name "github-actions[bot]"
git config user.email "41898282+github-actions[bot]@users.noreply.github.com"
mkdir -p candidates

stable_name="EQ-Library-mobile-test.apk"
versioned_name="EQ-Library-v${EXPECTED_VERSION_NAME}-mobile-test.apk"
zip_name="EQ-Library-v${EXPECTED_VERSION_NAME}-mobile-test.zip"
immutable_name="EQ-Library-v${EXPECTED_VERSION_NAME}-beta-${GITHUB_SHA:0:7}-${GITHUB_RUN_ID}.apk"

cp "$source_apk" "$stable_name"
cp "$source_apk" "$versioned_name"
immutable_path="candidates/$immutable_name"
if [[ -e "$immutable_path" ]]; then
  cmp -s "$source_apk" "$immutable_path" || {
    echo "Refusing to replace an existing immutable beta candidate with different bytes." >&2
    exit 1
  }
else
  cp "$source_apk" "$immutable_path"
fi
zip -9 -j "$zip_name" "$versioned_name"
sha256sum "$stable_name" > "${stable_name}.sha256"
sha256sum "$versioned_name" > "${versioned_name}.sha256"
sha256sum "$zip_name" > "${zip_name}.sha256"
sha256sum "$immutable_path" > "candidates/${immutable_name}.sha256"

pinned_name="EQ-Library-v0.6.0-beta-${PINNED_V06_READ_ONLY_COMMIT:0:7}.apk"
if [[ ! -f "candidates/$pinned_name" ]]; then
  pinned_zip="$RUNNER_TEMP/pinned-v06-read-only.zip"
  pinned_dir="$RUNNER_TEMP/pinned-v06-read-only"
  mkdir -p "$pinned_dir"
  curl --fail --location --retry 3 \
    -H "Authorization: Bearer ${GH_TOKEN}" \
    -H "Accept: application/vnd.github+json" \
    "https://api.github.com/repos/${GITHUB_REPOSITORY}/actions/artifacts/${PINNED_V06_READ_ONLY_ARTIFACT_ID}/zip" \
    --output "$pinned_zip"
  unzip -q "$pinned_zip" -d "$pinned_dir"
  pinned_source="$(find "$pinned_dir" -type f -name "$pinned_name" -print -quit)"
  test -n "$pinned_source"
  actual_pinned_sha="$(sha256sum "$pinned_source" | awk '{print $1}')"
  test "$actual_pinned_sha" = "$PINNED_V06_READ_ONLY_SHA256"
  cp "$pinned_source" "candidates/$pinned_name"
  printf '%s  %s\n' "$PINNED_V06_READ_ONLY_SHA256" "$pinned_name" > "candidates/${pinned_name}.sha256"
fi
test -f "candidates/$pinned_name"
test "$(sha256sum "candidates/$pinned_name" | awk '{print $1}')" = "$PINNED_V06_READ_ONLY_SHA256"

cat > README.md <<EOF
# EQ Library mobile test builds

Latest signed beta generated from commit ${GITHUB_SHA} for app version ${EXPECTED_VERSION_NAME}.

Latest convenience APK (moves as the development branch advances):
https://raw.githubusercontent.com/${GITHUB_REPOSITORY}/mobile-test-apk/${stable_name}

Latest version convenience APK (also moves while version ${EXPECTED_VERSION_NAME} is under development):
https://raw.githubusercontent.com/${GITHUB_REPOSITORY}/mobile-test-apk/${versioned_name}

Exact immutable candidate for this commit:
https://raw.githubusercontent.com/${GITHUB_REPOSITORY}/mobile-test-apk/candidates/${immutable_name}

Pinned v0.6 Black Pearl read-only qualification candidate:
https://raw.githubusercontent.com/${GITHUB_REPOSITORY}/mobile-test-apk/candidates/${pinned_name}

ZIP fallback for the latest convenience build:
https://raw.githubusercontent.com/${GITHUB_REPOSITORY}/mobile-test-apk/${zip_name}

Files under candidates/ are append-only exact-commit test candidates. Stable/versioned convenience names are not qualification provenance.
This branch is only a temporary hands-on testing surface. It is not a public release.
EOF

git add README.md "$stable_name" "$stable_name.sha256" \
  "$versioned_name" "$versioned_name.sha256" "$zip_name" "$zip_name.sha256" candidates/
if git diff --cached --quiet; then
  echo "No mobile-test publication changes."
else
  git commit -m "Publish signed mobile test APK from ${GITHUB_SHA:0:7}"
  git push origin HEAD:mobile-test-apk
fi

{
  echo "### Mobile test APK"
  echo "Latest convenience: https://raw.githubusercontent.com/${GITHUB_REPOSITORY}/mobile-test-apk/${stable_name}"
  echo ""
  echo "Exact immutable candidate: https://raw.githubusercontent.com/${GITHUB_REPOSITORY}/mobile-test-apk/candidates/${immutable_name}"
  echo ""
  echo "Pinned v0.6 read-only candidate: https://raw.githubusercontent.com/${GITHUB_REPOSITORY}/mobile-test-apk/candidates/${pinned_name}"
} >> "$GITHUB_STEP_SUMMARY"
