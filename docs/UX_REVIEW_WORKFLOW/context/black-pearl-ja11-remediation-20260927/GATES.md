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

Current gate status: **READY_FOR_PIXEL_9**. The exact signed candidate provenance is recorded in the
signed-candidate addendum below; this is an owner handoff state, not a physical-fix claim.

## Current follow-up gate state — exact candidate source `b61f02e8c91656f14ffc639e4c6d937b2162a1b7`

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
- Current state: `READY_FOR_PIXEL_9`; no physical qualification or public support claim.

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
  EVIDENCE: NOT RUN. Reference commit `45bbf3c65c899181395eb7936615ced1fbd5d4be` uses interrupt
  OUT endpoint `bulkTransfer` when available; the captured Black Pearl exposes OUT `0x05`, while
  OPRA currently uses only control-transfer writes. Testing that path requires changing endpoint
  behavior, which is explicitly outside the current remediation guardrails. Status is
  **BLOCKED — boundary requires owner decision**; no hardware test or push occurred.
