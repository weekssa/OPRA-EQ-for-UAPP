# EW300 parity — local readiness package

Status: **local-ready; physical qualification pending.** No push, PR, merge, publication, signing change, or public hardware claim has occurred.

## Candidate identity

- Base: `180f4a962a53d22d294311121dd49d92a570d89a` (verified origin/main, 2026-11-05)
- Branch: `codex/ew300-ja11-parity`
- Worktree: `/Users/stephenweeks/.codex/worktrees/ew300-ja11-parity/OPRA-EQ-development`
- Code candidate head: `4866db88385ddbf476554f02df87bc86c9229917` (includes the review P1 fix)
- This readiness file is a documentation-only follow-up commit on the same branch.

## APK

- Path: `app/build/outputs/apk/ew300Parity/app-ew300Parity.apk`
- File: `app-ew300Parity.apk`
- Size: 22,844,887 bytes
- SHA-256: `1599d98e9224264d7a88993a9e328159f63c6f256d12c422e339ddb86936e778`
- Package: `com.weekssa.opraeqforuapp.ew300parity`
- versionName: `0.10.0-ew300parity`
- versionCode: `22`
- Signer SHA-256: `945614206b73258162a2e0722ddcf4a53ae3018652e0fb65d2b9bcc3e38521a5` (Android debug certificate, local test signer)
- Updates: disabled in the parity variant
- Separate package: installs beside the public app

## Read-only Mac evidence

- Tool: `tools/ew300_readonly_probe.py`
- Self-test: PASS, only `0x52` frames can be emitted; `0x57/0x53/0x43/0x08/0x54` rejected; missing register and foreign identity fail closed.
- Live capture: 256/256 registers; every emitted command was `0x52`; no write or Save was sent.
- Identity: `31B2:0111`, `LE XIAN`, `SIMGOT EW300 DSP`, HID interface 3.
- Capture file: `/Users/stephenweeks/.codex/evidence/ew300/2026-11-05/ew300-readonly-capture.json`
- Capture file SHA-256: `030a38cd6cfd75661c9c935e509f7cbcd358747c131d91aa80314838bb672d71`
- Internal capture SHA-256: `f54de03cd8ff0a6a104b54936f130346dd0d086cf7ecebe5178d1bfe094bf17a`
- Observed state: firmware `1.0.0`; five-band Peak bank flat at 1 kHz / 0 dB / Q 1; output gain 0.

## Local test results

- Pre-change baseline: `./tools/codex-android :app:testDebugUnitTest --tests '*Ew300*'` — 15 classes, 97 tests, 0 failures.
- Current EW300 JVM: 15 classes, 112 tests, 0 failures, 0 errors, 0 skipped.
- Full debug JVM suite: 855 tests, 0 failures, 0 errors, 0 skipped.
- Release EW300 JVM suite: 116 tests, 0 failures, 0 errors, 0 skipped.
- Python probe tests: 7/7 pass; probe self-test PASS.
- Explicit-emulator Compose flow: `ANDROID_SERIAL=emulator-5554 ./tools/codex-android :app:connectedEw300ParityAndroidTest -Pandroid.testInstrumentationRunnerArguments.class=com.weekssa.opraeqforuapp.ui.screens.Ew300ParityFlowTest` — 4/4 pass on `ew300_parity_api36` (API 36).
- Lint: `./tools/codex-android :app:lintDebug` — 0 errors, 120 warnings, 2 hints.
- Builds: `assembleDebug`, `assembleEw300Parity`, `assembleEw300ParityAndroidTest` succeed.

## Behavior implemented

- Runtime editor Apply (`APPLY_RUNTIME`): product-allowlisted writes, same-session readback, zero Save.
- Runtime output gain (`SET_GAIN_RUNTIME`): stereo `0x66`, bounded range, readback verified, zero Save; tracked delta rebased on success, verified no-op, and uncertain-write paths.
- Explicit Save (`SAVE_RUNTIME`): fresh comparison, exactly one `0x53`, expected reconnect, final readback; uncertain Save is not replayed.
- Truthful saved-session state: Runtime only / Saving / Saved in this session / Save not verified / Unknown.
- Five-band rows with accessible type/frequency/gain/Q values.
- Read-only About: model, firmware, USB identity, validation class, experimental limits.
- Capability-driven editor type chips: EW300 exposes Peak only; JA11 retains its types.
- Reset confirmation explicitly says it saves flat EQ to the DAC.
- Native Low/High Shelf editor exposure remains dormant; fixed shelf targets are future listening-test hypotheses only.

## Production isolation

- Default/release flags: `EW300_PARITY_EXPERIMENT_ENABLED=false`, `EW300_SHELF_QUALIFICATION_ENABLED=false`, `EW300_SHELF_EDITOR_ENABLED=false`.
- Production Apply remains persistent; Flash/Reset/reconnect/capture/output-gain paths are preserved.
- Experimental behavior is reachable only through the separate `ew300Parity` package.

## Final-session readiness checklist

- [x] Read-only Mac harness documented, self-tested, and captured successfully.
- [x] Golden/protocol and applicable JVM tests pass.
- [x] Explicit-emulator UI/fake-transport and accessibility checks pass.
- [x] Debug experiment APK built, hashed, signed, and identified.
- [x] Production flags and existing persistent paths verified by tests/build.
- [ ] Independent review complete.
- [x] Original-state capture/restoration procedure and rollback artifacts prepared (read-only capture; restoration procedure below).
- [ ] One-page listening guide and physical plan accepted, or session restricted to reads.
- [x] EW300 can be moved safely from Mac to Pixel when the owner authorizes it.
- [ ] Wireless ADB targets the exact Pixel; USB fallback arranged before DAC testing.

## Rollback and restoration

- Stop using the separate experiment package; return to the existing app.
- Uninstalling the experiment does not restore DAC settings. Complete any hardware restoration first.
- Before any physical mutation, capture the live baseline and the stored-startup baseline separately.
- Use at most one target Save and one restoration Save; never retry an uncertain Save.
- The read-only capture above records the current live flat state, not a stored-startup claim.

## Evidence and documentation

- `docs/planning/ew300-ja11-parity/FUNCTIONAL_MAPPING.md`
- `docs/planning/ew300-ja11-parity/TEST_EVIDENCE_LOG.md`
- `docs/planning/ew300-ja11-parity/RESEARCH_SOURCES.md`
- `docs/implementation/ew300-ja11-parity-20261105/PLAN.md`
- `docs/EW300_DSP_PROTOCOL_NOTES.md`, `docs/EW300_CAPABILITY_MATRIX.md`, `docs/EW300_VALIDATION_LEDGER.md`
- `docs/CHATGPT_PROJECT_RUNBOOK.md`, `docs/PROJECT_STATE.md`

## Limits

- No physical EW300 mutation, Save, power cycle, or restoration ran for this candidate.
- No native-shelf listening or acoustic qualification ran.
- No Pixel install/launch ran.
- No end-to-end Android USB emulation is claimed.
- Accepted physical records E001 and E037-E046 remain tied to their original sources/artifacts.
- Physical qualification pending.

## Next action

Run exactly one fresh `gpt-6-luna` review from the other account on the final candidate head, close findings, and run one findings-only confirmation pass. Then the owner can decide whether to begin the bounded physical session described in the plan.

