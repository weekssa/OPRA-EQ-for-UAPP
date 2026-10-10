# FiiO / JadeAudio JA11 protocol notes

Status: **Model D makes serial optional continuity evidence while retaining exact candidate/session/cardinality, expected-reset, authoritative-readback, and no-replay gates. The clean branch `codex/ja11-v0.8.1-minimal` starts from `9b9a1f610025b3d6233d4ff6f0c2309a668577cc`; G1-G7 pass on its uncommitted source: 761 JVM tests, lint, debug/release/diagnostic/Android-test builds, R8, 64/64 isolated API 35 instrumentation, and guarded helper fixtures. The emulator used an isolated ADB server and is shut down. Independent review, exact-head CI, and candidate-provenance gates remain pending. PR #80 and its physical evidence remain historical. No Pixel, JA11, or physical-device command has been used on the clean branch. Do not use the phone until the exact tuple and all off-phone gates are reported and the owner confirms a fresh phone window.**


## 2026-10-09 owner-approved Model D — current identity and restart policy

The owner approved session-specific identity requirements for the v0.8.1 correction. This section
supersedes the older J020/J021/J024 operational instructions in this file that make a unique
nonblank serial mandatory. It does not change their historical evidence: J024's permissioned initial
JA11 session opened and completed a read-only snapshot, but both Android serial readers returned
null; no write or reset occurred.

A USB serial is optional continuity evidence. Initial JA11 connection, ordinary reads, same-session
controls, and scheduling a restart-capable control require the exact supported FiiO vendor/PID set,
one unambiguous candidate, valid HID interface/endpoints, current Android permission, a successful
fresh open/claim, and a current session generation. Multiple supported JA11 candidates fail closed
regardless of serial values; never select by enumeration order. This JA11 rule does not change shared
non-JA11 transport defaults.

For expected reset/re-enumeration, require an accepted write, expected detach, invalidation of the
old session, a fresh permissioned descriptor, successful open/claim, a new session generation, one
supported replacement candidate, and authoritative fresh readback. Compare serials only when both
sessions provide usable values and reject a mismatch. Missing serial is allowed only when exactly one
supported candidate passes all operation-continuity checks. Describe the serialless result as
**requested state verified on the sole returning supported JA11**; never claim same-physical-unit
verification. Unsolicited reconnect without a pending operation starts a new session and baseline.
Never replay an uncertain write.

Flash/Save/Reset preserve their protocol-specific ordering while applying the same no-arbitrary-
candidate and authoritative-final-readback rules. Keep exactly one logical Flash action and Save.
Volume and preset persistence remain unresolved until the approved full-power physical test. J026
on source `92c11fb0` used User 1 before data writes, passed Apply and pre-Save verification, accepted
one Save, then detached 677 ms later during final readback. That physical result remains a failure
for that candidate. The clean branch contains an in-progress bounded reconnect/readback-only
correction; it has no frozen artifact or physical result. Its exact tests and all remaining gates are
tracked in the mission State and Gate Matrix.

## 2026-10-09 late Save detach — prior candidate failure and software correction

The prior diagnostic source `92c11fb0e41ae11b118b2e7bb105234d6606dbdb` accepted one Save (`0x19`). The JA11 detached 677 ms later while band 4 final readback was in flight. A fresh session completed one full read-only snapshot matching the original captured state. No Save retry occurred, Test C failed for that APK, Test D did not run, and the Pixel was released. USB permission had already been granted; this was not a permission-timeout failure.

The old PR #80 source used a 1-second initial observation and a 45-second control-restart deadline. Those figures and results belong to that candidate. The clean branch observes USB state events through a bounded post-Save window; a detected detach requires a sole fresh session before final readback. If detach is detected during final readback, it permits one bounded readback-only reconnect attempt. Ambiguity, serial mismatch when both values exist, timeout, and cancellation fail closed. It never replays data writes, Apply, or Save. Reconnect alone is not success; matching final readback on one stable generation is required. The exact clean-candidate timeout and behavior are not physically qualified yet.

Physical qualification begins with an exact-candidate read-only baseline requiring one permissioned current JA11 session, one supported candidate, valid generation, and complete source-bound snapshot. Serial is optional. If both sessions expose usable serials, mismatch fails; without serial, report only state verified on the sole returning supported JA11 and do not claim same-unit identity. Do not address the Pixel until off-phone gates pass and the owner confirms a new `PHONE WINDOW READY — PIXEL + JA11 NEEDED` request. If Mic reads Off, restore it to On as the first mutation using expected-reset handling and authoritative post-reconnect readback; if already On, skip the write.

