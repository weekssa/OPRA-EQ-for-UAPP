# Gates: FiiO JA11 v0.8.1 fix and release

Scope: Correct the proven JA11 expected-restart false-negative and complete the exact v0.8.1 software, physical, and release gates.

- [x] G0: This gate ledger passes Unlazy's structural and oracle lint.
  CHECK: node /Users/stephenweeks/.agents/skills/unlazy/scripts/gate-lint.mjs .unlazy/ja11-v0.8.1-fix/GATES.md
  EXPECT: LINT OK
  CWD: ../..
  EVIDENCE: automatic-evidence=v1; definition-sha256=a612eb8f17f46943919227247760a9bcaef893142c2b8ed5436df1f915ebd4e6; exit=0; EXPECT=matched; output-sha256=439dda63af918a3d8ffc66f037d90ab89df6957a57952278bce4d0bb0747de92; output-bytes=1071; shell=/bin/sh; cwd=/Users/stephenweeks/.codex/worktrees/ja11-v0-8-1-fix/OPRA-EQ-for-UAPP; path=802c868f40ee/16 entries

- [x] G1: The initial repository and evidence baseline is reconciled to live GitHub and current source.
  EVIDENCE: docs/implementation/ja11-v0.8.1-fix-ledger.md, Phase 1 hash/source reconciliation, public GitHub REST refresh; 2026-10-08

- [x] G2: Shared late permission callbacks and JA11's bounded 25-second restart-permission lifecycle pass denial, timeout, detach, stale-device, missing-identity, cancellation, and no-replay regressions.
  CHECK: ./tools/codex-android :app:testDebugUnitTest --tests 'com.weekssa.opraeqforuapp.ui.FiioJa11RestartVerificationWatchdogTest' --tests 'com.weekssa.opraeqforuapp.data.dac.FiioJa11ControlRepositoryTest' --tests 'com.weekssa.opraeqforuapp.ui.FiioJa11DeviceUiStateTest' --tests 'com.weekssa.opraeqforuapp.ui.screens.MyDacConnectionPresentationTest' --tests 'com.weekssa.opraeqforuapp.data.kt02h20.Kt02h20ConnectAttemptTrackerTest' --tests 'com.weekssa.opraeqforuapp.data.kt02h20.AndroidKt02h20HidSessionTest' --max-workers=2
  EXPECT: BUILD SUCCESSFUL
  CWD: ../..
  EVIDENCE: automatic-evidence=v1; definition-sha256=59061ca6f84a91b9e2f475ee14e7c087019086d2f2604e488d684b0bb4a0565b; exit=0; EXPECT=matched; output-sha256=5679ad644fc3de56a059b4c413f2b4adf76518596fb2bdb7d2f7c4ce1a5f6bb2; output-bytes=1938; shell=/bin/sh; cwd=/Users/stephenweeks/.codex/worktrees/ja11-v0-8-1-fix/OPRA-EQ-for-UAPP; path=802c868f40ee/16 entries

- [x] G3: Applicable full off-phone Android build gates pass on the corrected source.
  CHECK: ./tools/codex-android :app:testDebugUnitTest :app:lintDebug :app:assembleDebug :app:assembleRelease :app:assembleJa11Diagnostic :app:compileDebugAndroidTestKotlin :app:assembleDebugAndroidTest --max-workers=2 && bash tools/verify-r8-mapping.sh
  EXPECT: BUILD SUCCESSFUL
  CWD: ../..
  EVIDENCE: automatic-evidence=v1; definition-sha256=a2f12960be3f681fa49b1a539080ad8a1979d91479d3f5811c441f4e9748ed66; exit=0; EXPECT=matched; output-sha256=91d1a4579c201ce1f4a57f06f37f883d8e7090d71fb6198643f84687cade4d39; output-bytes=10338; shell=/bin/sh; cwd=/Users/stephenweeks/.codex/worktrees/ja11-v0-8-1-fix/OPRA-EQ-for-UAPP; path=802c868f40ee/16 entries

- [ ] G4: Independent review and all exact-head required CI checks pass for the corrected source and synchronized handoff.
  EVIDENCE: The final lifecycle follow-up confirmed the per-session UUID permission action prevents an old denial PendingIntent from colliding with a recreated session that reuses request ID 1; its Robolectric regression covers old denial followed by current grant. A separate read-only review of the rollback action and physical evidence plan found no actionable issue. Exact-head CI for the synchronized handoff is pending push.

- [x] G5: A source-bound corrected diagnostic APK, update-safe phone helper, emulator install, and fresh host-only ADB preflight are verified.
  EVIDENCE: Source `da1f8e25918065667648d676cb669fed4c803f17`; APK `opra-eq-ja11diag-0.8.0-source-da1f8e25.apk`, SHA-256 `767b42591adc92f0e1662480112bf9efe87ce15f51060d20a7aa39118b8d8c24`, package `com.weekssa.opraeqforuapp.ja11diag`/`0.8.0-ja11diag`/11, debug signer SHA-256 `73aa7581c8dc7dcc8ccea7586771119a98f9a74d7d8cf23716e09c557c9f6b41`. The exact-source build and signer/package metadata passed; 818 JVM tests (0 failed/error/skipped), R8, and 64/64 API 35 emulator tests passed. The APK installed/launched only on isolated `emulator-5560`; runtime `APP_BUILD_INFO` matched the source SHA. Mock ADB fixtures passed J020 exact in-place update, absent install, mismatch and wrong-device rejection; identity-current acceptance, false/stale/wrong-source rejection; exact baseline conversion, wrong-source rejection, and collision preservation. The rollback fixture installed the exact corrected APK over J020, restored and verified the exact J020 APK, and rejected rollback when the installed APK was not the corrected hash. Host-only adb 37.0.1-15733141, mDNS backend, default route, proxy/VPN state, and firewall were checked without device/service enumeration. Live Pixel endpoint remains pending; no Pixel command was sent.

- [ ] G6: The new exact-candidate Pixel/JA11 acceptance session passes the operation criteria and restores the exact original device state.
  EVIDENCE: J020 on source `a78808443c71d688e0f338e96495847569fe12f7` / APK SHA-256 `7beb5bcebbc0dc40a68b33de911cc8722d76d3f0ff2e98685b1fa25e17caed61` completed one Mic On-to-Off write and detached. USB permission arrived about 18.5 seconds after request. A current replacement session read Mic Off and unchanged EQ state, but `identityAvailable=false`; no `RESTART_VERIFY_*` event occurred. The last verified mic state is Off and restoration to the original On baseline remains outstanding. No further write, UAC, Flash, Reset, or Tests B/C/D occurred. This is incomplete/negative physical evidence, not a pass. The next exact candidate must keep identity fail-closed, restore On first when the fresh baseline is Off, and stop if replacement identity is unavailable.

- [ ] G7: The release version, exact merge, official signer/provenance, signed upgrade, and clean install pass on exact source.
  EVIDENCE: pending

- [ ] G8: Stable publication and independent public tag, release, APK, checksum, signer, and latest-state readback complete while preserving the prior release.
  EVIDENCE: pending
