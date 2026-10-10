# JA11 v0.8.1 Mission Constitution

Status: owner-approved mission contract, effective 2026-10-09.

This file governs the v0.8.1 JA11 mission and supersedes conflicting earlier mission instructions. It does not rewrite historical device observations or authorize merge, publication, or a public support claim.

## Mission boundary

- Complete the minimal safe FiiO JA11 v0.8.1 correction while preserving the published v0.8.0 application and its behavior.
- Preserve exact candidate provenance, regression evidence, physical evidence, failed results, and original device state. Keep private logs, serials, fingerprints, screenshots, and user data out of the repository.
- Keep work limited to JA11 connection/session safety, expected-reset transactions, truthful Flash/Reset/readback behavior, the prepared acceptance procedure, and evidence required to close those gates. Do not add unrelated controls or features.
- Do not change other DAC transport behavior through the JA11 serial fallback.

## Approved identity model: Model D

- A USB serial is optional continuity evidence. A missing, blank, unreadable, or unavailable serial must not block a valid JA11 session or operation.
- For every selection or reconnect, require exactly one supported JA11 candidate, current Android permission, an unambiguous supported device/interface/endpoints, a fresh claimed connection, current session generation, and the operation's required readback. Never select the first of multiple candidates.
- Compare serials when both the prior and current session provide usable serials. A mismatch fails closed. If either is unavailable, do not reject solely for that reason.
- Without a matching usable serial, report only that the requested state was verified on the sole returning supported JA11. Never claim that the same physical unit returned.
- Never substitute VID/PID alone for the candidate-cardinality, permission, live-session, generation, and readback gates.
- Explicitly prohibited: requiring a nonblank serial for JA11 operation, a first-session serial gate, or a manual unplug solely to prove serial continuity.

## Transaction and persistence safety

- Bind an expected-reset verification to its accepted logical request, requested value, original session and generation, observed detach, optional prior serial, and bounded reconnect wait. Do not add a standalone token/watchdog architecture for this correction.
- On an expected reset, invalidate the old session. Reacquire only one supported replacement with fresh permission/open/claim and a new generation, then perform authoritative fresh readback. If serials exist in both sessions, compare and reject mismatch.
- An unsolicited reconnect starts a new session and baseline; it does not inherit an old transaction.
- Never automatically replay a write with an uncertain result. An uncertain Flash, Save, Apply, Reset, or control operation is terminal for that attempt.
- Preserve the established User 1 write order: verify User 1 in the same current session before any Flash/Reset band or gain data writes; stop before data writes if selection/readback is uncertain.
- A Flash is one logical user action. Preserve the current protocol sequence and at most one Save for that action. Never resend data writes, Apply, or Save during reconnect recovery. Do not add a user-visible Save button or Save operation unless verified JA11 hardware behavior proves it is required.
- Use the minimum reset count required by the protocol and prepared acceptance plan. Do not create a reset or unplug solely to prove serial continuity.
- The late-Save correction retains the accepted operation through its bounded reconnect window and permits readback-only continuation on one valid replacement. It does not use an arbitrary longer sleep or replay any mutation.
- Do not make volume, preset, or other persistence claims without accepted physical evidence.

### Accepted late-Save physical finding

The latest physical Flash accepted exactly one Save. The JA11 detached about 677 ms later. The prior 350 ms observation then began final readback on the old session even though it was stale, so that readback failed. A fresh replacement session later existed, and its complete snapshot matched the captured original baseline. Do not retry Flash or Save to address this race. The corrective behavior waits for bounded fresh-session/readback resolution and permits only authoritative readback after reconnect; the historical Test C remains a failure for its exact source.

## Physical session policy

- Do all safe source, fixture, emulator, candidate, review, CI, and host preparation off-phone first. Do not access the Pixel until the exact candidate preflight is complete.
- Before each new physical session, send exactly `PHONE WINDOW READY — PIXEL + JA11 NEEDED` with the source SHA, APK SHA-256, purpose, planned writes/resets, Android interaction, expected occupancy, pass/fail meaning, and restoration obligation. Wait for the owner's confirmation before touching the phone.
- The first phone action is to install/verify the frozen diagnostic candidate and capture a read-only JA11 baseline proving exact app/source provenance, one supported candidate, permissioned current session, current generation, and complete baseline. Serial is diagnostic only and optional under Model D.
- If fresh Mic is Off, the first mutation restores Mic On with expected-reset handling and authoritative post-reconnect readback. If Mic is On, skip the write.
- Capture sufficient evidence, restore every changed value to its recorded original state, then send exactly `PHONE RELEASED — YOU CAN TAKE THE PIXEL BACK`. Do not keep the phone occupied for off-phone work.
- If any candidate, permission, cardinality, session, generation, write result, or readback is uncertain, stop mutation, preserve evidence, restore only when the safe restoration is known, and release the Pixel.

## Candidate and gate authority

- Freeze and report the tuple: production source SHA, exact PR head SHA, diagnostic APK SHA-256, package, version/versionCode, and signer certificate SHA-256. Every result must be tied to the tuple it actually tested.
- Source changes invalidate affected regression, app build, emulator, independent review, and CI evidence. Test-only changes invalidate the affected test gate; rebuild a production artifact only if required by the changed test validity. CI workflow changes invalidate the affected workflow checks only. Helper changes require helper fixtures and helper review; they do not invalidate the production build or emulator gate unless test validity actually changed. Documentation/evidence-only changes do not invalidate Android build or CI results.
- Rerun a gate only when its defined inputs changed or its earlier evidence is proven invalid. Preserve earlier candidate failures as historical evidence.
- Reviewers may identify findings and evidence gaps; only the owner-approved rules in this Constitution determine policy and acceptance gates.
- Keep merge, release, publication, and new public JA11 support claims behind their separate explicit owner approval.

## Constitution change control

If new evidence contradicts a rule, stop the affected implementation or physical operation. Report `CONSTITUTION AMENDMENT PROPOSED` with the current rule, new evidence, why the evidence invalidates it, the proposed replacement, and safety impact. Wait for owner approval before changing this Constitution or proceeding under the proposed rule.
