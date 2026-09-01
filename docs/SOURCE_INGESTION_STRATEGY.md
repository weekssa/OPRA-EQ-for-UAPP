# EQ Library v0.3 Source Ingestion Strategy

This document expands the source-adapter section of `AUTONOMOUS_V0.3_PLAN.md`. The goal is broad coverage without flattening provenance quality. EQ Library should ingest normalized source-authentic EQ parameters and source metadata, not republish third-party prose or convert canonical data to device constraints.

## Core ingestion rules

These rules apply to every lane below.

- Resolve headphone identity before presenting or publishing duplicate physical-model rows.
- Preserve source preamp exactly; when the source omits preamp, canonical `preamp_gain_db` remains null.
- Store any EQ Library-generated playback safety headroom separately as derived metadata.
- Preserve arbitrary source filter counts and source filter parameters/types supported by the canonical schema; never truncate ingestion to a device band limit.
- Never invent filters, target claims, variants, authorship, or provenance.
- Ambiguous identity/provenance/rights candidates remain review-only.
- Mirrors/reposts become secondary provenance when they carry an already-canonical acoustic tuning.
- Genuine changed tunings become immutable revisions; application-modeling corrections must not create fake acoustic history.
- Once a genuine canonical EQ/revision is validly published, retain it in the current living archive even if the original source later moves, disappears, pauses, or retires. Source status/provenance may change; ordinary source lifecycle events do not delete archived acoustic history.

## Ingestion lanes

### A. Structured canonical catalogs

Highest-priority machine-readable or consistently structured sources.

- OPRA runtime catalog
- AutoEq structured measurements/results/targets
- Squiglink/Squiglink-compatible public structured data where terms allow
- future structured EQ databases with explicit usable licensing/terms

These sources can feed automatic validation, headphone identity normalization, acoustic deduplication, revision detection, and publication when provenance is clear.

### B. Established creator / measurer sources

Preserve these independently from algorithmically derived AutoEq results even when AutoEq uses their measurements.

- oratory1990 authored EQ presets and measurement-linked revisions
- Crinacle / measurement-derived sources where redistribution terms allow
- Headphones.com / Resolve authored or community-posted EQs with explicit attribution
- other established reviewers/measurers with clear original-source presets

Never infer that an AutoEq result based on a creator's measurement is the same as that creator's authored EQ preset. Creator presets and measurement-derived AutoEq tunings may share measurement provenance while remaining distinct canonical profiles.

### C. Public forums and communities

Discover candidate EQs from public posts that contain structured EQ parameters or clearly linked preset files.

Initial forums/community surfaces:

- Reddit: r/headphones, r/oratory1990, relevant model/manufacturer communities, and other public audio communities when searches show structured EQ data
- Head-Fi headphone/model threads and EQ discussions
- Audio Science Review headphone/IEM threads, review discussions, and member-created PEQ posts
- The HEADPHONE Community / forum.headphones.com
- Topping community tuning/sharing ecosystem
- other established public audio forums discovered through the source registry

Discovery should search for high-signal tokens such as `Preamp:`, `Filter 1:`, `ON PK`, `Fc`, `Gain`, `Q`, `parametric EQ`, `PEQ`, and known preset attachment formats rather than scraping whole forums indiscriminately.

Forum/community entries are candidates until parsed, attributed, identity-resolved, validated, deduplicated, and classified. Store the normalized filters, creator username, original URL, timestamps, target claim, and minimal necessary metadata. Do not copy unrelated forum prose into the catalog.

Curated community inputs use the generic `catalog/discovery/*_community_curated.json` shape. The scheduled community publication workflow processes every such file in deterministic sorted order and chains each validated candidate set into the next. Edition XS remains a useful pilot dataset, but the workflow itself must never be Edition-XS-specific or require code changes for each new headphone.

### D. GitHub repositories and Gists

Search public GitHub repositories/Gists for structured preset files and maintained EQ collections, including:

- Equalizer APO / AutoEq `ParametricEQ.txt`-style files
- Peace configurations where filters can be parsed reliably
- JSON/CSV/YAML EQ datasets
- device-specific preset repositories that preserve source attribution
- maintained personal/community EQ collections

