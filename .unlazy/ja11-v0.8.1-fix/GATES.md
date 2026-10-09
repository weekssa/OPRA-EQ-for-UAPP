# Gates: FiiO JA11 v0.8.1 User 1 write-order correction

Scope: qualify the corrected JA11 Flash/Reset/editor ordering on the exact current source, complete the approved physical acceptance only after all off-phone gates, and preserve the public v0.8.0 release. Earlier Model D/J024 gate evidence remains historical in the validation ledger and current-state report; it does not transfer to this candidate.

- [x] G0: This gate ledger passes Unlazy structural and oracle lint.
  CHECK: node /Users/stephenweeks/.agents/skills/unlazy/scripts/gate-lint.mjs .unlazy/ja11-v0.8.1-fix/GATES.md
  EXPECT: LINT OK
  CWD: ../..
  EVIDENCE: automatic-evidence=v1; definition-sha256=a612eb8f17f46943919227247760a9bcaef893142c2b8ed5436df1f915ebd4e6; exit=0; EXPECT=matched; output-sha256=8be1221629521a902570008e78d1cbb546ddf237146f86ca2d80faa9e90a52cc; output-bytes=1687; shell=/bin/sh; cwd=/Users/stephenweeks/.codex/worktrees/ja11-v0-8-1-fix/OPRA-EQ-for-UAPP; path=802c868f40ee/16 entries

- [x] G1: The current repository, PR, and main baseline are reconciled before candidate work.
  EVIDENCE: Worktree `/Users/stephenweeks/.codex/worktrees/ja11-v0-8-1-fix/OPRA-EQ-for-UAPP`, branch `codex/ja11-v0.8.1-fix`. Remote `origin/main` is `7cbef435f2417381ed967d262574d7f5d1ba188b`; local merge commit `89e028ba3d2bafc0efb06697a6373a101101045c` integrates that main. PR #80 is open/draft/unmerged; its remote head remains `261276eef6ec0b2cbc23048ae5a1d3c5e9b5edb9` and recorded base remains `62b31a5713c01ab0d78ecb57697f30623d45c74e`. Its eight green check rows apply only to that old head. Current app-source correction commit is `92c11fb0e41ae11b118b2e7bb105234d6606dbdb`; no checks on that source or the local merge are claimed. Existing untracked `.unlazy/ja11-v0.8.1-model-d/` and user-owned `docs/.DS_Store` are preserved.

- [x] G2: The JA11 Flasher regression class passes on corrected app source `92c11fb0e41ae11b118b2e7bb105234d6606dbdb`.
  CHECK: ./tools/codex-android :app:testDebugUnitTest --tests 'com.weekssa.opraeqforuapp.domain.kt02h20.FiioJa11FlasherTest' --max-workers=2
  EXPECT: BUILD SUCCESSFUL
  CWD: ../..
  EVIDENCE: automatic-evidence=v1; definition-sha256=2fecce88af68a3a7182881c61d498cedfd90686639802a8e597d53209ebdff6f; exit=0; EXPECT=matched; output-sha256=b04bcac23ff7288cf1142dc7d9cf824ec4dd376348fd2dd4461eedee7f8aa76c; output-bytes=1940; shell=/bin/sh; cwd=/Users/stephenweeks/.codex/worktrees/ja11-v0-8-1-fix/OPRA-EQ-for-UAPP; path=802c868f40ee/16 entries

- [x] G3: Full local JVM, lint, debug/release/diagnostic, Android-test compile/assembly, and R8 gates pass on the corrected source.
  CHECK: ./tools/codex-android :app:testDebugUnitTest :app:lintDebug :app:assembleDebug :app:assembleRelease :app:assembleJa11Diagnostic :app:compileDebugAndroidTestKotlin :app:assembleDebugAndroidTest --max-workers=2 && bash tools/verify-r8-mapping.sh
  EXPECT: BUILD SUCCESSFUL
  CWD: ../..
  EVIDENCE: automatic-evidence=v1; definition-sha256=a2f12960be3f681fa49b1a539080ad8a1979d91479d3f5811c441f4e9748ed66; exit=0; EXPECT=matched; output-sha256=25722132f1b61160eaa84ae6858967fb0cb8bc66e5384c4c024377e68125759e; output-bytes=10132; shell=/bin/sh; cwd=/Users/stephenweeks/.codex/worktrees/ja11-v0-8-1-fix/OPRA-EQ-for-UAPP; path=802c868f40ee/16 entries

