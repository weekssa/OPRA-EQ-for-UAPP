# EQ Library Source Ingestion Strategy

This document defines how EQ Library discovers, qualifies, normalizes, deduplicates, archives, and publishes source-authentic EQ data. `docs/CHATGPT_PROJECT_RUNBOOK.md` and later explicit decisions are authoritative where older planning text differs. Current source automation ownership and closeout status are maintained in `docs/FUTURE_SOURCE_AUTOMATION_PLAN.md`.

The goal is broad coverage without flattening provenance quality. EQ Library ingests source-authentic EQ parameters and minimal source metadata; it does not republish unrelated third-party prose and does not rewrite canonical data to fit an output device.

## 1. Core ingestion rules

These rules apply to every source lane:

- Resolve headphone identity before presenting or publishing duplicate physical-model rows.
- Preserve source preamp exactly; when omitted, canonical `preamp_gain_db` remains null.
- Store EQ Library-generated playback safety headroom separately as derived metadata.
- Preserve arbitrary supported source filter counts, order/priority, type, frequency, gain, and Q. Never truncate canonical ingestion to a device band limit.
- Never invent filters, target claims, variants, authorship, provenance, General-EQ intent, or missing Q/filter types.
- Ambiguous identity/provenance/rights candidates are quarantined rather than guessed.
- Mirrors/reposts become secondary provenance when they carry an already-canonical acoustic tuning.
- Genuine changed tunings become immutable revisions; formatting/application-modeling corrections must not manufacture fake acoustic history.
- Once a genuine canonical EQ/revision is validly published, retain it in the current living archive even if the source later moves, disappears, pauses, or retires.
- Source failures never erase archived EQs and never replace last-known-good publication with a partial candidate.

## 2. Ingestion lanes

### A. Structured canonical/algorithmic catalogs

Examples:

- OPRA runtime catalog;
- AutoEq structured results/targets with measurement-source provenance;
- future machine-readable EQ databases with acceptable access/provenance.

Structured sources may feed automatic validation, identity normalization, acoustic dedupe, revision detection, and publication when provenance is clear.

Do not infer that an AutoEq result derived from a creator's measurement is the creator's authored EQ. Measurement provenance and EQ authorship are separate facts.

### B. Established creator/measurer sources

Preserve original creator presets independently from measurement-derived/algorithmic tunings. Direct creator provenance may be retained even when exact structured coefficients arrive through a separate qualified carrier such as OPRA.

### C. Public forums and communities

Qualified public-community surfaces include Head-Fi, Audio Science Review, The HEADPHONE Community / Headphones.com, HiFiGuides, public GitHub/Gist community collections, and other qualified sources added over time. Reddit remains a recognized source class but is paused until a compliant access path exists; Topping Community is paused until an authorized automated retrieval path exists.

Use targeted high-signal discovery rather than indiscriminate crawling. Useful markers include:

- `Preamp:`
- `Filter 1:`
- `ON PK`
- `Fc`
- `Gain`
- `Q`
- `parametric EQ`
- `PEQ`
- supported structured preset attachments/files.

A community record is a candidate until exact parsing, attribution, identity resolution, validation, acoustic dedupe/revision classification, source-policy checks, and living-archive validation complete. Store normalized coefficients, creator/username when available, original URL, dates/IDs where useful, and minimal tuning context. Do not copy unrelated forum prose.

A mechanically valid, source-traceable candidate from an already-qualified community lane may publish automatically as **Unverified**. Individual exact PEQ files/posts do not require a separate human approval merely because discovery found them. Unsafe candidates are quarantined individually so they do not block unrelated valid records.

### D. GitHub repositories and Gists

Search qualified public GitHub/Gist lanes for structured preset files such as:

- Equalizer APO / AutoEq `ParametricEQ.txt`-style files;
- reliably parseable Peace/device presets;
- JSON/CSV/YAML datasets that preserve exact PEQ structure;
- maintained personal/community EQ collections.

Preserve repository/file provenance and immutable commit/blob identity where possible. A discovered headphone PEQ may publish Unverified after exact parsing, safe identity resolution, creator/source provenance, acoustic dedupe/revision classification, source-policy checks, and living-archive validation. Exact duplicates attach provenance instead of creating another tuning.

A genuinely new repository ecosystem/domain or unsupported adapter still enters source-level qualification before becoming an active scheduled source.

### E. Squiglink-compatible measurement ecosystems

Squiglink/CrinGraph-compatible phone-book/frequency-response data is measurement/provenance data, not independently published source-authored PEQ. EQ Library may monitor the public ecosystem registry automatically, but it must not silently choose a target, run browser-generated AutoEQ, or manufacture canonical source-authored filters from curves.

Exact PEQ derived from Squig measurements may enter through separately qualified automated carriers such as AutoEq with the measurement source retained as provenance.

