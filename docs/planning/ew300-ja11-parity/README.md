# EW300 ↔ JA11 parity planning package

Status: planning input, 2026-11-04. This is not a capability claim and not maintained product state. Do not merge it without folding the accepted decisions into the maintained runbook and capability matrix.

## Canonical path

Project root:

/Users/stephenweeks/Documents/Codex/OPRA-EQ-development

Review packet:

/Users/stephenweeks/Documents/Codex/OPRA-EQ-development/docs/planning/ew300-ja11-parity

Review order is listed in **REVIEW_PACKET.md**.

## What this package is for

The SIMGOT EW300 DSP's five-band Peak EQ and persistent Flash path already work on the attached device, but the EW300 My DAC page is bare bones compared with the FiiO JA11. The goal of this package is to plan a local experimental build that:

- preserves the working EW300 EQ/Flash/Reset/reconnect path;
- inventories every JA11 My DAC row and interaction;
- adds every EW300 capability that can be proven safely;
- shows a truthful "not available on this EW300" state for the rest;
- mirrors the JA11 layout where the underlying capability is real;
- records the feature/register/UI/test mapping and every test result for future changes;
- produces a debug build for the owner's Pixel after emulator testing;
- stays local until the owner explicitly moves it to GitHub.

## Files

- **BRIEF.md** — durable research and current-state brief. Read this first.
- **PLANNING_PROMPT.md** — paste this into planning mode.
- **EXECUTION_PROMPT.md** — use after the plan is accepted and a gate exists.
- **PROGRESS_TEMPLATE.md** — copy into the task folder and update after every execution slice.
- **REVIEW_PACKET.md** — absolute paths, review order, and the review checklist.
- **FUNCTIONAL_MAPPING.md** — living JA11-to-EW300 feature/register/UI/test map.
- **TEST_EVIDENCE_LOG.md** — append-only evidence record for every test run.
- **RESEARCH_SOURCES.md** — internet source map, licenses, search log, and repeatable search checklist.

## Recommended handoff

1. Open a planning thread in the repository.
2. Paste **PLANNING_PROMPT.md**.
3. Planning reads **BRIEF.md** and returns the plan, parity matrix, byte map, UI concept, implementation sequence, test plan, and gate proposal.
4. Review the plan and accept or amend it.
5. Create a new task gate and a fresh DeepSeek execution thread in an isolated worktree from current **origin/main**. Do not execute in the planning thread.
6. Paste **EXECUTION_PROMPT.md** plus the accepted plan and gate path.
7. Execution works autonomously through implementation, unit tests, lint, build, emulator tests, and a debug APK. Local test/build/emulator iteration is pre-authorized. It should not pause after each test or ask whether to run another one. It continues until the accepted gates pass or a real stop condition is reached.
8. Keep the EW300 attached to the Mac for as long as possible. Use it for read-only register probes, golden-frame validation, and protocol conformance. The Mac already has working HID access.
9. Use the Android emulator with a fake transport for app UI and flow tests. Attempt emulator USB passthrough only if it is practical; do not block the plan on it.
10. The final wireless-ADB Pixel session is the last phase. Start it only when the Mac probe harness, golden tests, emulator tests, and local debug APK are ready. Move the EW300 to the Pixel for that session. Do not request the phone earlier.
11. No firmware flash, no arbitrary register console, no GitHub push.

## Hardware order

| Phase | Hardware | Purpose | Entry condition |
| --- | --- | --- | --- |
| 1 | EW300 on Mac | Read-only register probes, identity/state capture, golden-frame validation | Planning accepted; no writes |
| 2 | Mac + JVM | Protocol codec, fixture, flasher, and session tests | Probe results and fixtures available |
| 3 | Android emulator | App UI, fake-transport flows, accessibility, error/stale states | App builds and unit tests pass |
| 4 | Android emulator with USB passthrough (optional) | End-to-end Android USB path if the emulator can claim the EW300 | Phase 3 stable; passthrough works |
| 5 | Local debug APK | Installable candidate for the Pixel | Local gates pass |
| 6 | Pixel over wireless ADB (USB fallback) | Final real-device read-only check, then the approved bounded physical test | All prior phases pass and the readiness gate is signed |

## Gate setup

When you seed the task gate, set the turn cap and stop time large enough for the full implementation, test, build, and emulator loop. The cap is a safety boundary, not a per-test checkpoint. If the cap is reached, execution should report the exact state and continue in a fresh turn; it should not ask whether to run another test.

## Non-negotiable boundaries

- Preserve the existing working EW300 EQ/Flash path; do not replace it wholesale.
- The EW300 stays on the Mac until the final Pixel session is explicitly ready.
- The execution thread is persistent: a failed test, a rebuild, an emulator restart, or more work is not a reason to hand control back.
- Documentation is part of the test loop: update FUNCTIONAL_MAPPING.md and TEST_EVIDENCE_LOG.md after each slice, not at the end.
- A fresh internet search pass is part of planning and of any unresolved capability decision. Record it in RESEARCH_SOURCES.md.
- Ellyn is JA11 Tuning (DCR/gate), not an EQ filter. It is excluded; on EW300 the same registers are analog PGA.
- "Flash" means the 0x53 EQ persistence save. Firmware cross-flashing is out of scope.
- Do not invent JA11 rows that the EW300 cannot support.
- Do not claim shelf acoustics from a raw type-byte echo.
- Do not write USB identity or arbitrary configuration registers.
- Do not repeat accepted physical tests merely because the source SHA changed.
- No push, PR, tag, or release without explicit owner approval.

