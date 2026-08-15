# OPRA EQ for UAPP — Architecture

This document supplements `docs/CHATGPT_PROJECT_RUNBOOK.md`. The runbook remains authoritative for product and UX decisions.

For implementation-time product decisions approved after Phase 0, also read:

- `docs/PHASE1_DECISIONS.md`

## Phase 1 foundation

Phase 1 uses a single native Android application module with Kotlin and Jetpack Compose.

Baseline:

- application ID / namespace: `com.weekssa.opraeqforuapp`
- minSdk: 26
- compileSdk / targetSdk: 36 (Android 16 stable baseline)
- Java target: 17
- Android Gradle Plugin: 9.2.0
- Kotlin / Compose compiler plugin: 2.3.21
- Compose BOM: 2026.06.00

The project uses AGP 9 built-in Kotlin support rather than applying the legacy `org.jetbrains.kotlin.android` plugin. API 37 is supported by AGP 9.2 but remains an Android 17 preview SDK at this stage, so the app deliberately targets stable API 36 until a later validated baseline update is justified.

## Package boundaries

`com.weekssa.opraeqforuapp.ui`

- Compose screens, app shell, navigation state, accessibility semantics, and theme presentation.
- UI may call domain-facing repositories/actions but must not parse OPRA JSON or generate ToneBoosters XML directly.

`com.weekssa.opraeqforuapp.domain`

- Product rules independent of Android presentation: profile compatibility, selection semantics, conversion models, catalog change semantics, deterministic naming, and export decisions.
- Compatibility and conversion rules should be unit-testable without Android framework dependencies.

`com.weekssa.opraeqforuapp.data`

- Local persistence, runtime catalog acquisition/cache, repository implementations, and mapping between stored/network representations and domain models.
- Room owns managed-headphone/profile state, explicit exclusions, review flags, and last-known OPRA snapshots.

Platform-specific integrations such as Storage Access Framework export and WorkManager scheduling should be isolated behind narrow interfaces rather than leaking into conversion/domain rules.

## Navigation foundation

The two approved top-level destinations are modeled as peer state rather than a history stack. Switching between **My Headphones** and **Browse OPRA** therefore does not make system Back switch tabs.

Settings is modeled as a secondary screen that returns to the top-level destination that opened it. Deeper Browse navigation now unwinds Product → Manufacturer → Browse root; managed-headphone detail navigation will preserve the approved per-area Back behavior when Room-managed state is wired into Compose.

## Local settings

Preferences DataStore currently stores:

- appearance: System default / Light / Dark;
- visibility of Fully compatible profiles;
- visibility of Compatible-with-limitation profiles;
- visibility of Not-compatible profiles.

These are presentation preferences only. They must never mutate profile compatibility, selection, conversion, or export eligibility.

## Runtime OPRA catalog

The runtime catalog source is exactly:

`https://opra.roonlabs.net/database_v1.jsonl`

The app does not bundle a fallback headphone database and does not scrape GitHub during normal runtime.

Catalog acquisition is intentionally separate from managed-headphone persistence:

- `HttpOpraCatalogSource` downloads the JSONL candidate into app-private storage with timeouts and a bounded maximum size.
- `OpraCatalogParser` parses vendor, product, and EQ entries and validates IDs plus vendor/product/EQ relationships.
- A candidate must parse and validate completely before it may replace the current cache.
- The last-known-good raw JSONL file is stored under app-private files and promoted with an atomic filesystem move when supported, with a safe replace fallback on filesystems that do not support atomic move.
- A failed, malformed, partial, or otherwise invalid candidate is discarded without replacing a valid current cache.
- A fresh cached catalog is loaded immediately on startup and reused offline. Startup performs a network freshness check only after roughly 24 hours; WorkManager-based background checks remain a later slice.
- Manual Refresh uses the same validation/promotion path and leaves the current cached catalog usable while a refresh is running.

The raw file cache is not Room data. Room is reserved for durable app-owned state such as managed headphones, exact selections/exclusions, review state, and catalog snapshots needed to compare managed profiles over time.

## Browse and local search

Browse is backed only by the current in-memory model parsed from the last-known-good catalog.

- Root browse lists manufacturers alphabetically.
- Manufacturer screens list models alphabetically.
- Search is local/offline and searches manufacturer/model identity only, not creators or EQ details.
- Search is case-insensitive and tolerant of ordinary spacing/punctuation differences by normalizing alphanumeric text.
- Product/profile counts derive from the parsed catalog relationships; no hierarchy or variant meaning is invented from IDs.

