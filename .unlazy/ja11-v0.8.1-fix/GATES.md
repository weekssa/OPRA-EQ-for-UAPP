# Gates: FiiO JA11 v0.8.1 User 1 write-order correction

Scope: qualify the JA11 User 1 write-order and late Save/reconnect corrections on the exact current source, then complete the approved physical acceptance only after all off-phone gates and the Model D read-only candidate/session/cardinality/full-baseline gate. Preserve the public v0.8.0 release. Earlier candidate results remain historical and do not transfer to this candidate.

- [x] G0: This gate ledger passes Unlazy structural and oracle lint.
  CHECK: node /Users/stephenweeks/.agents/skills/unlazy/scripts/gate-lint.mjs .unlazy/ja11-v0.8.1-fix/GATES.md
  EXPECT: LINT OK
  CWD: ../..
  EVIDENCE: automatic-evidence=v1; definition-sha256=a612eb8f17f46943919227247760a9bcaef893142c2b8ed5436df1f915ebd4e6; exit=0; EXPECT=matched; output-sha256=f27cfadc49451ca83380a6087fedcaa5c21ada937f1ff02424f1b506e5c6087b; output-bytes=1430; shell=/bin/sh; cwd=/Users/stephenweeks/.codex/worktrees/ja11-v0-8-1-fix/OPRA-EQ-for-UAPP; path=802c868f40ee/16 entries

- [x] G1: The current repository, PR, and main baseline are reconciled before candidate work.
  EVIDENCE: Mission worktree `/Users/stephenweeks/.codex/worktrees/ja11-v0-8-1-fix/OPRA-EQ-for-UAPP`, branch `codex/ja11-v0.8.1-fix`. Refreshed `origin/main` is `9b9a1f610025b3d6233d4ff6f0c2309a668577cc`; current committed production source is `3d7bc1d91e6c39327477d1341bde80e8a37bfbd4`. PR #80 is open/draft/unmerged; remote head is `186c43b22490281ba2fbfceae52e7fa5d13b9c3b` and does not contain the current source. Android CI, CodeQL, catalog currentness, and priority-community workflows pass on that stale head only; exact candidate-head checks are pending. The Model D Constitution, State, Gate Matrix, helper, fixtures, and synchronized mission documents are local changes. Preserve untracked `.unlazy/ja11-v0.8.1-model-d/`, `.unlazy/ja11-v0.8.1-save-reconnect/`, and user-owned `docs/.DS_Store`.

- [x] G2: The JA11 Flasher regression class passes on candidate app source `3d7bc1d91e6c39327477d1341bde80e8a37bfbd4`.
  CHECK: ./tools/codex-android :app:testDebugUnitTest --tests 'com.weekssa.opraeqforuapp.domain.kt02h20.FiioJa11FlasherTest' --max-workers=2
  EXPECT: BUILD SUCCESSFUL
  CWD: ../..
  EVIDENCE: automatic-evidence=v1; definition-sha256=2fecce88af68a3a7182881c61d498cedfd90686639802a8e597d53209ebdff6f; exit=0; EXPECT=matched; output-sha256=e06a60c908071625fd045e29630afd48f49d8157daae2f5e8ed5d3dcf83616c8; output-bytes=1938; shell=/bin/sh; cwd=/Users/stephenweeks/.codex/worktrees/ja11-v0-8-1-fix/OPRA-EQ-for-UAPP; path=802c868f40ee/16 entries

- [x] G3: Full local JVM, lint, debug/release/diagnostic, Android-test compile/assembly, and R8 gates pass on source `3d7bc1d91e6c39327477d1341bde80e8a37bfbd4`.
  CHECK: ./tools/codex-android :app:testDebugUnitTest :app:lintDebug :app:assembleDebug :app:assembleRelease :app:assembleJa11Diagnostic :app:compileDebugAndroidTestKotlin :app:assembleDebugAndroidTest -PCANDIDATE_SOURCE_SHA=3d7bc1d91e6c39327477d1341bde80e8a37bfbd4 --max-workers=2 && bash tools/verify-r8-mapping.sh
  EXPECT: BUILD SUCCESSFUL
  CWD: ../..
  EVIDENCE: automatic-evidence=v1; definition-sha256=05ba15a15e2127494a8698c2389e40d22ff49ac819c55f5671aa654ef0c7afad; exit=0; EXPECT=matched; output-sha256=6995cb383f588cfc68fb6d078306f4e3ceec3d31deaca9d354f645421d84178a; output-bytes=10119; shell=/bin/sh; cwd=/Users/stephenweeks/.codex/worktrees/ja11-v0-8-1-fix/OPRA-EQ-for-UAPP; path=802c868f40ee/16 entries

