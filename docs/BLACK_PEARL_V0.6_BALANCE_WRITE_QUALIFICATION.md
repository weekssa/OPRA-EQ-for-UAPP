# TRN Black Pearl — v0.6 balance write qualification

Status: **SUPERSEDED — HISTORICAL SINGLE-CONTROL DRAFT**

This document records the earlier plan to qualify Black Pearl balance in a standalone physical session after the DAC reconstruction-filter write.

The project owner subsequently approved one consolidated hands-on round for the remaining normal DEVICE controls using small reversible changes. The authoritative physical checklist is now:

`docs/BLACK_PEARL_V0.6_DEVICE_CONTROLS_BATCH_QUALIFICATION.md`

Do **not** use this standalone balance checklist for current testing and do not pin a separate balance-only APK from this document.

## Historical scope

The earlier balance-only proposal would have tested:

`fresh complete read -> local balance adjustment -> Current/New review -> Apply -> balance-only write -> complete readback -> requested balance verification -> unrelated-state verification -> restore Center`

Its safety intent remains valid inside the consolidated checklist:

- begin from an actual current session read;
- use only a 1 dB change from Center;
- local slider/stepper movement must not write hardware;
- Apply must be explicit;
- success must wait for complete readback;
- unrelated DEVICE fields must remain unchanged;
- restore Center after the test;
- do not use the Black Pearl generic Save-to-Flash command;
- do not claim power-cycle persistence;
- stop immediately on mismatch, stale session, disconnect, transfer failure, inconsistent balance, or unrelated-state change.

## Current authority

The consolidated checklist independently records PASS/FAIL for balance, microphone gain, amplifier topology, gain mode, and playback/global level within one exact signed candidate. A later-control failure does not erase already-completed evidence for an earlier control that passed its own change/readback/restoration sequence.

Balance remains unqualified until its section of the consolidated physical checklist passes on the exact candidate pinned there.
