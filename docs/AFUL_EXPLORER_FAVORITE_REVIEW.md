# AFUL Explorer canonical Favorite candidate review

Status: **VERIFIED FOR REPOSITORY PROMOTION** on 2026-09-30. This closes the bounded Favorite
identity verification stage only; it is not app release readiness or hardware qualification.

## 2026-09-30 final verification and closeout

The owner directly authorized Luna to complete final verification and repository closeout, replacing
the earlier pending Sol-first baton for this candidate. A separate read-only reviewer returned
PASS with no actionable implementation finding. The prior access-limited Sol attempt remains
INCONCLUSIVE and is not treated as a code review result.

- Focused JVM command `./tools/codex-android :app:testDebugUnitTest --tests 'com.weekssa.opraeqforuapp.data.library.CanonicalFavoriteAliasIntegrationTest' --tests 'com.weekssa.opraeqforuapp.data.library.CanonicalFirstCatalogRepositoryTest' --tests 'com.weekssa.opraeqforuapp.domain.library.CanonicalLegacyCatalogAdapterTest' --tests 'com.weekssa.opraeqforuapp.domain.library.CatalogOverlayTest'`: **PASS**, 22 tests.
- Full JVM command `./tools/codex-android :app:testDebugUnitTest`: **PASS**, 711 tests, no failures, errors, or skips.
- Android test Kotlin compilation `./tools/codex-android :app:compileDebugAndroidTestKotlin`: **PASS**.
- API 36 installation `./tools/codex-android :app:installDebug :app:installDebugAndroidTest`: **PASS**.
- Room instrumentation command `./tools/codex-android adb shell am instrument -w -e class com.weekssa.opraeqforuapp.data.library.SavedEqCanonicalSelectionPersistenceTest com.weekssa.opraeqforuapp.test/androidx.test.runner.AndroidJUnitRunner`: **PASS**, `OK (6 tests)`.
- UI smoke: **PASS** on AVD `codex-api36` / `emulator-5554` / API 36. Both affected AFUL Explorer community Favorites restored under their intended Reddit and HiFiGuides source identities after force-stop/relaunch; the unrelated AutoEQ Favorite remained. Test Favorites were removed afterward and the initially empty library state was restored. The detailed procedure and observations are in `docs/AFUL_EXPLORER_FAVORITE_UI_SMOKE.md`.
- No physical device was attached, and none was needed for this software path. No release or hardware claim is made.
- The closeout made no production or test changes. Documentation, commit, push, and PR outcomes are recorded in `docs/AFUL_EXPLORER_REVIEW_OUTCOME.md` and the dated runbook entry.

## Candidate identity and repository state

- Repository: `weekssa/OPRA-EQ-for-UAPP`
- Isolated worktree: `/Users/stephenweeks/.codex/worktrees/aful-favorite-fix/OPRA-EQ-for-UAPP`
- Branch: `codex/aful-favorite-identity`
- Implementation commit: `fcb0f5e03b22fc51c778d1989ec1c0ad8c7ddb38`
- Parent / current `origin/main`: `6ada7efc84425f7b149f9f9e9fb4e9599bc80cf2`
- Current `origin/catalog-live`: `b85ee763de4712aa39d38095db93aac083b03585`
- Saved checkout at task start: branch `codex/ja11-protocol-evidence`, HEAD
  `dc34db33258159a7f73e92df0c792908c92166c8`, with 46 pre-existing untracked paths and no
  tracked modifications. It was not changed.
- Candidate files changed: `CanonicalFirstCatalogRepository.kt`,
  `SavedEqCanonicalSelectionPersistenceTest.kt`,
  `CanonicalFavoriteAliasIntegrationTest.kt`, `docs/ARCHITECTURE.md`,
  `docs/V0.6_LIBRARY_OWNERSHIP_AND_RECOVERY.md`, and `CHANGELOG.md`.
- `git diff --check` passed before commit. The worktree was clean immediately after the
  implementation commit.
