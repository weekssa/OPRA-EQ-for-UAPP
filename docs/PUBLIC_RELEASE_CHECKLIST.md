# Public release checklist — EQ Library

This document records the GitHub-release gates for EQ Library. Google Play remains intentionally out of scope until a later product decision.

The checklist is organized around the **current release state**. Detailed historical implementation/testing evidence remains in the versioned release notes and hands-on checklists instead of being duplicated indefinitely here.

## Repository release readiness

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
- [x] The repository front page describes the current **EQ Library** product rather than the original OPRA-only workflow.

## Continuing release invariants

These apply to every installable GitHub release:

- Keep application ID `com.weekssa.opraeqforuapp` unchanged.
- Keep the permanent release-signing identity unchanged.
- Increment Android `versionCode` for every installable release.
- Use SemVer `0.x` during development; reserve `v1.0.0` for the first stable release.
- Update `CHANGELOG.md` and curated release notes for every release.
- Build, test, and sign from the exact intended source commit.
- Require the applicable automated gates before physical qualification/publication.
- Require hands-on testing whenever the release changes behavior covered by a physical hardware qualification gate.
- Never replace an already-published APK with differently signed or different-content bytes under the same version/tag.
- Never commit signing keys, passwords, tokens, or credentials.

# v0.5.0 — COMPLETE

**Published:** 2026-09-09  
**Tag:** `v0.5.0`  
**Exact release source:** `ff2fa351d5f38f9dcf37a77859f1e988bbdb76a8`  
**Signed GitHub Release run:** #5 / run ID `34341588059`  
**Public APK SHA-256:** `58e6ac5c62f9af1caf354f97cf2e7d9e2bcddea9937fac3c35532c279cd429eb`

Status: **Phase 1 implementation/release preparation and Phase 2 release-candidate testing are complete. The controlled public v0.5.0 publication and post-publication verification are also complete.**

Phase 2 tested the same v0.5.0 milestone built in Phase 1. Testing did not create a separate installable version and did not require a v0.6.0 version bump.

## Product/source state

- [x] `versionName` is `0.5.0` and `versionCode` is `5`.
- [x] Application ID remains `com.weekssa.opraeqforuapp`.
- [x] `CHANGELOG.md` contains the v0.5.0 feature/change/validation record.
- [x] Curated `docs/releases/v0.5.0.md` release notes are present.
- [x] README/front-page copy describes the shipped v0.5.0 output registry and hardware qualification state.
- [x] The permanent Android signing identity remains pinned and unchanged.

## Phase 1 — implementation and automated candidate qualification

Candidate source `30535bd3b1bce9940d23e8735d88a4d9b6a9a4ef` contains the MAD-style architecture refactor while preserving device/DSP/conversion behavior.

- [x] Android CI run #1018 / run ID `34295024047` completed successfully.
- [x] CodeQL run #899 / run ID `34295024125` completed successfully.
- [x] Signed EQ Library Beta Candidate run #691 / run ID `34295020653` completed successfully.
- [x] Catalog currentness passed for the candidate source.
- [x] Priority community coverage passed for the candidate source.
- [x] Automatic Dependency Submission run #1143 / run ID `34295022902` completed successfully.

Pinned signed candidate record:

- APK: `EQ-Library-v0.5.0-beta-30535bd.apk`
- APK SHA-256: `5a2d4ff47097b1ba37b6bd625a4bfd3de444bf1895d4c0d0484fa2075adea042`
- Signer DN: `CN=OPRA EQ for UAPP, O=weekssa`
- Signer certificate SHA-256: `65c1c1256dae3c49e3548f334c91f0ba991969e9be9e0b223ba4e253d2114747`
- Actions artifact ID: `10082967650`
- Artifact ZIP SHA-256: `c55d7b53e7b355e85a5b54b2b9a0da6925448c35563f85796605885ad6de91c3`

## Phase 2 — Pixel 9 release-candidate testing

- [x] `docs/V0.5_HANDS_ON_RELEASE_CHECKLIST.md` completed on the primary Pixel 9 with final result **PASS** on 2026-09-09.
- [x] In-place upgrade/state retention passed.
- [x] Navigation/lifecycle and unsaved-selection recreation passed.
- [x] Catalog/search/refresh/offline behavior passed.
- [x] Output-registry and hardware-validation-pending wording passed.
- [x] Fidelity/adaptation-reason presentation passed.
- [x] UAPP/ToneBoosters export/import passed.
- [x] Additional file-output testing passed.
- [x] SAF ownership/recovery invariants exercised by the checklist passed.
- [x] Non-destructive Black Pearl connection/lifecycle/Cancel smoke passed.
- [x] Update/What's New presentation passed.
- [x] Final regression sweep passed.