Require repository/license review before redistribution. When redistribution is not clearly permitted, store/link provenance and ingest only data that can legally be normalized/published.

Explicitly qualified repositories/files are publication inputs; broad GitHub/Gist discovery remains review-only until originality and licensing are established.

Initial qualified General-EQ repository source: `wabsto1/ParaEQ`. Its built-in preset definitions are MIT licensed and source-authored in `Sources/Models.swift`; EQ Library pins the reviewed commit, republishes only exact structured EQ parameters/labels with attribution, and uses the generic `*_general_presets.json` publication lane. Missing source preamp stays null and generated safety headroom remains separate derived metadata.

### E. Manufacturer/device community ecosystems

Treat device ecosystems as both discovery sources and export-validation references.

Initial candidates:

- Topping community tuning/sharing curves
- Qudelix shared/user presets if a stable public surface and acceptable terms are available
- FiiO/community presets where publicly indexable and structured
- Poweramp/Wavelet/Equalizer APO/Peace communities when they expose original user-created settings rather than mirrors of AutoEq
- RME/device forums for creator/user PEQ presets

Do not duplicate AutoEq simply because another app bundles or mirrors AutoEq results. Attach the mirror as provenance only when useful.

### F. User submissions / forms

Provide a structured low-friction contribution route so useful EQs do not depend on scraping.

Current repository surface:

1. GitHub Issue Form: `Submit an EQ source`

Potential later surfaces:

2. lightweight web form linked from the repository/app
3. in-app `Submit source URL` / `Import from URL`

The GitHub Issue Form uses separate fields for:

- manufacturer
- exact model
- exact variant/revision/pads/mode when materially relevant
- EQ creator/username
- original source URL
- source platform
- source published/updated date if known
- target/curve only if explicitly stated
- exact structured PEQ/preset data or exact preset-file link
- optional submitter-authorship and provenance notes

The Issue Form is intake, not publication. `tools/eq_submission_issue.py` normalizes GitHub issue events into `catalog/submissions/github-issue-<number>.json` with `candidate_state: needs_review` and `publication_eligible: false`.

Form intake rules:

- do not infer manufacturer/model from a legacy combined label;
- do not invent a variant or target;
- parse structured PEQ with the same conservative community parser;
- preserve every parsed filter and preserve a missing source preamp as null;
- stage a single preset URL as `preset_link_needs_fetch` rather than fetching/interpreting it automatically;
- retain invalid/incomplete input with diagnostics for review instead of silently dropping it;
- require the normal source-policy, identity, provenance, acoustic dedupe, and revision pipeline before any later publication.

The issue-event staging workflow writes only to the submission queue and never directly to `catalog/catalog.json`.

### G. User-local imports

Support personal EQs that never need to become public catalog entries.

Potential inputs:

- pasted Equalizer APO / AutoEq text
- local `.txt`, `.xml`, JSON, CSV, or supported device preset files
- pasted public URL
- manual filter editor

Store these as `My EQs`. A user may later explicitly submit provenance for public-catalog consideration. This is a future user-facing feature and remains subject to the project UX approval gate.

### H. Search/discovery fallback

Use targeted web discovery to find new source communities and one-off original presets that are not in known registries. Newly discovered domains remain disabled or `needs_review` until their access/terms/provenance strategy is documented.

## Canonical headphone identity across lanes

Different sources frequently spell the same physical product differently. Identity cleanup is therefore part of ingestion, not a display-only post-process.

Use three classes of decisions:

1. **Auto-safe normalization** — punctuation/spacing/casing or redundant manufacturer tokens where manufacturer/model/subtype make equivalence unambiguous.
2. **Reviewed aliases** — evidence-backed alternate labels for the same physical product, stored in `config/headphone_identity_decisions.json` with a chosen canonical model name and evidence.
3. **Reviewed distinct pairs** — similarly named products/variants with evidence that they must not be merged.

`tools/headphone_identity_audit.py` emits unresolved review candidates. Ambiguous candidates stay unresolved rather than triggering a broad heuristic. The Android browse/managed-state migration layer must follow reviewed canonical aliases so dedupe does not lose saved state.

## Trust / provenance tiers

Use source quality independently from popularity.

