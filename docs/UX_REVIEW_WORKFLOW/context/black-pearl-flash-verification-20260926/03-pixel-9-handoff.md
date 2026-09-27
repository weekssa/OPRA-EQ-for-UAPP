# Black Pearl Flash verification — Pixel 9 owner handoff

This checklist is the final physical gate for the exact signed candidate produced by Luna. Do not use it with an older APK or an APK whose source SHA, checksum, or signer is not recorded.

## Candidate provenance — Luna must fill before handoff

- Repository: `weekssa/OPRA-EQ-for-UAPP`
- Source branch: `codex/ja11-protocol-evidence`
- Exact implementation source SHA: `7bb419bc` (`Verify Black Pearl Flash with final readback`); the final signed candidate source SHA will be the reviewed merge commit on `main` and must be recorded before installation.
- APK filename: `NOT PRODUCED — signed workflow not run`
- APK SHA-256: `NOT AVAILABLE`
- Package: `com.weekssa.opraeqforuapp`
- `versionName` / `versionCode`: `0.7.0` / `7`
- Build variant: `release signed candidate — NOT PRODUCED`
- Signer certificate SHA-256: `NOT AVAILABLE`
- CI workflow/run and artifact URL or ID: `PENDING exact reviewed main source and signed-beta dispatch`
- Install mode: `PENDING exact signed candidate provenance`
- Local/emulator checks: `PASS` for Gradle unit tests, lint, debug/release assembly, R8, local release-signature tests, and 20/20 instrumented tests on the `codex-api36` emulator. The emulator was stopped after testing.

Do not install or mutate hardware until the signed candidate filename, checksum, signer, source SHA, and CI/artifact provenance are all filled for the exact APK under test. Current software handoff status: `SOFTWARE_READY_PENDING_SIGNED_CANDIDATE`.

A local unsigned release APK exists at `app/build/outputs/apk/release/app-release-unsigned.apk` with SHA-256 `2594c26d4ee7033aaccfbf3c0444fd853a9838b696c16aff250a5a6b68bd23d5`; it is not installable as the signed Pixel 9 candidate and must not be used for hardware testing.

If any required identity is missing, stop before installing and return the missing provenance to the agent.

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
