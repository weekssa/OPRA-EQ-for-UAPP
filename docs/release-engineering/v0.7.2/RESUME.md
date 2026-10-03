# Resume v0.7.2

MISSION: v0.7.2 stabilization and release

CURRENT PHASE: PR #65 merged; post-merge checks passed. Official candidate `37095180116` / artifact `11264251526` passed independent verification. Promotion run `37096259477` passed API 35 upgrade but failed in `create-candidate-tag` because the publisher compared a prefixed GitHub artifact digest to an unprefixed value. No tag/release was created; publish was skipped.

CURRENT WORKTREE: `/Users/stephenweeks/.codex/worktrees/a79b/OPRA-EQ-for-UAPP`

CURRENT BRANCH: `codex/v0.7.2-stabilization`

BASELINE: v0.7.1 -> `c48f6a5daa08a5e03475b2e415fe80b41d3357db`, versionName 0.7.1, versionCode 8

CURRENT HEAD: merged `main` is `a60411bebfdbd1cea4218d3bde45013bb7ed26a9`. Its tree exactly matches reviewed and green PR head `e7f2fc937e9b265770296dbdc4cbb40a4e5e13c9`. Recovery branch currently ends at `e7f2fc93`; this status checkpoint must be committed and pushed there.

VERSION: 0.7.2 / 9

LAST VERIFIED COMPLETION: all eight GitHub checks passed on exact PR head `e7f2fc937e9b265770296dbdc4cbb40a4e5e13c9`; independent review PASS with no P0-P2 finding. PR #65 merged normally as `a60411bebfdbd1cea4218d3bde45013bb7ed26a9`. Post-merge local Gradle validation passed 731 tests, 0 failures/errors/skips; lint 0 errors, 111 warnings, 2 hints; debug and release assembly; R8 mapping verification.

NEXT EXACT ACTION: finish independent review of the working-tree digest fix, stage only the ten intended tracked files, commit and push normally, rebase the recovery branch topology by merging `origin/main` if needed, and open/update the promotion-fix PR. Complete CI/review and merge normally, then build and verify a fresh candidate from the corrected exact main SHA before promotion.

CURRENT FAILURE: promotion run `37096259477` failed before tag creation because the immutable artifact digest prefix was normalized on only one side. The working tree now normalizes and validates both sides in the tag and publish commands. Focused tests pass 39/39, full Python tools pass 238/238 under bundled Python 3.12, and `check-contract` passes. Independent review, PR checks/merge, a fresh candidate, and a new promotion run remain.

LAST GREEN GATES: all eight exact-head checks on `e7f2fc93`: Android build/UI/API26 run `37092536385`; Analyze Kotlin and CodeQL `37092536382`; Catalog `37092536383`; Priority community `37092536380`; dependency submission `37092533146`. API35 UI report `11263771217` records 25/25; API26 report `11262861589`; lint report `11263656445`.

ACTIVE PR: https://github.com/weekssa/OPRA-EQ-for-UAPP/pull/65 (merged)

ACTIVE CI RUN: no required PR check is pending; resolve post-merge `main` status live before promotion.

ACTIVE RELEASE RUN: failed Promote Signed Release Candidate `37096259477`, https://github.com/weekssa/OPRA-EQ-for-UAPP/actions/runs/37096259477; new corrected-source candidate/promotion not yet dispatched.

ARTIFACT STATUS: candidate run `37095180116`, artifact `11264251526`, archive SHA-256 `1da409dcf47368ec254a5432e7c1d316473920c28e27c19416b10cc8e29969d9`, and APK SHA-256 `f3afaa102a31491286828faa37cfe1454853721d1e4aa736da57bf4919e89ded` were verified for main `a60411bebfdbd1cea4218d3bde45013bb7ed26a9`. API 35 upgrade passed in promotion run `37096259477`, but the tag step failed; no tag or release exists. A corrected source merge requires a new candidate.

HUMAN ACTION REQUIRED: NONE
