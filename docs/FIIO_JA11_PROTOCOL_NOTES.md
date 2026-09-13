# FiiO / JadeAudio JA11 protocol notes

Status: **expanded v0.6 software implementation in progress; physical hardware validation pending**

These notes document observable normal run-mode behavior used by EQ Library. They are not firmware-update documentation and must not be expanded into bootloader, firmware-flash, cross-flash, USB-identity mutation, or raw-command functionality without a separately approved product scope.

## Current v0.6 product direction

The project owner approved building the JA11 integration as completely as safely possible before the physical unit arrives. Software implementation and automated validation therefore proceed now; hands-on testing and device-specific tweaks are deferred until the software-side build is substantially complete.

The intended user experience is plug-and-recognize: a supported JA11 does not require the user to preselect FiiO in Settings before My DAC can recognize it. Automatic output selection is the recommended default, with an explicit manual-output override available in Settings.

Physical qualification is still mandatory before any JA11 write is described as hardware-qualified or production-proven.

## Clean-room / evidence boundary

The Kotlin implementation in this repository is independently written from observable protocol facts. Public reference projects may corroborate command values, packet shapes, device identities, and externally observable behavior; incompatible implementation code, structure, comments, or UI are not copied.

The expanded v0.6 protocol facts were cross-checked against maintained public JA11 research, including `Cyfine/ja11-web-control` at commit `4d4eb83df6fcdf9e20b52e1bdf59a77f463b2c30`, which in turn documents independent comparison with FiiO Control behavior and live JA11 firmware 2.20 observations.

## USB identity and UAC re-enumeration

Vendor ID:

- `0x2972`

The same JA11 model uses two observed product IDs depending on USB Audio Class mode:

- UAC 1.0: `0x0101`
- UAC 2.0: `0x0102`

Both identities use the same JA11 vendor-HID protocol surface and must resolve to the single EQ Library device identity `FIIO_JA11`. The app must not make the JA11 appear unsupported merely because UAC mode changed.

HID report ID:

- `0x02`

Android dynamically locates the HID interface containing interrupt IN + OUT endpoints. It does not assume an interface index and does not accept arbitrary related-chipset devices.

## Run-mode packet envelope

WebHID-style references omit report ID `0x02` from the payload; Android USB sends it as the first report byte.

Simple one-byte read:

```text
BB 0B 00 00 <command> 00 00 EE
```

Simple one-byte write:

```text
AA 0A 00 00 <command> 01 <value> 00 EE
```

PEQ read/write retains the established v0.5 codec. Response parsing accepts the protocol payload with or without a leading report-ID byte and validates the expected command.

## Software-established normal controls

| Purpose | Command | Value / semantics | v0.6 software status | Physical status |
| --- | ---: | --- | --- | --- |
| Device output volume | `0x02` | integer `0..60` | implemented | pending |
| Current stream/sample rate | `0x09` | enum, read-only | implemented | pending |
| Firmware version | `0x0B` | major/minor bytes, read-only | implemented | pending |
| Headset mic / inline remote control | `0x12` | `0/1` | implemented; may restart USB | pending |
| PEQ band | `0x15` | five structured bands | existing implementation | pending |
| Active EQ program | `0x16` | `0..4` | implemented | pending |
| PEQ master/global EQ gain | `0x17` | signed fixed point | existing implementation | pending |
| Apply PEQ | `0x18` | existing run-mode apply | existing implementation | pending |
| Save User 1 PEQ | `0x19` | observed save payload | existing implementation | pending |
| UAC mode | `0x20` | `0/1` | implemented; re-enumerates | pending |

### Active EQ program

- `0` = Vocal
- `1` = Classic
- `2` = Bass
- `3` = User 1
- `4` = Off

This value is important to hardware truth. Reading stored User 1 PEQ bands does **not** prove that User 1 is currently active. My DAC must read the active EQ program before presenting User 1 bands as the current acoustic EQ.

If `Off` is active, current EQ response is flat even though stored User 1 parameters may remain on-device. If Vocal/Classic/Bass is active and the device does not provide the actual underlying coefficients, EQ Library must identify the built-in program without inventing a response curve or attributing the stored User 1 bands as current.

### UAC mode

- `0` = UAC 1.0 → PID `0x0101`
- `1` = UAC 2.0 → PID `0x0102`

