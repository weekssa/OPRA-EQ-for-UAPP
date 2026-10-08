# Gates: FiiO JA11 v0.8.1 fix and release

Scope: Correct the proven JA11 expected-restart false-negative and complete the exact v0.8.1 software, physical, and release gates.

- [x] G0: This gate ledger passes Unlazy's structural and oracle lint.
  CHECK: node /Users/stephenweeks/.agents/skills/unlazy/scripts/gate-lint.mjs .unlazy/ja11-v0.8.1-fix/GATES.md
  EXPECT: LINT OK
  EVIDENCE: LINT OK (8 warnings, all manual-gate/mostly-manual annotations); 2026-10-08

- [x] G1: The initial repository and evidence baseline is reconciled to live GitHub and current source.
  EVIDENCE: docs/implementation/ja11-v0.8.1-fix-ledger.md, Phase 1 hash/source reconciliation, public GitHub REST refresh; 2026-10-08

- [x] G2: The test-first JA11 regressions pass for expected restart, replacement-session readback, terminal pending cleanup, no replay, and Flash generation safety.
  CHECK: ./tools/codex-android :app:testDebugUnitTest --max-workers=2
  EXPECT: BUILD SUCCESSFUL
  EVIDENCE: Current-tree focused repository/Flasher/snapshot-reader tests and full `:app:testDebugUnitTest` BUILD SUCCESSFUL in 14s/13s; all 64 current-tree `:app:connectedDebugAndroidTest` cases pass on clean API 35 AVD `ja11-v081-api35-clean-20261008` (`emulator-5560`), zero skipped/failed, BUILD SUCCESSFUL in 1m46; 2026-10-08. First run on preserved older AVD ran zero tests due to pre-existing signature conflict; no tests failed.

- [x] G3: Applicable full off-phone Android build gates pass on the coherent pre-phone candidate.
  CHECK: ./tools/codex-android :app:lintDebug :app:assembleDebug :app:assembleRelease :app:compileDebugAndroidTestKotlin --max-workers=2
  EXPECT: BUILD SUCCESSFUL
  EVIDENCE: Current-tree `:app:lintDebug :app:assembleDebug :app:assembleRelease :app:compileDebugAndroidTestKotlin :app:assembleJa11Diagnostic --max-workers=1 --no-daemon` BUILD SUCCESSFUL in 2m; `bash tools/verify-r8-mapping.sh` passed; 2026-10-08.

- [ ] G4: Independent source review and all exact-head required CI checks pass before physical testing.
  EVIDENCE: Independent source review closed both actionable findings: replacement verification is generation/identity pinned; Flash/Reset/editor reads carry the exact token into the HID mutex. Narrow residual: DEVICE snapshot reads are fenced before/after but are not transport-token-pinned, so a read-only query may be sent to a replacement in the check/capture gap; its result is discarded. Draft PR #80 head `63df0a720849c3cc281a0b198da0142484bad992`: catalog, priority-community coverage, and CodeQL succeeded. Android CI attempt 1 failed during SDK emulator image installation before tests; attempts 2 and 3 booted but failed the same eight cross-feature UI tests with window-focus loss. Build and min-API smoke passed on both reruns. Attempt-3 emulator artifact ID `11576442524`, digest `sha256:30c4c96bd2d1ef051292ddb115493e951cdf79c37a3893c3ad58624a50fa4958`; normal emulator boot confirmed, trigger unproven. A follow-up patch adds failure-time power/window/activity/input/focus logs and diagnostic-build source SHA logging; independent patch review found no actionable issues. Exact-head rerun is pending.

- [ ] G5: The exact acceptance APK is frozen and the complete Mac-side ADB preflight and hardware procedure are ready before the owner is contacted.
  EVIDENCE: pending

- [ ] G6: The prepared Pixel/JA11 acceptance window passes the mission's operation and restoration criteria.
  EVIDENCE: pending

- [ ] G7: The release version, exact merge, official signer/provenance, signed upgrade, and clean install pass on exact source.
  EVIDENCE: pending

- [ ] G8: Stable publication and independent public tag, release, APK, checksum, signer, and latest-state readback complete while preserving the prior release.
  EVIDENCE: pending
