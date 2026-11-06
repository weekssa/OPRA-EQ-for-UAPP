# EXECUTION HANDOFF PROMPT — EW300 / JA11 parity

Use this only after the planning plan has been reviewed and accepted and a new task gate exists.

## Canonical paths

Project root:

/Users/stephenweeks/Documents/Codex/OPRA-EQ-development

Review packet:

/Users/stephenweeks/Documents/Codex/OPRA-EQ-development/docs/planning/ew300-ja11-parity

## Mode

You are the execution writer. Work in an isolated worktree from current **origin/main**. Use a local branch only. One writer. No GitHub push, PR, tag, or release.

Read first:

- AGENTS.md
- the accepted plan
- the task gate file
- docs/planning/ew300-ja11-parity/BRIEF.md
- docs/planning/ew300-ja11-parity/RESEARCH_SOURCES.md
- docs/planning/ew300-ja11-parity/FUNCTIONAL_MAPPING.md
- docs/planning/ew300-ja11-parity/TEST_EVIDENCE_LOG.md
- docs/CHATGPT_PROJECT_RUNBOOK.md, docs/PROJECT_STATE.md, docs/OPERATING_RUNBOOK.md
- the exact EW300/JA11 files named by the plan

Run the workflow checks required by the gate:

- /Users/stephenweeks/Documents/Codex/Codex-Usage-Policies/tools/selftest
- tools/flow-check with the gate's turn/stop values
- tools/scope-guard before any commit

## Outcome

Implement the accepted experimental scope for the SIMGOT EW300 DSP My DAC page:

- preserve the current working five-band Peak EQ, Apply, Flash, Reset, reconnect, capture, and output-gain paths;
- add the accepted JA11-parity features that exact EW300 evidence supports;
- show truthful unavailable states for the rest;
- expose only proven native filter types;
- implement or reject session-only Apply vs persistent Flash according to the plan;
- keep Ellyn, firmware cross-flash, arbitrary register writes, and unsupported JA11 rows out;
- produce a local debug APK for the owner's Pixel after emulator validation.

## Autonomy

Local implementation, test, build, and emulator iteration is pre-authorized. Continue through implementation, targeted tests, full tests, lint, build, and emulator runs without asking for permission at each step. Fix failures and keep going while the work is safe and in scope.

Do not stop after a single test, a failed test, a passed test, a rebuild, an emulator restart, or a new test case. If a test fails, diagnose it, fix it, rerun it, and continue. If a fix requires more tests, run them. Do not ask whether to try another test or whether to continue because it will take more work.

Stop only when:

- a material product choice cannot be resolved from the plan;
- a physical device action is required and is not yet authorized;
- the gate turn/stop limit would be exceeded;
- a destructive or irreversible action would be required;
- the accepted plan conflicts with current repository evidence;
- a test failure cannot be resolved without changing product scope or without owner input.

Do not stop merely because the work is long, repetitive, or inefficient. Do not claim success because the app compiles. Keep going until the accepted gates pass or a real stop condition is reached. If context is compacted, resume from the progress log.

The gate turn cap is a safety boundary, not a per-test checkpoint. Confirm the gate has enough turns and a late enough stop time for the full loop before starting. If the cap is reached, report the exact state and continue in a fresh turn; do not ask whether to run another test.

## Brownfield rule

The existing EW300 path works. Before changing it:

1. Run the current EW300-focused tests and record the result.
2. Add or confirm regression tests for current read/edit/Apply/Flash/Reset/reconnect/capture/output-gain behavior.
3. Make small changes.
4. Re-run the current tests after each slice.

Do not replace the flasher, transaction coordinator, session, editor, or optimizer wholesale. Extend them.

## Documentation discipline

Documentation is part of every slice, not a final cleanup.

- Keep FUNCTIONAL_MAPPING.md current as a living JA11-to-EW300 feature/register/byte/UI/test/status/confidence map.
- Add an append-only TEST_EVIDENCE_LOG.md entry after every test run that informs the design.
- Record source SHA, artifact, environment, exact command, exact bytes, observed result, what it proves, what it does not prove, restoration, and artifact paths.
- Update the maintained EW300 protocol notes, capability matrix, validation ledger, and project runbook when a durable fact changes.
- Keep RESEARCH_SOURCES.md current. If a capability or register question remains unresolved, run a targeted internet search and record the query, source, license, claim, confidence, and decision.
- Do not copy GPL-3.0 or unlicensed code. Use behavior descriptions and exact-device evidence.
- Never delete evidence; mark superseded entries and link replacements.
- A test is not complete until its mapping row and evidence entry are updated.

