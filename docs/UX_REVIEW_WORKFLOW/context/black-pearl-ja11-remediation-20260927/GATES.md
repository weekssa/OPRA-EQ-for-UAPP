# Gates: Black Pearl and JA11 remediation

OWNS: app/src/**, docs/UX_REVIEW_WORKFLOW/context/black-pearl-ja11-remediation-20260927/**, docs/V0.6_MY_DAC_STATUS.md, docs/V0.7_RELEASE_READINESS_AUDIT.md, CHANGELOG.md

Scope: Implement and verify only the named Black Pearl final-readback truth correction and complete JA11 User 1 five-band My DAC editor/apply path, then prepare an exact owner handoff.

- [x] G0: the acceptance ledger is syntactically valid and its runnable checks are reviewable
  CHECK: node /Users/stephenweeks/.agents/skills/unlazy/scripts/gate-lint.mjs docs/UX_REVIEW_WORKFLOW/context/black-pearl-ja11-remediation-20260927/GATES.md
  EXPECT: LINT OK
  CWD: ../../../../
  EVIDENCE: automatic-evidence=v1; definition-sha256=d4f36d8f47c2c3c0b9f12c7d39c81131e1c5a7d76df53e348ac303d78db356bb; exit=0; EXPECT=matched; output-sha256=c75cc1d92dd7795f34b2f134c4428e9061dfdd73923d9b818960467947b58329; output-bytes=617; shell=/bin/sh; cwd=/Users/stephenweeks/.codex/worktrees/black-pearl-ja11-remediation-20260927/OPRA-EQ-for-UAPP; path=60a9ac3e3e95/16 entries

- [x] G1: focused Black Pearl and JA11 regression tests pass on the exact candidate source
  CHECK: ./tools/codex-android :app:testDebugUnitTest --tests '*BlackPearl*' --tests '*FiioJa11*' && printf 'FOCUSED_DAC_TESTS_PASS\n'
  EXPECT: FOCUSED_DAC_TESTS_PASS
  CWD: ../../../../
  EVIDENCE: automatic-evidence=v1; definition-sha256=0500097e178a0b3e2a7094d55b0c71e1d7ef13e013c596a62f54da3fd3050e86; exit=0; EXPECT=matched; output-sha256=6dde672322dbf2f848910020dca42cc12109b4542173be89d835ab7ded1b2c4f; output-bytes=1720; shell=/bin/sh; cwd=/Users/stephenweeks/.codex/worktrees/black-pearl-ja11-remediation-20260927/OPRA-EQ-for-UAPP; path=60a9ac3e3e95/16 entries
  no-retry, one-Save, baseline-token, and Edit-action state tests.

- [x] G2: the complete debug unit-test suite passes on the exact candidate source
  CHECK: ./tools/codex-android :app:testDebugUnitTest && printf 'UNIT_TESTS_PASS\n'
  EXPECT: UNIT_TESTS_PASS
  CWD: ../../../../
  EVIDENCE: automatic-evidence=v1; definition-sha256=fa80b7674269ce4d93f57757e7df2ec0597e39e2ec3f5c4ccc9d1af8bc20424e; exit=0; EXPECT=matched; output-sha256=0bfed3eb2f977c945d3230795134ef02862b7b1503796fbc02eb2ae9c3f61770; output-bytes=1714; shell=/bin/sh; cwd=/Users/stephenweeks/.codex/worktrees/black-pearl-ja11-remediation-20260927/OPRA-EQ-for-UAPP; path=60a9ac3e3e95/16 entries

- [x] G3: lint and debug assembly pass on the exact candidate source
  CHECK: ./tools/codex-android :app:lintDebug :app:assembleDebug && printf 'LINT_ASSEMBLE_PASS\n'
  EXPECT: LINT_ASSEMBLE_PASS
  CWD: ../../../../
  EVIDENCE: automatic-evidence=v1; definition-sha256=2897a109bd9ae26bc6fe38b497ec2a418f96b3174969868c2c4b2adcc10c4471; exit=0; EXPECT=matched; output-sha256=602e478e63a3e5a6b4736ce8e9753b508fcf6501f25d92981337e4f2302dd9b1; output-bytes=2786; shell=/bin/sh; cwd=/Users/stephenweeks/.codex/worktrees/black-pearl-ja11-remediation-20260927/OPRA-EQ-for-UAPP; path=60a9ac3e3e95/16 entries

- [x] G4: clean API 36 emulator instrumentation passes, or the prescribed unavailable boundary is recorded
  CHECK: ./tools/codex-android :app:connectedDebugAndroidTest && printf 'EMULATOR_INSTRUMENTATION_PASS\n'
  EXPECT: EMULATOR_INSTRUMENTATION_PASS
  CWD: ../../../../
  EVIDENCE: automatic-evidence=v1; definition-sha256=80cf7db05a5e254dc8b13b99a0cdee5845815564a64475d968c145386364af79; exit=0; EXPECT=matched; output-sha256=3b522b174be4d5fb0fae95fbda6d320dfcacea44d8752624d49fdfee6810dbc2; output-bytes=4357; shell=/bin/sh; cwd=/Users/stephenweeks/.codex/worktrees/black-pearl-ja11-remediation-20260927/OPRA-EQ-for-UAPP; path=60a9ac3e3e95/16 entries
  `EMULATOR_INSTRUMENTATION_PASS`. Non-`-r` debug install and cold launch also passed; large-text
  launch and `uiautomator dump` passed.

- [x] G5: exact signed beta provenance is complete for the final source, or the trusted main-only signing boundary is recorded
  EVIDENCE: PASS; owner-approved merge `acaf4dd32ddd9379ec2860e45e34fb8039219583` and trusted
  signed-beta run `#1371` produced `EQ-Library-v0.7.0-beta-acaf4dd.apk`, APK SHA-256
  `af83a5e0148263057b1c43e3b775157ab6aedd9d2c1e3ab0558cef8fa3cea665`, signer SHA-256
  `65c1c1256dae3c49e3548f334c91f0ba991969e9be9e0b223ba4e253d2114747`, artifact ID
  `10937890771`, digest `sha256:88b1555e9a14642874110d05d5c5ae3554c25cc64b9385898a78287ccea9f52d`;
  signed emulator install/cold launch passed; actionlint v1.7.12 passed on the exact workflow.

- [x] G6: mandatory independent read-only review finds no unresolved scoped defect or prohibited behavior
  EVIDENCE: PASS; review recorded in `04-final-review.md`; no protocol/transport/identity/retry/
  canonical-EQ/hardware mutation scope breach found.

- [x] G7: owner Pixel 9 handoff is complete and hardware mutation remains owner-controlled
  EVIDENCE: PASS as an actionable exact-candidate handoff in `05-release-handoff.md` and
  `07-owner-usage-guide.md`; status is `READY_FOR_PIXEL_9`. Luna did not mutate hardware.
