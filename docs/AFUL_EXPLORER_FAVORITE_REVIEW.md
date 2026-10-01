# AFUL Explorer canonical Favorite candidate review

Status: **CANDIDATE FIX VERIFIED — ready for independent review**. This is an implementation
handoff, not a declaration that the issue is closed and not release or hardware qualification.

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

## Verification evidence

- **Pre-fix RED:** `./tools/codex-android :app:testDebugUnitTest --tests 'com.weekssa.opraeqforuapp.data.library.CanonicalFavoriteAliasIntegrationTest'` failed at the regression expectation: expected compatibility vendor `aful`, observed `eq-library-vendor:aful`. Direct `matchesSelection` was false; substituting the selection's product ID made it true.
- **Focused JVM PASS:** `CanonicalFavoriteAliasIntegrationTest`, `CanonicalFirstCatalogRepositoryTest`, `CanonicalLegacyCatalogAdapterTest`, and `CatalogOverlayTest` passed together.
- **Full JVM PASS:** `./tools/codex-android :app:testDebugUnitTest` passed.
- **Android test compilation PASS:** `./tools/codex-android :app:compileDebugAndroidTestKotlin` passed.
- **Instrumented Room PASS:** direct API 36 AVD run of `SavedEqCanonicalSelectionPersistenceTest` passed `OK (6 tests)`, including the invalid-product no-write and valid-save/remove controls.
- **Fresh debug-app UI smoke PASS:** AFUL Explorer loaded from current feeds; the AutoEQ Jaytiss baseline Favorite was saved; both Reddit/LoboNautics and HiFiGuides/Jaytiss Favorites saved and appeared in My EQs with their source labels; LoboNautics was removed; force-stop/relaunch preserved HiFiGuides/Jaytiss and AutoEQ while keeping LoboNautics removed. The detailed command/layout record is `docs/AFUL_EXPLORER_FAVORITE_UI_SMOKE.md`.
- A temporary API 36 AVD was removed after the smoke run. No physical DAC, USB mutation, or hardware test was used or needed because this failure occurs before the hardware path.
- No remote CI, signed APK, release artifact, or PR exists for this unpushed candidate. These are not implied by the local results.

## Remaining uncertainty and review boundary

The candidate is ready for an independent code/regression review, not closure. Review the narrow
alias evidence gate and ensure wrong, stale, ambiguous, and unrelated selections still fail closed.
The UI invalid-identity case was not exercised by constructing an invalid UI row; it is covered by
the JVM and Room negative controls. No signed artifact, clean-install/upgrade release validation,
remote CI, physical device, or hardware qualification was performed. Do not infer release readiness
or physical-device qualification from this candidate.

## Independent review handoff prompt

```text
Perform an independent, read-only review of the AFUL Explorer Favorite candidate in:

/Users/stephenweeks/.codex/worktrees/aful-favorite-fix/OPRA-EQ-for-UAPP

Repository: weekssa/OPRA-EQ-for-UAPP
Branch: codex/aful-favorite-identity
Implementation commit: fcb0f5e03b22fc51c778d1989ec1c0ad8c7ddb38
Parent: 6ada7efc84425f7b149f9f9e9fb4e9599bc80cf2

Read AGENTS.md, docs/CHATGPT_PROJECT_RUNBOOK.md, docs/ARCHITECTURE.md,
docs/V0.6_LIBRARY_OWNERSHIP_AND_RECOVERY.md, and this review report. Review the production diff
and tests against the parent. Confirm whether the compatibility-ID rebase is limited to an exact
effective-catalog product alias with an existing displayed product/vendor; canonical revision,
fingerprint, and source provenance remain intact; and wrong product, stale projection, wrong
canonical ID, ambiguity, and exact legacy OPRA fallback behavior remain safe.

Use the recorded test results as evidence; run the focused JVM tests only if needed to resolve a
review question. Do not edit files, commit, push, create a PR, change catalogs, run release/signing
workflows, or use physical hardware. Report actionable findings first with priority, file/line,
failure scenario, and a concrete correction. If there are no findings, say so explicitly and list
any remaining non-blocking uncertainty. Do not declare product/release closure.
```
