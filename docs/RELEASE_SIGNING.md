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

On Windows, `tools/create-release-keystore.ps1` locates `keytool` from the JDK/Android Studio, creates the keystore under the user's home directory rather than inside the repository, and creates a Base64 text representation for GitHub Actions.

The helper never stores the password. `keytool` prompts for it locally.

Expected local outputs:

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

## Release workflow

`.github/workflows/github-release.yml` runs only for pushed tags beginning with `v`, and then performs additional checks before publishing:

1. requires exact `vMAJOR.MINOR.PATCH` tag syntax;
2. requires the tag version to equal Android `versionName`;
3. requires curated `docs/releases/<tag>.md` release notes;
4. requires the public pinned signing-certificate SHA-256 fingerprint;
5. runs unit tests, Android lint, and the release build from the exact tagged source;
6. aligns and signs the unsigned APK with Android build tools;
7. verifies the APK signature;
8. refuses publication if the actual signing certificate does not match the pinned public fingerprint;
9. creates a SHA-256 checksum file for the APK; and
10. publishes the APK and checksum through GitHub Releases.

The workflow uses the repository-provided `GITHUB_TOKEN` only for the final release publication step. Normal Android CI never receives the release-signing key and never publishes a development APK.

## First-release gate

Before `v0.1.0` is published, install the signed APK on the Pixel 9 and perform the short signed-release smoke test recorded in `docs/PUBLIC_RELEASE_CHECKLIST.md`. Only after that passes should the final tag be pushed and the public GitHub Release be created.
