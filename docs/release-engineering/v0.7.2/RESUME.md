# Resume v0.7.2

MISSION: v0.7.2 stabilization and release

CURRENT PHASE: second EW300 replay path fixed and pushed; exact-head CI is running and independent review is pending

CURRENT WORKTREE: /Users/stephenweeks/.codex/worktrees/a79b/OPRA-EQ-for-UAPP

CURRENT BRANCH: codex/v0.7.2-stabilization

BASELINE: v0.7.1 -> c48f6a5daa08a5e03475b2e415fe80b41d3357db

CURRENT HEAD: Tested production source commit is e63fc4bf5be629135b0fd56449bad4c1b0bfd1b4. The release ledger may be a docs-only descendant; resolve the live checkout and PR SHA before relying on CI or review results.

VERSION: 0.7.2 / 9

LAST VERIFIED COMPLETION: exact source commit e63fc4bf5be629135b0fd56449bad4c1b0bfd1b4 passed a forced full Gradle run: 731 JVM tests, 0 failures/errors/skips; lint 0 errors, 111 warnings, 2 hints; debug/release assembly; and R8 mapping verification. The EW300 persistence suite passed 17/17, including both restoration paths and qualifier recreation after failed checkpoints. Earlier API 35, Python, and API 26 local evidence remains tied to its recorded source. All eight GitHub checks passed on source head e63fc4bf; the docs-only ledger commit requires a fresh run.

NEXT EXACT ACTION: if the ledger sync is still uncommitted, finish its diff check and push normally; then resolve the exact PR head, complete its CI and independent review, and repair any finding before merge.

CURRENT FAILURE: independent reviews of heads af3f4c59 and 13bf1f20 found two P2 replay windows in separate baseline-restoration paths. Commits 40b4f5d8 and e63fc4bf now checkpoint RESTORATION_ATTEMPTED before writes in both paths, with regression coverage. Exact-head CI and review on e63fc4bf remain pending.

LAST GREEN GATES: all eight checks pass on source head e63fc4bf. Android/API26/UI run 37087067275, Analyze Kotlin run 37087067259, CodeQL check 111100633597, Catalog run 37087067266, Priority community run 37087067347, and dependency submission run 37087067174. Prior head af3f4c59 artifacts are historical and predate both recovery fixes.

ACTIVE PR: https://github.com/weekssa/OPRA-EQ-for-UAPP/pull/65

ACTIVE CI RUN: All checks passed on source head e63fc4bf. The pending docs-only descendant triggers a fresh run; refresh live status after it is pushed.

ACTIVE RELEASE RUN: NONE

ARTIFACT STATUS: no official v0.7.2 candidate; remote tag and GitHub Release remain absent. Local API 26 APK and temporary signer are smoke-only.

HUMAN ACTION REQUIRED: NONE
