# v0.7.2 artifact and remote state

## Source

- Repository: `weekssa/OPRA-EQ-for-UAPP`
- Worktree: `/Users/stephenweeks/.codex/worktrees/a79b/OPRA-EQ-for-UAPP`
- Branch: `codex/v0.7.2-stabilization`
- Base/tag: `v0.7.1`
- Base commit: `c48f6a5daa08a5e03475b2e415fe80b41d3357db`
- Starting metadata: versionName `0.7.1`, versionCode `8`
- Target metadata: versionName `0.7.2`, versionCode `9`
- Latest fetched `origin/main` at the last refresh: `afed3dc90b5873218d5e333882528f8c5ddd54a2` (2026-10-03)
- Latest main merge on this branch: `1530d02f`, with `afed3dc90b5873218d5e333882528f8c5ddd54a2` as its main parent; the v0.7.1 source remains an ancestor
- Product source candidate already in the PR: commit `1c36349ca3edb69061a34b44d385670380f60512`.
- Latest production source commit: `e63fc4bf5be629135b0fd56449bad4c1b0bfd1b4`; recovery commits `40b4f5d8c7b255ebed7cc886158d6e6621a911c4` and `e63fc4bf5be629135b0fd56449bad4c1b0bfd1b4` are in the branch. Latest fully verified PR snapshot is `3ccf6728a9edd6c85a76ece1f5e71bc93fa37b74`; all eight checks and independent review passed. This pre-merge ledger sync creates a docs-only descendant that needs fresh exact-head checks and review.
- Physical DAC writes: none

## Local verification

- Fresh full Gradle gate on source commit `e63fc4bf5be629135b0fd56449bad4c1b0bfd1b4`: 731 JVM tests passed with 0 failures/errors/skips; lint passed with 0 errors, 111 warnings, and 2 hints; debug and release APK assembly passed.
- Focused `Ew300PersistenceQualificationTest`: 17/17 passed, including pre-write checkpoint failures and failed `UNCERTAIN` checkpoints after either baseline-restoration path.
- R8 mapping verification on source commit `e63fc4bf5be629135b0fd56449bad4c1b0bfd1b4` passed; at least one app class is renamed.
- Forced Kotlin compile passed; its two warnings are in unchanged files
- API 35 instrumentation passed 25 tests with 0 failures/errors/skips on `opra-v072-api35` (ARM64, emulator 37.1.11)
- PR-head emulator UI/API35 instrumentation on `3ccf6728`: 25 tests, 0 failures/errors/skips; report artifact ID `11262353847`, run `37090841176`.
- Python tool suite passed 235 tests; release contract, registry validation, and Favorite sample validation passed
- actionlint 1.7.12 passed all workflow YAML; ShellCheck 0.11.0 passed repository shell scripts
- Independent reviews of `af3f4c59` and `13bf1f20` returned FAIL with two P2 replay paths, each now fixed in the pushed `e63fc4bf` source. Exact review of `8db499af` and its corrected docs descendant `3ccf6728` returned PASS; this ledger sync's descendant needs a fresh review.

## API 26 smoke artifacts

- Exact-source smoke for commit `1c36349ca3edb69061a34b44d385670380f60512` used a fresh `-wipe-data` API 26 ARM64 emulator with a 48 MiB heap-growth limit. It rendered manufacturer, model, and profile lists; PID 4267 remained alive and resumed after the 60-second observation, with no AndroidRuntime errors.
- Unsigned minified v0.7.2/code 9 APK SHA-256: `67a2663d53cedc30ad3395117636e02891f86b9a98f4ada4c6e552a4b000480c`
- Temporary smoke-signed APK SHA-256: `ea4da8e12ff7ffd680f3e7d5345a86bb7df5af0bee2e2d61fa8c2a8d51e517fd`
- Temporary smoke signer certificate SHA-256: `bba3818c88f1c0faf315b02dc725ca5f53a28fe4f5d001085b616973754d7b50`; it is not the project release signer
- `aapt` verified package `com.weekssa.opraeqforuapp`, versionName `0.7.2`, versionCode `9`, and minSdk 26; `apksigner verify` passed v2/v3
- Final Dalvik allocation was 47,111 KiB of 49,152 KiB, leaving 2,041 KiB free
- Full exact-source evidence, the earlier dirty-source smokes, and one separately recorded contaminated attempt are under `.unlazy/v0.7.2-autonomous-release/evidence/api26-final/`
- The clean local ARM64 run used committed source but a temporary signer. It is not CI x86_64 or release candidate provenance. The separate exact PR CI x86_64 evidence below passes on `8db499af`.
- PR-head API 26 x86_64 smoke on `8db499af`: fresh wipe, minified release APK built and temporary smoke-signed, cold install succeeded, MainActivity launch returned `Status: ok`, Manufacturers appeared, and the app remained alive and resumed through a 60-second observation with no app-process AndroidRuntime error. Diagnostics artifact `11261199392`, run `37088331227`. The smoke-signed APK is not retained as release provenance.
- Downloaded UI debug APK from run `37088331227`: package `com.weekssa.opraeqforuapp`, versionName `0.7.2`, versionCode `9`, SHA-256 `bf18e80806756814bd30bd4a7d35e3c0615cb211689accc44a69995a8e367b9c`. It is CI test evidence only, not the release candidate.

