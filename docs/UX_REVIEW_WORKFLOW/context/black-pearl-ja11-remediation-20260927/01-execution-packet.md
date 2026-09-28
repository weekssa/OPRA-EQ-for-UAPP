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

The “no merge” boundary above governed implementation before owner approval. The owner later
authorized the minimum main integration required by the main-only trusted signing workflow; PR #50
was merged without tag/public release/hardware mutation, and workflow #1372 produced the exact
non-public candidate. The remaining gate is the owner Pixel 9 review.

## Follow-up terminal-result repair contract

For the owner-authorized follow-up, the stock Android UAPP routing prompt is expected and out of
scope. The app-owned `EDITOR_APPLY` result must render exactly one compact live-region surface from
the existing completed trace/status identity, use truthful Apply success/not-verified wording,
retain readable/technical reports, allow dismissal, expire only verified success on the controlled
test clock, and keep failure/uncertainty actionable until dismissal or recovery. The JA11
transaction, Save boundary, reconnect behavior, and retry policy remain unchanged.

## Read-only protocol-diagnosis follow-up

After the exact-candidate physical failure, the owner authorized one Black Pearl read-only
protocol-diagnosis capture. The follow-up may inspect native read request/response bytes and the
resulting My DAC snapshot, but may not infer a write correction or change wire bytes, gain math,
tolerance, ordering, retry policy, or hardware state without separate evidence. A raw capture that
matches the maintained codec is a diagnostic closure, not a physical qualification pass.

## Owner-authorized write-side diagnostic exception — 2026-09-27

After the read-only diagnosis, the owner explicitly authorized one Black Pearl Flash for diagnosis
only. The run used the isolated debug package and the AutoEq/Jaytiss Explorer representation, which
is distinct from the earlier Hifigues Explorer target. No automatic retry, Reset, Save, Restore, or
second mutation was permitted. The temporary package and instrumentation were removed afterward;
the observed hardware state is not restored by this worker and no source fix or candidate claim is
derived from the run.
