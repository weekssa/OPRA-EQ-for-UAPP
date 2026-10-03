# v0.7.2 artifact and remote state

## Source

- Repository: `weekssa/OPRA-EQ-for-UAPP`
- Worktree: `/Users/stephenweeks/.codex/worktrees/a79b/OPRA-EQ-for-UAPP`
- Branch: `codex/v0.7.2-stabilization`
- Base/tag: `v0.7.1`
- Base commit: `c48f6a5daa08a5e03475b2e415fe80b41d3357db`
- Starting metadata: versionName `0.7.1`, versionCode `8`
- Target metadata: versionName `0.7.2`, versionCode `9`
- Latest fetched `origin/main` at the last refresh: `afed3dc90b5873218d5e333882528f8c5ddd54a2` (2026-10-02)
- Latest main merge on this branch: `1530d02f`, with `afed3dc90b5873218d5e333882528f8c5ddd54a2` as its main parent; the v0.7.1 source remains an ancestor
- Product source candidate already in the PR: commit `1c36349ca3edb69061a34b44d385670380f60512`.
- Latest production source commit: `e63fc4bf5be629135b0fd56449bad4c1b0bfd1b4`; recovery commits `40b4f5d8c7b255ebed7cc886158d6e6621a911c4` and `e63fc4bf5be629135b0fd56449bad4c1b0bfd1b4` are in the branch. The PR may have a docs-only descendant; resolve its exact current SHA before relying on CI or review.
- Physical DAC writes: none

## Local verification

- Fresh full Gradle gate on source commit `e63fc4bf5be629135b0fd56449bad4c1b0bfd1b4`: 731 JVM tests passed with 0 failures/errors/skips; lint passed with 0 errors, 111 warnings, and 2 hints; debug and release APK assembly passed.
- Focused `Ew300PersistenceQualificationTest`: 17/17 passed, including pre-write checkpoint failures and failed `UNCERTAIN` checkpoints after either baseline-restoration path.
- R8 mapping verification on source commit `e63fc4bf5be629135b0fd56449bad4c1b0bfd1b4` passed; at least one app class is renamed.
- Forced Kotlin compile passed; its two warnings are in unchanged files
- API 35 instrumentation passed 25 tests with 0 failures/errors/skips on `opra-v072-api35` (ARM64, emulator 37.1.11)
- Python tool suite passed 235 tests; release contract, registry validation, and Favorite sample validation passed
- actionlint 1.7.12 passed all workflow YAML; ShellCheck 0.11.0 passed repository shell scripts
- Independent exact-head review of `af3f4c59` and `13bf1f20` returned FAIL with two P2 replay paths, each now fixed in the pushed `e63fc4bf` source. Exact-head review is still required.

## API 26 smoke artifacts

- Exact-source smoke for commit `1c36349ca3edb69061a34b44d385670380f60512` used a fresh `-wipe-data` API 26 ARM64 emulator with a 48 MiB heap-growth limit. It rendered manufacturer, model, and profile lists; PID 4267 remained alive and resumed after the 60-second observation, with no AndroidRuntime errors.
- Unsigned minified v0.7.2/code 9 APK SHA-256: `67a2663d53cedc30ad3395117636e02891f86b9a98f4ada4c6e552a4b000480c`
- Temporary smoke-signed APK SHA-256: `ea4da8e12ff7ffd680f3e7d5345a86bb7df5af0bee2e2d61fa8c2a8d51e517fd`
- Temporary smoke signer certificate SHA-256: `bba3818c88f1c0faf315b02dc725ca5f53a28fe4f5d001085b616973754d7b50`; it is not the project release signer
- `aapt` verified package `com.weekssa.opraeqforuapp`, versionName `0.7.2`, versionCode `9`, and minSdk 26; `apksigner verify` passed v2/v3
- Final Dalvik allocation was 47,111 KiB of 49,152 KiB, leaving 2,041 KiB free
- Full exact-source evidence, the earlier dirty-source smokes, and one separately recorded contaminated attempt are under `.unlazy/v0.7.2-autonomous-release/evidence/api26-final/`
- The clean run used committed source but a temporary signer. It is not CI x86_64 or release candidate provenance. Exact PR CI remains required.

## Remote promotion state

- Prior PR branch heads `af3f4c596e99e0cf8dce408b982c641c6898de6a` and `13bf1f20002f32da622392980eea145a0eb2a776` are superseded by current source head `e63fc4bf5be629135b0fd56449bad4c1b0bfd1b4`.
- Pull request: [#65](https://github.com/weekssa/OPRA-EQ-for-UAPP/pull/65), open, base `main` at `afed3dc90b5873218d5e333882528f8c5ddd54a2`
- All eight checks passed on exact PR head `af3f4c596e99e0cf8dce408b982c641c6898de6a`: Android build run `37082764610`, CodeQL run `37082764637`, Catalog run `37082764642`, Priority community run `37082764613`, dependency-submission run `37082761608`, and the associated UI/API 26 jobs. API 26 report artifact `11259384390` and emulator UI report artifact `11258319894` were downloaded and inspected. These results predate the current local recovery fix.
- Source head `e63fc4bf5be629135b0fd56449bad4c1b0bfd1b4`: all eight checks passed. Android build/UI/API26 run `37087067275`, Analyze Kotlin run `37087067259`, CodeQL check `111100633597`, Catalog run `37087067266`, Priority community run `37087067347`, and dependency submission run `37087067174`. The ledger commit will create a docs-only descendant that needs fresh exact-head CI and review.
- Independent review of `af3f4c59` found replay risk in the post-cycle restore path. Review of `13bf1f20` found the same risk in `restoreBeforeCommitOrFail()`. Both paths now persist terminal `RESTORATION_ATTEMPTED` before their first baseline write; if the checkpoint fails, no baseline write is sent.
- Full local validation on `e63fc4bf`: 731 JVM tests, zero failures/errors/skips; lint 0 errors, 111 warnings, 2 hints; debug/release assembly and R8 mapping pass. The focused EW300 suite passes 17/17. Fresh exact-head CI and independent follow-up review remain pending.
- Active `Protect main` ruleset: prevents deletion and non-fast-forward updates, with no bypass actors. It defines no required reviewer or named status checks; the task's CI and independent-review gates still apply.
- Merge SHA: none
- Signed candidate workflow/run/artifact: none
- Release APK SHA-256: none
- Release signer certificate SHA-256: expected pinned project identity, to be independently verified on the candidate
- Candidate manifest: none
- Remote `v0.7.2` tag: absent at the last remote check
- GitHub Release: absent at the last remote check

Update this file after every branch push, PR/CI change, merge, candidate, tag, publication, and public asset verification. Never copy a previous candidate checksum or signer verification forward as evidence for a changed source.
