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
- Initial unit tests for compatibility, visibility defaults, isolation of visibility changes, and theme fallback behavior.

### Not yet implemented

- OPRA runtime catalog download/cache/refresh and offline catalog use.
- Room catalog and managed-headphone persistence.
- OPRA-to-UAPP/ToneBoosters conversion.
- System document-tree export.
- WorkManager background checks.
- GitHub Release update checks.
- Production app-icon assets.
