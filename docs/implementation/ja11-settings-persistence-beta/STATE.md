# JA11 settings persistence beta state

Status: **LOCAL SOFTWARE GATES PASS; EXACT-HEAD AND EXACT-APK GATES PENDING**. This is a private beta-only feature; no production support or hardware
qualification claim is made by this implementation record.

## Mission tuple

- Repository: `weekssa/OPRA-EQ-for-UAPP`
- Prepared worktree: `/Users/stephenweeks/.codex/worktrees/ja11-settings-persistence-beta/OPRA-EQ-for-UAPP`
- Branch: `codex/ja11-settings-persistence-beta`
- Base: `421f03331e45e6c1bdc77e5442db1de467432930`
- Plan commit: `9544bfded026229ee99944786995c7ab5e4ffb90`
- Candidate source: pending the first frozen executable commit
- PR / exact-head CI: pending

## Scope

The separate `ja11Beta` build routes only changed My DAC volume Apply and DEVICE EQ-program
selection through one typed, program-aware native Save transaction. Production debug and release
default the policy off. Existing custom-EQ Flash/editor/Reset, microphone, UAC, other DACs, public
release metadata, and physical capability classifications retain their established behavior.

The transaction records coherent baseline, target and Save attempts, program/session/detach
boundaries, pre/post readable EQ state, final complete DEVICE readback, and whether the final state
is still current. Built-in program raw parameter data is not described as stored User 1 bank state.
Missing built-in coefficient evidence remains `INSUFFICIENT_EVIDENCE`; a required User 1 read
failure stops before mutation. The report describes software-recorded request counts and does not
claim USB bus counts or firmware reboot counts.

## Gate status

| Gate | Status | Evidence |
| --- | --- | --- |
| Prepared branch/worktree | PASS | Exact path and branch checked before edits |
| Source implementation | PASS | Beta-only routing, typed Save codec/transport, one-gate transaction, retained truthful report UI |
| Focused and full JVM suites | PASS | `:app:testDebugUnitTest` and `:app:testJa11BetaUnitTest`; repository/transport and beta BuildConfig coverage included |
| Debug/beta lint and debug/release assembly | PASS | `:app:lintDebug`, `:app:lintJa11Beta`, `:app:assembleDebug`, `:app:assembleRelease` |
| R8 mapping | PASS | `bash tools/verify-r8-mapping.sh`; at least one application class renamed |
| Beta Android-test Kotlin compile | PASS | `:app:compileJa11BetaAndroidTestKotlin` |
| API 35 beta instrumentation | PASS | All 70 tests passed on disposable `codex_ja11_beta_api35` (`emulator-5554`) |
| API 35/API 26 exact-APK install and launch | PENDING | Must run on the frozen APK bytes; API 26 AVD remains to be started |
| Independent source review | PASS | Required scope/safety review complete; follow-up confirms initial findings closed |
| Exact-head CI | NOT RUN | Candidate must be pushed; use exact-head fallback workflow |
| Private beta APK provenance | PENDING | Build, signer/hash/alignment/package metadata, and exact-emulator verification pending |
| Phone install / physical use | NOT AUTHORIZED YET | Ask only after all off-phone gates; wait for owner confirmation |

## Safety boundary

No physical hardware has been addressed. Do not probe a remembered wireless endpoint or connect,
install, or run a JA11 operation before the owner confirms the phone is ready and wireless debugging
is enabled. The owner conducts normal hardware testing manually. Stop at the first uncertain
physical result; no mutation replay or recovery mutation is implied.