- Tier 1: structured authoritative source / original creator / established measurer
- Tier 2: measurement-derived algorithmic source with explicit measurement + target provenance
- Tier 3: traceable community/user tuning with original public source
- Tier 4: repost/mirror where original source is known; attach as secondary provenance only
- Tier 5: ambiguous/unattributed candidate; never auto-publish

Likes, votes, downloads, or forum reputation may be stored as popularity metadata but must never replace provenance quality.

## Deduplication across lanes

One acoustic tuning is shown once even if it appears in OPRA, AutoEq mirrors, forum reposts, GitHub files, device communities, or user submissions.

- resolve canonical headphone identity first
- exact/normalized acoustic fingerprint match -> one canonical revision
- original/authoritative source becomes primary
- mirrors/reposts become `source_references`
- same creator/headphone/target with materially changed acoustic fingerprint -> new revision when lineage indicates an update
- clearly separate named alternatives (for example Neutral vs Bass) remain separate canonical profiles unless the creator explicitly marks one as a replacement

Acoustic dedupe never means device conversion: the canonical profile keeps its source filter count and source data even when an export target can represent fewer bands.

## Revision handling

For public/community sources, retain old genuine acoustic revisions when the source changes.

Store when available:

- source-published timestamp
- source-updated timestamp
- first-seen timestamp
- last-seen/verified timestamp
- creator-provided version label
- acoustic fingerprint
- change summary
- source-removed state

Formatting-only edits do not create a new acoustic revision.

Generated safety headroom is derived metadata and is not a source preamp. Same-fingerprint changes to that derived value update metadata in place. A narrowly proven legacy case where generated safety headroom was previously stored as source preamp may be repaired in place when canonical profile, exact source-reference identity, exact filters, and numeric safety value all establish that the old revision is an application representation bug rather than genuine source history.

## Access and redistribution policy

For every registered source, record:

- discovery method
- structured API/feed availability
- robots/terms constraints
- rate limits
- redistribution status: `allowed`, `structured-data-only`, `link-only`, `review-required/unknown`
- required attribution

Prefer APIs, feeds, public structured endpoints, repository files, and search indexes over brittle HTML scraping. Never scrape authenticated/private content. Never bypass access controls. If terms are uncertain, keep the source in discovery/link-only/review mode until resolved.

## Permanent currentness pathway

Keeping EQ Library current is a permanent operating requirement, not a one-time v0.3 migration task. The ingestion system must maintain three independent update loops so the Android app can stay current without requiring an APK for ordinary catalog/source changes.

### 1. Known-source update loop

Continuously scan active registered sources for:

- newly published EQs
- changed EQ parameters
- creator/source version labels
- target or provenance corrections
- removed or moved source pages
- source-side metadata changes

Use source-specific cursors, timestamps, ETags, release IDs, hashes, or equivalent high-water marks so unchanged content is not repeatedly reprocessed.

Default cadence guidance:

- high-change structured sources and active communities: daily where appropriate
- slower creator pages/forums: weekly where appropriate
- source health probes: at least weekly

A source-specific cadence may be tightened or relaxed based on observed change frequency, rate limits, reliability, and terms.

### 2. Existing-profile revision loop

Every changed candidate must be compared against the latest canonical revision.

- identical acoustic fingerprint: update provenance/last-seen/derived metadata only
- materially changed acoustic fingerprint in the same tuning lineage: create an immutable new revision
- clearly separate alternate tuning: create a separate canonical profile
- source deletion/removal: retain every already-published genuine canonical EQ/revision in the living archive and mark/update source state/provenance rather than erasing acoustic history

Users who pin/favorite an older revision must never be silently moved to a newer revision.

### 3. New-source discovery loop

Periodically search beyond the existing source registry for newly launched or newly useful:

- EQ databases/catalogs
- measurement projects
- creator repositories
- public GitHub/Gist collections
- headphone/IEM forums and communities
- manufacturer/device tuning ecosystems
- public APIs/feeds
- maintained preset projects

Newly discovered sources enter a qualification queue rather than becoming active automatically.

Qualification must determine:

- originality vs mirror/repackaged AutoEq data
- structured parseability
- public accessibility
- licensing/redistribution status
- attribution requirements
- source reliability/stability
- expected update cadence
- likely data quality/provenance tier

