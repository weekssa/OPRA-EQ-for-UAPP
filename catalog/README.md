# EQ Library canonical catalog publication

`catalog/catalog.json` is the repository-maintained canonical EQ archive. The Android app downloads its runtime copy from the stable `catalog-live` branch:

`https://raw.githubusercontent.com/weekssa/OPRA-EQ-for-UAPP/catalog-live/catalog/catalog.json`

The stable publication branch is deliberately separate from versioned development branches such as `eq-library-community-v0.3` or `v0.3-release-polish`. App releases must not point normal runtime catalog refresh at a temporary/versioned development branch.

A catalog candidate is publishable only after the normal schema, identity, provenance, acoustic dedupe/revision, source-policy, deterministic-generation, and living-archive regression gates pass. Publication must preserve every previously published genuine canonical EQ and genuine acoustic revision; source movement, outage, removal, pause, retirement, or URL loss updates provenance/lifecycle state rather than deleting acoustic history.

The Android client validates a downloaded candidate before atomically promoting it to its private last-known-good cache. Failed downloads or invalid candidates never replace the last-known-good local catalog.

The `catalog-live` branch is a distribution surface, not an Android backend and not a runtime scraping mechanism. Source discovery/ingestion remains repository/GitHub-Actions tooling outside the app. Source-specific publishers may produce a validated catalog while they run, but the authoritative stable-publication workflow shares the main catalog-writer lock and then re-fetches the latest `main` catalog before rendering the Android transport copy. This prevents a slower workflow from leaving `catalog-live` on an older run-start snapshot after another source writer has advanced `main`. A scheduled reconciliation also repairs any drift without requiring an APK release.

For maintainability and useful Git review, the canonical catalog committed to normal source branches is kept deterministically pretty-printed. The validated `catalog-live` distribution copy is deterministically compacted for Android transport without changing its canonical data.
