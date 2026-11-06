# EW300 / JA11 parity — execution progress log

## Identity

- Base SHA: 5832bc5e9194cbbff7d9321e8a2bf10395d0c9c6 (five-goal continuation)
- Worktree: /Users/stephenweeks/.codex/worktrees/ew300-ja11-parity/OPRA-EQ-development
- Local branch: codex/ew300-ja11-parity
- Task gate: docs/implementation/ew300-ja11-parity-20261105/GATES.md (five-goal reseal 5ef34b797a53e153)
- Accepted plan: docs/implementation/ew300-ja11-parity-20261105/PLAN.md
- Current candidate SHA: 6c276b098a92e4bf169f06abe410e6ea99821eac
- Debug APK path: app/build/outputs/apk/ew300Parity/app-ew300Parity.apk
- APK SHA-256: 20993f8bc96066572cee9007d8632f07c6bbd136ecf8acf0644372a7ec24ebb3
- Pixel serial / wireless endpoint: not used in this local run
- EW300 location: Mac; reversible shelf/volume/mic tests run, baseline restored

## Hardware phase

- Mac read-only probe complete: yes (256/256 registers, only 0x52)
- Golden-frame tests complete: yes
- JVM tests complete: yes (123 debug EW300 / 866 full debug / 123 release EW300)
- Emulator UI tests complete: yes (Ew300FullParityFlowTest 3/3; prior parity 4/4)
- Emulator USB passthrough result: not attempted
- Debug APK built and hashed: yes
- Final-session readiness gate signed: readiness manifest verified; independent review pending
- EW300 moved to Pixel: no

## Research pass

- Last search date: 2026-11-04 (planning pass, appended to RESEARCH_SOURCES.md)
- Queries run: recorded in RESEARCH_SOURCES.md
- New sources added: planning pass only
- Sources rejected / license concern: GPL-3.0 and unlicensed sources remain behavior-only
- Unresolved capability question: headset-disable and UAC-mode vendor control; native shelf acoustics; active-stream sample rate
- Research decision: mic mute is a real UAC1 control; headset/UAC mode remain BLOCKED with exact descriptor evidence

## Persistent loop

- Last test command: ./tools/codex-android :app:testDebugUnitTest :app:lintDebug :app:assembleDebug :app:assembleEw300Parity :app:assembleEw300ParityAndroidTest
- Last result: PASS; 866 debug tests, 123 debug EW300, 123 release EW300, lint 0 errors, all builds, 4/4 full-parity emulator tests
- Current failure: none
- Fix in progress: none
- Next test command: final gate rerun on the reviewed head
- Continue without asking: yes
- Stop condition (if any): none
- Gate turns used / limit: 1 / 12
- Stop time: owner review availability open-ended per 2026-11-05 clarification
- Functional mapping rows updated: M-03, M-04, M-07, M-08, M-13, M-15, M-16, M-17, M-18, M-19, M-20
- Test evidence log entry: T-000 through T-004
- Maintained docs updated: EW300 protocol notes, capability matrix, validation ledger, runbook, project state

## Completed

- [x] Workflow selftest, flow-check, ocx health at execution start
- [x] DeepSeek global default restored (model + high effort)
- [x] Isolated worktree created from origin/main 180f4a96
- [x] Planning packet copied into the worktree
- [x] Accepted plan persisted; machine-readable contract added
- [x] Gate seeded and sealed; scope-guard IN SCOPE
- [x] Pre-change EW300 baseline suite recorded
- [x] Contract OWNS expanded on 2026-11-05 to the four UI wiring files required by the accepted plan (MainActivity, EqLibraryActions, EqLibraryApp, MyDacRootScreen) and resealed through scope-guard.
- [x] Runtime/save split, capability flags, editor type chips, five-band rows, About, explicit Save, and Reset confirmation implemented.
- [x] Read-only Mac probe written, self-tested, and run live.
- [x] JVM, lint, emulator, and build gates pass; APK hashed and readiness manifest verified.
- [x] Review P1 fixed: no-op and uncertain runtime gain paths rebase/fence the tracked delta; coverage added.

## Current slice

- Objective: exact die confirmation and KT0211L rescore
- Files: CHIP_CONFIDENCE.md, CHIP_MAP.md, GOALS.md, evidence
- Tests run: read-only CHP, UAC/HID, vendor APK/firmware, full local gates
- Result: exact die KT0211L; G3 CHIP-LIMITED; G2-headset CHIP-LIMITED on USB surface; 96% coverage; G1/G4/G5 complete
- Evidence: TEST_EVIDENCE_LOG.md T-013; CHIP_CONFIDENCE.md; CHIP_MAP.md
- Mapping update: complete for implemented/open rows
- Evidence-log entry: T-011

- [x] Pixel shelf engineering-control self-check: Low Shelf, High Shelf, and Flat each applied and read back exactly; zero Save in the sequence.
- [x] 24-state blind runner withdrawn from the app; shelf state controls relabelled engineering-only and kept behind the parity build flag.
- [x] Acoustic measurement path evaluated: no input device on this Mac, no rig available; shelf acoustics marked UNMEASURED.

## Next action

Run the single fresh gpt-6-luna review from the other account over the shelf A/B code, close findings, and run one findings-only confirmation pass. Acoustic qualification stays open and unclaimed.

