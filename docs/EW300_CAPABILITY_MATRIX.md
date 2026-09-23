# EW300 capability matrix

This matrix is the current product boundary for the exact EW300 identity. Every plausible
capability is classified so implementation does not imply unsupported hardware behavior.

**Physical evidence (2026-09-22):** E043-E046 are owner reports from exact signed executable
source `7599dd52fc9e8c58c96e021f581b86a669dcc148`. Read-only passed; Flash, exact-baseline
Restore, and Reset each completed with one Save, zero permission requests before first write,
matching replacement identity, final readback, and known state. Restore records
`restorationVerified=true`. Replay/competing-job fields remain null/unmeasured. Nine later
commits to pre-criteria source `b11190f9bbc08326f963190ad8b5a9f4b0872b2c` changed docs only.
E048 records that b11190's six exact-head gates and signed candidate passed. A newer docs commit
requires fresh gates/signing, not a physical retest. Capture UX remains not yet evidenced.

The testable v0.7 finished-product and cross-DAC acceptance contract is
`docs/V0.7_PRODUCT_SUCCESS_CRITERIA.md`. Do not interpret “Black Pearl parity” as permission to
copy hardware controls; classify each category for this exact EW300 profile.

## 2026-09-22 independent capability research close-out

The Black Pearl-to-EW300 category crosswalk was re-audited against exact-product material and
current third-party KT02H20-family references before UX close-out. The result does **not** promote
any additional EW300 DEVICE control.

- Product/manual material for the EW300 DSP cable describes an integrated microphone and one
  in-line media/call control button, while listing no in-line volume control. This is useful
  exact-product UI evidence, but it does not prove whether firmware exposes an independent
  software-settable DAC playback level.
- `gxcreator/ktmicro-tools` documents generic KT02H20 analog/digital gain controls, including
  register `0x66` as model-dependent digital DAC gain. Its BSD-3-Clause license was reviewed. This
  corroborates the family-level meaning of the already observed field; it does not establish the
  EW300's intended independent volume UX, safe range, persistence contract, or user-control
  semantics.
- `jeromeof/devicePEQ` and `Bukutsu/glacier-eq` show that `0x31B2` and even `0x31B2:0x0111` are
  reused across multiple products/profiles with differing constraints. Glacier is GPL-3.0 and is
  research-only here. These references reinforce the exact manufacturer/product/serial/interface
  gate and are not authority to inherit another device's controls.
- No code was copied from third-party references. Their behavior is corroboration/discovery input
  only; accepted EQ Library physical evidence remains the authority for mutation support.

Accordingly, `0x66` remains **playback/global-gain device state used by guarded EQ transactions**,
not a standalone EW300 volume row. The physical microphone does not imply an app-controllable
microphone gain/control path. Lack of evidence is never converted to `HARDWARE_DOES_NOT_PROVIDE`.

