# v0.7.2 governance remediation final report

STATUS: COMPLETE

Application behavior changed: NO

v0.7.2 tag target: `b8e90b9b53fc63ea00fefa499d7d4bd6ce4d55ea` (unchanged)

Public APK SHA-256: `efdd63ddb305d0624f805cc53e4ce27aae7d1ddeb169e8f965302d0f290ba64a` (unchanged)

Findings F1-F4 are resolved. F5/F6 are not retroactively recoverable; future capture is documented. See `FINDINGS.md`.

PR: [#68](https://github.com/weekssa/OPRA-EQ-for-UAPP/pull/68), merged at `468436d8236248bf1f017b1a503113cd41f81d73`.

CI: PASS, 7/7 exact-head checks on `85daba251b85995a77e1675174f4ef6946d427fd` (build, validate, Analyze Kotlin, submit-gradle, emulator UI, minimum API smoke, CodeQL).

GitHub stable-tag protection: Active ruleset ID `24444174`; matching version tags cannot be updated or deleted, there are no bypass actors, and creation remains unrestricted.

GitHub Release immutability: Repository setting reads `enabled=true`; the public v0.7.2 Release API and page read `immutable=true`. This live result differs from GitHub's setup guide, which describes the toggle as future-release-only; the exact observed response is recorded in `05_TAG_RELEASE_PROTECTION.md`.

Public release erratum: Verified in public Release ID `402346895`; title unchanged, and the body exactly matches `docs/releases/v0.7.2.md` (3,709 UTF-8 characters; SHA-256 `6943b87df850485fa037df0822fbb013a35acf81fb1943d4553157a5abc707aa`). No release assets were replaced.

Public binary verification: Freshly downloaded APK is 2,835,216 bytes with SHA-256 `efdd63ddb305d0624f805cc53e4ce27aae7d1ddeb169e8f965302d0f290ba64a`; `apksigner` verified signer SHA-256 `65c1c1256dae3c49e3548f334c91f0ba991969e9be9e0b223ba4e253d2114747`; `aapt` verified package `com.weekssa.opraeqforuapp`, versionName `0.7.2`, versionCode `9`.

Evidence bundle: `postrelease_remediation_audit_v0.7.2/` plus `postrelease_remediation_audit_v0.7.2.zip`; 12 files including the SHA-256 manifest, generated locally and kept untracked.

Human action required: NONE.
