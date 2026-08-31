# Changelog

All notable changes to **OPRA EQ for UAPP / EQ Library** will be documented in this file.

The project uses Semantic Versioning. Development releases remain in the `0.x` series until the first stable `v1.0.0` release.

## [0.3.0] - Unreleased

### Added

- Global active-output context across **My EQs** and **EQ Library**, with locally enabled outputs in Settings. Initial selectable outputs are UAPP/ToneBoosters, TRN Black Pearl, Universal Parametric EQ, Poweramp/Poweramp Equalizer, and Wavelet.
- Output-specific **My EQs** collections for managed headphone selections, General EQs, favorites/saved snapshots, and personal imports. Existing pre-output-context saved state migrates to UAPP/ToneBoosters.
- **Headphones** and **General EQs** library sections, with General EQ filters for **All**, **Sound**, **Genre**, and **Utility**.
- Device-independent canonical source usability plus active-output **Exact**, **Optimized**, and **Not exportable** status. A valid canonical curve remains visible/selectable even when the active output cannot represent it.
- Multi-source canonical catalog foundation for OPRA, AutoEq, qualified creator/repository data, public community EQ submissions, immutable acoustic revisions, provenance, verification state, and general presets.
- **Unverified** community EQ presentation with original source links and manual selection while excluding Unverified profiles from silent automatic future-profile inclusion.
- Conditional **Export all** and per-item **Export** recovery actions based on the current active-output candidate, app-owned SAF ownership metadata, generated fingerprint/content hash, and the actual exported document when available.
- Optional TRN Black Pearl **Direct Flash** from My EQs, including DAC connection state, current-slot discovery, confirmation, source-preamp/headroom playback-gain adjustment through the observed global-gain command, PEQ transfer, and success/error reporting.
- Independent Black Pearl EQ protocol implementation for native Peak, Low Shelf, and High Shelf filters with a 10-band hardware limit and explicit unused-band flattening.
- Output-scoped Room association tables and non-destructive migrations for managed headphone selections, General EQs, and favorites/personal imports.

### Changed

- Output selection is now an operating context rather than a catalog filter. Choosing a device/app changes conversion, export, Flash availability, capability status, and the My EQs collection, but never hides valid library curves.
- The obsolete prototype setting **Show presets that none of my devices can export** is ignored/removed from user-facing behavior.
- New headphones start with no EQ profiles selected and automatic future-profile inclusion off; users explicitly choose the presets they want before Add.
- Add/Save persists the selected EQs and initiates their export for the active output. Normal Export/Export all controls stay hidden while the expected app-managed files are present and current, and reappear only for recovery when files are missing or stale.
- Selection, Select all/none, and automatic future-profile behavior are based on canonical source usability and trust/history state, not UAPP compatibility.
- UAPP/ToneBoosters compatibility is enforced only at the UAPP conversion/export boundary. UAPP XML is optional generated state rather than a prerequisite for saving a canonical EQ.
- Personal PEQ imports preserve a missing source preamp as null rather than silently inventing `0 dB`.
- TRN Black Pearl conversion/Flash preserves corroborated native shelf/peak filter types instead of approximating shelves with synthetic peaking filters.
- Black Pearl direct Flash applies the selected profile's required preamp/headroom through the observed `0x03` global playback-gain command in 1/256 dB units. It reads the current gain, replaces the previous EQ Library-applied adjustment instead of stacking reductions, allows a later 0 dB preset to restore that prior adjustment, and fails rather than clamping if the requested absolute gain is outside the validated device range.
- Black Pearl profiles over 10 bands use the first 10 source-priority bands only with an explicit Optimized warning; canonical source data remains complete and unchanged.
- Navigation and terminology now use **My EQs**, **EQ Library**, and **Settings** instead of the earlier My Headphones/Browse OPRA framing.

### Fixed

- Export status no longer stays permanently available after an output file is current; it is recalculated after export and when the active output, folder, or saved collection changes.
- Removing a favorite/personal EQ from one output no longer implicitly removes it from another output where it is still selected.
- A valid canonical profile that is unsupported by UAPP no longer becomes unselectable or silently excluded from automatic selection solely because of UAPP limits.
- ToneBoosters conversion now explicitly rechecks UAPP-specific compatibility so device-independent selection cannot bypass the established UAPP filter/range/preamp safety gate.
- Removed the superseded duplicate app shell that caused stale Black Pearl call signatures to break Android CI.

### Validation

- Final exact-head Android CI, CodeQL, APK artifact, and Pixel 9 / Black Pearl hands-on validation are required before this version may be promoted or PR #3 may be merged.

## [0.2.0] - 2026-08-28

### Added

- Visible product rebrand to **EQ Library** while preserving application ID `com.weekssa.opraeqforuapp` and the permanent Android release-signing identity for in-place upgrades.
- Explicit one-device-at-a-time export chooser for UAPP / ToneBoosters, TRN Black Pearl, Topping DX5 II, and Topping DX1 II.
- Device-first root-folder layout: device → manufacturer → headphone → exported preset.
- TRN Black Pearl text conversion constrained to a maximum of 10 PK filters to avoid passing unsupported/broken shelf filters through directly.
- Topping Tune text output for DX5 II and DX1 II, marked hardware-validation pending until physical devices are available.
- Full saved-library cleanup in addition to existing single-preset and single-headphone cleanup.

### Changed

- Selecting or deselecting EQ profiles is now a library-management action only; it does not automatically export or delete files.
- New-headphone preset selection is non-destructive and no longer warns that unselected default profiles will be removed.
- Export now requires an explicit target-device choice and writes only that target format.
- Cleanup actions are separated from ordinary selection and can optionally remove only ownership-tracked files created by EQ Library.

### Validation

- Android CI and CodeQL passed on the exact beta commit promoted to release.
- The permanently signed beta candidate passed test/lint/release assembly, APK alignment, pinned signing-certificate fingerprint verification, and SHA-256 generation.
- Hands-on testing passed for in-place upgrade, OPRA browsing/selection, revised selection behavior, device-targeted export, app-owned file cleanup, UAPP import, and TRN Black Pearl import.
- DX5 II and DX1 II export formats remain implemented but hardware-untested.

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
