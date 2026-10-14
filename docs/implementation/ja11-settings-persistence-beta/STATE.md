# JA11 settings persistence beta state

Status: **ALL OFF-PHONE SOFTWARE AND PRIVATE APK GATES PASS**. This is a private beta-only feature; no production support or hardware
qualification claim is made by this implementation record.

## Mission tuple

- Repository: `weekssa/OPRA-EQ-for-UAPP`
- Prepared worktree: `/Users/stephenweeks/.codex/worktrees/ja11-settings-persistence-beta/OPRA-EQ-for-UAPP`
- Branch: `codex/ja11-settings-persistence-beta`
- Base: `421f03331e45e6c1bdc77e5442db1de467432930`
- Plan commit: `9544bfded026229ee99944786995c7ab5e4ffb90`
- Candidate source: `620e1a46761de2d3621dbcd32135193efb42cf49`
- Draft PR: [#96](https://github.com/weekssa/OPRA-EQ-for-UAPP/pull/96); executable candidate SHA `620e1a46761de2d3621dbcd32135193efb42cf49`
- Exact-head workflow: [run 38512637427](https://github.com/weekssa/OPRA-EQ-for-UAPP/actions/runs/38512637427), all four jobs passed against the exact candidate SHA

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
| API 35 exact-APK install and cold launch | PASS | Exact APK installed alongside stable app on disposable `emulator-5554`; both packages remained installed |
| API 26 exact-APK install and cold launch | PASS | Exact APK cold-launched on disposable `emulator-5556`; API 26 emulator shut down after verification |
| Independent source review | PASS | Required scope/safety review complete; follow-up confirms initial findings closed |
| Exact-head CI | PASS | Manual fallback run 38512637427; build, exact-head CodeQL, API 26 smoke, and API 35 UI jobs all passed against `620e1a46761de2d3621dbcd32135193efb42cf49` |
| Private beta APK provenance | PASS | Exact APK metadata, injected source SHA, debug signer, alignment, checksum, and API 26/API 35 installs recorded in `ARTIFACTS.md` |
| Standard Android CI workflow | FAIL (existing workflow) | Run 38512307326 fails before jobs exist; same known malformed-workflow issue on the base branch, not candidate-specific |
| PR CodeQL | PASS | Run 38512320065 on exact PR head |
| Private beta phone install | PASS | After owner confirmation, exact APK installed on Pixel 9 / API 37; package/version verified; app was not launched |
| JA11 / physical hardware testing | PENDING OWNER | Owner performs normal manual hardware testing; no JA11 connection or operation has occurred in this task |

## Safety boundary

The owner confirmed the phone was ready and wireless debugging was enabled. The exact private beta
APK was installed on the Pixel 9 / API 37 and its package/version was verified; it was left
unopened. No JA11 was connected and no hardware operation or physical capability test occurred.
The owner conducts normal hardware testing manually. Stop at the first uncertain physical result;
no mutation replay or recovery mutation is implied.
