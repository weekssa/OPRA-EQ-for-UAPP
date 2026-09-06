# EQ Library — Future Source Automation Plan

This document records the source-currentness and discovery work that follows the v0.3 catalog foundation. The living-archive guarantees locked in `docs/V0.3_RELEASE_POLISH_PLAN.md` remain mandatory for every source-automation change.

The goal is to make the canonical EQ archive increasingly self-maintaining through repository tooling and GitHub Actions, without making Android clients scrape forums, without requiring ChatGPT in production, and without requiring an APK release for ordinary catalog/source updates.

## 1. Operating model

Production source maintenance uses the path appropriate to each source:

`public source -> source adapter/runtime refresh -> parse/validate -> canonical dedupe/revision/archive checks -> catalog/local cache`

- GitHub Actions provides scheduled repository-side source discovery/publication where a stable public source adapter exists.
- Repository Python tooling performs source retrieval, parsing, provenance handling, identity resolution, acoustic dedupe/revision classification, archive-preservation validation, source-health reporting, and publication.
- OPRA remains a special runtime-managed source: the Android app directly checks the official `database_v1.jsonl` feed on its approximately-daily refresh path and keeps its own last-known-good OPRA cache. Repository source-health timestamps therefore do not pretend to represent each user's OPRA refresh.
- The Android app does not scrape forums during normal operation. Canonical multi-source additions beyond its direct OPRA compatibility/base feed arrive through the validated published EQ Library catalog.
- ChatGPT may help develop/maintain adapters and investigate failures, but normal catalog currentness must not depend on ChatGPT being present or running.

## 2. Implementation checkpoint — 2026-09-07

The first post-v0.4 source-expansion/currentness milestone now contains:

- **Reddit audio communities:** the whole-library targeted adapter is implemented and preserves failure isolation, but live GitHub-hosted anonymous JSON scanning is **paused** after every configured `r/headphones` and `r/oratory1990` listing/search probe returned HTTP 403 `Blocked` on 2026-09-06. Existing curated and archived Reddit EQs remain valid and preserved. Do not describe Reddit as autonomously scanned until a compliant public access path is validated.
- **oratory1990 direct update discovery:** direct creator provenance remains link-only/manual while anonymous Reddit access is blocked. The parser remains available for explicit/local listing inputs, but repository automation does not attempt the blocked Reddit JSON path.
- **The HEADPHONE Community / Headphones.com:** a reusable public Discourse JSON adapter performs targeted structured-PEQ discovery, preserves username/topic/post provenance, and records source health.
- **HiFiGuides:** the same public Discourse adapter provides scheduled targeted structured-PEQ discovery and source-health tracking.
- **Head-Fi and Audio Science Review:** their already-qualified curated publication lanes remain valid and are explicitly registered as **manual** currentness. They do not yet have sufficiently robust direct live-site adapters; curated inputs continue to feed the same generic community publisher.
- **General EQs:** the generic General-preset publication lane has a second qualified MIT-licensed source (`MilcioSSQ/eq`) with 20 additional source-authored presets. Clearly named genre/style presets are Genre; Podcast/Spoken Word map through the existing Utility presentation; ambiguous labels are deliberately excluded rather than guessed.
- **General-source currentness:** qualified GitHub-backed General sources have a scheduled blob/source-health probe. Upstream file changes are surfaced as `changed_needs_review`; a source-code change never silently rewrites a qualified General manifest or existing canonical acoustic history.
- **General-source discovery:** scheduled GitHub code discovery maintains a separate review-only General-EQ candidate queue. Discovery is intentionally broader than publication; candidates remain blocked on originality, license/redistribution, creator attribution, explicit General EQ intent/category, structured parseability, and canonical dedupe.
- **Explicit currentness ownership:** the source registry distinguishes `scheduled`, `runtime`, `manual`, `review`, and `paused` currentness modes so a source cannot appear overdue merely because it is intentionally maintained outside repository scheduling.
- **Strict source-health SLA:** `Source health currentness` runs on relevant pull requests/pushes and daily after the established catalog/source-expansion jobs. A genuinely scheduled source blocks the audit when it has never recorded a successful scan, when its last success exceeds two configured cadence intervals, or when it reaches three consecutive failures. One or two recent failures within the freshness window are warnings rather than false catalog failures.
- **Failure isolation:** forum/API degradation preserves the current candidate/catalog and records source-health failure state. Every changed catalog candidate still passes atomic validation and the living-archive baseline check before publication.

This expands community/General coverage and makes source health enforceable without changing Android UX, conversion, export, or Black Pearl DSP behavior. Ordinary source/catalog changes remain independent of APK releases.

## 3. Currentness ownership model and remaining expansion

Each registry source has one currentness owner:

- **scheduled** — a repository adapter is responsible for the configured cadence and is subject to the strict source-health SLA;
- **runtime** — currentness is performed by the Android runtime rather than the repository health ledger (currently OPRA);
- **manual** — curated/manual publication or provenance review; no scheduled scan success is claimed;
- **review** — discovery/qualification-only lane that cannot publish merely because a search ran;
- **paused** — live access is intentionally disabled while already-published archive data remains preserved.

