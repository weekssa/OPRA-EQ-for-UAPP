# Owner session draft — JA11 filter and vendor-ordering qualification

Status: **DRAFT ONLY.** This document grants no device access, installation, connection,
mutation, firmware or release authority. Every action below requires its own explicit owner
authorization recorded in the task that will run it.

## Purpose

One consolidated owner window would resolve the two remaining physical questions, and only if a
separately authorized read first reopens filter capability:

1. Does the current JA11 firmware answer a complete, valid filter read under one justified
   mechanism?
2. If filters reopen, does vendor-exact built-in save ordering (selection, then a preset-valued
   `0x19`, with no band/gain restaging or Apply) work as the vendor app does?

This session is conditional. It is not scheduled, and a negative result closes the branch; it does
not justify another attempt.

## Candidate identity (must be filled and verified before any action)

| Field | Required value |
| --- | --- |
| Source commit | exact SHA under test |
| APK file name | exact signed artifact |
| APK SHA-256 | exact digest |
| Signer certificate SHA-256 | pinned release or explicitly authorized test signer |
| Package / versionName / versionCode | exact values |
| Test-plan version | exact revision of this checklist |
| Device | the one intended Pixel, identified without recording unrelated data |
| DAC | JA11, VID/PID as enumerated, firmware read via `0x0B` |

Any mismatch between the installed bytes and the recorded identity stops the session before
mutation. Do not substitute a nearby build.

## Separate authorizations this draft assumes

Each is a separate decision; approving the session does not grant the others:

- connecting the phone or DAC;
- installing any diagnostic build;
- each category of write (program, volume, filter, save);
- any firmware action, which is out of scope for this draft.

## Prerequisites

- A fresh preflight and the owner's current sitting limit.
- A charged phone and DAC, a known-good USB path, and the app's existing supported-identity check.
- Confirmation that the current device state is a state the owner accepts restoring to.

## Fresh baseline (read-only first)

Record, before any mutation:

- firmware version and USB identity;
- runtime EQ program and runtime volume;
- **stored** startup program and stored volume, established by a method that does not itself
  mutate;
- User 1 five bands and global gain;
- microphone state and UAC mode;
- current filter value, only if a valid read is obtained;
- session and detach counters shown by the app.

If the stored state cannot be established, stop before any mutation. A session that cannot prove
what it will restore is not authorized to change anything.

## Sequence

1. **Read-only identity and baseline.** Verify the candidate, firmware, descriptor identity and
   the complete baseline above. A mismatch stops here.
2. **One selected filter read.** Use exactly one mechanism chosen before the session, with a
   predicted discriminating result recorded in advance. The probe is read-only and bounded by the
   app's existing timeout.
   - Valid complete reply for a supported value: the branch reopens for a separately authorized
     implementation plan. Do not write a filter value in this session.
   - Null, garbage, wrong shape or unstable session: end the filter branch. No retry, no second
     mechanism, no write.
3. **Optional vendor-ordering qualification (only if filters reopened and separately authorized).**
   For Off and Classic: select the program, send the preset-valued `0x19` payload only, with no
   band/gain restaging and no Apply. Verify the selected program, volume, unchanged User 1 bank,
   microphone and UAC. This is a save-behavior test; it is not filter delivery.
4. **Restoration.** Restore the original program, stored and runtime volume, User 1 bank, global
   gain, microphone and UAC, and any qualified filter value.

At most one expected re-enumeration is handled per approved operation: invalidate the old session,
reacquire permission and identity, and require a fresh readback. Ambiguity or timeout stops the
work.

## What each result means

| Result | Meaning | Next action |
| --- | --- | --- |
| Complete valid filter reply, supported value | H criterion met at the device-read level for this tuple | Separately approved implementation plan and qualification |
| Null or malformed reply | No device evidence; prior deferral stands | Stop; no retry |
| Reply only after much longer than the documented timeout | Not a valid read; timing was not predicted | Stop; do not sweep timings |
| Vendor-ordering save passes for Off and Classic | Save ordering is qualified for those programs | Record; propose the smallest safe adoption decision |
| Vendor-ordering save changes User 1 or another setting | Ordering is unsafe as tested | Stop; restore; record failure |
| Restoration mismatch | Session failed | Stop; report the exact mismatch and do not retry |

## Restoration and stop conditions

Restore and verify across a genuine power removal: original stored program and volume, runtime
program and volume, User 1 bank and global gain, microphone, UAC, and any qualified filter state.

Stop immediately on: identity or checksum mismatch; stored baseline unknown; unexpected setting
change; uncertain mutation; invalid or null read followed by any urge to retry; restoration
failure; or any request to exceed the authorized sitting.

## What the owner will see

The app will show a plain result for each step: read succeeded with a value, read unavailable, save
verified, save failed, or restoration verified. The owner does not need to interpret protocol
bytes, run commands or read logs. Any failure produces a report, not another attempt.
