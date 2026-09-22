# EW300 v0.7 bounded hands-on checklist

## Current evidence and no-repeat boundary

Owner-provided capability JSON (17) and operation JSON (11), (12), and (13) are tied to exact signed candidate source `7599dd52fc9e8c58c96e021f581b86a669dcc148`. They record a passing exact-device read-only report, successful Flash, exact-baseline Restore, and Reset. Each mutation report records one Save, zero permission requests before the first write, matching replacement identity, final readback, and known state; Restore explicitly records `restorationVerified=true`. The reports' replay/competing-job fields are unmeasured/null.

Do not repeat Apply, Flash, Restore, Reset, or the accepted E001 Save qualification. The RESTORE operation verified the original baseline at its completion; a separate later Reset then completed and may leave the device at flat rather than an arbitrary original state. The current report set does not prove My EQs was the Flash entrypoint, and it does not prove end-to-end Personal EQ capture UX.

The documentation-only closeout creates a new PR head. Do not use source 7599 as if it were the new head's signed provenance. If an owner UI check is still required, wait for PR #23 to show a fresh candidate whose source exactly matches the head and whose Android CI, CodeQL, catalog currentness, priority coverage, dependency submission, and signed-candidate workflow all pass. The exact APK/checksum/signer/artifact digest are in the live PR manifest. The docs-only delta does not require repeating hardware mutations.

## Authorized device boundary

The only qualified fingerprint is:

```text
vid=31b2|pid=111|manufacturer=LE XIAN|product=SIMGOT EW300 DSP|serial=2024-07-03-0000-0000-0000|interface=3
```

If any future non-mutating owner check is performed and the app reports another identity, stale/unknown state, or interface, stop. Do not test another revision.

## The only possible remaining owner check — non-mutating

Do this only after the fresh exact-head candidate is available, and only if existing evidence does not already answer it:

1. If the app still needs a state refresh for capture, confirm the exact fingerprint and known state. The supplied read-only report already passed, so do not repeat it just to recreate the same evidence.
2. Capture one Personal EQ from the known five-band Peak state. Confirm the saved item appears in My EQs and that the five frequency/gain/Q values and device provenance are correct. If you already captured and saved it on the matching candidate, use the existing evidence; do not capture it again.
3. The operation JSON does not identify whether Flash came from My EQs or EQ Library. If you know the successful Flash was launched from My EQs, no extra Flash-path check is needed. Otherwise, open the saved My EQ's Flash review, confirm expected profile/target, and cancel before final write confirmation.
4. Share only the saved-item/capture evidence and, if needed, screenshot of the canceled review. No operation report or broad logs are requested.

## Stop conditions

Stop without writing if identity/state is unclear, capture values/provenance do not match, Flash targets the wrong profile/device, a system chooser appears, a screen asks for final write confirmation, or any unexpected warning appears. Do not press Apply, Flash, Reset, Restore, or Save; do not run persistence qualification.

This check can establish only Personal EQ capture UX and, if not already demonstrated by the successful entrypoint, My EQs Flash-review availability. It does not create additional physical mutation evidence. Release remains NO-GO pending the current-head software/signing gates, remaining UI evidence if needed, final review, synchronized docs, and explicit owner approval.