- [x] G3a: The acceptance helper parses either valid identity/snapshot event order and retains its fail-closed source, process, session, permission, cardinality, liveness, detach, and ambiguity checks.
  CHECK: bash -n docs/implementation/ja11-v0.8.1-acceptance/phone-session.sh && bash -n docs/implementation/ja11-v0.8.1-acceptance/test-phone-session.sh && python3 -m py_compile docs/implementation/ja11-v0.8.1-acceptance/make-baseline-profile.py && bash docs/implementation/ja11-v0.8.1-acceptance/test-phone-session.sh
  EXPECT: PHONE SESSION HELPER FIXTURES PASS
  CWD: ../..
  EVIDENCE: automatic-evidence=v1; definition-sha256=cb9f560d7352b3e42127b9dbfe3ff1f9a9c4b26ddddcd20dc39a82947796a01a; exit=0; EXPECT=matched; output-sha256=fdf3432800d05e89ec61148213e107ce11e9ee3aecda3efddaffcab8f767aa40; output-bytes=311; shell=/bin/sh; cwd=/Users/stephenweeks/.codex/worktrees/ja11-v0-8-1-fix/OPRA-EQ-for-UAPP; path=802c868f40ee/16 entries

- [x] G4: Independent read-only review finds no actionable ordering, trace-state, or acceptance-helper defect.
  EVIDENCE: On 2026-10-09, an independent reviewer checked the corrected Flasher and tests at source commit `92c11fb0e41ae11b118b2e7bb105234d6606dbdb`: same-session User 1 confirmation precedes all `0x15`/`0x17` writes, uncertain post-selection readback returns `TransferFailed`/`stateKnown=false`, and no Apply/Save follows. The editor's fresh User 1 guard and race regression were confirmed. A separate read-only re-review confirmed the helper accepts both snapshot/identity event orders while retaining process/source/generation/cardinality/current-session, permission, liveness, detach, close, and ambiguity checks. The banked fake proves software ordering under its model, not JA11 bank semantics. Neither reviewer edited files, ran tests/builds, used ADB, or accessed hardware.

- [x] G5: The exact source-bound diagnostic candidate is provenance-verified, cold-launches on an isolated API 35 emulator, and passes all applicable instrumentation tests.
  EVIDENCE: Source `92c11fb0e41ae11b118b2e7bb105234d6606dbdb`; APK `opra-eq-ja11diag-0.8.0-source-92c11fb0.apk`, SHA-256 `ce3f417f20c275fd4d535cf5e70f658d8430af8fdd3f87ea950705fbfd574637`, 23,146,700 bytes; package `com.weekssa.opraeqforuapp.ja11diag`, version `0.8.0-ja11diag` / 11; APK v2 signature verified, one Android Debug signer certificate SHA-256 `73aa7581c8dc7dcc8ccea7586771119a98f9a74d7d8cf23716e09c557c9f6b41`. Installed and cold-launched on isolated API 35 AVD `ja11-v081-api35-clean-20261008` at explicit serial `emulator-5580`; runtime `JA11_DIAG` `APP_BUILD_INFO` reported the exact package/version and source SHA with diagnostics enabled. Source-matched debug instrumentation run `ANDROID_SERIAL=emulator-5580 ./tools/codex-android :app:connectedDebugAndroidTest -PCANDIDATE_SOURCE_SHA=92c11fb0e41ae11b118b2e7bb105234d6606dbdb --max-workers=2` passed 64 tests, 0 failures, 0 skipped (`BUILD SUCCESSFUL` in 1m47). Private evidence: `/private/tmp/ja11-v0.8.1-order-92c11fb0/` (`emulator-runtime.log`, `emulator-package-dump.txt`, exact APK); Gradle report `app/build/reports/androidTests/connected/debug/index.html`. Emulator shut down after capture; no Pixel ADB target was queried.

- [ ] G6: All required PR checks pass on the exact pushed PR #80 head containing the reconciled main merge and candidate documentation.
  EVIDENCE: pending. The existing 8/8 result on remote head `261276eef6ec0b2cbc23048ae5a1d3c5e9b5edb9` does not qualify the new candidate.

- [ ] G7: The exact-candidate Pixel/JA11 session passes read-only identity first, completes the prepared acceptance checks, restores every changed baseline value, and releases the Pixel.
  EVIDENCE: pending. The 2026-10-09 source `1d19067c` session passed its identity gate and restored Mic On / UAC 2.0 / Off / volume 30 / 384 kHz / -3.7 dB / flat five-band state, but Flash failed band 1 before Save because that source wrote data before selecting User 1. Its full evidence is append-only in `docs/FIIO_JA11_VALIDATION_LEDGER.md`. Do not repeat Flash on that APK. The corrected exact candidate must not reach hardware until G2-G6 pass and its exact APK is verified. Begin with a read-only identity/baseline check; stop and release on absent/ambiguous identity or any uncertain result. Mic write is first only if a fresh baseline is Off; skip it when already On.

- [ ] G8: The release version, official signing/provenance, signed upgrade, clean install, and merge are completed under explicit owner authorization.
  EVIDENCE: pending; public v0.8.0 is immutable and unchanged. Merge/release approval is not inferred from test authorization.

- [ ] G9: Stable publication and independent public tag, release, APK, checksum, signer, and latest-state verification are completed under explicit owner authorization.
  EVIDENCE: pending; do not publish or make a new public JA11 support claim without the separate owner approval required by `AGENTS.md`.
