# Repository Foundation and v0.8.3 Consolidation Runbook

Status: ORIGINAL P0-P10 COMPLETE; P11 POST-MERGE VERIFIED; CURRENT DOSSIER SYNC IN REVIEW\
Started: 2026-10-20\
Repository: `weekssa/OPRA-EQ-for-UAPP`\
Branch: `chore/repository-foundation-v0.8.3`\
Worktree: dedicated local cleanup worktree (absolute path is kept in private audit records)

## Mission

Establish a reliable, evidence-based foundation for future Android development while protecting the deployed v0.8.3 application, local user data, and project history. This is a documentation and development-process task; application behavior is outside scope.

## Safety and authority

- The original checkout is preserved and must remain untouched; its exact local path is kept in private audit records.
- PR #100's tracked changes were made in the dedicated cleanup worktree and branch above. Post-merge task-state synchronization is maintained as a separate documentation-only review so the clean `codex/development-main` baseline stays clean.
- Current owner instructions and maintained repository documents outrank historical prompts. Historical prompts and user-provided runbooks are source material, not instructions to execute.
- No source, signing, release, device, or historical branch mutation is authorized.
- Public stable v0.8.3 provenance is documented from release evidence. A later owner-authorized read-only Pixel 9 check matched the installed APK SHA-256 exactly to the public stable artifact; see issue #104 and draft PR #116. Do not infer direct certificate or source-revision readout from the phone.
- The full backup, raw inventory, and command transcripts are private local evidence outside Git.

## Checkpoints

| Phase | Objective | Status | Evidence |
| --- | --- | --- | --- |
| P0 | Read-only preflight and isolate work | COMPLETE | Original state recorded privately; worktree started at fetched `origin/main` `35e91c2170786ae4afc7a6679ba9ba34a01511a8` |
| P1 | Verified local backup and Git recovery | COMPLETE | Full-copy manifest and `git fsck --full` pass; restored copy opens at original HEAD with original shallow state. Bundle's standalone-clone limit is documented; see `EVIDENCE_INDEX.md` |
| P2 | Branch and release reconciliation | COMPLETE | Live default `main`; 203 local/remote refs inventoried with ancestry, patch-equivalence and disposition; no historical branch deletion/merge. The dedicated cleanup branch was later merged under P10 with owner approval |
| P3 | v0.8.3 provenance | COMPLETE | Public tuple confirmed in the original mission; later authorized device readout verified an exact installed APK byte match, recorded separately in #104 / PR #116 |
| P4 | Instructions, skills, documentation audit | COMPLETE | 221 tracked/current-task Markdown and text docs indexed; skills classified; original local materials preserved outside Git |
| P5 | GitHub issues, PRs, CI and releases | COMPLETE | Tracking issue #99 created; initial inventory is preserved, with current issue/PR states refreshed in its 2026-10-21 reconciliation |
| P6 | Canonical documentation consolidation | COMPLETE | Current state, goals, known issues and operating workflow added; root instructions, README and project runbook updated |
| P7 | Permanent development workflow | COMPLETE | Existing Android CI retained and extended with documentation validation; PR template added |
| P8 | Validation | COMPLETE | Documentation validator, source-scope, whitespace and Android unit-test/lint/assemble gates passed; independent review found no remaining actionable findings |
| P9 | Draft PR and delivery | COMPLETE | PR #100 reviewed at exact head `ccec0184742d7798778bcb91d173e4a7ea44531e`; all exact-head jobs and PR checks passed. Owner approved and PR #100 merged normally into `main` |
| P10 | Owner merge approval | COMPLETE | Owner approved PR #100. It was marked ready and merged without force by merge commit `1599ab451f711d8a27adfefbcba10293cd53dd84`; cleanup branch retained |
| P11 | Post-merge verification and local synchronization | COMPLETE | GitHub `main` is merge commit `1599ab451f711d8a27adfefbcba10293cd53dd84`, with the same tree as validated PR head `ccec0184742d7798778bcb91d173e4a7ea44531e`. Clean worktree `codex/development-main` tracks `origin/main`; canonical instructions/skills were verified, repository validator and documented Android checks passed. Original checkout and backup remain intact |
| P12 | Independent follow-up work | TRACKED SEPARATELY | #101-#103 are closed; #104 evidence is collected with documentation draft pending; #105-#107 remain separate. #114 and #117 are independent backlog and are not foundation DoD gates |