A UAC change is session-disruptive by design. A write cannot be reported as verified merely because the outgoing packet succeeded. EQ Library must wait for a replacement USB session, read the new mode and actual PID, and verify that they agree before presenting success.

### Headset mic / inline remote control

- `0` = disabled
- `1` = enabled

Public live testing observed a USB restart/re-enumeration when this control changed. EQ Library therefore treats it like a session-disruptive write: outgoing transfer is only an intermediate state; a fresh replacement-session read is required before success.

### Device output volume

Observed value domain is integer `0..60`. This is an independent JA11 device-output level and is not the same concept as command `0x17` PEQ master gain.

It is level-sensitive. EQ Library must:

- start from a fresh actual hardware value;
- never push cached volume on reconnect;
- preview the requested value locally;
- write only after explicit user action;
- read back in the same current session;
- never retry on a replacement session;
- never report success before exact readback.

The app should display the native `0..60` level unless and until a separate, verified acoustic/dB mapping exists. It must not invent a percentage or dB conversion.

### Current stream/sample-rate labels

Observed values:

| Value | Label |
| ---: | --- |
| 0 | 32 kHz |
| 1 | 44.1 kHz |
| 2 | 48 kHz |
| 3 | 88.2 kHz |
| 4 | 96 kHz |
| 5 | 176.4 kHz |
| 6 | 192 kHz |
| 7 | 352.8 kHz |
| 8 | 384 kHz |
| 9 | 705.6 kHz |
| 10 | 768 kHz |
| 11 | DSD64 |
| 12 | DSD128 |
| 13 | DSD256 |
| 14 | DSD512 |

This is informational state and may change independently while audio starts/stops or sample rate changes. It must not be treated as an unrelated-setting mutation during another targeted write.

## PEQ path

The established JA11 PEQ contract remains:

- five bands;
- Peak / Low Shelf / High Shelf;
- frequency `20 Hz..20 kHz`;
- per-band gain `-24 dB..+12 dB`;
- Q `0.1..10.0`;
- global PEQ gain `-12 dB..+12 dB`;
- complete target built before writes;
- all five slots written, with validated flat padding for unused slots;
- global gain written;
- Apply;
- full readback verification;
- Save User 1;
- final readback verification.

Source values are never silently clamped. Complete-response adaptation uses the shared deterministic finite-hardware response machinery and leaves canonical source data unchanged.

## Device-control transaction model

Non-disruptive writes use:

`fresh complete device read → local choice → review → one targeted write → complete readback → exact value verification → unrelated-state verification`

Session-disruptive writes such as UAC/headset control use:

`fresh complete device read → local choice → review → one targeted write → restart/re-enumeration expected → replacement session → complete read → exact requested value verification → stable unrelated-state verification`

An outgoing packet is never sufficient for success. Permission loss, detach, stale session generation, read failure, mismatch, or unexpected unrelated mutation must fail visibly.

No cached state is automatically restored after reconnect.

## SPDIF / digital output

**No JA11 SPDIF control is established by the current evidence.** EQ Library must not invent a JA11 SPDIF row, DoP/D2P selector, or digital-output mode.

The generic FiiO capability architecture should support a future `Digital Output / SPDIF` module for exact FiiO models that actually expose verified SPDIF behavior. UAC mode and SPDIF mode are distinct concepts and must never be conflated.

## File interchange

No sufficiently verified external JA11 preset-file interchange format has been established. JA11 remains hardware-managed rather than inventing a `.txt`, JSON, or binary import format. A future verified interchange format may be additive and must not replace Direct Flash.

## Explicitly prohibited / not inferred

- firmware flashing or update mode;
- bootloader operations;
- cross-flashing;
- USB VID/PID/string mutation;
- raw register/command console;
- arbitrary chipset-relative commands;
- invented DAC reconstruction-filter control;
- invented SPDIF control;
- invented output-volume dB/percent mapping;
- unverified persistence claims outside the established User 1 PEQ Save behavior.

## Physical qualification gate

`docs/FIIO_JA11_HANDS_ON_CHECKLIST.md` remains the physical authority. Physical testing is intentionally deferred until the software-side build and automated regression sweep are complete enough to produce one consolidated candidate. At that point, pin the exact source SHA and signed APK and test one small safe step at a time.

Until physical PASS, JA11 must remain clearly labeled **Hardware validation pending** even when the software path is complete.
