# JA11 v0.8.1 Mission State

As of 2026-10-09. This concise snapshot applies to the clean implementation branch. Historical physical outcomes remain append-only in the validation ledger.

## Candidate and repository

- Repository: `weekssa/OPRA-EQ-for-UAPP`
- Worktree: `/Users/stephenweeks/.codex/worktrees/ja11-v081-minimal/OPRA-EQ-for-UAPP`
- Branch: `codex/ja11-v0.8.1-minimal`
- Base: `9b9a1f610025b3d6233d4ff6f0c2309a668577cc`, equal to refreshed `origin/main`
- Frozen production source commit: `9493cf030acb440f92e547fc667f6a5399616045`. It follows the source implementation commit `32f1006fb1b1edbcba3b0470ed71772448e48886` only to correct the `applyEditorWorkingCopy` KDoc transaction order; no executable behavior changed in that final commit.
- PR #80 remains open/draft/unmerged at head `5f471dfdce93c35f93a4632ca76ff729fcb58a6b`; it is preserved as historical evidence and has not been changed.
- Draft PR [#81](https://github.com/weekssa/OPRA-EQ-for-UAPP/pull/81) is open for the clean candidate. Its live head and required-check results are authoritative; exact-head CI must pass before the candidate tuple is frozen.
- The clean A-E implementation, focused regressions, helper correction, and maintained evidence updates are committed. The exact-source diagnostic APK is built and host-verified. No physical result transfers to this source.

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

G1-G7 pass on frozen production source `9493cf030acb440f92e547fc667f6a5399616045`: the focused JA11 regressions pass; the complete JVM suite reports 761 tests with zero failures, errors, or skips; lint, debug/release/diagnostic/Android-test assembly, R8 mapping verification, isolated API 35 instrumentation (64/64), and guarded helper fixtures pass. The required ten-question independent review and supplemental review both pass; the reviewer found no blocker. The exact diagnostic APK is `98ee2eb876ec0d6d5bcfcebebdf55e1c77f2f7edfbecfd59322a94e5feea48ae`, package `com.weekssa.opraeqforuapp.ja11diag`, version `0.8.1-ja11diag` / code `12`, signer certificate SHA-256 `73aa7581c8dc7dcc8ccea7586771119a98f9a74d7d8cf23716e09c557c9f6b41`. Its DEX contains the source SHA; APK Signature Scheme v2 verifies and zip alignment passes. It was clean-installed and cold-launched on the disposable API 35 emulator. G9 exact-head CI and the PR-head part of G10 remain pending. G11 scope review passes. All emulator work used only `emulator-5554` on dedicated ADB port 5039; the emulator is shut down. No Pixel, JA11, or physical-device command has been run on this branch.

## Physical evidence and restoration

The latest prior physical mutation record is J026, tied to source `92c11fb0`. It records one accepted Save followed by detach about 677 ms later during final readback; no mutation was replayed, and a fresh complete snapshot matched the captured original baseline. That candidate's Flash result failed its readback step and was not qualified by that observation. The earlier Mic/UAC record J025 is separately attributed to its exact candidate. Neither physical result qualifies this clean branch.

The last verified prior session recorded the original device state restored before releasing the Pixel. The live state after release is unknown. Do not infer current state from the historical record.

## Next authorized steps

The required sequence is exact-head CI on the live PR #81 head, exact tuple freeze, the **CLEAN v0.8.1 CANDIDATE READY** report, then a new `PHONE WINDOW READY — PIXEL + JA11 NEEDED` request and owner confirmation. Follow the single-window test and restoration sequence in `PHONE-PLAN.md`. Do not infer the current PR head or CI status from this snapshot; read PR #81 immediately before freezing the tuple.

No merge, publication, or new public JA11 hardware-support claim is authorized.
