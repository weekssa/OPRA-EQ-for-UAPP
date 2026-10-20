# Repository Foundation and v0.8.3 Consolidation Runbook

Status: IN PROGRESS\
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
| P9 | Draft PR and delivery | IN_PROGRESS | Final diff, commit, push, draft PR and exact-head CI verification remain; do not merge |
| P10 | Owner approval | NOT AUTHORIZED | Merge, close, branch retirement and device actions remain gated |

## Resume procedure

At each continuation, verify the branch/worktree and status, read `STATUS.md`, this runbook, and `CONTRADICTION_REGISTER.md`, then resume the earliest incomplete phase. Keep private raw evidence in the protected local audit area. Update this record and `STATUS.md` after each phase. Preserve the original checkout and all historical branch evidence.

## Restore and rollback

A verified full local repository copy and supplementary all-available-refs Git bundle are stored in the protected private local backup location. The full copy includes tracked, untracked, ignored, and `.git` content, passes `git fsck --full`, and has a successful separate full-copy restore test. The source repository is shallow, so the bundle verifies against the source but cannot independently clone beyond its shallow boundary; do not treat it as the standalone restore. To inspect recovery, use `git -C <backup>/repository status`, `git -C <backup>/repository fsck --full`, and `git bundle verify <backup>/all-available-refs.bundle`. Restore the full copy to a new directory; never overwrite the original checkout. Keep the backup until owner review and retention approval.

The cleanup branch can be discarded only after preserving its local changes and with owner approval. Do not reset, clean, force-push, delete branches, or merge automatically.