## 2026-10-09 User 1 write-order correction — prior candidate

Source commit `92c11fb0e41ae11b118b2e7bb105234d6606dbdb` changes JA11 Flash and Reset to read the
active program, select User 1 if needed, and verify User 1 in the same current session before any
`0x15` band or `0x17` global-gain write. An uncertain selector result stops as `TransferFailed` with
state unknown before band/gain writes, Apply, or Save. Editor Apply checks fresh User 1 state directly
before its data writes and does not select User 1 afterward. The confirmation copy discloses
pre-write User 1 selection.

The physical trace on the previous exact candidate strongly supports this ordering diagnosis but
does not prove device-side bank semantics: that candidate sent five band writes and global gain
before selecting User 1, then failed band 1 volatile readback before Save. Software regression tests
use banked fakes to prove the application command order, not the JA11's hardware behavior.

The diagnostic APK for source `92c11fb0` has SHA-256
`ce3f417f20c275fd4d535cf5e70f658d8430af8fdd3f87ea950705fbfd574637`; package/version is
`com.weekssa.opraeqforuapp.ja11diag`, `0.8.0-ja11diag` / 11, with the established debug signer. Full
local G3 reports 861 JVM tests with zero failures, errors, or skips; lint/build/R8 and helper fixtures
pass. Independent app and helper reviews found no actionable defect. The exact APK cold-launched on
isolated API 35 with matching runtime source SHA, and source-matched debug instrumentation passed
64/64. Exact-head PR #80 CI and physical qualification remain pending; do not reuse the old APK or
use the Pixel before exact-head CI passes.

## 2026-10-08 J021 — Android device serial getter returned null

J021 used source `da1f8e25918065667648d676cb669fed4c803f17` and diagnostic APK SHA-256
`767b42591adc92f0e1662480112bf9efe87ce15f51060d20a7aa39118b8d8c24` on the Pixel 9 / JA11,
firmware `2.20`, VID/PID `0x2972:0x0102`. With USB permission granted, the initial session's
`UsbDevice.serialNumber` getter returned null. The session completed a full read-only snapshot but
reported `identityAvailable=false` while current. This was not a replacement session and recorded
no write or restart. The visible Mic Off state means the original Mic On restoration remains
outstanding.