Profile-selection persistence is now implemented at the Room/domain layer. Compose wiring for real checkboxes, staged Save, Select all/none, exact exclusions, future-profile behavior, and My Headphones population is the next UI integration step.

## Managed headphone persistence and selection

Room persists managed headphone identity plus per-profile state including:

- selected state;
- explicit exclusion state;
- full last-known OPRA profile snapshot with Unicode metadata;
- semantic fingerprint;
- first/last seen timestamps;
- unreviewed New/Updated flags;
- No-longer-available state.

A never-before-managed headphone defaults to automatic future inclusion ON and all currently selectable profiles checked. Not-compatible profiles are never included in that default. Unchecking a selectable profile while automatic inclusion remains ON becomes an explicit exclusion when saved.

Profile semantic fingerprints intentionally ignore provenance-link-only changes, matching the reference converter’s comparison behavior, while retaining the complete source link in the stored snapshot. Author/details comparison is case-insensitive for semantic-change detection.

## Catalog validity versus UAPP compatibility

Catalog validity and UAPP/ToneBoosters compatibility are separate concerns.

A structurally valid OPRA profile with a filter that the established converter cannot map safely must remain discoverable; it does **not** invalidate the entire OPRA catalog.

Current compatibility evaluation preserves the proven converter boundaries:

- `peak_dip`, `low_shelf`, and `high_shelf` are the supported filter mappings;
- frequency must be within 16 Hz–20 kHz;
- gain/preamp must be within -20 dB–+20 dB;
- Q must be within 0.1–10 where required by the supported mapping;
- OPRA's documented missing band-gain default is treated as 0 dB;
- otherwise-supported profiles with more than 10 bands are **Compatible with limitation**, with only the first 10 OPRA-priority bands eligible for conversion;
- unsupported filters or missing/out-of-range acoustic values are **Not compatible** and must remain non-selectable/non-exportable.

Missing creator/author data does not change acoustic compatibility. If the EQ itself is safely convertible, the profile remains selectable/exportable and uses the literal display/export label **Creator information missing** wherever the creator slot is required. The underlying stored OPRA author remains null/missing; the app never invents a real creator identity.

## Native ToneBoosters conversion

The Kotlin conversion core ports the established Python reference behavior without bundling Python:

- linear gain/preamp normalization over -20 dB to +20 dB;
- cube-root frequency normalization over 16 Hz to 20 kHz;
- cube-root Q normalization over 0.1 to 10;
- proven mappings for peak/dip, low shelf, and high shelf only;
- OPRA priority order retained;
- first 10 bands used when a profile exceeds the target limit, with an explicit warning;
- disabled placeholder filters fill the 10-band ToneBoosters shape;
- deterministic 66-value XML structure;
- ISO-8859-1-safe preset names/XML with full Unicode source metadata retained separately;
- deterministic headphone-first naming.

Golden fixtures and unit tests compare this behavior with the read-only Python reference.

## Compatibility invariant

`ProfileCompatibility.NotCompatible` is non-selectable and non-exportable in the domain model. UI filtering only decides whether that OPRA profile is shown; it never changes those domain properties.

If an already-selected profile later changes upstream and becomes Not compatible, reconciliation must clear its current selected state while preserving the last successfully generated local preset state and any existing exported file. No external file is deleted or rewritten automatically.

## Validation status

The runtime-catalog slice is validated at commit `fe1e273fcded3e51c6f79c30a6f4d9eb0f99daba`.

GitHub Actions passed both:

- `:app:testDebugUnitTest`
- `:app:assembleDebug`

The Room-managed-state and Kotlin-conversion slices are implemented but remain pending a green combined CI run after their current parity/serialization fixes.

## Planned Phase 1 slices

1. **Complete:** Foundation app shell and persistent settings.
2. **Complete:** Runtime `database_v1.jsonl` client, validation, last-known-good cache, and local browse/search model.
3. **In validation:** Room persistence for managed headphones, selections, exclusions, review state, and catalog snapshots.
4. **In validation:** Kotlin conversion port with golden parity fixtures from the read-only Python reference.
5. **Next after green:** Compose managed-headphone/profile-selection wiring, then refresh/change reconciliation and WorkManager background checks.
6. Storage Access Framework export with managed-file ownership/conflict behavior.
7. Public GitHub Release update checks and bundled changelog presentation.
8. Production adaptive/monochrome icon assets and final accessibility/release hardening.

Each slice must retain the approved Phase 0 UX and add tests before advancing behavior that depends on it.