### F. Manufacturer/device community ecosystems

Treat device ecosystems as discovery sources only when exact source-authored PEQ and acceptable access/provenance exist. Do not duplicate AutoEq merely because another app/device mirrors it; attach mirror provenance when useful.

Measurement curves, screenshots, graphic-EQ-only data, or source material missing exact frequency/gain/Q/filter-type information must not be converted into invented parametric filters.

Topping Community remains paused for autonomous retrieval until TOPPING provides an authorized public API/feed or explicit permission for the required automation. Existing archived data remains preserved.

### G. General EQ sources

General EQ publication requires exact parametric structure plus explicit source-authored General intent/category. Never infer Sound/Genre/Utility from filter shape.

Qualified General sources such as ParaEQ and MilcioSSQ retain reviewed exact source structure. Scheduled upstream probes detect changes; a changed source file is review-gated where classification/qualification may have changed and never silently rewrites archived acoustics.

### H. User submissions/forms

The repository `Submit an EQ source` Issue Form remains an optional low-friction contribution route, not the primary population strategy.

Submission intake should preserve:

- manufacturer;
- exact model;
- materially relevant stated variant/revision/pads/mode;
- creator/username when known;
- original source URL/platform;
- published/updated date if known;
- target/curve only if explicitly stated;
- exact structured PEQ/preset data or exact preset-file link;
- optional authorship/provenance notes.

Issue-event staging must not publish directly. Incomplete/invalid input is retained with diagnostics rather than silently dropped. A submission only publishes after the normal source-policy, identity, provenance, acoustic dedupe/revision, and archive-validation pipeline.

### I. User-local imports

Personal user imports are local canonical ingestion and need not become public catalog records. The current Android import supports explicit pasted/chosen-file Equalizer APO / AutoEq text with strict validation. Future user-facing import formats remain subject to the UX approval gate.

## 3. Canonical headphone identity

Different sources frequently spell the same physical product differently. Identity cleanup is part of ingestion.

Use three classes of decisions:

1. **Auto-safe normalization** — punctuation/spacing/casing or redundant manufacturer tokens where equivalence is unambiguous.
2. **Reviewed aliases** — evidence-backed alternate labels for the same physical product, stored in the maintained identity-decision data.
3. **Reviewed distinct pairs/configurations** — similarly named products, nozzles, pads, revisions, ANC/acoustic modes, or other variants proven distinct.

If a source explicitly states a materially relevant configuration, preserve it. If it does not, use the safe base/generic model rather than inventing a configuration. Ambiguous candidates remain unresolved/quarantined rather than triggering a broad heuristic.

Saved-state migration follows reviewed canonical aliases so identity improvements do not lose user selections.

## 4. Trust/provenance tiers

Use source quality independently from popularity:

- Tier 1: structured authoritative source / original creator / established measurer;
- Tier 2: measurement-derived algorithmic source with explicit measurement + target provenance;
- Tier 3: traceable community/user tuning with original public source;
- Tier 4: repost/mirror where the original is known; attach as secondary provenance;
- Tier 5: ambiguous/unattributed candidate; never auto-publish.

Likes, votes, downloads, or forum reputation may be metadata but never replace provenance quality.

## 5. Acoustic deduplication and revisions

One acoustic tuning is shown once even if it appears in OPRA, AutoEq mirrors, forums, GitHub files, device communities, or submissions.

Process:

1. resolve canonical headphone identity;
2. compare normalized acoustic fingerprint;
3. exact fingerprint match -> one canonical revision with merged provenance;
4. original/authoritative source becomes primary where known;
5. same lineage with materially changed fingerprint -> immutable new revision;
6. clearly separate named alternatives remain separate profiles unless the creator explicitly identifies replacement lineage.

Acoustic dedupe never means device conversion. Canonical data retains the full source filter set even if a target output can represent fewer bands.

Store when available: source-published/updated timestamps, first/last-seen timestamps, creator version labels, acoustic fingerprint, change summary, and source-removed state.

Formatting-only changes do not create revisions. Generated safety headroom is derived metadata and is never silently reclassified as source preamp.

## 6. Access and redistribution policy

For every registered source, record as applicable:

- discovery/retrieval method;
- structured API/feed availability;
- robots/terms/access constraints;
- rate limits/cadence considerations;
- redistribution status;
- required attribution;
- last terms/license review where useful.

Prefer APIs, public feeds, structured endpoints, repository files, search indexes, and bounded public retrieval over brittle HTML crawling.

Never access authenticated/private/restricted content, bypass controls, rotate/proxy around blocks, or automate a source contrary to its published access restrictions. If safe automation is not available, pause the source rather than creating a recurring manual-currentness dependency.

## 7. Permanent currentness pathway

Keeping EQ Library current is permanent operating infrastructure, not a one-time migration.

### Known-source update loop