| Capability | Decision | Evidence / product behavior |
| --- | --- | --- |
| Exact USB identity and HID interface 3 | SUPPORTED_AND_IMPLEMENTED | `EW300_DSP_PROTOCOL_NOTES.md`, E001; exact manufacturer/product/interface matcher. |
| Five-band native register read | SUPPORTED_AND_IMPLEMENTED | E001 and protocol fixtures; strict four-byte reads. |
| Direct-Hz frequency encoding | SUPPORTED_AND_IMPLEMENTED | Accepted acoustic/protocol evidence; bounded codec rejects out-of-range values. |
| Peak-only capture and canonical conversion | SUPPORTED_AND_IMPLEMENTED | Five-band Peak readback/conversion is implemented and the exact profile is qualified. End-to-end Personal EQ capture UX has not yet been physically evidenced; this is an evidence gap, not a claim of unsupported hardware. Playback gain remains outside captured EQ identity. |
| Peak Apply / Flash / Reset transaction | SUPPORTED_AND_IMPLEMENTED | Guarded flasher, shared session gate, pre-Save volatile readback, one Save, final readback, exact replacement-session checks, and validation-only exact-baseline restoration are implemented. Earlier failures E031/E033 are superseded by verified E037-E038. Apply is physically verified on source 381 (E039); Flash/restoration are verified on source 381 (E037-E038) and exact signed source 7599 (E044-E045); Reset is verified on 381 (E040) and 7599 (E046). The pre-criteria source b11190 gates passed (E048); later software/documentation changes require fresh exact-head gates but do not erase accepted physical evidence. Personal EQ capture UX remains not yet evidenced. |
| Save persistence | SUPPORTED_AND_IMPLEMENTED | E001 is the accepted frozen exact-candidate persistence qualification; E037-E040 and E044-E046 are separate verified operations. Do not repeat E001. The latest operation counters remain truthful; replay/competing-job fields are unmeasured/null. |
| Playback-gain device state | SUPPORTED_AND_IMPLEMENTED | E001 and bounded gain codec; tracked separately from canonical EQ. Generic KT02H20 references corroborate `0x66` as digital DAC/playback gain, but exact EW300 evidence qualifies it only as state used by guarded EQ transactions/restoration. |
| Source low-shelf / high-shelf adaptation | SUPPORTED_AND_IMPLEMENTED | Full-response fitting can produce a bounded `Optimized` five-band Peak representation for source low/high shelves when RMS/max-error gates pass. This is not native EW300 shelf readback, capture, editor, or Flash support. |
| Native low-shelf / high-shelf capture or Flash | INSUFFICIENT_EVIDENCE | Native EW300 transport/product semantics remain Peak-only; raw experiments and family-level shelf support are not enough to establish native EW300 shelf behavior. |
| Disabled/unused-band semantics | INSUFFICIENT_EVIDENCE | Complete five-band representation required; no guessed disabled state. |
| Standalone playback-volume control | INSUFFICIENT_EVIDENCE | Register `0x66` is verified global-gain state/baseline and participates in qualified EQ headroom/restoration. Exact-product material says the in-line control has no volume buttons, while generic KT02H20 tools expose digital/analog gain controls. Neither fact proves an exact EW300 independent software volume contract, so no volume row/write is exposed. |
| DAC digital-filter selection | INSUFFICIENT_EVIDENCE | Generic-family tools may expose filter/control families, but no exact EW300 control/readback/write evidence establishes a DAC reconstruction-filter selector. |
| Gain mode or amplifier/output-stage topology | INSUFFICIENT_EVIDENCE | No exact EW300 control/readback/write evidence. Generic PGA/gain registers are not inherited. |
| Left/right balance | INSUFFICIENT_EVIDENCE | No exact EW300 control/readback/write evidence. |
| Microphone gain or microphone controls | INSUFFICIENT_EVIDENCE | Exact-product material establishes that the cable has a microphone and media/call button, but no exact EW300 DSP register/control/readback semantics establish app-managed microphone gain or headset-control configuration. |
| UAC mode detection, switching, or startup helper | INSUFFICIENT_EVIDENCE | No exact EW300 UAC behavior is established; Black Pearl's UAC behavior is not inherited. |
| Firmware/version display or update | INSUFFICIENT_EVIDENCE | No exact EW300 firmware identity/update semantics established; firmware/bootloader commands remain out of scope. |
| Headset/output-routing controls | INSUFFICIENT_EVIDENCE | The physical media/call button does not establish a programmable routing/headset-control setting. No exact EW300 control/readback/write evidence. |
| Factory device defaults | INSUFFICIENT_EVIDENCE | Reset EQ to flat is qualified; factory-default values/reset semantics are not established. |
| Reported exact device identity / connection freshness | SUPPORTED_AND_IMPLEMENTED | Exact fingerprint and current-versus-last-read session freshness are available in the shared My DAC state; only the exact identity is authorized. |
| Manual EQ/DEVICE Refresh through authoritative session | SUPPORTED_AND_IMPLEMENTED | The shared session owns current readback. v0.7 product close-out adds a compact manual Refresh escape hatch that rereads current state without inventing a second connection/session path. |
| Erase, calibration, recovery, and cross-flash | UNSAFE_OR_OUT_OF_SCOPE | These destructive or vendor-level operations require separate exact protocol, safety, and restoration evidence; they are not part of the current EW300 release. |
| Unknown EW300 revisions or VID/PID-only matches | UNSAFE_OR_OUT_OF_SCOPE | Exact identity and capability profile reject them. Third-party family registries demonstrate that VID/PID reuse is not safe model identity. |
| Automatic mutation retry | UNSAFE_OR_OUT_OF_SCOPE | No write or Save replay; uncertain state is terminal. |
| Replacement-session reconnect after Save | SUPPORTED_AND_IMPLEMENTED | Reconnect gate plus exact session generation/fingerprint checks; E037-E040 and E044-E046 record replacement observed, exact identity/generation matching, and final readback on sources 381 and 7599 respectively. Android permission remains OS-controlled for a re-enumerated USB instance. |
| Exact post-Save baseline restoration | SUPPORTED_AND_IMPLEMENTED | Current software writes the captured qualified five-band pairs plus `0x66`, performs one guarded restoration Save, requires an exact fingerprint and strictly newer replacement generation after detach, and verifies complete byte-for-byte final readback. Physical Flash/restoration are verified on source 381 (E037-E038) and on exact signed candidate source 7599 (E044-E045). Candidate 7599's exact-head gates passed (E047). Pre-criteria source b11190's exact-head software/security/signed-candidate gates passed (E048); later changes require fresh gates/signing but no automatic physical retest. Release remains NO-GO. |
| Readable/JSON operation evidence | SUPPORTED_AND_IMPLEMENTED | DEVICE surface shares privacy-safe `Ew300OperationTrace`; replay and competing-job counters are explicitly `unmeasured`/`null` when the build does not instrument them, not reported as measured zero. |
| Read-only EW300 Device tab state surface | SUPPORTED_AND_IMPLEMENTED | Shows verified playback/global-gain state, active Peak-band count, connection freshness, and readable/JSON operation status. It does not claim or copy Black Pearl DEVICE controls. |

Decision vocabulary is intentionally explicit: `SUPPORTED_AND_IMPLEMENTED`,
`HARDWARE_DOES_NOT_PROVIDE`, `INSUFFICIENT_EVIDENCE`, and `UNSAFE_OR_OUT_OF_SCOPE`.