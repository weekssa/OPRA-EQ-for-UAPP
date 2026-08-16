# Changelog

All notable changes to **OPRA EQ for UAPP** will be documented in this file.

The project uses Semantic Versioning. Development releases remain in the `0.x` series until the first stable `v1.0.0` release.

## [0.1.0] - Unreleased

### Added

- Native Android project foundation for application ID `com.weekssa.opraeqforuapp`, minSdk 26, stable Android 16 / API 36 compile and target baseline.
- Kotlin + Jetpack Compose application with the approved **My Headphones** and **Browse OPRA** peer destinations, secondary Settings and managed-headphone detail screens, accessible Refresh actions, and Android Back behavior.
- Persistent local **System default / Light / Dark** appearance preference.
- Persistent local visibility preferences for **Fully compatible**, **Compatible with limitation**, and **Not compatible**, all enabled by default and presentation-only.
- Runtime OPRA `database_v1.jsonl` download from the Roon Labs mirror with full-candidate validation before cache promotion.
- App-private last-known-good OPRA catalog cache with offline reuse, startup freshness checks, and manual Refresh.
- Local Manufacturer → Model browsing and manufacturer/model search over the cached OPRA catalog.
- Compatibility classification separated from catalog validity; unsupported/unsafe profiles remain discoverable but disabled and unexportable.
- Room persistence for managed headphones, exact profile selections, explicit exclusions, auto-inclusion mode, review state, retained removed profiles, OPRA snapshots, semantic fingerprints, generated XML, and app-owned export records.
- First-time management defaults to all currently selectable profiles checked with automatic future-profile inclusion ON.
- Native Kotlin OPRA → UAPP/ToneBoosters conversion port covering proven normalization, preamp, supported filters, deterministic 10-band XML, first-10 priority truncation, naming, and ISO-8859-1-safe output.
- Golden/reference conversion tests based on the read-only Python converter behavior.
- `Creator information missing` handling that preserves acoustic compatibility/exportability without inventing an OPRA creator identity.
- Deterministic catalog reconciliation for new, changed, removed, and newly incompatible managed profiles; selected profiles are regenerated locally when safely convertible, removed profiles retain last-good XML, and selected profiles that become Not compatible are unselected while their last-good generated state is preserved.
- Approximately daily WorkManager catalog checks with connected-network constraint and quiet failure behavior; user-relevant state is surfaced in-app rather than through notification permission.
- My Headphones library grouped by manufacturer with selected counts, New/Updated/No-longer-available attention summaries, managed-headphone detail/review, profile management, and explicit removal flows.
- Staged profile editing with **Select all**, **Select none**, auto-include-new-profiles setting, unsaved-change protection, explicit exclusions, disabled Not-compatible controls, and optional app-created saved-file cleanup on removal.
- Android Storage Access Framework export with persisted tree access, suggested Documents/OPRA EQ for UAPP/Presets location, Manufacturer/Model folder layout, deterministic filenames, incremental create/update/current handling, exact unmanaged-file conflicts, per-file failure isolation, and app-file ownership tracking.
- Per-headphone **Export XMLs** from the Browse/managed profile editor; exporting a new or changed headphone persists its staged selection first so My Headphones is created or updated before files are written.
- GitHub Release update checks at a modest cadence, SemVer comparison, non-blocking update banner, About & updates status/actions, What’s new dialog, manual browser handoff, and one-time post-update card state; no self-install or APK-install permission.
- OPRA attribution in Browse and Settings using the official OPRA logo, project description/link, individual creator identity from profile metadata, CC BY-SA 4.0 data attribution, software provenance notice, privacy disclosure, and non-endorsement language.
- Original production **Equalizer Headphones** adaptive launcher icon direction with round and monochrome/themed-icon treatment.
- Android CI with unit tests, Android lint, and debug APK assembly; obsolete same-branch CI runs are cancelled automatically.

### Fixed

- A never-managed headphone with the approved default profiles already selected now offers an enabled **Add to My Headphones** action immediately instead of requiring an artificial checkbox change before it can be saved.
- Optional saved-preset cleanup now uses the retained Storage Access Framework tree grant and attempts deletion before local managed-state removal; only ownership-tracked app-created files are deleted.
- First-launch catalog initialization now retries one transient network failure before showing an unavailable state.
- Prevented the foreground first catalog download and WorkManager background sync from racing over the same candidate cache file: periodic background sync now waits roughly 24 hours before its first run, and catalog refresh/promotion is serialized across repository instances.

### Documentation

- Added and maintained `docs/CHATGPT_PROJECT_RUNBOOK.md` as the product/UX source of truth.
- Added `docs/ARCHITECTURE.md` for implementation boundaries and validated architecture.
- Added `docs/PHASE1_DECISIONS.md` for post-Phase-0 implementation decisions.
- Added `NOTICE` for converter/software provenance and `DATA_LICENSE.md` for OPRA-derived data licensing/attribution.

### Validation still required before a public release

- Hands-on Pixel 9 validation of first launch, offline reuse, Browse/Search, managed selections, compatibility states, refresh/change review, SAF export/cleanup, themes, large text, TalkBack, and launcher/themed icon presentation.
- Import representative generated XML into USB Audio Player PRO/ToneBoosters on the target device and verify practical preset loading/behavior.
- Resolve public GitHub Release accessibility while the development repository remains private.
- Introduce and securely retain one stable release-signing identity; no signing key or credential may be committed.
- Complete release-version/release-notes checks and a final signed release build before `v1.0.0`.
