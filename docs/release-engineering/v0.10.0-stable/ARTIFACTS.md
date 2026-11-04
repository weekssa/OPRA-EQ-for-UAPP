# EQ Library v0.10.0 stable release artifacts

Exact provenance for the public stable release. Recorded 2026-11-04.

## Release

| Field | Value |
| --- | --- |
| Release | [EQ Library v0.10.0](https://github.com/weekssa/OPRA-EQ-for-UAPP/releases/tag/v0.10.0) |
| Release ID | `434267766` |
| Published | `2026-11-04T18:04:05Z` |
| State | immutable, non-draft, non-prerelease, `/releases/latest` |
| Tag | annotated `v0.10.0`, object `a9d37fcaa4d7d458b0602ead8376a8735f40c3a4` |
| Source | `9cbdfdb0df9be146338881aae617caa03818205c` (PR #140 merge) |

## Application

| Field | Value |
| --- | --- |
| Package | `com.weekssa.opraeqforuapp` |
| versionName | `0.10.0` |
| versionCode | `22` |
| prev stable | v0.9.0 / code `20` |
| prev prerelease | v0.9.0-beta.4 / code `21` |

## Signed candidate

| Field | Value |
| --- | --- |
| Run | `41880767569` ([workflow](https://github.com/weekssa/OPRA-EQ-for-UAPP/actions/runs/41880767569)) |
| Artifact ID | `13348838458` |
| Artifact name | `EQ-Library-v0.10.0-signed-9cbdfdb0df9be146338881aae617caa03818205c` |
| Archive SHA-256 | `1eda1551c57b6ba26300c855c4705bedcaa12b806dfeff8a0fbf2a680714d06a` |
| Release gate | passed on the exact source |

## Promotion

| Field | Value |
| --- | --- |
| Run | `41882199504` ([workflow](https://github.com/weekssa/OPRA-EQ-for-UAPP/actions/runs/41882199504)) |
| Workflow | `.github/workflows/promote-signed-release.yml` |
| Result | exact-candidate verification and the published-v0.9.0 upgrade lane passed; tag created; release published |

## Public assets

| Asset | Size (bytes) | SHA-256 |
| --- | --- | --- |
| `EQ-Library-v0.10.0.apk` | 2921232 | `755fe80c0f8fb1a3276814a5c372d669d35f9e87d4f328e683d5b90bd8f9d7ae` |
| `EQ-Library-v0.10.0.apk.sha256` | 89 | `6e3291509703f204bc53f2bbc678d468ef38045717f85e8a6c07a78cee4ac72d` |
| `candidate-manifest.json` | 609 | `f51bc71c409a452a471f59aa7e5d888337accccab3c6a2277ca47e27dbfe7547` |
| `release-provenance.json` | 689 | `ee7c83bad9f38fc8a3ff3410099f963131ed35af3f835d7653b60ae031574704` |
| `apksigner-verification.txt` | 951 | `ad34f86ffe5fb59711128eb260f44bcce0fad0813d13a73a2a13fd9a711ccccb` |
| `zipalign-verification.txt` | 9983 | `eeaa4d518522506d64b4fc8ab438c8b3c4816655b3a0fee963c191bcdffd4d6c` |

## Verification performed

- The public APK was downloaded from the release and hashed locally; the bytes match `755fe80c0f8fb1a3276814a5c372d669d35f9e87d4f328e683d5b90bd8f9d7ae` and the published sidecar.
- `apksigner verify --print-certs` on the public APK reports signer SHA-256 `65c1c1256dae3c49e3548f334c91f0ba991969e9be9e0b223ba4e253d2114747`, the project's permanent release identity pinned in `release-signing-cert.sha256`.
- The candidate manifest reports the same APK digest, signer, `versionName 0.10.0`, `versionCode 22` and `sourceSha 9cbdfdb0df9be146338881aae617caa03818205c`.
- `/releases/latest` returns `v0.10.0`; the v0.9.0 release is unchanged and remains available.

## Boundaries

- Physical evidence remains one Pixel 9 and one JA11 at firmware 2.20; the audible tuning result is an owner report, not an acoustic measurement.
- The published v0.10.0 APK has not been re-read from the owner's phone, so the installed-bytes claim in `docs/PROJECT_STATE.md` still describes the earlier v0.9.0 install.
- Digital reconstruction filters are not delivered; the research handoff is preserved.
