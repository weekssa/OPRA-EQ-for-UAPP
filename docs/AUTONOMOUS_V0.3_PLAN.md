# EQ Library v0.3 Autonomous Development Plan

This document defines the autonomous implementation lane for the multi-source/community EQ Library milestone. Work should continue without user intervention until a signed APK is ready for hands-on validation or a genuine product judgment cannot be resolved safely from source data, device constraints, or existing project decisions.

## Branch and release discipline

- Development branch: `eq-library-community-v0.3`.
- `main` remains the last validated public release line until v0.3 hands-on testing passes.
- PR #3 remains draft and unmerged until the signed v0.3 candidate passes hands-on validation.
- Never rotate the Android application ID or permanent release-signing identity.
- Increment `versionCode` for any installable candidate that supersedes v0.2.0.
- Do not publish v0.3 publicly before the user completes the hands-on test checkpoint.
- CI/test/lint/signature failures are implementation problems to fix autonomously, not reasons to interrupt the user.

## Product model

The app is a device-agnostic EQ library. OPRA is one source adapter and UAPP is one export target.

Canonical flow:

`many sources -> candidate ingestion -> headphone identity normalization -> source validation -> acoustic deduplication/versioning -> canonical EQ profile library -> device export`

Normal browsing shows one canonical physical headphone/IEM identity even when sources spell or brand it differently, and one canonical EQ profile when multiple sources reference the same acoustic tuning. All source aliases and provenance remain attached behind those canonical records.

Canonical source data is never rewritten to fit an export device. Arbitrary source filter counts and supported source filter types/frequency/gain/Q are retained. A source-provided preamp is preserved exactly; when a source omits preamp, canonical source preamp remains null and any EQ Library-generated safety headroom is stored and labeled separately.

## Required v0.3 scope

### 1. Common EQ domain model

Create a normalized EQ profile model independent of OPRA and export device. Preserve at minimum:

- canonical profile ID
- canonical headphone/model identity and variant
- source model/manufacturer aliases used to resolve that identity
- creator/author
- source category
- primary source
- secondary/source references
- source URLs
- target/curve
- source preamp, including an explicit absent/null state
- separately labeled EQ Library-generated playback safety headroom when needed
- normalized source-authentic filters without a device band-count limit
- source notes/details where redistribution is allowed
- provenance/confidence status
- original-source fingerprint
- normalized acoustic fingerprint
- first-seen, source-updated, discovered, and last-verified timestamps
- revision/version identity
- brief generated sound-impact description
- device compatibility/conversion status

### 2. Canonical headphone identity

Removing duplicate headphone rows is a canonicalization requirement, not cosmetic cleanup.

- Safe spelling, punctuation, whitespace, casing, and redundant-manufacturer differences may normalize automatically when manufacturer/model/subtype evidence is unambiguous.
- Reviewed aliases may collapse alternate source labels for the same physical headphone/IEM while preserving the original source labels as aliases/provenance.
- Variants, revisions, pads, impedance versions, modes, collaborations, or similarly named products must remain distinct whenever physical/acoustic equivalence is uncertain.
- Maintain explicit reviewed `distinct_pairs` to prevent future heuristics from over-merging known different products.
- Ambiguous identity candidates remain queued for review instead of being guessed.
- Browse and My Headphones must not show duplicate rows solely because different sources spell or brand the same physical product differently.
- Saved/managed state must survive canonical-identity migration by following reviewed aliases rather than becoming detached or duplicated.

The maintained decision source is `config/headphone_identity_decisions.json`; `tools/headphone_identity_audit.py` and CI provide the review queue and regression guard.

### 3. Canonical acoustic deduplication

Acoustic-profile deduplication occurs after headphone identity is resolved.

Matching order:

1. Exact acoustic/filter match: same source preamp state and equivalent frequency/gain/Q/filter response.
2. Normalized acoustic match after harmless aliases, ordering, precision, and formatting differences.
3. Identity/lineage match: same canonical headphone + creator + target + effectively identical response.
4. Materially different tunings remain distinct even when creator/target names match.

Never deduplicate solely by name. Choose the most authoritative/original source as primary and retain all mirrors/reposts as references.

### 4. Revision history

Community/source updates must never silently replace a previous genuine tuning.

