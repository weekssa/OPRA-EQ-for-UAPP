# OPRA EQ for UAPP — Architecture

This document supplements `docs/CHATGPT_PROJECT_RUNBOOK.md`. The runbook remains authoritative for product and UX decisions.

For implementation-time product decisions approved after Phase 0, also read:

- `docs/PHASE1_DECISIONS.md`

## Android baseline

The application is a single native Android module using Kotlin and Jetpack Compose.

- application ID / namespace: `com.weekssa.opraeqforuapp`
- minSdk: 26
- compileSdk / targetSdk: 36 (Android 16 stable baseline)
- Java target: 17
- Android Gradle Plugin: 9.2.0
- Kotlin / Compose compiler plugin: 2.3.21
- Compose BOM: 2026.06.00
- Room: durable app-owned managed-headphone/export state
- Preferences DataStore: local appearance, compatibility visibility, export-tree, and update presentation preferences
- WorkManager: approximately daily catalog reconciliation
- Storage Access Framework / DocumentFile: explicit user-folder export and app-owned file cleanup

The app deliberately targets the stable API 36 platform rather than a preview SDK. Dependency and toolchain changes must be checked against current official compatibility guidance and validated in CI.

## Package and responsibility boundaries

`com.weekssa.opraeqforuapp.ui`

- Compose screens, app shell, navigation, dialogs, accessibility semantics, appearance, update banners, and user interaction.
- UI must not parse OPRA JSON or implement ToneBoosters normalization.

`com.weekssa.opraeqforuapp.domain`

- platform-independent product rules: compatibility, selection semantics, deterministic naming, conversion, export planning, SemVer/update comparison, and managed-state decisions.
- Critical rules are unit-testable without Android framework dependencies.

`com.weekssa.opraeqforuapp.data`

- runtime catalog acquisition and last-known-good cache;
- Room entities/DAOs/repositories;
- managed-profile snapshots and fingerprints;
- catalog reconciliation;
- WorkManager scheduling/worker coordination;
- SAF export ownership and cleanup;
- GitHub public-release metadata client;
- Preferences DataStore repository.

Platform integrations stay behind narrow repositories/coordinators rather than leaking into conversion/domain logic.

## Runtime OPRA catalog

Normal runtime source is exactly:

`https://opra.roonlabs.net/database_v1.jsonl`

The app ships with zero headphone/EQ records and does not scrape GitHub during normal catalog operation.

`HttpOpraCatalogSource` downloads a bounded JSONL candidate to app-private storage. `OpraCatalogParser` parses vendor/product/EQ entries and validates IDs plus vendor/product/EQ relationships. A candidate must parse and validate completely before it can replace the current cache. Failed, partial, malformed, or invalid candidates are discarded without replacing known-good data.

A fresh saved catalog loads immediately and supports Browse/Search offline. Manual Refresh and approximately daily WorkManager checks share the same safe acquisition/reconciliation behavior. Network failures leave cached/local state usable.

Foreground and WorkManager refreshes are serialized across repository instances so they cannot race over the shared candidate cache file. The first periodic worker run is delayed by roughly 24 hours so fresh-install foreground catalog acquisition owns initial synchronization.

## Browse and local search

Browse uses the current in-memory model parsed from the last-known-good catalog:

- Manufacturer → Model hierarchy from OPRA source names only;
- no invented variants/path meaning;
- manufacturer/model-only local search;
- case-insensitive and ordinary spacing/punctuation-tolerant matching;
- no network-per-keystroke or remote fallback;
- managed selected counts shown where useful;
- official OPRA attribution at the catalog-browser root.

## Compatibility and conversion

Catalog validity and UAPP/ToneBoosters compatibility are independent.

Current proven conversion supports:

- `peak_dip`;
- `low_shelf`;
- `high_shelf`.

Established safe ranges remain:

- frequency: 16 Hz–20 kHz;
- gain/preamp: -20 dB–+20 dB;
- Q: 0.1–10.

