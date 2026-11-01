# Session handoffs

Durable records for task handoffs under the shared Codex/OpenCodex workflow
(`/Users/stephenweeks/Documents/Codex/Codex-Usage-Policies/WORKFLOW.md`).

Each record carries the current state, exact commit/branch/PR/artifact identity,
what remains and why, the next authorized action, and where the durable evidence lives.

- A handoff is evidence, not authority. Re-verify it against `docs/PROJECT_STATE.md`, the maintained runbooks, and current remote state before acting.
- A chat transcript or a copy under `~/.codex/handoffs/` does not satisfy the durable-record requirement on its own.
- Keep superseded records; append new dated files rather than rewriting history.
