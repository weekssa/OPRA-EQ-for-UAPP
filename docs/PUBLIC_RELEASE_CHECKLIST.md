# Public release checklist — OPRA EQ for UAPP

This checklist covers public GitHub distribution only. Google Play work is intentionally out of scope until a later product decision.

## Source repository readiness

- [x] Apache-2.0 software license present.
- [x] Software provenance documented in `NOTICE`.
- [x] OPRA-derived data licensing documented separately in `DATA_LICENSE.md`.
- [x] Public-facing README reflects implemented behavior and current validation status.
- [x] Standalone privacy policy present in `PRIVACY.md`.
- [x] Contribution and security-reporting guidance present.
- [x] `.gitignore` excludes Android keystores, APK/AAB outputs, local configuration, Google Services configuration, IDE state, and build artifacts.
- [x] Current-tree search found no committed credential/token/key material.
- [x] Android manifest requests only `android.permission.INTERNET`.
- [x] Pixel 9 functional/accessibility/UAPP import validation passed and is recorded in `docs/DEVICE_TEST_PLAN.md`.
- [x] Repository visibility changed from private to public.
- [x] Normal Android CI validates unit tests, Android lint, debug assembly, and unsigned release assembly without publishing development APK artifacts.
- [x] Public-repository Actions permissions are read-only by default and external-contributor workflows require approval.
- [x] GitHub Actions dependencies are pinned to full commit SHAs.
- [x] Secret Protection / push protection, Dependabot security features, and automatic dependency submission are enabled.
- [x] Advanced Kotlin CodeQL analysis passes with 0 open code-scanning alerts.
- [x] `main` ruleset prevents branch deletion and force pushes without blocking normal maintenance.
- [x] Dependabot reports 0 open runtime-scope vulnerabilities; current open alerts are development/build-tool scope.

## Stable Android release signing

Do not publish a public installable release until one permanent signing identity is established.

- [x] Document one fixed release-key profile and local generation procedure in `docs/RELEASE_SIGNING.md`.
- [x] Provide a local PowerShell helper that creates the key outside the repository and never stores its password.
- [ ] Generate one dedicated Android release keystore outside the repository.
- [ ] Back up the keystore securely in at least two controlled locations.
- [ ] Record the key alias and signing-certificate SHA-256 fingerprint in a non-secret release record.
- [ ] Store the Base64 keystore/password/alias only in GitHub Actions secrets or other approved secure stores; never commit them.
- [x] Configure a manually dispatched signed-release workflow whose signing secrets are supplied externally and scoped only to signing steps.
- [x] Candidate mode produces a signed, fingerprint-verified short-lived Actions artifact without creating a public release.
- [x] Publish mode is separate, requires explicit `PUBLISH` confirmation, refuses to replace an existing tag/release, and creates the version tag only after the signed build passes.
- [ ] Confirm future release builds can be signed by the same identity.

The signing identity is effectively part of the app's long-term update identity. Losing it can prevent users of a GitHub-distributed build from receiving normal in-place updates.

## First GitHub binary release — v0.1.0

- [x] Keep Android package ID `com.weekssa.opraeqforuapp` unchanged.
- [x] `versionName` is `0.1.0` and `versionCode` is `1`.
- [x] Curated `docs/releases/v0.1.0.md` release notes are prepared.
- [ ] Add the generated public signing-certificate SHA-256 fingerprint as `release-signing-cert.sha256`.
- [ ] Finalize the `0.1.0` changelog date before building the signed candidate.
- [ ] Run the full automated gate on the exact finalized `main` release commit.
- [ ] Run **Signed GitHub Release** in `candidate` mode for `v0.1.0` from that exact commit.
- [ ] Download the signed candidate APK and install it on the Pixel 9.
- [ ] Perform the short release-build smoke test: launch, first catalog sync, Browse/Search, add one headphone, export XML, import one preset into UAPP/ToneBoosters, Settings/About.
- [ ] Verify the signed release contains no debug-only labeling or unintended permissions.
- [ ] Confirm no source change is needed after the signed candidate passes.
- [ ] Run **Signed GitHub Release** in `publish` mode for `v0.1.0` with confirmation `PUBLISH` from the same finalized `main` commit.
- [ ] Verify that the workflow created tag `v0.1.0` at the exact release commit and published the signed APK, APK SHA-256 checksum, and public signature-verification output.
- [ ] Verify unauthenticated access to the release page and release metadata endpoint.
- [ ] Verify the app's **Check for update** path can read public release metadata.

## After publishing

- [ ] Keep the release-signing identity unchanged for subsequent GitHub releases.
- [ ] Increment `versionCode` for every installable Android release.
- [ ] Use SemVer `0.x` while the project is in development; reserve `v1.0.0` for the first stable release.
- [ ] Update `CHANGELOG.md` for every release.
- [ ] Never replace an already-published APK with a differently signed file under the same version/tag.

## Explicitly deferred

The following are not required to make the source repository public or publish the first GitHub release:

- Google Play Console setup;
- Play App Signing;
- Play Store listing assets/forms;
- Play testing-track requirements;
- Play-specific update routing.

Those items will be handled separately when Google Play work is intentionally started.
