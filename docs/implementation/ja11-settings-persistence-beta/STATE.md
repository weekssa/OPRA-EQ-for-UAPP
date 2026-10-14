# JA11 settings persistence beta state

Status: **SOFTWARE GATES PASS; OWNER HARDWARE REPORTS INCLUDE A USER_1 EQ MISMATCH — HOLD FOR DIAGNOSIS**. This private beta is not hardware-qualified.

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
| Private beta phone install | PASS | After owner confirmation, exact APK installed on Pixel 9 / API 37; package/version verified |
| Owner beta report review | MIXED; HOLD | Volume Save and Off-program Save reports passed; selecting User 1 returned `FINAL_EQ_MISMATCH` with a stored-bank difference |
| Original-state restoration | NOT VERIFIED | Exported reports end at volume 56 / Off; no report documents restoring the original volume 60 or the pre-Save User 1 bank |

## Safety boundary

The owner confirmed the phone was ready and wireless debugging was enabled. The exact private beta
APK was installed and the owner later conducted the manual session. The reports tie the operations
to candidate `620e1a46761de2d3621dbcd32135193efb42cf49`, Pixel 9 / API 37, JA11 firmware 2.20,
and USB product ID 258; no USB serial is retained. The volume 60→56 Save and the later Off-program
selection report `SUCCESS`. The intervening Off→User 1 selection reports `FINAL_EQ_MISMATCH`: the
User 1 stored bank read before Save differed from the bank read after Save. The later Off Save
started from the already-flat User 1 bank, so it does not establish restoration of that bank.

The report set contains no volume 56→60 transaction or other proof that the original volume was
restored. Therefore the physical session is **FAIL / RESTORATION NOT VERIFIED**, not a beta pass.
Report transport counts are software-recorded and do not prove USB bus packet counts or firmware
reboot counts. Do not issue another Save, retry the User 1 selection, or attempt recovery until the
failure is diagnosed and an explicit recovery plan is authorized. Keep the owner-shared raw reports
outside GitHub; a sanitized outcome is appended to `docs/FIIO_JA11_VALIDATION_LEDGER.md`.
