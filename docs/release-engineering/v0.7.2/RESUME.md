# Resume v0.7.2

MISSION: v0.7.2 stabilization and release

CURRENT PHASE: repair the exact-head review finding; local validation passes, and the fix is being prepared for a fresh PR-head review and CI run

CURRENT WORKTREE: /Users/stephenweeks/.codex/worktrees/a79b/OPRA-EQ-for-UAPP

CURRENT BRANCH: codex/v0.7.2-stabilization

BASELINE: v0.7.1 -> c48f6a5daa08a5e03475b2e415fe80b41d3357db

CURRENT HEAD: PR #65 is at af3f4c596e99e0cf8dce408b982c641c6898de6a. Local EW300 source/test changes and this recovery-ledger update are uncommitted. Refresh PR head before any remote action.

VERSION: 0.7.2 / 9

LAST VERIFIED COMPLETION: the local working tree based on af3f4c59 passed the full Gradle gate: 729 JVM tests, 0 failures/errors/skips; lint 0 errors, 111 warnings, 2 hints; debug/release assembly; and R8 mapping verification. The EW300 persistence suite passed 15/15, including the failed-checkpoint-after-restoration regression. Existing API 35, Python, API 26, and remote CI evidence remains bound to its recorded earlier source/head.

NEXT EXACT ACTION: review the complete source and ledger diff, commit the fix and recovery record, push normally, then refresh PR #65 and require all checks plus a new independent review on the exact pushed head.

CURRENT FAILURE: the independent review of PR head af3f4c59 returned FAIL with one P2: a failed UNCERTAIN checkpoint after a rejected restoration write could leave TEMPORARY_COMMITTED persisted and replay the restoration after restart. A durable RESTORATION_ATTEMPTED marker now precedes that write. Local regression and full Gradle checks pass; exact-head CI/review remain pending.

LAST GREEN GATES: prior PR head af3f4c596e99e0cf8dce408b982c641c6898de6a passed all eight checks: Android build, emulator UI, API 26 x86_64 smoke, CodeQL, catalog currentness, priority community coverage, dependency submission, and Analyze Kotlin. The API 26 artifact was downloaded and inspected. This local fix is not included in those results.

ACTIVE PR: https://github.com/weekssa/OPRA-EQ-for-UAPP/pull/65

ACTIVE CI RUN: NONE for the uncommitted fix. Previous exact-head runs are recorded in ARTIFACTS.md.

ACTIVE RELEASE RUN: NONE

ARTIFACT STATUS: no official v0.7.2 candidate; remote tag and GitHub Release remain absent. Local API 26 APK and temporary signer are smoke-only.

HUMAN ACTION REQUIRED: NONE
