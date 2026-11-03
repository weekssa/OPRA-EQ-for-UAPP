# Remove peak-hour scheduling from the default workflow

Outcome: Align the OPRA project instructions with the owner-approved shared
workflow simplification: remove time-of-day pricing, off-peak delays, sitting
estimates and peak-dependent fallback while retaining quota capacity checks,
model roles, gates and safety boundaries.

Approval: APPROVED 2026-11-03 by the owner: “The cost is low enough, let's remove
peak hours from the process too. that should simplify the flow. Again, the focus
is results.” The canonical policy, configuration map, registry and discovery
skill are updated outside this repository in the same workstream.
Base: e80eedba5c03044eaafe8711ac35460d7024df03
Branch: main.

OWNS: GATES.md,
  docs/implementation/workflow-no-peak-20261103/GATES.md,
  docs/implementation/workflow-no-peak-20261103/GATES.md.sha,
  AGENTS.md,
  docs/OPERATING_RUNBOOK.md

## Gates

1. **Approval and scope**

   CHECK:

   ```sh
   test -s docs/implementation/workflow-no-peak-20261103/GATES.md.sha &&
   /Users/stephenweeks/Documents/Codex/Codex-Usage-Policies/tools/scope-guard --root . --base e80eedba5c03044eaafe8711ac35460d7024df03
   ```

   EXPECT: the approval is recorded, the seal matches, and changes stay in OWNS.

2. **Canonical removal**

   CHECK:

   ```sh
   rg -n -i 'peak|off-peak|pricing window|fit the sitting' WORKFLOW.md CONFIGURATION_MAP.md AGENT_REGISTRY.md SKILL.md
   ```

   EXPECT: remaining matches are limited to the compatibility skill name, the
   historical owner-decision line and the explicit removal decision. No active
   scheduling or peak-dependent fallback rule remains.

3. **Runtime and preflight**

   CHECK:

   ```sh
   tools/flow-check
   ocx fallback status
   ```

   EXPECT: preflight reports capacity, quota and fallback only; no pricing or
   next-clean-window output. Global/deepseek/commandcode fallback are closed;
   OpenAI hard-lock fallback and combo/research are open.

4. **Project binding and final diff**

   CHECK:

   ```sh
   git diff --check &&
   git diff --name-only e80eedba5c03044eaafe8711ac35460d7024df03
   ```

   EXPECT: only OWNS paths changed; OPRA instructions describe hard-lock fallback
   without time windows and preserve all approval/evidence boundaries.

## Constraints

- Do not alter model roles, hard-lock thresholds, credentials, provider/model
  catalogs, research/review independence or approval gates.
- Do not create recurring schedules.
- The owner's existing authorization covers these policy and binding edits and
  their documentation commit. It does not authorize release publication,
  deployment or device work.

## Review

Independent `gpt-6-luna` inspection found two project-binding defects and no
policy contradiction. Both are corrected:

- OPRA now names `commandcode/deepseek-deepseek-v4.1-flash`, not the retired
  short `deepseek/deepseek-flash` route.
- OPRA now requires a fresh execution thread and no longer permits switching a
  planning thread into execution.

The reviewer confirmed that remaining peak references are only the compatibility
skill identifier, the superseded historical decision, and the removal decision.
Runtime fallback was already independently verified: global, DeepSeek and
Command Code fallback closed; OpenAI-to-DeepSeek hard-lock fallback and
`combo/research` open.
