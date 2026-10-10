# JA11 v0.8.1 Mission State

As of 2026-10-10. This concise snapshot applies to the clean implementation branch. Historical physical outcomes remain append-only in the validation ledger.

## Candidate and repository

- Repository: `weekssa/OPRA-EQ-for-UAPP`
- Worktree: `/Users/stephenweeks/.codex/worktrees/ja11-v081-minimal/OPRA-EQ-for-UAPP`
- Branch: `codex/ja11-v0.8.1-minimal`
- Base: `9b9a1f610025b3d6233d4ff6f0c2309a668577cc`, equal to refreshed `origin/main`
- Frozen production source commit: `9493cf030acb440f92e547fc667f6a5399616045`. It follows the source implementation commit `32f1006fb1b1edbcba3b0470ed71772448e48886` only to correct the `applyEditorWorkingCopy` KDoc transaction order; no executable behavior changed in that final commit.
- PR #80 remains open/draft/unmerged at head `5f471dfdce93c35f93a4632ca76ff729fcb58a6b`; it is preserved as historical evidence and has not been changed.
- Draft PR [#81](https://github.com/weekssa/OPRA-EQ-for-UAPP/pull/81) is open for the clean candidate. Its live head and required-check results are authoritative; exact-head CI must pass before the candidate tuple is frozen.
- The clean A-E production implementation is frozen at `9493cf030acb440f92e547fc667f6a5399616045`. This continuation adds real Android USB-session Robolectric regressions and a guarded helper backup-collision fix; those additions do not change production runtime behavior. The new local changes are being finalized for PR #81. The APK tuple below is an already host-verified exact-source artifact; physical outcomes remain tied to their recorded PR head and are not being relabelled.

## Scope and implementation status

The candidate is limited to expected Mic/UAC reset verification, Model D optional-serial and unique-candidate handling, User 1-before-data ordering, late-Save readback-only recovery, and stale-session/no-replay protection. The separate helper install guard verifies and preserves an exact prior diagnostic APK before `install -r`, permits guarded plain install only when absent, and fails closed on ambiguous or failed state queries.

### Production scope map

The production diff is 420 insertions and 71 deletions across seven files (491 changed lines), all mapped to A-E or candidate identity:

| Production file | Requirement | Reason |
| --- | --- | --- |
| `app/build.gradle.kts` | Candidate identity | Set v0.8.1/versionCode 12 and add a separate `.ja11diag` candidate variant with source-SHA metadata. |
| `data/dac/FiioJa11ControlRepository.kt` | A, B, E | Bind Mic/UAC pending verification to the prior session, detach generation, optional serial, sole-candidate state, and authoritative replacement readback. |
| `data/kt02h20/AndroidFiioJa11UsbTransport.kt` | A, B, D, E | Apply strict JA11 candidate rules, preserve accepted Mic/UAC/Save writes across expected detach, and coordinate Save and replacement-readback boundaries. |
| `data/kt02h20/AndroidKt02h20HidSession.kt` | A, B, D, E | Capture optional opened-session serial, enforce JA11-only candidate/interface cardinality, invalidate stale sessions, wait for expected detach and fresh open/claim, and observe bounded Save detach events. The strict flag remains off for other transports. |
| `domain/kt02h20/FiioJa11Flasher.kt` | C, D, E | Select and verify User 1 before EQ writes; allow only final-readback retry after a Save detach and require one stable generation. |
| `ui/EqLibraryViewModel.kt` | A, E | Start expected-reset verification immediately and avoid a duplicate verifier when the connection collector sees the replacement session. |
| `ui/FiioJa11DeviceUiState.kt` | A | Clear the pending reset-write record after failure so stale restart state cannot be reused. |

Current local gates pass: focused JA11 plus Android USB-session regressions; 768 JVM tests with zero failures, errors, or skips; lint; debug/release/diagnostic/Android-test assembly; R8 mapping; and helper install fixtures. The Android session suite contains seven Robolectric tests against the production `AndroidKt02h20HidSession`. The earlier 64/64 isolated API 35 instrumentation result remains valid because the production source is unchanged; a later emulator rerun could not select the stopped AVD and was not redirected to the Pixel. G8 requires a fresh independent review of the revised tests/helper; G9 must be refreshed on the final live PR #81 head; G10 is not frozen until that head is known. G11 scope audit passes.

The host-verified diagnostic APK is `98ee2eb876ec0d6d5bcfcebebdf55e1c77f2f7edfbecfd59322a94e5feea48ae`, package `com.weekssa.opraeqforuapp.ja11diag`, version `0.8.1-ja11diag` / code `12`, signer certificate SHA-256 `73aa7581c8dc7dcc8ccea7586771119a98f9a74d7d8cf23716e09c557c9f6b41`. Its DEX contains source SHA `9493cf030acb440f92e547fc667f6a5399616045`; APK Signature Scheme v2 verifies and zip alignment passes. The APK clean-installed and cold-launched on the disposable API 35 emulator. The owner has reported the phone window ready, but no additional physical action begins until the final off-phone gates and exact tuple are reported.

## Physical evidence and restoration

J025/J026 remain historical records on their exact source/APK tuples. A later partial physical session, J027, used source `9493cf030acb440f92e547fc667f6a5399616045`, prior PR #81 head `2e81dc99f4335aaa70645fad33adb391626e7cdc`, and the diagnostic APK below. It observed Mic already On (no Mic write), passed the UAC round trip and fresh readbacks, and obtained a matching fresh User 1 readback after one Flash action from Off. The app-generated transaction report was not captured after expiry, so wire-level Save count was not physically evidenced; Test D was not run. Temporary profile/device-file cleanup, complete final restoration, and phone release were not verified in that capture. Keep J027 partial and tied to that exact tuple; do not promote it to the final PR head. Current live device state is unknown.

The live state after the partial J027 session is unknown. Do not infer current state from a historical snapshot. Any new physical session must begin with the exact-candidate read-only baseline and use that baseline as the only restoration authority.

## Next authorized steps

The required sequence is fresh independent review, exact-head CI on the live PR #81 head, exact tuple freeze, the **CLEAN v0.8.1 CANDIDATE READY** report, then the already owner-confirmed phone window. Follow the single-window test and restoration sequence in `PHONE-PLAN.md`. Do not infer the current PR head or CI status from this snapshot; read PR #81 immediately before freezing the tuple.

No merge, publication, or new public JA11 hardware-support claim is authorized.
