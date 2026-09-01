# Public release checklist — EQ Library

This checklist covers public GitHub distribution. Google Play remains intentionally out of scope until a later product decision.

## Source repository readiness

- [x] Apache-2.0 software license present.
- [x] Software provenance documented in `NOTICE`.
- [x] Source/data licensing and attribution documented separately in `DATA_LICENSE.md` and `NOTICE`.
- [x] Standalone privacy policy present in `PRIVACY.md`.
- [x] Contribution and security-reporting guidance present.
- [x] `.gitignore` excludes Android keystores, APK/AAB outputs, local configuration, Google Services configuration, IDE state, and build artifacts.
- [x] Android manifest requests only the network permission required by the app's public catalog/update checks.
- [x] Repository visibility is public.
- [x] Normal Android CI validates unit tests, Android lint, debug assembly, and unsigned release assembly without publishing development APKs.
- [x] GitHub Actions dependencies are pinned and repository security/dependency checks are enabled.
- [x] One permanent Android release-signing identity is established and its public certificate fingerprint is pinned in `release-signing-cert.sha256`.
- [x] Candidate signing is separate from public publication, and publish mode requires an explicit controlled release action.
- [x] The repository front page describes the current **EQ Library** product rather than the original OPRA-only v0.1 workflow.

## Continuing release invariants

These apply to every installable GitHub release:

- Keep application ID `com.weekssa.opraeqforuapp` unchanged.
- Keep the permanent release-signing identity unchanged.
- Increment Android `versionCode` for every installable release.
- Use SemVer `0.x` during development; reserve `v1.0.0` for the first stable release.
- Update `CHANGELOG.md` and curated release notes for every release.
- Build/test/sign from the exact intended source commit.
- Never replace an already-published APK with a differently signed or different-content file under the same version/tag.
- Never commit signing keys, passwords, tokens, or credentials.

## v0.3.0 release gate

### Product/source state

- [x] `versionName` is `0.3.0` and `versionCode` is `3`.
- [x] Application ID remains `com.weekssa.opraeqforuapp`.
- [x] `CHANGELOG.md` contains the v0.3.0 feature/change/fix record.
- [x] Curated `docs/releases/v0.3.0.md` release notes are prepared.
- [x] README/front-page copy is updated for the source-agnostic EQ Library product, current output model, catalog/privacy behavior, personal import, and Black Pearl Direct Flash.
- [x] The permanent Android signing identity remains pinned and unchanged.

### Automated and hands-on qualification

- [x] The earlier v0.3 foundation passed Android/software/signing gates plus Pixel 9 / TRN Black Pearl hardware qualification, including the explicit outside-validated-range `-11.9 dB` no-clamp caution/Flash-anyway case.
- [x] PR #4 release-polish Android code at `3b95d384fb772514081383f801cf22b5b3aa8cbf` passed the focused Pixel 9 release-polish regression on 2026-09-01, including the compact managed-headphone action row and immediate new-EQ review-attention clearing.
- [x] The final release-polish diff after that device pass is restricted to release/front-page documentation and catalog-currentness synchronization unless a new Android/device/DSP change is explicitly introduced.
- [ ] Synchronize PR #4 with the latest validated `main` catalog/currentness state without losing PR #4 living-archive/General-EQ data.
- [ ] Run the full automated/currentness/security/signing gate on the resulting exact PR #4 head.
- [ ] Confirm the exact-head signed beta uses the pinned permanent signing certificate.

### Merge and final release source

- [ ] Mark PR #4 ready only after the synchronized exact head is green.
- [ ] Merge PR #4 to `main` without bypassing validation.
- [ ] Confirm the merged/final `main` release source passes the applicable release checks; catalog-only automation may advance `main` without invalidating the prior hardware pass.
- [ ] Confirm tag `v0.3.0` and a public `v0.3.0` release do not already exist.
- [ ] Run the controlled public release workflow only after explicit final publish authorization.
- [ ] Verify the published release tag points at the intended finalized source commit and contains the signed APK, checksum, and signature-verification output.
- [ ] Verify GitHub's latest-release metadata exposes v0.3.0 and the in-app public update check can see it.

## Previously published releases

### v0.1.0

The first GitHub binary release established the permanent application/signing identity and completed its signed Pixel 9/UAPP smoke gate before publication.

### v0.2.0

The second GitHub release preserved the same application/signing identity, introduced the visible **EQ Library** rebrand and device-targeted export foundation, and passed its automated/signing plus hands-on upgrade/export validation before publication.

## Explicitly deferred

The following are not required for GitHub development releases:

- Google Play Console setup;
- Play App Signing;
- Play Store listing assets/forms;
- Play testing-track requirements;
- Play-specific update routing.

Those items will be handled separately when Google Play work is intentionally started.
