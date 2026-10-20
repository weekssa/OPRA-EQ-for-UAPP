# GitHub Triage and Tracking

Repository: `weekssa/OPRA-EQ-for-UAPP`\
Default branch: `main`\
Latest fetched main: `35e91c2170786ae4afc7a6679ba9ba34a01511a8`

## Foundation tracking issue

[Issue #99](https://github.com/weekssa/OPRA-EQ-for-UAPP/issues/99) was created after the read-only search found no matching tracking issue. It tracks this mission only. The cleanup branch is local at this checkpoint; add its PR URL after the draft PR is created.

## Current issues and pull requests

| Item | Classification | Evidence / recommended action |
| --- | --- | --- |
| Issue #99, repository foundation | Active tracking issue | Created 2026-10-20 after a repository-scoped search returned no matching issue. Keep open through owner review. |
| Closed issue #2 | Historical discussion | v0.3-era; leave unchanged |
| Closed issue #9 | Historical source-currentness tracking | Leave unchanged |
| PR #96, private JA11 settings persistence beta | Active unresolved physical failure | `FINAL_EQ_MISMATCH`; Save `0x19`; original-state restoration is not verified per the owner report recorded on the PR and its cited validation ledger. Preserve draft. Do not replay mutation or merge. Follow current PR evidence; owner/device gate remains separate. |
| PR #93, JA11 post-Flash diagnostic trace | Historical/active draft investigation | Physical reproduction/root cause remains incomplete per current report. Preserve; no hardware action in this mission. |
| PR #97, JA11 explicit defaults | Merged release integration | Merged at `899755507f812c940f3ab9381a856c80c6030885`; retain as release history |
| PR #98, v0.8.3 documentation closeout | Merged documentation | Merged at `0c8a594b07549aa7d7e8d9cd1fbf27004d48315d`; retain |
| PRs #17–#24, #33, #37, #55 | Open historical EW300/v0.7/Favorite candidate and evidence drafts | Preserve exact historical scope and limits; do not close, merge or modify during this cleanup |

## Current CI/release evidence

- Public v0.8.3 release is immutable, non-draft/non-prerelease, `/releases/latest`; tag peels to `899755507f812c940f3ab9381a856c80c6030885`.
- Historical exact-main Android CI for release source: run `38648012952`, all four jobs passed on exact `899755...`.
- Current main source-health workflow: run `39471956137` succeeded.
- Current main automatic dependency submission: run `39466646926` succeeded.
- Dependabot run `39467268390` failed on current main. Its job log reports `dependency_not_found`: security-update dependencies were absent from the dependency snapshot and no configured Gradle group matched. This is an automation/snapshot issue, not an Android app CI failure. Investigate separately.
- GitHub's read-only alert API reports 56 open Maven Dependabot alerts on current main: 3 critical, 22 high, 28 moderate, 3 low. All list `settings.gradle.kts` as the manifest. This inventory does not prove runtime APK inclusion or reachability; perform a separate dependency review.
- No current CI conclusion on main should be generalized from the source-health/dependency jobs alone. Refresh exact relevant check runs before final delivery.

## Required update

Issue #99 exists; add the draft PR URL here and to `STATUS.md` after creation. Keep issue progress factual and link local docs by repository path. Never close an existing issue, modify an unrelated PR, or merge this PR without owner approval.

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
