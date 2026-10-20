# Decision Log

## D-001 — Use an isolated cleanup worktree based on live main

Date: 2026-10-20\
Decision: Create `chore/repository-foundation-v0.8.3` in a dedicated local worktree, based on fetched `origin/main` `35e91c2170786ae4afc7a6679ba9ba34a01511a8`. Exact local paths are held privately.\
Evidence: Original checkout is dirty, on a JA11 evidence branch, 17 commits behind its remote, and has substantial untracked/ignored materials.\
Confidence: High.\
Rollback: Preserve changes and remove the worktree only with owner approval; original checkout remains intact.

## D-002 — Treat v0.8.3 as the public stable baseline, installed app as unverified

Date: 2026-10-20\
Decision: Current public stable release is v0.8.3 from source `899755507f812c940f3ab9381a856c80c6030885`. Do not claim that source produced the application currently installed on the owner's phone.\
Evidence: Release/tag/artifact records and live public release; no ADB device is connected; release record says the phone retained a distinct debug-signed Beta v1.\
Confidence: High for public release; unknown for current installed package.\
Approval required: None for documentation of this evidence boundary. A future device check requires fresh owner authorization.

## D-003 — Keep historical materials and do not import dirty checkout content blindly

Date: 2026-10-20\
Decision: Do not delete, move, or automatically commit the original checkout's untracked/ignored materials. Publish only reviewed, sanitized maintenance documentation from the isolated branch.\
Evidence: Original checkout inventory includes historical prompts, task ledgers, review bundles and six untracked DAC/release skills.\
Confidence: High.\
Rollback: No originals are altered; backup also preserves the complete checkout.

## D-004 — Update stale active v0.7.0 milestone wording

Date: 2026-10-20\
Decision: Rewrite root active guidance to refer to the verified current release and direct future version updates to maintained release state. Preserve v0.7 history in historical release documents.\
Evidence: `AGENTS.md` current milestone sentence conflicts with v0.8.3 state in README, changelog, public release checklist and exact artifact provenance.\
Confidence: High.\
Approval required: No; this is an authorized documentation correction.
