# JA11 v0.8.1 Gate Matrix

This matrix applies with the [mission Constitution](JA11_V081_CONSTITUTION.md). A result qualifies only the exact source, artifact, or PR head recorded with it.

## Gate definitions and current disposition

| Gate | Evidence required | Current disposition |
| --- | --- | --- |
| G0 Repository and historical evidence | Verify repository, clean branch/base, current PR #80 identity/state, and preserve its findings without importing its architecture. | Pass: base `9b9a1f610025b3d6233d4ff6f0c2309a668577cc`; source `9493cf030acb440f92e547fc667f6a5399616045`; PR #80 remains open/draft at `5f471dfdce93c35f93a4632ca76ff729fcb58a6b`. |
| G1 Focused JA11 regressions | Model D single/multiple candidate and serial cases; reset/readback; permission delay; timeout/cancellation; no replay; User 1 ordering; one Save; delayed-detach replacement readback; production Android USB-session enumeration/open/detach behavior. | Pass on source `9493cf0`: focused JA11 and `AndroidKt02h20HidSession` regressions, including serial fallback, candidate ambiguity, delayed permission, open/claim failure, detach fencing, and replacement timeout; output fingerprint `6607bbdbc194446f8216d0f3509d8db3791799ba288420d532b60a1f449737b9`. |
| G2 Full JVM suite | Complete `:app:testDebugUnitTest` on the coherent source. | Pass on source `9493cf0`: 768 tests, zero failures/errors/skips; output fingerprint `263381d7b944cc27e80f01107295f21ebaf6f8c9b2dfe33fddbc92dcb407e102`. |
| G3 Lint | `:app:lintDebug`; distinguish baseline warnings from errors in changed code. | Pass; output fingerprint `fa0281e9a677f3c2067fd87794ef35a43da37ed3de7af52106e3b05a0733275a`. |
| G4 Android builds | Assemble debug, release, separate-package diagnostic, and Android-test APKs. | Pass: all four APK variants assembled; output fingerprint `9b3c4f302e44f537933654884c7e50d4ffa5fa9c0b7fc0fc32fb58f6c7462dee`. |
| G5 R8 | Verify the minified release mapping for the candidate source. | Pass on `9493cf0`: at least one application class is renamed in the nonempty release mapping. |
| G6 Isolated API 35 emulator | Run instrumentation on a disposable API 35 emulator. Never select or target the Pixel. | Reuse prior pass: 64/64 on `ja11-v081-api35-clean-20261008`, pinned to `emulator-5554` on isolated ADB port 5039; APK clean-install/cold-launch passed there and emulator shut down. Production Android source is unchanged by this continuation. A new rerun could not select the stopped AVD; no device was selected as a substitute. |
| G7 Phone-helper fixtures | Exercise clean install, verified in-place update, prior-APK preservation including filename collision, and fail-closed query/hash/signer/version cases using fake ADB only. | Pass: `phone-session install fixtures passed`; the collision fixture preserves the existing rollback copy and stops before pull/install. |
| G8 Independent focused review | Reviewer answers the exact ten owner questions against the revised tests/helper and frozen production source. | Pending a fresh independent review after the tests/helper fix. The previous supplemental review was INCONCLUSIVE and is preserved as historical. |
| G9 Exact-head CI | All required repository checks pass for the exact pushed draft-PR head. | The old head `2e81dc99f4335aaa70645fad33adb391626e7cdc` has eight successful checks, but does not qualify the new local changes. Read live PR #81 after pushing the revised candidate and require every required check's `head_sha` to match. |
| G10 Candidate tuple | Independently verify exact source SHA, PR head SHA, diagnostic APK SHA-256, package, version/versionCode, and signer certificate SHA-256. | Source/artifact fields are reverified: source `9493cf030acb440f92e547fc667f6a5399616045`, APK SHA-256 `98ee2eb876ec0d6d5bcfcebebdf55e1c77f2f7edfbecfd59322a94e5feea48ae`, package `com.weekssa.opraeqforuapp.ja11diag`, version/code `0.8.1-ja11diag` / `12`, signer SHA-256 `73aa7581c8dc7dcc8ccea7586771119a98f9a74d7d8cf23716e09c557c9f6b41`. Freeze the final PR head after G8/G9. |
| G11 Scope audit | Map each changed production file to A-E and confirm no unrelated shared-DAC behavior, discarded PR #80 architecture, diagnostic logging, or unsupported persistence claim. | Pass: all seven Kotlin/Gradle production files map to A-E or candidate identity; separate helper and tests stay within approved scope; reviewer confirmed shared defaults and prior architecture remain unchanged. |
| G12 Physical acceptance | One owner-confirmed phone window after G0-G11; complete baseline, bounded tests, restoration, evidence capture, and immediate Pixel release. | J027 is partial physical evidence on prior PR #81 head `2e81dc99f4335aaa70645fad33adb391626e7cdc`, source `9493cf030acb440f92e547fc667f6a5399616045`, APK `98ee2eb876ec0d6d5bcfcebebdf55e1c77f2f7edfbecfd59322a94e5feea48ae`. UAC round trip and fresh Flash readback matched the captured baseline; Mic was already On. Wire-level Save count was not evidenced, Test D was not run, and cleanup/restoration/release were not verified in the capture. Do not relabel J027 to the revised PR head; current live state is unknown. |

## Invalidation rules

- A production-source change invalidates focused regressions, full JVM, lint, Android builds, R8, emulator results, independent source review, exact-head CI, and candidate provenance as applicable.
- A test-only change invalidates the affected test gate; it does not by itself change the APK source or invalidate unrelated app/emulator gates.
- A helper-only change requires helper fixtures and helper review; it does not invalidate production Android qualification.
- A workflow change invalidates the affected CI checks. A documentation-only change does not change executable candidate identity or invalidate Android validation.
- Preserve every superseded failure and candidate attribution in the append-only ledger. Never rewrite an earlier result as a pass.

## Candidate and phone-window rule

Freeze together: source SHA, PR head SHA, diagnostic APK SHA-256, package, version/versionCode, and signer certificate SHA-256. Do not access the Pixel until all applicable off-phone gates pass and the candidate tuple is reported. The owner has already supplied the fresh phone-window confirmation on 2026-10-10; use it only after the final report.

The first physical step is read-only: verify the exact installed candidate and capture one supported JA11, permissioned current session/generation, and complete device baseline. Serial is optional continuity evidence under Model D. Compare it when both sessions provide it; reject mismatch. Without a matching serial, report state verified on the sole returning JA11 and do not claim same-unit proof. The first permitted mutation, if still needed, restores Mic to its recorded original state with expected-reset handling and authoritative post-reconnect readback.

Merge, publication, and public support claims remain separate owner-approval gates.
