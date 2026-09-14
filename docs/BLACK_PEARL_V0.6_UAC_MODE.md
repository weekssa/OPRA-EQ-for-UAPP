# EQ Library v0.6 — TRN Black Pearl USB audio mode

Status date: 2026-09-13

Status: **approved product feature; current-mode readback implemented; software-switch command not yet established**.

This document supplements `docs/CHATGPT_PROJECT_RUNBOOK.md`, `docs/V0.6_MY_DAC_APPROVED_DESIGN.md`, `docs/V0.6_MY_DAC_IMPLEMENTATION_PLAN.md`, and `docs/BLACK_PEARL_PROTOCOL_NOTES.md`. Later explicit project-owner decisions supersede older wording.

## Approved product behavior

The project owner approved adding **USB audio mode** to **My DAC → DEVICE → USB / System**.

Intended finished UX:

- show the current mode as **UAC 1.0** or **UAC 2.0** only when actual hardware/session evidence supports it;
- offer exactly those two choices when a safe software switch is established;
- describe UAC 1.0 as compatibility mode and UAC 2.0 as the normal mode without inventing sample-rate limits for this exact Black Pearl;
- stage a requested mode locally before any hardware action;
- warn that changing USB audio mode is session-disruptive and will restart/re-enumerate the DAC;
- treat the expected replacement USB session differently from an ordinary stale-session failure;
- report success only after the replacement session is recognized and a fresh read verifies the requested mode;
- never silently retry a write after an unexpected session replacement;
- never push cached device state after reconnect;
- never send generic Save to Flash as part of this control;
- make no power-cycle-persistence claim until independently proven on the exact Black Pearl.

## Exact Black Pearl evidence established so far

Public Black Pearl and WalkPlay clean-room/reference work corroborates the normal HID report envelope and the maintained DEVICE commands already used by EQ Library, including mic gain, global gain, PEQ, temporary/latch, firmware/version, reconstruction filter, balance, gain mode and amplifier work mode.

No reviewed exact-Black-Pearl source establishes a vendor HID command/register for switching UAC 1.0 ↔ UAC 2.0. The reviewed WalkPlay/Black Pearl controller sources likewise do not expose a software UAC control for the Black Pearl.

Independent Black Pearl owner reports establish a physical UAC mode toggle using the DAC's buttons during connection/startup and report successful UAC 1.0 use on console hardware. Those reports are evidence that the hardware feature exists; they are **not** evidence for a software HID write packet.

Therefore EQ Library must not infer a UAC command from a different CB5100/WalkPlay product and must not probe undocumented vendor command values on the user's DAC.

## Current-mode readback implementation

EQ Library now determines the currently enumerated USB Audio Class mode from standard USB descriptors rather than from a guessed vendor register.

The parser:

1. walks the raw USB descriptor stream defensively;
2. enters only a standard Audio / AudioControl interface;
3. reads the class-specific AudioControl header descriptor;
4. interprets its little-endian `bcdADC` revision;
5. maps exactly `0x0100` → **UAC 1.0** and `0x0200` → **UAC 2.0**;
6. returns **Unknown** for malformed, unsupported or conflicting descriptor evidence.

This read is session-local. A reconnect creates a new USB session and the mode must be read again; retained prior mode is never made current merely because a device reconnects.

USB audio mode is modeled as a typed `USB_SYSTEM` DEVICE capability but remains **read-only** until a trustworthy Black Pearl software switch command is established.

## Automated safety coverage

Domain tests cover:

- UAC 1.0 descriptor recognition;
- UAC 2.0 descriptor recognition;
- ignoring class-specific headers outside AudioControl;
- malformed/truncated descriptor failure;
- unsupported revision failure;
- conflicting UAC revisions failing closed rather than guessing.

The existing DEVICE transaction tests remain the authority for unrelated normal controls. UAC mode is not added to the write-candidate policy merely because it can now be read.

## Remaining implementation gate

A true one-tap in-app UAC switch requires independently established exact Black Pearl write semantics. At minimum, the evidence must establish:

- exact command/report format;
- exact UAC 1.0 and UAC 2.0 values;
- whether the command itself causes disconnect/re-enumeration;
- expected VID/PID/identity after each mode switch;
- replacement-session timing and Android permission behavior;
- a deterministic post-reenumeration verification path;
- failure/recovery behavior;
- whether persistence across a full power cycle is real.

Until those are established, the product must not expose a speculative software **Change** action.

If no exact software command can be independently established, the project owner must make a separate UX decision on whether v0.6 should instead offer a **button-assisted mode switch**: EQ Library would release the session, instruct the user to perform the known physical Black Pearl button action, wait for re-enumeration, and then descriptor-verify the resulting mode. That alternative is not silently substituted for the approved software switch.

## Qualification state

- hardware feature existence — **ESTABLISHED**
- current-mode USB-descriptor parser — **IMPLEMENTED / AUTOMATED TEST PENDING FINAL CI**
- Pixel 9 / exact Black Pearl descriptor readback — **PHYSICAL PENDING**
- software UAC write command — **NOT ESTABLISHED**
- software UAC write — **NOT IMPLEMENTED / NOT EXPOSED**
- button-assisted alternative — **NOT APPROVED / NOT IMPLEMENTED**
- persistence claim — **NOT ESTABLISHED**

No merge or release is authorized by this document.