Unsupported filter mappings, missing required acoustic data, and out-of-range acoustic values classify as **Not compatible**. Those profiles remain discoverable but are never selectable/exportable. No acoustic value is guessed, clamped, or silently dropped.

Profiles above 10 bands classify as **Compatible with limitation** and preserve OPRA priority/order by converting only the first 10 bands with an explicit warning.

The Kotlin converter ports the established Python reference behavior:

- linear gain/preamp normalization;
- cube-root frequency and Q normalization;
- proven ToneBoosters filter-type constants;
- disabled placeholder filters to fill the 10-band structure;
- deterministic 66-value ToneBoosters XML;
- deterministic headphone-first names;
- ISO-8859-1-safe exported XML/name while full Unicode source metadata stays local.

Golden fixtures and parity-oriented unit tests protect this behavior.

Missing creator/author data does not change acoustic compatibility. A safely convertible profile remains selectable/exportable and uses the literal label **Creator information missing** in the creator slot while the stored OPRA author remains null/missing.

## Managed state and selection

Room stores managed headphone identity and per-profile state including:

- selected state;
- explicit exclusion state;
- auto-include-new-profiles mode;
- full last-known OPRA profile snapshot with Unicode metadata;
- semantic fingerprint;
- first/last-seen timestamps;
- unreviewed New/Updated flags;
- No-longer-available state;
- generated preset name/XML/source fingerprint/time;
- external app-owned export records.

A never-managed headphone starts with auto-inclusion ON and all currently selectable profiles checked. Not-compatible profiles are excluded from this default and from Select all/automatic inclusion. These first-use defaults are staged UI state rather than an already-persisted baseline: if at least one selectable profile exists, the screen must immediately allow **Add to My Headphones** without forcing an artificial checkbox change.

With auto-inclusion ON, unchecking a selectable profile becomes an explicit exclusion; future unrelated compatible profiles are included. With auto-inclusion OFF, the saved selection is fixed exact state.

A headphone is keyed by OPRA product identity in Room. Explicit Add/Save and per-headphone XML export both create or update that same record rather than creating parallel/duplicate library state.

## Catalog reconciliation and review

Manual and background refresh share deterministic managed-state reconciliation.

- New profiles are stored and marked New; compatible ones auto-select only when the headphone's auto-inclusion rule allows it.
- Changed selected compatible profiles regenerate local deterministic XML and become Updated.
- Removed profiles retain last-known OPRA metadata and generated XML and become **No longer available in OPRA**; they are never silently deleted.
- A selected profile that changes and becomes Not compatible is unselected/disabled while the last successfully generated preset state is retained for explicit review/cleanup.
- Source-link-only provenance changes do not count as acoustic/profile semantic changes.
- Re-refreshing does not clear unreviewed New/Updated state.
- Opening the managed-headphone review clears persisted transient New/Updated markers while the opened review surface preserves enough local presentation context to inspect the changes; No-longer-available state persists until explicit removal/upstream return.

## Export architecture

Export is explicit and user-driven through Android's Storage Access Framework.

- Bulk **Export presets** from My Headphones exports the selected presets across the managed library.
- Per-headphone **Export XMLs** is available from the headphone profile editor. If that headphone is new or has staged changes, the staged selection/future-profile setting is persisted first; export then reloads the durable Room record by OPRA product ID and writes only that headphone's selected generated presets. This guarantees that any headphone explicitly exported from Browse is also present/updated in My Headphones.
- First Export opens the system directory picker; the UI suggests `Documents/OPRA EQ for UAPP/Presets` but the user chooses.
- Supported tree access is persisted in DataStore and via Android persistable URI permission.
- No broad storage permission or writes into another app's private storage.
- Folder hierarchy begins Manufacturer/Model; deeper folders are only allowed for verified OPRA distinctions.
- Selected generated profiles become deterministic filename candidates.
- `ExportOwnershipEntity` records documents created by this app.
- Existing unknown same-name files are conflicts: never overwrite and never rename to `(2)`.
- App-owned files can be updated on later explicit Export.
- Content hashes distinguish current/modified copies.
- Per-file failures do not roll back independent successful files.
- Optional removal cleanup deletes only ownership-tracked app-created files; local state removal succeeds even if external cleanup fails.