Scheduled/runtime sources check for new EQs, changed parameters, provenance corrections, moved/removed source pages, and source-side metadata changes using appropriate cursors, timestamps, ETags, release IDs, hashes, or other high-water marks.

Default cadence guidance:

- high-change structured/community sources: daily where appropriate;
- slower repositories/forums/ecosystems: weekly where appropriate;
- source-health audit: daily;
- broad new-source discovery: approximately monthly unless a narrower cadence is justified.

### Existing-profile revision loop

Every changed candidate is compared with the latest canonical revision:

- identical acoustic fingerprint -> provenance/last-seen/derived metadata update only;
- materially changed same lineage -> immutable revision;
- clearly separate alternate tuning -> separate canonical profile;
- source deletion/removal -> preserve archived canonical EQ/revisions and update source state/provenance.

Users pinned to older revisions are never silently moved.

### New-source discovery loop

Periodically search beyond the registry for new databases, measurement projects, creator repositories, public GitHub/Gist collections, forums, device ecosystems, APIs/feeds, and maintained preset projects.

A genuinely new source/lane enters source-level qualification for originality, structured parseability, public access, attribution/provenance, stability, expected cadence, any specific redistribution restriction, and likely provenance tier. Once the lane is qualified, exact candidates can flow automatically through deterministic publication/quarantine rules.

## 8. Automation-first currentness ownership

The final post-v0.4 model has no recurring manual-currentness owner.

A registered source is maintained as one of:

- **scheduled** — repository/GitHub Actions adapter owns currentness and source-health SLA;
- **runtime** — Android owns currentness (currently OPRA);
- **paused** — automated access is intentionally unavailable/disabled while archive data remains preserved;
- **retired** — source is no longer an active acquisition surface but archive provenance remains.

One-off human review may still occur for quarantine, source qualification, changed terms, or ambiguous identity/provenance. That is exception handling, not recurring source maintenance.

Current scheduled/runtime/paused source identities and exact reasons are maintained in `docs/FUTURE_SOURCE_AUTOMATION_PLAN.md` and `config/source_registry.json`.

## 9. Source health and failure handling

Machine-readable source health persists, as applicable:

- source ID/lifecycle/currentness mode;
- parser/adapter version;
- cadence;
- last scan attempt/success;
- content fingerprint/cursor/high-water mark;
- consecutive failure count/last error;
- last source-policy review.

Scheduled sources are unhealthy when they have never succeeded, are more than two cadence intervals overdue, or reach three consecutive failures. Source-specific cursor staleness may also fail when upstream change is provable without cursor advancement.

Ordinary failures should not require owner intervention:

- transient timeout/rate-limit -> retry/backoff;
- repeated failure -> surface degraded/paused state while retaining last-known-good data;
- parser break -> quarantine new candidates until repaired;
- moved URL -> update only when confidently resolved;
- removed source -> retain archived canonical EQs/revisions and provenance;
- changed terms/license -> stop newly affected acquisition/redistribution until reviewed.

No failed source may invalidate the last-known-good canonical catalog.

## 10. Catalog publication discipline

Every publication candidate passes:

1. parse/schema validation;
2. canonical headphone/General identity validation;
3. provenance validation;
4. acoustic dedupe/revision classification;
5. target/intent classification where applicable;
6. source-policy checks;
7. deterministic catalog generation;
8. hard living-archive regression validation against the prior published catalog.

Publication is atomic. Android continues using the previous last-known-good catalog if a candidate build fails.

## 11. APK independence

Ordinary source/catalog changes do not require an Android release while the client schema stays compatible. Data/pipeline-only changes include new qualified sources, new community EQs, new immutable revisions, identity aliases supported by the existing schema, provenance/status changes, source pause/retirement, and mirror/reference additions.

An APK is required only when source/data changes demand a genuinely new client schema, on-device parser/interaction model, or output/device capability.

## 12. Automation closeout and ongoing maintenance

The source-expansion project is no longer defined by converting every provider into a crawler at any cost. It is complete when every registered source has accurate automated/runtime/paused ownership, scheduled sources have real adapters and health, there are zero recurring manual-currentness dependencies, publication/archive gates are green, and a post-merge `main` run successfully publishes the validated catalog to `catalog-live`.

After closeout, source work becomes normal maintenance:

- keep source-health/CI green;
- repair adapters when public formats change;
- resolve safe identity improvements without over-merging variants;
- qualify newly discovered source classes;
- process exact community candidates automatically through publish/dedupe/quarantine;
- preserve source-authentic data and the living archive;
- resume paused Reddit/TOPPING-related lanes only when a compliant/authorized automation path exists.

The Android app consumes only the validated canonical catalog. Discovery, source qualification, access/terms checks, repository-side parsing, currentness monitoring, and catalog publication remain outside normal Android runtime except for the explicitly runtime-managed OPRA refresh.
