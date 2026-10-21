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

Status: This was the correct 2026-10-20 decision. It is superseded for the installed-artifact question by D-005, based on the later owner-authorized read-only check. The distinction between public provenance and direct device readout remains applicable.

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

## D-005 — Reconcile post-merge phases and later installed-artifact evidence

Date: 2026-10-21\
Decision: Treat original P0-P10 and post-merge P11 as complete, based on the owner-approved PR #100 merge and verified post-merge handoff. Keep #101-#107 as separately tracked follow-ups; they do not retroactively reopen the original Definition of Done. Treat the 2026-10-20 P10-pending/P11-not-started dossier language as a stale checkpoint, not an active acceptance gate. Record the later authorized Pixel 9 read-only v0.8.3 APK match at high confidence, while distinguishing artifact byte identity from direct signer-certificate or source-revision readout.\
Evidence: issue #99's original phase checklist and live merge record; PR #100 merge `1599ab451f711d8a27adfefbcba10293cd53dd84`; P11 evidence in the foundation dossier and Mac baseline worktree; issue #104 and PR #116 evidence; official `docs/release-engineering/v0.8.3-stable/ARTIFACTS.md`.\
Confidence: High for original P0-P10/P11 completion and exact installed APK byte match.\
Approval required: No for this factual reconciliation. Owner approval remains required before merging any PR. Approval applies only to the exact reviewed head; a later head change requires fresh approval before merge.
