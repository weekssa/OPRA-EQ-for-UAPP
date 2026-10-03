# v0.7.2 release state

## Published release

- Repository: `weekssa/OPRA-EQ-for-UAPP`
- Release: [EQ Library v0.7.2](https://github.com/weekssa/OPRA-EQ-for-UAPP/releases/tag/v0.7.2), published 2026-10-03 and latest
- Version: `0.7.2`, versionCode `9`; package `com.weekssa.opraeqforuapp`
- Release source and annotated tag target: `b8e90b9b53fc63ea00fefa499d7d4bd6ce4d55ea`
- Candidate run: [37099431204](https://github.com/weekssa/OPRA-EQ-for-UAPP/actions/runs/37099431204), artifact `11265653006`, ZIP SHA-256 `dc2607ad3b8c43aae4b0d41502ff5f13ce03d8b7b635511941f8271be255ab33`
- Promotion run: [37099991693](https://github.com/weekssa/OPRA-EQ-for-UAPP/actions/runs/37099991693); candidate verification and API 35 baseline install, in-place upgrade, and cold launch passed; tag creation and publication succeeded
- APK SHA-256: `efdd63ddb305d0624f805cc53e4ce27aae7d1ddeb169e8f965302d0f290ba64a`
- Pinned signer SHA-256: `65c1c1256dae3c49e3548f334c91f0ba991969e9be9e0b223ba4e253d2114747`
- R8 mapping SHA-256: `2ef382f4838c49c89fbea9985fdba976bedef5202aa59711dcbe263690515021`
- All six uploaded release assets were downloaded and their bytes matched GitHub's SHA-256 metadata; GitHub also lists two generated source archives. APK bytes match the candidate and its published checksum. `/releases/latest` resolves to v0.7.2.
- No physical DAC writes were performed; v0.7.2 adds no DAC support claim.

## Source, checks, and review

- Stabilization branch: `codex/v0.7.2-stabilization`
- PR #65 product work merged normally as `a60411bebfdbd1cea4218d3bde45013bb7ed26a9` after all eight exact-head checks and independent review passed.
- PR #66 fixed the prefixed Actions artifact digest comparison and merged normally. Its exact tested head is `11275d510f3a0b9b1ad4aa4edae2c25bd2044b4b`; all seven checks and independent code/documentation review passed. Its tree matches release source `b8e90b9b53fc63ea00fefa499d7d4bd6ce4d55ea`.
- PR #65 exact-head CI included API 35 UI instrumentation (25/25) and API 26 x86_64 cold-install, catalog readiness, and 60-second survival evidence. Post-merge local Gradle validation on the product changes passed 731 JVM tests, lint, debug/release assembly, and R8 mapping verification.
- The promotion defect was reproduced in historical run `37096259477`; its tag job failed before tag creation and its publish job was skipped. PR #66 corrected the comparison. Fresh candidate run `37099431204` and promotion run `37099991693` passed on the corrected release source.
- The release-tree Python validation passed 238/238 tests under bundled Python 3.12; the release promotion contract passed. Registry validation passed for 15 sources; Favorite validation passed for 14 samples, 13 profiles, and 2 explicit exclusions.

## Remaining tracked risk and handoff

- 51 transitive Maven Dependabot alerts (3 critical, 20 high, 26 medium, 2 low) remain in Gradle/build/emulator/test components. None was found in v0.7.2 `releaseRuntimeClasspath` or mapped minified DEX; build-environment risk remains and is tracked separately.
- No release gate or owner action remains. The docs-only closeout update is being recorded on the stabilization branch; its normal PR and checks are documentation maintenance after publication.
- Detailed checks, decisions, blockers, artifacts, and recovery history are in `CHECKLIST.md`, `TEST_MATRIX.md`, `DECISIONS.md`, `BLOCKERS.md`, and `ARTIFACTS.md`.
