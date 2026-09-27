# Gates: Black Pearl and JA11 remediation

OWNS: app/src/**, docs/UX_REVIEW_WORKFLOW/context/black-pearl-ja11-remediation-20260927/**, docs/V0.6_MY_DAC_STATUS.md, docs/V0.7_RELEASE_READINESS_AUDIT.md, CHANGELOG.md

Scope: Implement and verify only the named Black Pearl final-readback truth correction and complete JA11 User 1 five-band My DAC editor/apply path, then prepare an exact owner handoff.

- [x] G0: the acceptance ledger is syntactically valid and its runnable checks are reviewable
  CHECK: node /Users/stephenweeks/.agents/skills/unlazy/scripts/gate-lint.mjs docs/UX_REVIEW_WORKFLOW/context/black-pearl-ja11-remediation-20260927/GATES.md
  EXPECT: LINT OK
  EVIDENCE: PASS; `LINT OK`.

- [x] G1: focused Black Pearl and JA11 regression tests pass on the exact candidate source
  CHECK: ./tools/codex-android :app:testDebugUnitTest --tests '*BlackPearl*' --tests '*FiioJa11*' && printf 'FOCUSED_DAC_TESTS_PASS\n'
  EXPECT: FOCUSED_DAC_TESTS_PASS
  EVIDENCE: PASS; focused Black Pearl/JA11 tests completed successfully, including final-readback,
  no-retry, one-Save, baseline-token, and Edit-action state tests.

- [x] G2: the complete debug unit-test suite passes on the exact candidate source
  CHECK: ./tools/codex-android :app:testDebugUnitTest && printf 'UNIT_TESTS_PASS\n'
  EXPECT: UNIT_TESTS_PASS
  EVIDENCE: PASS; `UNIT_TESTS_PASS`.

- [x] G3: lint and debug assembly pass on the exact candidate source
  CHECK: ./tools/codex-android :app:lintDebug :app:assembleDebug && printf 'LINT_ASSEMBLE_PASS\n'
  EXPECT: LINT_ASSEMBLE_PASS
  EVIDENCE: PASS; `LINT_ASSEMBLE_PASS`; lint HTML report generated.

- [x] G4: clean API 36 emulator instrumentation passes, or the prescribed unavailable boundary is recorded
  CHECK: ./tools/codex-android :app:connectedDebugAndroidTest && printf 'EMULATOR_INSTRUMENTATION_PASS\n'
  EXPECT: EMULATOR_INSTRUMENTATION_PASS
  EVIDENCE: PASS; 20 tests on AVD `codex-api36`, serial `emulator-5554`, API 36, with
  `EMULATOR_INSTRUMENTATION_PASS`. Non-`-r` debug install and cold launch also passed; large-text
  launch and `uiautomator dump` passed.

- [x] G5: exact signed beta provenance is complete for the final source, or the trusted main-only signing boundary is recorded
  EVIDENCE: BOUNDARY RECORDED; `.github/workflows/signed-beta.yml` requires `refs/heads/main` and
  exposes only `ja11`/`ew300` candidate targets. No signed branch candidate exists; status is
  `MERGE_APPROVAL_REQUIRED`.

- [x] G6: mandatory independent read-only review finds no unresolved scoped defect or prohibited behavior
  EVIDENCE: PASS; review recorded in `04-final-review.md`; no protocol/transport/identity/retry/
  canonical-EQ/hardware mutation scope breach found.

- [x] G7: owner Pixel 9 handoff is complete and hardware mutation remains owner-controlled
  EVIDENCE: PASS as a prepared, non-actionable handoff in `05-release-handoff.md` and
  `07-owner-usage-guide.md`; Pixel 9 is blocked until exact signed candidate provenance exists.
