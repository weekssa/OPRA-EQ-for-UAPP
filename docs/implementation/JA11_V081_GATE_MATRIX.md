# JA11 v0.8.1 Gate Matrix

This matrix applies with the [mission Constitution](JA11_V081_CONSTITUTION.md). A result qualifies only the exact source, artifact, or PR head recorded with it.

## Gate definitions and current disposition

| Gate | Evidence required | Current disposition |
| --- | --- | --- |
| G0 Repository and historical evidence | Verify repository, clean branch/base, current PR #80 identity/state, and preserve its findings without importing its architecture. | Pass: base `9b9a1f610025b3d6233d4ff6f0c2309a668577cc`; source `9493cf030acb440f92e547fc667f6a5399616045`; PR #80 remains open/draft at `5f471dfdce93c35f93a4632ca76ff729fcb58a6b`. |
| G1 Focused JA11 regressions | Model D single/multiple candidate and serial cases; reset/readback; permission delay; timeout/cancellation; no replay; User 1 ordering; one Save; delayed-detach replacement readback. | Pass on source `9493cf0`; output fingerprint `e97021eab8ed2f2869049496050482604d3080a50100fdf1a2c5c21963079df6`. |
| G2 Full JVM suite | Complete `:app:testDebugUnitTest` on the coherent source. | Pass on `9493cf0`: 761 tests, zero failures/errors/skips; output fingerprint `cd5a3a071e329b7e96363cca5f2802c929975da00a9de35d4d1edef6f5030025`. |
| G3 Lint | `:app:lintDebug`; distinguish baseline warnings from errors in changed code. | Pass on `9493cf0`; output fingerprint `24570454d6a1875b7548df988870bfc511206b5be6bff8788612d4149a6a39b1`. |
| G4 Android builds | Assemble debug, release, separate-package diagnostic, and Android-test APKs. | Pass on `9493cf0`: all four APK variants assembled; output fingerprint `70a4a5c8632352aa2a763965c0348cdadcf1363ad09b150a4c0c2f472d6d93e6`. |
| G5 R8 | Verify the minified release mapping for the candidate source. | Pass on `9493cf0`: at least one application class is renamed in the nonempty release mapping. |
| G6 Isolated API 35 emulator | Run instrumentation on a disposable API 35 emulator. Never select or target the Pixel. | Pass: 64/64 on `ja11-v081-api35-clean-20261008`, pinned to `emulator-5554` on isolated ADB port 5039; the exact diagnostic APK also clean-installed and cold-launched there; emulator shut down. |
| G7 Phone-helper fixtures | Exercise clean install, verified in-place update, prior-APK preservation, and fail-closed query/hash/signer/version cases using fake ADB only. | Pass: `phone-session install fixtures passed`; no device access. |
| G8 Independent focused review | Reviewer answers the exact ten owner questions against the frozen diff. | Pass on source `9493cf030acb440f92e547fc667f6a5399616045`; all ten answers pass. Supplemental review confirms the only intervening KDoc correction resolved the finding and introduced no blocker. |
| G9 Exact-head CI | All required repository checks pass for the exact pushed draft-PR head. | Read the live PR #81 checks and require all eight check runs to be successful with `head_sha` equal to the current PR head. PR #80 results do not qualify it. |
| G10 Candidate tuple | Independently verify exact source SHA, PR head, diagnostic APK SHA-256, package, version/versionCode, and signer certificate SHA-256. | Source/APK fields pass: source `9493cf030acb440f92e547fc667f6a5399616045`, APK SHA-256 `98ee2eb876ec0d6d5bcfcebebdf55e1c77f2f7edfbecfd59322a94e5feea48ae`, package `com.weekssa.opraeqforuapp.ja11diag`, version/code `0.8.1-ja11diag` / `12`, signer SHA-256 `73aa7581c8dc7dcc8ccea7586771119a98f9a74d7d8cf23716e09c557c9f6b41`. PR #81 is open; freeze its exact live head with the tuple after G9 passes. |
| G11 Scope audit | Map each changed production file to A-E and confirm no unrelated shared-DAC behavior, discarded PR #80 architecture, diagnostic logging, or unsupported persistence claim. | Pass: all seven Kotlin/Gradle production files map to A-E or candidate identity; separate helper and tests stay within approved scope; reviewer confirmed shared defaults and prior architecture remain unchanged. |
| G12 Physical acceptance | One new owner-confirmed phone window after G0-G11; complete baseline, bounded tests, restoration, evidence capture, and immediate Pixel release. | Not started. No Pixel, JA11, or physical-device access has occurred on this branch; only the isolated API 35 emulator was used. |

## Invalidation rules

- A production-source change invalidates focused regressions, full JVM, lint, Android builds, R8, emulator results, independent source review, exact-head CI, and candidate provenance as applicable.
- A test-only change invalidates the affected test gate; it does not by itself change the APK source or invalidate unrelated app/emulator gates.
- A helper-only change requires helper fixtures and helper review; it does not invalidate production Android qualification.
- A workflow change invalidates the affected CI checks. A documentation-only change does not change executable candidate identity or invalidate Android validation.
- Preserve every superseded failure and candidate attribution in the append-only ledger. Never rewrite an earlier result as a pass.

## Candidate and phone-window rule

Freeze together: source SHA, PR head SHA, diagnostic APK SHA-256, package, version/versionCode, and signer certificate SHA-256. Do not access the Pixel until all applicable off-phone gates pass and the candidate tuple is reported. Then send exactly `PHONE WINDOW READY — PIXEL + JA11 NEEDED` and wait for fresh owner confirmation.

The first physical step is read-only: verify the exact installed candidate and capture one supported JA11, permissioned current session/generation, and complete device baseline. Serial is optional continuity evidence under Model D. Compare it when both sessions provide it; reject mismatch. Without a matching serial, report state verified on the sole returning JA11 and do not claim same-unit proof. The first permitted mutation, if still needed, restores Mic to its recorded original state with expected-reset handling and authoritative post-reconnect readback.

Merge, publication, and public support claims remain separate owner-approval gates.