J021 does not prove the USB descriptor lacks a serial, and it does not resolve J020's replacement
session. The candidate did not query the serial through its open `UsbDeviceConnection`. Android
documents `UsbDeviceConnection.getSerial()` as the serial for that opened device connection; the
current change queries it only when the JA11 device getter returns null. It does not substitute a
port path, PID, firmware, product name, or interface number. Blank values and exceptions remain
unavailable, and other shared USB transports do not enable the fallback. The change has automated
fail-closed regression coverage but no physical result yet. See the [Android connection API
reference](https://developer.android.com/reference/android/hardware/usb/UsbDeviceConnection) and
[AOSP USB host implementation](https://android.googlesource.com/platform/system/core/+/3597339226f5c0681631df9039eebf07485c04de/libusbhost/usbhost.c).

## 2026-10-08 opened-connection serial candidate — J024 candidate, superseded

Application source `616958037349e2f0e0784a556c6430b0de6ceb18`, tree
`fdb6c8d10c8aa865da1a4818d22a7c14b2559b21`, adds the Android connection serial as a JA11-only
fallback when the primary `UsbDevice.serialNumber` getter returns null. The value comes from the
same opened and claimed connection, before entering the lifecycle gate. Blank values and both
security/runtime exceptions remain unavailable, other shared transports leave this option off, and
`fiioJa11PhysicalIdentityKey` still requires one unique nonblank serial. Seven Robolectric identity
regressions pass; the full local suite reports 825 tests with zero failures/errors/skips. A wiped
API 35 emulator passed 64/64 instrumentation and the candidate's `APP_BUILD_INFO` source SHA. The
diagnostic APK SHA-256 is `ee0fe4fbfaae7b3f959d4122f0c21c128dffdf21d47586376ee383e2534ceb0d`.
These results do not prove the physical JA11 connection yields a serial. Exact latest PR-head checks,
physical restoration of Mic On, and remaining acceptance tests are pending.

## 2026-10-08 J020 USB permission and replacement identity findings

J020 recorded one completed Mic On-to-Off write followed by USB detach. Android returned the
replacement-session permission result about 18.5 seconds after the request. The replacement
session then opened and read the device state, but its identity status was unavailable while the
session remained current; no `RESTART_VERIFY_*` event was recorded. The last verified mic state is
Off, and restoration to the original On state was not attempted.

The stable JA11 restart identity is based on exactly one nonblank USB serial; expected PID and HID
interface changes are excluded. J020's initial session passed that identity gate. On re-enumeration,
the attach-side log had no readable serial value before permission; the later session's safe status
was `identityAvailable=false` with `sessionCurrent=true`. Because the old diagnostic candidate
suppressed serial-access exceptions and did not record a per-session serial status, the physical
evidence cannot distinguish a missing/blank descriptor from permission-gated access or a
post-permission access exception. A successful current session and readback are not identity proof.
Do not substitute PID, name, firmware, or USB path for the serial key.

The delayed permission and unavailable identity co-occurred, but the captured facts do not establish
that one caused the other. The current implementation treats them separately: each permission
attempt carries a request ID, device name/product ID and detach generation; an eligible grant
re-resolves the current descriptor and checks permission before opening. A 10-second waiting
fallback no longer destroys the JA11 attempt. JA11 has its own 25-second hard deadline; a grant
after expiry is stale and requires a fresh Connect action. Retry, detach, explicit terminal
cancellation and session close invalidate the applicable attempt. There is no automatic mutation
replay.

Other shared transports retain an eligible late permission callback after their 10-second retry
fallback until an explicit retry, detach, cancellation, or close; their UI is retryable. JA11's
prompt has a bounded 25-second lifetime. These policies are covered by Robolectric callback,
denial, detach, stale-device, duplicate-callback, cancellation, and timeout regressions. Physical
confirmation of the corrected JA11 path remains pending.

The replacement diagnostic build reports only a per-session serial status category
(`READABLE_NULL`, `READABLE_BLANK`, `READABLE_NONBLANK`, or exception category), never the serial
value or a fingerprint. This provides the next physical session a read-only cause clue without
relaxing the identity policy.

Android documents that `UsbManager.requestPermission` returns the decision through the supplied
`PendingIntent` extras but specifies no response-time service level. Android also documents that
`UsbDevice.getSerialNumber()` can return `null`, and can throw `SecurityException` for apps
targeting Android Q or later when they lack device permission. Those API behaviors make both
permission-gated access and a missing descriptor plausible; neither proves which occurred in J020.
See the official [UsbManager reference](https://developer.android.com/reference/android/hardware/usb/UsbManager)
and [UsbDevice reference](https://developer.android.com/reference/android/hardware/usb/UsbDevice).

## 2026-10-08 corrected source preflight

The correction is committed at source `da1f8e25918065667648d676cb669fed4c803f17`. Its permission
attempt tracker preserves a late grant after the shared 10-second retryable fallback, and JA11's
own permission window terminates at 25 seconds. Each attempt is fenced by request ID, USB device
name/PID, detach generation, and a freshly resolved permissioned `UsbDevice`. A UUID in the
permission action makes PendingIntent identity unique across HID-session recreation, including
reused in-memory request IDs. Grants for retired attempts remain stale. The unique-nonblank-serial
identity key remains unchanged, and no uncertain mutation is replayed.

Focused delayed-grant/stale-callback regressions and the full 818-test JVM suite passed, as did
lint/build/R8 gates and all 64 API 35 emulator tests. The source-bound diagnostic APK and emulator
runtime build event were verified off-phone. All eight required check rows passed on corrected-code
PR head `ef688ca1a2c805c349439eb0e9ac24fb456641ad`; check PR #80 for any later documentation-only
head. This narrows the software lifecycle defect; it does not identify why the J020 replacement
session lacked a readable identity and does not prove the new candidate will pass on this JA11.
Physical acceptance remains pending.

## 2026-09-26 J017 physical evidence — observed reconnect and exact restoration

The owner returned six reports from exact source `c886fdbb2ae326e562dc110b2b779cb075869798`.
Readable/JSON reports `(3)` and `(4)` are duplicate exports of one successful Flash; report `(5)`
is a successful Reset. The Flash report records firmware `2.20`, USB product `0x0102`, the
sanitized exact JA11 fingerprint, `9 → 5` optimized response-fit conversion, canonical/selected/
quantized/readback gain `-3.9 dB`, corrected `0x17` bytes `FF D9`, one Save, final-readback
comparison, and known state. The Reset report begins at session/detach generation `3/2` after
Flash's `1/0`, and its final flat readback matches the Flash report's original flat baseline in
the raw response multiset and normalized five-band values.

J017 therefore supports the actual observed Flash → detach/reconnect history → Reset restoration
path. It does not identify a power-removal event or duration, so it cannot by itself establish
power-cycle retention. No protocol, tolerance, retry, or transaction-order change is justified.

## 2026-09-26 official FiiO Control evidence — global-gain root cause proven

The J012 failure is now explained by a source codec defect. FiiO's public Android Control APK
V4.6.0 was downloaded from the official [FiiO Control download page](https://forum.fiio.com/note/showNoteContent.do?id=202105071628040377809&tid=17)
and verified as a complete APK (SHA-256
`516c6d882a6723b03d7860f8ab000ce8dc600d9fabe986bfe187ba15d37d110c`). It was inspected
read-only with JADX 1.5.6; it was not installed, connected to hardware, or used as a product
dependency. Its JA11 implementation is in the `classes2.dex` `ed.a` model.

The official implementation provides the decisive codec contract:

- `setMasterGain` multiplies the dB value by `10`, converts the signed integer to a four-digit
  hexadecimal value, and places the high byte before the low byte in command `0x17`.
- The corresponding response parser reads the two-byte field as signed 16-bit big-endian and
  divides by `10`, yielding one decimal dB.
- For `-3.9 dB`, the official request/readback device-domain bytes are `FF D9`, not `00 D9`.

J012 recorded the app writing `00 D9` and the JA11 returning `FF D9` in the same stable session.
Under the official domain, the returned `FF D9` is exactly `-3.9 dB`; the prior Android codec
misread it as little-endian `0xD9FF / 2560 = -3.800390625 dB`. This proves the verification
failure was caused by the Android JA11 global-gain codec's wrong scale and byte order. J017 now
supplies physical evidence for one observed reconnect/restoration path, but it does not prove
explicit power-cycle retention or complete physical qualification.

The correction is intentionally limited to command `0x17` encoding, decoding, and device-domain
quantization. It does not remove verification, widen tolerance, add retries or offsets, change
canonical EQ data, alter Apply/Save/session behavior, or change other DAC paths. The official APK
also computes sequence/check bytes; those fields are a separate protocol concern and are not
needed to explain J012 because the device accepted the existing framed commands and returned the
exact official gain bytes. No checksum or sequence change is included in this correction.

This official-app evidence supersedes the earlier third-party-only interpretation of `0x17` as
little-endian/2560. The pinned third-party implementations remain useful behavioral evidence for
identity, framing, bands, and Save, but they were not sufficient authority for this field.

## 2026-09-26 J016 owner report — corrected same-session transaction PASS

The owner returned parser-valid readable and technical reports from exact source
`c886fdbb2ae326e562dc110b2b779cb075869798`. They record the corrected `0x17` value `FF D9`
(`-3.9 dB`) on write and final readback, Apply before volatile verification, exactly one Save,
`FINAL_READBACK`, `Success`, and `stateKnown=true`. All 31 events remained on session/detach
generations `1/0`, so no detach or reconnect was observed. Report SHA-256 values are
`42de6d72e524bb83eb8581ae8af153a8c017012bb4f49a1d753cc7ce1056a3a` (readable) and
`6e2e88af5436713555222aebf18fbd2447a45aed7d978328203233bb993d18f0` (technical JSON).

J016 proves the corrected same-session transaction and Save/final-readback path. It does not prove
unplug/reconnect persistence, power-cycle retention, original-state restoration, or full hardware
qualification. Keep the maintained physical gate and fail-closed behavior unchanged; a UI-only
follow-up does not require another physical mutation when these transaction boundaries remain
untouched.

## 2026-09-25 J012 exact-candidate result — no protocol correction proven

This is the historical J012 interpretation before the official FiiO Control evidence above. The
official-app section supersedes its conclusion about command `0x17` scale and byte order; the
physical facts and the fail-closed pre-Save result remain valid.

The latest owner report pair is valid and is tied to the signed J011 source `5b4b40bfccabae91e3839de9ff2f7b1edcb0d67a`:

- Readable report SHA-256: `5cf579a19f4706d3895e0286079f46a8bb00af68acc87b1e74d4c5e326a60d3a`.
- JSON report SHA-256: `d88b3ed45ed821000616c5fb260356e415311aafbf72d3415b5dd30e451e7e41`; `jq` validation passed.
- Firmware: `2.20`; VID/PID: `0x2972:0x0102`; session/detach generations: `1/0`; permission requests: `0`.
- Canonical/selected/quantized gain: `-3.9 dB`; write: `0x17` raw `0xD900` (`00 D9`).
- Same-session readback: `0xD9FF` (`FF D9`) = `-3.800390625 dB`; delta `0.099609375 dB` versus `0.001 dB` tolerance.
- All five bands matched and Apply completed; Save count was `0`; outcome was `VerificationFailed`.

This repeats the earlier J009 symptom on firmware `2.20` but still does not distinguish device-side
transformation/quantization, stale same-command response, timing, or another protocol-semantic
behavior. No authoritative or independent implementation documents this exact `D900 → D9FF`
transition. Preserve signedness, little-endian order, `2560` scale, target derivation, tolerance,
ordering, and fail-closed Save gating. Do not add an offset, broad tolerance, retry, or readback
normalization. Original-state restoration, Save behavior, persistence, and hardware qualification
remain unproven; do not repeat the same mutation.

## 2026-09-26 official firmware-history cross-check

FiiO's [official JA11 firmware history](https://forum.fiio.com/note/showNoteContent.do?id=202406211150090736905&tid=77)
describes the `V2.2` release as fixing a wired inline-control pause issue and optimizing microphone
input gain. It does not document a PEQ, global-gain, `0x17`, readback, or persistence-format change.
The J012 report's firmware value `2.20` is consistent with that release naming, but treating the
two as the same version identity is an inference from the report and public version label, not a
device-proven fact. The official note therefore neither explains nor disproves the observed
`0xD900 → 0xD9FF` response. The current codec, tolerance, ordering, and fail-closed behavior
remain unchanged; no firmware-specific correction is justified.

## 2026-09-25 independent protocol-oracle matrix

This is a historical third-party-only comparison captured before the official FiiO APK was
inspected. Its `0x17` scale/order conclusions are superseded by the official-app section above;
the source and license records remain useful for the other protocol fields they corroborate.

The following pinned sources were compared against the clean-room Android implementation. They are
behavioral/provenance references only; no source code was copied.

| Source | Revision / license | Confirmed overlap | Unresolved limitation |
| --- | --- | --- | --- |
| [Cyfine ja11-web-control](https://github.com/Cyfine/ja11-web-control/tree/4d4eb83df6fcdf9e20b52e1bdf59a77f463b2c30) | `4d4eb83df6fcdf9e20b52e1bdf59a77f463b2c30`; [MIT](https://github.com/Cyfine/ja11-web-control/blob/4d4eb83df6fcdf9e20b52e1bdf59a77f463b2c30/LICENSE) | VID/PID, report ID, five bands, `0x17` signed LE/2560, `0x19` Save | Current browser path does not send Android’s explicit `0x18` Apply and does not prove PEQ post-Save readback |
| [Ircama ja11-config](https://github.com/Ircama/ja11-config/tree/affddff6c9808c33ce8b35b0b9759ff0d7f6e405) | `affddff6c9808c33ce8b35b0b9759ff0d7f6e405`; [EUPL-1.2](https://github.com/Ircama/ja11-config/blob/affddff6c9808c33ce8b35b0b9759ff0d7f6e405/LICENSE) | `0x15`, `0x17`, `0x18`, `0x19`, signed LE/2560 | Save helper does not independently prove global-gain persistence |
| [adithyasource fiiocontrol-oss](https://github.com/adithyasource/fiiocontrol-oss/tree/f38994b3bd51bbc898cfceb5d182a403180df33e) | `f38994b3bd51bbc898cfceb5d182a403180df33e`; [Unlicense](https://github.com/adithyasource/fiiocontrol-oss/blob/f38994b3bd51bbc898cfceb5d182a403180df33e/LICENSE) | PID `0x0102`, five bands, `0x17` signed LE/2560, Save | Explicitly reverse-engineered/not completely perfect; Save path lacks verification |

The three implementations corroborate the current `0x17` encoding and do not supply evidence for
an alternate byte order, signedness, scale, tolerance, or `0xD900 → 0xD9FF` interpretation. FiiO’s
[JA11 FAQ](https://www.jadeaudio.com/details?_l=en&article_id=178) supports Save/restart lifecycle
behavior, but does not define these raw gain semantics or prove persistence.

These notes document observable normal run-mode behavior used by EQ Library. They are not firmware-update documentation and must not be expanded into bootloader, firmware-flash, cross-flash, USB-identity mutation, or raw-command functionality without a separately approved product scope.

## Current v0.6 product direction

The project owner approved building the JA11 integration as completely as safely possible before the physical unit arrives. Software implementation and automated validation therefore proceed now; hands-on testing and device-specific tweaks are deferred until the software-side build is substantially complete.

The intended user experience is plug-and-recognize: a supported JA11 does not require the user to preselect FiiO in Settings before My DAC can recognize it. Automatic output selection is the recommended default, with an explicit manual-output override available in Settings.

Physical qualification is still mandatory before any JA11 write is described as hardware-qualified or production-proven.

## Clean-room / evidence boundary

The Kotlin implementation in this repository is independently written from observable protocol facts. Public reference projects may corroborate command values, packet shapes, device identities, and externally observable behavior; incompatible implementation code, structure, comments, or UI are not copied.

The expanded v0.6 protocol facts were cross-checked against maintained public JA11 research, including `Cyfine/ja11-web-control` at commit `4d4eb83df6fcdf9e20b52e1bdf59a77f463b2c30`, which in turn documents independent comparison with FiiO Control behavior and live JA11 firmware 2.20 observations.

## Save/readback lifecycle policy

FiiO's maintained FAQ documents Save as a chip restart/disconnect boundary. The current Android
transport treats Save as a lifecycle boundary: it observes USB state events, requires a fresh
session if detach is detected, and permits only final readback during replacement recovery.
Ordinary reads and writes remain bound to one current session and detach generation. The current
clean branch must pass its remaining gates and physical Test C before this behavior is qualified.
This is a lifecycle-safety correction, not evidence that the signed gain codec, scale, tolerance,
or device-domain value is wrong. Older PR #41 evidence remains historical.

The diagnostic trace now performs the existing read-only firmware-version query (`0x0B`) during
Flash/Reset preflight and records the result in both report formats. This metadata is optional: an
unavailable firmware response does not block an operation and does not alter the write/readback
transaction. It exists to correlate future raw `0x17` readback evidence with firmware revisions,
because FiiO's public release history identifies firmware changes affecting gain persistence.

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

### User 1 write ordering

JA11 Flash, Editor Apply, and Reset must select User 1 with `0x16` and read the active program back
before sending any `0x15` band or `0x17` global-gain writes. If the selector transfer or readback is
uncertain, stop before bands, gain, Apply, or Save. Apply, volatile readback, exactly one Save, and
final readback follow the data writes. The clean branch implementation and focused ordering tests
are pending full off-phone qualification and exact-candidate physical requalification.

This ordering is a fail-closed software contract, not a claim that protocol research has proved
per-program write-bank semantics. The J025 trace on an earlier candidate started on Off, wrote
bands/gain before selecting User 1, and failed volatile verification before Save. J026 later selected
and verified User 1 before writes, passed Apply/pre-Save verification, and failed only after the
late Save detach. Those candidate-specific observations motivate the corrected order but do not
qualify the clean branch.

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

FiiO's [JA11 FAQ](https://www.jadeaudio.com/details?_l=en&article_id=178) states that clicking Save causes the chip to power off and restart, so disconnect/re-enumeration is a documented lifecycle boundary. The clean branch must not treat the ordinary mutation settle delay as proof that the old session stayed valid. If no detach is observed during the bounded window, final readback is accepted only while the original session remains current; if detach is observed, readback requires a fresh connected session. A detach during final readback permits one readback-only retry. This remains software behavior evidence until the exact clean candidate passes physical acceptance.

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
