# EW300 v0.7 bounded hands-on checklist

This checklist supersedes earlier instructions in this file to run another Apply, Flash, Reset, or exact-baseline Restore. The accepted physical operation set is already recorded as E037-E040 on source `381286eb7ea29b0707e6da75f3565bada1d73928`. Do not repeat those mutations or the E001 Save qualification.

## Authorized device boundary

The only qualified fingerprint is:

```text
vid=31b2|pid=111|manufacturer=LE XIAN|product=SIMGOT EW300 DSP|serial=2024-07-03-0000-0000-0000|interface=3
```

If the app reports a different identity, unknown state, stale data, or a different interface/serial, stop. Do not test another revision.

## Candidate prerequisite

Wait until PR #23 identifies one exact signed beta candidate whose source SHA equals the current PR head and for which Android CI, CodeQL, catalog currentness, priority-community coverage, dependency submission, and signed-candidate workflow all pass on that same SHA. Use the APK filename, SHA-256, signer, and artifact digest recorded there. Do not install the older `3fbb968` candidate after the documentation-reconciliation commit creates a newer head.

## One bounded, non-mutating owner check

1. Install and open only the exact candidate from the current PR #23 candidate manifest.
2. Connect the exact qualified EW300. Approve Android's normal USB permission prompt if shown; do not use a system chooser to hand control to another app.
3. In My DAC, run the read-only capability report. Confirm the report names the exact fingerprint above, includes all five Peak bands and the playback/global-gain baseline, and reports `stateKnown=true`.
4. Use the existing capture action to save the current five-band Peak state as one Personal EQ. Confirm that the saved entry appears in My EQs and that its five frequency/gain/Q values and device provenance match the read-only state.
5. From that saved My EQ, open the Flash review only far enough to confirm the expected profile and target are shown. Cancel/back out before the final confirmation that sends any hardware write.
6. Share the readable capability report and JSON report, plus screenshots of the captured Personal EQ and the canceled Flash review. Do not export or share unrelated personal data.

## Stop conditions

Stop without retrying or mutating hardware if the exact identity is not shown, the report is not known/fresh, a captured value or provenance is missing/mismatched, Flash is absent or targets the wrong device/profile, another app chooser appears, or any screen asks to proceed with a write. Do not press Apply, Flash, Reset, Restore, or Save; do not run persistence qualification.

This check establishes only read-only identity/state, Personal EQ capture UX, and Flash-review availability. It does not create new physical Flash/Apply/Reset/Restore evidence. If all three checks are correct, the owner-facing product test gate is closed; final review, synchronized release evidence, and explicit owner approval remain. Release stays NO-GO until those release gates are complete.
