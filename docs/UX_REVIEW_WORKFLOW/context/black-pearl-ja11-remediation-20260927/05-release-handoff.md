# Release handoff - Black Pearl and JA11 remediation

Status: `PHYSICAL_FAIL` for the exercised Black Pearl Direct Flash path; JA11 remains independently
`PHYSICAL_INCONCLUSIVE`. The exact signed candidate and software/emulator gates remain valid, but
the owner-authorized AFUL Explorer mutation did not verify its final native state.

## Exact signed candidate — owner Pixel 9 gate

Use only this candidate for the final owner review. It is a non-public testing artifact and does
not establish physical qualification or public hardware support.

- Repository: `https://github.com/weekssa/OPRA-EQ-for-UAPP.git`
- Owner-approved PR #50: merged into `main` at source SHA
  `b61f02e8c91656f14ffc639e4c6d937b2162a1b7`.
- Candidate target: `black-pearl-ja11`.
- Workflow: [Signed EQ Library Beta Candidate #1372](https://github.com/weekssa/OPRA-EQ-for-UAPP/actions/runs/36347148408),
  successful; 9m53s.
- APK: `EQ-Library-v0.7.0-beta-b61f02e.apk`.
- APK SHA-256: `276587734fc863277b83e4310c040cee22c27261075e21fa75d766ded9eef27e`.
- Package/version: `com.weekssa.opraeqforuapp`, `0.7.0` / code `7`.
- Signer certificate SHA-256: `65c1c1256dae3c49e3548f334c91f0ba991969e9be9e0b223ba4e253d2114747`.
- Signature verification: v2 PASS; v3 PASS; one signer; RSA 4096-bit certificate.
- Immutable artifact ID/digest: `10941292707` /
  `sha256:188d2e4b973e7aab420d4c7d3890dba3407415c1d2c8f935a418acdacdbd8fc3`.
- Emulator diagnostics artifact ID/digest: `10941003406` /
  `sha256:e041ee672816ff85ba0c866ff0bdfa8b8a616ec540f95c93f3fd632f19197d57`.
- Exact candidate URL:
  `https://raw.githubusercontent.com/weekssa/OPRA-EQ-for-UAPP/mobile-test-apk/candidates/EQ-Library-v0.7.0-beta-b61f02e.apk`.
- Signed emulator install: PASS (`Success`). Cold launch: PASS (`Status: ok`, `LaunchState: COLD`,
  `Activity: com.weekssa.opraeqforuapp/.MainActivity`, `Complete`).
- Before the owner-authorized physical session, Luna did not connect, mutate, flash, apply, reset,
  save, restore, or otherwise operate a DAC.

## Owner-authorized Pixel 9 result — Black Pearl / AFUL Explorer

This addendum supersedes the pre-session checklist below for the exercised Black Pearl path.

- Exact candidate: `b61f02e8c91656f14ffc639e4c6d937b2162a1b7`, APK SHA-256
  `276587734fc863277b83e4310c040cee22c27261075e21fa75d766ded9eef27e`, workflow #1372,
  immutable artifact `10941292707` / `sha256:188d2e4b973e7aab420d4c7d3890dba3407415c1d2c8f935a418acdacdbd8fc3`.
- Pixel: Google Pixel 9 / `tokay` / API 37 / wireless serial
  `adb-46141FDAQ003KZ-3AwgSo._adb-tls-connect._tcp`.
- DAC identity: TTGK Technology TE-C, VID `0x3302`, PID `0x43E8`, serial `330243E8260129`.
- Baseline: app-connected, verified Flat, 10 filters, active slot 1, playback gain `-25.00 dB`.
- One authorized action: My EQs -> AFUL -> Explorer -> Flash. No second mutation was attempted.
- Result: **PHYSICAL_FAIL** for requested-state verification. Terminal UI reported
  `Final hardware readback did not confirm the requested EQ` and
  `expected raw -7398, actual raw -6400`.
- The app correctly withheld success and instructed reconnect/refresh before any later hardware
  action. The existing ViewModel performed its normal read-only post-operation refresh; no retry,
  Reset, Save, Restore, reconnect, or owner-directed recovery write was performed, so restoration
  is **NOT VERIFIED** and the requested post-operation state remains unqualified.
- The Black Pearl truthfulness defect's failure-path assertion is **PASS**; the Direct Flash
  application/restoration gate is **FAIL**. Do not label Black Pearl physically fixed or publicly
  supported.

### Read-only recovery result

The owner-authorized recovery closed and reopened the app session, tapped `Connect` once, and
performed no hardware write. Fresh My DAC readback showed `Verified current hardware`,
`Matches My EQs` for `Explorer`, 10 filters, active slot 1, and playback gain `-25.00 dB` / raw
`-6400`; the requested target raw gain was `-7398`. This confirms partial PEQ application but not a
complete verified Flash or restoration.

## Latest authorized write-side diagnostic — not a candidate

After the read-only protocol capture, the owner authorized exactly one write-side diagnostic Flash.
The isolated debug package used the catalog AutoEq/Jaytiss Explorer profile, not the earlier
Hifigues community Explorer target. The one operation's post-operation My DAC surface showed
`Verified current hardware`, `Matches My EQs`, 10 filters, active slot 1, and playback gain
`-31.00 dB`.

This run is not a signed beta, not an exact-candidate result, and not evidence that the earlier
`-7398` gain mismatch is repaired. The temporary raw logger used the wrong byte position and
captured no write transfer result. The temporary package and instrumentation were removed; the
original signed package was left installed. No Reset, retry, Save, Restore, or second mutation was
performed. Current state is **REPAIR_REQUIRED**, not `READY_FOR_PIXEL_9`; no branch push is
recommended.

The physical Black Pearl state after this diagnostic was not restored by Luna because the approved
boundary prohibited an automatic restore. The owner should treat the device as containing the
AutoEq/Jaytiss diagnostic state until a separately authorized, exact restoration or follow-up test
is chosen.

## Historical pre-candidate source boundary — `9f5cb852994e1c88fce80598f249a97fae047429`

This section preserves the pre-integration evidence. It is superseded by the exact candidate
section above; its old `MERGE_APPROVAL_REQUIRED` state and missing-candidate statements do not apply
to the merged source.

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

## Historical pre-candidate software handoff and prior physical evidence

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

## Read-only Black Pearl diagnosis addendum — 2026-09-27

The owner-authorized read-only capture completed on the same Pixel 9 and exact Black Pearl identity
after the failed AFUL Explorer Flash. The diagnostic session reached Connected, refreshed My DAC,
and captured native reads without Flash, Reset, Apply, Save, Restore, or retry. Evidence is in
`/tmp/opra-black-pearl-diagnostic-20260927-195236/`; the filtered log SHA-256 is
`5250ef7845041f55facea336790e65940d3a37dd7d2b8ffbee84bf6f678f11bf` and the full log SHA-256 is
`7512e300995553cb4093bd4a1820e5b32558b7cea590b49b42994c6657003b04`.

The decisive read was global gain request `4B 80 03 ...` and response `4B 80 03 02 00 E7 FF FF ...`,
which decodes to raw `-6400` / `-25.00 dB`. This matches the prior failure's observed final gain
and the refreshed My DAC state. The maintained codec/read path is corroborated, but no write-side
defect is proven. The temporary diagnostic changes were reverted, so the branch has no new source
fix and no new beta candidate to push. The exact signed candidate remains a truthful-failure
candidate only; it must not be presented as a successful Black Pearl Flash candidate.

Current handoff status: `REPAIR_REQUIRED`. Do not request another hardware write from this evidence.
A future write-side diagnosis would require a separately authorized, exact operation with explicit
stop conditions; Luna did not mutate hardware during this diagnostic capture.

## Current engineering blocker — endpoint path requires owner decision

The supplied working reference uses the Black Pearl HID interrupt OUT endpoint when available and
falls back to HID `SET_REPORT` otherwise. The exact Black Pearl descriptor captured in this run has
OUT endpoint `0x05`; OPRA currently ignores it and uses `controlTransfer` for every write. This is
the leading software hypothesis for the earlier global-gain mismatch, but changing the endpoint
path is explicitly outside the current remediation scope.

Status is **REPAIR_REQUIRED / BLOCKED — boundary requires owner decision**. There is no signed beta,
no pushable source fix, and no Pixel 9 handoff. The required decision is whether to authorize one
narrow source change that uses the existing interrupt OUT endpoint for Black Pearl writes, preserves
the current HID reports/timing/retry policy, and retains control-transfer fallback when OUT is absent.
