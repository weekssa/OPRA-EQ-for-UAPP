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
