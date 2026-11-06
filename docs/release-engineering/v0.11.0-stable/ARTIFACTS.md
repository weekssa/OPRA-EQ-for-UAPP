# v0.11.0 stable — artifact and provenance record

Published 2026-11-06T23:23:03Z. Immutable, non-draft, non-prerelease, `/releases/latest`.

## Identity

| Field | Value |
|---|---|
| Package | `com.weekssa.opraeqforuapp` |
| Version | `0.11.0` / versionCode `23` |
| Release source | `a3dbeac26f82dd844055a65ec8be97dd2729e890` (PR #144 merge) |
| Annotated tag | `v0.11.0`, object `f51b2baa0ec671bcff7f478f478093c82d91ab81`, peels to the release source |
| APK asset | `EQ-Library-v0.11.0.apk`, 2,937,616 bytes |
| APK SHA-256 | `b3de641b92e801dcee6dee44f8d1721ec08d55c63dd5de6f7fe1a9f64e74c307` |
| Signer SHA-256 | `65c1c1256dae3c49e3548f334c91f0ba991969e9be9e0b223ba4e253d2114747` (permanent release identity) |
| R8 mapping SHA-256 | recorded in the published `candidate-manifest.json` |

## Pipeline

1. Signed candidate: workflow `.github/workflows/github-release.yml`, run `42310173763`, artifact
   `13547539947`, built from the release source with `testDebugUnitTest`, `lintDebug`,
   `assembleRelease`, R8 mapping verification, `apksigner`/`zipalign` verification against
   `release-signing-cert.sha256`, and an API 35 install smoke.
2. Promotion: workflow `.github/workflows/promote-signed-release.yml`, run `42310935419`.
   It re-verified the artifact digest, manifest, APK signer, alignment and R8 mapping; ran the API 35
   clean install and the v0.10.0 in-place upgrade smoke (`tools/verify_stable_release_smoke.py`);
   created the annotated tag; uploaded the assets; and published the release without rebuilding or
   re-signing the APK.
3. Published assets: APK, checksum sidecar, `candidate-manifest.json`, `release-provenance.json`,
   `apksigner-verification.txt`, `zipalign-verification.txt`; release body is
   `docs/releases/v0.11.0.md` verbatim.

## Independent verification after publication

- Downloaded `EQ-Library-v0.11.0.apk` from the release and computed
  `b3de641b92e801dcee6dee44f8d1721ec08d55c63dd5de6f7fe1a9f64e74c307`, matching the published
  sidecar and the candidate manifest.
- `apksigner verify --print-certs` reports signer
  `65c1c1256dae3c49e3548f334c91f0ba991969e9be9e0b223ba4e253d2114747`, the pinned certificate.
- `aapt dump badging` reports `com.weekssa.opraeqforuapp` versionCode `23`, versionName `0.11.0`.
- `release-provenance.json` binds candidate run `42310173763`, artifact `13547539947` and promotion
  run `42310935419` to source `a3dbeac2`.

## Upgrade and install

- Same package and same signing identity as v0.10.0, so it installs as an ordinary update and keeps
  local app data, including My EQs, imports and settings.
- Clean install needs no account and no Google Play services.

## Limits carried with this release

- Native Low/High Shelf controls are not exposed: the protocol path is qualified, the acoustics are
  not measured.
- Headset-remote switching and UAC mode switching are chip-limited on this device.
- No listening test was run and no acoustic claim is made.

