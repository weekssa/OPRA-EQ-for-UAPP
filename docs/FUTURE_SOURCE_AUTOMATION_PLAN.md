# EQ Library — Future Source Automation Plan

This document records the source-currentness and discovery work that follows the v0.3 catalog foundation. The living-archive guarantees locked in `docs/V0.3_RELEASE_POLISH_PLAN.md` remain mandatory for every source-automation change.

The goal is to make the canonical EQ archive increasingly self-maintaining through repository tooling and GitHub Actions, without making Android clients scrape forums, without requiring ChatGPT in production, and without requiring an APK release for ordinary catalog/source updates.

## 1. Operating model

Production source maintenance must run outside the Android app:

`public source -> GitHub Actions/source adapter -> parse/validate -> canonical dedupe/revision/archive checks -> catalog publication -> Android last-known-good catalog`

- GitHub Actions provides scheduled/manual execution.
- Repository Python tooling performs source retrieval, parsing, provenance handling, identity resolution, acoustic dedupe/revision classification, archive-preservation validation, and publication.
- The Android app consumes only the validated published canonical catalog and never scrapes forums during normal operation.
- ChatGPT may help develop/maintain adapters and investigate failures, but normal catalog currentness must not depend on ChatGPT being present or running.

## 2. Implementation checkpoint — 2026-09-07

The first post-v0.4 source-expansion milestone is now implemented on the source-automation workstream:

- **Reddit audio communities:** the whole-library targeted adapter is implemented and preserves failure isolation, but live GitHub-hosted anonymous JSON scanning is currently **paused** after every configured `r/headphones` and `r/oratory1990` listing/search probe returned HTTP 403 `Blocked` on 2026-09-06. Existing curated and archived Reddit EQs remain valid and preserved. Do not describe Reddit as autonomously scanned until a compliant public access path is validated.
- **The HEADPHONE Community / Headphones.com:** a reusable public Discourse JSON adapter performs targeted structured-PEQ discovery, preserves username/topic/post provenance, and records source health.
- **HiFiGuides:** the same public Discourse adapter provides scheduled targeted structured-PEQ discovery and source-health tracking.
- **Head-Fi and Audio Science Review:** their already-qualified curated publication lanes remain valid and are explicitly registered with **manual** cadence. They do not yet have a sufficiently robust direct live-site adapter in this milestone. Do not describe them as autonomously scanned until that exists; their curated inputs continue to feed the same generic community publisher.
- **General EQs:** the generic General-preset publication lane now has a second qualified MIT-licensed source (`MilcioSSQ/eq`) with 20 additional source-authored presets. Clearly named genre/style presets are Genre; Podcast/Spoken Word map through the existing Utility presentation; ambiguous labels are deliberately excluded rather than guessed.
- **General-source currentness:** qualified GitHub-backed General sources have a scheduled blob/source-health probe. Upstream file changes are surfaced as `changed_needs_review`; a source-code change never silently rewrites a qualified General manifest or existing canonical acoustic history.
- **General-source discovery:** scheduled GitHub code discovery now maintains a separate review-only General-EQ candidate queue. Discovery is intentionally broader than publication; candidates remain blocked on originality, license/redistribution, creator attribution, explicit General EQ intent/category, structured parseability, and canonical dedupe.
- **Failure isolation:** forum/API degradation preserves the current candidate/catalog and records source-health failure state. Every changed catalog candidate still passes atomic validation and the living-archive baseline check before publication.

This moves community expansion beyond the original launch queue and expands General EQ population without changing Android runtime behavior or requiring users to connect accounts. Public Discourse and qualified General-source automation are live; Reddit remains safely paused until compliant public access is available.

## 3. Remaining coverage gap to close

The source registry defines intended cadences for structured sources, communities, repositories, and creator/device ecosystems, but some registered sources still need a fully automated live discovery adapter or an explicitly accurate curated/manual lifecycle.

Future work must distinguish clearly between:

- **registered and actively scanned**;
- **registered but curated/manual**;
- **registered and reviewing/qualification-only**;
- **paused/degraded**;
- **retired/unavailable but archived**.

An `active` scheduled source must not silently remain with no successful scan history indefinitely. The next forum-adapter priorities are a compliant public Reddit access path and robust live adapters for Head-Fi and Audio Science Review, using only stable public access paths that do not require authentication, bypass controls, or brittle whole-site crawling.

## 4. Cadence policy

Default cadence targets:

- high-change structured catalogs and active high-volume communities: **daily** where technically appropriate;
- slower creator pages, forums, qualified repositories, and device communities: **weekly** where appropriate;
- source-health probes: **at least weekly**;
- broad search for entirely new sources/communities/repositories/databases: **monthly review** by default, with narrower review-only discovery allowed more frequently when rate limits and source terms permit.

The source registry remains authoritative for source-specific cadence. A cadence may be tightened or relaxed based on observed change rate, rate limits, reliability, API/feed availability, and source terms.

## 5. Registered forum/community automation

Build or complete source-specific targeted discovery for registered public communities such as:

