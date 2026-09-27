# Release handoff - Black Pearl and JA11 remediation

Status: `MERGE_APPROVAL_REQUIRED`

## Follow-up source boundary — `9f5cb852994e1c88fce80598f249a97fae047429`

The owner-authorized JA11 terminal-result repair is software-verified, independently reviewed,
and committed on `codex/black-pearl-ja11-remediation-20260927`. It is not ready for Pixel 9 review:
the trusted signing workflows remain main-only, no exact signed beta exists for this SHA, and the
earlier `acaf4dd` APK/Pixel session cannot prove this changed source.

- Refreshed `origin/main`: `acaf4dd32ddd9379ec2860e45e34fb8039219583`.
- Final local source SHA: `9f5cb852994e1c88fce80598f249a97fae047429`.
- Local debug APK SHA-256: `79e75c4a39e3a0aeb8b7231643ca4306e29db3578c23124028402b2945ac5fc6`.
- Local unsigned minified release APK SHA-256:
  `945e330a1eb510d68405603f19f50770f06abda6b931c77ce9dbe33d6d40d746`.
- Package/version: `com.weekssa.opraeqforuapp`, `0.7.0` / code `7`.
- Signer, workflow run, immutable artifact ID/digest: `NOT AVAILABLE` for this source.
- `actionlint`: `NOT RUN` because it is unavailable locally.
- No push, merge, tag, publication, DAC connection, or DAC mutation occurred in this follow-up.

### Software evidence for this source

- Full unit suite: PASS.
- API-36 emulator `codex-api36`, serial `emulator-5554`: focused JA11 terminal UI 4/4 PASS;
  full instrumentation 24/24 PASS.
- `lintDebug`, `assembleDebug`, `assembleRelease`, and R8 mapping verification: PASS.
- Independent read-only review: PASS; see `04-final-review.md`.

### Owner-controlled next step

The next action is not a hardware test. The owner must separately authorize the minimum main
integration or a narrowly reviewed combined-manifest/signing workflow path so the trusted workflow
can create an exact signed candidate for `9f5cb852994e1c88fce80598f249a97fae047429`. Until then,
the state is `MERGE_APPROVAL_REQUIRED`, not `READY_FOR_PIXEL_9`.

## Current software handoff

- Repository: `https://github.com/weekssa/OPRA-EQ-for-UAPP.git`
- Branch: `codex/black-pearl-ja11-remediation-20260927`
- Refreshed live base: `origin/main` `0adcc8159a467790104cf2dc797f1279ef2c53ed`
- Final implementation source SHA: `dc6478a25b1745b4f78f833c69e96e066c615d56`
- Promotion-preparation source SHA: `0f080516e8d4aa34e9d0a04b16464bf65b2b6d7d`
- Reviewed implementation/workflow head: `0c77e081fd6abd12a6e20b482ce269ae9f5bb764`
- Final documentation evidence head before owner-approved merge: `3e8ff5d3f6cb750a627f76aba50f09645c0b41d3`
  (documentation-only commits atop the reviewed implementation/workflow head).
