# F1: Retire the stale v0.7.2 Unlazy checkpoint

## Finding

The local checkpoint `.unlazy/v0.7.2-autonomous-release/` still instructs an operator to publish v0.7.2. `RESUME.md` says to wait for candidate run `37099431204` and then dispatch the signed-candidate promotion, verify the tag and public assets, and update the release records. `STATE.md` still calls promotion run `37099991693` active. `PLAN.md` still requires tag creation and publication, while the root `GATES.md` leaves public release and final recovery gates pending. These are superseded instructions: the release has already been published.

## Identity and current release evidence

Before reading the checkpoint, its worktree was confirmed at `/Users/stephenweeks/.codex/worktrees/a79b/OPRA-EQ-for-UAPP`, branch `codex/v0.7.2-stabilization`, HEAD `3a692d787407a3556b0d4da2a9f1463995960b87`, tracking `origin/codex/v0.7.2-stabilization`. Tracked files were clean. The worktree contained untracked `.DS_Store`, `.unlazy/v0.7.2-autonomous-release/`, `postrelease_audit_v0.7.2/`, `postrelease_audit_v0.7.2 2/`, and `postrelease_audit_v0.7.2.zip`.

The maintained release checklist and runbook record v0.7.2 as published and latest from exact source `b8e90b9b53fc63ea00fefa499d7d4bd6ce4d55ea`. Read-only remote refresh confirmed the annotated `v0.7.2` tag dereferences to that commit; GitHub Release ID `402346895` is non-draft, non-prerelease, and latest, published at `2026-10-03T05:32:08Z`. PR #66 is merged and GitHub reports seven checks passed. The successful candidate is run `37099431204`, artifact `11265653006`; successful promotion is run `37099991693`. See [the public release checklist](../../PUBLIC_RELEASE_CHECKLIST.md), [the v0.7.2 final report](../v0.7.2/FINAL_REPORT.md), and [the v0.7.2 runbook closeout](../../CHATGPT_PROJECT_RUNBOOK.md).

## Unlazy status and lifecycle support

Ran only the installed Unlazy `gate-check.mjs --scope v0.7.2-autonomous-release --status` command. It executed no `CHECK:` lines and changed no checkpoint file. The scoped result is **37 met, 27 unmet**. The root ledger reports G1 unchecked, G2 checked but automatic evidence stale or unbound, and G3/G4 unchecked. Among the remaining stale requirements are exact-head re-verification, tag creation, publication, and final report completion. The `status.log` ends with the 05:29Z entry saying promotion is active; it has no publication closeout entry. `dispatch.json` records only the initial `ready-1` wave, which is complete.

The installed Unlazy CLI documents `--status`, `--reverify`, `--approve`, and pipeline actions for claim, lease release, log append, bind, and list scopes. It provides no archive, retire, or close command. `--release` releases serialized ownership leases; it does not archive a scope. The documented normal scope release is reserved until every leaf and dispatch wave has settled and branch, root, and final aggregate verification has run. The current stale ledger does not meet that condition.

## Evidence to preserve as history

Keep the checkpoint's ledgers, per-leaf and node gate files, `status.log`, `dispatch.json`, and all evidence files unchanged. In particular, retain the recorded branch/SHA-specific local and remote results, the contaminated first API 26 attempt alongside its clean rerun, the earlier failed no-tag promotion `37096259477`, superseded candidate records, and the successful candidate/promotion provenance. Preserve interim statements that no tag or release yet existed as dated snapshots; newer release evidence supersedes them without making those snapshots false. Do not mark old gates green, replace handwritten or stale gate evidence with the later release result, or claim that checks for an earlier SHA certify the published source.

## Primary remediation closeout

The read-only audit above made no checkpoint changes. The primary agent then retired the stale operator handoff in the old worktree, because the existing `RESUME.md` was still an actionable instruction to publish an already-published release and the installed CLI has no archive or retirement operation. The old worktree remains at `/Users/stephenweeks/.codex/worktrees/a79b/OPRA-EQ-for-UAPP` on `codex/v0.7.2-stabilization`, with its tracked files clean.

The former `RESUME.md` was preserved byte-for-byte as `RESUME-PREPUBLICATION-2026-10-03.md` (SHA-256 `1b5a71984d2224e7bc195b6a6062b364a47fa593a4acc75f3560bf9a4aa741b8`). `RESUME.md` now marks that release-publication checkpoint closed, points to the authoritative maintained release records, and says no v0.7.2 publication action remains (SHA-256 `9953c0f6f053ef7a8fe21d56b553db5900e91e8a6e8341a25c29bc10fb7f29f2`). The old `STATE.md`, `PLAN.md`, `GATES.md`, per-gate files, `status.log`, `dispatch.json`, and other evidence were not changed or marked green. The retired scope still reports 37 met and 27 unmet historical gates; those stale counts remain visible as history and are not a current release blocker.

All unrelated user-owned untracked files in the old worktree were preserved: `.DS_Store`, both `postrelease_audit_v0.7.2` directories, and `postrelease_audit_v0.7.2.zip`. Do not rerun or release the old scope's leases, dispatch its stale promotion actions, or remove those local files as part of this remediation. This closeout supersedes its prepublication resume instructions without falsifying gate history.
