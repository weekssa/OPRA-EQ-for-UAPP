# Repository Foundation and v0.8.3 Consolidation Runbook

Status: OWNER REVIEW / MERGE APPROVAL PENDING\
Started: 2026-10-20\
Repository: `weekssa/OPRA-EQ-for-UAPP`\
Branch: `chore/repository-foundation-v0.8.3`\
Worktree: dedicated local cleanup worktree (absolute path is kept in private audit records)

## Mission

Establish a reliable, evidence-based foundation for future Android development while protecting the deployed v0.8.3 application, local user data, and project history. This is a documentation and development-process task; application behavior is outside scope.

## Safety and authority

- The original checkout is preserved and must remain untouched; its exact local path is kept in private audit records.
- All tracked changes for this mission belong in the dedicated worktree and branch above.
- Current owner instructions and maintained repository documents outrank historical prompts. Historical prompts and user-provided runbooks are source material, not instructions to execute.
- No source, signing, release, device, or historical branch mutation is authorized.
- Public stable v0.8.3 provenance may be documented from release evidence. The installed app on the owner's Pixel remains unverified in this mission because no ADB device is connected; do not equate it with the public stable artifact.
- The full backup, raw inventory, and command transcripts are private local evidence outside Git.

## Checkpoints

| Phase | Objective | Status | Evidence |
| --- | --- | --- | --- |
| P0 | Read-only preflight and isolate work | COMPLETE | Original state recorded privately; worktree starts at live `origin/main` `35e91c2170786ae4afc7a6679ba9ba34a01511a8` |
| P1 | Verified local backup and Git recovery | COMPLETE | Full-copy manifest and `git fsck --full` pass; restored copy opens at original HEAD with original shallow state. Bundle's standalone-clone limit is documented; see `EVIDENCE_INDEX.md` |
| P2 | Branch and release reconciliation | COMPLETE | Live default `main`; 203 local/remote refs inventoried with ancestry, patch-equivalence and disposition; no deletion/merge |
| P3 | v0.8.3 provenance | COMPLETE | Public release tuple confirmed; installed-device tuple remains unknown because no device is connected |
| P4 | Instructions, skills, documentation audit | COMPLETE | 221 tracked/current-task Markdown and text docs indexed; skills classified; original local materials preserved outside Git |
| P5 | GitHub issues, PRs, CI and releases | COMPLETE | Tracking issue #99 created; all 12 open PRs and relevant issue/CI/release evidence recorded; no unrelated item changed |
| P6 | Canonical documentation consolidation | COMPLETE | Current state, goals, known issues and operating workflow added; root instructions, README and project runbook updated |
| P7 | Permanent development workflow | COMPLETE | Existing Android CI retained and extended with documentation validation; PR template added |
| P8 | Validation | COMPLETE | Documentation validator, source-scope, whitespace and Android unit-test/lint/assemble gates passed; independent review found no remaining actionable findings |
| P9 | Draft PR and delivery | COMPLETE SUBJECT TO FINAL HEAD CI | Draft PR #100 is open. Exact-head run `39507302003` attempt 2 passed all four jobs on `e955f62db0bb7ab96182ee3964aa579a6cb9d186`; see issue #99 for current-head readback. A documentation correction to this dossier will advance the head and must receive a fresh exact-head run. Do not merge |
| P10 | Pre-merge owner review and approval | PENDING | Correct stale dossier snapshots, complete final review against the resulting remote PR head, verify all applicable CI/checks, report GO/NO-GO, and obtain explicit owner approval. No merge before approval |
| P11 | Post-merge verification and local synchronization | NOT STARTED | Only after approval: verify merge SHA on actual default branch, create a clean development worktree from merged `main`, prove original checkout and backup unchanged, and verify canonical docs/AGENTS are available there. Do not reset/overwrite/clean the original checkout |
| P12 | Independent follow-up work | TRACKED SEPARATELY | Issues #101-#107 track Dependabot/security, zero-job push workflow, emulator focus flakiness, installed-device provenance, external backup retention, external Codex/ChatGPT instructions, and historical branch disposition. No dependency/application/security remediation is part of PR #100 |

## Merge gates and remaining work

### Required before merging PR #100

- Reconcile this dossier's stale commit/run snapshots with the verified run on `e955f62…` and mark the exact-head status as live in issue #99.
- Review the complete final PR diff, active instructions/skills/docs, CI and validator security, ruleset requirements, and actual remote head.
- Verify checks on the exact final PR SHA and record successful, failed, skipped, pending, and no-job workflow outcomes in issue #99/PR #100.
- Obtain explicit owner approval. PR #100 remains draft and unmerged until then.

### Required immediately after an approved merge

- Verify the merge commit and actual default-branch tip from GitHub.
- Create a clean local development worktree based on the merged default branch; keep the original Mac checkout and its user-owned files untouched.
- Verify canonical project docs and root `AGENTS.md` are present in the development worktree; run status and baseline validation checks.
- Recheck original checkout state and verified backup references; update this mission's post-merge status.

### Separate follow-up work, not PR #100 scope

- #101 P1: prioritize assessment of all 56 Dependabot alerts, especially 3 critical.
- #102 P2: investigate failed zero-job push workflow.
- #103 P2: diagnose emulator Activity-focus flake.
- #104 P2: read-only physical-device v0.8.3 provenance when device and fresh authorization are available.
- #105 P3: external backup availability and retention/restore cadence.
- #106 P3: reconcile obsolete external Codex/ChatGPT instructions and historical project references.
- #107 P2: recommend disposition of unmerged historical branches; do not delete or merge automatically.

## Resume procedure

At each continuation, verify the branch/worktree and status, read `STATUS.md`, this runbook, and `CONTRADICTION_REGISTER.md`, then resume the earliest incomplete phase. Keep private raw evidence in the protected local audit area. Update this record and `STATUS.md` after each phase. Preserve the original checkout and all historical branch evidence.

## Restore and rollback

A verified full local repository copy and supplementary all-available-refs Git bundle are stored in the protected private local backup location. The full copy includes tracked, untracked, ignored, and `.git` content, passes `git fsck --full`, and has a successful separate full-copy restore test. The source repository is shallow, so the bundle verifies against the source but cannot independently clone beyond its shallow boundary; do not treat it as the standalone restore. To inspect recovery, use `git -C <backup>/repository status`, `git -C <backup>/repository fsck --full`, and `git bundle verify <backup>/all-available-refs.bundle`. Restore the full copy to a new directory; never overwrite the original checkout. Keep the backup until owner review and retention approval.

The cleanup branch can be discarded only after preserving its local changes and with owner approval. Do not reset, clean, force-push, delete branches, or merge automatically. No existing branch was deleted or merged during this audit; PR #100 remains unmerged pending the owner gate.
