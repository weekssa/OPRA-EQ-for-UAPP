# v0.7.2 active execution state

## Identity

- Repository: `weekssa/OPRA-EQ-for-UAPP`
- Worktree: `/Users/stephenweeks/.codex/worktrees/a79b/OPRA-EQ-for-UAPP`
- Branch: `codex/v0.7.2-stabilization`
- Verified release base: `v0.7.1` at `c48f6a5daa08a5e03475b2e415fe80b41d3357db`, versionName `0.7.1`, versionCode `8`
- Latest pushed checkpoint at initial PR creation: `c81f329b35773141ee7ba583468eaa9e8d7e009b`
- Product source candidate with local test evidence: `1c36349ca3edb69061a34b44d385670380f60512`
- PR #65: open from `codex/v0.7.2-stabilization` into `main`; status ledger synchronization is being prepared after initial CI started
- Release target: versionName `0.7.2`, versionCode `9`
- No physical DAC was connected or mutated.

## Completed this phase

- Independent test-only dense response oracle added for PEAK, LOW_SHELF, and HIGH_SHELF.
- Confirmed 96-point high-Q under-read: 4.392811513 dB for +12 dB/Q=10 at 978.371245 Hz and 8.683458274 dB for two coincident +12 dB/Q=10 filters at 978 Hz.
- Optimizer generated headroom and editor Safe Gain use dense sampling; final non-exact optimizer results must pass dense RMS/max-error gates.
- Exact generated-headroom range-boundary rounding corrected without changing strict source-authored preamp validation.
- Dense regressions cover maximum-Q positive/negative boosts, 20 Hz/20 kHz edges, interacting filters, the source shelf corpus, target quantization, and a six-band JA11 fit rejected by dense validation.
- Finite-hardware representation versions advanced; export fingerprint expectation updated.
- Complete JVM suite on commit `1c36349ca3edb69061a34b44d385670380f60512`: 727 tests passed, 0 failures/errors/skips.
- Android lint on the committed candidate: 0 errors, 111 warnings, 2 hints. Review found only existing patterns in unchanged code and no new actionable warning in changed code.
- Debug and release assembly passed. Forced `compileDebugKotlin --rerun-tasks` passed; its two warnings are in unchanged files. R8 mapping verification passed with at least one app class renamed.
- API 35 instrumentation passed 25 tests with 0 failures/errors/skips on the local ARM64 API 35 emulator.
- Repository Python suite passed 235 tests. Release contract, 15-source catalog registry, 14-sample Favorite validation, actionlint, ShellCheck, and `git diff --check` passed.
- Android metadata now targets versionName `0.7.2` and versionCode `9`; package ID and SDK levels were not changed.
- User-facing v0.7.2 release notes, changelog entry, README preparation note, public checklist section, and runbook status have been prepared. Publication remains pending.
- Release automation changes and previous review-finding fixes are recorded in their corresponding investigation and gate files.
- API 26 startup follow-up OOMs were traced beyond the canonical file read: eager catalog indexes, per-profile formatted acoustic signatures, and main-thread overlay work. Lookup indexes are lazy, acoustic dedup retains compact collision-checked fingerprints, numeric formatting avoids formatter allocation except on rounding boundaries, and catalog rendering runs on `Dispatchers.Default`.
- Focused `CatalogOverlayTest` and `CanonicalFirstCatalogRepositoryTest` pass, including fixed-precision/negative-zero formatting checks.
- Exact-commit API 26 smoke on `1c36349ca3edb69061a34b44d385670380f60512` used a fresh wipe/install, rendered manufacturer, 1MORE model, and `oratory1990` profile UI, remained resumed/alive after 60 seconds, and had no AndroidRuntime error. The smoke signer is local-only. Final Dalvik use was 47,111 of 49,152 KiB. Evidence is in `.unlazy/v0.7.2-autonomous-release/evidence/api26-final/RESULT.md`.
- An earlier exact-commit smoke attempt logged OOM while overlapping UIAutomation diagnostics caused an Android system-process service-registration crash. That contaminated attempt is recorded separately; a serialized clean rerun passed.
- The API 26 CI smoke in `.github/workflows/android-ci.yml` waits for the EQ Library tab and manufacturer list, then observes the process for 60 seconds. actionlint passed; exact PR-head CI remains pending.
- An independent read-only review agent scanned the working diff and found no confirmed correctness or regression finding. This does not replace exact PR-head review.
- All eight checks passed on initial PR head `c81f329b35773141ee7ba583468eaa9e8d7e009b`: Android build, emulator UI, API 26 x86_64 smoke, CodeQL, catalog currentness, community coverage, and dependency submission. This docs-only status checkpoint advances the PR head; verify all checks again on the updated head.
- Active `Protect main` ruleset prohibits deletion and non-fast-forward updates, has no bypass actors, and has no required reviewer or named status checks. Merge must remain a normal GitHub merge after this task's checks and independent review pass.

## Current working changes

The product candidate at `1c36349ca3edb69061a34b44d385670380f60512` contains release automation, safe Kotlin/UI/persistence fixes, version metadata, and synchronized release documentation. Commit `c81f329b` records exact-source/API-26 evidence. PR #65 is open, and every check passed on its initial head. This local status synchronization is documentation-only and causes checks to rerun on the resulting PR head. Immutable v0.7.1 remains an ancestor.

## Next

1. Refresh PR #65's exact live head and wait for all checks to pass after the status checkpoint.
2. Obtain independent exact-head review, then merge under the active main ruleset.
3. Reverify merged main and generate the main-only signed candidate.
4. Verify the exact merged-main signed artifact before immutable tagging/publication.

## Current failure

No API 26 startup failure was reproduced on the latest clean committed-source smoke, though memory headroom remains low. PR #65 is open; exact PR CI, independent review, merge, signed candidate, and release gates remain pending. No v0.7.2 tag or public release exists.