- Preserve every materially different historical source revision.
- Mark one revision as Latest.
- Show source update time when available and discovery time separately.
- Allow users to view/select/export older revisions.
- Formatting-only/source-metadata edits that do not change the acoustic fingerprint may update provenance metadata without creating a new acoustic revision.
- A known application-modeling bug must not be preserved as fake acoustic history. The narrowly scoped legacy generated-preamp repair may remove only a provably equivalent erroneous representation when source identity and exact filters match the corrected source-authentic candidate.

### 5. Source adapters

Implement broad source ingestion subject to current license/terms verification:

1. Existing OPRA adapter migrated into the normalized model.
2. AutoEq structured data across the qualified corpus.
3. Squiglink-derived public data where technically and legally usable.
4. Established creator/measurer provenance distinct from measurement-derived AutoEq results.
5. Curated public community EQ candidates from Reddit, Head-Fi, Audio Science Review, The HEADPHONE Community, device communities, public GitHub repositories/Gists, and additional qualified public sources.
6. Structured user submissions through the repository `Submit an EQ source` Issue Form.

Do not promise literally every EQ on the internet. Track source coverage explicitly and expand by adding qualified source data/adapters rather than per-headphone application code.

### 6. Community discovery and form-intake pipeline

Community discovery runs outside the Android runtime. The app consumes only a cleaned published catalog/cache.

Pipeline:

`source watchers / submissions -> candidate queue -> parser -> headphone identity resolution -> provenance validation -> acoustic dedupe/versioning -> catalog publish -> Android sync`

Candidate states include:

- new_candidate
- parsed
- duplicate
- validated
- needs_review
- published
- rejected

Use incremental cursors/timestamps/IDs/hashes rather than continually rescanning the same content. Prefer structured/API/search feeds where available and respect source terms, robots guidance, rate limits, and redistribution constraints.

Store structured EQ parameters and provenance rather than copying forum prose.

The repository Issue Form is an intake surface only:

- manufacturer and exact model are separate fields so intake does not create avoidable identity ambiguity;
- variant/revision/pads/mode are optional and must never be invented;
- structured PEQ is parsed with the same source-authentic parser used by community ingestion;
- arbitrary filter counts are preserved and missing source preamp remains null;
- preset links may be staged for later source review/fetching but are not silently interpreted;
- invalid/incomplete submissions remain `needs_review` with diagnostics;
- staged submissions are never publication-eligible merely because the Issue Form was submitted.

Curated community publication must be whole-library capable. The scheduled publisher processes every qualified `catalog/discovery/*_community_curated.json` input in deterministic order; adding another headphone must not require a new hard-coded workflow.

### 7. Source registry and continual updates

Maintain a machine-readable source registry with at least:

- source ID/type
- source URL/scope
- discovery/search patterns
- cadence
- parser and parser version
- last successful scan
- cursor/high-water mark
- active/reviewing/paused/retired state
- health/failure state
- licensing/redistribution notes and review state

Maintain three independent currentness loops:

1. known-source update discovery,
2. existing-profile revision detection,
3. discovery/qualification of entirely new public sources.

Initial cadence target:

- active structured/community sources: daily where appropriate
- slower sources: weekly where appropriate

Publish conservatively. Ambiguous candidates stay in `needs_review` and do not become visible catalog EQs automatically. A failed source retains last-known-good data rather than invalidating the usable catalog.

### 8. Provenance and target classification

Classification should distinguish at least:

- verified/structured source
- measurement-derived/algorithmic
- established creator/measurer
- community/user tuning
- personal/imported

Only state a target when explicit or high confidence. Otherwise label as Custom/User tuning rather than guessing.

Mirrors and reposts are secondary provenance and must not create duplicate visible EQs when their original acoustic tuning is already canonical.

### 9. Sound-impact summaries

Generate short neutral descriptions of what an EQ changes relative to stock when reliable comparison data exists.

Examples of acceptable style:

- `Adds sub-bass and slightly reduces upper-mid energy.`
- `Warmer low end with a smoother 3-5 kHz region.`

When reliable stock response is unavailable, describe the filter action rather than claiming a measured stock-vs-EQ difference. Do not repeat subjective forum praise as objective fact. Generated safety headroom must be described separately from source-authored preamp/filter data.

### 10. Android UX

Primary flow:

`Headphone -> choose EQ/revision -> understand source/target/change -> choose device -> Export`

Required surfaces:

