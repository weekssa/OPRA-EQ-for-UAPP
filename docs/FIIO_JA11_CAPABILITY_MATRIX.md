# FiiO JA11 capability matrix

This matrix is the current evidence boundary for the exact FiiO JA11 identity. Software
implementation does not imply physical support. Unknown behavior stays insufficiently evidenced
or unsafe rather than being inherited from another KT02H20-family device.

The last owner-tested signed candidate is source `609911e2e51a254fc6f45b87fbdf4106c0049740` with
immutable APK SHA-256 `583ff7014fc3c0977b6679cd8bf56d3a4f615411a629fa6014bab088ece082ef`, signer
certificate SHA-256 `65c1c1256dae3c49e3548f334c91f0ba991969e9be9e0b223ba4e253d2114747`, and signed-beta
run [#1362](https://github.com/weekssa/OPRA-EQ-for-UAPP/actions/runs/36165218849). This provenance
clears the software/artifact gate only; its exact physical result is recorded as J006 below and is
negative. The current signed diagnostic candidate is source `af8c68c35d320223a13c635fac69c0f2ebacdb3f`,
APK SHA-256 `3b442cbab3cf8be59a9b8e4ddd0d7028e93f8c4067d94dbb833a5bbb60b1d37e`, signer certificate
SHA-256 `65c1c1256dae3c49e3548f334c91f0ba991969e9be9e0b223ba4e253d2114747`, and signed-beta
run [#1363](https://github.com/weekssa/OPRA-EQ-for-UAPP/actions/runs/36180975485). It has no physical
result yet and is for diagnosis only.

| Capability | Decision | Evidence / boundary |
| --- | --- | --- |
| Exact JA11 USB identity, VID `0x2972`, UAC PIDs `0x0101`/`0x0102` | SUPPORTED_AND_IMPLEMENTED; physical pending | Strict allowlist and dynamic HID interface discovery are implemented. Physical identity/PID for J001 was not captured. |
| Five-band Peak/Low Shelf/High Shelf target representation | SUPPORTED_AND_IMPLEMENTED; physical pending | Shared finite-hardware adapter, complete five-slot target, and codec tests exist. J001 displayed a 9→5 optimized plan but did not provide readback values. |
| Global EQ gain `0x17` encoding/decoding | CODEC CORROBORATED; DEVICE SEMANTICS INSUFFICIENTLY_EVIDENCED | Repository and independent implementations use signed 16-bit little-endian, `2560` raw units/dB. J009 proves this candidate wrote `0xD900` (`-3.9 dB`) and the same-session device response returned `0xD9FF` (`-3.800390625 dB`). Do not widen tolerance or change codec math until the device-side quantization/firmware semantics are independently characterized. |
| Apply command and volatile readback | SOFTWARE-SUPPORTED; PHYSICAL SEMANTICS INSUFFICIENTLY_EVIDENCED | A supplied My DAC frame shows the optimized five-band target present while connected, which supports volatile application of the band plan. It does not prove the gain wire value or the exact transaction phase. |
| Save User 1 persistence | SOFTWARE-SUPPORTED; PHYSICAL VALIDATION PENDING | J009 records `saveCommandCount=0` because volatile verification failed before Save. The supplied report therefore proves neither Save behavior nor persistence; the later flat `0.00 dB` view remains a negative observation without a Save-stage result. |
| Unplug/reconnect persistence | INSUFFICIENT_EVIDENCE | A supplied post-reconnect frame shows User 1 flat with `0.00 dB`, consistent with the owner's report, but no exact candidate, raw final readback, power-cycle duration, or baseline/restoration record is attached. |
| Fail-closed mismatch handling | SUPPORTED_AND_IMPLEMENTED | A global-gain mismatch prevents Save in the first verification path. Do not weaken the `0.001 dB` check or suppress the error. |
| Same-command stale-response correlation | PARTIALLY NARROWED; NOT PROVEN SAFE | J009 has a valid same-command `0x17` response on an unchanged session with exact event ordering and no detach/reconnect, so session replacement is not the cause of that attempt. The protocol still lacks request identity beyond command matching; delayed same-command responses remain an unresolved risk. |
| Complete baseline capture and failed-operation restoration | BASELINE PROVEN; RESTORATION PENDING | J009 contains the complete pre-write program, global gain, and five-band baseline and the complete post-Apply target readback. It does not contain a post-operation restoration or persistence result. No automatic retry or restoration mutation is added. |
| Session-generation enforcement across Flash | SUPPORTED_AND_IMPLEMENTED; AUTOMATED GATES PASS; PHYSICAL PENDING | The JA11 Android transport pins reads and ordinary writes to one session/detach generation; Save remains the explicit lifecycle exception and waits for its reconnect boundary. Focused tests, Android CI, signed emulator install/cold launch, and the exact signed candidate all pass. Physical lifecycle behavior remains unqualified. |
| Output volume, presets, headset/UAC controls | SOFTWARE IMPLEMENTED; PHYSICAL PENDING | These controls are outside the failed EQ-gain root-cause boundary; owner reports that general controls work do not qualify Flash or persistence. |
| Firmware update, bootloader, cross-flash, raw command console | UNSAFE_OR_OUT_OF_SCOPE | No JA11 firmware mutation or arbitrary command surface is authorized in this task. |
| Public JA11 support/release claim | UNSAFE_OR_OUT_OF_SCOPE | Hardware qualification remains blocked by J006 and the missing J008 raw transaction/restoration evidence. |

## Matrix rule

The only safe next step is the single bounded owner session using J008’s exact signed tuple,
with a read-only baseline and stage-aware transaction trace before one controlled Flash. Do not
convert J001 into a protocol change, tolerance change, retry, or support claim without evidence
that distinguishes stale response, firmware transformation, packet semantics, timing, and
intended-value errors.

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
