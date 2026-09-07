# EQ Library — Source Automation Plan and Closeout Record

This document is the maintained source of truth for repository-side source expansion/currentness after the v0.3 catalog foundation. The living-archive guarantees in `docs/V0.3_RELEASE_POLISH_PLAN.md`, the canonical rules in `docs/SOURCE_INGESTION_STRATEGY.md`, and later explicit user decisions remain mandatory.

The goal is a self-maintaining canonical EQ archive that does not depend on recurring manual data entry, ChatGPT in production, Android forum scraping, or an APK release for ordinary catalog/source updates.

## 1. Final automation-first operating model

Production source maintenance uses the path appropriate to each source:

`public source -> scheduled adapter or Android runtime refresh -> parse/validate -> provenance/identity -> acoustic dedupe/revision/archive checks -> catalog/local cache`

Rules:

- If a registered source has a legitimate, stable public retrieval path, it should be automated at an appropriate cadence.
- OPRA remains intentionally **runtime-managed** by the Android app against the official `database_v1.jsonl` feed.
- A source that cannot be automated safely because required access is unavailable or the provider prohibits the needed automated retrieval is **paused**, not disguised as a recurring manual-currentness dependency.
- One-off human review may still be required for exceptional quarantine, source-policy, identity, or changed-upstream qualification decisions, but ordinary source currentness must not require periodic hand entry.
- Source failure never deletes previously published canonical EQs/revisions and never replaces the last-known-good catalog with a partial or malformed candidate.
- Android normal operation consumes validated published catalog data and does not crawl forums/GitHub.
- ChatGPT may help develop or repair adapters, but normal production currentness must work without ChatGPT.

## 2. Registered source state at automation closeout

The registry contains 15 source identities. The automation-first target state is:

### Runtime-managed

- **OPRA** — active; approximately-daily Android runtime refresh against the official catalog feed.

### Scheduled repository automation

- **AutoEq** — daily structured-corpus ingestion/currentness.
- **MrChillStorm Headphone Target** — weekly qualified GitHub source.
- **Fairbuds by Juraj Fiala** — weekly qualified GitHub source.
- **Squiglink-compatible ecosystem** — weekly public-registry currentness/provenance monitoring. Measurement curves remain measurements; EQ Library does not invent source-authored PEQ filters from them. Exact PEQ derived from Squig measurements may arrive through separately qualified automated carriers such as AutoEq with provenance retained.
- **Head-Fi** — weekly bounded public RSS/thread discovery for exact structured PEQ.
- **Audio Science Review** — weekly bounded public RSS/thread discovery for exact structured PEQ.
- **The HEADPHONE Community / Headphones.com** — weekly public Discourse discovery.
- **HiFiGuides** — weekly public Discourse discovery.
- **Public GitHub repositories and Gists** — daily community discovery/ingestion.
- **ParaEQ General presets** — weekly qualified-source currentness probe; changed upstream source structure is review-gated rather than silently rewriting qualified presets.
- **MilcioSSQ General presets** — weekly qualified-source currentness probe/publication lane.

### Explicitly paused rather than manually maintained

- **Reddit audio communities** — paused because GitHub-hosted unauthenticated JSON access returned HTTP 403 and Reddit requires an approved access path. No scraping/circumvention/manual-currentness substitute.
- **oratory1990 direct Reddit currentness** — paused for the same direct Reddit access limitation. Structured presets can continue to arrive automatically through qualified carriers such as OPRA; AutoEq measurement-derived results remain AutoEq-created results rather than oratory-authored presets.
- **Topping Community** — paused because the presently available community interface/terms do not provide an authorized public bulk/API path suitable for scheduled ingestion. Resume only with an authorized API/feed or explicit permission. Existing archived data remains preserved.

This closeout intentionally has **zero recurring `manual` currentness owners**.

## 3. Community ingestion behavior

Mechanically valid public community PEQ is not held indefinitely for human approval merely because discovery found it.

For an already-qualified community lane, a candidate may publish as **Unverified** only after:

1. exact source retrieval;
2. supported PEQ parsing with no invented values;
3. creator/source provenance retention;
4. safe headphone identity resolution, or explicit source-authored General intent/category;
5. acoustic dedupe/revision classification;
6. applicable source-policy checks;
7. living-archive validation;
8. deterministic catalog generation/publication.

Exact duplicates attach provenance instead of becoming duplicate tunings. Ambiguous identity, malformed/unsupported PEQ, inaccessible content, repost ambiguity, or specific contrary restrictions are quarantined individually without blocking unrelated candidates.

The initial GitHub/Gist production batch fetched 50 headphone candidates: 39 parsed as exact supported PEQ, 28 became new Unverified profiles, 11 exact acoustic duplicates merged provenance, and 11 identity-ambiguous/unmatched candidates were quarantined. Three broad General candidates lacked exact parametric structure and were rejected rather than receiving invented Q/filter types.

## 4. Forum and ecosystem adapters

### Head-Fi / Audio Science Review

Use bounded public RSS/thread retrieval only. Do not log in, crawl arbitrary pagination, or scrape whole sites. High-signal PEQ candidates may publish Unverified through the canonical pipeline. Public-path failure records source-health degradation and preserves last-known-good/archive data.

### Headphones.com / HiFiGuides

Use public Discourse JSON endpoints with targeted PEQ discovery, preserving username/topic/post provenance and exact coefficients.

### Squiglink