## Latest exact-head snapshot

- PR #65 was open, non-draft, and `CLEAN` against base `afed3dc90b5873218d5e333882528f8c5ddd54a2` at exact head `3ccf6728a9edd6c85a76ece1f5e71bc93fa37b74`.
- All eight GitHub checks passed on that exact head: Android build/UI/API 26 run `37090841176` (jobs `111110648767`, `111110648890`, `111110648953`); Analyze Kotlin run `37090841130` (job `111110648040`); CodeQL check `111111725873`; dependency submission run `37090838488` (job `111110640795`); Catalog run `37090841097` (job `111110647550`); Priority community run `37090841101` (job `111110647754`).
- Independent read-only review returned PASS on exact head `3ccf6728` against base `afed3dc90b5873218d5e333882528f8c5ddd54a2`, with no actionable P0-P2 issue or unsupported release claim. No review was posted to GitHub.
- Exact-head API 35 report artifact `11262353847` records 25 tests, 0 failures/errors/skips. API 26 report artifact `11261474879` records a fresh minified install, visible Manufacturers list, live PID, resumed MainActivity after the observation period, and no app-process AndroidRuntime error. Lint artifact `11262258835`. Debug APK artifact `11262004385` has SHA-256 `084e3e778023d9dd11dbe9e85a0531c7a08a1dabefb2b563c53f2279a2869caa`; it is CI test evidence only, not a release candidate.
- The current tracked pre-merge sync is a docs-only descendant of `3ccf6728`; resolve its live head, refresh CI and independent review, and merge only after exact-head gates pass and protection permits.

## Remote promotion state

- Prior PR branch heads `af3f4c596e99e0cf8dce408b982c641c6898de6a` and `13bf1f20002f32da622392980eea145a0eb2a776` are superseded by current source head `e63fc4bf5be629135b0fd56449bad4c1b0bfd1b4`.
- Pull request: [#65](https://github.com/weekssa/OPRA-EQ-for-UAPP/pull/65), open, base `main` at `afed3dc90b5873218d5e333882528f8c5ddd54a2`
- All eight checks passed on exact PR head `af3f4c596e99e0cf8dce408b982c641c6898de6a`: Android build run `37082764610`, CodeQL run `37082764637`, Catalog run `37082764642`, Priority community run `37082764613`, dependency-submission run `37082761608`, and the associated UI/API 26 jobs. API 26 report artifact `11259384390` and emulator UI report artifact `11258319894` were downloaded and inspected. These results predate the current local recovery fix.
- Exact PR head `8db499af0212795d05b97d0439a0c82f462722b9`: all eight checks passed. Android build/UI/API26 run `37088331227`, Analyze Kotlin `37088331231`, CodeQL check `111104328833`, Catalog `37088331236`, Priority community `37088331239`, and dependency submission `37088326577`.
- Independent exact-head review of `8db499af0212795d05b97d0439a0c82f462722b9` returned PASS with no actionable P0-P2 issue or unsupported release claim.
- Follow-up independent review of docs-only head `adf871f2` found one P2 stale restart instruction, corrected in this release-ledger update. Review the resulting live head before merge.
- Current PR-head artifacts: API 26 diagnostics `11261199392`, emulator UI reports `11261079560`, lint reports `11261154040`, and debug APK `11261034576`. They are test evidence only. Earlier artifacts `11259384390` and `11258319894` predate both recovery fixes.
- Independent review of `af3f4c59` found replay risk in the post-cycle restore path. Review of `13bf1f20` found the same risk in `restoreBeforeCommitOrFail()`. Both paths now persist terminal `RESTORATION_ATTEMPTED` before their first baseline write; if the checkpoint fails, no baseline write is sent.
- Full local validation on `e63fc4bf`: 731 JVM tests, zero failures/errors/skips; lint 0 errors, 111 warnings, 2 hints; debug/release assembly and R8 mapping pass. The focused EW300 suite passes 17/17.
- Active `Protect main` ruleset: prevents deletion and non-fast-forward updates, with no bypass actors. It defines no required reviewer or named status checks; the task's CI and independent-review gates still apply.
- Merge SHA: none
- Signed candidate workflow/run/artifact: none
- Release APK SHA-256: none
- Release signer certificate SHA-256: expected pinned project identity, to be independently verified on the candidate
- Candidate manifest: none
- Remote `v0.7.2` tag: absent at the last remote check
- GitHub Release: absent at the last remote check

The check and review results above are immutable head-specific snapshots. Resolve live GitHub state before acting. Merge only when all eight checks and an independent PASS apply to the exact current PR head. Update this file after merge, candidate, tag, publication, and public asset verification. Never copy a previous candidate checksum or signer verification forward as evidence for a changed source.
