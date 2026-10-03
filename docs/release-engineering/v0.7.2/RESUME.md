# Resume v0.7.2

MISSION: v0.7.2 stabilization and release

CURRENT PHASE: PR #65 is open; initial-head checks passed; verify the status-sync head and obtain independent review

CURRENT WORKTREE: /Users/stephenweeks/.codex/worktrees/a79b/OPRA-EQ-for-UAPP

CURRENT BRANCH: codex/v0.7.2-stabilization

BASELINE: v0.7.1 -> c48f6a5daa08a5e03475b2e415fe80b41d3357db

CURRENT HEAD: PR #65 initially opened at `c81f329b35773141ee7ba583468eaa9e8d7e009b`; this post-creation status checkpoint is documentation-only and advances the PR head when pushed. Refresh the live PR head before resuming. Product source was validated at `1c36349ca3edb69061a34b44d385670380f60512`. The v0.7.1 commit remains an ancestor.

VERSION: 0.7.2 / 9

LAST VERIFIED COMPLETION: exact commit `1c36349ca3edb69061a34b44d385670380f60512` passed the full Gradle suite: 727 JVM tests, lint, debug/release assembly, R8, and 25/25 API 35 instrumentation tests. Python tools passed 235 tests; release/catalog contracts, actionlint, ShellCheck, and diff checks passed. A freshly wiped API 26 ARM64/48 MiB install rendered manufacturer, 1MORE product, and oratory1990 profile lists, remained resumed/alive for 60 seconds, and had no app runtime errors. All eight PR checks, including API 26 x86_64, passed on initial PR head `c81f329b`; this docs-only status checkpoint needs a fresh exact-head run. Local smoke artifact and signer are temporary and not release provenance.

NEXT EXACT ACTION: refresh PR #65's live head and checks after this status checkpoint; complete its exact-head checks and independent review, repair any owned failure without weakening gates, then merge under repository policy and reverify merged main.

CURRENT FAILURE: none reproduced locally or on the initial PR head. The API 26 process used 47,111 KiB of its 49,152 KiB Dalvik heap after the local observation window; refresh checks for this status-sync head.

LAST GREEN GATES: pristine v0.7.1 baseline; exact candidate 727-test JVM suite; lint with 0 errors; debug/release assembly; R8 mapping; API 35 instrumentation 25/25; API 26 cold install, manufacturer/product/profile render and 60-second resumed-process observation; 235 Python tests; release contract; catalog validations; actionlint; ShellCheck; git diff check; independent read-only review found no confirmed finding

ACTIVE PR: https://github.com/weekssa/OPRA-EQ-for-UAPP/pull/65 (PR #65, base main; refresh live head)

ACTIVE CI RUN: All eight initial checks passed on PR head c81f329b, including Android build, API 26 x86_64, Android UI, CodeQL, catalog, community coverage, and dependency submission. This documentation checkpoint advances the head; refresh GitHub for its rerun status.

ACTIVE RELEASE RUN: NONE

ARTIFACT STATUS: no signed release candidate. The local API 26 APK and temporary signer are smoke-only, not release provenance. Remote v0.7.2 tag is absent and `gh release view v0.7.2` reports not found.

HUMAN ACTION REQUIRED: NONE
