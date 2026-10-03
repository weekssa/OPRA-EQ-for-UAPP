# Resume v0.7.2

MISSION: v0.7.2 stabilization and release

CURRENT PHASE: PR #65 exact snapshot `3ccf6728a9edd6c85a76ece1f5e71bc93fa37b74` passed all eight GitHub checks and independent review. The tracked pre-merge ledger sync records this snapshot and creates a docs-only descendant that needs fresh exact-head gates.

CURRENT WORKTREE: `/Users/stephenweeks/.codex/worktrees/a79b/OPRA-EQ-for-UAPP`

CURRENT BRANCH: `codex/v0.7.2-stabilization`

BASELINE: v0.7.1 -> `c48f6a5daa08a5e03475b2e415fe80b41d3357db`, versionName 0.7.1, versionCode 8

CURRENT HEAD: resolve the live branch and PR #65 head before acting. The latest fully verified exact-head snapshot before this ledger sync is `3ccf6728a9edd6c85a76ece1f5e71bc93fa37b74`.

VERSION: 0.7.2 / 9

LAST VERIFIED COMPLETION: exact PR head 3ccf6728 passed all eight checks: Android build, API 35 UI instrumentation (25/25), API 26 x86_64 cold-install/catalog readiness/survival, Analyze Kotlin, CodeQL, catalog currentness, priority community, and dependency submission. Independent review returned PASS with no actionable P0-P2 issue or unsupported release claim. Exact-run artifacts: UI `11262353847`, API 26 `11261474879`, lint `11262258835`, debug APK `11262004385` (test evidence only). Full Gradle validation on source `e63fc4bf` passed 731 JVM tests, lint, debug/release assembly, and R8 mapping.

NEXT EXACT ACTION: resolve the live PR head and refresh all eight checks plus independent review on that exact head. Merge normally only after both gates pass and branch protection allows it.

CURRENT FAILURE: NONE. The stale restart instruction reported on `adf871f2` was fixed in `3ccf6728`. Both EW300 restoration replay findings are fixed and regression-tested.

LAST GREEN GATES: all eight GitHub checks and independent review passed on exact PR head `3ccf6728a9edd6c85a76ece1f5e71bc93fa37b74`. Android run `37090841176`; Analyze Kotlin/CodeQL workflow run `37090841130`; CodeQL status `111111725873`; dependency submission `37090838488`; Catalog `37090841097`; Priority community `37090841101`.

ACTIVE PR: https://github.com/weekssa/OPRA-EQ-for-UAPP/pull/65

ACTIVE CI RUN: resolve live status from PR #65; the next pushed ledger head requires fresh exact-head checks.

ACTIVE RELEASE RUN: NONE

ARTIFACT STATUS: API 35 and API 26 exact-head reports were downloaded and inspected for snapshot `3ccf6728`. No signed candidate, v0.7.2 tag, or GitHub Release exists.

HUMAN ACTION REQUIRED: NONE
