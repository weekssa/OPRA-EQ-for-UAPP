# GitHub Triage and Tracking

Repository: `weekssa/OPRA-EQ-for-UAPP`\
Default branch: `main`\
Latest fetched main: `35e91c2170786ae4afc7a6679ba9ba34a01511a8`

## Foundation tracking issue

[Issue #99](https://github.com/weekssa/OPRA-EQ-for-UAPP/issues/99) tracks this mission. Draft [PR #100](https://github.com/weekssa/OPRA-EQ-for-UAPP/pull/100) is open from `chore/repository-foundation-v0.8.3` to `main`. Its latest reviewed remote SHA, exact-head CI, and standard check states are recorded live in issue #99 and PR #100; do not use the historical snapshots below to infer current PR status.

## Current issues and pull requests

| Item | Classification | Evidence / recommended action |
| --- | --- | --- |
| Issue #99, repository foundation | Active tracking issue | Created 2026-10-20 after a repository-scoped search returned no matching issue. Keep open through owner review. |
| Closed issue #2 | Historical discussion | v0.3-era; leave unchanged |
| Closed issue #9 | Historical source-currentness tracking | Leave unchanged |
| #101 | Open, P1 security follow-up | Triage all 56 Dependabot alerts, 3 critical first; runtime reachability unknown; no dependency changes in PR #100. |
| #102 | Open, P2 CI follow-up | Diagnose ordinary push workflow failure with zero jobs/no logs. |
| #103 | Open, P2 CI reliability follow-up | Diagnose same-SHA emulator Activity-focus flake without masking tests. |
| #104 | Open, P2 owner/device follow-up | Read-only installed v0.8.3 provenance when device and fresh authorization are available. |
| #105 | Open, P3 backup follow-up | External backup availability, retention, and restore cadence. |
| #106 | Open, P3 documentation governance follow-up | Reconcile obsolete external Codex/ChatGPT instructions; preserve unique history. |
| #107 | Open, P2 repository stewardship follow-up | Review unmerged historical branch disposition; no automatic deletion or merge. |
| PR #96, private JA11 settings persistence beta | Active unresolved physical failure | `FINAL_EQ_MISMATCH`; Save `0x19`; original-state restoration is not verified per the owner report recorded on the PR and its cited validation ledger. Preserve draft. Do not replay mutation or merge. Follow current PR evidence; owner/device gate remains separate. |
| PR #93, JA11 post-Flash diagnostic trace | Historical/active draft investigation | Physical reproduction/root cause remains incomplete per current report. Preserve; no hardware action in this mission. |
| PR #97, JA11 explicit defaults | Merged release integration | Merged at `899755507f812c940f3ab9381a856c80c6030885`; retain as release history |
| PR #98, v0.8.3 documentation closeout | Merged documentation | Merged at `0c8a594b07549aa7d7e8d9cd1fbf27004d48315d`; retain |
| PRs #17–#24, #33, #37, #55 | Open historical EW300/v0.7/Favorite candidate and evidence drafts | Preserve exact historical scope and limits; do not close, merge or modify during this cleanup |

## Current CI/release evidence

Exact PR-head history captured on 2026-10-20:
- On prior PR head `e955f62db0bb7ab96182ee3964aa579a6cb9d186`, exact-head run [39507302003](https://github.com/weekssa/OPRA-EQ-for-UAPP/actions/runs/39507302003) attempt 2 passed all four jobs: `build`, `CodeQL exact head`, `min-api-smoke`, `emulator-ui-test`. No jobs were skipped. Attempt 1 failed only the emulator UI job, 8/72 tests because Activity focus was false (0 skipped); same-SHA attempt 2 passed all 72. No source change occurred between attempts.
- PR check-runs `Analyze Kotlin`, `CodeQL`, and `submit-gradle` were all successful on that prior head, with none pending or skipped.
- Ordinary push run [39507297134](https://github.com/weekssa/OPRA-EQ-for-UAPP/actions/runs/39507297134) on that same SHA failed with zero jobs and no logs (API 404). Cause is unknown. It is not a job-level Android failure and is not a pass.
- These results apply only to `e955f62…`. The latest reviewed head was independently validated; issue #99 and PR #100 contain its exact SHA and current successful, failed, skipped, pending, and no-job outcomes. Do not add a commit only to repeat CI status.


- Public v0.8.3 release is immutable, non-draft/non-prerelease, `/releases/latest`; tag peels to `899755507f812c940f3ab9381a856c80c6030885`.
- Historical exact-main Android CI for release source: run `38648012952`, all four jobs passed on exact `899755...`.
- Current main source-health workflow: run `39471956137` succeeded.
- Current main automatic dependency submission: run `39466646926` succeeded.
- Dependabot run `39467268390` failed on current main. Its job log reports `dependency_not_found`: security-update dependencies were absent from the dependency snapshot and no configured Gradle group matched. This is an automation/snapshot issue, not an Android app CI failure. Investigate separately.
- GitHub's read-only alert API reports 56 open Maven Dependabot alerts on current main: 3 critical, 22 high, 28 moderate, 3 low. All list `settings.gradle.kts` as the manifest. This inventory does not prove runtime APK inclusion or reachability; perform a separate dependency review.
- No current CI conclusion on main should be generalized from the source-health/dependency jobs alone. Refresh exact relevant check runs before final delivery.

## Required update

Issue #99 and draft PR #100 exist. Final review recommends GO for owner review/merge consideration; explicit owner approval remains mandatory. Separate actionable follow-ups are tracked in #101 (P1 security triage), #102 (P2 zero-job push workflow), #103 (P2 emulator Activity focus), #104 (P2 physical device provenance with owner/device gate), #105 (P3 external backup retention), #106 (P3 external Codex/ChatGPT instruction reconciliation), and #107 (P2 historical branch disposition). Each links back to #99. No dependency or application remediation is in PR #100. Never close an existing issue, modify an unrelated PR, or merge this PR without owner approval.

## Open PR snapshot (2026-10-20)

| PR | State | Head | Classification / action |
| --- | --- | --- | --- |
| #96 | Open, draft | `codex/ja11-settings-persistence-beta` `5b79d53778eac6ba9e18b75c729b6df5e4037f6d` | Active private JA11 beta; physical mismatch and restoration gap, hold. |
| #93 | Open, draft | `codex/ja11-postflash-diagnostic-trace` `f6bc1d6d87b9618bc35354f33103504c753c7f6a` | Diagnostic investigation; root cause/physical reproduction pending, owner phone authorization revoked. |
| #55 | Open, draft | `codex/aful-favorite-candidate-record` `e646717605d8e8cd8575beb17cc682556521925f` | Historical v0.7.1 Favorite evidence. |
| #37 | Open, draft | `codex/v0.7-release-readiness-closeout` `73441137eb48056c2d9b1628d49c186a21a25991` | Historical v0.7 beta provenance. |
| #33 | Open, not draft | `codex/ew300-closeout-docs` `330fe81630d8694abbef454e52c9f6daecbc1d2d` | Historical EW300 closeout record; no state change. |
| #24 | Open, draft | `codex/v07-handoff-fixes-20260923` `403e404a13911a305a1a91f1424a9b0766ffeefc` | Historical v0.7 source correctness; release blockers in its description. |
| #22 | Open, draft | `feature/v0.7-ew300-recovery` `ddd4116f4e094fbe87a85d197b856fb0d13f745e` | Historical EW300 recovery candidate. |
| #21 | Open, draft | `ew300-prelaunch-beta` `8630cc79b6aef0cb1c4078ef5bbd2d98e6def958` | Historical EW300 beta candidate. |
| #20 | Open, draft | `v0.6.0-ew300-flash-fix` `4b806fe8f521615a0bafad3ffe497f68636996c4` | Historical EW300 gain-aware candidate. |
| #19 | Open, not draft | `fix/ew300-flash-reconnect` `67528359d70d09ff2c4b5191ce22516e194b06d7` | Historical EW300 reconnect change; preserve pending owner review. |
| #18 | Open, draft | `ew300-discovery-candidate` `03c02d17eaf16eb27239d2671cdd476a38d6d449` | Historical EW300 implementation candidate. |
| #17 | Open, draft | `feature/v0.7-ew300-dsp` `c8b88e05c90de0029145a2296725d1d13c0bfe1d` | Historical EW300 descriptor discovery candidate. |

The open-PR search returned no other open items at this snapshot. Search for regular issues returned no existing open issue before #99 was created. Re-run the repository-scoped search before closing the maintenance task.
