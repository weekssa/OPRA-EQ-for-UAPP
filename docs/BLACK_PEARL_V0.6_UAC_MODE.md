# EQ Library v0.6 — TRN Black Pearl USB audio mode

Status date: 2026-09-13

Status: **approved product feature; current-mode readback implemented; exact software-switch command still not established**.

This document supplements `docs/CHATGPT_PROJECT_RUNBOOK.md`, `docs/V0.6_MY_DAC_APPROVED_DESIGN.md`, `docs/V0.6_MY_DAC_IMPLEMENTATION_PLAN.md`, and `docs/BLACK_PEARL_PROTOCOL_NOTES.md`. Later explicit project-owner decisions supersede older wording.

## Approved product behavior

The project owner approved adding **USB audio mode** to **My DAC -> DEVICE -> USB / System**.

Intended finished UX:

- show the current mode as **UAC 1.0** or **UAC 2.0** only when actual hardware/session evidence supports it;
- offer exactly those two choices only if a safe exact software switch is established;
- describe UAC 1.0 as compatibility mode and UAC 2.0 as the normal mode without inventing sample-rate limits for this exact Black Pearl;
- stage a requested mode locally before any hardware action;
- warn that changing USB audio mode is session-disruptive and will restart/re-enumerate the DAC;
- report success only after a replacement USB session is recognized and a fresh read verifies the requested mode;
- never replay cached state after reconnect, silently retry a setting write, or send generic Save to Flash as part of a UAC change;
- make no power-cycle-persistence claim until independently proven on the exact Black Pearl.

## Exact Black Pearl evidence

The maintained Black Pearl HID envelope remains corroborated for mic gain, global/playback gain, PEQ, latch, flash-save, firmware/version, reconstruction filter, balance, gain mode and amplifier topology.

A second public Black Pearl implementation was reviewed at exact source commit:

`Matr1x01/trnBlackPearlEq@f289230c5422c6f4222c79c34ac10d8a4a8aaeca`

Its public protocol map independently exposes the same normal controls (`0x02`, `0x03`, `0x09`, `0x01`, `0x0A`, `0x0C`, `0x11`, `0x16`, `0x19`, `0x1D`) but contains **no UAC read/write command or software UAC selector**. Its Android/main/v1 public source likewise does not establish a UAC HID register. The repository currently has no chosen license and states all rights reserved, so it is reference evidence only; EQ Library does not copy its implementation.

Independent owner reports still establish that the hardware itself supports UAC 1.0/2.0 through its physical button/startup behavior. That proves the feature exists, not that a host HID switch command exists.

Therefore EQ Library must not infer a UAC command from a related CB5100/WalkPlay product and must not probe undocumented vendor command values on the user's DAC.

## Current-mode readback implementation

EQ Library determines the currently enumerated USB Audio Class mode from standard USB descriptors rather than from a guessed vendor register.

The parser:

1. walks the raw USB descriptor stream defensively;
2. enters only a standard Audio / AudioControl interface;
3. reads the class-specific AudioControl header descriptor;
4. interprets its little-endian `bcdADC` revision;
5. maps exactly `0x0100` -> **UAC 1.0** and `0x0200` -> **UAC 2.0**;
6. returns **Unknown** for malformed, unsupported or conflicting descriptor evidence.

This read is session-local. A reconnect creates a new USB session and the mode must be read again; retained prior mode is never made current merely because a device reconnects.

USB audio mode is modeled as a typed `USB_SYSTEM` DEVICE capability but remains **read-only** until a trustworthy Black Pearl software switch command is established.

## Automated safety coverage

Domain tests cover UAC 1.0/2.0 recognition, malformed/truncated descriptors, non-AudioControl headers, unsupported/conflicting revisions, repository propagation, and the rule that the typed UAC control remains non-writable.

The normal DEVICE transaction tests remain authoritative for unrelated controls. UAC mode is not added to the write-candidate policy merely because it can now be read.

## Remaining gate

A true one-tap in-app UAC switch still requires independently established exact Black Pearl write semantics: packet/value, re-enumeration identity/timing, Android permission behavior, post-reconnect verification, failure recovery, and persistence behavior.

If no exact software command is established, a button-assisted flow remains a possible later product decision. It is not silently substituted for a software switch.

## Qualification state

- hardware UAC 1.0/2.0 feature existence — **ESTABLISHED**
- current-mode USB-descriptor parser — **IMPLEMENTED / AUTOMATED COVERAGE PRESENT**
- Pixel 9 / exact Black Pearl descriptor readback — **PHYSICAL PENDING**
- software UAC write command — **NOT ESTABLISHED**
- software UAC write — **NOT IMPLEMENTED / NOT EXPOSED**
- button-assisted alternative — **NOT IMPLEMENTED**
- persistence claim — **NOT ESTABLISHED**

No merge or release is authorized by this document.
