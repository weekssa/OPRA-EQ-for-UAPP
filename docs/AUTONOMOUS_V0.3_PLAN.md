# EQ Library v0.3 Autonomous Development Plan

This document defines the autonomous implementation lane for the multi-source/community EQ Library milestone. Work should continue without user intervention until a signed APK is ready for hands-on validation or a genuine product judgment cannot be resolved safely from source data, device constraints, or existing project decisions.

## Branch and release discipline

- Development branch: `eq-library-community-v0.3`.
- `main` remains the last validated public release line until v0.3 hands-on testing passes.
- Never rotate the Android application ID or permanent release-signing identity.
- Increment `versionCode` for any installable candidate that supersedes v0.2.0.
- Do not publish v0.3 publicly before the user completes the hands-on test checkpoint.
- CI/test/lint/signature failures are implementation problems to fix autonomously, not reasons to interrupt the user.

## Product model

The app is a device-agnostic EQ library. OPRA is one source adapter and UAPP is one export target.

Canonical flow:

`many sources -> candidate ingestion -> validation -> deduplication/versioning -> canonical EQ profile library -> device export`

Normal browsing shows one canonical EQ profile even when multiple sources reference the same tuning. All provenance remains attached behind that profile.

## Required v0.3 scope

### 1. Common EQ domain model

Create a normalized EQ profile model independent of OPRA and export device. Preserve at minimum:

- canonical profile ID
- headphone/model identity and variant
- creator/author
- source category
- primary source
- secondary/source references
- source URLs
- target/curve
- preamp
- normalized filters
- source notes/details where redistribution is allowed
- provenance/confidence status
- original-source fingerprint
- normalized acoustic fingerprint
- first-seen, source-updated, discovered, and last-verified timestamps
- revision/version identity
- brief generated sound-impact description
- device compatibility/conversion status

### 2. Canonical deduplication

Deduplication is required before multi-source data is shown.

Matching order:

1. Exact acoustic/filter match: same preamp and equivalent frequency/gain/Q/filter response.
2. Normalized acoustic match after harmless aliases, ordering, precision, and formatting differences.
3. Identity match: same headphone + creator + target + effectively identical response.
4. Materially different tunings remain distinct even when creator/target names match.

Never deduplicate solely by name. Choose the most authoritative/original source as primary and retain all mirrors/reposts as references.

### 3. Revision history

Community/source updates must never silently replace a previous tuning.

- Preserve every materially different historical revision.
- Mark one revision as Latest.
- Show source update time when available and discovery time separately.
- Allow users to view/select/export older revisions.
- Formatting-only/source-metadata edits that do not change the acoustic fingerprint may update provenance metadata without creating a new acoustic revision.

### 4. Source adapters

Implement source ingestion in this order, subject to current license/terms verification:

1. Existing OPRA adapter migrated into the normalized model.
2. AutoEQ structured data.
3. Squiglink-derived public data where technically and legally usable.
4. Curated community EQ candidates from public sources such as Reddit, Head-Fi, Audio Science Review, GitHub repositories/Gists, and established measurer communities.

Do not promise literally every EQ on the internet. Track source coverage explicitly.

### 5. Community discovery pipeline

Community discovery must run outside the Android runtime. The app consumes a cleaned published catalog/cache.

Pipeline:

`source watchers -> candidate queue -> parser -> normalization -> provenance validation -> acoustic dedupe/versioning -> catalog publish -> Android sync`

Candidate states:

- new_candidate
- parsed
- duplicate
- validated
- needs_review
- published
- rejected

Use incremental cursors/timestamps/IDs/hashes rather than continually rescanning the same content. Prefer structured/API/search feeds where available and respect source terms, robots guidance, rate limits, and redistribution constraints.

Store structured EQ parameters and provenance rather than copying forum prose.

### 6. Source registry and continual updates

Maintain a machine-readable source registry with at least:

- source ID/type
- source URL/scope
- discovery/search patterns
- cadence
- last successful scan
- cursor/high-water mark
- active/disabled state
- licensing/redistribution notes

Initial cadence target:

