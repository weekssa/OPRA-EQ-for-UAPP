# EQ Library — Architecture

This document supplements `docs/CHATGPT_PROJECT_RUNBOOK.md`. The runbook remains authoritative for product and UX decisions.

For current implementation-time decisions and execution order, also read:

- `docs/PHASE1_DECISIONS.md`
- `docs/SOURCE_INGESTION_STRATEGY.md`
- `docs/AUTONOMOUS_V0.3_PLAN.md`
- `docs/V0.3_LOCKED_EXECUTION_PLAN.md`

`docs/V0.3_LOCKED_EXECUTION_PLAN.md` contains the latest approved v0.3 product direction and supersedes older OPRA-only or export-target-visibility assumptions where they conflict. `docs/PHASE1_DECISIONS.md` records later approved refinements, including the 2026-08-31 zero-selection default, Add-triggered initial export, recovery-only Export UI, provider-resilient SAF ownership, and confirmed Black Pearl gain-adjustment/caution behavior.

## Android baseline

The application is a single native Android module using Kotlin and Jetpack Compose.

- application ID / namespace: `com.weekssa.opraeqforuapp`
- user-facing product name: **EQ Library**
- minSdk: 26
- compileSdk / targetSdk: 36
- Java target: 17
- Android Gradle Plugin: 9.2.0
- Kotlin / Compose compiler plugin: 2.3.21
- Compose BOM: 2026.06.00
- Room: durable app-owned saved-EQ/export state
- Preferences DataStore: local appearance, active output, enabled outputs, export-tree, refresh/update presentation preferences
- WorkManager: approximately daily catalog reconciliation backup
- Storage Access Framework / DocumentFile: explicit user-folder export and app-owned file cleanup

Do not casually bump build/dependency versions without checking current compatibility guidance and validating CI.

## Product model

EQ Library is a source-agnostic canonical EQ library. OPRA is one source. UAPP/ToneBoosters and TRN Black Pearl are output/device contexts.

Canonical flow:

`many sources -> discovery/intake -> source-authentic parse -> headphone/general identity -> provenance -> acoustic dedupe/revisions -> canonical library -> target-specific conversion/export/flash`

Canonical source records are never rewritten to fit an output device. Preserve arbitrary source filter counts, supported source filter types, frequency, gain, Q, creator, target/intent, details, provenance, and source preamp. Missing source preamp remains null. Any generated safety headroom is separate derived metadata.

## Package and responsibility boundaries

`com.weekssa.opraeqforuapp.ui`

- Compose screens, app shell, navigation, dialogs, accessibility semantics, appearance, update banners, active-output selector, EQ Library/My EQs/Settings interaction.
- UI must not parse source catalogs or implement device DSP conversion.

`com.weekssa.opraeqforuapp.domain`

- canonical EQ models, identity, compatibility/fidelity classification, selection/saved-state semantics, deterministic naming, target conversion, export planning, SemVer/update comparison.
- Critical rules remain unit-testable without Android framework dependencies.

`com.weekssa.opraeqforuapp.data`

- canonical catalog acquisition and last-known-good cache;
- Room entities/DAOs/repositories;
- saved profile snapshots/fingerprints/revisions;
- catalog reconciliation;
- WorkManager scheduling;
- SAF export ownership/cleanup;
- public GitHub release metadata;
- Preferences DataStore;
- Black Pearl USB integration behind a narrow device coordinator.

Source discovery, crawling, terms/provenance checks, candidate qualification, and catalog publication stay outside the Android runtime.

## Runtime canonical catalog

The Android runtime consumes only a validated published canonical catalog. It must not scrape GitHub/forums during normal operation.

OPRA runtime source remains `https://opra.roonlabs.net/database_v1.jsonl` as one upstream feed to the catalog-building pipeline.

Requirements:

- candidate catalog is fully parsed/validated before promotion;
- last-known-good cache remains usable during refresh and offline;
- malformed/partial candidates never replace good state;
- source failures never invalidate existing catalog data;
- catalog comparison/update behavior is deterministic;
- ordinary source/catalog updates do not require a new APK when schema stays compatible.

## Foreground refresh/currentness

On app launch/resume, use cached data immediately. If the **last successful** catalog refresh is approximately 24 hours old or older, start an opportunistic foreground refresh. A failed attempt does not advance the last-success timestamp.

No network must never block or empty the app. Keep cached data and show concise non-blocking status such as `Couldn't refresh EQ Library — using your saved library`.

Approximately-daily WorkManager remains a backup path, and Settings retains manual Refresh now.

## Canonical identity

Headphone identity cleanup is part of ingestion.

- safe spelling/punctuation/casing/redundant-manufacturer differences may normalize automatically when equivalence is clear;
- reviewed aliases collapse proven alternate labels while preserving source names;
- reviewed distinct pairs prevent over-merging;
- ambiguous identity may remain queued without blocking unrelated publication.

