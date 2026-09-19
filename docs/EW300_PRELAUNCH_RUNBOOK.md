# SIMGOT EW300 DSP — Pre-launch Candidate Runbook

Status: replacement-beta execution plan, hardware validation pending, 2026-09-19
Owner: project owner approval is required for the final merge.

## Objective

Complete the SIMGOT EW300 DSP cable as an additive extension of EQ Library. The EW300 experience must use the same My DAC, My EQs, EQ Library, connection, permission, recovery, capture, and operation-feedback framework already established for the TRN Black Pearl. EW300-specific differences are capability-driven: five observed native PEQ bands, conservative filter exposure, and a global-gain/persistence contract that remains pending until exact-device qualification.

The deliverable is an installable pre-launch beta candidate using the existing EQ Library application identity. It is intended to replace the currently installed app during one consolidated owner validation session after all software gates pass. It is not merged to the production branch until the owner explicitly approves the merge.

## Product decisions already confirmed

- My DAC mirrors Black Pearl behavior wherever the EW300 supports the same operation and public/device evidence supports it.
- EW300 exposes the five observed raw bands for guarded readback and Personal EQ capture. Ordinary builds do not expose editor Apply, persistent Flash, Reset, or a mutating qualification action while their exact semantics remain unqualified.
- The canonical EQ pipeline is source-neutral; “EQ profile” includes OPRA, AutoEQ, community/general, imports, Personal EQs, and captured DAC EQs.
- The shared filter-capability model is reusable across EW300, Black Pearl, FiiO, and future targets; no DAC-specific filter semantics are inferred from a chip family.
- The normal Android USB permission prompt remains; the app must reuse permission and avoid duplicate prompts or reconnect loops.
- The final owner session is a complete pre-launch functional test, not another isolated diagnostic test.
- Existing Black Pearl behavior must pass a focused regression check.
- Final merge is an explicit owner-approval gate.

## Execution phases

### 1. Baseline and branch control

- Work only in `weekssa/opra-eq-for-uapp`.
- Preserve the current Black Pearl implementation and its qualified behavior.
- Use the EW300 candidate branch as the beta line; keep the production branch unchanged until approval.
- Record the exact source commit, application version, artifact digest, and CI runs for every candidate.

### 2. Complete the shared My DAC surface

Reuse the approved My DAC shell and shared session. The EW300 target must provide:

- exact device recognition and one authoritative connection/session;
- normal Android permission handling and permission-loss recovery;
- current versus Last read state;
- digital DAC/playback gain readback, excluded from EQ identity/capture (editing remains gated);
- five-band direct-Hz Peak readback and capture; non-Peak snapshots fail closed;
- response/headroom calculation using the shared hardware adapter for supported target representations;
- a truthful pending state for editor Apply, persistent Flash, and Reset until their exact transactions are qualified;
- Personal EQ capture with EW300 cable provenance;
- disconnect, reconnect, stale-session protection, and no automatic mutating retry.

Unsupported or unverified Black Pearl controls must not appear for EW300.

### 3. Stabilize the USB transaction and capability utility

- Keep HID I/O off the UI thread.
- Make connection/open idempotent so duplicate connection requests do not create competing sessions.
- Treat disconnects and unsolicited reports as recoverable transport events, not app-fatal exceptions.
- Use the qualified write/commit settle timing and require a fresh replacement session plus readback after a device restart/re-enumeration.
- Never report success from a stale or incomplete readback.
- Run the allowlisted Android-free read-only capability batch before any mutating plan. It must record the exact fingerprint, read-only register values, strict lengths, first-failure stop, state-known flag, and human-readable/JSON reports.
- Do not probe `0x53` or any other persistence candidate automatically. Only the controlled signed-beta workflow may expose the exact-commit, pinned-release-signer, confirmation-gated Save qualification described in `EW300_CAPABILITY_BATCH.md`; ordinary builds must hard-disable and omit the action from the UI.
- Require a passing read-only batch before that action. Preserve the complete baseline before mutation, use only the two documented safer reductions, send at most one Save in each phase, detect a real physical detach before advancing, and require two fresh full-state reads: temporary persistence and exact restoration.
- Treat an uncertain write, Save, restoration, or readback as terminal. Persist the recovery stage synchronously and never retry an uncertain mutation automatically.

### 4. Automated validation

Run the full repository gates on one exact candidate head:

- EW300 protocol, capability, optimizer, gain, five-band, reset, readback, session, and failure-path tests;
- Black Pearl regression tests and existing My DAC tests;
- Android unit tests, lint, debug/release assembly, catalog and priority gates;
- CodeQL/dependency checks and signed-candidate verification;
- APK digest and signer-certificate recording.

No candidate is handed to the owner until all required software gates pass.

### 5. Owner's single consolidated test session

The owner should only be asked to test after the complete candidate is ready. The session should cover:

1. Install the beta candidate as an in-place update and confirm existing EQ Library data remains present.
2. Connect EW300 and approve USB access if Android asks.
3. Confirm My DAC shows the device and reads playback gain plus all five bands; run and share the read-only report.
4. In the signed-only Save qualification, confirm the displayed source commit, approve the two small safer reductions, and follow its first 10-second unplug/reconnect instruction.
5. Continue once. If temporary values did not persist, stop with the safe `NOT_PERSISTENT` result; Flash/Reset stay locked and no product persistence claim is made.
6. If they persisted, allow exact baseline restoration, perform the second instructed 10-second unplug/reconnect, and finish. Stop immediately on any uncertain result; never tap the action again.
7. Only after a two-cycle PASS, edit one small Peak value, review, Apply, and confirm readback; Flash one known five-band profile and verify the result after power removal; then Reset and verify the defined flat state after power removal.
8. Confirm the readback/capture result can be saved as a Personal EQ with exact EW300 provenance, and confirm reconnect state remains truthful/current with no crash, ANR, repeated prompt, or manual reconnect loop.
9. Run one focused Black Pearl connect/Flash/reset regression check.

If any step fails, capture the exact on-screen message and stop; do not repeat writes until a new candidate is provided.

### 6. Merge handoff

After a passing owner session:

- prepare the merge candidate and release notes;
- attach the exact APK, digest, CI evidence, and physical-test record;
- summarize any intentionally unsupported controls;
- stop and request explicit owner approval;
- merge only after that approval.

Until then, the candidate remains a beta/testing build and no public production release is claimed.

## Definition of ready

The owner is needed only when all of the following are true:

- the shared My DAC/capture flow is implemented;
- EW300 five-band values are visible and safely read back; ordinary-build writes are absent and the signed-only qualification is exact-commit gated;
- crash/ANR and repeated permission/reconnect behavior is resolved in automated and candidate testing;
- Black Pearl regression remains green;
- the signed beta APK and one-session checklist are available;
- the merge remains blocked only on the owner's final physical PASS and explicit approval.