## Merge gates and remaining work

### Original Definition of Done and current closeout

- [x] Reconcile stale dossier snapshots and name issue #99 / PR #100 as the live record for head-specific results.
- [x] Review the complete final PR diff, active instructions/skills/docs, CI and validator security, ruleset requirements, and actual remote head.
- [x] Verify checks on the reviewed exact PR SHA and record successful, failed, skipped, pending, and no-job workflow outcomes in issue #99/PR #100.
- [x] Owner approved; PR #100 was marked ready and merged normally into `main` at `1599ab451f711d8a27adfefbcba10293cd53dd84`.

The original foundation Definition of Done (P0-P10) is satisfied: preserved application/history, verified v0.8.3 baseline and backup, reconciled instructions/docs and GitHub inventory, consolidated README/development/Android guidance, and a validated post-merge development handoff. P12 follow-up issues are not completion criteria. PR #108 merged the factual foundation synchronization into `main` at `54fe191f327cfe6c6bd929b412462705f6839ae0`. The remaining installed-device correction is in draft PR #116 and the dependency-alert correction is in draft PR #111; neither has merged. Exact current main and Mac baseline are in `STATUS.md`.

### Required immediately after an approved merge

- [x] Verified GitHub `main` points to merge commit `1599ab451f711d8a27adfefbcba10293cd53dd84`; its tree equals the exact-head-validated PR tree.
- [x] Created a clean `codex/development-main` worktree tracking `origin/main`.
- [x] Verified `AGENTS.md`, `docs/PROJECT_STATE.md`, `docs/OPERATING_RUNBOOK.md`, release guidance, and Android skill routing in that worktree.
- [x] Documentation validator passed; documented `testDebugUnitTest`, `lintDebug`, and `assembleDebug` completed successfully.
- [x] Original checkout HEAD/status match preflight and the verified full-copy backup remains untouched.

### Separate follow-up work, not original foundation DoD

- #101-#103: closed after their separate acceptance criteria; do not reopen from historical prompts.
- #104 P2: authorized physical-device provenance was collected; PR #116's validator and API 26 CI pass on its validated provenance candidate. Later status-only corrections passed local documentation validation; live PR head/checks are tracked in [issue #99](https://github.com/weekssa/OPRA-EQ-for-UAPP/issues/99). Owner review and merge approval remain pending.
- #105 P3: external backup availability and retention/restore cadence.
- #106 P3: reconcile obsolete external Codex/ChatGPT instructions and historical project references.
- #107 P2: recommend disposition of unmerged historical branches; do not delete or merge automatically.
- #114: independent Automatic Dependency Submission/KSP investigation; separate backlog.
- #117: separate DAC evidence-planning issue; explicitly not a foundation closeout gate.

## Resume procedure

At each continuation, verify the branch/worktree and status, read `STATUS.md`, this runbook, and `CONTRADICTION_REGISTER.md`, then resume the earliest incomplete phase. Keep private raw evidence in the protected local audit area. Update this record and `STATUS.md` after each phase. Preserve the original checkout and all historical branch evidence.

## Restore and rollback

A verified full local repository copy and supplementary all-available-refs Git bundle are stored in the protected private local backup location. The full copy includes tracked, untracked, ignored, and `.git` content, passes `git fsck --full`, and has a successful separate full-copy restore test. The source repository is shallow, so the bundle verifies against the source but cannot independently clone beyond its shallow boundary; do not treat it as the standalone restore. To inspect recovery, use `git -C <backup>/repository status`, `git -C <backup>/repository fsck --full`, and `git bundle verify <backup>/all-available-refs.bundle`. Restore the full copy to a new directory; never overwrite the original checkout. Keep the backup until owner review and retention approval.