Source lifecycle states include:

- `proposed`
- `reviewing`
- `active`
- `limited_link_only`
- `paused`
- `retired`

A source can change states automatically for technical health reasons, but licensing/redistribution changes that require product judgment remain a user stop condition.

## Source health and freshness metadata

The machine-readable source registry/health state must persist at least:

- source ID/type/name
- current URL/scope
- parser/adapter version
- discovery method
- cadence
- last scan attempted
- last successful scan
- last content change detected
- current cursor/high-water mark
- consecutive failure count
- source lifecycle state
- redistribution/attribution status
- last terms/license review date where available
- notes/reason when paused or retired

Catalog publication should expose source freshness internally so stale sources can be diagnosed without deleting otherwise valid EQs.

## Automated failure handling

Ordinary source failures should not require user intervention.

- transient timeout/rate-limit -> retry with backoff
- repeated source failure -> mark degraded/paused while retaining last-known-good catalog data
- parser break due to format change -> quarantine new candidates from that source until parser validation passes
- moved URL -> update registry if confidently resolved
- removed source -> retain archived canonical EQs/revisions, preserve provenance, and mark source removed/retired
- changed terms/license -> stop redistribution for newly affected data until reviewed

No failed source may invalidate the last-known-good canonical catalog.

The current v0.3 repository has scaffolding plus several real currentness lanes, but not every registered forum/community yet has a fully autonomous live scanner. Completion of scheduled adapters, overdue-source enforcement, and monthly discovery of additional sources is intentionally tracked in `docs/FUTURE_SOURCE_AUTOMATION_PLAN.md` rather than hidden as an assumed v0.3 capability. Production source automation runs through GitHub Actions/repository tooling, not ChatGPT or the Android client.

## Catalog publication discipline

Updates are published only after:

1. parse/schema validation
2. canonical headphone identity validation
3. provenance validation
4. acoustic dedupe/revision classification
5. target classification
6. source/license policy checks
7. deterministic catalog generation
8. regression validation against the prior catalog, including a hard living-archive check that previously published canonical profiles/revisions have not disappeared or changed acoustically in place

Publication must be atomic. Android clients continue using the previous last-known-good catalog if a new catalog build fails validation.

Staged user submissions are deliberately outside this publication sequence until reviewed into a qualified source candidate.

## APK independence

Ordinary changes should not require a new Android release. The following should normally be data/pipeline-only updates:

- adding another source that maps to an existing adapter/schema
- adding another curated community headphone input
- discovering new EQs
- adding new community revisions
- resolving a source-side headphone alias through existing schema
- changing provenance links/status
- retiring or pausing a source
- adding mirrors/secondary references

An APK update is required only when the new source/data requires a genuinely new client schema, interaction model, parser executed on-device, or new export/device capability.

## v0.3 implementation priority from here

Already established foundation:

- OPRA and broad AutoEq canonical ingestion
- canonical source-agnostic model and acoustic dedupe/revisions
- source-authentic arbitrary filter/preamp handling
- reviewed headphone-identity audit/alias/distinct-pair pipeline
- creator/oratory provenance lane
- qualified GitHub repository ingestion
- source registry/currentness/health scaffolding
- curated multi-forum Edition XS pilot data
- generalized all-file curated community publisher
- structured GitHub Issue Form intake staged as review-only

Next source-expansion work:

P0: keep CI/currentness green while reducing reviewed headphone-identity duplicates and protecting distinct variants

P1: add qualified community/expert EQ inputs beyond the Edition XS pilot using the generic curated/community pipeline; improve recurring discovery for Reddit/Head-Fi/ASR/HEADPHONE Community without inventing filters from screenshots/curves

P2: progress Squiglink-compatible sources from discovery to publication only where source-specific rights are verified; expand qualified GitHub/Gist sources

P3: connect reviewed form submissions into the normal candidate qualification/publication tooling while preserving the explicit review gate; broaden new-source discovery and health/freshness reporting

P4: complete Android real-path canonical catalog/identity/revision integration and the signed v0.3 validation candidate

The Android app should consume only the validated canonical catalog. Discovery, parsing, terms checks, currentness monitoring, source qualification, submission review, and catalog publication remain outside the Android runtime.