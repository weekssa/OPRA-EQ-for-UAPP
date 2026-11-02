# Gates

No task is active.

When a task starts, point this file at its gate file inside the task folder:

    active: docs/UX_REVIEW_WORKFLOW/context/<task-slug>/GATES.md

`tools/scope-guard` resolves this pointer, so the seal and the drift check apply
to the task file rather than to this pointer. Approval seals the task file;
a change to it afterwards needs a new approval and a fresh seal.
