# FiiO JA11 capability matrix

This matrix is the current evidence boundary for the exact FiiO JA11 identity. Software
implementation does not imply physical support. Unknown behavior stays insufficiently evidenced
or unsafe rather than being inherited from another KT02H20-family device.

The last owner-tested signed candidate is source `609911e2e51a254fc6f45b87fbdf4106c0049740` with
immutable APK SHA-256 `583ff7014fc3c0977b6679cd8bf56d3a4f615411a629fa6014bab088ece082ef`, signer
certificate SHA-256 `65c1c1256dae3c49e3548f334c91f0ba991969e9be9e0b223ba4e253d2114747`, and signed-beta
run [#1362](https://github.com/weekssa/OPRA-EQ-for-UAPP/actions/runs/36165218849). This provenance
clears the software/artifact gate only; its exact physical result is recorded as J006 below and is
negative. The current diagnostic source is `054298b866fad4ca97cb649790af54ccc6a4cfba`; it has no
signed artifact or physical result yet.

| Capability | Decision | Evidence / boundary |
| --- | --- | --- |
| Exact JA11 USB identity, VID `0x2972`, UAC PIDs `0x0101`/`0x0102` | SUPPORTED_AND_IMPLEMENTED; physical pending | Strict allowlist and dynamic HID interface discovery are implemented. Physical identity/PID for J001 was not captured. |
| Five-band Peak/Low Shelf/High Shelf target representation | SUPPORTED_AND_IMPLEMENTED; physical pending | Shared finite-hardware adapter, complete five-slot target, and codec tests exist. J001 displayed a 9→5 optimized plan but did not provide readback values. |
| Global EQ gain `0x17` encoding/decoding | SOFTWARE-SUPPORTED; PHYSICAL SEMANTICS INSUFFICIENTLY_EVIDENCED | Repository uses signed 16-bit little-endian, `2560` raw units/dB, matching independent public implementations. The supplied connected-device frame displays `-3.80 dB` against the Jaytiss plan's `-3.90 dB`; raw write/read bytes are missing, so device transformation, stale response, and codec interpretation remain distinct hypotheses. |
| Apply command and volatile readback | SOFTWARE-SUPPORTED; PHYSICAL SEMANTICS INSUFFICIENTLY_EVIDENCED | A supplied My DAC frame shows the optimized five-band target present while connected, which supports volatile application of the band plan. It does not prove the gain wire value or the exact transaction phase. |
| Save User 1 persistence | SOFTWARE-SUPPORTED; PHYSICAL VALIDATION PENDING | Save is sent only after successful pre-Save verification and final readback follows. The supplied `-3.80 dB` mismatch likely prevented Save in the older/current pre-Save path, while the later flat `0.00 dB` frame shows no persistence; exact Save count remains unproven. |
| Unplug/reconnect persistence | INSUFFICIENT_EVIDENCE | A supplied post-reconnect frame shows User 1 flat with `0.00 dB`, consistent with the owner's report, but no exact candidate, raw final readback, power-cycle duration, or baseline/restoration record is attached. |
| Fail-closed mismatch handling | SUPPORTED_AND_IMPLEMENTED | A global-gain mismatch prevents Save in the first verification path. Do not weaken the `0.001 dB` check or suppress the error. |
| Same-command stale-response correlation | INSUFFICIENT_EVIDENCE / PARTIAL GUARD | Command and band filtering exist, and JA11 reads/ordinary writes now reject a detach or session-generation change spanning the exchange. A valid delayed same-command response on an unchanged session still has no protocol sequence/request identity. Causality for J001 is unproven; do not change behavior without raw evidence. |
| Complete baseline capture and failed-operation restoration | SOFTWARE CORRECTION IN PROGRESS; PHYSICAL EVIDENCE PENDING | The diagnostic JA11 transaction now reads all five bands, active program, and global gain before any write and includes that baseline in the shareable report. Restoration remains owner-session evidence; no automatic retry or restoration mutation is added. The exact signed diagnostic candidate and physical result are pending. |
| Session-generation enforcement across Flash | SUPPORTED_AND_IMPLEMENTED; AUTOMATED GATES PASS; PHYSICAL PENDING | The JA11 Android transport pins reads and ordinary writes to one session/detach generation; Save remains the explicit lifecycle exception and waits for its reconnect boundary. Focused tests, Android CI, signed emulator install/cold launch, and the exact signed candidate all pass. Physical lifecycle behavior remains unqualified. |
| Output volume, presets, headset/UAC controls | SOFTWARE IMPLEMENTED; PHYSICAL PENDING | These controls are outside the failed EQ-gain root-cause boundary; owner reports that general controls work do not qualify Flash or persistence. |
| Firmware update, bootloader, cross-flash, raw command console | UNSAFE_OR_OUT_OF_SCOPE | No JA11 firmware mutation or arbitrary command surface is authorized in this task. |
| Public JA11 support/release claim | UNSAFE_OR_OUT_OF_SCOPE | Hardware qualification remains blocked by J001 and the missing exact transaction evidence. |

## Matrix rule

The only safe next step is the single bounded owner session using the exact signed tuple above,
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
