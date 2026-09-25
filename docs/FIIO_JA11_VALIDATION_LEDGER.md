# FiiO JA11 validation ledger

This is the append-only evidence ledger for the exact FiiO JA11 target. Do not reuse the
EW300 ledger for JA11. A software, artifact, emulator, or owner-reported UI result does not
qualify JA11 hardware unless the exact physical gate is satisfied.

## Current disposition

JA11 is **implemented but hardware-validation pending**. The latest owner-provided result is a
negative Flash verification observation. It is not attributed to a source SHA or signed APK
because the screenshot does not contain that provenance.

## Deterministic software value trace for the supplied Jaytiss record

This is a software/artifact trace, not a physical packet trace:

1. The retained AFUL Explorer Jaytiss record declares source preamp `-3.9 dB` at
   `catalog/discovery/aful_explorer_community_curated.json:52`.
2. The JA11 capability profile leaves `preampStepDb = null`, so the finite-hardware optimizer
   preserves that source preamp rather than applying the JM12 half-dB quantizer.
3. The intended JA11 device-domain value is therefore `-3.9 dB`.
4. The current codec computes `round(-3.9 × 2560) = -9984`, represented as unsigned 16-bit
   `0xD900`, with little-endian payload bytes `00 D9`.
5. Decoding the same raw value returns `-9984 / 2560 = -3.9 dB`; the flasher compares the
   readback to the wire-quantized expected value with `0.001 dB` tolerance.
6. The supplied connected-device frame displays `-3.80 dB`, a `0.10 dB` difference from this
   intended value, but the raw device response is not available. The evidence therefore does
   not distinguish a device transformation, stale same-command response, or protocol-parser
   interpretation error.

## Evidence records

| ID | Exact source / artifact | Result and claim | Restoration / limits |
| --- | --- | --- | --- |
| J001 | Owner screenshot: `/Users/stephenweeks/Library/CloudStorage/GoogleDrive-weekssa@gmail.com/My Drive/OPRA UAPP Presets/EQ Library Testing/Screenshot (Sep 25, 2026 9:59:19 AM)`; PNG SHA-256 `93efb6135fde53f60b285e1f16681f01ff72cc8f7e46a1934e016e280eed8b6f`; 1080×2424; APK/source/checksum/signer not supplied | **FAILURE OBSERVED.** Connected FiiO JA11; Jaytiss profile; displayed `Optimized · 9 → 5 bands · full-response fit`; Flash ended with `JA11 global EQ gain readback did not match the intended value.` Evidence category: owner-reported physical UI artifact. | Exact intended/actual gain, raw `0x17` packets, firmware, PID/UAC, transaction phase, Save count, device identity, and restoration status are unknown. Do not repeat the mutation from this record. Not a qualification result. |
| J001a | Owner follow-up observation from the same investigation: after unplug/replug, the device reportedly returned to `0` | **SUPPLEMENTAL NEGATIVE OBSERVATION.** This is not accompanied by a readback report, exact candidate provenance, raw bytes, or a baseline, so it cannot distinguish failed persistence from a UI/device-state interpretation. | Do not treat as proof of a codec defect or persistence contract. Combine with J001 only as a reason to require a complete baseline, Save-stage trace, and post-power-cycle readback in the next authorized session. |
| J001b | Earlier owner screenshot `/Users/stephenweeks/Library/CloudStorage/GoogleDrive-weekssa@gmail.com/My Drive/OPRA UAPP Presets/EQ Library Testing/Screenshot (Sep 25, 2026 2:17:20 AM)`; PNG SHA-256 `6a4997ecbdd916ba487f4e6f2313eacb14221a218ca0ac86ecca42a9be0b48f2`; 1080×2424 | **FAILURE OBSERVED.** Same connected JA11/Jaytiss/9→5 screen shows the same global-gain verification failure. | No exact candidate, raw packets, firmware, PID/UAC, or restoration state. This confirms recurrence in the supplied UI sequence but not the root cause. |
| J001c | Earlier owner screenshot `/Users/stephenweeks/Library/CloudStorage/GoogleDrive-weekssa@gmail.com/My Drive/OPRA UAPP Presets/EQ Library Testing/Screenshot (Sep 25, 2026 2:17:45 AM)`; PNG SHA-256 `4047080df428ac1ee228b25fe7be3fe9126d700f53c343bfde04e93734157e98`; 1080×2424 | **VOLATILE STATE OBSERVED.** My DAC shows the five target bands and `Global EQ gain -3.80 dB` while JA11 is connected. The displayed target bands are consistent with the optimized Jaytiss 9→5 representation; the displayed gain differs by `0.10 dB` from the source preamp `-3.90 dB` retained by the current JA11 plan. | UI evidence is not raw `0x17` evidence and does not prove which source SHA/APK produced it. Strongly narrows the failure to gain representation/device response or stale readback; does not authorize a codec or tolerance change. |
| J001d | Earlier owner screenshot `/Users/stephenweeks/Library/CloudStorage/GoogleDrive-weekssa@gmail.com/My Drive/OPRA UAPP Presets/EQ Library Testing/Screenshot (Sep 25, 2026 2:18:03 AM)`; PNG SHA-256 `9b1eb5f802151f4887b06ffd8ad88e5293447fbadc818ce1059a228fc30d079a`; 1080×2424 | **POST-RECONNECT NEGATIVE OBSERVATION.** My DAC shows User 1 with all five bands flat and `Global EQ gain 0.00 dB`. | Supports the owner's persistence-loss report, but no exact power-cycle duration, raw final readback, firmware/PID, or candidate provenance is available. Not a qualification result. |
| J001e | Earlier owner screenshot `/Users/stephenweeks/Library/CloudStorage/GoogleDrive-weekssa@gmail.com/My Drive/OPRA UAPP Presets/EQ Library Testing/Screenshot (Sep 25, 2026 2:18:16 AM)`; PNG SHA-256 `2408965a894b103641dce69d2181aba4f7eaccdcf46e7824f86c456040d18f44`; 1080×2424 | **FAILURE OBSERVED AGAIN.** Same JA11/Jaytiss screen again reports the global-gain verification failure. | No exact candidate, raw packets, firmware, PID/UAC, or restoration state. Recurrence does not distinguish an incorrect wire value from a device-side transform or stale same-command response. |
| J002 | Software candidate `f3765b03a3d8517880956390c9c4eca3b4157222`; signed APK `EQ-Library-v0.7.0-beta-f3765b0.apk`; APK SHA-256 `928f9a68e0abaeb01d16a1aa1d227432b39c691f191214791a5540cd1c3f3037`; signer `65c1c1256dae3c49e3548f334c91f0ba991969e9be9e0b223ba4e253d2114747` | **SOFTWARE / ARTIFACT EVIDENCE ONLY.** JA11 codec, pacing, command filtering, fail-closed mismatch handling, signed candidate, emulator install/cold launch, and documented software gates are recorded in the release audit. | No physical JA11 qualification is inferred. J001 is not proven to have used this candidate. |
| J003 | Uncommitted working-tree correction after J001; no source SHA, signed APK, checksum, or CI result yet | **SECONDARY SOFTWARE CORRECTION PREPARED.** JA11 Save is now a transport lifecycle operation that waits for the optional FiiO-documented power-cycle/re-enumeration boundary before final readback. JA11 reads and ordinary writes also reject a detach/session-generation change spanning the exchange. Focused Save-boundary, final-mismatch, Jaytiss `-3.9 dB`, and optimizer tests were added. This hardens lifecycle safety but is not claimed as the cause of the observed pre-persistence gain mismatch. | Gradle/Android/Kotlin execution is NOT RUN locally because the required toolchain is absent. No physical test, signed candidate, merge, publication, or support claim follows. |

