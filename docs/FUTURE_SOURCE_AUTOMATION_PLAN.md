# EQ Library — Future Source Automation Plan

This document records the post-v0.3 source-currentness and discovery work that must not be lost while v0.3.0 is being finished. It is intentionally **not** a blocker for the current v0.3.0 release unless a change is required to protect the living-archive guarantees already locked in `docs/V0.3_RELEASE_POLISH_PLAN.md`.

The goal is to make the canonical EQ archive increasingly self-maintaining through repository tooling and GitHub Actions, without making Android clients scrape forums, without requiring ChatGPT in production, and without requiring an APK release for ordinary catalog/source updates.

## 1. Operating model

Production source maintenance must run outside the Android app:

`public source -> GitHub Actions/source adapter -> parse/validate -> canonical dedupe/revision/archive checks -> catalog publication -> Android last-known-good catalog`

- GitHub Actions provides scheduled/manual execution.
- Repository Python tooling performs source retrieval, parsing, provenance handling, identity resolution, acoustic dedupe/revision classification, archive-preservation validation, and publication.
- The Android app consumes only the validated published canonical catalog and never scrapes forums during normal operation.
- ChatGPT may help develop/maintain adapters and investigate failures, but normal catalog currentness must not depend on ChatGPT being present or running.

## 2. Current coverage gap to close

The source registry already defines intended cadences for structured sources, communities, repositories, and creator/device ecosystems, but not every registered source currently has a fully automated live discovery adapter or a recorded successful scan.

Future work must distinguish clearly between:

- **registered and actively scanned**;
- **registered but curated/manual**;
- **registered and reviewing/qualification-only**;
- **paused/degraded**;
- **retired/unavailable but archived**.

An `active` source must not silently remain with no successful scan history indefinitely.

## 3. Cadence policy

Default cadence targets:

- high-change structured catalogs and active high-volume communities: **daily** where technically appropriate;
- slower creator pages, forums, qualified repositories, and device communities: **weekly** where appropriate;
- source-health probes: **at least weekly**;
- broad search for entirely new sources/communities/repositories/databases: **monthly review** by default.

The source registry remains authoritative for source-specific cadence. A cadence may be tightened or relaxed based on observed change rate, rate limits, reliability, API/feed availability, and source terms.

## 4. Registered forum/community automation

Build or complete source-specific targeted discovery for registered public communities such as:

- Reddit audio communities;
- Head-Fi;
- Audio Science Review;
- The HEADPHONE Community / Headphones.com;
- HiFiGuides;
- qualified device/manufacturer communities such as Topping where structured public EQ data is available.

Do not indiscriminately crawl whole sites. Prefer APIs, public feeds, structured endpoints, search indexes, or narrow high-signal retrieval using markers such as `Preamp:`, `Filter 1:`, `ON PK`, `Fc`, `Gain`, `Q`, `parametric EQ`, `PEQ`, and supported preset attachments.

Never access authenticated/private/restricted content or bypass controls.

## 5. Source health and overdue enforcement

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

- an active source that has never completed a successful scan;
- an active source whose last success is overdue relative to its configured cadence plus a reasonable grace period;
- repeated parser/network failures;
- stale source cursors that are no longer advancing when upstream content changes.

Overdue/failure reporting must never delete or invalidate previously archived EQs.

## 6. New-source discovery loop

At least monthly, search beyond the existing registry for newly useful public sources, including:

- headphone/IEM forums and communities;
- creator/measurer pages and repositories;
- EQ databases/catalogs;
- public GitHub/Gist collections;
- measurement projects;
- device/manufacturer tuning communities;
- maintained preset projects;
- public structured APIs/feeds.

Newly discovered sources enter a qualification queue and do not become publication-active automatically.

Qualification records must cover:

- originality vs mirror/repackaged data;
- structured parseability;
- public accessibility;
- attribution/provenance quality;
- source stability/reliability;
- expected update cadence;
- redistribution/permission status;
- likely provenance tier.

## 7. Living-archive interaction

The future automation must preserve the v0.3 living-archive invariant:

- source disappearance, URL movement, pause, retirement, or crawler failure never deletes a valid canonical EQ/revision;
- moved URLs are updated only when confidently resolved;
- source lifecycle/availability changes are metadata;
- genuine acoustic changes create immutable revisions;
- same acoustic fingerprint updates provenance/last-seen/derived metadata rather than manufacturing a revision;
- mirrors/reposts attach as provenance instead of duplicating an existing tuning;
- previously published genuine canonical profiles/revisions remain represented in the current catalog.

## 8. Publication and failure isolation

Every automated source lane must feed the same deterministic canonical publication pipeline:

`discover -> retrieve -> parse -> provenance -> identity -> acoustic dedupe/revision -> archive preservation -> validate -> publish`

Publication remains atomic. A malformed source, parser regression, temporary outage, or ambiguous candidate must be isolated/quarantined without replacing the last-known-good catalog.

Ordinary source additions and new EQ/revision publication should remain data/pipeline changes and should not require a new Android APK while the client schema remains compatible.

## 9. Review/observability goal

Provide a maintainable source-currentness view from repository data/CI so a maintainer can answer, without manual forensic work:

- Which sources are active?
- When was each source last checked successfully?
- Is any active source overdue?
- What changed on the last successful scan?
- How many candidates were discovered, published, deduplicated, or quarantined?
- Which sources are degraded, paused, or awaiting qualification?
- When was broad new-source discovery last performed?

A later UI/dashboard may be added if useful, but machine-readable repository state and CI summaries come first.

## 10. Acceptance criteria for the future release

The source-automation milestone is complete when:

1. every `active` registered source is either backed by a real scheduled scan adapter or explicitly reclassified to an accurate non-active/manual lifecycle;
2. configured daily/weekly cadences are actually exercised by GitHub Actions or equivalent repository automation;
3. overdue active-source currentness is automatically surfaced;
4. source-health timestamps/cursors are updated from real successful runs;
5. a monthly new-source discovery/qualification process is implemented and recorded;
6. all discovered candidates flow through the same canonical provenance/dedupe/revision/archive validation;
7. source failures cannot erase archived EQs or replace last-known-good publication;
8. normal production operation remains independent of ChatGPT and independent of Android forum scraping;
9. tests cover cadence/overdue behavior, failure isolation, source movement/removal, dedupe/revision behavior, and living-archive preservation.

## 11. v0.3 boundary

For v0.3.0, finish only the source work already required by the current release-polish plan: living-archive preservation, catalog regression protection, and the catalog/app behavior needed by the approved UI changes.

Do **not** delay v0.3.0 merely to build every future forum crawler, health dashboard, or monthly discovery workflow. Those items are retained here for the next source-automation milestone and should be prioritized immediately after the v0.3.0 release is stable.
