# v0.7.2 final report

STATUS: IN PROGRESS

MISSION: v0.7.2 stabilization completed: NO

BASELINE: v0.7.1 SHA c48f6a5daa08a5e03475b2e415fe80b41d3357db; versionName 0.7.1; versionCode 8

WORKTREE: /Users/stephenweeks/.codex/worktrees/a79b/OPRA-EQ-for-UAPP

RELEASE BRANCH: codex/v0.7.2-stabilization

OWNER PRIMARY CHECKOUT MODIFIED: NO

RELEASE: versionName 0.7.2; versionCode 9; release SHA TBD; tag TBD; GitHub Release URL TBD

UNLAZY: located /Users/stephenweeks/.agents/skills/unlazy/SKILL.md; read and used; checkpoint path .unlazy/v0.7.2-autonomous-release; final state TBD

SKILLS: android-skills:android-dev, android-skills:android-testing, android-skills:android-debugging; other relevant skills TBD

TOOLS: inventory TBD; installed tools TBD; SDK/emulators TBD

INTERNET RESEARCH: topic/source/evidence TBD

FIXES: CanonicalCatalogRepository now streams cached/downloaded JSON from disk to avoid an API 26 startup OOM; regression coverage is in progress

INVESTIGATED BUT NOT CHANGED: TBD

DSP VERIFICATION: oracle/cases/discrepancy/headroom and fidelity outcomes/production change TBD

LOCAL TESTING: exact v0.7.1 baseline debug/JVM/lint/release/R8 passed; JVM 713 tests, 0 failures; lint 0 errors, 114 warnings, 2 hints; API 35 instrumentation 25 passed; baseline API 26 reproduced an OOM; fixed-source debug/JVM/lint/release/R8 passed with JVM 714 tests; focused catalog suite and cold API 26 smoke pass; full candidate/workflow/Python/diff checks pending

GITHUB: PR URL TBD; CI TBD; merge SHA TBD; post-merge verification TBD; candidate run URL TBD

RELEASE SECURITY: package/version/signer/APK hash/manifest/public APK verification TBD

HARDWARE SAFETY: physical DAC writes NO; Flash/Save/reset on physical hardware NO; USB protocol behavior changed TBD

CONTEXT / RECOVERY: all ledger documents and final remote checkpoint status TBD

RECOVERY EVENTS: resolved API 26 startup OOM by decoding the canonical catalog as a stream; details and evidence in BLOCKERS.md and TEST_MATRIX.md

REMAINING NON-BLOCKING ITEMS: TBD

HUMAN ACTION REQUIRED: NONE unless a release gate is impossible without a distinct human action
