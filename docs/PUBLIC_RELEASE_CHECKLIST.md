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

## v0.4.0 release gate

### Product/source state

- [x] `versionName` is `0.4.0` and `versionCode` is `4` on the release-preparation branch.
- [x] Application ID remains `com.weekssa.opraeqforuapp`.
- [x] `CHANGELOG.md` contains the v0.4.0 feature/change/validation record.
- [x] Curated `docs/releases/v0.4.0.md` release notes are prepared.
- [x] README/front-page copy includes Black Pearl **Reset EQ to flat** and the v0.4.0 release-candidate status.
- [x] The permanent Android signing identity remains pinned and unchanged.

### Automated and hands-on qualification

- [x] Exact Black Pearl device/DSP candidate `15f220bd055a2aec49c0cb97c16acbd43ac588da` passed Android unit tests, Android lint, debug assembly, release assembly, and CodeQL.
- [x] The same exact candidate passed the signed-beta gate, APK alignment/signature verification, and pinned signing-certificate verification.
- [x] Signed hardware candidate SHA-256 is `96d9ea12caf8c7944ecd059f7fdda533d1c936c5ed9583910a3d3ab01168c3cf`.
- [x] The same signed candidate passed the focused Pixel 9 / TRN Black Pearl Reset EQ to flat checklist on 2026-09-06.
- [x] Focused hardware sections 1–7 passed; controlled mid-transfer disconnect injection was not exercised, with PEQ/final-gain-write failure ordering and retry safety covered by automated domain tests.
- [x] Release-preparation changes after the hardware-qualified device/DSP commit are restricted to version/release/documentation metadata.
- [ ] Final exact PR #6 head passes Android CI and CodeQL after the v0.4.0 release-preparation metadata changes.

### Merge and final release source

- [ ] Mark PR #6 ready only after its final exact head is green.
- [ ] Merge PR #6 to `main` without bypassing validation.
- [ ] Confirm merged/final `main` source passes Android CI and CodeQL.
- [ ] Confirm tag `v0.4.0` and a public `v0.4.0` release do not already exist.
- [ ] Run the controlled **Signed GitHub Release** workflow from `main` with `mode=publish`, `tag=v0.4.0`, and `confirm_publish=PUBLISH` only after the merged exact source is green.
- [ ] Verify the publish workflow's exact-source tests, lint, release assembly, APK alignment/signature verification, and pinned certificate check all pass.
- [ ] Verify the published `v0.4.0` tag points at the intended finalized source commit and the release contains `EQ-Library-v0.4.0.apk`, its SHA-256 file, and `apksigner-verification.txt`.
- [ ] Verify GitHub's latest-release metadata exposes v0.4.0 so installed v0.3.0 clients can discover the update.

Catalog/currentness/community workflows are path-scoped. PR #6 changes no catalog/source-pipeline paths, so this focused app release does not require republishing or mutating the canonical catalog.

## Previously published releases

### v0.1.0

The first GitHub binary release established the permanent application/signing identity and completed its signed Pixel 9/UAPP smoke gate before publication.

### v0.2.0

The second GitHub release preserved the same application/signing identity, introduced the visible **EQ Library** rebrand and device-targeted export foundation, and passed its automated/signing plus hands-on upgrade/export validation before publication.

### v0.3.0

The third GitHub release established the source-agnostic EQ Library foundation, output-specific My EQs, qualified General EQs, explicit new-EQ review, Hide/Unhide, personal PEQ import, safer SAF ownership/currentness, and TRN Black Pearl Direct Flash. It was published on 2026-09-01 through the controlled Signed GitHub Release workflow from exact source commit `ddda2acf9c573d42283ab8ca50d276c179631b88` after automated/signing and focused Pixel 9 / Black Pearl validation.

## Explicitly deferred

The following are not required for GitHub development releases:

- Google Play Console setup;
- Play App Signing;
- Play Store listing assets/forms;
- Play testing-track requirements;
- Play-specific update routing.

Those items will be handled separately when Google Play work is intentionally started.
