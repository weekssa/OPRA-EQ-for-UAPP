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
- Read only the allowlisted identity/current-state registers (`0x24`, the ten five-band fields,
  and `0x66`) with strict four-byte response validation.
- Stop after the first failed read and mark the device state unknown.
- Never send a write, commit, persistence candidate, firmware, bootloader, erase, calibration, or
  cross-flash command.
- Export both a human-readable report and dependency-free JSON containing the plan version,
  fingerprint, case results, register values, stop state, and whether the post-run state is known.
- Serialize the entire run through the same exclusive EW300 session gate used by EQ reads so a
  report cannot interleave with another device operation.

## Current product gates

The report does not unlock a feature by itself. Public same-earpiece acoustic analysis resolves the
raw EW300 frequency word as direct Hz. Independent KT02H20-family evidence classifies `0x66` as
ordinary digital DAC/playback gain, so it is displayed but excluded from EQ identity and capture.
Complete Peak-only readbacks can be saved as canonical Personal EQs; non-Peak snapshots fail closed
because shelf acoustics remain unqualified. Ordinary builds expose no persistence test and keep
Flash, Apply, Reset, and persistence unavailable.

The controlled signed-beta workflow is the only project build path that enables the bounded Save
qualification. Ordinary builds hard-disable the UI gate. The installed app also verifies the repository-pinned release-signing certificate
at runtime and embeds its exact 40-character source commit. The qualification remains
locked until the read-only batch passes on the connected exact fingerprint, requires an explicit
confirmation, lowers one Peak gain by 0.1 dB and playback gain by 0.5 dB, sends the provisional
`0x53` Save once, and requires a complete unplug/reconnect before proceeding. It then restores the
entire preserved baseline, sends Save once more, and requires a second complete unplug/reconnect
before it can mark persistence qualified. A transfer or Save with an uncertain result becomes a
terminal safe-stop; the app does not automatically retry it.

Passing the first power-removal check qualifies only that the temporary Peak and playback-gain
values persisted. Passing the second proves exact baseline restoration. Only the complete two-cycle
PASS unlocks the existing Peak-only editor/Flash/Reset paths for that exact device fingerprint.
If the temporary values do not survive the first power removal, the result is safely
`NOT_PERSISTENT`, the baseline must match exactly, and persistent features remain locked.

## Hardware budget

Run repository tests, public research, protocol comparison, emulator tests, and fault injection
first. The unresolved `0x53` persistence question is folded into the one consolidated signed-beta
session; there is no separate exploratory loop. Run the read-only batch once, then follow the
candidate's guided two-cycle qualification and share the combined readable/JSON report. Do not
repeat a failed or uncertain mutation, and do not call a value persistent until both complete
power-removal reads pass on the exact signed candidate.
