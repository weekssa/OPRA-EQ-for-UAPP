# Owner usage guide — Black Pearl and JA11 remediation

This guide will be finalized with the exact candidate provenance and Pixel 9 steps.

Owner boundary: install only the exact candidate named in the handoff; do not use an older APK as a substitute; do not interpret raw USB bytes; do not retry a failed or uncertain hardware operation. Stop and return the app’s interpreted report if there is a permission loop, disconnect, mismatch, missing final readback, unexpected unrelated change, crash, or uncertain result.
# Owner usage guide - Black Pearl and JA11 remediation

## Current state

The branch contains software-verified remediation for exactly two issues. The API 36 emulator and
automated software gates passed. Luna did not connect to or mutate a DAC. There is currently no
signed beta candidate for this branch, so do not install the branch debug APK for hardware testing.

The handoff is stopped at `MERGE_APPROVAL_REQUIRED` because the trusted signing workflow remains
main-only. The branch now contains a reviewed `black-pearl-ja11` combined candidate-manifest target,
but no signed candidate exists until the branch is owner-approved for trusted-main integration.

## What the owner should do next

No routine implementation action is needed now. If the owner wants to continue toward Pixel 9, review
and merge the prepared branch into trusted `main`, then dispatch the existing signed-beta workflow
with `candidate_target=black-pearl-ja11`. After an exact signed candidate is produced, use the
plain-language checklist in `05-release-handoff.md` and return the candidate provenance plus
separate Black Pearl and JA11 reports. Do not send credentials, signing material, or private reports
in chat.

## Safety boundaries

- Do not retry a failed or uncertain Black Pearl Flash, JA11 Apply, Save, or Reset automatically.
- Stop on an identity mismatch, permission prompt during mutation, disconnect, session replacement,
  or missing final readback.
- Treat emulator/debug evidence as software evidence only.
- Keep Black Pearl and JA11 results independent; one passing device cannot prove the other.
- Do not infer power-cycle persistence, acoustic fidelity, public support, or release readiness from
  immediate readback alone.
