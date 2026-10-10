# JA11 v0.8.1 Gate Matrix

This matrix applies with the [mission Constitution](JA11_V081_CONSTITUTION.md). A result qualifies only the exact source, artifact, or PR head recorded with it.

## Gate definitions and current disposition

| Gate | Evidence required | Current disposition |
| --- | --- | --- |
| G0 Repository and historical evidence | Verify repository, clean branch/base, current PR #80 identity/state, and preserve its findings without importing its architecture. | Pass: base/current HEAD `9b9a1f610025b3d6233d4ff6f0c2309a668577cc`; PR #80 remains open/draft at `5f471dfdce93c35f93a4632ca76ff729fcb58a6b`. |
| G1 Focused JA11 regressions | Model D single/multiple candidate and serial cases; reset/readback; permission delay; timeout/cancellation; no replay; User 1 ordering; one Save; delayed-detach replacement readback. | Pass on current source. |
| G2 Full JVM suite | Complete `:app:testDebugUnitTest` on the coherent source. | Pass: 761 tests, zero failures/errors/skips. |
| G3 Lint | `:app:lintDebug`; distinguish baseline warnings from errors in changed code. | Pass. |
| G4 Android builds | Assemble debug, release, separate-package diagnostic, and Android-test APKs. | Pass: debug, release, `ja11Diagnostic`, and Android-test APKs assembled. |
| G5 R8 | Verify the minified release mapping for the candidate source. | Pass: at least one application class is renamed in the nonempty release mapping. |
| G6 Isolated API 35 emulator | Run instrumentation on a disposable API 35 emulator. Never select or target the Pixel. | Pass: 64/64 on `ja11-v081-api35-clean-20261008`, pinned to `emulator-5554` on isolated ADB port 5039; emulator shut down. |
| G7 Phone-helper fixtures | Exercise clean install, verified in-place update, prior-APK preservation, and fail-closed query/hash/signer/version cases using fake ADB only. | Pass: `phone-session install fixtures passed`; no device access. |
| G8 Independent focused review | Reviewer answers the exact ten owner questions against the frozen diff. | Pending. |
| G9 Exact-head CI | All required repository checks pass for the exact pushed draft-PR head. | Pending; no new PR exists yet. |
| G10 Candidate tuple | Independently verify exact source SHA, PR head, diagnostic APK SHA-256, package, version/versionCode, and signer certificate SHA-256. | Pending; candidate not frozen. |
| G11 Scope audit | Map each changed production file to A-E and confirm no unrelated shared-DAC behavior, discarded PR #80 architecture, diagnostic logging, or unsupported persistence claim. | Pending final review. |
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
