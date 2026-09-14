# EQ Library v0.6 — TRN Black Pearl USB audio mode

Status date: 2026-09-14

Status: **approved product feature; current-mode readback implemented; manual button-assisted UAC 1.0 startup documented; exact software-switch command still not established**.

This document supplements `docs/CHATGPT_PROJECT_RUNBOOK.md`, `docs/V0.6_MY_DAC_APPROVED_DESIGN.md`, `docs/V0.6_MY_DAC_IMPLEMENTATION_PLAN.md`, and `docs/BLACK_PEARL_PROTOCOL_NOTES.md`. Later explicit project-owner decisions supersede older wording.

## Approved product behavior

The project owner approved adding **USB audio mode** to **My DAC -> DEVICE -> USB / System**.

Finished v0.6 UX:

- show the current mode as **UAC 1.0** or **UAC 2.0** only when actual hardware/session evidence supports it;
- keep the control read-only because no safe exact in-app software switch command is established;
- provide concise manual-switch help directly under the row rather than speculative protocol wording;
- after reconnect, automatically read the replacement USB session and show its actual mode;
- never replay cached state after reconnect, silently retry a setting write, or send generic Save to Flash as part of a UAC change.

## Exact Black Pearl evidence

The maintained Black Pearl HID envelope remains corroborated for mic gain, global/playback gain, PEQ, latch, flash-save, firmware/version, reconstruction filter, balance, gain mode and amplifier topology.

A second public Black Pearl implementation was reviewed at exact source commit:

`Matr1x01/trnBlackPearlEq@f289230c5422c6f4222c79c34ac10d8a4a8aaeca`

Its public protocol map independently exposes the same normal controls (`0x02`, `0x03`, `0x09`, `0x01`, `0x0A`, `0x0C`, `0x11`, `0x16`, `0x19`, `0x1D`) but contains **no UAC read/write command or software UAC selector**. Its Android/main/v1 public source likewise does not establish a UAC HID register. The repository currently has no chosen license and states all rights reserved, so it is reference evidence only; EQ Library does not copy its implementation.

Independent owner reports establish that the hardware supports UAC 1.0/2.0 through startup/button behavior. Public owner testing on Black Pearl firmware v0.5 further reports a working UAC 1.0 startup sequence: with headphones already inserted, hold both **+** and **−** while reconnecting the DAC to the USB host, then release after power/enumeration. The same report also describes an equivalent sequence in which the DAC is connected without headphones, both buttons are held, and headphones are then inserted. EQ Library uses the simpler reconnect-with-headphones sequence in its help text.

This is evidence for a manual hardware/startup behavior, not a host HID register. EQ Library therefore must not infer a UAC command from a related CB5100/WalkPlay product and must not probe undocumented vendor command values on the user's DAC.

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

## Manual mode-switch help

For Black Pearl hardware/firmware that supports the qualified startup behavior, the app presents:

- **UAC 1.0:** disconnect the Black Pearl, leave headphones connected, hold **+** and **−**, reconnect USB, then release after the DAC powers on;
- **UAC 2.0:** reconnect normally without holding the buttons;
- after either reconnect, EQ Library automatically detects and displays the active mode from the new USB descriptors.

The detected descriptor state is authoritative. The help text does not promise that every hardware revision exposes the same startup behavior, and it does not claim the app itself switches UAC mode.

## Automated safety coverage

Domain tests cover UAC 1.0/2.0 recognition, malformed/truncated descriptors, non-AudioControl headers, unsupported/conflicting revisions, repository propagation, and the rule that the typed UAC control remains non-writable.

The normal DEVICE transaction tests remain authoritative for unrelated controls. UAC mode is not added to the write-candidate policy merely because it can now be read.

## Remaining gate

A true one-tap in-app UAC switch still requires independently established exact Black Pearl write semantics: packet/value, re-enumeration identity/timing, Android permission behavior, post-reconnect verification, failure recovery, and persistence behavior.

The manual button-assisted flow is the v0.6 user-facing fallback and removes the need for speculative in-app wording.

## Qualification state

- hardware UAC 1.0/2.0 feature existence — **ESTABLISHED**
- current-mode USB-descriptor parser — **IMPLEMENTED / AUTOMATED COVERAGE PRESENT**
- manual button-assisted UAC 1.0 startup — **PUBLIC OWNER EVIDENCE; IN-APP HELP APPROVED**
- Pixel 9 / exact Black Pearl descriptor readback — **OWNER CONFIRMED WORKING 2026-09-14**
- software UAC write command — **NOT ESTABLISHED**
- software UAC write — **NOT IMPLEMENTED / NOT EXPOSED**

No merge or release is authorized by this document.
