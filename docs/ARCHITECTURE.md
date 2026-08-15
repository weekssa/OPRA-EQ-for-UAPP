# OPRA EQ for UAPP — Architecture

This document supplements `docs/CHATGPT_PROJECT_RUNBOOK.md`. The runbook remains authoritative for product and UX decisions.

## Phase 1 foundation

Phase 1 starts as a single native Android application module using Kotlin and Jetpack Compose.

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
- Future Room entities/DAOs belong here.

Platform-specific integrations such as Storage Access Framework export and WorkManager scheduling should be isolated behind narrow interfaces rather than leaking into conversion/domain rules.

## Navigation foundation

The two approved top-level destinations are modeled as peer state rather than a history stack. Switching between **My Headphones** and **Browse OPRA** therefore does not make system Back switch tabs.

Settings is currently modeled as a secondary screen that returns to the top-level destination that opened it. Deeper Browse and My Headphones navigation will preserve the approved per-area Back behavior as those destinations are implemented.

## Local settings

Preferences DataStore currently stores:

- appearance: System default / Light / Dark;
- visibility of Fully compatible profiles;
- visibility of Compatible-with-limitation profiles;
- visibility of Not-compatible profiles.

These are presentation preferences only. They must never mutate profile compatibility, selection, conversion, or export eligibility.

## Compatibility invariant

`ProfileCompatibility.NotCompatible` is non-selectable and non-exportable in the domain model. UI filtering only decides whether that OPRA profile is shown; it never changes those domain properties.

## Planned Phase 1 slices

1. Foundation app shell and persistent settings.
2. Runtime `database_v1.jsonl` client, validation, last-known-good cache, and local browse/search model.
3. Room persistence for managed headphones, selections, exclusions, review state, and catalog snapshots.
4. Kotlin conversion port with golden parity fixtures from the read-only Python reference.
5. Refresh/change reporting and WorkManager background checks.
6. Storage Access Framework export with managed-file ownership/conflict behavior.
7. Public GitHub Release update checks and bundled changelog presentation.
8. Production adaptive/monochrome icon assets and final accessibility/release hardening.

Each slice must retain the approved Phase 0 UX and add tests before advancing behavior that depends on it.
