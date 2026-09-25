# EW300 capability batch

The v0.7 candidate contains an Android-free, declarative capability runner in
`Ew300CapabilityBatch`. It is deliberately read-only by default and exists to make the one
possible owner capability session bounded, repeatable, and understandable without asking the
owner to interpret USB traffic.

The runner is available in **My DAC → SIMGOT EW300 DSP → DEVICE → Run read-only report**. The app
shows a plain-language result and can share either a readable report or the machine-readable JSON
through Android's share sheet. Sharing is owner-initiated; the app does not upload the report.

CI runs the owner-facing report surface on an Android emulator in addition to the Android-free
protocol and fault-path tests. The emulator test verifies the safety explanation, run action,
result presentation, and both share actions without requiring or simulating physical USB writes.

## Default behavior

- Require the exact EW300 fingerprint; VID/PID alone is rejected.
- Read only the allowlisted protocol-layout/current-state registers (`0x01`, `0x24`, the ten
  five-band fields, and `0x66`) with strict four-byte response validation. Register `0x01` is
  required to interpret whether `0x66` contains one digital-gain byte or stereo left/right bytes.
- Stop after the first failed read and mark the device state unknown.
- Never send a write, commit, persistence candidate, firmware, bootloader, erase, calibration, or
  cross-flash command.
- Export both a human-readable report and dependency-free JSON containing the plan version,
  fingerprint, case results, register values, stop state, and whether the post-run state is known.
- Serialize the entire run through the same exclusive EW300 session gate used by EQ reads so a
  report cannot interleave with another device operation.

## Current product gates

This report is read-only and does not itself authorize or unlock a hardware action. The exact fingerprint's five-band Peak path is implemented; E001 records accepted Save/persistence qualification, and E037-E040 record physical Apply, Flash, exact-baseline Restore, and Reset verification. Do not repeat Save qualification or any of those mutation operations.

The five-band read path supports a Personal EQ representation, but the end-to-end Personal EQ capture UX remains not yet physically evidenced. This is an evidence gap, not a finding that the hardware is unsupported. After a new exact-head signed candidate passes every applicable software/security gate, the remaining owner check is limited to read-only identity/state, one Personal EQ capture and value/provenance confirmation, then opening and canceling the My EQs Flash review before final write confirmation. No Apply, Flash, Reset, Restore, or Save is part of that check.

The release-signing workflow embeds the exact source commit and verifies the pinned signer. Signed-candidate-only validation controls stay out of public release builds. PR #23 remains draft and v0.7.0 NO-GO pending the remaining evidence/review and explicit owner approval.

## Hardware budget

No additional protocol research or physical mutation is in scope. E001 is accepted Save qualification; E037-E040 close Apply, Flash, exact-baseline Restore, and Reset. The current owner check, after a fresh exact-head candidate passes all gates, is read-only report + one Personal EQ capture/value/provenance check + My EQs Flash-review open/cancel. Follow the current hands-on checklist and stop without writing if any identity, state, or UI result is unclear.
