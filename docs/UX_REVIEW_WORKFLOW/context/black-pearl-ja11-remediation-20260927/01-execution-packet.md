# Execution packet — Black Pearl and JA11 remediation

## Acceptance contract

The candidate is complete only when both named defects are closed in source and decisive tests:

- Black Pearl verified success is impossible without active-session baseline, complete ten-band native readback, raw global-gain match, and final session validity. Readable gain mismatch reconciles anti-stacking truth or blocks later mutation; missing/wrong-session readback leaves mutation unsafe until a fresh baseline.
- JA11 editing is discoverable only for a fresh verified active User 1 snapshot with all five bands and native global EQ gain, exact session/identity token, no busy/stale/restart state, and an implemented end-to-end path. Open/edit/review/cancel/back/close issue no writes. Apply is only from Review and re-reads the entire token immediately before the first write, then uses the existing exact five-band/quantized-global-gain/User 1/Apply/volatile-readback/one-Save/reconnect/final-readback transaction.

## Required truth states

Success wording must assert saved-and-verified final readback. Failure and uncertainty must say the requested state was not verified and must not suggest an automatic retry. Built-in JA11 programs and EQ Off do not expose a User 1 editor or invented coefficients.

## Validation order

1. Read-only source and maintained-document audit.
2. Focused failing tests before implementation where feasible.
3. Minimal production/test implementation.
4. Focused tests, complete unit tests, lint, debug assembly.
5. API 36 clean emulator instrumentation and accessibility/layout smoke validation.
6. Release/R8/security/static checks available in this repository.
7. Independent read-only review at the exact final source.
8. Exact signed provenance attempt through the existing non-public workflow only if it binds the branch SHA. If trusted signing is main-only, stop at `MERGE_APPROVAL_REQUIRED` or `SOFTWARE_READY_PENDING_SIGNED_CANDIDATE` as prescribed.
9. `READY_FOR_PIXEL_9` only if all software/emulator/review/signed-candidate gates are complete.

## Prohibitions

No hardware mutation, automatic retry, source/APK proof transfer, protocol reinterpretation, merge, publication, tag, main push, or public hardware-support claim.
