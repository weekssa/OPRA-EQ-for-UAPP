# v0.9.0-beta.2 owner readiness packet

Status: **SESSION COMPLETE (2026-11-01) — see J034 and RECOVERY_REPORT.md.** This packet is retained as
the pre-session readiness record: User 1 + volume persisted, built-in program persistence failed and
the User 1 bank (damaged by the attempt) was restored exactly. Retained here for provenance; do not
re-run the built-in native save from this packet.
This packet is the first planned owner checkpoint. It exists so the physical session is one
controlled pass over frozen software, with every possible result interpreted in advance.

## Frozen candidate

| Item | Value |
|---|---|
| App on screen | **EQ Library 0.9 Beta** |
| Package | `com.weekssa.opraeqforuapp.v090beta` |
| Version | `0.9.0-beta.2` / code `16` |
| APK | `app/build/outputs/apk/privateBeta/app-privateBeta.apk` |
| SHA-256 | `bbe1c78956737ac94fec0069b8accd4ca9419e8f0806578af3ce2274277c0dd1` |
| Signer | local test certificate `0f89797ba477006eea3e1ee837eaf654e687a14fc50b5a213b40312cdd1f869f` |
| Source | `fd63a81e5061959ce44ebb65478b7f582fd307de` on `codex/v0.9.0-recovery` |
| Stable / older beta | unaffected; they keep their packages and data |

This is a locally test-signed private beta, not an official release. Installing it does not change the
public app or its data.

## What is already demonstrated (off-device)

830 JVM tests; 306 Python/repository/contract tests; lint debug/release/privateBeta; debug, release,
privateBeta and androidTest builds; R8 mapping; API 35 and API 36 instrumentation 78/78 each; API 26
minified install/launch; API 36 background/force-stop/relaunch and font-scale 1.5/density 480.
Full detail and limits: [RECOVERY_REPORT.md](RECOVERY_REPORT.md).

Known limitation: instrumentation cannot run against the R8-minified APK (AGP does not package app
dependencies into the androidTest APK, so shrinking removes classes the tests call). The exact
minified artifact is verified by install, identity, launch and lifecycle checks instead. The phone
session is the only proof of JA11 persistence.

## Unresolved hardware questions

1. Does **Set as Default** make the selected program and volume survive a real power removal for
   **each of the five programs** (Vocal, Classic, Bass, User 1, Off)?
2. Does it preserve the stored **User 1** five bands, global gain, microphone and UAC unchanged?
3. Does this JA11 report the digital filter (`0x11`), and does a selected mode read back and survive?
4. Is the pre-mutation state restorable exactly?

## Session shape

I drive the terminal, app actions and every readback. The owner does only what a phone requires:
unlocking, granting the USB permission prompt, and physically unplugging/replugging the JA11.

Before any mutation I record: phone/Android, JA11 firmware and USB identity, current volume, program,
microphone, UAC, the stored User 1 five bands (frequency/gain/Q/type) and global gain, plus a written
restoration plan. Then: runtime edit → **Set as Default** (disclosed brief disconnect) → fresh
readback → owner removes JA11 power → reconnect → read-only comparison of every field → force-stop /
relaunch → Flash/editor/Reset spot checks → filter check → exact restoration → verification.

## Troubleshooting (decided in advance, no improvisation)

| Situation | Response |
|---|---|
| Runtime edit fails or reports a mismatch | Stop persistence work; re-read and diagnose the field; no retry loop |
| Save reports success but a field is wrong after re-plug | Record which field failed; stop further saves; continue read-only diagnosis |
| Save outcome is uncertain / app and device disagree | Stop all mutations; re-establish a fresh session; read-only recovery only |
| Save fails but the baseline is verified intact | Report the failure; optionally propose the prepared microphone fallback as a **new** candidate (new version code, new gates), never silently |
| Filter row is unavailable | Expected outcome on an unsupported unit; record it; no filter commands are sent |
| A filter change produces a bad or missing response | Stop mode writes immediately; re-read; do not retry |
| Phone connection problem | Use the wireless-ADB recovery skill's read-only diagnostics and its approval boundaries |
| Restoration is uncertain | Do not unplug in a mutated state; diagnose before any further write |

## Stop conditions

Ambiguous device identity, contradictory readback, uncertain mutation outcome, stale session,
unexpected microphone change, unsupported or unsafe response, or unreliable restoration. After any of
these: read-only only; no automatic Save/Flash retry, no hidden mic fallback, no second mechanism.

## Outcomes and next actions

- **All mandatory fields persist and restoration verifies** → Candidate A passes; prepare the go/no-go
  record, then separately request merge/official-signing/publication authority.
- **Program/volume do not persist** → Candidate A fails on hardware; diagnose the transaction from the
  captured evidence, then decide whether to build Candidate B (explicit microphone round trip) with a
  new identity and affected gates.
- **Bank/gain/mic/UAC damage** → Stop; restore from the captured values first; treat as a release
  blocker and record the exact sequence that caused it.

## Authorization boundary

Local install, tests, emulator work and this private beta are approved. Merge, official signing,
publication, public hardware-support claims and credential changes remain separately owner-gated. No
phone or DAC action occurs before the owner confirms this packet.