The Android build string and pre-upgrade app version were not captured during the pass and remain explicitly documented as unknown rather than inferred after the fact.

## Hardware qualification state at v0.5.0 publication

- [x] **TRN Black Pearl** — qualified for the v0.5 path. The destructive/fidelity/persistence regression passed, including Exact Flash, Optimized complete-response Flash, power-cycle persistence, Reset-to-flat persistence, and the explicit out-of-validated-range caution path.
- [ ] **FiiO JA11** — **Hardware validation pending**. Software implementation shipped, but physical qualification remains deferred until hardware is available.
- [ ] **JCALLY JM12 (stock firmware)** — **Hardware validation pending**. Software implementation shipped; power-cycle persistence remains unclaimed until physically established.

JA11/JM12 pending status does not retroactively change the v0.5.0 publication result. Their release/in-app wording must continue to state the pending status accurately until future exact-candidate hardware checklists pass.

## Merge and exact-main validation

- [x] Final PR #14 head `a94aedfd8533e94496908bec3460cf9e5178760f` passed Android CI #1046, CodeQL #927, Catalog currentness #982, and Priority community coverage #470.
- [x] PR #14 final diff was reviewed and the PR was intentionally moved out of draft before merge.
- [x] PR #14 merged to `main` without bypassing validation; merge commit `58d761b5d9677b5605613c6e61cfc06f6ea831d9`.
- [x] Merged main passed Android CI #1047 and CodeQL #928.
- [x] Phase-semantics documentation corrections after that merge changed documentation only.
- [x] Final publication source `ff2fa351d5f38f9dcf37a77859f1e988bbdb76a8` passed Android CI #1049, CodeQL #930, and Automatic Dependency Submission #1174 before publication.
- [x] No `v0.5.0` tag or public release existed before the controlled publication run.

## Controlled public publication

- [x] **Signed GitHub Release #5** was started from `main` with `mode=publish`, `tag=v0.5.0`, and `confirm_publish=PUBLISH`.
- [x] Run ID `34341588059` completed successfully.
- [x] `build-signed-apk` completed successfully, including exact-source checkout, release input validation, signing-secret validation, release gate/build, alignment, signing, and APK verification.
- [x] `publish-release` completed successfully, including publication confirmation, duplicate-tag protection, GitHub Release creation, and immutable version-tag creation.
- [x] Public tag `v0.5.0` points to exact source `ff2fa351d5f38f9dcf37a77859f1e988bbdb76a8`.
- [x] Public release contains `EQ-Library-v0.5.0.apk`.
- [x] Public release contains `EQ-Library-v0.5.0.apk.sha256`.
- [x] Public release contains `apksigner-verification.txt`.
- [x] Public APK SHA-256 is `58e6ac5c62f9af1caf354f97cf2e7d9e2bcddea9937fac3c35532c279cd429eb`.
- [x] Release signer matches the permanently pinned Android release certificate.
- [x] GitHub latest-release metadata exposes **v0.5.0**, allowing installed clients to discover the update.

The beta candidate remains a qualification artifact only. The public v0.5.0 APK was rebuilt and signed from the exact finalized publication source through the controlled workflow.

## Previous public releases

- **v0.4.0** — focused TRN Black Pearl Reset EQ to flat release; published 2026-09-06.
- **v0.3.0** — source-agnostic EQ Library foundation and TRN Black Pearl Direct Flash; published 2026-08-31.
- **v0.2.0** — EQ Library rebrand and device-targeted export foundation; published 2026-08-28.
- **v0.1.0** — first signed public Android release; published 2026-08-16.

Version-specific details belong in `CHANGELOG.md`, `docs/releases/`, and the applicable hands-on/protocol records rather than being copied into the current release gate.

## Explicitly deferred

The following are not required for the current GitHub development-release path:

- Google Play Console setup;
- Play App Signing;
- Play Store listing assets/forms;
- Play testing-track requirements;
- Play-specific update routing.

Those items will be handled separately when Google Play work is intentionally started.
