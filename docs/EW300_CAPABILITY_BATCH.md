# EW300 capability batch

The v0.7 candidate contains an Android-free, declarative capability runner in
`Ew300CapabilityBatch`. It is deliberately read-only by default and exists to make the one
possible owner capability session bounded, repeatable, and understandable without asking the
owner to interpret USB traffic.

## Default behavior

- Require the exact EW300 fingerprint; VID/PID alone is rejected.
- Read only the allowlisted identity/current-state registers (`0x24`, the ten five-band fields,
  and `0x66`) with strict four-byte response validation.
- Stop after the first failed read and mark the device state unknown.
- Never send a write, commit, persistence candidate, firmware, bootloader, erase, calibration, or
  cross-flash command.
- Export both a human-readable report and dependency-free JSON containing the plan version,
  fingerprint, case results, register values, stop state, and whether the post-run state is known.

Mutating or persistence cases are represented in the declarative model but are skipped unless a
future signed diagnostic plan explicitly enables an exact operation after public research and
automated analysis leave a necessary question unresolved. A skipped case is reported as
inconclusive, not as a pass.

## Hardware budget

Run repository tests, public research, protocol comparison, emulator tests, and fault injection
first. If a physical decision is still required, run this batch once in a guided session and share
the exported reports. Do not repeat a failed mutation, and do not call a value persistent until a
complete power-removal and fresh-read test passes on the exact signed candidate.
