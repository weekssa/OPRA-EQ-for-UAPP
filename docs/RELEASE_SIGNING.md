# Android release signing: GitHub distribution

This document covers the permanent Android signing identity used for direct GitHub APK distribution. Google Play setup is intentionally deferred.

## Security invariant

Every installable GitHub release must be signed by the same long-lived Android release key.

The private keystore and its password are credentials. They must never be committed, attached to a GitHub Issue/Release, pasted into documentation, or otherwise made public. Losing the key can prevent existing GitHub-installed users from receiving normal in-place updates; leaking it can allow an attacker to impersonate future releases.

The signing certificate fingerprint is public information and is intentionally pinned in the repository after the key is generated.

## Permanent key profile

The project uses one dedicated signing identity with these fixed public parameters:

- keystore type: PKCS12
- alias: `opra-eq-for-uapp-release`
- key algorithm: RSA
- key size: 4096 bits
- validity: 10,000 days
- certificate subject: `CN=OPRA EQ for UAPP,O=weekssa`

Do not generate a different replacement key after a public APK has shipped unless an intentional key-migration plan exists.

## Generate the key locally

Use the helper for the maintainer's local platform:

- macOS: `tools/create-release-keystore.sh`
- Windows: `tools/create-release-keystore.ps1`

Both helpers locate `keytool` from the local JDK/Android Studio, create the keystore under the user's home directory rather than inside the repository, and create a Base64 text representation for GitHub Actions. The helpers never store the password; `keytool` prompts for it locally.

On macOS, run the helper from Terminal with:

```bash
bash tools/create-release-keystore.sh
```

Expected local outputs on either platform:

- `~/OPRA-EQ-release-signing/opra-eq-for-uapp-release.p12`
- `~/OPRA-EQ-release-signing/opra-eq-for-uapp-release.p12.base64.txt`

Both files contain the private signing key and must be protected as secrets.

Before any public APK is published:

1. Save the keystore password in a password manager.
2. Back up the `.p12` keystore in at least two controlled secure locations.
3. Keep the Base64 copy only where needed to populate the GitHub Actions secret; it is equally sensitive.
4. Record only the SHA-256 certificate fingerprint in the repository file `release-signing-cert.sha256`.

## GitHub Actions secrets

The repository release workflow expects exactly these Actions secrets:

- `OPRA_RELEASE_KEYSTORE_BASE64`: the full one-line contents of `opra-eq-for-uapp-release.p12.base64.txt`.
- `OPRA_RELEASE_KEYSTORE_PASSWORD`: the password chosen when the PKCS12 keystore was created.
- `OPRA_RELEASE_KEY_ALIAS`: `opra-eq-for-uapp-release`.

Secrets are scoped only to the workflow steps that need them. The checkout/setup actions never receive the signing secrets.

## Signed candidate and release workflow

`.github/workflows/github-release.yml` is now a candidate-only workflow manually dispatched from `main`. The signed beta workflow is also manual and main-only; neither signing workflow accepts a PR/feature-branch ref. This keeps branch-controlled Gradle and workflow code away from the release key. Checkout credentials are disabled, and signing secrets are introduced only in the post-build signing step. The candidate workflow:

1. requires exact `vMAJOR.MINOR.PATCH` tag syntax;
2. requires the requested version to equal Android `versionName`;
3. requires curated `docs/releases/<tag>.md` release notes;
4. requires the public pinned signing-certificate SHA-256 fingerprint;
5. runs unit tests, Android lint, and the release build from the exact selected `main` commit;
6. requires R8 to produce a mapping with at least one renamed application class;
7. aligns and signs the unsigned APK with Android build tools;
8. verifies the APK signature and runs `zipalign -c` on the signed APK;
9. refuses to continue if the actual signing certificate does not match the pinned public fingerprint;
10. creates a SHA-256 checksum and source/package/version/signer/R8 candidate manifest; and
11. uploads the signed outputs as a 90-day Actions artifact.

Build Tools `apksigner` may also produce an APK Signature Scheme v4 sidecar named
`EQ-Library-<tag>.apk.idsig` in that artifact. The publisher accepts only that exact optional
sidecar alongside the required candidate files, includes it in the checked Actions artifact
digest, and requires it to be nonempty. It does not publish the sidecar; the public release contains
the standalone APK whose embedded v2/v3 signatures, checksum, package, version, and alignment are
verified independently.