- active structured/community sources: daily where appropriate
- slower sources: weekly where appropriate

Publish conservatively. Ambiguous candidates stay in `needs_review` and do not become visible catalog EQs automatically.

### 7. Provenance and target classification

Classification should distinguish at least:

- verified/structured source
- measurement-derived/algorithmic
- established creator/measurer
- community/user tuning
- personal/imported

Only state a target when explicit or high confidence. Otherwise label as Custom/User tuning rather than guessing.

### 8. Sound-impact summaries

Generate short neutral descriptions of what an EQ changes relative to stock when reliable comparison data exists.

Examples of acceptable style:

- `Adds sub-bass and slightly reduces upper-mid energy.`
- `Warmer low end with a smoother 3-5 kHz region.`

When reliable stock response is unavailable, describe the filter action rather than claiming a measured stock-vs-EQ difference. Do not repeat subjective forum praise as objective fact.

### 9. Android UX

Primary flow:

`Headphone -> choose EQ/revision -> understand source/target/change -> choose device -> Export`

Required surfaces:

- browse/search by headphone
- source/creator/target filters
- clear source/provenance labeling
- Latest and historical revision list with timestamps
- favorites/saved profiles
- My EQs/personal imports architecture
- existing My Headphones and cleanup behaviors preserved
- device chooser preserved from v0.2.0
- device export remains explicit and never occurs merely because an EQ is selected

### 10. Export architecture

Preserve the v0.2.0 target-device abstraction and extend it rather than adding source-specific export logic.

Current targets:

- UAPP/ToneBoosters
- TRN Black Pearl
- Topping DX5 II (hardware validation pending)
- Topping DX1 II (hardware validation pending)

Each export should report one of:

- Exact
- Converted
- Optimized
- Not faithfully representable

Preserve the original normalized source tuning separately from the generated device representation.

### 11. Offline and failure behavior

- Last-known-good catalog remains available offline.
- Source or discovery outages must not invalidate an existing usable catalog.
- Catalog publication must be atomic/validated before replacing the last-known-good cache.
- Source removals should preserve provenance/history where legally appropriate rather than silently deleting user-visible history.

## Decisions that do NOT require user intervention

Resolve autonomously:

- schema and package organization
- parser implementation details
- harmless naming normalization
- filter aliases and numeric precision normalization
- CI/test/lint/compiler failures
- cache/storage implementation
- source adapter boundaries
- deterministic IDs/fingerprints
- routine provenance metadata handling
- search/index implementation
- source discovery rate limiting and retry behavior
- UI copy where intent is already defined

## Only interrupt the user for

1. Ambiguous headphone identity/physical variant that materially affects catalog correctness.
2. Questionable attribution where choosing a primary creator/source is subjective or potentially misleading.
3. A licensing/terms question that requires a product choice to omit, link-only, or redistribute a source.
4. A materially different UX/product tradeoff not covered by this plan or the project runbook.
5. Physical hardware validation that cannot be completed in CI/emulation.

Do not interrupt for ordinary implementation errors or decisions that can be resolved from existing technical evidence.

## Software acceptance gate before hands-on testing

Do not ask the user to test until all applicable items pass:

- unit tests
- lint
- release assembly
- CodeQL/security checks configured for the repo
- deterministic catalog/schema validation
- deduplication/version-history tests
- offline last-known-good tests
- export regression tests for UAPP and Black Pearl
- device-target isolation tests
- file ownership/conflict/cleanup tests
- migration/upgrade test from the v0.2.0 data model
- permanent signing identity verification
- signed APK artifact generated from the v0.3 candidate branch

## Hands-on test checkpoint

The first required user intervention should be one consolidated test package containing:

- one signed APK that upgrades v0.2.0 in place
- concise change summary
- one ordered checklist with explicit STOP conditions
- known-deferred items listed separately (currently DX5 II/DX1 II hardware testing)
- sample searches/profiles that exercise dedupe and revision history
- UAPP and Black Pearl export validation steps

If those tests pass, v0.3 can move to release preparation. Until then, keep the candidate off `main` and do not publish it as the latest public release.