Monitor the public Squiglink/CrinGraph-compatible site registry for ecosystem currentness and provenance. Frequency-response/phone-book records are not independently published PEQ presets. Do not choose a target, run browser-generated AutoEQ, or manufacture creator-authored filters from curves on the source's behalf.

### GitHub/Gists

Retrieve exact public files by immutable blob/raw provenance where possible. Parse exact supported PEQ, preserve repository/file/creator provenance, deduplicate acoustically, and quarantine unsafe identity or malformed records without blocking the batch.

## 5. General-EQ automation

Qualified GitHub-backed General sources are checked automatically on their configured cadence.

- Published General presets remain pinned to reviewed exact source structure.
- Source changes are detected automatically and surfaced as `changed_needs_review` when qualification/classification must be revisited.
- Never infer Sound/Genre/Utility intent from filter shape.
- Never silently mutate existing canonical acoustic history because upstream code changed.

## 6. Source health and cadence enforcement

The source registry is authoritative for lifecycle, cadence, parser version, currentness mode, redistribution/attribution notes, and cursor strategy.

For every `scheduled` source, persisted health should include as applicable:

- last scan attempt;
- last successful scan;
- cursor/content fingerprint/high-water mark;
- parser version;
- consecutive failure count and last error;
- lifecycle/currentness state;
- last terms/policy review where applicable.

The strict source-health gate must surface and fail on:

- a scheduled source that has never succeeded;
- last success older than two configured cadence intervals;
- three or more consecutive failures;
- source-specific cursor staleness where the adapter can prove upstream change without advancement.

One or two recent failures inside the freshness window are warnings, not an instruction to delete data.

## 7. Unified automated-source workflow

`.github/workflows/automated-source-currentness.yml` is the automation-first currentness lane for the sources added in this final expansion. It:

1. validates the source registry and adapter regression tests;
2. builds a cadence-aware source plan;
3. refreshes due Head-Fi / Audio Science Review adapters;
4. refreshes due Squiglink ecosystem currentness;
5. probes due qualified General sources;
6. validates the resulting catalog atomically against the living archive;
7. persists source-health/catalog state;
8. commits validated state when changed;
9. on `main`, publishes the validated transport catalog to `catalog-live`;
10. uploads source-plan/reports/catalog/health artifacts for observability.

Existing dedicated scheduled lanes such as AutoEq, GitHub community ingestion, Discourse communities, and other qualified-source workflows remain valid owners for their registered sources. Concurrency/publication must continue to preserve atomic catalog behavior.

## 8. Living archive and failure isolation

All automation must preserve these invariants:

- source disappearance, movement, pause, retirement, timeout, parser failure, or access restriction never deletes a valid canonical EQ/revision;
- genuine acoustic changes create immutable revisions;
- same acoustic fingerprint updates provenance/currentness rather than manufacturing a revision;
- mirrors/reposts attach as provenance instead of duplicating the tuning;
- previously published canonical profiles/revisions remain represented in the current catalog;
- a failed source cannot replace last-known-good publication with partial state.

## 9. New-source discovery after closeout

Source expansion becomes routine maintenance after this milestone rather than an unfinished project.

At least monthly, broad discovery may identify newly useful databases, repositories, measurement projects, forums, creator sources, device communities, or General preset collections. A genuinely new source class/domain still requires source-level qualification for access, provenance, reliability, expected cadence, attribution, and any specific redistribution restriction.

Once a lane is qualified, individual exact community PEQ candidates should flow through deterministic publish/dedupe/quarantine decisions rather than accumulating in an indefinite review queue.

## 10. Stop conditions that require the project owner

Ordinary adapter/parser/CI/source-health repairs should be handled autonomously in the repository. Escalate to the owner only when a source requires something the repository cannot legitimately decide or obtain itself, including:

- account-bound API approval or credentials;
- explicit permission from a provider such as TOPPING;
- a changed license/terms question requiring product-owner acceptance;
- a genuinely ambiguous product/provenance policy decision that cannot be resolved from maintained project rules.

Never request passwords/tokens in chat and never commit credentials.

## 11. Source-automation milestone acceptance criteria

The source-expansion/automation project is complete when all of the following are true on merged `main`:

1. every registered source has accurate ownership (`scheduled`, `runtime`, `paused`, or retired as appropriate);
2. there are zero recurring manual-currentness dependencies;
3. every scheduled source has a real adapter/probe exercised by GitHub Actions at its configured cadence;
4. source-health timestamps/cursors come from real runs rather than invented success;
5. overdue/never-successful/repeated-failure scheduled sources are automatically surfaced;
6. active community discovery produces deterministic publish/dedupe/quarantine outcomes;
7. every publication path preserves provenance, dedupe/revision semantics, and the living archive;
8. source failures cannot erase archived EQs or replace last-known-good publication;
9. normal operation is independent of ChatGPT and Android forum scraping;
10. regression coverage for automation contracts, adapters, quarantine, currentness, General change gating, and archive preservation is green;
11. blocked ecosystems such as Reddit/TOPPING are explicitly paused rather than represented as working/manual automation;
12. the final PR is merged with all required CI green;
13. at least one post-merge production run on `main` succeeds and `catalog-live` is confirmed current.

After those criteria pass, future source work is ordinary maintenance/coverage improvement and does not keep this milestone open.

## 12. Release boundary

Source automation is repository/catalog infrastructure. It does not require a new Android APK while the canonical client schema remains compatible and Android/device/DSP behavior is unchanged. Ordinary new sources, EQs, immutable revisions, source-health updates, and catalog publication should continue independently of app releases.