- Owner-approved merge commit on `main`: `acaf4dd32ddd9379ec2860e45e34fb8039219583`.
- Review PR: [#49](https://github.com/weekssa/OPRA-EQ-for-UAPP/pull/49), owner-approved and merged;
  all required remote review checks passed on the final exact head.
- Worktree: `/Users/stephenweeks/.codex/worktrees/black-pearl-ja11-remediation-20260927/OPRA-EQ-for-UAPP`
- Black Pearl issue: software PASS; success remains final-readback verified and anti-stacking
  uncertainty is fail-closed.
- JA11 issue: software PASS; User 1-only editor path is complete through exact transaction and
  one-Save/final-readback result.
- Focused/full unit tests, lint, debug assembly, API 36 instrumentation, clean install/cold launch,
  large-text launch, and UI hierarchy smoke: PASS.
- Remote Android CI `#1874`, CodeQL `#1758`, priority-community `#1627`, catalog currentness
  `#2142`, and dependency submission `#2231`: PASS on the final documentation evidence head.
- Signed beta workflow run [#1371](https://github.com/weekssa/OPRA-EQ-for-UAPP/actions/runs/36336477287):
  PASS for `candidate_target=black-pearl-ja11` and exact source `acaf4dd32ddd9379ec2860e45e34fb8039219583`.
- Candidate APK: `EQ-Library-v0.7.0-beta-acaf4dd.apk`; SHA-256
  `af83a5e0148263057b1c43e3b775157ab6aedd9d2c1e3ab0558cef8fa3cea665`.
- Package/version: `com.weekssa.opraeqforuapp`, `0.7.0` / code `7`.
- Signer SHA-256: `65c1c1256dae3c49e3548f334c91f0ba991969e9be9e0b223ba4e253d2114747`.
- Immutable artifact ID/digest: `10937890771` /
  `sha256:88b1555e9a14642874110d05d5c5ae3554c25cc64b9385898a78287ccea9f52d`.
- Exact candidate: `https://raw.githubusercontent.com/weekssa/OPRA-EQ-for-UAPP/mobile-test-apk/candidates/EQ-Library-v0.7.0-beta-acaf4dd.apk`.
- Signed emulator install and cold launch: PASS; `Status: ok`, `LaunchState: COLD`.
- Hardware before the controlled session: NOT RUN by Luna. No DAC was connected or mutated.

## Pixel 9 controlled JA11 session — 2026-09-27

The exact signed candidate above was installed and verified on a Google Pixel 9 (`tokay`, Android
17/API 37). The FiiO JA11 was identified as VID/PID `0x2972/0x0102` in Android USB host mode.

- Baseline: User 1, five PEAK bands at 80/250/1000/4000/12000 Hz, all `+0.00 dB`, Q `0.70`,
  global EQ gain `0.00 dB`.
- Apply: one explicit reviewed change of Band 1 to `-1.00 dB`; final current hardware readback
  showed `-1.00 dB` and all other values unchanged.
- Restore: one explicit reviewed change of Band 1 back to `0.00 dB`; final current hardware
  readback showed the original flat state.
- USB host connection count: `273 -> 275 -> 277` across the two reconnect boundaries.
- The Android prompt to open USB Audio Player PRO appeared after each reconnect; the owner canceled
  both prompts. UAPP was not allowed to take ownership.
- Installed candidate SHA-256 after the session:
  `af83a5e0148263057b1c43e3b775157ab6aedd9d2c1e3ab0558cef8fa3cea665`.
- Evidence: `/tmp/opra-pixel-automated.ZRPv17/` (`editor-apply1-review.xml`,
  `apply1-after-cancel.png`, `editor-restore-review.xml`, `restore2-after-cancel.png`, and USB
  snapshots).

Classification: JA11 immediate readback and original-state restoration **PASS**; complete physical
qualification **PHYSICAL_INCONCLUSIVE** because the routing dialog obscured transient terminal
feedback and the post-dialog surface did not retain an observable editor success sentence or expose
an exported operation trace proving the exact Save count. Black Pearl remains **NOT EXERCISED**.
This does not establish power-cycle persistence, acoustic fidelity, or public hardware support.

## Why this stops before broader physical qualification

The trusted main-only boundary has been satisfied by owner approval. The exact signed candidate and
its provenance are now verified above. The remaining boundary is owner physical validation only.
This handoff does not claim either issue physically fixed or publicly supported. The bounded JA11
session has now occurred, but the evidence boundary above must be resolved or explicitly accepted by
the owner before `OWNER_ACCEPTED`.

## Pixel 9 checklist after an exact signed candidate exists

This checklist is actionable for the owner. Use only the exact candidate APK and record every result
against its source SHA, APK SHA-256, signer certificate, workflow run, and immutable artifact ID.

### Before connecting either DAC

- Confirm package `com.weekssa.opraeqforuapp`, version/code, signer, source SHA, and APK checksum
  match the immutable candidate manifest.
- Install cleanly or upgrade only after capturing the pre-test app/device state.
- Confirm the app identifies the exact TRN Black Pearl or FiiO JA11 identity; stop on ambiguity,
  permission prompt during mutation, disconnect, competing operation, or session replacement.
- Capture the baseline report and current state before any owner-authorized mutation.

### Black Pearl bounded review

- Use the existing approved exact Black Pearl profile and Direct Flash path only.
- Confirm progress text says the final hardware state is being verified.
- Confirm success appears only when all ten native fields and raw global gain match in final
  readback, with the compact verified wording.
- If any readback is missing/mismatched or the session changes, classify as NOT VERIFIED and do not
  retry automatically. Confirm later Flash/Reset is blocked until a fresh authoritative baseline.
- Restore the original hardware state using the approved owner procedure and verify restoration.

### FiiO JA11 bounded review

- Confirm current program is User 1. For Off/Vocal/Classic/Bass, confirm no User 1 coefficients are
  presented as current and Edit EQ is unavailable.
- With a fresh verified User 1 read, open Edit EQ and make local changes. Confirm no transport write
  occurs during open, edit, reset-local-edits, Review, Back, Close, or Cancel.
- Confirm Review lists all five type/frequency/gain/Q values, global EQ gain, headroom consequences,
  warnings, and that Apply is the first write.
- Apply once. Confirm exact five-band write, quantized global gain, User 1 selection, Apply, volatile
  readback, exactly one Save/reconnect boundary, final readback, and truthful success/failure.
- Stop on any mismatch, timeout, disconnect, permission prompt, or replacement session. Do not retry
  the mutation. Restore the original User 1 state and verify it.

### After the session

- Export readable and technical reports, checksum each artifact, record restoration and each DAC's
  independent result, and classify `PASS`, `FAIL`, or `INCONCLUSIVE`.
- Do not call either issue physically fixed or publicly supported from software/emulator evidence.
- Use `06-post-pixel-closure-prompt.md` for the next bounded closure update.