The next adapter-expansion priorities remain a compliant public Reddit access path and robust live adapters for Head-Fi and Audio Science Review. Topping/Squiglink and other review-only sources may advance only when stable public access, provenance, rights, and exact structured data are qualified. These are future coverage improvements, not hidden claims that those sources are already autonomously scanned.

## 4. Cadence policy

Default cadence targets:

- high-change structured catalogs and active high-volume communities: **daily** where technically appropriate;
- slower creator pages, forums, qualified repositories, and device communities: **weekly** where appropriate;
- source-health audit: **daily**, after the normal source-refresh windows;
- broad search for entirely new sources/communities/repositories/databases: **monthly review** by default, with narrower review-only discovery allowed more frequently when rate limits and source terms permit.

The source registry remains authoritative for source-specific cadence and currentness mode. A cadence may be tightened or relaxed based on observed change rate, rate limits, reliability, API/feed availability, and source terms.

## 5. Registered forum/community automation

Build or complete source-specific targeted discovery for registered public communities such as:

- Reddit audio communities when a compliant public path becomes available;
- Head-Fi;
- Audio Science Review;
- The HEADPHONE Community / Headphones.com;
- HiFiGuides;
- qualified device/manufacturer communities such as Topping where structured public EQ data is available.

Do not indiscriminately crawl whole sites. Prefer APIs, public feeds, structured endpoints, search indexes, or narrow high-signal retrieval using markers such as `Preamp:`, `Filter 1:`, `ON PK`, `Fc`, `Gain`, `Q`, `parametric EQ`, `PEQ`, and supported preset attachments.

Never access authenticated/private/restricted content or bypass controls.

## 6. Source health and overdue enforcement

Maintain machine-readable health/currentness for registered sources, including as applicable:

- source ID, lifecycle, and currentness mode;
- configured cadence;
- parser/adapter version;
- last scan attempted;
- last successful scan;
- content fingerprint/cursor/high-water mark/ETag as appropriate;
- consecutive failure count and last error;
- last source-policy/terms review where applicable.

For `scheduled` sources, deterministic validation must surface:

- a source that has never completed a successful scan;
- a source whose last success is overdue beyond the configured cadence plus the documented grace period;
- repeated parser/network failures;
- stale source cursors when a source-specific adapter can prove upstream content changed without cursor advancement.

The strict repository audit currently uses two cadence intervals as the freshness grace and three consecutive failures as the blocking repeated-failure threshold. These values are operational policy rather than permission to delete data. Overdue/failure reporting must never delete or invalidate previously archived EQs.

`runtime`, `manual`, `review`, and `paused` modes are reported accurately but are not forced through a fake repository scan-success SLA.

## 7. New-source discovery loop

At least monthly, review beyond the existing registry for newly useful public sources, including:

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

Every automated publication lane feeds the same deterministic canonical pipeline:

`discover -> retrieve -> parse -> provenance -> identity -> acoustic dedupe/revision -> archive preservation -> validate -> publish`

Publication remains atomic. A malformed source, parser regression, temporary outage, or ambiguous candidate must be isolated/quarantined without replacing the last-known-good catalog.

Ordinary source additions and new EQ/revision publication remain data/pipeline changes and should not require a new Android APK while the client schema stays compatible.

## 10. Review/observability goal

Repository data/CI should let a maintainer answer without forensic work:

- Which sources are scheduled, runtime-managed, manual, review-only, paused, or retired?
- When was each scheduled source last checked successfully?
- Is any scheduled source overdue or repeatedly failing?
- What changed on the last successful scan?
- Which candidates were discovered, published, deduplicated, or quarantined where the adapter reports those counts?
- Which General EQ source files changed and are waiting for requalification?
- When was broad new-source discovery last reviewed?

The daily source-health artifact/step summary is the first maintained observability surface. A separate UI/dashboard is optional and not required for production correctness.

## 11. Acceptance criteria for the source-automation milestone

The milestone is complete when:

1. every registered source has accurate currentness ownership rather than silently appearing scheduled;
2. every `scheduled` source is backed by a real adapter and its configured cadence is exercised by GitHub Actions;
3. overdue/never-successful/repeated-failure scheduled-source health is automatically surfaced and can fail the health gate;
4. scheduled source-health timestamps/cursors come from real adapter runs rather than invented success;
5. recurring review-only discovery covers new headphone-specific and General EQ candidates without auto-publishing unqualified sources;
6. all publication candidates flow through canonical provenance/dedupe/revision/archive validation;
7. source failures cannot erase archived EQs or replace last-known-good publication;
8. normal production operation remains independent of ChatGPT and independent of Android forum scraping;
9. tests cover cadence/currentness ownership/overdue behavior, failure isolation, source movement/removal, dedupe/revision behavior, General source change review gating, and living-archive preservation;
10. Reddit live scanning is either restored through a compliant public access path or remains explicitly paused, and Head-Fi / Audio Science Review remain accurately manual until robust live adapters exist.

The implementation listed in section 2 is designed to satisfy these criteria once its PR and post-merge `main` validation are green.

## 12. Release boundary

Source automation is repository/catalog infrastructure. It must not be coupled to an APK release when the canonical client schema remains compatible. New qualified sources, new community EQs, new General presets, source-health changes, and ordinary immutable revisions should publish through the catalog pipeline independently of Android application releases.
