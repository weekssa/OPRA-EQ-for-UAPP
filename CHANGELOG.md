# Changelog

All notable changes to **OPRA EQ for UAPP** will be documented in this file.

The project uses Semantic Versioning. Development releases remain in the `0.x` series until the first stable `v1.0.0` release.

## [0.1.0] - 2026-08-16

### Added

- Native Android app for `com.weekssa.opraeqforuapp`, minSdk 26, targeting Android 16 / API 36 with Kotlin and Jetpack Compose.
- Approved **My Headphones** and **Browse OPRA** navigation, Settings, appearance preferences, profile-visibility preferences, accessibility semantics, and Android Back behavior.
- Runtime OPRA `database_v1.jsonl` download with full-candidate validation, last-known-good local cache, offline Browse/Search after first sync, manual Refresh, and approximately daily WorkManager checks.
- Manufacturer → Model browsing and local manufacturer/model search without runtime GitHub scraping or bundled headphone data.
- Room-backed managed-headphone state with exact selections, explicit exclusions, automatic future-profile inclusion, review state, retained removed profiles, generated XML, and app-owned export records.
- Native Kotlin OPRA → UAPP/ToneBoosters conversion with golden/reference parity tests, deterministic XML, OPRA preamp/frequency/gain/Q preservation, supported `peak_dip` / `low_shelf` / `high_shelf` mappings, first-10 priority handling for ToneBoosters' 10-band limit, deterministic naming, and ISO-8859-1-safe exported XML/name handling while full Unicode metadata remains local.
- Explicit **Not compatible** handling for unsupported or unsafe OPRA profiles; no silent approximation, clamping, dropping, or invented creator metadata.
- `Creator information missing` handling for otherwise safely convertible profiles with missing OPRA creator data.
- Deterministic reconciliation for new, changed, removed, and newly incompatible managed profiles, including retention of last-good generated state where required.
- Android Storage Access Framework export with persisted tree access, Manufacturer/Model folder layout, deterministic filenames, app-owned file tracking, safe same-name conflict handling, and optional cleanup of files created by this app.
- Per-headphone **Export XMLs** plus bulk **Export presets** from My Headphones.
- Public GitHub Release update checks, SemVer comparison, non-blocking update UX, What’s new, and browser handoff without silent download/install or APK-install permission.
- OPRA attribution, CC BY-SA 4.0 data attribution, privacy disclosure, non-endorsement language, software provenance, and individual creator/source preservation where OPRA provides it.
- Original **Equalizer Headphones** adaptive launcher icon with round and monochrome/themed-icon treatment.
- Android CI covering unit tests, lint, debug assembly, and unsigned release assembly; advanced Kotlin CodeQL; Dependabot/security hardening; and a protected `main` branch ruleset.
- Permanent GitHub-distribution release-signing identity with a repository-pinned public certificate fingerprint and a controlled candidate/publish GitHub Actions workflow.

### Fixed

- A never-managed headphone with default compatible profiles already selected can be added immediately without forcing an artificial checkbox change.
- Optional saved-preset cleanup uses retained Storage Access Framework access and deletes only ownership-tracked app-created files.
- First-launch catalog initialization retries one transient network failure before showing an unavailable state.
- Foreground first-sync and WorkManager background sync are serialized so they cannot race over the candidate catalog cache.

### Documentation and validation

- Added and maintained `docs/CHATGPT_PROJECT_RUNBOOK.md`, `docs/ARCHITECTURE.md`, and `docs/PHASE1_DECISIONS.md` as the project source of truth and implementation decision record.
- Added `NOTICE`, `DATA_LICENSE.md`, `PRIVACY.md`, `CONTRIBUTING.md`, `SECURITY.md`, public issue templates, `docs/PUBLIC_RELEASE_CHECKLIST.md`, and `docs/RELEASE_SIGNING.md`.
- Completed Pixel 9 hands-on validation covering first launch, offline reuse, Browse/Search, My Headphones, selection persistence, SAF export/cleanup, UAPP/ToneBoosters preset import, appearance, large text, TalkBack, privacy, attribution, and launcher presentation.
- Generated and backed up the permanent Android signing identity; stored release signing material only in approved secure stores/GitHub Actions secrets.
- Built a signed `v0.1.0` candidate from commit `7bc0f687aece6f58f3431a71b5bb32794c0b7ffa`, verified its pinned signing certificate, installed it on the Pixel 9, and passed the release-build smoke test including UAPP/ToneBoosters import.
- Published GitHub Release `v0.1.0` from that exact tested commit with the signed APK, APK SHA-256 checksum, and `apksigner` verification output.
- Verified the public `releases/latest` metadata endpoint and confirmed the installed `v0.1.0` app reports **You're up to date** against the live public release metadata.