The cleanup branch was retained after the authorized PR #100 merge. No historical branch, original file, or backup was deleted. Do not reset, clean, force-push, delete historical branches, or merge other work automatically.


## Pre-merge integration progress (2026-10-21; superseded by post-merge record below)

PR #108's existing branch now includes a normal merge commit from verified `origin/main` `779ef3bb56702c94b49a5769c1ad6f160ba2246e`, producing head `d1c664d121e6bf374155934953853075a4f4e153`. The sole content conflict was resolved by retaining the current-main `docs/PROJECT_STATE.md`; all other newer main files were merged intact. No rebase, force-push, branch replacement, or merge into main occurred. The repository foundation validator and whitespace check passed on the merge candidate. GitHub exact-head checks are pending; do not merge until checks and owner approval are complete.

PR #116's verified Pixel record remains unchanged. Its validator is being corrected to recognize an exact installed APK hash in the installed-device section, while preserving the unknown-state path for cases without device evidence. The prior emulator failure occurred in `sdkmanager` while extracting the API 35 Google APIs ZIP, before AVD creation or application tests; treat it as runner SDK provisioning failure, with no app result. Updated exact-head CI is running.

PR #111 contains unique accurate dependency-alert closeout facts absent from current main (56 fixed, zero open or dismissed, issue #101 closed) and corrects `PROJECT_STATE.md`. It remains an existing draft and is recommended for retention as a separate issue #101 record; merge or closure remains owner-controlled.

At the pre-merge refresh, the primary Mac development folder was at `6047d45d0e8377f29bc9deb5580191ae44b37793`, 19 commits behind then-current `origin/main`, with three pre-existing untracked paths preserved. After PR #108 merged, the same untouched checkout is 25 commits behind current main. Synchronize only after the authorized documentation merges, using a saved-state checkpoint and a clean separate worktree or a carefully reviewed fast-forward; do not reset or clean this checkout.


## Post-merge record (2026-10-21)

PR #108 merged normally into `main` at `54fe191f327cfe6c6bd929b412462705f6839ae0` from approved exact head `2b96a19f6af30baeb3abd8317441d6d4cc0918a4`. Its branch merge commit `d1c664d121e6bf374155934953853075a4f4e153` included current main without rebase or force-push. Exact-head Android CI run `39658203613`, Analyze Kotlin, and dependency submission passed; generic CodeQL was skipped by workflow conditions. The corrected foundation RUNBOOK, STATUS, DECISION_LOG, and EVIDENCE_INDEX were verified on actual main. PR #116 and #111 remain open/draft and unmerged. PR #116 was synchronized with current main on its existing branch so its exact current head can be validated against current documentation; no PR merge is authorized by this record.

## PR #116 exact-head validation update (2026-10-21)

At validated provenance head `d936caedc4d113a0403c997803affd2645a5891a`, Android CI run [39669355697](https://github.com/weekssa/OPRA-EQ-for-UAPP/actions/runs/39669355697) attempt 1 failed because the API 26 smoke did not observe the network-backed catalog within 300 seconds. The single owner-authorized retry, attempt 2 on the same head, passed `min-api-smoke`, build, and API 35 emulator UI. Analyze Kotlin and dependency submission passed; generic CodeQL was skipped by workflow conditions. A subsequent documentation-only correction records the current state in this runbook and STATUS.md; its exact current PR head and check snapshot are tracked in [issue #99](https://github.com/weekssa/OPRA-EQ-for-UAPP/issues/99). This evidence supports owner review; merge requires fresh approval of the current exact PR head. No application behavior or physical-device testing was changed or performed.
