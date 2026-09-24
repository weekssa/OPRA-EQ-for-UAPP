# Android release signing — GitHub distribution

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

- `OPRA_RELEASE_KEYSTORE_BASE64` — the full one-line contents of `opra-eq-for-uapp-release.p12.base64.txt`.
- `OPRA_RELEASE_KEYSTORE_PASSWORD` — the password chosen when the PKCS12 keystore was created.
- `OPRA_RELEASE_KEY_ALIAS` — `opra-eq-for-uapp-release`.

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

Public tag/release publication is deliberately unavailable in this workflow. The previous publisher rebuilt and re-signed instead of promoting the exact candidate that passed human testing, so source equality could not establish APK byte identity. A future publisher must accept an immutable candidate artifact identity, verify its source SHA, manifest, recomputed checksum, package/version, signer, alignment, and artifact digest, and publish those exact bytes. It must be separately reviewed and explicitly owner-approved before use.

Normal Android CI never receives the release-signing key and never publishes a development APK.
No workflow action here creates a public release or version tag. Candidate preparation remains a
main-only owner action after merge approval; do not dispatch it from the feature branch or infer
hardware qualification from its build/install/cold-launch result.

## Public release gate (not yet implemented for exact-candidate promotion)

For `v0.7.0`, only after every release gate and explicit owner approval:

1. Run **Signed Release Candidate** from the finalized `main` commit.
2. Download and install the signed candidate on the Pixel 9.
3. Perform the short signed-release smoke test recorded in `docs/PUBLIC_RELEASE_CHECKLIST.md`.
4. If the candidate passes, make no source changes that would alter the release commit.
5. Obtain explicit owner approval and use a separately reviewed exact-artifact promotion workflow (not yet implemented).
6. Verify the public release page, assets, checksum, signer, source provenance, and in-app update metadata path.
