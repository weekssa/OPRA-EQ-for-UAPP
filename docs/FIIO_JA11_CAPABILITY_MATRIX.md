# FiiO JA11 capability matrix

This matrix is the current evidence boundary for the exact FiiO JA11 identity. Software
implementation does not imply physical support. Unknown behavior stays insufficiently evidenced
or unsafe rather than being inherited from another KT02H20-family device.

The latest owner-tested signed candidate is source `c886fdbb2ae326e562dc110b2b779cb075869798`, with
immutable APK SHA-256 `c390bbd429ce4101ce7fad3aa3820990da0e7ffec7a4f688e5eafe4eb11f6341`, signer
certificate SHA-256 `65c1c1256dae3c49e3548f334c91f0ba991969e9be9e0b223ba4e253d2114747`, and signed-beta
run [#1365](https://github.com/weekssa/OPRA-EQ-for-UAPP/actions/runs/36223017450). J017 records a
successful Flash, observed detach/reconnect history, and exact original-flat-state restoration
on this candidate. The software/artifact gate, corrected same-session transaction, observed
reconnect path, and restoration path are evidenced; explicit power-cycle retention and full
qualification remain pending. J012 and earlier candidates remain historical negative evidence and
must not be reused.

| Capability | Decision | Evidence / boundary |
| --- | --- | --- |
| Exact JA11 USB identity, VID `0x2972`, UAC PIDs `0x0101`/`0x0102` | SUPPORTED_AND_IMPLEMENTED; physical pending | Strict allowlist and dynamic HID interface discovery are implemented. Physical identity/PID for J001 was not captured. |
| Five-band Peak/Low Shelf/High Shelf target representation | SUPPORTED_AND_IMPLEMENTED; physical pending | Shared finite-hardware adapter, complete five-slot target, and codec tests exist. J001 displayed a 9→5 optimized plan but did not provide readback values. |
| Global EQ gain `0x17` encoding/decoding | CORRECTION IMPLEMENTED; PHYSICAL PASS; FULL QUALIFICATION PENDING | Official FiiO Control V4.6.0 evidence establishes signed 16-bit tenths-of-a-dB, high-byte-first encoding. J017 physically records the corrected source writing and reading `FF D9` as `-3.9 dB`; explicit power-cycle persistence remains pending. |
| Apply command and volatile readback | SOFTWARE-SUPPORTED; PHYSICAL SEMANTICS INSUFFICIENTLY_EVIDENCED | A supplied My DAC frame shows the optimized five-band target present while connected, which supports volatile application of the band plan. It does not prove the gain wire value or the exact transaction phase. |
| Save User 1 persistence | FLASH/SAVE/FINAL-READBACK AND OBSERVED RECONNECT PASS; POWER-CYCLE PERSISTENCE PENDING | J017 records exactly one Save and final readback, then a later session/detach generation (`3/2` versus `1/0`) with the flashed target present as the Reset baseline. The observed reconnect path passed; no explicit power-removal marker or duration is recorded. |
| Unplug/reconnect persistence | OBSERVED RECONNECT PATH PASS; FULL POWER-CYCLE PERSISTENCE PENDING | J017's Reset report begins after the session/detach generations advanced and its final readback matches the original flat baseline. This is strong evidence for the observed detach/reconnect history, not proof of a separately identified full power cycle. |
| Fail-closed mismatch handling | SUPPORTED_AND_IMPLEMENTED | A global-gain mismatch prevents Save in the first verification path. Do not weaken the `0.001 dB` check or suppress the error. |
| Same-command stale-response correlation | PARTIALLY NARROWED; NOT PROVEN SAFE | J012 has a valid same-command `0x17` response on an unchanged session with exact event ordering and no detach/reconnect, so session replacement is not the cause of that attempt. The protocol still lacks request identity beyond command matching; delayed same-command responses remain an unresolved risk. |
| Complete baseline capture and failed-operation restoration | EXACT FLAT-STATE RESTORATION PASS; GENERAL RESTORATION PENDING | J017's Reset report captures the flashed target as baseline and ends with a final raw readback matching the Flash report's original flat baseline. This proves the observed flat-state restoration path, not arbitrary-state restoration. No automatic retry is added. |
| Session-generation enforcement across Flash | SUPPORTED_AND_IMPLEMENTED; AUTOMATED GATES PASS; PHYSICAL PENDING | The JA11 Android transport pins reads and ordinary writes to one session/detach generation; Save remains the explicit lifecycle exception and waits for its reconnect boundary. Focused tests, Android CI, signed emulator install/cold launch, and the exact signed candidate all pass. Physical lifecycle behavior remains unqualified. |
| Output volume, presets, headset/UAC controls | SOFTWARE IMPLEMENTED; PHYSICAL PENDING | These controls are outside the failed EQ-gain root-cause boundary; owner reports that general controls work do not qualify Flash or persistence. |
| Firmware update, bootloader, cross-flash, raw command console | UNSAFE_OR_OUT_OF_SCOPE | No JA11 firmware mutation or arbitrary command surface is authorized in this task. |
| Public JA11 support/release claim | UNSAFE_OR_OUT_OF_SCOPE | J017 closes the observed reconnect/restoration evidence gap for one session but does not close explicit power-cycle retention or the complete qualification checklist. Keep public support and final-release claims owner-controlled. |

## Matrix rule

J017 is the latest accepted owner evidence and its Flash/Reset session is consumed; there is no
safe reason to repeat that mutation. The observed reconnect/restoration path is accepted for the
exact candidate and state recorded in the ledger. Do not convert the remaining power-cycle evidence
gap into a protocol change, tolerance change, retry, or support claim. Any future physical session
requires a new exact candidate and a bounded owner-approved plan for the specific unresolved gate.

## 2026-09-25 independent protocol-oracle matrix

The following pinned sources were inspected as independent behavioral evidence. None documents the
observed `0xD900` write followed by `0xD9FF` readback, and none proves PEQ Save persistence by a
post-restart readback. Their licenses constrain reuse; no external code was copied.

| Oracle | Pinned revision | License | Agreement with Android | Limit |
| --- | --- | --- | --- | --- |
| [Cyfine ja11-web-control](https://github.com/Cyfine/ja11-web-control/tree/4d4eb83df6fcdf9e20b52e1bdf59a77f463b2c30) | `4d4eb83df6fcdf9e20b52e1bdf59a77f463b2c30` | [MIT](https://github.com/Cyfine/ja11-web-control/blob/4d4eb83df6fcdf9e20b52e1bdf59a77f463b2c30/LICENSE) | VID/PID, report ID, five bands, `0x17` signed LE/2560, and Save framing | Current app omits explicit `0x18` Apply and does not show PEQ post-Save verification |
| [Ircama ja11-config](https://github.com/Ircama/ja11-config/tree/affddff6c9808c33ce8b35b0b9759ff0d7f6e405) | `affddff6c9808c33ce8b35b0b9759ff0d7f6e405` | [EUPL-1.2](https://github.com/Ircama/ja11-config/blob/affddff6c9808c33ce8b35b0b9759ff0d7f6e405/LICENSE) | `0x15`, `0x17`, `0x18`, `0x19`, signed LE/2560 | Save helper does not independently prove global-gain persistence |
| [adithyasource fiiocontrol-oss](https://github.com/adithyasource/fiiocontrol-oss/tree/f38994b3bd51bbc898cfceb5d182a403180df33e) | `f38994b3bd51bbc898cfceb5d182a403180df33e` | [Unlicense](https://github.com/adithyasource/fiiocontrol-oss/blob/f38994b3bd51bbc898cfceb5d182a403180df33e/LICENSE) | PID `0x0102`, five bands, `0x17` signed LE/2560, Save | Repository says reverse-engineered/not completely perfect; Save path does not verify readback |

The oracle convergence supports retaining the current codec and fail-closed comparison. It does
not establish whether J012 is device-side transformation, firmware quantization, stale same-command
response, or another protocol-semantic issue.

## 2026-09-25 J012 exact signed-candidate result

J012 is the latest physical evidence: valid JSON, firmware `2.20`, PID `0x0102`, stable session
`1/0`, no permission request, five matching bands, Apply completed, and no Save. It repeats the
same `0xD900 → 0xD9FF` mismatch as J009. Global-gain codec semantics remain **insufficiently
evidenced on hardware**; Save persistence and restoration remain **not established**. Keep the
capability matrix fail-closed and keep JA11 hardware-validation pending.

## 2026-09-25 exact-candidate failure update

The owner has now reproduced the failure on the exact signed `609911e` candidate. This changes
the evidence classification from “candidate provenance not established” to **physical negative
evidence on the current signed candidate**, but it does not identify the protocol root cause.
The current implementation therefore adds a bounded JA11 transaction report at the shared Flash
boundary. It records the canonical preamp, optimized target, device-domain quantized target,
phase-specific decoded readback, raw request/response bytes, Save count, and session generations.
It does not change the signedness, endian order, `2560` scale, tolerance, retry policy, or
fail-closed mismatch behavior. The next candidate is for diagnosis only; JA11 remains physically
unqualified until the report-backed owner session proves the exact transaction and restoration.

## 2026-09-25 J009 exported transaction result

The owner returned the readable and technical reports from J008. The readable report is
authoritative for the operation because the technical export is malformed JSON. The exact trace
shows a stable-session mismatch before Save:

- Intended and quantized target: `-3.9 dB`, raw `0xD900`, write payload `00 D9`.
- Device readback: raw `0xD9FF`, decoded `-3.800390625 dB`.
- Difference: `255` raw units / `0.099609375 dB`; tolerance was `0.001 dB`.
- Five target bands: all read back exactly.
- Session/detach generations: `1/0` throughout; permission requests: `0`.
- Save commands: `0`; terminal stages: `VOLATILE_READBACK`, then `FAILED`.

This is a diagnosed physical failure, not a protocol fix or qualification result. It rules out a
detach/session replacement cause for this transaction and leaves device-side global-gain
quantization/firmware semantics, or a still-uncharacterized same-command response behavior, as
the remaining protocol questions. Independent public implementations corroborate the existing
`0x17` signed little-endian `2560`-scale codec but do not explain this `0xD9FF` response.

The technical export itself is not parser-valid: each nested object begins with `{,`. This is a
separate software export defect and is being corrected with a deterministic JSON parse test. It
does not alter hardware behavior. Do not repeat Flash or Reset until the export correction is
validated and a bounded protocol decision is made.

## 2026-09-25 exact signed diagnostic candidate

The diagnostic implementation is merged on `main` at `af8c68c35d320223a13c635fac69c0f2ebacdb3f`.
The signed candidate is J008 in the validation ledger:

- APK: [`EQ-Library-v0.7.0-beta-af8c68c.apk`](https://raw.githubusercontent.com/weekssa/OPRA-EQ-for-UAPP/mobile-test-apk/candidates/EQ-Library-v0.7.0-beta-af8c68c.apk).
- APK SHA-256: `3b442cbab3cf8be59a9b8e4ddd0d7028e93f8c4067d94dbb833a5bbb60b1d37e`.
- Signed-beta run: [#1363](https://github.com/weekssa/OPRA-EQ-for-UAPP/actions/runs/36180975485), artifact ID `10884371877`, ZIP SHA-256 `77477ad6bd52e9b114cf18e949368424d8d5c1dbc85246679c9c5fd561d5b44d`.
- Signer: `CN=OPRA EQ for UAPP, O=weekssa`, RSA 4096, certificate SHA-256 `65c1c1256dae3c49e3548f334c91f0ba991969e9be9e0b223ba4e253d2114747`; v2/v3 verified.
- R8 mapping SHA-256: `71036cf05464e6b84f07165e75c17c5a5cd5517a843bd1a8efb0bb49add0b374`.

This is a signed diagnostic artifact, not a support-qualified release. The owner must perform
only the one bounded session in the checklist, export the readable and JSON report, and stop on
missing raw evidence, an unknown baseline, an unexpected disconnect, or uncertain restoration.
