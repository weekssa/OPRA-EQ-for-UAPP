# SIMGOT EW300 DSP — Pre-launch Candidate Runbook

Status: owner-aligned execution plan, 2026-09-18  
Owner: project owner approval is required for the final merge.

## Objective

Complete the SIMGOT EW300 DSP cable as a full extension of EQ Library. The EW300 experience must use the same My DAC, My EQs, EQ Library, connection, permission, Flash, reset, readback, recovery, and operation-feedback behavior already established for the TRN Black Pearl. EW300-specific differences are capability-driven: five native PEQ bands and the verified global-gain control.

The deliverable is an installable pre-launch beta candidate using the existing EQ Library application identity. It is intended to replace the currently installed app during owner testing. It is not merged to the production branch until the owner explicitly approves the merge.

## Product decisions already confirmed

- My DAC mirrors Black Pearl behavior wherever the EW300 supports the same operation.
- EW300 global gain and all five native bands are first-class device state.
- My DAC follows the established Black Pearl edit/review/apply/readback pattern rather than introducing a separate EW300 workflow.
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
- global gain readback and editing;
- five-band readback and editing, including filter type, frequency, gain, Q, and enabled/unused state where the device reports it;
- response/headroom calculation using the shared hardware adapter;
- Review changes before a write;
- Apply, final readback, and verified success/failure feedback;
- Flash from My EQs and EQ Library;
- Reset EQ to flat when the verified device transaction supports it;
- Personal EQ capture with EW300 cable provenance;
- disconnect, reconnect, stale-session protection, and no automatic mutating retry.

Unsupported or unverified Black Pearl controls must not appear for EW300.

### 3. Stabilize the USB transaction

- Keep HID I/O off the UI thread.
- Make connection/open idempotent so duplicate connection requests do not create competing sessions.
- Treat disconnects and unsolicited reports as recoverable transport events, not app-fatal exceptions.
- Use the qualified write/commit settle timing and require a fresh replacement session plus readback after a device restart/re-enumeration.
- Never report success from a stale or incomplete readback.

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
3. Confirm My DAC shows the device and reads global gain plus all five bands.
4. Edit one small gain/band value, review, apply, and confirm readback.
5. Flash a saved/library EQ containing nonzero global gain and five-band changes.
6. Allow any expected device reconnect to complete; confirm there is no crash, ANR, repeated prompt, or manual reconnect loop.
7. Confirm the final success notification and the displayed readback values.
8. Reset EQ to flat and confirm verified flat readback.
9. Disconnect/reconnect once and confirm the state remains truthful and current.
10. Run one focused Black Pearl connect/Flash/reset regression check.

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

- the full My DAC and shared library flow is implemented;
- EW300 gain plus five-band values are visible, editable, flashable, resettable, and read back;
- crash/ANR and repeated permission/reconnect behavior is resolved in automated and candidate testing;
- Black Pearl regression remains green;
- the signed beta APK and a one-session checklist are available;
- the merge remains blocked only on the owner's final physical PASS and explicit approval.
