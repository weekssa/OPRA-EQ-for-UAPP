# Resume v0.7.2

MISSION: v0.7.2 stabilization and release

CURRENT PHASE: local gates pass; prepare the coherent checkpoint, PR, and remote CI

CURRENT WORKTREE: /Users/stephenweeks/.codex/worktrees/a79b/OPRA-EQ-for-UAPP

CURRENT BRANCH: codex/v0.7.2-stabilization

BASELINE: v0.7.1 -> c48f6a5daa08a5e03475b2e415fe80b41d3357db

CURRENT HEAD: `d37e114d466d681138140a0280a7c988235c3c94` is the latest pushed checkpoint. The v0.7.1 commit remains an ancestor; no history was rewritten. The complete v0.7.2 candidate is still in the working tree and has not been committed.

VERSION: 0.7.2 / 9

LAST VERIFIED COMPLETION: full Gradle suite passed: 727 JVM tests, lint, debug/release assembly; R8 verified. API 35 instrumentation passed 25/25. Python tools passed 235 tests and the release contract, catalog registry, favorite-sample, actionlint, ShellCheck, and diff checks passed. A fresh API 26 ARM64/48 MiB cold install rendered the manufacturer, 1MORE product, and oratory1990 profile lists, remained resumed for 60 seconds, and had no AndroidRuntime errors. The APK was built from the dirty worktree and used a temporary smoke signer; exact committed-source rerun and CI x86_64 remain pending.

NEXT EXACT ACTION: reconcile the independent no-findings review, finish and commit the release ledger, push without force, and create the PR. Then wait for exact-head GitHub checks/review before merge.

CURRENT FAILURE: none reproduced on the latest dirty worktree source. The API 26 process used 47,094 KiB of its 49,152 KiB Dalvik heap after the observation window; CI x86_64 and post-commit checks must confirm the candidate.

LAST GREEN GATES: pristine v0.7.1 baseline; current 727-test JVM suite; lint with 0 errors; debug/release assembly; R8 mapping; API 35 instrumentation 25/25; API 26 manufacturer/product/profile render and 60-second resumed-process observation; 235 Python tests; release contract; catalog validations; actionlint; ShellCheck; git diff --check; independent read-only review found no confirmed finding

ACTIVE PR: NONE

ACTIVE CI RUN: NONE

ACTIVE RELEASE RUN: NONE

ARTIFACT STATUS: no signed release candidate. The local API 26 APK and temporary signer are smoke-only, not release provenance. Remote v0.7.2 tag is absent and `gh release view v0.7.2` reports not found.

HUMAN ACTION REQUIRED: NONE
