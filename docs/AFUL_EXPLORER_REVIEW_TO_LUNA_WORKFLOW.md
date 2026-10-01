# AFUL Explorer review and Luna worker routing

## Purpose

This workflow routes the Favorite candidate through an independent Sol 5.6 review and then back to
a new Codex Luna Extra High task. Sol is the reviewer. Luna is the worker. Both use the shared
candidate folder at:

`/Users/stephenweeks/.codex/worktrees/aful-favorite-fix/OPRA-EQ-for-UAPP`

The candidate's production and test changes are in implementation commit
`fcb0f5e03b22fc51c778d1989ec1c0ad8c7ddb38`, based on `6ada7efc84425f7b149f9f9e9fb4e9599bc80cf2`.
The dispatcher supplies the exact review HEAD for the Sol task because this workflow and the review
record may receive later documentation-only updates.

## Sol reviewer stage

Sol independently reviews the fixed candidate source against its parent and the evidence in
`AFUL_EXPLORER_FAVORITE_REVIEW.md` and `AFUL_EXPLORER_FAVORITE_UI_SMOKE.md`. Sol verifies the exact
HEAD supplied by the dispatcher and records the starting Git status. A mismatch is reported without
resetting, rebasing, or discarding anything.

Sol may run the focused JVM tests if needed to resolve a review question. Sol does not edit
production code or tests, stage changes, commit, push, create a PR or Codex task, run signing/release
workflows, or use physical hardware. Sol may write review-only documentation and append the result
to the runbook so the following Luna task has durable context.

Sol must complete or replace these prepared records:

1. `docs/AFUL_EXPLORER_REVIEW_OUTCOME.md`
2. `docs/AFUL_EXPLORER_LUNA_WORKER_HANDOFF.md`

The outcome record includes the status, exact reviewed HEAD and implementation commit, accessible
evidence, findings with priority/file/line/reproduction, any tests run, access gaps, and limits of
the review. Sol also appends a dated runbook line linking the result and the Luna handoff. These
documentation changes remain uncommitted for the Luna worker to inspect and preserve.

## Routing outcomes

### PASS

PASS means Sol found no actionable code-review defect using the available candidate source and
evidence. It does not mean the app issue is fully closed, CI passed, release gates passed, or a
release is ready. The Luna prompt asks the worker to verify Sol's report against the actual
worktree, run only the remaining in-scope software checks justified by the evidence, ensure the
maintained docs still match, and return a clear owner handoff. Luna must not invent code changes if
the review identified none.

### FAIL

FAIL means Sol found at least one actionable implementation or regression issue. The Luna handoff
must enumerate each finding with priority, exact file/line, failing scenario, evidence or
reproduction, expected behavior, and acceptance test. Luna is authorized to act as the worker in the
same isolated worktree: implement the smallest justified correction, add or adjust regression
coverage, run focused tests and the appropriate broader checks, update maintained documentation,
and independently verify the resulting diff. Preserve Sol's review records. Do not reset, clean, or
replace the branch.

### INCONCLUSIVE

INCONCLUSIVE means Sol could not inspect a required local file or reproduce a review question with
the access actually available. The handoff names the exact missing access or evidence and asks
Luna to resolve that gap with repository-local tools first. Lack of remote CI, signing access, or
physical hardware alone is not a code-review failure for this Favorite issue and must not trigger
release or hardware work.

## Luna worker boundaries

- Start in the named candidate folder and inspect `git status`, branch, and HEAD before changes.
- Read `AGENTS.md`, the runbook, the review outcome, the existing implementation report, and the
  smoke record. Preserve reviewer-authored documentation.
- For FAIL, address every accepted actionable finding; keep canonical revision, fingerprint, and
  source references exact and retain fail-closed behavior for unrelated, stale, wrong, or ambiguous
  selections.
- For PASS or INCONCLUSIVE, do not modify code unless new evidence justifies a correction.
- Re-run tests only as needed for code changes or a specific unresolved finding. Do not repeat the
  emulator smoke unless a changed path makes it necessary.
- Do not push, create a PR, merge, sign, publish, release, or make hardware support claims without
  separate owner authorization.
- End with exact branch/HEAD/worktree status, changes, checks actually run, remaining limits, and a
  clear owner handoff. Any new code commit creates a new source SHA and does not inherit future CI,
  signing, or release evidence.

## Copyable Luna prompt template

Sol must replace the pending text in `docs/AFUL_EXPLORER_LUNA_WORKER_HANDOFF.md` with this template,
fill every bracketed
field with its actual review result, and return the complete prompt to the user:

```text
You are the worker in the second stage of the AFUL Explorer Favorite review workflow. Use Codex
Luna Extra High. Work in the existing isolated folder:

/Users/stephenweeks/.codex/worktrees/aful-favorite-fix/OPRA-EQ-for-UAPP

Repository: weekssa/OPRA-EQ-for-UAPP
Branch: codex/aful-favorite-identity
Expected starting HEAD: [exact current HEAD]
Implementation source commit: fcb0f5e03b22fc51c778d1989ec1c0ad8c7ddb38
Review outcome: [PASS, FAIL, or INCONCLUSIVE]
Sol review record: docs/AFUL_EXPLORER_REVIEW_OUTCOME.md

Read AGENTS.md, docs/CHATGPT_PROJECT_RUNBOOK.md,
docs/AFUL_EXPLORER_REVIEW_OUTCOME.md,
docs/AFUL_EXPLORER_FAVORITE_REVIEW.md,
docs/AFUL_EXPLORER_FAVORITE_UI_SMOKE.md, and this workflow. Inspect the current worktree before
editing. Preserve all reviewer-authored documentation and unrelated changes. Never reset or clean.

[For FAIL: implement the accepted findings below in this existing worktree, add or adjust tests,
run focused tests and the appropriate broader checks, update maintained docs, then independently
verify the diff. Findings and acceptance criteria: ...]
[For PASS: independently confirm the reviewed candidate state, perform only remaining bounded
software verification needed for a complete owner handoff, and do not invent code changes if none
are justified.]
[For INCONCLUSIVE: resolve the precise access/evidence gaps below with available repository-local
tools first, then decide whether a code change or further review is needed. Gaps: ...]

Preserve canonical profile/revision/fingerprint/source provenance and the strict fail-closed
selection checks. Do not touch catalog inputs, source registry, publication tooling, database
schema, UI, or hardware unless specific review evidence shows the finding requires it. Do not use
physical hardware. Do not push, create a PR, merge, sign, publish, or release without separate
owner authorization.

Finish with exact branch/HEAD/status, findings addressed, files changed, commands actually run and
their results, any remaining uncertainty, and the next owner action. Do not claim product or
release closure from local tests alone.
```