Public tag/release publication remains separate from candidate building. The main-only
`.github/workflows/promote-signed-release.yml` workflow accepts the tag, signed-candidate run ID,
and immutable artifact ID. It requires the successful candidate run to come from
`.github/workflows/github-release.yml` on `main`, requires the artifact to belong to that run, and
requires both the candidate source SHA and current `main` SHA to equal the publisher's exact source
commit. It verifies GitHub's artifact digest against the downloaded ZIP, requires the exact expected
file set, and validates the manifest, recomputed APK checksum, package/version, R8 mapping record,
pinned signer, APK v2/v3 signatures, alignment, and Android package metadata. The workflow also
downloads the latest public release APK, verifies its digest and signer, then installs it on a clean
API 35 emulator and upgrades that install with the exact candidate APK before a cold launch.

The publisher verifies the candidate in a read-only job, passes only the unchanged candidate ZIP to
the publish job, and repeats all candidate and APK checks there. It creates a draft release only
after verification, uploads the exact candidate APK bytes plus checksum, manifest, verification
reports, and a provenance record, verifies release-asset digests and public download bytes, checks
that `main` has not moved, and only then publishes the release. A private draft can have the exact
`target_commitish` while its Git tag ref is still absent; the publisher accepts that state and
verifies the tag after publication creates it. A later run can resume a matching asset-free draft
with no tag ref. It can retarget a previous draft only when that draft has the expected tag, name,
and notes, no uploaded assets or tag ref, and its full source SHA is verified as an ancestor of the
exact current candidate source. It reads back the new target before continuing. Drafts with assets,
diverged sources, mismatched metadata, or an unexpected tag ref fail closed for owner-reviewed
recovery. The publish request pins `target_commitish` to the verified candidate source before
publication, then requires the public tag to resolve to that same source. The publisher never
rebuilds, re-signs, or edits the APK. It marks the release latest and verifies the
`/releases/latest` metadata used by the app's update check.

The verify job has read-only repository access; the runner artifact service transfers its verified
candidate to the publish job. The publish job has release-content write access and Actions read
access. Signing secrets are not
available to either job. Every external action is pinned to a full commit SHA, and checkout
credentials are disabled. `tools/test_promote_release_candidate.py` and the workflow contract check
run in Android CI on pull requests and main.

Normal Android CI never receives the release-signing key and never publishes a development APK.
The Signed Release Candidate workflow only prepares a signed candidate. The separate main-only
promotion workflow can create the public tag and release after every promotion check passes and
the applicable owner authorization is recorded. Never dispatch it from a feature branch or infer
hardware qualification from a candidate's build/install/cold-launch result.

## Public release gate (exact-candidate promotion)

For a future version, complete the applicable release gates and receive explicit owner approval
before public publication. The v0.7.1 source-wide Favorite release is published; its exact candidate,
publication, and post-release evidence is recorded in `docs/PUBLIC_RELEASE_CHECKLIST.md`.

1. Merge the reviewed release source through the protected `main` process and pass the exact-head
   main checks.
2. Run **Signed Release Candidate** from that finalized `main` commit.
3. Verify the candidate manifest, immutable artifact identity, recomputed APK checksum, package
   and version, pinned signer, alignment, and R8 mapping.
4. Install the exact signed candidate on an emulator or owner device appropriate to the changed
   behavior, and complete the version-specific smoke test recorded in
   `docs/PUBLIC_RELEASE_CHECKLIST.md`.
5. Make no source changes that would alter the qualified release commit or artifact.
6. Confirm an explicit owner authorization applies to the exact release. Dispatch
   **Promote Signed Release Candidate** from current `main` with the candidate run and artifact IDs.
   A passing workflow publishes only the exact tested APK bytes; do not rebuild or re-sign during
   promotion.
7. Verify the public release page, tag-to-source mapping, all asset digests and downloaded APK
   bytes, signer, provenance record, release notes, and in-app `/releases/latest` metadata path.

If promotion stops after creating a matching draft, first confirm that the candidate artifact is
still available and that the current `main` source is the candidate source. A matching asset-free
draft with no tag ref can resume. When `main` advanced after an earlier failed attempt, the
publisher may update that private draft only if its recorded full source SHA is an ancestor of the
new exact candidate source and the draft has no assets or tag ref. It checks the tag, draft name,
notes, ancestry, source readback, and any already uploaded bytes before publication. Any draft with
assets from another candidate, a diverged source, a mismatched tag or notes, or an unexpected tag
ref requires owner-reviewed recovery before another attempt.
