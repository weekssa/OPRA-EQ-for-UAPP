# Final physical qualification — NOT RUN

This is preparation, not permission to connect, pair, install or mutate. Execute only after the
owner confirms phone readiness and participation. Under the [recovery amendment](RECOVERY_AMENDMENT.md)
the private beta precedes merge and official signing for this workstream; merge, official signing and
publication remain separately owner-gated. The [sealed plan](PLAN.md) and original source remain
binding.

## Frozen identity and prerequisites

Record before any action: exact source SHA; APK path and SHA-256; package
`com.weekssa.opraeqforuapp.v090beta`; version `0.9.0-beta.2`/code16; signer certificate (local test
signer `0f89797b…`); launcher label "EQ Library 0.9 Beta"; plan SHA
`a108a2182a28a82252c64956216d31b6dcba9c67bf522a51d6d86a073d6e729b`; computer gate evidence;
Pixel9/Android version; exact JA11 firmware/identity, host adapter and sole-device cardinality;
actual current volume/program/mic/UAC; all five stored User 1 bands (frequency/gain/Q/type) and
global gain. If the stored bank cannot be captured authoritatively, do not invent it or begin an
unsafe restoration-dependent test.

Use the intended authorized connected phone if available. Otherwise consult the installed android-wireless-adb-recovery skill at the final stage only: inspect executable/version, target/authorization, mDNS/current endpoint, route and TCP reachability. Follow its connect/pair/disruptive-action boundaries; do not request pairing codes in chat or reset keys/network/ADB automatically.

The unresolved decisions are: whether the native transaction makes the selected program and volume
survive true power removal for **all five programs**, without changing the stored User 1 bank, global
gain, microphone or UAC; and whether the digital filter row is available on this JA11 and survives
power removal. The native path performs no microphone write and selects no helper program. The filter
row sends nothing unless the device returns a validated `0x11` reply.

## Consolidated sequence

- [ ] 1. Connect JA11 and grant USB permission; verify one supported candidate/current session.
- [ ] 2. Read relevant device configuration authoritatively, including available sample rate/firmware.
- [ ] 3. Capture actual known-good baseline and a reversible, exact restoration procedure. Historical lost bank is not restored by assertion.
- [ ] 4. Capture the stored User 1 bank read-only first (select User 1 at runtime, read the five bands and global gain, record them). Then apply the approved non-flat User 1 test bank with recorded global gain. Record requested volume and program; use safe listening conditions.
- [ ] 5. Change the volume and program through the normal DEVICE rows. Confirm each edit applies at runtime, then press **Set as Default…** once and confirm the disclosed brief disconnect. Check five identical band writes, one unchanged gain, one Apply, one Save(3), and zero mic/UAC writes; distinguish app request accounting from a bus capture.
- [ ] 6. Verify the fresh outcome, each band field/gain, volume/program and companions; confirm the startup line reads "Saved for startup and verified by readback." Stop on contradiction.
- [ ] 7. Owner physically removes JA11 power (not merely closing the app or dismissing USB permission).
- [ ] 8. Confirm detach and invalidation of the old session.
- [ ] 9. Owner reconnects JA11.
- [ ] 10. Establish required permission and a fresh session; reject ambiguity/stale identity and serial mismatch when usable serials exist.
- [ ] 11. Read device state before any mutation or replay.
- [ ] 12. Compare volume, program, each band frequency/gain/Q/type, global gain, stored User 1 bank, mic and UAC separately against expected values. A different field's retention cannot establish a pass. Repeat the pair save for at least one non-User 1 program and for User 1; never infer one program's persistence from another.
- [ ] 13. Verify no automatic replay, Save retry, mic fallback after native uncertainty, or reconnect mutation.
- [ ] 14. Force-stop/relaunch and prove fresh device-derived state rather than cached presentation.
- [ ] 15. Verify native mic preservation or fallback's restored original mic setting, as appropriate.
- [ ] 16. Run safe existing Flash/editor/Reset regressions against recorded reversible targets, verifying each result.
- [ ] 17. Digital filter: record whether DEVICE shows a mode or "Not available on this JA11". If available, record the reported mode, select each mode once, verify readback, and stop immediately on any bad or missing response, unexpected audio behavior, or companion drift. Filter persistence is unproven; confirm whether the mode survives power removal and report it as a separate claim.
- [ ] 18. Restore the complete actual baseline, including bank/gain/active program/volume/mic/UAC, through the approved safe operation sequence.
- [ ] 19. Verify restoration authoritatively, record observed outcome and limits, remove only this session's disposable test items, and release the phone.

## Stop and interpretation

Stop mutations immediately on ambiguity, contradictory readback, uncertain operation outcome, stale session, unexpected microphone change, unsupported/unsafe response, or unreliable restoration. Only read-only diagnosis/recovery follows uncertainty; never automatic Save/Flash retry or hidden fallback. Diagnose the exported evidence before any repeat; a changed APK creates a new identity and affected qualification.

PASS requires every mandatory field's current evidence and exact restoration. Runtime/final readback alone does not prove unplug persistence. A native failure is reported as unqualified native behavior; a fallback candidate must receive its own artifact identity and affected checks. FAIL or insufficient mandatory evidence means NOT READY FOR BETA. Append actual results to the hardware ledger with exact tuple, plan, claim boundaries and restoration status; do not mark this preparation as a physical record.