- Reddit audio communities;
- Head-Fi;
- Audio Science Review;
- The HEADPHONE Community / Headphones.com;
- HiFiGuides;
- qualified device/manufacturer communities such as Topping where structured public EQ data is available.

Do not indiscriminately crawl whole sites. Prefer APIs, public feeds, structured endpoints, search indexes, or narrow high-signal retrieval using markers such as `Preamp:`, `Filter 1:`, `ON PK`, `Fc`, `Gain`, `Q`, `parametric EQ`, `PEQ`, and supported preset attachments.

Never access authenticated/private/restricted content or bypass controls.

## 6. Source health and overdue enforcement

Maintain machine-readable health/currentness for every registered source, including at least:

- source ID and lifecycle;
- configured cadence;
- parser/adapter version;
- last scan attempted;
- last successful scan;
- last content change detected;
- cursor/high-water mark/ETag/content hash as appropriate;
- candidates discovered/published/held;
- consecutive failure count and last error;
- last source-policy/terms review where applicable.

Add deterministic validation/reporting that flags:

- an active scheduled source that has never completed a successful scan;
- an active source whose last success is overdue relative to its configured cadence plus a reasonable grace period;
- repeated parser/network failures;
- stale source cursors that are no longer advancing when upstream content changes.

Overdue/failure reporting must never delete or invalidate previously archived EQs.

## 7. New-source discovery loop

At least monthly, search beyond the existing registry for newly useful public sources, including:

- headphone/IEM forums and communities;
- creator/measurer pages and repositories;
- EQ databases/catalogs;
- public GitHub/Gist collections;
- measurement projects;
- device/manufacturer tuning communities;
- maintained preset projects;
- public structured APIs/feeds;
- source-authored General EQ/effect/utility/genre preset collections.

Newly discovered sources enter a qualification queue and do not become publication-active automatically.

Qualification records must cover:

- originality vs mirror/repackaged data;
- structured parseability;
- public accessibility;
- attribution/provenance quality;
- source stability/reliability;
- expected update cadence;
- redistribution/permission status;
- likely provenance tier;
- for General EQs, explicit source intent/category rather than category inferred from filter shape.

## 8. Living-archive interaction

All automation must preserve the living-archive invariant:

- source disappearance, URL movement, pause, retirement, or crawler failure never deletes a valid canonical EQ/revision;
- moved URLs are updated only when confidently resolved;
- source lifecycle/availability changes are metadata;
- genuine acoustic changes create immutable revisions;
- same acoustic fingerprint updates provenance/last-seen/derived metadata rather than manufacturing a revision;
- mirrors/reposts attach as provenance instead of duplicating an existing tuning;
- previously published genuine canonical profiles/revisions remain represented in the current catalog.

## 9. Publication and failure isolation

Every automated source lane must feed the same deterministic canonical publication pipeline:

`discover -> retrieve -> parse -> provenance -> identity -> acoustic dedupe/revision -> archive preservation -> validate -> publish`

Publication remains atomic. A malformed source, parser regression, temporary outage, or ambiguous candidate must be isolated/quarantined without replacing the last-known-good catalog.

Ordinary source additions and new EQ/revision publication should remain data/pipeline changes and should not require a new Android APK while the client schema remains compatible.

## 10. Review/observability goal

Provide a maintainable source-currentness view from repository data/CI so a maintainer can answer, without manual forensic work:

- Which sources are active?
- When was each source last checked successfully?
- Is any active source overdue?
- What changed on the last successful scan?
- How many candidates were discovered, published, deduplicated, or quarantined?
- Which sources are degraded, curated/manual, paused, or awaiting qualification?
- When was broad new-source discovery last performed?
- Which General EQ source files changed and are waiting for requalification?

A later UI/dashboard may be added if useful, but machine-readable repository state and CI summaries come first.

## 11. Acceptance criteria for the source-automation milestone

The milestone is complete when:

1. every `active` registered source is either backed by a real scheduled scan adapter or explicitly reclassified to an accurate non-active/manual lifecycle;
2. configured daily/weekly cadences are actually exercised by GitHub Actions or equivalent repository automation;
3. overdue active-source currentness is automatically surfaced;
4. source-health timestamps/cursors are updated from real successful runs;
5. a recurring new-source discovery/qualification process covers both headphone-specific and General EQ sources;
6. all discovered candidates flow through the same canonical provenance/dedupe/revision/archive validation;
7. source failures cannot erase archived EQs or replace last-known-good publication;
8. normal production operation remains independent of ChatGPT and independent of Android forum scraping;
9. tests cover cadence/overdue behavior, failure isolation, source movement/removal, dedupe/revision behavior, General source change review gating, and living-archive preservation;
10. Reddit live scanning is either restored through a compliant public access path or remains explicitly paused, and Head-Fi / Audio Science Review are either backed by robust live adapters or explicitly represented as curated/manual rather than silently appearing fully automated.

## 12. Release boundary

Source automation is repository/catalog infrastructure. It must not be coupled to an APK release when the canonical client schema remains compatible. New qualified sources, new community EQs, new General presets, source-health changes, and ordinary immutable revisions should publish through the catalog pipeline independently of Android application releases.
