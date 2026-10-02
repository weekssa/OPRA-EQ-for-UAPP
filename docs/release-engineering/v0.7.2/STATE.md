# v0.7.2 active execution state

## Identity

- Repository: `weekssa/OPRA-EQ-for-UAPP`
- Worktree: `/Users/stephenweeks/.codex/worktrees/a79b/OPRA-EQ-for-UAPP`
- Branch: `codex/v0.7.2-stabilization`
- Verified release base: `v0.7.1` at `c48f6a5daa08a5e03475b2e415fe80b41d3357db`, versionName `0.7.1`, versionCode `8`
- Current pushed checkpoint: `d37e114d466d681138140a0280a7c988235c3c94`
- Release target: versionName `0.7.2`, versionCode `9`
- No physical DAC was connected or mutated.

## Completed this phase

- Independent test-only dense response oracle added for PEAK, LOW_SHELF, and HIGH_SHELF.
- Confirmed 96-point high-Q under-read: 4.392811513 dB for +12 dB/Q=10 at 978.371245 Hz and 8.683458274 dB for two coincident +12 dB/Q=10 filters at 978 Hz.
- Optimizer generated headroom and editor Safe Gain use dense sampling; final non-exact optimizer results must pass dense RMS/max-error gates.
- Exact generated-headroom range-boundary rounding corrected without changing strict source-authored preamp validation.
- Dense regressions cover maximum-Q positive/negative boosts, 20 Hz/20 kHz edges, interacting filters, the source shelf corpus, target quantization, and a six-band JA11 fit rejected by dense validation.
- Finite-hardware representation versions advanced; export fingerprint expectation updated.
- Complete JVM suite on the dirty v0.7.2/code 9 worktree: 727 tests passed, 0 failures/errors/skips.
- Android lint on the dirty candidate: 0 errors, 111 warnings, 2 hints. Review found only existing patterns in unchanged code and no new actionable warning in changed code.
- Debug and release assembly passed. Forced `compileDebugKotlin --rerun-tasks` passed; its two warnings are in unchanged files. R8 mapping verification passed with at least one app class renamed.
- API 35 instrumentation passed 25 tests with 0 failures/errors/skips on the local ARM64 API 35 emulator.
- Repository Python suite passed 235 tests. Release contract, 15-source catalog registry, 14-sample Favorite validation, actionlint, ShellCheck, and `git diff --check` passed.
- Android metadata now targets versionName `0.7.2` and versionCode `9`; package ID and SDK levels were not changed.
- User-facing v0.7.2 release notes, changelog entry, README preparation note, public checklist section, and runbook status have been prepared. Publication remains pending.
- Release automation changes and previous review-finding fixes are recorded in their corresponding investigation and gate files.
- API 26 startup follow-up OOMs were traced beyond the canonical file read: eager catalog indexes, per-profile formatted acoustic signatures, and main-thread overlay work. Lookup indexes are lazy, acoustic dedup retains compact collision-checked fingerprints, numeric formatting avoids formatter allocation except on rounding boundaries, and catalog rendering runs on `Dispatchers.Default`.
- Focused `CatalogOverlayTest` and `CanonicalFirstCatalogRepositoryTest` pass, including fixed-precision/negative-zero formatting checks.
- Latest local API 26 cold smoke from the dirty 0.7.2/code 9 worktree completed the catalog overlay, rendered the manufacturer/model/profile UI, and remained resumed/alive after a 60-second observation with no AndroidRuntime error. The smoke APK and signer are local-only, not final candidate evidence. The earlier 13-minute run is preserved separately.
- The API 26 CI smoke in `.github/workflows/android-ci.yml` now waits for the EQ Library tab and manufacturer list, then observes the process for 60 seconds. actionlint passed; exact PR-head CI and a committed-source rerun remain pending.
- An independent read-only review agent scanned the working diff and found no confirmed correctness or regression finding. This does not replace exact PR-head review.

## Current working changes

The worktree contains uncommitted release automation, safe Kotlin/UI/persistence fixes, version metadata, and synchronized release documentation. The pushed branch includes the current DSP checkpoint and a clean merge of current `origin/main`; the immutable v0.7.1 commit remains an ancestor.

## Next

1. Finish synchronizing the local release ledger, commit the coherent v0.7.2/code 9 candidate, and push without force.
2. Repeat release-critical local validation and API 26 smoke against that exact commit.
3. Open the PR, obtain exact-head independent review and required checks, then merge under protection.
4. Verify the exact merged-main signed artifact before immutable tagging/publication.

## Current failure

No API 26 startup failure is currently reproduced on the latest dirty worktree source. The local gates pass, but exact committed-source validation, CI x86_64 verification, PR review, merge, signed candidate, and release gates remain pending. No v0.7.2 tag or public release exists.
