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

`.github/workflows/github-release.yml` is manually dispatched from `main`. It has two explicit modes:

- **candidate** — builds the real signed APK, verifies it, and uploads a short-lived GitHub Actions artifact for device smoke testing. It does not create a tag or GitHub Release.
- **publish** — repeats the same signed build and verification after the candidate passes, then creates the version tag at that exact `main` commit and publishes the GitHub Release.

Both modes:

1. require exact `vMAJOR.MINOR.PATCH` tag syntax;
2. require the requested version to equal Android `versionName`;
3. require curated `docs/releases/<tag>.md` release notes;
4. require the public pinned signing-certificate SHA-256 fingerprint;
5. run unit tests, Android lint, and the release build from the exact selected `main` commit;
6. align and sign the unsigned APK with Android build tools;
7. verify the APK signature;
8. refuse to continue if the actual signing certificate does not match the pinned public fingerprint;
9. create a SHA-256 checksum file for the APK; and
10. upload the signed outputs as a short-lived Actions artifact.

Publish mode additionally requires the literal confirmation value `PUBLISH`, refuses to replace an existing tag/release, and uses the repository-provided `GITHUB_TOKEN` only in the final publication job. That final job creates the requested tag at the exact workflow commit and publishes the APK, APK checksum, and public `apksigner` verification output through GitHub Releases.

Normal Android CI never receives the release-signing key and never publishes a development APK.

## First-release gate

For `v0.1.0`:

1. Run **Signed GitHub Release** in `candidate` mode from the finalized `main` commit.
2. Download and install the signed candidate on the Pixel 9.
3. Perform the short signed-release smoke test recorded in `docs/PUBLIC_RELEASE_CHECKLIST.md`.
4. If the candidate passes, make no source changes that would alter the release commit.
5. Run the same workflow from `main` in `publish` mode with tag `v0.1.0` and confirmation `PUBLISH`.
6. Verify the public release page, assets, checksum, and in-app update metadata path.
