# EW300 capability matrix

This matrix is the current product boundary for the exact EW300 identity. Every plausible
capability is classified so implementation does not imply unsupported hardware behavior.

**Latest evidence update (2026-09-22):** E043-E046 are owner-provided reports from exact signed
candidate source `7599dd52fc9e8c58c96e021f581b86a669dcc148`. The read-only snapshot passed, and
Flash, exact-baseline Restore, and Reset each completed with one Save, zero permission requests
before first write, matching replacement identity, final readback, and known state. The separate
Restore report has `restorationVerified=true`. Replay and competing-job counts remain null /
unmeasured. This adds evidence for the existing capability boundary; it does not add new EW300
controls. The docs-only follow-up must pass fresh exact-head CI/signing. Personal EQ capture UX is
still not evidenced by these operation reports.

| Capability | Decision | Evidence / product behavior |
| --- | --- | --- |
| Exact USB identity and HID interface 3 | SUPPORTED_AND_IMPLEMENTED | `EW300_DSP_PROTOCOL_NOTES.md`, E001; exact manufacturer/product/interface matcher. |
| Five-band native register read | SUPPORTED_AND_IMPLEMENTED | E001 and protocol fixtures; strict four-byte reads. |
| Direct-Hz frequency encoding | SUPPORTED_AND_IMPLEMENTED | Accepted acoustic/protocol evidence; bounded codec rejects out-of-range values. |
| Peak-only capture and canonical conversion | SUPPORTED_AND_IMPLEMENTED | Five-band Peak readback/conversion is implemented and the exact profile is qualified. End-to-end Personal EQ capture UX has not yet been physically evidenced; this is an evidence gap, not a claim of unsupported hardware. Playback gain remains outside captured EQ identity. |
| Peak Apply / Flash / Reset transaction | SUPPORTED_AND_IMPLEMENTED | Guarded flasher, shared session gate, pre-Save volatile readback, one Save, final readback, exact replacement-session checks, and validation-only exact-baseline restoration are implemented. Earlier failures E031/E033 are superseded by verified E037-E038. Apply is physically verified on source 381 (E039); Flash/restoration are verified on source 381 (E037-E038) and exact signed source 7599 (E044-E045); Reset is verified on 381 (E040) and 7599 (E046). The docs-only follow-up requires fresh gates, not a physical retest. Personal EQ capture UX remains not yet evidenced.
| Save persistence | SUPPORTED_AND_IMPLEMENTED | E001 is the accepted frozen exact-candidate persistence qualification; E037-E040 and E044-E046 are separate verified operations. Do not repeat E001. The latest operation counters remain truthful; replay/competing-job fields are unmeasured/null.
| Playback-gain device state | SUPPORTED_AND_IMPLEMENTED | E001 and bounded gain codec; tracked separately from canonical EQ. |
| Source low-shelf / high-shelf adaptation | SUPPORTED_AND_IMPLEMENTED | Full-response fitting can produce a bounded `Optimized` five-band Peak representation for source low/high shelves when RMS/max-error gates pass. This is not native EW300 shelf readback, capture, editor, or Flash support. |
| Native low-shelf / high-shelf capture or Flash | INSUFFICIENT_EVIDENCE | Native EW300 transport/product semantics remain Peak-only; raw experiments are not enough to establish native shelf behavior. |
| Disabled/unused-band semantics | INSUFFICIENT_EVIDENCE | Complete five-band representation required; no guessed disabled state. |
| Volume, headset, microphone, UAC, balance, firmware, bootloader, and other DAC controls | INSUFFICIENT_EVIDENCE | These controls are required for any exact hardware profile that genuinely supports them and has safe protocol evidence. No such support is established for this EW300 fingerprint, so the controls remain absent and unclaimed in this release; Black Pearl controls are not copied. |
| Erase, calibration, recovery, and cross-flash | UNSAFE_OR_OUT_OF_SCOPE | These destructive or vendor-level operations require separate exact protocol, safety, and restoration evidence; they are not part of the current EW300 release. |
| Unknown EW300 revisions or VID/PID-only matches | UNSAFE_OR_OUT_OF_SCOPE | Exact identity and capability profile reject them. |
| Automatic mutation retry | UNSAFE_OR_OUT_OF_SCOPE | No write or Save replay; uncertain state is terminal. |
| Replacement-session reconnect after Save | SUPPORTED_AND_IMPLEMENTED | Reconnect gate plus exact session generation/fingerprint checks; E037-E040 and E044-E046 record replacement observed, exact identity/generation matching, and final readback on sources 381 and 7599 respectively. Android permission remains OS-controlled for a re-enumerated USB instance. |
| Exact post-Save baseline restoration | SUPPORTED_AND_IMPLEMENTED | Current software writes the captured qualified five-band pairs plus `0x66`, performs one guarded restoration Save, requires an exact fingerprint and strictly newer replacement generation after detach, and verifies complete byte-for-byte final readback. Physical Flash/restoration are verified on source 381 (E037-E038) and on exact signed candidate source 7599 (E044-E045). Candidate 7599's exact-head gates passed (E047); this documentation-only follow-up requires fresh gates/signing but no physical retest because it changes no runtime behavior. Release remains NO-GO. |
| Readable/JSON operation evidence | SUPPORTED_AND_IMPLEMENTED | DEVICE surface shares privacy-safe `Ew300OperationTrace`; replay and competing-job counters are explicitly `unmeasured`/`null` when the build does not instrument them, not reported as measured zero. |
| Read-only EW300 Device tab state surface | SUPPORTED_AND_IMPLEMENTED | Shows verified playback/global-gain state, active Peak-band count, connection freshness, and readable/JSON operation status. It does not claim or copy Black Pearl DEVICE controls. |

Decision vocabulary is intentionally explicit: `SUPPORTED_AND_IMPLEMENTED`,
`HARDWARE_DOES_NOT_PROVIDE`, `INSUFFICIENT_EVIDENCE`, and `UNSAFE_OR_OUT_OF_SCOPE`.
