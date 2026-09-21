# EW300 capability matrix

This matrix is the current product boundary for the exact EW300 identity. Every plausible
capability is classified so implementation does not imply unsupported hardware behavior.

| Capability | Decision | Evidence / product behavior |
| --- | --- | --- |
| Exact USB identity and HID interface 3 | SUPPORTED_AND_IMPLEMENTED | `EW300_DSP_PROTOCOL_NOTES.md`, E001; exact manufacturer/product/interface matcher. |
| Five-band native register read | SUPPORTED_AND_IMPLEMENTED | E001 and protocol fixtures; strict four-byte reads. |
| Direct-Hz frequency encoding | SUPPORTED_AND_IMPLEMENTED | Accepted acoustic/protocol evidence; bounded codec rejects out-of-range values. |
| Peak-only capture and canonical conversion | SUPPORTED_AND_IMPLEMENTED | E001 and current capability profile; playback gain remains outside captured EQ identity. |
| Peak Apply / Flash / Reset transaction | SUPPORTED_AND_IMPLEMENTED | Current guarded flasher, shared session gate, pre-Save volatile readback, one Save, final readback; current-head physical result pending. |
| Save persistence | SUPPORTED_AND_IMPLEMENTED | E001 frozen exact-candidate evidence; current candidate must still complete the consolidated session before release claim. |
| Playback-gain device state | SUPPORTED_AND_IMPLEMENTED | E001 and bounded gain codec; tracked separately from canonical EQ. |
| Low-shelf / high-shelf production capture or Flash | INSUFFICIENT_EVIDENCE | Raw experiments are not enough to establish acoustic semantics; product stays Peak-only. |
| Disabled/unused-band semantics | INSUFFICIENT_EVIDENCE | Complete five-band representation required; no guessed disabled state. |
| Volume, headset, microphone, UAC, balance, firmware, bootloader, erase, calibration | HARDWARE_DOES_NOT_PROVIDE | No exact EW300 evidence; Black Pearl controls are not copied. |
| Unknown EW300 revisions or VID/PID-only matches | UNSAFE_OR_OUT_OF_SCOPE | Exact identity and capability profile reject them. |
| Automatic mutation retry | UNSAFE_OR_OUT_OF_SCOPE | No write or Save replay; uncertain state is terminal. |
| Replacement-session reconnect after Save | SUPPORTED_AND_IMPLEMENTED | Reconnect gate plus exact session generation/fingerprint checks; physical behavior pending current candidate. |
| Readable/JSON operation evidence | SUPPORTED_AND_IMPLEMENTED | DEVICE surface shares privacy-safe `Ew300OperationTrace`. |

Decision vocabulary is intentionally explicit: `SUPPORTED_AND_IMPLEMENTED`,
`HARDWARE_DOES_NOT_PROVIDE`, `INSUFFICIENT_EVIDENCE`, and `UNSAFE_OR_OUT_OF_SCOPE`.