- [x] G3a: The acceptance helpers pass Model D fixtures for optional initial and reconnect serials, permission-denial rejection, exact-one-candidate/current-session checks, mismatch rejection when both serials exist, previously opened-generation and physical-detach ordering, and redacted output.
  CHECK: bash -n docs/implementation/ja11-v0.8.1-acceptance/phone-session.sh && bash -n docs/implementation/ja11-v0.8.1-acceptance/test-phone-session.sh && python3 -m py_compile docs/implementation/ja11-v0.8.1-acceptance/make-baseline-profile.py && bash docs/implementation/ja11-v0.8.1-acceptance/test-phone-session.sh
  EXPECT: PHONE SESSION HELPER FIXTURES PASS
  CWD: ../..
  EVIDENCE: automatic-evidence=v1; definition-sha256=cb9f560d7352b3e42127b9dbfe3ff1f9a9c4b26ddddcd20dc39a82947796a01a; exit=0; EXPECT=matched; output-sha256=944d79f424382f0f7979d2e85d610c46cbc3030adab5726b8635b7a9faed34be; output-bytes=311; shell=/bin/sh; cwd=/Users/stephenweeks/.codex/worktrees/ja11-v0-8-1-fix/OPRA-EQ-for-UAPP; path=802c868f40ee/16 entries

- [x] G4: Independent read-only review finds no actionable defect in the production reconnect behavior and corrected Model D helper/procedure.
  EVIDENCE: Production source `3d7bc1d91e6c39327477d1341bde80e8a37bfbd4` review found no actionable issue. Final corrected-helper/procedure review found no remaining actionable issue after the first review's missing prior-open-generation and permission-denial fixture findings were fixed. It confirmed optional serial behavior, mismatch rejection when both serials exist, candidate cardinality, generation binding, detach ordering, redacted output, and fresh phone-window confirmation. G3a output SHA-256 `944d79f424382f0f7979d2e85d610c46cbc3030adab5726b8635b7a9faed34be`. No ADB or hardware access was used.

- [x] G5: The exact source-bound diagnostic candidate is provenance-verified, cold-launches on an isolated API 35 emulator, and passes all applicable instrumentation tests.
  EVIDENCE: Exact source `3d7bc1d91e6c39327477d1341bde80e8a37bfbd4`; diagnostic APK `ja11diag-3d7bc1d91e6.apk`, SHA-256 `3b74672a587daeaaf8f562634c5dcecea073f7ad6e01df736f874ee448fc6261`, package `com.weekssa.opraeqforuapp.ja11diag`, version `0.8.0-ja11diag`/11, v2 signature verified, signer SHA-256 `73aa7581c8dc7dcc8ccea7586771119a98f9a74d7d8cf23716e09c557c9f6b41`. Isolated API 35 AVD `ja11-v081-api35-clean-20261008` cold-launched and runtime `APP_BUILD_INFO` reported the exact source; source-matched instrumentation passed `OK (64 tests)`. The emulator was shut down. Detailed provenance/output is in `/private/tmp/ja11-v0.8.1-acceptance-3d7bc1d91e6-corrected/CANDIDATE.md`. The rejected predecessor hash and unresolvable source SHA remain recorded there; no Pixel command was issued.

- [ ] G6: All required PR checks pass on the exact pushed PR #80 head containing the reconciled main merge and candidate documentation.
  EVIDENCE: pending. Existing success on remote PR head `186c43b22490281ba2fbfceae52e7fa5d13b9c3b` does not qualify source `3d7bc1d91e6c39327477d1341bde80e8a37bfbd4`; refresh all required checks after the current coherent branch head is pushed.

- [ ] G7: After a separate confirmed phone window, the exact candidate passes a read-only Model D session/cardinality/full-baseline gate, completes the prepared acceptance checks, restores every changed baseline value, and releases the Pixel.
  EVIDENCE: pending for source `3d7bc1d91e6c39327477d1341bde80e8a37bfbd4`. Do not access the Pixel until applicable off-phone gates pass and the owner confirms a new `PHONE WINDOW READY — PIXEL + JA11 NEEDED` request. The initial gate requires exact candidate provenance, one supported JA11, current permissioned claimed session/generation, and complete baseline. Serial is optional; compare when both sessions expose it, reject mismatch, and never claim same-unit identity without a matching usable serial. If Mic is Off, restoration to On is the first mutation with expected-reset handling and authoritative readback; if already On, skip the write. Prior source `92c11fb0` Test C failure and full original-state restoration remain historical.

- [ ] G8: The release version, official signing/provenance, signed upgrade, clean install, and merge are completed under explicit owner authorization.
  EVIDENCE: pending; public v0.8.0 is immutable and unchanged. Merge/release approval is not inferred from test authorization.

- [ ] G9: Stable publication and independent public tag, release, APK, checksum, signer, and latest-state verification are completed under explicit owner authorization.
  EVIDENCE: pending; do not publish or make a new public JA11 support claim without the separate owner approval required by `AGENTS.md`.