- The branch is local and unpushed. There is no PR or remote CI result for this candidate. GitHub
  was refreshed: related historical Favorites PRs #34 and #35 are closed; neither is this head.

## Current catalog evidence

The catalog was refreshed from `origin/catalog-live` at the SHA above. Both expected entries are
latest community revisions:

| Field | LoboNautics | Jaytiss |
|---|---|---|
| Canonical profile | `community-96571b708868cdf52c109d5f` | `community-d11e5db85fc6a1bb1fa7c825` |
| Headphone | AFUL / Explorer | AFUL / Explorer |
| Creator / tuning label | LoboNautics / Basshead tuning | Jaytiss / Jaytiss community tuning |
| Revision / latest | `rev-b2b639d1d1dfee12e6b87f97` / yes | `rev-47f31da216daad79be8a19bd` / yes |
| Acoustic fingerprint | `b2b639d1d1dfee12e6b87f970fbe8e3d0d4e0a86493d1a3bcee1dfbe4783b5e9` | `47f31da216daad79be8a19bd0cfc463596c7470ff925010976b30aff7a462318` |
| Primary source / record | `reddit-audio` / `reddit-iems-1pi6g5d-lobonautics-explorer-basshead` | `hifiguides` / `hifiguides-aful-explorer-jaytiss` |
| Source vendor / product | AFUL / Explorer | AFUL / Explorer |
| Source URL | [Reddit r/iems](https://www.reddit.com/r/iems/comments/1pi6g5d/aful_explorer_basshead_tuning/) | [HiFiGuides](https://forum.hifiguides.com/t/aful-explorer-1dd-2ba-hybrid-in-ear-monitors/42713) |

The current legacy OPRA product is vendor `aful` (AFUL), product `aful::explorer` (Explorer),
manufacturer AFUL, model Explorer. Its existing EQ profile records include
`aful:explorer::autoeq_fahryst`, `aful:explorer::autoeq_super_review`, and
`aful:explorer::autoeq_jaytiss`. The curated source files and catalog publication inputs were not
changed.

## Identity transformation and root cause

The production canonical adapter projects both community profiles with the same compatibility
vendor and product IDs:

- synthetic vendor: `eq-library-vendor:aful`
- synthetic product: `eq-library-product:aful|explorer||`
- canonical adapter profile ID for LoboNautics:
  `eq-library:community-96571b708868cdf52c109d5f@rev-b2b639d1d1dfee12e6b87f97`
- canonical adapter profile ID for Jaytiss:
  `eq-library:community-d11e5db85fc6a1bb1fa7c825@rev-47f31da216daad79be8a19bd`

| Identity | LoboNautics | Jaytiss |
|---|---|---|
| Effective overlay alias | synthetic product -> `aful::explorer` | synthetic product -> `aful::explorer` |
| Displayed profile ID / product ID | canonical adapter profile ID / `aful::explorer` | canonical adapter profile ID / `aful::explorer` |
| Pre-fix canonical resolution | non-null, exact latest revision | non-null, exact latest revision |
| Pre-fix `matchesSelection` | false | false |
| Post-fix compatibility vendor / product | `aful` / `aful::explorer` | `aful` / `aful::explorer` |
| Post-fix `matchesSelection` | true | true |
| Selected source record retained | `reddit-iems-1pi6g5d-lobonautics-explorer-basshead` | `hifiguides-aful-explorer-jaytiss` |

H1 is confirmed. Exact canonical resolution returned a selection, but its
`compatibilityProductId` was `eq-library-product:aful|explorer||` while the overlaid displayed
profile used `aful::explorer`. `SavedEqRepository.toggleFavorite()` revalidated the selection using
the displayed product ID; `projectSelection(selection, displayedProductId)` rejected this mismatch
and returned `CANONICAL_SOURCE_UNAVAILABLE` before a Room write. Substituting the selection's
synthetic product ID into the otherwise unchanged displayed projection made
`matchesSelection` pass, isolating the mismatch.

H2 was not the cause: both current rows resolve non-null, and a duplicate/ambiguous canonical
projection fixture resolves null. H3 was not reproduced: the refreshed catalog contains both
records, and a fresh API 36 emulator loaded the current feeds and displayed the Reddit and
HiFiGuides entries.

## Implementation and regression coverage

`CanonicalFirstCatalogRepository.resolveCanonicalSelection()` now rebases only compatibility
vendor/product IDs after all of the following are true:

1. the effective catalog proves the exact canonical product ID to displayed product ID alias;
2. the displayed legacy product exists uniquely; and
3. its vendor exists.

The immutable canonical profile, selected revision, fingerprint, and source references are kept as
resolved. `CanonicalLegacyCatalogAdapter.matchesSelection()` remains strict. No curated catalog,
source registry, publication tooling, database schema, UI, or USB/hardware code changed.

The new JVM `CanonicalFavoriteAliasIntegrationTest` composes the canonical repository, production
adapter/overlay, displayed profile, canonical selection resolver, and selection matcher for both
current AFUL community profiles. It checks exact source/revision/fingerprint retention, codec
round-trip, stale acoustic projection rejection, wrong canonical ID rejection, and ambiguous
selection rejection. Existing exact legacy OPRA fallback tests remain covered.

The Room-backed `SavedEqCanonicalSelectionPersistenceTest.aliasedCommunityFavoritePersistsCanonicalSelectionAndCanBeRemoved`
exercises the LoboNautics path through the production resolver and `SavedEqRepository`, verifies
that an unrelated product ID returns `CANONICAL_SOURCE_UNAVAILABLE` without a database write,
then verifies canonical selection persistence, action-profile identity, source record, and removal.

## 2026-09-30 source-independent Favorite coverage

The owner requested stronger evidence that the alias correction applies to catalog Favorites from
different source types and headphone products. On branch `codex/aful-source-matrix`, based on the
merged fix at `2a39bc53d7ee3caf2b98fe2fb043aff7bf4bdc74`, a test-only regression matrix was added in
`CanonicalFavoriteAliasIntegrationTest.favoriteAliasResolutionIsIndependentOfCatalogSourceKindAndHeadphoneProduct`.
It runs the production canonical repository, overlay, alias resolution, and strict Favorite
selection matcher for every canonical headphone-catalog source kind: `STRUCTURED_CATALOG`,
`MEASUREMENT_DERIVED`, `CREATOR`, `COMMUNITY`, `REPOSITORY`, `DEVICE_COMMUNITY`, and
`USER_SUBMISSION`. The test asserts exact set coverage so a future catalog source kind cannot be
silently omitted. Cases span AFUL Explorer (in-ear), Sony WH-1000XM5 (over-ear), and Sennheiser HD
600 (over-ear), with distinct acoustics to prevent overlay deduplication.

For each case, the test verifies the canonical-to-legacy product alias, displayed product and
vendor compatibility IDs, strict `matchesSelection`, exact selected profile and revision, source
kind, and unchanged source references. Existing regressions still cover the live LoboNautics and
Jaytiss profiles, stale projection, wrong canonical ID, ambiguity, unrelated product rejection,
exact OPRA fallback, Room persistence, and UI restoration.

`DEVICE_CAPTURE` and `PERSONAL_IMPORT` are deliberately outside this catalog Favorite matrix. The
application stores those as local saved EQs through separate capture/import flows; their adapter
and snapshot migration behavior has separate coverage in `LocalSavedEqAdapterTest` and
`SavedEqCanonicalSnapshotMigrationTest`. The Favorite alias resolver consumes displayed catalog
headphone profiles, so the seven catalog source kinds above are the complete in-scope matrix.

Final checks after adding the source matrix:

- Focused catalog/alias JVM command using `CanonicalFavoriteAliasIntegrationTest`,
  `CanonicalFirstCatalogRepositoryTest`, `CanonicalLegacyCatalogAdapterTest`, and `CatalogOverlayTest`:
  **PASS**, 23 tests.
- Full JVM command `./tools/codex-android :app:testDebugUnitTest`: **PASS**, 712 tests, no
  failures, errors, or skips.
- Android test compilation `./tools/codex-android :app:compileDebugAndroidTestKotlin`: **PASS**.
- API 36 AVD `codex-api36` install of debug app and Android tests: **PASS**.
- Room command
  `./tools/codex-android adb shell am instrument -w -e class com.weekssa.opraeqforuapp.data.library.SavedEqCanonicalSelectionPersistenceTest com.weekssa.opraeqforuapp.test/androidx.test.runner.AndroidJUnitRunner`:
  **PASS**, `OK (6 tests)`.
- The fresh debug UI Favorite/restart smoke recorded above remains applicable because this
  follow-up changes only test and review documentation, not production code. No physical device was
  used.

This evidence establishes source-kind independence at the canonical headphone Favorite resolver
boundary. It does not quantify a statistical probability or claim coverage of every external feed
parser independently. The production implementation has no source-kind branch in the alias rebase;
source-specific ingestion remains covered by its source tests and remote CI. PR #52 was opened from
test commit `913ea660dbdd89d37f17e8b066f43296c144058f` against merged `main` commit
`2a39bc53d7ee3caf2b98fe2fb043aff7bf4bdc74`. On that exact PR head, Android CI run 1889, CodeQL run
1774, Catalog currentness CI run 2147, and Priority community coverage CI run 1632 all passed.
Android CI included unit tests, lint, debug/release builds, the API 26 cold-install smoke, and API
36 connected UI tests. PR #52 remains open for normal owner review; it has not been merged.

## Verification evidence

- **Pre-fix RED:** `./tools/codex-android :app:testDebugUnitTest --tests 'com.weekssa.opraeqforuapp.data.library.CanonicalFavoriteAliasIntegrationTest'` failed at the regression expectation: expected compatibility vendor `aful`, observed `eq-library-vendor:aful`. Direct `matchesSelection` was false; substituting the selection's product ID made it true.
- **Focused JVM PASS:** `CanonicalFavoriteAliasIntegrationTest`, `CanonicalFirstCatalogRepositoryTest`, `CanonicalLegacyCatalogAdapterTest`, and `CatalogOverlayTest` passed together.
- **Full JVM PASS:** `./tools/codex-android :app:testDebugUnitTest` passed.
- **Android test compilation PASS:** `./tools/codex-android :app:compileDebugAndroidTestKotlin` passed.
- **Instrumented Room PASS:** direct API 36 AVD run of `SavedEqCanonicalSelectionPersistenceTest` passed `OK (6 tests)`, including the invalid-product no-write and valid-save/remove controls.
- **Fresh debug-app UI smoke PASS:** AFUL Explorer loaded from current feeds; the AutoEQ Jaytiss baseline Favorite was saved; both Reddit/LoboNautics and HiFiGuides/Jaytiss Favorites saved and appeared in My EQs with their source labels; LoboNautics was removed; force-stop/relaunch preserved HiFiGuides/Jaytiss and AutoEQ while keeping LoboNautics removed. The detailed command/layout record is `docs/AFUL_EXPLORER_FAVORITE_UI_SMOKE.md`.
- A temporary API 36 AVD was removed after the smoke run. No physical DAC, USB mutation, or hardware test was used or needed because this failure occurs before the hardware path.
- At the initial implementation handoff, no remote CI, signed APK, release artifact, or PR existed
  for the unpushed candidate. These are not implied by the local verification results.

## Historical pre-closeout review boundary

This described the evidence boundary at the earlier implementation handoff. The later 2026-09-30
verification above supersedes its pending-review status. The UI invalid-identity case was not
constructed as a UI row; fail-closed behavior is covered by the JVM and Room negative controls.
No signed artifact, clean-install/upgrade release validation, physical device, or hardware
qualification was performed, and repository-promotion verification does not imply those claims.

## Previous Sol-first routing workflow (historical)

This was the next role before the owner supplied the direct Luna final-verification request. That
request is now complete. The general workflow and PASS, FAIL, and INCONCLUSIVE paths remain in
`docs/AFUL_EXPLORER_REVIEW_TO_LUNA_WORKFLOW.md` for future candidates.

The reviewer may update review/handoff documentation and append the review result to the runbook.
It must not modify production code or tests. A PASS means only that the reviewer found no
actionable code-review issue within the files and evidence it could inspect. It does not mean the
bug is closed, remote CI passed, or a release is ready. FAIL findings and INCONCLUSIVE access gaps
must both be packaged for Luna, with FAIL adding concrete fix and verification criteria.

## Archived independent review handoff prompt (not active)

```text
You are the independent reviewer in the first stage of a two-stage handoff. This review will be
performed in a Sol 5.6 chat. The second stage is a new Codex Luna Extra High task that takes the
worker role. You can access the candidate folder below; use only the repository and evidence that
are actually available to you.

Candidate folder:
/Users/stephenweeks/.codex/worktrees/aful-favorite-fix/OPRA-EQ-for-UAPP

Repository: weekssa/OPRA-EQ-for-UAPP
Branch: codex/aful-favorite-identity
Expected review HEAD: use the exact SHA supplied by the dispatcher with this prompt; first verify
`git rev-parse HEAD` matches it. If it does not, preserve the checkout and report the actual HEAD
and status before reviewing. Do not reset, rebase, or discard changes.
Implementation commit: fcb0f5e03b22fc51c778d1989ec1c0ad8c7ddb38
Parent: 6ada7efc84425f7b149f9f9e9fb4e9599bc80cf2

Read AGENTS.md, docs/CHATGPT_PROJECT_RUNBOOK.md, docs/ARCHITECTURE.md,
docs/V0.6_LIBRARY_OWNERSHIP_AND_RECOVERY.md, this report, the UI smoke record, and
docs/AFUL_EXPLORER_REVIEW_TO_LUNA_WORKFLOW.md. Review the production diff and tests against the
parent. Confirm whether the compatibility-ID rebase is limited to an exact effective-catalog
product alias with an existing displayed product/vendor; canonical revision, fingerprint, and
source provenance remain intact; and wrong product, stale projection, wrong canonical ID,
ambiguity, and exact legacy OPRA fallback behavior remain safe.

Classify the result as PASS, FAIL, or INCONCLUSIVE. PASS means no actionable code finding from the
accessible source and evidence. It is not a product/release closure claim. Do not fail the code
review merely because remote CI, a signed APK, or hardware evidence is absent; those are explicitly
not part of this pre-merge code review. Use INCONCLUSIVE if the candidate source or required local
evidence is unavailable, and name the exact access gap.

Use recorded test results as evidence; run focused JVM tests only if needed to resolve a review
question. Do not modify production code/tests, commit, push, create a PR/task, change catalogs, run
release/signing workflows, or use physical hardware. You are authorized to create or update only
review/handoff documentation: write `docs/AFUL_EXPLORER_REVIEW_OUTCOME.md`, write
`docs/AFUL_EXPLORER_LUNA_WORKER_HANDOFF.md` with a complete copyable prompt for the next new Codex
Luna Extra High task, and append a concise dated status/link to `docs/CHATGPT_PROJECT_RUNBOOK.md`.
Do not stage or commit these documentation updates. Preserve existing worktree changes.

The Luna prompt is required for every outcome:

- PASS: ask Luna to take the worker role for bounded final verification and owner handoff, without
  inventing code changes or claiming closure.
- FAIL: carry every finding with priority, exact file/line, reproduction, correction, and
  acceptance tests; authorize Luna as worker to implement and verify those fixes in this worktree.
- INCONCLUSIVE: identify precisely what could not be inspected and ask Luna to resolve that gap
  using available repository tools before deciding whether code changes are needed.

Return the outcome, changed documentation paths, and the full Luna handoff prompt in your response.
Do not declare product/release closure, merge, publish, or request owner hardware work.
```
