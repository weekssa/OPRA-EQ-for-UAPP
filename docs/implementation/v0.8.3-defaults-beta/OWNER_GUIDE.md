# EQ Library 0.8.3 Beta v1 owner guide

## Candidate identity

- Source: `7cea4545dddbee2da0b436e0684a9c489835e3cd` on `codex/v0.8.3-defaults-beta`.
- App: `com.weekssa.opraeqforuapp.v083beta`, version `0.8.3-beta.1`, code 14.
- APK SHA-256: `ac982eafd76f4db80f48e0f26040bf1f849388c4f0a51f778b51c8c013c59ce5`.
- Signer: local Android debug certificate SHA-256 `968b15ab4802bba98fbd058d7b81f537da6c2c5542333d0b107b3495135587b4`.
- Full APK and test APK identity is in `ARTIFACTS.md`.

This is a separate beta app. It does not replace the stable app. The exact minified Beta APK installs and launches on API 26 and 35 emulator images. The minified Beta instrumentation runner crashes before test discovery on both images because `androidx.tracing.Trace` is absent from the split app/test runtime. On the exact source, the unminified Debug JA11 UI class passes 9/9 on API 26 and the full Debug UI suite passes 72/72 on API 35. The API 26 Debug full suite has four preserved unrelated failures. The owner must explicitly accept this bounded substitution before a phone session begins; see `TEST_RESULTS.md`.

## After gate exception acceptance

Only after the owner accepts that exact emulator-gate substitution, request confirmation that the phone is ready, unlocked, and wireless debugging is enabled. Then automate wireless ADB installation of the exact APK and automatic app/platform log and report capture. The owner should not need to run Terminal, Git, or ADB commands.

Before connecting the JA11, close competing USB clients without deleting their apps or data. Confirm the app's current device identity, firmware, UAC, mic/remote state, volume, EQ program and any available User 1 baseline. Use conservative listening volume. Keep volume/program targets visible and reviewed together before explicit **Save defaults** confirmation. Let the app stage values and perform one reviewed operation; do not repeat a setting after timeout, detach, ambiguous reconnect, mismatch, or any uncertain result.

For the persistence check, wait until the app confirms both mic writes, both replacement sessions, original-mic restoration, final target pair and companion readback. Then pause for the owner's physical JA11 unplug/replug. Resume only after the owner confirms the JA11 was physically disconnected and reconnected, and the app reacquires it for fresh readback. A simulated detach, app restart, or USB re-enumeration is not a power-removal test.

Stop at the first genuine failed or uncertain transaction. Preserve logs and reports privately; do not replay or auto-restore a mutation. Leave the exact beta and its app data installed on PASS or FAIL. Report actual or unknown final volume, program, mic, UAC, product ID, firmware, connection and EQ-bank comparison state. Keep runtime values distinct from verified persisted defaults. Do not claim EQ-bank preservation when either User 1 snapshot was unavailable.

## Expected product behavior

- Cancel or dismiss staged edits without a write.
- Apply a changed volume/program for the current session only.
- Set as Default opens a confirmation with both volume and program; Cancel returns to the editor without writing.
- Save defaults performs the reviewed operation and reports success only after the requested pair, original microphone state, companion state and applicable read-only EQ comparison are verified on fresh sessions.
- No native EQ Save, UAC changes, EQ-band writes, helper program selection or replay is used for the defaults action.
- Keep the exact beta installed for owner manual testing after the automated session. Wait for the owner to identify this candidate as passing and direct publication; that instruction activates the existing merge/publish pre-authorization.
