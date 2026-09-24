# SIMGOT EW300 DSP — Pre-launch Candidate Runbook

Status: 2026-09-22 continuation — E037-E040 close the exact-device physical mutation set; remaining owner check is non-mutating Personal EQ capture and Flash-review UI confirmation after a fresh signed candidate passes all gates.
Owner: project owner approval is required for merge/publication.

Current sections 5 and 6 control this closeout; older execution-phase wording is historical. The physical mutation set is accepted in E037-E040, so do not repeat it.

## Objective

Complete the SIMGOT EW300 DSP cable as an additive extension of EQ Library. The EW300 experience must use the same My DAC, My EQs, EQ Library, connection, permission, recovery, capture, and operation-feedback framework already established for the TRN Black Pearl. EW300-specific differences are capability-driven: five observed native PEQ bands, conservative filter exposure, and a playback/global-gain state that is transactionally covered by the exact-device qualification (E001, E037-E040).

The deliverable is an installable pre-launch beta candidate using the existing EQ Library application identity. It is intended to replace the currently installed app during one consolidated owner validation session after all software gates pass. It is not merged to the production branch until the owner explicitly approves the merge.

## Product decisions already confirmed

- My DAC mirrors Black Pearl behavior wherever the EW300 supports the same operation and public/device evidence supports it.
- EW300 exposes the five observed raw bands for guarded readback and Personal EQ capture. The historical Save qualification is not a product action. Apply, persistent Flash, and Reset are available only to the exact qualified fingerprint through the guarded product transaction.
- The canonical EQ pipeline is source-neutral; “EQ profile” includes OPRA, AutoEQ, community/general, imports, Personal EQs, and captured DAC EQs.
- The shared filter-capability model is reusable across EW300, Black Pearl, FiiO, and future targets; no DAC-specific filter semantics are inferred from a chip family.
- The normal Android USB permission prompt remains; the app must reuse permission and avoid duplicate prompts or reconnect loops.
- The remaining owner check is narrowly non-mutating; do not repeat any physical Apply, Flash, Reset, Restore, or Save qualification already accepted in E001/E037-E040.
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
- digital DAC/playback gain readback, excluded from EQ identity/capture;
- five-band direct-Hz Peak readback and capture; non-Peak snapshots fail closed;
- response/headroom calculation using the shared hardware adapter for supported target representations;
- exact-profile Apply, persistent Flash, and Reset with complete baseline, one Save maximum, fresh readback, and no automatic retry;
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
- Do not run the completed Save qualification again. The frozen exact signed-candidate report is the authoritative persistence evidence; the normal product build must omit the qualification action.
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

### 5. Owner's bounded non-mutating evidence check

Run only after the latest signed candidate's exact source SHA and all applicable Android CI, CodeQL, catalog, priority-coverage, dependency-submission, signing, installation, and cold-launch gates are verified in PR #23.

The accepted physical mutation set is E037-E040; do not repeat Apply, Flash, Reset, or exact-baseline Restore. Do not repeat E001 Save qualification. Use `docs/EW300_DSP_HANDS_ON_CHECKLIST.md` to:
- confirm the exact fingerprint and known read-only five-band/global-gain state;
- capture one Personal EQ and verify all five values and device provenance;
- open the saved EQ's My EQs Flash review and cancel before final write confirmation.

Stop on identity/state/provenance/UI uncertainty. No hardware write is authorized by this closeout step. Personal EQ capture remains not yet evidenced until that workflow is observed; do not describe it as unsupported hardware.

### 6. Merge handoff

After a passing owner session:

- prepare the merge candidate and release notes;
- attach the exact APK, digest, CI evidence, and physical-test record;
- summarize any intentionally unsupported controls;
- stop and request explicit owner approval;
- merge only after that approval.

Until then, the candidate remains a beta/testing build and no public production release is claimed.

## Definition of ready

The owner is needed only after the current PR #23 head has one matching signed APK and all applicable software/security/signing/install/launch gates pass on that same SHA. The remaining owner check is non-mutating and limited to exact-device read-only state, Personal EQ capture with value/provenance confirmation, and open/cancel of the My EQs Flash review. Physical Apply, Flash, Reset, exact-baseline Restore, and E001 Save qualification are already accepted and must not be repeated.

After that check, close the remaining evidence/scope and final review work. PR #23 remains draft and the release remains NO-GO until explicit owner approval for merge, publication, and any public EW300 support claim.
