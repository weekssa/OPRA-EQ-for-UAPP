# FiiO JA11 capability matrix

This matrix is the current evidence boundary for the exact FiiO JA11 identity. Software
implementation does not imply physical support. Unknown behavior stays insufficiently evidenced
or unsafe rather than being inherited from another KT02H20-family device.

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
| Complete baseline capture and failed-operation restoration | INSUFFICIENT_EVIDENCE / SOFTWARE GAP | Current JA11 preflight reads program, global gain, and band 0 only; it does not capture all five bands or define exact restoration after failed Apply/readback. |
| Session-generation enforcement across Flash | PARTIAL / SOFTWARE CORRECTION PRESENT; COMPLETE GATE PENDING | The JA11 Android transport pins reads and ordinary writes to one session/detach generation; Save remains the explicit lifecycle exception and waits for its reconnect boundary. End-to-end Kotlin/Android validation is not yet run. |
| Output volume, presets, headset/UAC controls | SOFTWARE IMPLEMENTED; PHYSICAL PENDING | These controls are outside the failed EQ-gain root-cause boundary; owner reports that general controls work do not qualify Flash or persistence. |
| Firmware update, bootloader, cross-flash, raw command console | UNSAFE_OR_OUT_OF_SCOPE | No JA11 firmware mutation or arbitrary command surface is authorized in this task. |
| Public JA11 support/release claim | UNSAFE_OR_OUT_OF_SCOPE | Hardware qualification remains blocked by J001 and the missing exact transaction evidence. |

## Matrix rule

The only safe next step is to recover exact candidate provenance and a read-only, stage-aware
transaction trace. Do not convert J001 into a protocol change, tolerance change, retry, or
support claim without evidence that distinguishes stale response, firmware transformation,
packet semantics, timing, and intended-value errors.
