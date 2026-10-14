# EQ Library 0.8.3 Beta v1 owner guide

## Candidate identity

- Source: `7cea4545dddbee2da0b436e0684a9c489835e3cd` on `codex/v0.8.3-defaults-beta`.
- App: `com.weekssa.opraeqforuapp.v083beta`, version `0.8.3-beta.1`, code 14.
- APK SHA-256: `ac982eafd76f4db80f48e0f26040bf1f849388c4f0a51f778b51c8c013c59ce5`.
- Signer: local Android debug certificate SHA-256 `968b15ab4802bba98fbd058d7b81f537da6c2c5542333d0b107b3495135587b4`.
- Full APK and test APK identity is in `ARTIFACTS.md`.

This is a separate beta app. It does not replace the stable app. The exact minified Beta APK installs and launches on API 26 and 35 emulator images. The minified Beta instrumentation runner crashes before test discovery on both images because `androidx.tracing.Trace` is absent from the split app/test runtime. On the exact source, the unminified Debug JA11 UI class passes 9/9 on API 26 and the full Debug UI suite passes 72/72 on API 35. The API 26 Debug full suite has four preserved unrelated failures. The owner explicitly accepted these bounded substitutions on 2026-10-14; they remain exceptions, not automated passes. See `TEST_RESULTS.md`.

## Automated phone results and owner review

The exact APK is installed on the owner's Pixel 9 and remains open on My DAC. Stable 0.8.2 and the prior separate JA11 beta remain installed. The owner authorized USB `Always allow`.

On the exact candidate, staged volume/program changes cancelled without changing hardware readback. Runtime-only Apply changed volume to 20. Three confirmed defaults actions passed: changed volume to 19, unchanged-current volume 19, then non-User-1 program Classic at volume 19. The app-reported volume/program/mic write counts were `1/0/2`, `0/0/2`, and `0/1/2`; each restored mic On and reported no EQ-band writes. After the owner physically disconnected JA11 power for 10 seconds, fresh readback remained volume 19/60 and Classic. Current final device state is mic On, UAC 2.0, 384 kHz, firmware 2.20, VID:PID `2972:0102`.

The beta reports User 1 EQ data could not be compared for the tested Off/Classic programs. The EQ tab now reports last-read Classic; its built-in coefficients are not exposed by maintained JA11 protocol evidence, so it shows no invented graph. Your manual acceptance is recorded for this identified beta, while no public User 1 preservation or same-unit continuity claim is made. Write counts are app reports, not USB bus captures; a unique USB serial/fingerprint was not captured.

Private automated evidence is under `/private/tmp/ja11-v083-session-20261014/`; the sanitized summary digest is in J032. Review the evidence limits and continue any manual testing you want with this exact installed candidate. The tested final volume/program are 19/Classic; mic is On, UAC 2.0, sample rate 384 kHz and firmware 2.20. Avoid another Set as Default action unless you intend to change the device's saved defaults.

Your 2026-10-14 instruction identifies this candidate as passing and authorizes publication of v0.8.3. The authorized merge, official signing, release checks, and publication are now in progress; no further approval is required for that chain.

## Expected product behavior

- Cancel or dismiss staged edits without a write.
- Apply a changed volume/program for the current session only.
- Set as Default opens a confirmation with both volume and program; Cancel returns to the editor without writing.
- Save defaults performs the reviewed operation and reports success only after the requested pair, original microphone state, companion state and applicable read-only EQ comparison are verified on fresh sessions.
- No native EQ Save, UAC changes, EQ-band writes, helper program selection or replay is used for the defaults action.
- Keep the exact beta installed for owner manual testing after the automated session. Wait for the owner to identify this candidate as passing and direct publication; that instruction activates the existing merge/publish pre-authorization.
