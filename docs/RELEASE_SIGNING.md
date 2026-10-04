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

`.github/workflows/github-release.yml` is a candidate-only workflow manually dispatched from `main`. Its tag input has no version default. The signed beta workflow is also manual and main-only; neither signing workflow accepts a PR/feature-branch ref. Both workflows read `versionName` and `versionCode` from the checked-out Gradle file instead of carrying a release-specific version assertion. This keeps branch-controlled Gradle and workflow code away from the release key. Checkout credentials are disabled, and signing secrets are introduced only in the post-build signing step. The candidate workflow:

1. requires exact `vMAJOR.MINOR.PATCH` tag syntax;
2. requires the requested tag version to equal Android `versionName` and reads the positive `versionCode` from that same Gradle source;
3. requires curated `docs/releases/<tag>.md` release notes;
4. requires the public pinned signing-certificate SHA-256 fingerprint;
5. runs unit tests, Android lint, and the release build from the exact selected `main` commit;
6. requires R8 to produce a mapping with at least one renamed application class;
7. aligns and signs the unsigned APK with Android build tools;
8. verifies the APK signature and runs `zipalign -c` on the signed APK;
9. refuses to continue if the actual signing certificate does not match the pinned public fingerprint;
10. creates a SHA-256 checksum and source/package/version/signer/R8 candidate manifest, retaining the exact R8 mapping in the private candidate artifact; and
11. uploads the signed outputs as a 90-day Actions artifact.

Build Tools `apksigner` may also produce an APK Signature Scheme v4 sidecar named
`EQ-Library-<tag>.apk.idsig` in that artifact. The publisher accepts only that exact optional
sidecar alongside the required candidate files, includes it in the checked Actions artifact
digest, and requires it to be nonempty. It does not publish the sidecar; the public release contains
the standalone APK whose embedded v2/v3 signatures, checksum, package, version, and alignment are
verified independently.

Public tag/release publication remains separate from candidate building. The main-only
`.github/workflows/promote-signed-release.yml` workflow accepts the tag, signed-candidate run ID,
and immutable artifact ID. Its read-only verification job requires the successful candidate run to come from
`.github/workflows/github-release.yml` on `main`, requires the artifact to belong to that run, and
requires both the candidate source SHA and current `main` SHA to equal the publisher's exact source
commit. It verifies GitHub's artifact digest against the downloaded ZIP, requires the exact expected
file set, recomputes the private R8 mapping digest and confirms a renamed app class, and validates
the manifest, APK checksum, package/version, pinned signer, APK v2/v3 signatures, alignment, and
Android package metadata. The workflow also
downloads the latest public release APK, verifies its digest and signer, then installs it on a clean
API 35 emulator and upgrades that install with the exact candidate APK before a cold launch.

The promotion workflow orders three gates. First, the read-only verification job validates the
candidate and completes the clean API 35 install, in-place upgrade, and cold launch. Second, a
separate tag job rechecks the immutable Actions artifact and creates an annotated candidate tag only after
verification succeeds. Its tag message binds the `Release-Tag`, `Source-SHA`, `Candidate-Run-ID`,
`Candidate-Artifact-ID`, and `Candidate-Artifact-SHA256` to the exact candidate. It rejects lightweight tags, mismatched annotations,
an already-used version, and a `main` branch that moved. A retry may reuse only the same annotated
tag and candidate tuple; it never moves a ref. The application remains byte-for-byte tied to the
candidate. The exact mapping file is retained only in the private candidate artifact and is never a
public release asset.