| J004 | Draft PR [#41](https://github.com/weekssa/OPRA-EQ-for-UAPP/pull/41), exact head `b32c52a82a46899efd115efd8544deeb16b9eb4c`; Android CI run `36161662881`; CodeQL `36161662907`; priority coverage `36161662950`; catalog currentness `36161662958`; CI artifact `EQ-Library-beta-debug-apk` ID `10876492157` | **AUTOMATED SOFTWARE GATE PASS.** The Save/reconnect boundary, session-generation guards, deterministic JA11 codec/optimizer/flasher tests, lint, debug/release assembly, R8/minified verification, connected UI tests, API-26 cold-install smoke, CodeQL, priority coverage, and catalog-currentness all passed on this exact head. | The CI APK is unsigned. No signed APK checksum/signer tuple, physical JA11 trace, restoration result, merge, publication, or hardware-support claim exists. JA11 remains hardware-validation pending. |
| J005 | Merged source `609911e2e51a254fc6f45b87fbdf4106c0049740`; exact immutable APK [`EQ-Library-v0.7.0-beta-609911e.apk`](https://raw.githubusercontent.com/weekssa/OPRA-EQ-for-UAPP/mobile-test-apk/candidates/EQ-Library-v0.7.0-beta-609911e.apk); APK SHA-256 `583ff7014fc3c0977b6679cd8bf56d3a4f615411a629fa6014bab088ece082ef`; signer certificate SHA-256 `65c1c1256dae3c49e3548f334c91f0ba991969e9be9e0b223ba4e253d2114747`; signed-beta [run #1362](https://github.com/weekssa/OPRA-EQ-for-UAPP/actions/runs/36165218849); artifact ID `10877122640`; artifact ZIP SHA-256 `1b124c63cb799a5a069384d48a9477e32078167a509ccd8872b69693880988cd` | **SIGNED SOFTWARE CANDIDATE READY.** Candidate manifest records target `ja11`, package `com.weekssa.opraeqforuapp`, version `0.7.0`/code `7`, R8 mapping SHA-256 `9f9d0e28271b4cfefdc9e71c6416061bd4a99e38341ab72290b159825de6e6ec`, v2/v3 signature verification, one RSA-4096 signer, and the JA11 capability profile. Android CI `36163197805`, CodeQL `36163197784`, priority coverage `36163197775`, catalog currentness `36163197794`, signed emulator install/cold launch, and artifact integrity all passed. | Software/artifact/emulator evidence only. No physical JA11 write, readback, power-cycle persistence result, or restoration result is claimed. Hardware validation remains pending; this record authorizes only the bounded owner session described by the checklist. |

## Required fields for the next physical record

Before any authorized mutation, record the exact source SHA, signed APK SHA-256, signer, app
version/code, JA11 firmware response, VID/PID/UAC mode, sanitized device fingerprint, source
profile/revision, complete five-band/global-gain baseline, and original-state restoration plan.
The exported operation trace must include the intended dB and raw gain, outgoing and returned
`0x17` packets, command timestamps/order, active program, Save count, session generation, and
whether the failure was pre-Save or post-Save.

## Gate rule

Do not record `PASS`, remove Hardware validation pending, publish JA11 support, or repeat an
uncertain mutation until the exact candidate, complete baseline, raw transaction evidence, and
restoration result are all present and reviewed.