Community configuration rule:

- explicitly stated nozzle/pad/revision/ANC/acoustic mode is retained as a labeled configuration identity;
- if a source does not state a configuration, use the generic/base model bucket rather than blocking publication or inventing a variant;
- SIMGOT EW300 explicitly follows this rule: named nozzle -> preserve nozzle; unstated -> generic EW300.

Saved-state migration follows canonical aliases so users do not lose selections when identity improves.

## Canonical acoustic dedupe and revisions

After identity resolution:

- equivalent acoustic fingerprints merge into one canonical tuning with multiple source references;
- original/authoritative provenance becomes primary where known;
- mirrors/reposts become secondary provenance;
- materially changed same-lineage tuning becomes an immutable new revision;
- clearly separate alternatives remain separate profiles;
- formatting/provenance-only changes do not manufacture acoustic revisions.

Verified/Unverified promotion is metadata-only when acoustic fingerprint is unchanged.

## Community and source ingestion

Primary public-community surfaces:

- Reddit audio communities
- Head-Fi
- Audio Science Review
- The HEADPHONE Community / Headphones.com
- qualified public GitHub repositories/Gists
- additional communities qualified over time

Use high-signal structured discovery rather than indiscriminate scraping. Preserve coefficients and provenance, not unrelated forum prose. Do not access private/restricted/authenticated content or bypass controls.

A mechanically valid, source-traceable community EQ may publish as **Unverified** when identity/provenance/dedupe checks are safe. Ambiguous/malformed records remain quarantined without blocking unrelated catalog growth.

The repository Submit an EQ Issue Form remains an optional contribution route, not the primary population strategy.

Focused discovery begins with the priority queue defined in `docs/V0.3_LOCKED_EXECUTION_PLAN.md` while recurring discovery expands coverage in parallel.

## General EQs

Canonical classification separates scope/purpose from headphone identity.

Supported user-facing General EQ groups:

- Sound
- Genre
- Utility

General presets remain standalone in v0.3. Do not silently layer/combine them with headphone-specific EQs.

Do not invent genre/intent from filter shape. Classification must follow explicit source context.

## Output/device context

The current output/device is global operating context, not a library filter.

Initial contexts:

- USB Audio Player PRO / ToneBoosters
- TRN Black Pearl
- Universal Parametric EQ
- Poweramp / Poweramp Equalizer
- Wavelet

The canonical library remains complete regardless of active output.

Each profile is evaluated against the active output as:

- Exact / preserved
- Optimized
- Not exportable / not faithfully representable

Changing output changes conversion/export/flash context and My EQs saved collection; it does not delete or hide canonical profiles.

## My EQs and Add workflow

My EQs is output-specific and may contain different saved profiles for different outputs without duplicating canonical source data.

Saved content is grouped into Headphones and General EQs.

For a newly managed headphone, no EQ profiles are selected by default and automatic future-profile inclusion starts OFF. The user explicitly chooses the EQs they want.

`Add` is the normal completion action: save the active-output selection and immediately create/update the initial exported file for each selected exportable EQ. When the output requires SAF export and no retained folder exists yet, Add requests the folder once and then completes the initial export. Add never automatically flashes hardware.

Export state remains output-specific but is recovery/currentness-oriented. When every expected app-owned file exists and matches the current generated output, My EQs shows no Export controls or routine export-status text. `Export all`, per-row Export, and a concise affected count appear only when files are missing/stale, generated output changed, or folder access needs repair. Remove remains explicit. Removing the final selected EQ removes that headphone from My EQs for the active output.

## Conversion and target capabilities

Every output implements declared capabilities rather than altering canonical ingestion.

A `DeviceEqCapabilities`-style domain model includes supported filter types/ranges, preamp/headroom behavior, format restrictions, and nullable/unlimited `maxBands` where appropriate.

UAPP/ToneBoosters currently uses the proven 10-band representation. Preserve source priority/order and first 10 in generated output with an explicit limitation warning; retain all source bands canonically.

Kotlin ToneBoosters conversion preserves the established reference behavior and deterministic XML. ToneBoosters XML remains ISO-8859-1-safe while full Unicode source metadata stays local.

Golden fixtures protect parity for normalization, preamp, filters, deterministic output, >10-band handling, unsupported filters, naming/encoding, and export behavior.

## TRN Black Pearl architecture

Black Pearl direct Flash is EQ-focused and exists only from My EQs when Black Pearl is active and direct Flash is enabled.

Reference implementations are GPL-licensed and may be studied for observable protocol behavior only; do not copy GPL implementation code into this Apache-2.0 project.

Known independently corroborated protocol behavior includes:

- USB VID `0x3302`, PID `0x43E8`;
- HID report `0x4B`;
- global playback gain command `0x03`;
- PEQ values command `0x09`;
- flash/save command `0x01`;
- 10 hardware PEQ bands;
- active-slot value included in PEQ writes;
- native peak, low-shelf, and high-shelf type codes in the device protocol.

Direct Flash may adjust global playback gain when that is required to faithfully apply source preamp or separately generated safety headroom. This behavior is explicitly user-approved. Implement the device's actual raw/dB mapping rather than the reference app's approximate percentage importer mapping.

The Flash sequence must read current hardware gain, replace any prior EQ Library-applied attenuation rather than accumulating it, apply the required new attenuation only when representable, write/pad the EQ bands, latch, and persist. Confirmation must disclose the required playback-gain adjustment. Fail rather than silently clamp an unrepresentable gain. Do not change DAC reconstruction filter, gain mode, amplifier topology, balance, microphone controls, or other unrelated settings.

The reference applications' approximately `-10 dB..+10 dB` **per-filter gain range** is maintained as the currently validated/recommended Black Pearl range, not misclassified as the signed packet's encoding limit. A finite band gain outside ±10 dB that still fits the signed 1/256 dB protocol field remains source-preserving/Exact at the conversion boundary and may be flashed only after a caution that identifies the affected value and requires **Flash anyway**. No clamp or target-specific acoustic mutation is allowed. Frequency/Q limits, unsupported filter types, non-finite or wire-unencodable gain, and absolute global playback-gain limits remain hard failures until independently validated evidence changes them.

Hardware-independent packet/conversion behavior must be unit-tested; final USB/flash behavior remains a hands-on validation checkpoint.

## Export architecture

Export remains explicit at the storage-permission boundary and automatic after Add once access exists.

- active output drives generated file format; no redundant target chooser is required;
- first needed folder export uses the system picker and persisted supported directory access;
- Add completes the initial export after folder access is available;
- no broad storage permission or writes into another app's private storage;
- preferred human-readable deterministic filenames are generated in the domain layer;
- stable export identity is output/path + product/profile identity, not exact provider filename spelling;
- the data layer records the exact new SAF document URI returned by the provider, the provider-returned actual display name, generated fingerprint, and content hash;
- provider normalization/extension changes on a newly created document are accepted and tracked rather than causing the successful file to be deleted;
- internal same-name candidates receive stable identity-derived suffixes before export;
- an unknown same-name external file is never overwritten or deleted; EQ Library requests a stable disambiguated fallback name and tracks the new URI the provider creates;
- if a provider ever returns the exact URI of a pre-existing unowned file instead of creating a new child, treat that as unsafe provider behavior and do not write;
- app-owned files are updated by their tracked URI even if their physical display name differs from the current preferred candidate name;
- app-owned files may be updated during Add or later explicit recovery Export;
- per-file failures do not roll back independent successes or local My EQs membership;
- optional cleanup deletes only app-owned tracked files, with the exact tracked URI as the primary identity;
- Export controls are hidden while expected files are present/current and surface only for recovery/currentness needs.

Existing app-owned files are not renamed merely because naming logic changes. Their tracked URI remains authoritative while they exist.

Black Pearl Flash remains independent of file Export.

## App updates, attribution, privacy

The app checks public GitHub Release metadata without credentials at a modest cadence. A newer version may show a nonblocking banner and What's new/Get update actions. No silent APK download/self-install/install-unknown-apps permission.

Source attribution is retained for OPRA, AutoEq, community creators, and other qualified sources. Do not imply endorsement by OPRA, Roon Labs, UAPP, ToneBoosters, TRN, Sony, or headphone manufacturers.

Selections/settings/generated preset state remain local. No account, analytics, telemetry, or cloud backend is required for end users.

## Validation and CI

Android CI remains the automated gate and must include applicable unit tests, lint, debug/release assembly, catalog/schema validation, and security checks.

Before hands-on v0.3 testing, validate at least:

- multi-source real Android catalog path;
- canonical identity migration/dedupe/revisions;
- arbitrary source filter counts and missing-preamp behavior;
- community recurring discovery and coverage state;
- General EQ classification;
- active-output persistence/migration;
- no active-output library hiding;
- output-specific My EQs;
- zero-selection/auto-include-off defaults;
- Add-triggered initial export and recovery-only Export visibility;
- removal of empty managed headphones;
- export-state behavior, including provider-adjusted names, stable internal collisions, unowned-name fallback, tracked-URI updates, and cleanup safety;
- launch/resume due/not-due/offline refresh;
- UAPP regression/parity;
- Black Pearl target conversion, exact global-gain adjustment, replacement-not-cumulative attenuation, protocol-encodable out-of-validated-range filter-gain caution/no-clamp behavior, and hardware-independent USB protocol behavior;
- Room/DataStore migration from installed release state;
- signed candidate upgrade integrity.

The v0.3 branch/PR stays unmerged until the signed candidate passes hands-on testing.
