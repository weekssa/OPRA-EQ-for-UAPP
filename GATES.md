# Gates

active: docs/implementation/ja11-filter-b1-b2-prep/GATES.md

The active task's gate file is the contract. `tools/scope-guard` resolves this
pointer, so the seal and the drift check apply to the task file. Approval seals
the task file; a change afterwards needs a new approval and a fresh seal.
