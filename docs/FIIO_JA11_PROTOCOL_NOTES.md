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

## Current Save/readback lifecycle correction

FiiO's maintained FAQ documents Save as a chip restart/disconnect boundary. The current Android
transport therefore treats Save as the explicit lifecycle exception: it waits for the optional
detach/re-enumeration boundary before final readback, while ordinary reads and writes must remain
on the same session and detach generation for the full exchange. This is a lifecycle-safety
correction, not evidence that the signed gain codec, scale, tolerance, or device-domain value is
wrong. Exact draft PR #41 head `b32c52a82a46899efd115efd8544deeb16b9eb4c` passed automated software
gates; physical JA11 semantics remain pending raw transaction evidence.

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

### Mutation pacing and response correlation

Independent JA11 protocol research documents an approximately `200 ms` device command interval.
The Android JA11 transport therefore waits `200 ms` after every mutation report before issuing or
verifying the next transaction boundary. This is an evidence-backed software mitigation for
device-side processing/pacing loss, but it is not physical qualification: the exact unit firmware,
PID/UAC mode, and packet trace remain owner-test evidence.

FiiO's [JA11 FAQ](https://www.jadeaudio.com/details?_l=en&article_id=178) separately states that clicking Save causes the chip to power off and restart,
so disconnect/re-enumeration is a documented Save lifecycle boundary. The Android transport must
await the optional replacement-session boundary after Save before final readback; it must not
assume that the ordinary mutation settle delay is sufficient. Firmware variants that persist
without re-enumerating may continue on the same healthy session, but a detected detach requires a
fresh connected session. This remains software behavior evidence, not physical qualification.

The supplied owner screenshot sequence adds a separate physical observation: after the optimized
Jaytiss target was visible on connected JA11 hardware, My DAC displayed `-3.80 dB` global EQ gain,
while the source record and current JA11 plan retain `-3.90 dB`; a later connected view displayed
flat bands and `0.00 dB` after reconnect. This is consistent with a volatile global-gain mismatch
followed by lost persistence, but no raw `0x17` packets were captured. Do not change the `2560`
scale, signedness, endianness, or verification tolerance from this UI evidence alone.

JA11 read exchanges accept only a decoded response for the requested command, and band reads also
require the requested band index. Wrong-command, stale, malformed, or out-of-range responses are
discarded until the bounded read timeout; a transfer is never treated as successful merely because
it returned enough bytes.

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

### 2026-09-25 diagnostic trace boundary

The exact signed `609911e` candidate failed on owner hardware with the same global-gain mismatch
seen in the earlier unproven record. Independent protocol evidence still supports the current
`0x17` interpretation: signed 16-bit little-endian, `2560` raw units per dB. The failure is not
evidence for changing that codec, widening the comparison, retrying writes, or copying readback
into the target.

The shared JA11 Flash transaction now has an opt-in bounded trace that starts at the authoritative
flasher and ends after the terminal result. Android records the raw request and response bytes,
command, elapsed time, session generation, detach generation, and transport outcome only while
that trace is active. The domain report joins those events to the canonical preamp, optimized
target gain, exact wire-domain quantization, decoded global-gain comparison, comparison phase,
Save count, and final outcome. Background reads and other DAC paths are not recorded by this
trace. Shareable output redacts the USB serial from the device fingerprint.

This diagnostic boundary is not a protocol correction and does not qualify JA11 hardware. The
next physical session must use one exact signed diagnostic candidate, capture the report, and stop
on missing evidence or uncertain restoration.

### 2026-09-25 exact signed diagnostic candidate

The diagnostic implementation is merged on `main` at
`af8c68c35d320223a13c635fac69c0f2ebacdb3f`. The owner-test artifact is
[`EQ-Library-v0.7.0-beta-af8c68c.apk`](https://raw.githubusercontent.com/weekssa/OPRA-EQ-for-UAPP/mobile-test-apk/candidates/EQ-Library-v0.7.0-beta-af8c68c.apk),
with APK SHA-256 `3b442cbab3cf8be59a9b8e4ddd0d7028e93f8c4067d94dbb833a5bbb60b1d37e`, signer
certificate SHA-256 `65c1c1256dae3c49e3548f334c91f0ba991969e9be9e0b223ba4e253d2114747`, and signed-beta
[run #1363](https://github.com/weekssa/OPRA-EQ-for-UAPP/actions/runs/36180975485). Its signed
artifact is ID `10884371877` with ZIP SHA-256
`77477ad6bd52e9b114cf18e949368424d8d5c1dbc85246679c9c5fd561d5b44d`; R8 mapping SHA-256 is
`71036cf05464e6b84f07165e75c17c5a5cd5517a843bd1a8efb0bb49add0b374`.

This candidate is diagnostic only. The next physical session must be the one bounded session in
the maintained hands-on checklist. Its report must preserve the raw `0x17` request/response,
decoded values, phase, timestamps, Save count, session generations, and complete baseline. No
codec, scale, tolerance, retry, or public-support decision may be changed from UI evidence alone.

### 2026-09-25 J009 device readback observation

The owner returned the first complete JA11 operation trace from the exact signed diagnostic
candidate. The app wrote global gain command `0x17` with raw little-endian payload `00 D9`
(`0xD900`, signed `-9984`, `-3.9 dB` at the maintained `2560` raw-units-per-dB scale). After
the `0x18` Apply command, the same session returned a valid `0x17` read response with payload
`FF D9` (`0xD9FF`, signed `-9729`, `-3.800390625 dB`). The five band readbacks exactly matched
the target, and session/detach generations remained `1/0`; no Save was sent because the volatile
readback failed.

This is physical evidence that the JA11 returned a different global-gain device value for this
transaction. It is not evidence that the app's signedness, byte order, scale, or framing is
wrong: those remain independently corroborated by the maintained repository tests and the
independent `ja11-web-control` and `ja11-config` implementations. No source currently documents
the observed `0xD9FF` result for a `0xD900` write. The remaining question is whether the device
firmware intentionally quantizes/transforms the value, has an off-by-one fixed-point behavior, or
exposes a response behavior not yet characterized. Do not widen tolerance, copy readback into
the target, retry indefinitely, or send Save on this evidence alone.

The owner-provided technical JSON export is malformed (`{,` at each nested object boundary), so
the readable report is the authoritative artifact for this operation. The serializer defect is
separate from the hardware mismatch and must be fixed and parse-tested before the next report.