Room schema version 2 adds generated-preset state and export ownership with an explicit v1→v2 migration.

Cleanup uses the retained SAF tree permission and attempts external owned-file deletion before local managed-state removal. Files orphaned by an uninstall are deliberately treated as untracked and must not be deleted merely because their names match.

## App updates

The app checks the repository's latest GitHub Release metadata without user credentials at a modest cadence. SemVer comparison uses the normal release channel only; prereleases are not normal update candidates.

A newer version can surface a nonblocking banner and Settings → About & updates actions. What’s new displays release notes; Get update opens the release page in the browser. There is no silent APK download, self-install, install-unknown-apps permission, or forced update.

Public unauthenticated release checking becomes operational once the repository is public and at least one GitHub Release exists.

## Attribution, privacy, and licenses

The app bundles the official OPRA logo solely for source attribution and does not download OPRA headphone artwork at runtime. Browse and Settings credit OPRA, link to the OPRA project, preserve individual profile creator/source metadata where available, and disclose CC BY-SA 4.0 data licensing.

Root `NOTICE` documents converter/software provenance and `DATA_LICENSE.md` documents OPRA-derived data licensing. `PRIVACY.md` is the public privacy statement and matches the app's in-product disclosure. The UI states that the app is not affiliated with or endorsed by OPRA, Roon Labs, USB Audio Player PRO/UAPP, ToneBoosters, or headphone manufacturers.

Selections, settings, generated preset state, and conversion remain local; there is no account, analytics, or telemetry.

## Launcher icon

The approved **Equalizer Headphones** direction is implemented as original Android adaptive launcher assets: headphone/earcup geometry around three EQ slider controls, plus round and monochrome/themed-icon treatment. No OPRA/UAPP/ToneBoosters brand mark is incorporated.

## Validation and CI

Android CI is the automated gate and runs:

- `:app:testDebugUnitTest`
- `:app:lintDebug`
- `:app:assembleDebug`

Same-branch obsolete runs are cancelled so validation tracks the newest `main` state. Tests cover catalog parsing/cache safety, startup retry/concurrent refresh safety, search, compatibility, selection defaults/exclusions, first-add action eligibility, snapshot/fingerprint semantics, native conversion/golden XML, reconciliation, export planning/conflicts, SemVer, and other deterministic domain rules.

The primary Pixel 9 hands-on validation gate **passed on 2026-08-15**. It covered first launch/fresh catalog acquisition, offline reuse, Browse/Search, My Headphones management, selection persistence, SAF export and owned-file cleanup, successful UAPP/ToneBoosters XML import, appearance, large text, TalkBack, themed icon presentation, privacy, and attribution. See `docs/DEVICE_TEST_PLAN.md`.

A future signed release build still requires a short release-build smoke test because signing/build-type differences are not covered by the completed debug-device pass.

## Phase 1 / public release status

Implemented and validated product slices:

1. Foundation Android/Compose app and local settings.
2. Runtime OPRA client, validation, last-known-good cache, Browse/Search.
3. Room managed state and selection semantics.
4. Native Kotlin ToneBoosters conversion with golden parity fixtures.
5. Managed change reconciliation plus approximately daily WorkManager checks.
6. SAF export, ownership/conflict tracking, persisted folder access, and optional app-created-file cleanup.
7. Public GitHub Release metadata/update UX.
8. OPRA attribution/privacy/license surfaces and production adaptive icon assets.
9. Accessibility/release hardening and Pixel 9/UAPP hands-on validation.
10. Public-repository documentation, privacy, contribution/security guidance, and release checklist.

The app implementation and primary device-validation gate are complete for the current `0.1.0` development line. The remaining pre-binary-release work is distribution infrastructure: change repository visibility to public, establish one stable Android release-signing identity, build/smoke-test the signed `0.1.0` APK, and publish the `v0.1.0` GitHub Release. Google Play work is intentionally deferred.
