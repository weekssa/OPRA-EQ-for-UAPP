# EQ Library v0.3 Source Ingestion Strategy

This document expands the source-adapter section of `AUTONOMOUS_V0.3_PLAN.md`. The goal is broad coverage without flattening provenance quality. EQ Library should ingest normalized EQ parameters and source metadata, not republish third-party prose.

## Ingestion lanes

### A. Structured canonical catalogs

Highest-priority machine-readable or consistently structured sources.

- OPRA runtime catalog
- AutoEq structured measurements/results/targets
- Squiglink/Squiglink-compatible public structured data where terms allow
- future structured EQ databases with explicit usable licensing/terms

These sources can feed automatic validation, normalization, deduplication, revision detection, and publication when provenance is clear.

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

Forum/community entries are candidates until parsed, attributed, validated, deduplicated, and classified. Store the normalized filters, creator username, original URL, timestamps, target claim, and minimal necessary metadata. Do not copy forum prose into the catalog.

### D. GitHub repositories and Gists

Search public GitHub repositories/Gists for structured preset files and maintained EQ collections, including:

- Equalizer APO / AutoEq `ParametricEQ.txt`-style files
- Peace configurations where filters can be parsed reliably
- JSON/CSV/YAML EQ datasets
- device-specific preset repositories that preserve source attribution
- maintained personal/community EQ collections

Require repository/license review before redistribution. When redistribution is not clearly permitted, store/link provenance and ingest only data that can legally be normalized/published.

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

Recommended surfaces:

1. GitHub Issue Form: `Submit an EQ source`
2. Future lightweight web form linked from the repository/app
3. Future in-app `Submit source URL` / `Import from URL`

Submission fields should include:

- headphone manufacturer/model
- exact variant/revision/pads/mode when relevant
- EQ creator/username
- original source URL
- source platform
- source published/updated date if known
- target/curve if explicitly stated
- preamp
- filters or attached structured preset
- optional notes describing whether the submitter is the creator

Submissions enter the same candidate pipeline and do not publish directly.

### G. User-local imports

Support personal EQs that never need to become public catalog entries.

Potential inputs:

- pasted Equalizer APO / AutoEq text
- local `.txt`, `.xml`, JSON, CSV, or supported device preset files
- pasted public URL
- manual filter editor

Store these as `My EQs`. A user may later explicitly submit provenance for public-catalog consideration.

### H. Search/discovery fallback

Use targeted web discovery to find new source communities and one-off original presets that are not in known registries. Newly discovered domains remain disabled or `needs_review` until their access/terms/provenance strategy is documented.

## Trust / provenance tiers

Use source quality independently from popularity.

- Tier 1: structured authoritative source / original creator / established measurer
- Tier 2: measurement-derived algorithmic source with explicit measurement + target provenance
- Tier 3: traceable community/user tuning with original public source
- Tier 4: repost/mirror where original source is known; attach as secondary provenance only
- Tier 5: ambiguous/unattributed candidate; never auto-publish

Likes, votes, downloads, or forum reputation may be stored as popularity metadata but must never replace provenance quality.

## Deduplication across lanes

One acoustic tuning is shown once even if it appears in OPRA, AutoEq mirrors, forum reposts, GitHub files, and device communities.

- exact/normalized acoustic fingerprint match -> one canonical revision
- original/authoritative source becomes primary
- mirrors/reposts become `source_references`
- same creator/headphone/target with materially changed acoustic fingerprint -> new revision when lineage indicates an update
- clearly separate named alternatives (for example Neutral vs Bass) remain separate canonical profiles unless the creator explicitly marks one as a replacement

## Revision handling

For public/community sources, retain old acoustic revisions when the source changes.

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

## Access and redistribution policy

For every registered source, record:

- discovery method
- structured API/feed availability
- robots/terms constraints
- rate limits
- redistribution status: `allowed`, `structured-data-only`, `link-only`, `unknown/review`
- required attribution

Prefer APIs, feeds, public structured endpoints, repository files, and search indexes over brittle HTML scraping. Never scrape authenticated/private content. Never bypass access controls. If terms are uncertain, keep the source in discovery/link-only mode until resolved.

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

- identical acoustic fingerprint: update provenance/last-seen metadata only
- materially changed acoustic fingerprint in the same tuning lineage: create an immutable new revision
- clearly separate alternate tuning: create a separate canonical profile
- source deletion/removal: preserve already-valid historical records where legally appropriate and mark source state rather than silently erasing the EQ

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

Source lifecycle states:

- `proposed`
- `reviewing`
- `active`
- `limited_link_only`
- `paused`
- `retired`

A source can change states automatically for technical health reasons, but licensing/redistribution changes that require product judgment remain a user stop condition.

## Source health and freshness metadata

The machine-readable source registry must persist at least:

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
- last terms/license review date
- notes/reason when paused or retired

Catalog publication should expose source freshness internally so stale sources can be diagnosed without deleting otherwise valid EQs.

## Automated failure handling

Ordinary source failures should not require user intervention.

- transient timeout/rate-limit -> retry with backoff
- repeated source failure -> mark degraded/paused while retaining last-known-good catalog data
- parser break due to format change -> quarantine new candidates from that source until parser validation passes
- moved URL -> update registry if confidently resolved
- removed source -> preserve provenance/history where appropriate and mark source removed/retired
- changed terms/license -> stop redistribution for newly affected data until reviewed

No failed source may invalidate the last-known-good canonical catalog.

## Catalog publication discipline

Updates are published only after:

1. parse/schema validation
2. headphone identity validation
3. provenance validation
4. dedupe/revision classification
5. target classification
6. source/license policy checks
7. deterministic catalog generation
8. regression validation against the prior catalog

Publication must be atomic. Android clients continue using the previous last-known-good catalog if a new catalog build fails validation.

## APK independence

Ordinary changes should not require a new Android release. The following should normally be data/pipeline-only updates:

- adding another source that maps to an existing adapter/schema
- discovering new EQs
- adding new community revisions
- changing provenance links/status
- retiring or pausing a source
- adding mirrors/secondary references

An APK update is required only when the new source/data requires a genuinely new client schema, interaction model, parser executed on-device, or new export/device capability.

## Initial v0.3 priority

P0: OPRA, AutoEq, canonical data model, dedupe/revisions

P1: oratory1990 as a distinct creator source, Squiglink-compatible structured data where allowed, GitHub/Gist parser

P2: Reddit, Head-Fi, Audio Science Review, HEADPHONE Community, Topping Community candidate discovery

P3: public user submission form / GitHub Issue Form, additional device/app communities, new source discovery

P4: source-health dashboard data, automated source qualification scaffolding, permanent post-v0.3 currentness jobs, and documented maintenance/recovery procedures

The Android app should consume only the validated canonical catalog. Discovery, parsing, terms checks, currentness monitoring, source qualification, and catalog publication remain outside the Android runtime.