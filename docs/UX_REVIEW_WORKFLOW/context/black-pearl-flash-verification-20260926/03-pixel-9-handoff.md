# Black Pearl Flash verification — Pixel 9 owner handoff

This checklist is the final physical gate for the exact signed candidate produced by Luna. Do not use it with an older APK or an APK whose source SHA, checksum, or signer is not recorded.

## Candidate provenance — verified before owner handoff

- Repository: `weekssa/OPRA-EQ-for-UAPP`
- Source branch: `main`
- Exact candidate source SHA: `ce5efdf7985e4fc48f975b14fcedb1f592d43772` (PR #48 merge commit)
- APK filename: `EQ-Library-v0.7.0.apk`
- APK SHA-256: `3d723ffa17042fbef7e6e192c14ecce460628d0f08a55eeb30caa59566ff8731`
- Package: `com.weekssa.opraeqforuapp`
- `versionName` / `versionCode`: `0.7.0` / `7`
- Build variant: `release signed candidate`
- Signer certificate SHA-256: `65c1c1256dae3c49e3548f334c91f0ba991969e9be9e0b223ba4e253d2114747`
- CI workflow/run: [Signed Release Candidate #9](https://github.com/weekssa/OPRA-EQ-for-UAPP/actions/runs/36299335354)
- Artifact: ID `10924538542`, digest `sha256:e950b34686cb12406e0828cbd138ed7d866c3760b9b7c987a24541410dfbb9c9`
- Install mode: download the exact Actions artifact ZIP, extract `EQ-Library-v0.7.0.apk`, and install that APK only. Emulator smoke used `adb install -r`; Pixel 9 installation remains owner-controlled.
- Local/emulator checks: `PASS` for Gradle unit tests, lint, debug/release assembly, R8, local release-signature tests, and 20/20 instrumented tests on the `codex-api36` emulator. The emulator was stopped after testing.

Do not install or mutate hardware with any APK other than the exact candidate identified above. Current software handoff status: `READY_FOR_PIXEL_9`.

The signed candidate artifact ZIP is available at `/Users/stephenweeks/Downloads/EQ-Library-v0.7.0-signed-ce5efdf7985e4fc48f975b14fcedb1f592d43772.zip`. The artifact ZIP SHA-256 is `e950b34686cb12406e0828cbd138ed7d866c3760b9b7c987a24541410dfbb9c9`.

The local unsigned release APK remains unsuitable for Pixel 9 testing. If the downloaded APK does not match every identity above, stop before installing and report the mismatch.

## Starting-state safety

1. Use the owner’s Pixel 9 and the exact TRN Black Pearl that is already qualified for this product path.
2. Start from a known device state. In the app, connect/read the DAC and capture the current My DAC EQ state if the current app flow offers capture.
3. Proceed only if the original state can be restored through a known saved/app-owned EQ or the owner has explicitly accepted the bounded state change. Do not rely on memory or a screenshot alone to restore arbitrary hardware state.
4. Close other USB-control apps and keep the Black Pearl connected directly as instructed by the app.

## Test 1 — successful Flash is verified

**Question:** Does the exact candidate show success only after the Black Pearl’s final native EQ and global-gain readback match the requested target?

1. Open EQ Library and select TRN Black Pearl as the active hardware output.
2. Connect the Black Pearl through My EQs/My DAC and wait for the app to show the current connected state.
3. Choose one known saved EQ that is already suitable for Black Pearl and whose original state can be restored.
4. Open the normal Flash confirmation. Confirm the displayed target and playback-gain implication are expected, then confirm Flash once.
5. Keep the DAC connected and do not leave the app while the operation is running.
6. Expected success result: a compact message beginning with `Flash successful` and stating that the TRN Black Pearl EQ was saved and verified and that final hardware readback matched. Existing fidelity/playback-gain or safety-warning detail may follow.
7. Open or refresh My DAC EQ if the app offers the normal read-only state view. The displayed native state must correspond to the flashed EQ; this is supporting evidence, not a replacement for the transaction result.

**Immediate stop conditions:** a permission loop, disconnect, unexpected device replacement, missing progress, readback mismatch, message that claims success without verification language, or any request to retry the mutation. Capture a screenshot and the app-generated report if available, then stop.

## Test 2 — failure is not reported as success

This test is normally covered by automated transport fault injection. Do not deliberately create a hardware failure just to exercise the error copy. If the real Pixel session produces an unverified result:

1. Do not tap Flash again.
2. Confirm the message says the Flash was not verified or otherwise gives a precise failure, not `Flash successful`.
3. Follow only the displayed reconnect/refresh recovery guidance after the evidence is captured.

## Restoration and evidence return

1. Restore the original EQ using the known app-owned/saved path, if the test changed it and restoration is authorized.
2. Read the restored state in My DAC and verify it matches the captured starting state as far as the app exposes it.
3. Return the exact candidate identity, outcome (`PASS`, `FAIL`, `INCONCLUSIVE`, or `NOT EXERCISED`), screenshots, any app-generated report, whether a permission prompt appeared, whether a disconnect occurred, and whether the original state was restored.

## Claim boundary

`PASS` proves the exact candidate completed the tested Black Pearl Flash and its final readback was reported truthfully on this Pixel 9 session. It does not by itself prove every EQ profile, every firmware revision, every acoustic result, or a public release. Do not repeat a mutation after an uncertain result without a new engineering decision and explicit owner authorization.
