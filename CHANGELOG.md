# Changelog

All notable changes to **OPRA EQ for UAPP** will be documented in this file.

The project uses Semantic Versioning. Development releases remain in the `0.x` series until the first stable `v1.0.0` release.

## [0.1.0] - Unreleased

### Added

- Native Android project foundation for application ID `com.weekssa.opraeqforuapp`.
- Kotlin + Jetpack Compose app shell with the approved **My Headphones** and **Browse OPRA** peer destinations.
- Accessible Refresh and Settings actions and Settings back behavior matching the approved Phase 0 navigation model.
- Persistent local **System default / Light / Dark** appearance preference using Preferences DataStore.
- Persistent local visibility preferences for **Fully compatible**, **Compatible with limitation**, and **Not compatible**, all enabled by default.
- Initial domain compatibility model that makes **Not compatible** profiles non-selectable and non-exportable by construction.
- Runtime OPRA `database_v1.jsonl` download from the Roon Labs mirror with full-candidate validation before cache promotion.
- App-private last-known-good OPRA catalog cache with atomic replacement, offline reuse, startup freshness checks, and manual Refresh.
- Local Manufacturer → Model browsing and manufacturer/model search over the cached OPRA catalog.
- OPRA profile metadata display with **Fully compatible**, **Compatible with limitation**, and **Not compatible** classification separated from catalog validity.
- Settings catalog status with saved-catalog counts, last successful refresh time, and manual refresh action.
- Unit tests covering compatibility, visibility defaults, theme fallback, OPRA JSONL parsing, relationship validation, local search normalization, unsupported-filter discoverability, 10-band classification, OPRA band-gain defaults, first-load failure, fresh-cache reuse, and last-known-good preservation after a malformed refresh.

### Not yet implemented

- Room persistence for managed headphones, exact profile selections/exclusions, review state, and catalog snapshots.
- OPRA-to-UAPP/ToneBoosters conversion.
- System document-tree export.
- WorkManager background catalog checks and managed-profile change reporting.
- GitHub Release update checks.
- Production app-icon assets.
