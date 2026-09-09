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

## v0.5.0 release gate

Status: **signed candidate provenance pinned; Black Pearl qualified; JA11/JM12 hardware validation pending and explicitly deferred/non-blocking for v0.5.0; public release not yet authorized pending release closeout**.

### Product/source state

- [x] `versionName` is `0.5.0` and `versionCode` is `5` on `v0.5-kt02h20-direct-flash`.
- [x] Application ID remains `com.weekssa.opraeqforuapp`.
- [x] `CHANGELOG.md` contains the v0.5.0 feature/change/validation record.
- [ ] Curated `docs/releases/v0.5.0.md` release notes are prepared.
- [x] The permanent Android signing identity remains pinned and unchanged.
- [x] Candidate source commit `30535bd3b1bce9940d23e8735d88a4d9b6a9a4ef` contains the merged MAD-style architecture refactor while explicitly preserving device/DSP/conversion behavior.
- [x] Release-record reconciliation after `30535bd3...` is documentation-only and does not alter the pinned candidate APK. Any later Android/device/DSP behavior change requires a new exact candidate assessment.

### Exact candidate automated gates

For candidate source `30535bd3b1bce9940d23e8735d88a4d9b6a9a4ef`:

- [x] Android CI run #1018 / run ID `34295024047` completed successfully.
- [x] CodeQL run #899 / run ID `34295024125` completed successfully.
- [x] Signed EQ Library Beta Candidate run #691 / run ID `34295020653` completed successfully.
- [x] Catalog currentness CI completed successfully for the candidate source.
- [x] Priority community coverage CI completed successfully for the candidate source.
- [x] Automatic Dependency Submission run #1143 / run ID `34295022902` completed successfully.

### Signed candidate identity

- [x] Signed APK is `EQ-Library-v0.5.0-beta-30535bd.apk`.
- [x] Signed APK SHA-256 is `5a2d4ff47097b1ba37b6bd625a4bfd3de444bf1895d4c0d0484fa2075adea042`; it matches the checksum shipped inside the Actions artifact.
- [x] `apksigner` verification reports one signer, RSA 4096, APK Signature Scheme v2 verified and v3 verified.
- [x] Signer DN is `CN=OPRA EQ for UAPP, O=weekssa`.
- [x] Signer certificate SHA-256 is `65c1c1256dae3c49e3548f334c91f0ba991969e9be9e0b223ba4e253d2114747`, exactly matching `release-signing-cert.sha256` after normalizing punctuation/case.
- [x] GitHub Actions artifact ID is `10082967650`, named `EQ-Library-signed-beta-30535bd3b1bce9940d23e8735d88a4d9b6a9a4ef`.
- [x] Artifact ZIP SHA-256 is `c55d7b53e7b355e85a5b54b2b9a0da6925448c35563f85796605885ad6de91c3`.

### Hardware qualification state

- [x] TRN Black Pearl destructive/fidelity/persistence regression passed on signed behavior candidate `34a9cd819466cb301456c052eecadb02e6271e5e` / APK SHA-256 `b03bf244473b8640011718c0918c7ee7b6a2d7f4aa1483817d72660c70f8a43f` on Pixel 9.
- [x] The informational Black Pearl Reset-result wording correction was followed by the focused confirmation required by `docs/BLACK_PEARL_V0.5_REGRESSION.md`; result **PASS**. Black Pearl v0.5 hardware/DSP qualification is complete.
- [x] The later `30535bd3...` architecture candidate preserves device/DSP/conversion behavior and passed the exact signed/automated gates above, so the completed Black Pearl qualification remains valid through this documentation-only release closeout.
- [ ] FiiO JA11 hands-on qualification is complete. **Current status: Hardware validation pending — deferred to the next incremental release; not a v0.5.0 publication blocker.**
- [ ] Stock JCALLY JM12 hands-on qualification is complete. **Current status: Hardware validation pending — deferred to the next incremental release; not a v0.5.0 publication blocker.** Power-cycle persistence remains unclaimed until physically established.

JA11/JM12 are intentionally shipping, if v0.5.0 is published, as **implemented but not hardware-qualified** outputs. Their pending status does not invalidate the qualified Black Pearl path and does not block v0.5.0. Release/in-app wording must continue to state their pending status accurately. When the devices arrive, refresh their hands-on records to the exact signed candidate for the next incremental release before physical qualification.

### Remaining release closeout

- [ ] Prepare curated `docs/releases/v0.5.0.md` and reconcile final public-facing release notes/README wording with the qualified-vs-pending hardware matrix.
- [ ] Confirm the final documentation/release-preparation PR #14 head is green for all gates required by its changed paths.
- [ ] Review PR #14's final diff and remove draft status only when release closeout is intentionally ready to proceed.
- [ ] Merge PR #14 to `main` without bypassing validation.
- [ ] Confirm merged/final `main` source passes the required Android CI and CodeQL gates.
- [ ] Confirm tag `v0.5.0` and a public `v0.5.0` release do not already exist.
- [ ] Run the controlled signed-release publication workflow from the exact intended `main` source only after the merged source is green and public release is intentionally authorized.
- [ ] Verify the publish workflow rebuilds/tests/signs from the exact release source and the actual signer still matches the permanent pinned certificate.
- [ ] Verify the published `v0.5.0` tag points at the intended finalized source commit and release assets/checksums/signature verification are complete.
- [ ] Verify GitHub's latest-release metadata exposes v0.5.0 so existing installations can discover the update.

The signed `30535bd3...` beta is a pinned qualification/candidate artifact, not authorization to publish that beta artifact directly as the public release. Public publication must still rebuild and verify the exact finalized release source through the controlled release workflow.

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
