# v0.8.0 stable artifact record

This file is append-only release evidence. `NOT RUN` means no evidence is yet available for that gate.

## Pre-merge identity

- Starting main: `0f4236b64c6642b4cd7c1a0ffe2b0d9778330f01` (tree `e31fe181c012a5cd48d0573efe7e8a3d88e383a8`)
- Stable PR: draft [#73](https://github.com/weekssa/OPRA-EQ-for-UAPP/pull/73), open against `main`.
- Last exact app/test PR head/tree: `3e557e1a0f7868bc5cf54f9e363da0c9e3c41bbd` / `680564d2488e54c194488d67d8d006ba22a6ee3f`; live PR head must be refreshed before merge.
- Production app-source tree: `857d02a53d0df44fb0bd46e5ddad3b319dc48dab`, matching the qualified beta.
- Independent pre-merge reviewer: PASS on the complete stable delta and the narrow test-only correction at `3e557e1a`; no correctness blocker.
- Exact-head CI at `3e557e1a`: PASS. Android CI run `37736600658` (build/unit/lint/assembly job `113177438410`, API 26 job `113177438121`, API 35 UI job `113177438418`); CodeQL `37736600633`; Catalog currentness `37736600706`; Priority community coverage `37736600726`.
- API 35 JUnit: 64 tests, 0 failures/errors/skips; artifact `11531339710`, digest `sha256:2692842828d81789c085ea2a09d52c5ad6566997a5c819142104190a60e31888`.
- API 26 smoke: PASS; diagnostic artifact `11532521333`, digest `sha256:1ecf8c9203030da778b8e02d1e23cecd89628b0e379eb0a67e3adad64375db46`.
- Documentation-only checkpoint `17494acd5e6f974e79adaf58675499caa765f811` is the latest recorded PR head. Android CI `37738430621` and CodeQL `37738430589` were running at the last read; Catalog currentness `37738430666` and Priority community coverage `37738430590` had passed. Re-resolve the live head and all applicable checks before marking PR #73 ready or merging.
- Merged main SHA/tree: NOT RUN

## Official stable artifact

- Main-only Signed Release Candidate run/artifact: NOT RUN
- Source SHA/tree: NOT RUN
- Package: `com.weekssa.opraeqforuapp`
- VersionName/versionCode: `0.8.0` / `11`
- APK SHA-256: NOT RUN
- Artifact ZIP ID/digest: NOT RUN
- Pinned signer SHA-256: `65c1c1256dae3c49e3548f334c91f0ba991969e9be9e0b223ba4e253d2114747`
- Manifest, APK signature, alignment, and provenance verification: NOT RUN

## Exact signed API 35 evidence

- Emulator configuration: disposable API 35, Google APIs, x86_64 Pixel 6 profile; exact run details pending.
- Signed v0.7.2 / code 9 -> stable / code 11 with seeded Room, DataStore and Library state: NOT RUN
- Signed v0.8.0-beta / code 10 -> stable / code 11 with seeded Room, DataStore and Library state: NOT RUN
- Stable signed clean install, cold launch, catalog readiness, General EQs, Settings and crash buffer: NOT RUN
- Diagnostics artifact ID/SHA-256: NOT RUN

## Publication

- Stable annotated tag/source: NOT RUN
- Release ID/state/latest metadata: NOT RUN
- Public APK SHA-256 and signer: NOT RUN
- Public checksum/provenance sidecar verification: NOT RUN
- Independent final artifact review: NOT RUN
- Final review bundle: NOT RUN

## Existing immutable baselines

- v0.7.2: code 9, APK SHA-256 `efdd63ddb305d0624f805cc53e4ce27aae7d1ddeb169e8f965302d0f290ba64a`.
- v0.8.0-beta: code 10, APK SHA-256 `b352c4d10a91f9c378a1a18012054a803a23807d4c454fb9b2dde613b7729782`, immutable prerelease.
- Both are signed with pinned certificate SHA-256 `65c1c1256dae3c49e3548f334c91f0ba991969e9be9e0b223ba4e253d2114747`.


## Final stable publication and public verification — 2026-10-08

The following final disposition supersedes earlier `NOT RUN` and pending entries above without rewriting those historical checkpoints.

- Stable main SHA/tree: `54823e1a464f8b716a32c4f4808037fc0cdd99bd` / `e20330e2c4921823ecdf2881db0bf6fa1b3c3c0d`; reviewed PR #77 head `a3b6854900319807291a69d1b052546112848159` is the second parent. Production app-source tree remains the qualified beta tree.
- Exact-merge checks passed: Android CI `37766209677` attempt 2 (build `113278609154`, API-26 smoke `113278610203`, API-35 UI retry `113278607682`, 64/64); CodeQL `37766209564`; dependency submission `37766209509`; automatic dependency submission `37766209730`. Initial API-35 job failure was focus-only; one failed-job-only retry passed.
- Official main-only signing: run `37769090436`; immutable artifact `11546549425`, ZIP SHA-256 `2305f7fc18b3b9d391ac96f01b438e12ea4d025a690805443d3bab8f6ff364a7`. Source is the exact main above. Package/version/code: `com.weekssa.opraeqforuapp` / `0.8.0` / `11`; APK SHA-256 `2ea1d4b76a840e7448fddba556c3dc03aba19870eeb2499182af7d5759cce54b`; pinned signer SHA-256 `65c1c1256dae3c49e3548f334c91f0ba991969e9be9e0b223ba4e253d2114747`; R8 mapping SHA-256 `16fd8eec96d248592886db31445f2a9093f5928300deb568b1e3a7e52f3330d6`. Manifest, APK signature, alignment, checksum, and provenance independently passed.
- Independent final-artifact review: PASS. Promotion run `37770238047` passed all gates. v0.7.2 baseline: code 9, APK SHA-256 `efdd63ddb305d0624f805cc53e4ce27aae7d1ddeb169e8f965302d0f290ba64a`; beta baseline: code 10, APK SHA-256 `b352c4d10a91f9c378a1a18012054a803a23807d4c454fb9b2dde613b7729782`; stable code 11. The sequential v0.7.2 in-place upgrade, beta in-place upgrade, and stable clean-install/core smoke each logged `STABLE_RELEASE_SMOKE_PASSED`. Job `113287763621` diagnostics artifact `11548155809`, digest `sha256:319492ce8b417a2cd92b70495116c1829bb7bb494b7834459239156c171ef268`.
- Annotated tag `v0.8.0` peels to main SHA `54823e1a464f8b716a32c4f4808037fc0cdd99bd`; tag object `320cc55289d0f4ea4b956c89807f7068eae70160` binds signing run/artifact/digest. Immutable release `406758581` was published at `2026-10-08T11:34:37Z`, `draft=false`, `prerelease=false`; `/releases/latest` resolves to v0.8.0. Immutable beta release `406271597` remains `prerelease=true`.
- All six public assets were downloaded and matched GitHub's digest metadata. Public APK matches the signed candidate bytes and checksum sidecar. Local public assets and hash manifest are in `.unlazy/v080-stable-promotion/evidence/public-release-v0.8.0/`; the independent reviewer and public verification reports are in the same Unlazy work area.
- C05-C read-only qualification remains passed on exact beta candidate HEAD `3329590db531c31be84e7e28d89209028a688075`, tree `79808ef8052812427461f480e9abf19c2e930c22`, app source `f24b1582e22b19fc75757a61186361b71eab3974`, APK SHA-256 `f33818309d55571166d91e501065706ddbb8faf180833b03bb6eb948e6bffed1`, plan revision 2.48. The 2026-10-07 Pixel 9 / Black Pearl read-only session compared 31 displayed-value/session checks and found no unexpected change; no mutation was performed. Class B remains the honest classification. Raw ignored evidence remains at `/Users/stephenweeks/.codex/worktrees/59e5/OPRA-EQ-for-UAPP/.unlazy/v080-beta/evidence/c05c-20261007T212825832635Z/`; no hardware test was repeated for stable.
- Dependency caveat: 56 open repository transitive Maven alerts were identified in build/test tooling; the refreshed releaseRuntimeClasspath contained no alert-bearing coordinates, but this is not a clean scan and no DEX scan was done.