- browse/search by canonical headphone identity
- source/creator/target filters
- clear source/provenance labeling
- Latest and historical revision list with timestamps
- favorites/saved profiles
- My EQs/personal imports architecture
- existing My Headphones and cleanup behaviors preserved
- device chooser preserved from v0.2.0
- device export remains explicit and never occurs merely because an EQ is selected

Major user-facing changes remain behind the project UX approval gate. In particular, the larger export-capability/fidelity presentation must be approved before implementation.

### 11. Export architecture

Preserve the v0.2.0 target-device abstraction and extend it rather than adding source-specific export logic.

Current targets:

- UAPP/ToneBoosters
- TRN Black Pearl
- Topping DX5 II (hardware validation pending)
- Topping DX1 II (hardware validation pending)

Every target declares capabilities such as supported filter types/ranges, preamp support/range, format, and `maxBands`, where `maxBands` may be null for no known fixed limit. Device limits never constrain canonical ingestion or storage.

Each export should report one of:

- Exact/preserved — source EQ fits the target without acoustic transformation.
- Optimized — target conversion drops, converts, clamps, substitutes generated playback headroom, or otherwise transforms the source for target constraints. Optimized output is labeled `EQ Library optimized conversion`.
- Not faithfully representable — safe/defined conversion is not available.

For the current UAPP/ToneBoosters target, the target capability is 10 bands: preserve OPRA/source priority and use the first 10 only in the generated target representation, with a warning. The canonical EQ retains every source band.

Preserve the original normalized source tuning separately from every generated device representation.

### 12. Offline and failure behavior

- Last-known-good catalog remains available offline.
- Source or discovery outages must not invalidate an existing usable catalog.
- Catalog publication must be atomic/validated before replacing the last-known-good cache.
- Source removals should preserve provenance/history where legally appropriate rather than silently deleting user-visible history.
- A removed selected OPRA/source profile keeps its last generated file until the user explicitly removes it and is marked no longer available.

## Decisions that do NOT require user intervention

Resolve autonomously:

- schema and package organization
- parser implementation details
- harmless naming normalization
- evidence-backed headphone alias decisions when physical identity is unambiguous
- preserving an ambiguous pair for review rather than guessing
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

1. Ambiguous headphone identity/physical variant that materially affects catalog correctness and cannot safely remain queued for later review.
2. Questionable attribution where choosing a primary creator/source is subjective or potentially misleading.
3. A licensing/terms question that requires a product choice to omit, link-only, or redistribute a source.
4. A materially different UX/product tradeoff not covered by this plan or the project runbook.
5. Physical hardware validation that cannot be completed in CI/emulation.

Do not interrupt for ordinary implementation errors or identity/source candidates that can safely remain in a review queue.

## Software acceptance gate before hands-on testing

Do not ask the user to test until all applicable items pass:

- unit tests
- lint
- release assembly
- CodeQL/security checks configured for the repo
- deterministic catalog/schema validation
- source-authentic arbitrary-filter/preamp tests
- canonical headphone-identity dedupe/alias/distinct-pair tests
- Android canonical-identity migration/state-preservation tests
- acoustic deduplication/version-history tests
- form-intake staging tests proving submissions cannot auto-publish
- whole-library community publication tests/workflow without per-headphone hard-coding
- offline last-known-good tests
- export regression tests for UAPP and Black Pearl
- Exact/preserved vs Optimized fidelity tests from target capabilities
- device-target isolation tests
- file ownership/conflict/cleanup tests
- migration/upgrade test from the v0.2.0 data model
- permanent signing identity verification
- signed APK artifact generated from the v0.3 candidate branch

The actual running Android path must consume/render the canonical multi-source catalog; isolated domain-model tests alone are not sufficient. The validation catalog must demonstrate more than one manufacturer and more than one source/provenance kind through the real application path.

## Hands-on test checkpoint

The first required user intervention should be one consolidated test package containing:

- one signed APK that upgrades v0.2.0 in place
- concise change summary
- one ordered checklist with explicit STOP conditions
- known-deferred items listed separately (currently DX5 II/DX1 II hardware testing)
- sample searches/profiles that exercise canonical headphone dedupe and revision history
- UAPP and Black Pearl export validation steps

If those tests pass, v0.3 can move to release preparation. Until then, keep the candidate off `main` and do not publish it as the latest public release.