Third, the publish job downloads only the unchanged candidate ZIP and repeats the candidate and APK
checks. It creates or resumes a private draft only when the annotated tag binds the exact candidate,
uploads the exact candidate APK bytes plus checksum, manifest, verification reports, and provenance,
verifies release-asset digests and public download bytes, checks that `main` has not moved, and only
then publishes. A partial draft may resume only for the same tag, source, notes, and immutable
candidate annotation. A missing or lightweight tag, a different source or artifact tuple, mismatched
release metadata, or a changed `main` fails closed for owner-reviewed recovery. The publisher does
not rebuild, re-sign, retarget, or modify the tag or APK. GitHub documents in the [release endpoint
reference](https://docs.github.com/en/rest/releases/releases#create-a-release) that
`target_commitish` is unused when the release tag already exists, so the annotated tag object is the
source-of-truth binding. It marks the release latest and verifies the `/releases/latest` metadata
used by the app's update check.

The verify job has read-only repository access. Only the tag and publish jobs have content-write
permission; the runner artifact service transfers the verified candidate to the publish job. The
beta candidate build is read-only, and a separate writer downloads and checks its exact artifact
before updating `mobile-test-apk`. Signing secrets are not
available to either job. Every external action is pinned to a full commit SHA, and checkout
credentials are disabled. `tools/test_promote_release_candidate.py` and the workflow contract check
run in Android CI on pull requests and main.

Normal Android CI never receives the release-signing key and never publishes a development APK.
The Signed Release Candidate workflow only prepares a signed candidate. The separate main-only
promotion workflow can create the public tag and release after every promotion check passes and
the applicable owner authorization is recorded. Never dispatch it from a feature branch or infer
hardware qualification from a candidate's build/install/cold-launch result.

## Public release gate (exact-candidate promotion)

### Record release-engineering execution provenance

For each future release, add concise execution-provenance rows to that version's `ARTIFACTS.md`. Record skills and tools separately; do not infer use from installation or availability. Use `not declared` when a version or source is unavailable, and `NO` when an item was inspected but not invoked.

| Skill | Source/version | Purpose in this release | Actually invoked |
|---|---|---|---|
| `<skill name>` | `<source and version, or not declared>` | `<specific use>` | `YES` / `NO` |

| Tool | Version | Trusted source | Purpose in this release | Verification/hash | Actually invoked |
|---|---|---|---|---|---|
| `<tool name>` | `<version>` | `<official source or configured runtime>` | `<specific use>` | `<published digest/checksum when downloaded manually, otherwise not applicable>` | `YES` / `NO` |

Record manually downloaded tool checksums before running the tools. Do not backfill missing historical provenance from memory or current availability; state that it is not verifiable.

For a future version, complete the applicable release gates and receive explicit owner approval
before public publication. The v0.7.1 source-wide Favorite release is published; its exact candidate,
publication, and post-release evidence is recorded in `docs/PUBLIC_RELEASE_CHECKLIST.md`.

1. Merge the reviewed release source through the protected `main` process and pass the exact-head
   main checks.
2. Run **Signed Release Candidate** from that finalized `main` commit.
3. Independently verify the candidate manifest, immutable artifact identity, APK checksum, package
   and version, pinned signer, alignment, and private R8 mapping.
4. Install the exact signed candidate on an emulator or owner device appropriate to the changed
   behavior, and complete the version-specific smoke test recorded in
   `docs/PUBLIC_RELEASE_CHECKLIST.md`.
5. Make no source changes that would alter the qualified release commit or artifact.
6. Confirm an explicit owner authorization applies to the exact release. Dispatch
   **Promote Signed Release Candidate** from current `main` with the candidate run and artifact IDs.
   Its verification and API 35 upgrade job must finish before the annotated tag can be created.
   The tag records the exact source and immutable candidate tuple. The publish job then publishes
   only the exact tested APK bytes; it does not rebuild or re-sign during promotion.
7. Verify the public release page, annotated tag-to-source and candidate binding, all asset digests
   and downloaded APK bytes, signer, provenance record, release notes, and in-app `/releases/latest`
   metadata path.

If promotion stops after tag creation or while uploading a private draft, first confirm that the
candidate artifact remains available, `main` still matches the candidate source, and the annotated
tag binds the same run, artifact, and archive digest. A retry may continue that exact draft and
verified asset set. It may not retarget a draft or reuse the tag for another source or artifact.
Mismatched notes, assets, tag metadata, a changed `main`, or an unavailable candidate require
owner-reviewed recovery before another attempt.