## Hardware order

Keep the EW300 attached to the Mac until the final phase.

1. **EW300 on Mac:** read-only identity/state probes, golden-frame checks, and protocol conformance. No writes.
2. **Mac JVM:** protocol, codec, flasher, session, and capability tests.
3. **Android emulator:** UI, fake-transport flows, accessibility, stale/error states.
4. **Android emulator with USB passthrough:** optional; document whether it can claim the EW300. Do not block on it.
5. **Local debug APK:** build and hash it.
6. **Pixel over wireless ADB (USB fallback):** final phase only, after the readiness gate below.

Readiness gate for the Pixel:

- Mac read-only probe works and is documented;
- golden-frame and protocol tests pass;
- emulator UI and fake-transport tests pass;
- debug APK is built and hashed;
- physical test plan is approved, or the session is read-only;
- EW300 can be moved safely;
- rollback and exact-baseline restoration are documented;
- the owner has a one-page test guide.

Do not request the phone before this gate.

## Implementation slices

Follow the accepted plan's order. A typical order is:

1. Baseline/regression tests and a progress log.
2. Targeted research pass for any unresolved capability/register question, logged in RESEARCH_SOURCES.md.
3. Protocol/codec support for accepted filter types and exact fixtures.
4. Capability profile and editor spec updates.
5. Flasher/session support for accepted Apply/Flash behavior.
6. UI rows and states for accepted DEVICE/EQ parity.
7. About/read-only rows if accepted.
8. Accessibility and stale/error/empty states.
9. FUNCTIONAL_MAPPING.md and TEST_EVIDENCE_LOG.md updates for the slice.
10. Lint, full unit tests, debug build.
11. Mac-attached read-only probe and golden-frame checks; update mapping/evidence.
12. Emulator UI and fake-transport tests; update mapping/evidence.
13. Debug APK and final readiness check.
14. Pixel wireless-ADB session, read-only first; update mapping/evidence.
15. One-page owner test guide and final report.

Do not add a second protocol stack, a second editor, a second session, or a new navigation root.

## Test commands

Confirm the actual tasks in the checkout before running them. In this environment, prefer:

- ./tools/codex-android :app:testDebugUnitTest
- ./tools/codex-android :app:lintDebug
- ./tools/codex-android :app:assembleDebug
- ./tools/codex-android :app:connectedDebugAndroidTest

Use the smallest relevant test during iteration. Run the complete applicable gate on the coherent candidate head. Record exact command, source SHA, result, and failures.

Emulator use is expected for UI and fake-transport tests. Phone use is expected only after local gates pass. Do not claim emulator or phone evidence unless the command actually ran.

Mac-attached EW300 use is expected for read-only probes and golden-frame validation. Do not send a write while the device is on the Mac unless the accepted plan explicitly authorizes it.

## Physical boundary

Do not move the EW300 to the Pixel until the readiness gate passes. Do not send a hardware write until the accepted plan's physical gate is active and the exact candidate is identified.

When a physical test is authorized:

- verify the exact EW300 fingerprint;
- capture a fresh full baseline;
- use the exact approved operation only;
- use at most one Save per phase;
- tolerate re-enumeration;
- verify the replacement session;
- require final readback;
- prove power-cycle persistence where the plan requires it;
- restore the exact baseline;
- stop on permission, detach, mismatch, or uncertainty;
- never retry an uncertain Save;
- never flash firmware.

## Deliverables

At the end, report:

- branch/worktree, base SHA, and final SHA;
- files changed;
- parity rows implemented, deferred, or rejected;
- FUNCTIONAL_MAPPING.md and TEST_EVIDENCE_LOG.md status;
- RESEARCH_SOURCES.md status and any new source decisions;
- maintained-doc updates;
- Mac-attached read-only probe and golden-frame results;
- exact test commands and results;
- emulator evidence;
- debug APK path, package, version, and SHA-256;
- final-session readiness checklist;
- Pixel install/launch state;
- physical test state and limits;
- remaining risks and next action;
- confirmation that no GitHub push, PR, release, or unsupported hardware claim occurred.

Keep a progress log in the task folder so a compacted or fresh thread can resume.

