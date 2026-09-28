# Gates: Black Pearl and JA11 remediation

OWNS: app/src/**, docs/UX_REVIEW_WORKFLOW/context/black-pearl-ja11-remediation-20260927/**, docs/V0.6_MY_DAC_STATUS.md, docs/V0.7_RELEASE_READINESS_AUDIT.md, CHANGELOG.md

Scope: Implement and verify only the named Black Pearl final-readback truth correction and complete JA11 User 1 five-band My DAC editor/apply path, then prepare an exact owner handoff.

## Current source checkpoint — `eb761f1bc510a612acde7b71b453631e1ff23a8f`

This addendum supersedes the historical endpoint blocker below. The owner authorized one scoped
source change: use HID interrupt OUT when present, retain `SET_REPORT` fallback, preserve report
bytes/timing/retry policy, and perform no hardware write during development.

- Source: `codex/black-pearl-ja11-remediation-20260927`, based on refreshed `origin/main`
  `ce5efdf7985e4fc48f975b14fcedb1f592d43772`.
- Implementation: interrupt OUT is selected only for OUT+interrupt endpoints; complete 64-byte
  transfer is required; missing/zero/negative/short transfers fail closed; existing SET_REPORT
  parameters and result policy remain unchanged.
- Tests/gates: focused and full unit PASS; API-36 `codex-api36` instrumentation PASS (24/24); lint,
  lintVitalRelease, debug assembly, and release/R8 assembly PASS; `git diff --check` PASS.
- Independent review: final repaired-commit review recorded in `04-final-review.md`.
- `actionlint`: NOT RUN, unavailable locally; no workflow was changed.
- Exact signed candidate: NOT AVAILABLE for this branch SHA. Local debug and unsigned release APKs
  are not candidates. The separately authorized non-public branch was pushed after the software
  gates for implementation source `eb761f1bc510a612acde7b71b453631e1ff23a8f`; no signed beta
  resulted.
- Hardware: no DAC connected or mutated by Luna for this change; physical qualification remains
  owner-controlled and not proven.

Pre-physical handoff gate status: **READY_FOR_PIXEL_9**. The exact signed candidate provenance is recorded in the
signed-candidate addendum below; this is an owner handoff state, not a physical-fix claim.

## Superseded follow-up gate state — exact candidate source `b61f02e8c91656f14ffc639e4c6d937b2162a1b7`

This section supersedes the historical post-Pixel gate state recorded below. The owner-authorized
JA11 terminal-result repair is software-verified and independently reviewed, the minimum main
integration was authorized and merged, and the exact combined signed candidate is ready for the
owner Pixel 9 gate. The Android UAPP routing prompt is expected stock-device behavior and was not
changed or retested.

- Source: branch `codex/black-pearl-ja11-remediation-20260927`; merged `origin/main`
  `b61f02e8c91656f14ffc639e4c6d937b2162a1b7`.
- Current software gates: unit PASS; focused JA11 terminal UI 4/4 PASS; full API-36 emulator
  instrumentation 24/24 PASS; lint PASS; debug assemble PASS; minified release/R8 PASS; mapping
  verification PASS; `git diff --check` PASS.
- Independent review: PASS after resolving missing dismiss/expiry, stale global Flash feedback,
  rendered failure coverage, and trace/status identity concerns.
- `actionlint`: NOT RUN because it is unavailable locally.
- Exact signed beta: workflow [#1372](https://github.com/weekssa/OPRA-EQ-for-UAPP/actions/runs/36347148408)
  PASS for `candidate_target=black-pearl-ja11`; APK
  `EQ-Library-v0.7.0-beta-b61f02e.apk`, SHA-256
  `276587734fc863277b83e4310c040cee22c27261075e21fa75d766ded9eef27e`; package/version
  `com.weekssa.opraeqforuapp`, `0.7.0` / code `7`; signer
  `65c1c1256dae3c49e3548f334c91f0ba991969e9be9e0b223ba4e253d2114747`; immutable artifact
  `10941292707` / `sha256:188d2e4b973e7aab420d4c7d3890dba3407415c1d2c8f935a418acdacdbd8fc3`.
- Signed emulator install/cold launch: PASS; `Success`, `Status: ok`, `LaunchState: COLD`,
  `Complete`. Emulator diagnostics artifact `10941003406` /
  `sha256:e041ee672816ff85ba0c866ff0bdfa8b8a616ec540f95c93f3fd632f19197d57`.
- Superseded state: `READY_FOR_PIXEL_9`; no physical qualification or public support claim.

- [x] G0: the acceptance ledger is syntactically valid and its runnable checks are reviewable
  CHECK: node /Users/stephenweeks/.agents/skills/unlazy/scripts/gate-lint.mjs docs/UX_REVIEW_WORKFLOW/context/black-pearl-ja11-remediation-20260927/GATES.md
  EXPECT: LINT OK
  CWD: ../../../../
  EVIDENCE: automatic-evidence=v1; definition-sha256=d4f36d8f47c2c3c0b9f12c7d39c81131e1c5a7d76df53e348ac303d78db356bb; exit=0; EXPECT=matched; output-sha256=c75cc1d92dd7795f34b2f134c4428e9061dfdd73923d9b818960467947b58329; output-bytes=617; shell=/bin/sh; cwd=/Users/stephenweeks/.codex/worktrees/black-pearl-ja11-remediation-20260927/OPRA-EQ-for-UAPP; path=60a9ac3e3e95/16 entries

- [x] G1: focused Black Pearl and JA11 regression tests pass on the current source
  CHECK: ./tools/codex-android :app:testDebugUnitTest --tests '*BlackPearl*' --tests '*FiioJa11*' && printf 'FOCUSED_DAC_TESTS_PASS\n'
  EXPECT: FOCUSED_DAC_TESTS_PASS
  CWD: ../../../../
  EVIDENCE: automatic-evidence=v1; definition-sha256=0500097e178a0b3e2a7094d55b0c71e1d7ef13e013c596a62f54da3fd3050e86; exit=0; EXPECT=matched; output-sha256=6dde672322dbf2f848910020dca42cc12109b4542173be89d835ab7ded1b2c4f; output-bytes=1720; shell=/bin/sh; cwd=/Users/stephenweeks/.codex/worktrees/black-pearl-ja11-remediation-20260927/OPRA-EQ-for-UAPP; path=60a9ac3e3e95/16 entries
  no-retry, one-Save, baseline-token, and Edit-action state tests.

- [x] G2: the complete debug unit-test suite passes on the current source
  CHECK: ./tools/codex-android :app:testDebugUnitTest && printf 'UNIT_TESTS_PASS\n'
  EXPECT: UNIT_TESTS_PASS
  CWD: ../../../../
  EVIDENCE: automatic-evidence=v1; definition-sha256=fa80b7674269ce4d93f57757e7df2ec0597e39e2ec3f5c4ccc9d1af8bc20424e; exit=0; EXPECT=matched; output-sha256=0bfed3eb2f977c945d3230795134ef02862b7b1503796fbc02eb2ae9c3f61770; output-bytes=1714; shell=/bin/sh; cwd=/Users/stephenweeks/.codex/worktrees/black-pearl-ja11-remediation-20260927/OPRA-EQ-for-UAPP; path=60a9ac3e3e95/16 entries

- [x] G3: lint and debug assembly pass on the current source
  CHECK: ./tools/codex-android :app:lintDebug :app:assembleDebug && printf 'LINT_ASSEMBLE_PASS\n'
  EXPECT: LINT_ASSEMBLE_PASS
  CWD: ../../../../
  EVIDENCE: automatic-evidence=v1; definition-sha256=2897a109bd9ae26bc6fe38b497ec2a418f96b3174969868c2c4b2adcc10c4471; exit=0; EXPECT=matched; output-sha256=602e478e63a3e5a6b4736ce8e9753b508fcf6501f25d92981337e4f2302dd9b1; output-bytes=2786; shell=/bin/sh; cwd=/Users/stephenweeks/.codex/worktrees/black-pearl-ja11-remediation-20260927/OPRA-EQ-for-UAPP; path=60a9ac3e3e95/16 entries

- [x] G4: clean API 36 emulator instrumentation passes on the current source, or the prescribed unavailable boundary is recorded
  CHECK: ./tools/codex-android :app:connectedDebugAndroidTest && printf 'EMULATOR_INSTRUMENTATION_PASS\n'
  EXPECT: EMULATOR_INSTRUMENTATION_PASS
  CWD: ../../../../
  EVIDENCE: automatic-evidence=v1; definition-sha256=80cf7db05a5e254dc8b13b99a0cdee5845815564a64475d968c145386364af79; exit=0; EXPECT=matched; output-sha256=3b522b174be4d5fb0fae95fbda6d320dfcacea44d8752624d49fdfee6810dbc2; output-bytes=4357; shell=/bin/sh; cwd=/Users/stephenweeks/.codex/worktrees/black-pearl-ja11-remediation-20260927/OPRA-EQ-for-UAPP; path=60a9ac3e3e95/16 entries
  `EMULATOR_INSTRUMENTATION_PASS`. Non-`-r` debug install and cold launch also passed; large-text
  launch and `uiautomator dump` passed.

- [x] G5: exact signed beta provenance is complete for the current source, or the trusted main-only signing boundary is recorded
  EVIDENCE: PASS for merged source `b61f02e8c91656f14ffc639e4c6d937b2162a1b7`; workflow #1372,
  candidate target `black-pearl-ja11`, APK checksum, signer, immutable artifact ID/digest,
  package/version, signed emulator install/cold launch, and diagnostics digest are recorded above.
  `actionlint` is NOT RUN because it is unavailable locally; remote workflow execution passed.

- [x] G6: mandatory independent read-only review finds no unresolved scoped defect or prohibited behavior
  EVIDENCE: PASS for current source; second independent read-only review is recorded in
  `04-final-review.md`; no protocol/transport/identity/retry/canonical-EQ/hardware mutation scope
  breach found.

- [x] G7: owner Pixel device handoff is complete and hardware mutation remains owner-controlled
  EVIDENCE: READY_FOR_PIXEL_9; exact candidate provenance and plain-language checklist are in
  `05-release-handoff.md` and `07-owner-usage-guide.md`. Hardware remains owner-controlled.

## Post-Pixel evidence status

Historical physical evidence below belongs to the prior `acaf4dd` candidate and does not transfer.
For the exact candidate above, owner physical validation is pending; Black Pearl and JA11 remain
independent and neither is physically qualified by this software/emulator evidence.

## Post-diagnosis disposition — 2026-09-27

- [x] G8: one owner-authorized Black Pearl write-side diagnostic was bounded and cleaned up
  EVIDENCE: PASS for exactly one Flash attempt on the isolated diagnostic package; no automatic
  retry, Reset, Save, Restore, or second mutation was performed. The temporary package and logger
  instrumentation were removed, and the production source remained unchanged.

- [ ] G9: Black Pearl write-side defect is repaired and ready for a new signed candidate
  EVIDENCE: NOT RUN / NOT PROVEN. The diagnostic used AutoEq/Jaytiss Explorer (`-6.00 dB`) rather
  than the earlier Hifigues Explorer target (`-3.90 dB`, expected raw `-7398`). My DAC subsequently
  showed verified current hardware at `-31.00 dB`, but the temporary logger captured no raw write
  transfer because it tested the report ID byte instead of the write marker byte. This does not
  resolve the earlier mismatch. Status remains **REPAIR_REQUIRED** and no branch push is authorized
  by this evidence.

- [ ] G10: physical state restored after the diagnostic
  EVIDENCE: NOT RUN. The diagnostic state is explicitly not restored by Luna. Any later restoration
  or follow-up mutation requires a separately identified target and explicit authorization.

- [ ] G11: external transport hypothesis is resolved within the authorized scope
  EVIDENCE: historical pre-remediation blocker. The owner later authorized the narrow interrupt-OUT
  source change and the exact candidate was physically tested; the current post-Pixel result is
  recorded below as a final-gain mismatch, so the hypothesis remains **NOT PROVEN**.

## Current post-Pixel gate state — exact candidate AFUL Explorer retest

This section supersedes the earlier `READY_FOR_PIXEL_9` handoff state for the exercised Black Pearl
path. The exact candidate was source `2cba1322245221103b8790bfcef705a38022c2ed`, APK SHA-256
`50cee56aa59a8980a61bff42625fe9d839a28189068ba5adddd1a03530ea02d6`, workflow `36368698803`,
and immutable artifact `10948632352` / `sha256:3e9fb31a814a8feb30ff0c48ac69a71cddd579e71b90401ddf34e491b8fc45ab`.

- [x] G12: exact-candidate install and identity check — **PASS**. Pixel 9 `tokay`, API 37,
  package `com.weekssa.opraeqforuapp`, version `0.7.0` / code `7`; installed APK hash matched
  `50cee56aa59a8980a61bff42625fe9d839a28189068ba5adddd1a03530ea02d6`.
- [x] G13: one bounded AFUL Explorer Flash and truthful terminal result — **PASS for the
  failure-safety gate; PHYSICAL_FAIL for requested-state verification**. Fresh read-only baseline
  was taken, exactly one Flash was submitted, and the app reported expected raw `-8934` versus
  actual raw `-8960` with no automatic retry.
- [ ] G14: complete Black Pearl Flash physically verified and state restored — **FAIL / NOT
  VERIFIED**. No Reset, Save, Restore, second Flash, or recovery mutation was performed.
- [ ] G15: source repair proven by the interrupt-OUT candidate — **NOT PROVEN**. The candidate
  still failed at the final global-gain readback boundary; deterministic source diagnosis is
  required before another physical write.

## Deterministic diagnosis addendum

- Focused command: `./tools/codex-android :app:testDebugUnitTest --tests '*BlackPearl*' --tests '*Kt02h20FiveBandOptimizerTest*'`
- Result: **PASS** (`BLACK_PEARL_DIAG_TESTS_PASS`).
- Source trace: baseline raw `-7936` + `round(-3.90 * 256) = -998` -> expected raw `-8934`;
  physical final readback raw `-8960`.
- Independent oracle review: the supplied Android reference uses interrupt OUT when present,
  retains SET_REPORT fallback, and preserves signed raw/256 gain; it does not establish a
  Black-Pearl global-gain quantization rule.
- Disposition: no source correction is proven. Do not widen tolerance, alter wire bytes, replace
  the target with readback, add retry, or request another hardware write. Current status remains
  **REPAIR_REQUIRED**.

Current gate status: **REPAIR_REQUIRED** for Black Pearl. JA11 remains an independent result and
must not be inferred from this Black Pearl failure. No public hardware-support claim follows.

## Latest software repair checkpoint — implementation `7183d2c5beb76dca697e8497ba15b5c7627a320b`

The exact AFUL Explorer mismatch is now addressed at the narrow planning boundary supported by the
physical trace and independent exact-identity evidence: direct Black Pearl Flash/editor global-gain
planning uses a whole-dB native overlay, while the signed 16-bit 1/256 wire codec and shared file
export remain unchanged. No tolerance, readback substitution, report-byte change, timing change,
retry, or automatic hardware replay was added.

G16: Source repair implementation and focused regressions — PASS; `7183d2c5`, including
`-3.90 dB -> -4.00 dB` and raw `-7936 -> -8960` deterministic coverage.
G17: Full unit, lint, debug assembly — PASS.
G18: API 36 emulator — PASS; `codex-api36`, serial `emulator-5554`, 24/24, 0 skipped, 0 failed.
G19: Release/R8 and mapping verification — PASS.
G20: Diff/scope audit — PASS; five production/test files only, no transport/protocol/identity/
retry/canonical-EQ/hardware mutation changes.
G21: Independent Black Pearl review of this exact checkpoint — PENDING reviewer response.
G22: Exact signed beta for this checkpoint — NOT AVAILABLE; trusted workflow is main-only.
G23: Owner Pixel 9 physical validation and restoration — NOT RUN; blocked until exact candidate.

Current gate status: **MERGE_APPROVAL_REQUIRED**. This is software evidence only and does not claim
the Black Pearl is physically fixed or publicly supported. Luna did not mutate hardware for this
checkpoint.

G24: Final source-clarity follow-up `1393150b` — PASS. File-export wording and its two assertions
now state the shared 1/256 file path is independent from direct-Flash native whole-dB planning;
file bytes, canonical EQ, and hardware behavior are unchanged. Full unit/lint/debug/release/R8 and
API-36 24/24 gates were rerun and passed. Exact signed beta remains unavailable on the main-only
workflow; current status remains **MERGE_APPROVAL_REQUIRED**.

G25: Editor native-gain regression at `3f818d89` — PASS. A fractional but protocol-representable
Black Pearl response plans `-4.0 dB`; the existing editor Apply transaction emits raw tracked
delta `-1024`, reaches the expected fake native global gain, and passes final readback verification.

G26: Maintained protocol-plan source-of-truth repair — PASS. The Black Pearl protocol notes and
v0.5 plan now distinguish Direct Flash/editor native whole-dB global-gain mutation from independent
1/256-dB text export; no wire bytes, canonical EQ, or file output changed.

G27: Full current-tip software gates at `3f818d89` — PASS. Unit 707, lint, debug assembly,
release/R8 assembly, mapping verification, API-36 `codex-api36` instrumentation 24/24, clean debug
install/cold launch/UI dump, diff check, and gate-lint all passed. `actionlint` is NOT RUN because
it is unavailable locally.

G28: Mandatory independent re-review of exact pushed `3f818d89` — PENDING at this checkpoint.
G29: Exact signed beta for `3f818d89` — NOT AVAILABLE; trusted signing workflow remains main-only.
G30: Owner Pixel 9 validation — NOT RUN; no exact signed candidate exists and Luna did not mutate
hardware.

Current gate status: **MERGE_APPROVAL_REQUIRED**.

## Latest exact-candidate AFUL Explorer attempt — 2026-09-27 local / 2026-09-28 UTC

- Candidate source: `e1ab5fa5a65dc2d64624d871ac53d436f792ea6a`; APK SHA-256
  `7fffba26f32991ce8c936f725bdc6c3c4b6956d3a0800c539b8d45e6b52a2501`.
- Device: Pixel 9 `tokay`, API 37, wireless serial
  `adb-46141FDAQ003KZ-3AwgSo._adb-tls-connect._tcp`; target `TRN Black Pearl`.
- Preflight baseline: **PASS** for connected/verified Flat state, ten filters, active slot 1.
- One authorized `AFUL Explorer -> Flash`: **PASS** for reaching the guarded transaction boundary.
- Terminal result: **PASS** for truthful fail-closed behavior; **PHYSICAL_FAIL / PRECHECK_BLOCKED**
  for requested-state verification. The app stopped before mutation because `-4.00 dB` would
  exceed the Black Pearl validated volume range.
- Final native readback: **NOT RUN**; no write was permitted.
- Retry/Reset/Save/Restore/second Flash: **NOT RUN**, by safety boundary.

This supersedes `READY_FOR_PIXEL_9` for the exercised Black Pearl path. Black Pearl remains
**REPAIR_REQUIRED / NOT VERIFIED** as a physical capability claim. A later attempt requires a
fresh read-only baseline after the owner adjusts DAC volume and a new explicit one-operation
authorization. JA11 remains independent and is not changed by this result.

## Latest exact-candidate AFUL Explorer retest after volume adjustment

- Exact source/APK/device/DAC identity: **PASS**. Source `e1ab5fa5a65dc2d64624d871ac53d436f792ea6a`,
  APK SHA-256 `7fffba26f32991ce8c936f725bdc6c3c4b6956d3a0800c539b8d45e6b52a2501`, Pixel 9 `tokay`
  API 37, Black Pearl VID `0x3302` / PID `0x43E8`, serial `330243E8260129`.
- Fresh baseline: **PASS**. Verified Flat, ten filters, active slot 1, playback gain `-25.00 dB`.
- One authorized AFUL Explorer Flash: **PASS**. Confirmation showed the intended `-4.00 dB` change.
- Final app readback: **PASS**. Verified current hardware matched My EQs Explorer with nine active
  bands, ten filters, active slot 1, and playback gain `-29.00 dB`.
- Restoration: **NOT RUN**; not authorized. Retry/second Flash/Reset/Save: **NOT RUN**.

Current Black Pearl physical disposition for this exact candidate and identity is **PHYSICAL_PASS**
for the bounded AFUL Explorer transaction. It is not a persistence, broad-revision, or public-support
claim. JA11 remains independent. The transient terminal success surface was not captured before it
expired; the durable verified-current-hardware and Matches My EQs readback is the captured result.
