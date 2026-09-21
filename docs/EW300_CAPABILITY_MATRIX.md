# EW300 capability matrix

This matrix is the current product boundary for the exact EW300 identity. Every plausible
capability is classified so implementation does not imply unsupported hardware behavior.

| Capability | Decision | Evidence / product behavior |
| --- | --- | --- |
| Exact USB identity and HID interface 3 | SUPPORTED_AND_IMPLEMENTED | `EW300_DSP_PROTOCOL_NOTES.md`, E001; exact manufacturer/product/interface matcher. |
| Five-band native register read | SUPPORTED_AND_IMPLEMENTED | E001 and protocol fixtures; strict four-byte reads. |
| Direct-Hz frequency encoding | SUPPORTED_AND_IMPLEMENTED | Accepted acoustic/protocol evidence; bounded codec rejects out-of-range values. |
| Peak-only capture and canonical conversion | SUPPORTED_AND_IMPLEMENTED | E001 and current capability profile; playback gain remains outside captured EQ identity. |
| Peak Apply / Flash / Reset transaction | SUPPORTED_AND_IMPLEMENTED | Current guarded flasher, shared session gate, pre-Save volatile readback, one Save, final readback; current-head hardware edit and exact restoration were observed, but the candidate Apply operation trace is incomplete. |
| Save persistence | SUPPORTED_AND_IMPLEMENTED | E001 frozen exact-candidate evidence; current candidate must still complete the consolidated session before release claim. |
| Playback-gain device state | SUPPORTED_AND_IMPLEMENTED | E001 and bounded gain codec; tracked separately from canonical EQ. |
| Low-shelf / high-shelf production capture or Flash | INSUFFICIENT_EVIDENCE | Raw experiments are not enough to establish acoustic semantics; product stays Peak-only. |
| Disabled/unused-band semantics | INSUFFICIENT_EVIDENCE | Complete five-band representation required; no guessed disabled state. |
| Volume, headset, microphone, UAC, balance, firmware, bootloader, and other DAC controls | INSUFFICIENT_EVIDENCE | These controls are required for any exact hardware profile that genuinely supports them and has safe protocol evidence. No such support is established for this EW300 fingerprint, so the controls remain absent and unclaimed in this release; Black Pearl controls are not copied. |
| Erase, calibration, recovery, and cross-flash | UNSAFE_OR_OUT_OF_SCOPE | These destructive or vendor-level operations require separate exact protocol, safety, and restoration evidence; they are not part of the current EW300 release. |
| Unknown EW300 revisions or VID/PID-only matches | UNSAFE_OR_OUT_OF_SCOPE | Exact identity and capability profile reject them. |
| Automatic mutation retry | UNSAFE_OR_OUT_OF_SCOPE | No write or Save replay; uncertain state is terminal. |
| Replacement-session reconnect after Save | SUPPORTED_AND_IMPLEMENTED | Reconnect gate plus exact session generation/fingerprint checks; current hardware behavior showed expected re-enumeration and persisted readback after manual replacement authorization, while candidate operation telemetry remains incomplete. |
| Readable/JSON operation evidence | SUPPORTED_AND_IMPLEMENTED | DEVICE surface shares privacy-safe `Ew300OperationTrace`. |

Decision vocabulary is intentionally explicit: `SUPPORTED_AND_IMPLEMENTED`,
`HARDWARE_DOES_NOT_PROVIDE`, `INSUFFICIENT_EVIDENCE`, and `UNSAFE_OR_OUT_OF_SCOPE`.
