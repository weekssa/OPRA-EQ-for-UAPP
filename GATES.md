# Gates

active: docs/UX_REVIEW_WORKFLOW/context/ja11-v010-filter-evaluation/GATES.md

The active task's gate file is the contract. `tools/scope-guard` resolves this
pointer, so the seal and the drift check apply to the task file. Approval seals
the task file; a change afterwards needs a new approval and a fresh seal.
