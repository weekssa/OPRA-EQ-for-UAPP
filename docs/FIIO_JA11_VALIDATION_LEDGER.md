# FiiO JA11 validation ledger

This is the append-only evidence ledger for the exact FiiO JA11 target. Do not reuse the
EW300 ledger for JA11. A software, artifact, emulator, or owner-reported UI result does not
qualify JA11 hardware unless the exact physical gate is satisfied.

## 2026-09-28 owner-authorized release disposition

The owner authorized final software/release closeout. No new JA11 operation report or exact-current
candidate physical trace was supplied in this closeout turn, so the evidence boundary remains
unchanged: the User 1 editor/apply path is software-verified, J016/J017 remain valid physical
records for the earlier exact signed candidate, and explicit power-cycle retention/full
qualification are not claimed for the combined `e1ab5fa` candidate. Publication approval does not
rewrite or broaden this append-only hardware evidence.

## Current disposition

As of 2026-10-09, the clean implementation branch `codex/ja11-v0.8.1-minimal` is based on `9b9a1f610025b3d6233d4ff6f0c2309a668577cc`, with frozen production source `9493cf030acb440f92e547fc667f6a5399616045`. G1-G7 pass on that source: 761 JVM tests with zero failures/errors/skips; lint; debug, release, diagnostic, and Android-test assembly; R8 mapping verification; 64/64 isolated API 35 instrumentation; and fake-ADB helper install fixtures. The required ten-question independent review and its KDoc-only supplemental review pass. The exact diagnostic APK has SHA-256 `98ee2eb876ec0d6d5bcfcebebdf55e1c77f2f7edfbecfd59322a94e5feea48ae`, package `com.weekssa.opraeqforuapp.ja11diag`, version `0.8.1-ja11diag` / code `12`, and signer certificate SHA-256 `73aa7581c8dc7dcc8ccea7586771119a98f9a74d7d8cf23716e09c557c9f6b41`; host verification confirms the embedded source SHA, v2 signature, and alignment, and the APK clean-installed and cold-launched on the disposable emulator. G9 exact-head CI and the PR-head portion of G10 remain pending. G11 scope review passes. Only `emulator-5554` on isolated ADB port 5039 was used; it is shut down. No Pixel, JA11, or physical-device command has been issued on this branch.

PR #80 remains open/draft/unmerged at historical head `5f471dfdce93c35f93a4632ca76ff729fcb58a6b`; its evidence is preserved and does not qualify the clean candidate. J025 and J026 remain unchanged, candidate-specific physical records. J026 records one Save followed by detach about 677 ms later during final readback, no replay, a fresh snapshot matching the original baseline, a failed Test C for that exact candidate, and release of the Pixel. The latest verified prior session had original-state restoration, but current live hardware state is unknown.

Model D remains current: serial is optional continuity evidence; require one supported JA11 candidate, current permissioned claimed session/generation, expected detach and fresh replacement for restart verification, and authoritative readback. Compare serial only if both sessions provide usable values; reject mismatch. Without matching serial evidence, report state verified on the sole returning supported JA11, not proof of the same physical unit. The `0x17` codec correction remains established by the cited official FiiO Control and historical physical records. Volume/preset power-cycle persistence remains unverified.

## 2026-10-09 clean v0.8.1 source and host artifact preflight — no physical evidence

The production source commit is `9493cf030acb440f92e547fc667f6a5399616045`, based on current `origin/main` `9b9a1f610025b3d6233d4ff6f0c2309a668577cc`. The separately packaged diagnostic APK at `app/build/outputs/apk/ja11Diagnostic/app-ja11Diagnostic.apk` has SHA-256 `98ee2eb876ec0d6d5bcfcebebdf55e1c77f2f7edfbecfd59322a94e5feea48ae`, package `com.weekssa.opraeqforuapp.ja11diag`, version `0.8.1-ja11diag` / versionCode `12`, and Android debug signer certificate SHA-256 `73aa7581c8dc7dcc8ccea7586771119a98f9a74d7d8cf23716e09c557c9f6b41`. `apkanalyzer`, `apksigner`, and `zipalign` verified package/version, one v2 signer, alignment, and the exact source SHA embedded in DEX.

After the final source comment correction, G1-G7 were reverified. Focused regressions, 761 JVM tests, lint, four APK assemblies, R8 mapping, 64/64 API 35 instrumentation, and fake-ADB helper fixtures all passed. The independent ten-question review and supplemental review pass; the only initial finding was the corrected KDoc order. The exact diagnostic APK was clean-installed and cold-launched on AVD `ja11-v081-api35-clean-20261008` as `emulator-5554` using isolated ADB port 5039, then the emulator was shut down. No Pixel, JA11, physical ADB, or hardware command was used.

The production scope audit maps all seven changed production files to approved corrections A-E or candidate identity, keeps shared defaults unchanged for other DACs, and confirms no discarded reconnect architecture or diagnostic-only logging was transplanted. Required CI on the final draft-PR head and that PR head's addition to the complete tuple remain pending. This host-only record does not qualify JA11 hardware or authorize a phone operation.

## 2026-10-09 exact-head CI and host-only candidate preflight

This software/preflight record applies to production source `3d7bc1d91e6c39327477d1341bde80e8a37bfbd4`, PR #80 head `5f471dfdce93c35f93a4632ca76ff729fcb58a6b`, diagnostic APK SHA-256 `3b74672a587daeaaf8f562634c5dcecea073f7ad6e01df736f874ee448fc6261`, package `com.weekssa.opraeqforuapp.ja11diag`, version `0.8.0-ja11diag` / code `11`, and signer certificate SHA-256 `73aa7581c8dc7dcc8ccea7586771119a98f9a74d7d8cf23716e09c557c9f6b41`.

All eight required checks passed on the exact PR head: Android CI run `37994098421` (build `114035550183`, emulator UI `114035550009`, API-26 smoke `114035550219`); CodeQL `114037802407`; Analyze Kotlin `114035549876`; Catalog currentness `114035549670`; Priority community `114035549526`; and dependency submission `114035536001`. The corresponding run pages are [Android CI](https://github.com/weekssa/OPRA-EQ-for-UAPP/actions/runs/37994098421), [CodeQL](https://github.com/weekssa/OPRA-EQ-for-UAPP/actions/runs/37994098423), [Catalog currentness](https://github.com/weekssa/OPRA-EQ-for-UAPP/actions/runs/37994098445), [Priority community](https://github.com/weekssa/OPRA-EQ-for-UAPP/actions/runs/37994098426), and [dependency submission](https://github.com/weekssa/OPRA-EQ-for-UAPP/actions/runs/37994092937).

Host verification reconfirmed the candidate APK hash, package/version, v2 signature, single debug signer, and exact source SHA string in its DEX. The pinned rollback APK has SHA-256 `ee0fe4fbfaae7b3f959d4122f0c21c128dffdf21d47586376ee383e2534ceb0d`, matching the helper's prior-APK pin; package/version, v2 signature, and signer also match. No ADB, phone, emulator, or JA11 command was issued for this preflight. Physical acceptance remains pending fresh owner confirmation; the prior `92c11fb0` failure and restoration are unchanged.

## 2026-10-09 owner-approved Model D — initial implementation checkpoint (superseded status)

This paragraph records the first pending state when the implementation was still uncommitted. The
latest measured Model D candidate and its current gates are recorded at the end of this ledger.

Model D requires one exact supported JA11 candidate for initial selection and restart continuation,
valid uniquely selected HID interface/endpoints, current permission, a fresh claimed connection and
generation, accepted operation-bound writes, expected detach for resets, fresh authoritative
readback, and no automatic replay of uncertain writes. A usable serial is optional; compare it only
when both sessions expose a usable value and reject mismatch. When serial is absent, the only
permitted success claim is state verified on the sole returning supported JA11, not the same
physical unit. Unsolicited reconnect without a pending operation starts a new session and baseline.

At ledger update time the production changes and regressions are still uncommitted. Focused/full
tests, API 35 instrumentation, independent lifecycle review, all exact-head PR checks, exact
diagnostic APK provenance, and host-only candidate preflight remain pending. No Model D physical
test has occurred. These fields will be replaced with measured hashes/results only after the exact
candidate is frozen and each gate is independently verified.

## Deterministic software value trace for the supplied Jaytiss record

This is a software/artifact trace, not a physical packet trace:

1. The retained AFUL Explorer Jaytiss record declares source preamp `-3.9 dB` at
   `catalog/discovery/aful_explorer_community_curated.json:52`.
2. The JA11 capability profile leaves `preampStepDb = null`, so the finite-hardware optimizer
   preserves that source preamp rather than applying the JM12 half-dB quantizer.
3. The intended JA11 device-domain value is therefore `-3.9 dB`.
4. The old Android codec computed `round(-3.9 × 2560) = -9984`, represented as `0xD900`, with
   little-endian payload bytes `00 D9`.
5. The official FiiO Control JA11 model instead computes the signed tenths-dB value
   `int(-3.9 × 10) = -39`, represented as `0xFFD9`, with high-byte-first payload bytes `FF D9`.
6. J012 records the old app writing `00 D9` and the unchanged-session JA11 returning `FF D9`.
   Decoded under the official JA11 domain, that response is exactly `-3.9 dB`; the prior
   `-3.800390625 dB` result was an Android decode error, not a device quantization result.
7. The five target bands read back exactly, and no detach, reconnect, permission request, or Save
   occurred. This proves the global-gain codec defect. J017 supplies the separate observed
   reconnect/restoration evidence, while explicit power-cycle retention remains pending.

## Evidence records

| ID | Exact source / artifact | Result and claim | Restoration / limits |
| --- | --- | --- | --- |
| J017 | Owner-exported readable Flash reports `(3)` and `(4)`: `/Users/stephenweeks/Library/CloudStorage/GoogleDrive-weekssa@gmail.com/My Drive/OPRA UAPP Presets/EQ Library Testing/FiiO JA11 operation report (3)` and `(4)` (SHA-256 `f23b8c25720d87dbbdf806c48ae09ae857fc80822d1c60ddca66bcbb8c53d6eb` for each); owner-exported Flash JSON reports `(3)` and `(4)`: `/Users/stephenweeks/Library/CloudStorage/GoogleDrive-weekssa@gmail.com/My Drive/OPRA UAPP Presets/EQ Library Testing/FiiO JA11 operation report JSON (3)` and `(4)` (SHA-256 `f9feebad2dd516a24908937638aa4b1c4f877cf6f8e5ccc146d635179dc80de7` for each); owner-exported Reset report `(5)`: `/Users/stephenweeks/Library/CloudStorage/GoogleDrive-weekssa@gmail.com/My Drive/OPRA UAPP Presets/EQ Library Testing/FiiO JA11 operation report (5)` (SHA-256 `ee6b8fc96b634b80872ca0aa6b23d4ab22d42a34dc99e4ccde6ad17d63482b6f`) and JSON `(5)`: `/Users/stephenweeks/Library/CloudStorage/GoogleDrive-weekssa@gmail.com/My Drive/OPRA UAPP Presets/EQ Library Testing/FiiO JA11 operation report JSON (5)` (SHA-256 `1b5cc11897efb663388e0962d0da12e816ed73837d53626adf9bc24b99cd798`); exact source `c886fdbb2ae326e562dc110b2b779cb075869798`; APK `EQ-Library-v0.7.0-beta-c886fdb.apk`; APK SHA-256 `c390bbd429ce4101ce7fad3aa3820990da0e7ffec7a4f688e5eafe4eb11f6341`; signer certificate SHA-256 `65c1c1256dae3c49e3548f334c91f0ba991969e9be9e0b223ba4e253d2114747`; operation IDs `fc63479b-4d26-44b4-b90f-77a8db2c9c4f` and `402a004a-0217-4700-9b31-9b252f6c8aeb`; firmware `2.20`; VID/PID `0x2972:0x0102`; interface `3` | **OWNER PHYSICAL PASS — FLASH, ONE SAVE, FINAL READBACK, OBSERVED RECONNECT HISTORY, AND RESET RESTORATION.** Flash wrote/read `FF D9` as `-3.9 dB`, optimized `9 → 5` bands with response fit, sent Apply, sent exactly one Save, passed final-readback verification, and ended with known state. The later Reset report began at session/detach generation `3/2` versus Flash `1/0`; its baseline was the flashed target and its final raw readbacks matched the Flash report’s original flat baseline. | Reports `(3)`/`(4)` are duplicate exports, not independent Flash operations. The generation change proves an observed detach/reconnect history, but no report explicitly identifies power removal or duration. This proves the observed reconnect/restoration path and exact captured flat-state restoration for this session; explicit power-cycle retention, arbitrary-state restoration, complete qualification, public support, and final release remain pending/owner-controlled. |
| J015 | `main` merge `c886fdbb2ae326e562dc110b2b779cb075869798`; immutable APK [`EQ-Library-v0.7.0-beta-c886fdb.apk`](https://raw.githubusercontent.com/weekssa/OPRA-EQ-for-UAPP/mobile-test-apk/candidates/EQ-Library-v0.7.0-beta-c886fdb.apk); APK SHA-256 `c390bbd429ce4101ce7fad3aa3820990da0e7ffec7a4f688e5eafe4eb11f6341`; signer certificate SHA-256 `65c1c1256dae3c49e3548f334c91f0ba991969e9be9e0b223ba4e253d2114747`; signed-beta [run #1365](https://github.com/weekssa/OPRA-EQ-for-UAPP/actions/runs/36223017450); uploaded artifact ID `10899662688`; emulator diagnostics artifact ID `10899543957` | **SOFTWARE / ARTIFACT PASS; OWNER HARDWARE GATE READY.** Beta unit/lint/release build, R8 mapping verification, unsigned identity, APK signing and signer match, zipalign, disposable emulator install/cold launch, diagnostics upload, and immutable publication passed. The corrected candidate is ready for one bounded physical JA11 Flash/readback/persistence/restoration session. | No physical Flash was performed by the agent. JA11 remains hardware-validation pending; no public support or final-release claim is authorized. Verify APK checksum and signer before use; export readable and JSON reports and stop on any mismatch or uncertain restoration. |
| J016 | Owner-exported readable report `/Users/stephenweeks/Library/CloudStorage/GoogleDrive-weekssa@gmail.com/My Drive/OPRA UAPP Presets/EQ Library Testing/FiiO JA11 operation report (2)` (SHA-256 `42de6d72e524bb83eb8581ae8af153a8c017012bb4f49a1d753cc7ce1056a3a`) and parser-valid technical report `/Users/stephenweeks/Library/CloudStorage/GoogleDrive-weekssa@gmail.com/My Drive/OPRA UAPP Presets/EQ Library Testing/FiiO JA11 operation report JSON (2)` (SHA-256 `6e2e88af5436713555222aebf18fbd2447a45aed7d978328203233bb993d18f0`); exact source `c886fdbb2ae326e562dc110b2b779cb075869798`; operation `587ebf2a-b9e9-4758-ba0a-6ddf48bef2d0`; app `0.7.0`; signer verified; VID/PID `0x2972:0x0102`; firmware `2.20`; interface `3` | **OWNER PHYSICAL PASS — SAME-SESSION FLASH, ONE SAVE, AND FINAL READBACK VERIFIED.** Canonical/selected/quantized/readback global gain was `-3.9 dB`; corrected `0x17` write/readback used `FF D9`; source bands `9` were optimized to `5`; response fit was used; Apply preceded volatile readback; Save count was exactly `1`; comparison phase was `FINAL_READBACK`; outcome was `Success`; state was known; all `31` transport events succeeded. | Session/detach generations stayed `1/0`, so no USB detach or reconnect was observed. The report proves the corrected same-session transaction and Save/final-readback path, but not unplug/reconnect persistence, power-cycle retention, or original-state restoration. Do not label JA11 fully hardware-qualified or make a public support claim from J016 alone. A UI-only follow-up that leaves the transaction path unchanged does not require another physical mutation; J016 remains tied to source `c886fdb…`. |
| J025 | Private session `/private/tmp/ja11-v0.8.1-acceptance-61695803/owner-phone-session-20261009T161940Z-model-d-resume/`; source `1d19067c9150aae1b09e01fafb8af3647bf65f71`; diagnostic APK SHA-256 `23adf9f4955b056f110562362717c706767b7e3ecc50225c07439a3ff5c23711`; package `.ja11diag`, version `0.8.0-ja11diag` / 11; Android Debug signer SHA-256 `73aa7581c8dc7dcc8ccea7586771119a98f9a74d7d8cf23716e09c557c9f6b41`; Pixel 9 / JA11 firmware 2.20, VID/PID `0x2972:0x0102` (UAC transition PID `0x0101`); no serial value retained in this record | **PHYSICAL FAIL — FLASH FROM OFF, PRE-SAVE.** The Model D single-candidate/current-session gate passed. Mic Off-to-On and UAC 2.0-to-1.0 passed. UAC restore's verifier expired before permission was granted; a later fresh read verified UAC 2.0. One Flash attempt failed band 1 readback before Save because source wrote bands/gain before selecting User 1. | Original Off program, Mic On, UAC 2.0, volume 30, 384 kHz, gain `-3.7 dB`, and flat five-band state were restored/read back; temporary profile removed, prior APK restored and verified, evidence captured, and Pixel released. Flash excerpt SHA-256 `6fbe917ca29637d057598c7dc110b9367dd31ed938f68171c3678ee175be3f9a`; final baseline capture SHA-256 `f1435d61a38061fcdbba3bfd0fd22a2f291d98c3f47fe327477f0a4e192e9370`. This exact candidate is superseded. |
| J026 | Private session `/private/tmp/ja11-v0.8.1-acceptance-92c11fb0/owner-phone-session-20261009T183705Z-continued/`; source `92c11fb0e41ae11b118b2e7bb105234d6606dbdb`; diagnostic APK SHA-256 `ce3f417f20c275fd4d535cf5e70f658d8430af8fdd3f87ea950705fbfd574637`; package `.ja11diag`, version `0.8.0-ja11diag` / 11; Android Debug signer SHA-256 `73aa7581c8dc7dcc8ccea7586771119a98f9a74d7d8cf23716e09c557c9f6b41`; Pixel 9 / JA11 firmware 2.20, VID/PID `0x2972:0x0102`, HID interface 3; private diagnostic events and screenshots remain outside Git | **PHYSICAL FAIL — LATE SAVE / FINAL READBACK.** The read-only Model D gate, Mic Off-to-On, and UAC 2.0-to-1.0-to-2.0 passed. Flash from Off selected/read back User 1 before data writes, wrote five bands and gain, passed Apply/pre-Save verification, and accepted exactly one Save. The JA11 detached 677 ms later during band 4 readback. No write, Apply, or Save was replayed; Test C failed and Test D was not run. | USB permission was already granted. A new sole supported session opened, and the full snapshot matched the original User 1 baseline: Mic On, UAC 2.0, volume 60, 384 kHz, gain `-3.7 dB`, original five bands. Pixel was released. Event-log SHA-256 `121dbc000367ad597217cb78efd1caccaf78804bfe35f25c1a97a33d8ffc4832`; final snapshot SHA-256 `62a3bce80167c8baf89165ad744409a8103fba9f8f11878c53f8ef13e62fb23f`. |
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
| J006 | Exact signed candidate `EQ-Library-v0.7.0-beta-609911e.apk`, source `609911e2e51a254fc6f45b87fbdf4106c0049740`, installed over the prior build; owner video `/Users/stephenweeks/Library/CloudStorage/GoogleDrive-weekssa@gmail.com/My Drive/OPRA UAPP Presets/EQ Library Testing/screen-20260925-130921-1790359660539.mp4` SHA-256 `afa7bd92db069bec2d543d90d98fc4d4639810a0c062954607f4d73191b2d2d7`; owner screenshot `/Users/stephenweeks/Library/CloudStorage/GoogleDrive-weekssa@gmail.com/My Drive/OPRA UAPP Presets/EQ Library Testing/Screenshot (Sep 25, 2026 12:40:41 PM)` SHA-256 `52ed5324861b2a51f154332258d9e4729ae8fc31677a0e2a49be3ea07653df3c` | **PHYSICAL FAILURE REPRODUCED ON EXACT SIGNED CANDIDATE.** Owner reports that every EQ and Flash path failed immediately with the same global-gain mismatch; the Save/reconnect correction was exercised; the target graph appeared, but no visible progress/restart/disconnect/reconnect/Save/successful EQ was observed. The first video includes an intentional Reset-to-EQ action; later unplug/reconnect was only to demonstrate that the state did not persist. This is the strongest current physical negative result and rules out treating the prior provenance gap as the explanation. | No raw request/response packets, firmware response, PID/UAC, sanitized fingerprint, exact timestamps, session generations, or exported diagnostic trace were attached. Do not repeat Flash or Reset until a diagnostic candidate captures those fields. JA11 remains hardware-validation pending and no support claim follows. |
| J007 | Diagnostic correction implementation `054298b866fad4ca97cb649790af54ccc6a4cfba`; latest handoff head `1b56035ff9dcbe9a49b3a44432e7ff9e49fc3010` on branch `codex/ja11-signed-provenance`, pushed to the project remote. Adds bounded JA11 trace/reporting, complete five-band/program/global-gain preflight, phase-aware comparison metadata, and regression fixtures. | **DIAGNOSTIC SOFTWARE EVIDENCE ONLY.** The correction preserves the independently corroborated `0x17` codec, `2560` scale, tolerance, fail-closed behavior, and Save lifecycle. Local Android tests are **NOT RUN** because the host has no Android SDK location. Final-head Android CI `36178118593`, CodeQL `36178118501`, priority-community `36178118603`, catalog-currentness `36178118617`, and dependency-submission `36178111903` all **PASS**. CI produced unsigned debug artifact `EQ-Library-beta-debug-apk`, artifact ID `10883860070`; no signed artifact exists. | No physical mutation, signed APK, checksum, signer tuple, or hardware-support claim exists for this head. Do not install or Flash from this source until an exact signed candidate is produced and the owner handoff is refreshed. |
| J007 | Diagnostic correction implementation `054298b866fad4ca97cb649790af54ccc6a4cfba`; latest handoff head `1b56035ff9dcbe9a49b3a44432e7ff9e49fc3010` on branch `codex/ja11-signed-provenance`, pushed to the project remote. Adds bounded JA11 trace/reporting, complete five-band/program/global-gain preflight, phase-aware comparison metadata, and regression fixtures. | **DIAGNOSTIC SOFTWARE EVIDENCE ONLY.** The correction preserves the independently corroborated `0x17` codec, `2560` scale, tolerance, fail-closed behavior, and Save lifecycle. Local Android tests are **NOT RUN** because the host has no Android SDK location. Final-head Android CI `36178118593`, CodeQL `36178118501`, priority-community `36178118603`, catalog-currentness `36178118617`, and dependency-submission `36178111903` all **PASS**. CI produced unsigned debug artifact `EQ-Library-beta-debug-apk`, artifact ID `10883860070`; no signed artifact exists. | No physical mutation, signed APK, checksum, signer tuple, or hardware-support claim exists for this head. Do not install or Flash from this source until an exact signed candidate is produced and the owner handoff is refreshed. |
| J008 | Merged diagnostic source `af8c68c35d320223a13c635fac69c0f2ebacdb3f` (PR [#42](https://github.com/weekssa/OPRA-EQ-for-UAPP/pull/42)); immutable APK [`EQ-Library-v0.7.0-beta-af8c68c.apk`](https://raw.githubusercontent.com/weekssa/OPRA-EQ-for-UAPP/mobile-test-apk/candidates/EQ-Library-v0.7.0-beta-af8c68c.apk); APK SHA-256 `3b442cbab3cf8be59a9b8e4ddd0d7028e93f8c4067d94dbb833a5bbb60b1d37e`; signer certificate SHA-256 `65c1c1256dae3c49e3548f334c91f0ba991969e9be9e0b223ba4e253d2114747`; signed-beta [run #1363](https://github.com/weekssa/OPRA-EQ-for-UAPP/actions/runs/36180975485); signed artifact ID `10884371877`, ZIP SHA-256 `77477ad6bd52e9b114cf18e949368424d8d5c1dbc85246679c9c5fd561d5b44d`; R8 mapping SHA-256 `71036cf05464e6b84f07165e75c17c5a5cd5517a843bd1a8efb0bb49add0b374` | **EXACT SIGNED DIAGNOSTIC CANDIDATE READY.** Package `com.weekssa.opraeqforuapp`, version `0.7.0`/code `7`, RSA-4096 signer, APK Signature Scheme v2/v3, zipalign, build/test/lint/release/R8, signed emulator install/cold launch, diagnostics, and immutable candidate publication all passed on this exact source. The candidate includes bounded readable/JSON JA11 transaction reporting and complete preflight baseline capture; it does not claim a protocol root-cause fix or physical qualification. | No physical JA11 mutation, raw packet report, restoration result, final release, or public support claim exists for J008. The single next action is the owner’s bounded hardware session in the checklist. Do not reuse J006 or any moving APK. |
| J009 | Owner-exported readable report `/Users/stephenweeks/Library/CloudStorage/GoogleDrive-weekssa@gmail.com/My Drive/OPRA UAPP Presets/EQ Library Testing/FiiO JA11 operation report` (SHA-256 `ce588c7b2bc0047e7edca12277ddf86564fafeab9a39cb11bf3abb305c5f653c`), technical report `/Users/stephenweeks/Library/CloudStorage/GoogleDrive-weekssa@gmail.com/My Drive/OPRA UAPP Presets/EQ Library Testing/FiiO JA11 operation report JSON` (SHA-256 `0a2c024941049b2ec3ed78e00d0180ba6461a70ee2e6596518999361606346d1`), screenshot `/Users/stephenweeks/Library/CloudStorage/GoogleDrive-weekssa@gmail.com/My Drive/OPRA UAPP Presets/EQ Library Testing/Screenshot (Sep 25, 2026 4:15:34 PM)` (SHA-256 `ed308e4ddb9df7c922a9820e866035d95dc89a4784eb6b838c3bdad225c16442`); exact source `af8c68c35d320223a13c635fac69c0f2ebacdb3f`; APK SHA-256 `3b442cbab3cf8be59a9b8e4ddd0d7028e93f8c4067d94dbb833a5bbb60b1d37e` | **PHYSICAL FAILURE DIAGNOSED TO A STABLE PRE-SAVE GLOBAL-GAIN MISMATCH.** The report records source/target/wire gain `-3.9 dB`, write `0x17` bytes with raw `0xD900` (`00 D9`), Apply, exact readback of all five bands, and a same-session `0x17` response with raw `0xD9FF` (`FF D9`) decoded as `-3.800390625 dB`. Delta is 255 raw units (`0.099609375 dB`), beyond the `0.001 dB` tolerance. Session generation and detach generation remain `1/0`; permission requests are `0`; `saveCommandCount=0`; outcome is `VerificationFailed`. This falsifies a detach/session replacement explanation for this attempt and isolates the failure to JA11 global-gain semantics or device-side quantization not yet independently characterized. | The readable report and screenshot are valid evidence. The JSON export is **malformed** because every nested object begins with `{,`; its values are retained as evidence but it is not parser-valid. No Save or persistence result, post-operation restoration result, firmware version, or public qualification claim is established. Do not repeat Flash or Reset until the export fix is built and the remaining protocol question is resolved. |

| J010 | Draft PR [#44](https://github.com/weekssa/OPRA-EQ-for-UAPP/pull/44), exact head `f17a17270aa8d10bc7a34dc1534e90d198b960da`; Android CI run [36195006123](https://github.com/weekssa/OPRA-EQ-for-UAPP/actions/runs/36195006123), CodeQL [36195006257](https://github.com/weekssa/OPRA-EQ-for-UAPP/actions/runs/36195006257), catalog-currentness [36195006185](https://github.com/weekssa/OPRA-EQ-for-UAPP/actions/runs/36195006185), priority-community [36195006136](https://github.com/weekssa/OPRA-EQ-for-UAPP/actions/runs/36195006136), dynamic Gradle [36195001416](https://github.com/weekssa/OPRA-EQ-for-UAPP/actions/runs/36195001416) | **DIAGNOSTIC EVIDENCE ENHANCEMENT PASSED.** The optional JA11 firmware read (`0x0B`) is now captured in the operation trace and both readable/JSON reports before Flash/Reset writes. The change does not alter codec math, transaction ordering, tolerance, retry policy, Save behavior, or qualification status. The exact head passed Android unit/lint/debug/release/R8, emulator UI, min-API smoke, CodeQL analysis/scanning, catalog-currentness, priority-community, and dynamic Gradle checks. | Software/artifact evidence only. No physical mutation, signed APK, firmware value, restoration result, protocol root-cause fix, or public JA11 support claim exists for J010. A future owner report may now correlate observed `0xD9FF` behavior with the device's reported firmware. |
| J011 | Merged source `5b4b40bfccabae91e3839de9ff2f7b1edcb0d67a` (PR [#44](https://github.com/weekssa/OPRA-EQ-for-UAPP/pull/44)); immutable APK [`EQ-Library-v0.7.0-beta-5b4b40b.apk`](https://raw.githubusercontent.com/weekssa/OPRA-EQ-for-UAPP/mobile-test-apk/candidates/EQ-Library-v0.7.0-beta-5b4b40b.apk); APK SHA-256 `847e2ed07b2373aa17c2feb026a843081123bae00882d777bdb43222375a4049`; signer certificate SHA-256 `65c1c1256dae3c49e3548f334c91f0ba991969e9be9e0b223ba4e253d2114747`; signed-beta [run #1364](https://github.com/weekssa/OPRA-EQ-for-UAPP/actions/runs/36197533476); signed artifact ID `10890643015`; artifact ZIP SHA-256 `9e3c9123d548607227beb3c94e405cbb1a6159ac8b391e20e7a472a8ca34af25`; R8 mapping SHA-256 `70823c91269be19a1a8405c7bb9fd446f1bfa44df67e668dd0a5fce5341f38a6` | **EXACT SIGNED DIAGNOSTIC CANDIDATE READY FOR OWNER TESTING.** Package `com.weekssa.opraeqforuapp`, version `0.7.0`/code `7`, target `ja11`, RSA-4096 signer, APK Signature Scheme v2/v3, zipalign, Android unit/lint/release/R8, emulator install/cold launch, diagnostics, artifact integrity, and immutable candidate publication all passed on this exact source. The candidate contains the corrected readable/JSON report serializer and optional firmware capture; it does not change JA11 codec math or claim a protocol root-cause fix. | No physical JA11 mutation, raw packet report from this candidate, restoration result, final release, or public support claim exists. The single next action is the bounded owner hardware session in the checklist. |

| J012 | Owner-readable report `/Users/stephenweeks/Library/CloudStorage/GoogleDrive-weekssa@gmail.com/My Drive/OPRA UAPP Presets/EQ Library Testing/FiiO JA11 operation report (1)` (SHA-256 `5cf579a19f4706d3895e0286079f46a8bb00af68acc87b1e74d4c5e326a60d3a`) and valid JSON `/Users/stephenweeks/Library/CloudStorage/GoogleDrive-weekssa@gmail.com/My Drive/OPRA UAPP Presets/EQ Library Testing/FiiO JA11 operation report JSON (1)` (SHA-256 `d88b3ed45ed821000616c5fb260356e415311aafbf72d3415b5dd30e451e7e41`); operation `3b348512-833a-4942-bb64-2a2e7bca1b5d`; reported source `5b4b40bfccabae91e3839de9ff2f7b1edcb0d67a`; firmware `2.20`; VID/PID `0x2972:0x0102`; session/detach `1/0`; permission requests `0` | **PHYSICAL NEGATIVE RESULT.** Canonical, selected, and quantized gain were `-3.9 dB`; write was `0x17` raw `0xD900` (`00 D9`); same-session readback was `0xD9FF` (`FF D9`) = `-3.800390625 dB`; delta was `0.099609375 dB` against `0.001 dB` tolerance. All five bands matched; Apply completed; Save count was `0`; outcome was `VerificationFailed`. The valid JSON confirms the corrected export. This proves a repeatable pre-Save mismatch on firmware 2.20, not its root cause. | Original-state restoration, Save behavior, persistence, and public qualification are **not proven**. No detach, reconnect, or permission issue occurred. Do not repeat Flash or Reset from this evidence; retain fail-closed verification. |
| J013 | Draft PR [#45](https://github.com/weekssa/OPRA-EQ-for-UAPP/pull/45), exact head `9d1b68252f3dd758dfa10d114b78217c71137e3c`; Android CI [#1845](https://github.com/weekssa/OPRA-EQ-for-UAPP/actions/runs/36218116958); CodeQL [#1729](https://github.com/weekssa/OPRA-EQ-for-UAPP/actions/runs/36218116924); Catalog currentness [#2120](https://github.com/weekssa/OPRA-EQ-for-UAPP/actions/runs/36218116920); Priority community [#1605](https://github.com/weekssa/OPRA-EQ-for-UAPP/actions/runs/36218116916) | **AUTOMATED SOFTWARE EVIDENCE PASS.** Unit tests, lint, debug/release assembly, R8 verification, API-26 min-API smoke, connected emulator UI tests, CodeQL, Catalog currentness, and Priority community coverage passed on the exact head. The only code changes are deterministic JA11 regression fixtures; no production protocol behavior changed. | Local Android execution is **NOT RUN** because the checkout has no Gradle wrapper, Gradle executable, or Android SDK. No signed artifact was produced for this test/documentation-only branch. No physical claim follows. |
| J014 | Corrected-codec source `c63c4060132ac9f45e898f413da5e4aefdbb7137` on draft PR [#45](https://github.com/weekssa/OPRA-EQ-for-UAPP/pull/45); Android CI [#1851](https://github.com/weekssa/OPRA-EQ-for-UAPP/actions/runs/36221829461); CodeQL [#1735](https://github.com/weekssa/OPRA-EQ-for-UAPP/actions/runs/36221829471); Priority community [#1611](https://github.com/weekssa/OPRA-EQ-for-UAPP/actions/runs/36221829444); Catalog currentness [#2126](https://github.com/weekssa/OPRA-EQ-for-UAPP/actions/runs/36221829496); dependency submission [#2191](https://github.com/weekssa/OPRA-EQ-for-UAPP/actions/runs/36221827629) | **AUTOMATED SOFTWARE EVIDENCE PASS.** The official FiiO contract is implemented as signed tenths-of-a-dB, high-byte-first `0x17` encoding/decoding. The follow-up correction keeps the wire word for `-3.9 dB` as `FF D9` while returning signed device-domain quantization `-3.9 dB`, preventing a false `6549.7 dB` comparison. Focused protocol, flasher, trace, optimizer, session, lint, debug/release assembly, R8/minified verification, API-26 smoke, connected emulator UI, CodeQL, priority coverage, catalog currentness, and dependency gates all passed on this exact head. | Local Android execution remains **NOT RUN** because the checkout has no Gradle wrapper, Gradle executable, or Android SDK. No signed artifact or physical mutation exists for J014. JA11 remains hardware-validation pending; the next step is one new signed owner-test candidate, not a repeat of J012. |

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

## Public `v0.7.0` release provenance — 2026-09-28

Record J018 is release provenance, not a new physical JA11 result. The owner-approved public
[v0.7.0 release](https://github.com/weekssa/OPRA-EQ-for-UAPP/releases/tag/v0.7.0) is latest,
published, and non-prerelease at release ID `397979580`. Tag `v0.7.0` points to
`4f325d673159b40515086fe5143df12b29ddb076`; the executable behavior was tested on
`e1ab5fa5a65dc2d64624d871ac53d436f792ea6a`, with documentation-only closeout synchronization on
the tagged head. The trusted release workflow was `36381764266`; immutable artifact ID was
`10952494777` with ZIP SHA-256
`08cffddad84f4c5648a4ec2e884784188689a24041f020cc9925e7af8040bccc`; public APK SHA-256 was
`27dada499bcbf9be9bd21d1349164858c93a5d2b83f78fd61134de13b4eb4025`; signer certificate
SHA-256 was `65c1c1256dae3c49e3548f334c91f0ba991969e9be9e0b223ba4e253d2114747`. The APK and
`.sha256` assets both report `uploaded` through the public GitHub API.

J018 does not promote JA11 hardware status. The complete User 1 editor/apply software path is
verified, while power-cycle retention and broader physical qualification remain bounded by the
exact J016/J017 reports and are not generalized to the public release.

## 2026-10-08 J019 — automatic headset restart verification failure

J019 is exact-candidate physical negative evidence for the JA11 DEVICE headset/microphone
cross-re-enumeration verification path. It does not invalidate the distinct J016/J017 EQ Flash and
restoration results and does not establish a protocol root cause.

- Candidate source `3f5e0c3a39687e27d962dd7f7f80d2667ff396ae`; diagnostic APK
  `opra-eq-ja11diag-0.8.0-source-3f5e0c3a.apk`, SHA-256
  `85e06ca0db818586a7eb2eab3378a1b21949b3c8593e1318536ec651d8369305`; package
  `com.weekssa.opraeqforuapp.ja11diag`, version `0.8.0-ja11diag`/11; debug signer certificate
  SHA-256 `73aa7581c8dc7dcc8ccea7586771119a98f9a74d7d8cf23716e09c557c9f6b41`.
- Physical context: Pixel 9; FiiO JA11 VID/PID `0x2972:0x0102`; firmware `2.20`. The private
  report, logs and screenshots are retained at
  `/private/tmp/ja11-v0.8.1-acceptance-3f5e0c3a/evidence`. Serial and full fingerprint are
  intentionally omitted from this repository.
- Complete pre-mutation baseline: volume 30/60; mic/headset On; program Off; UAC 2.0; sample rate
  384 kHz; global EQ gain `-3.7 dB`; five 0.0 dB Peak/Dip bands at 1000, 2000, 5000, 8000 and
  10000 Hz, each Q 0.7.
- One On-to-Off write (`0x12`) completed and session 1 detached; session 2 read fresh state and
  automatically displayed Mic Off. One Off-to-On restoration write (`0x12`) completed and session
  2 detached; session 3 read a matching complete User 1 EQ snapshot, but automatic DEVICE
  verification timed out and UI remained Mic Off. No write was retried. A later read-only Refresh
  read Mic On in session 3. Volume, program, UAC, full User 1 bank and gain matched the original
  baseline; restoration is confirmed.
- Stop boundary: Tests B/C/D, UAC, Flash, Reset, profile staging, and physical unplug/reconnect were
  **NOT RUN**. No Save occurred. The Pixel was released with the diagnostic app and app data
  preserved. This record proves the automatic Mic-On verifier defect on the exact superseded
  candidate and confirms read-only baseline restoration; it does not prove JA11-wide support,
  another candidate's behavior, or a root cause.
- Descriptor evidence recorded the class-3 HID interface ID changing `3 → 2 → 3`; the app did not
  record its selected interface, so this is a candidate explanation only. Cancellation of the
  previous inline verifier during `collectLatest` was another plausible explanation. The replacement
  implementation removes PID/interface from the stable restart key and transfers verifier ownership
  to a reconnect watchdog while preserving per-session generation checks. It requires a unique
  nonblank serial for restart controls and leaves same-session controls available without one. J019
  did not establish whether the owner's JA11 exposes that serial. The replacement source/APK and any
follow-up physical result must be recorded as a new exact-candidate record; J019 must not be
rewritten as a pass.

## 2026-10-08 J020 — delayed USB permission and replacement identity unavailable

J020 is incomplete/negative physical evidence for the expected-restart path. It records a
completed microphone write and fresh device readback, but the app did not complete automatic
restart verification. It is not a pass and is tied only to the candidate below.

- Candidate source `a78808443c71d688e0f338e96495847569fe12f7`; diagnostic APK
  `opra-eq-ja11diag-0.8.0-source-a7880844.apk`, SHA-256
  `7beb5bcebbc0dc40a68b33de911cc8722d76d3f0ff2e98685b1fa25e17caed61`; package
  `com.weekssa.opraeqforuapp.ja11diag`, version `0.8.0-ja11diag`/11; debug signer certificate
  SHA-256 `73aa7581c8dc7dcc8ccea7586771119a98f9a74d7d8cf23716e09c557c9f6b41`.
- Physical context: Pixel 9; JA11 firmware `2.20`; VID/PID `0x2972:0x0102`. The raw report,
  logs, and screenshots remain in the owner-only private evidence bundle. No serial or full
  fingerprint is copied here.
- The initial session passed the identity gate (`identityAvailable=true`, `sessionCurrent=true`).
  One Mic On-to-Off write completed and detached the device. Android granted replacement-session
  USB permission approximately 18.5 seconds after the request, later than the app's 10-second
  fallback. The replacement session opened and produced fresh readback: Mic Off; volume 30/60;
  program Off; UAC 2.0; sample rate 384 kHz; and the complete User 1 gain/band snapshot unchanged.
- In the replacement session, identity was unavailable while the session was current
  (`identityAvailable=false`, `sessionCurrent=true`). No `RESTART_VERIFY_*` event was recorded.
  A pre-permission attach log reported no readable serial value. That observation cannot distinguish
  a missing/blank descriptor from permission-gated access; the exported record has no raw serial,
  and the capture did not establish whether the post-permission lookup was blank or raised a
  security exception.
- The stable identity key is derived from exactly one nonblank USB serial. The current-session
  readback proves the device session itself was usable; it does not prove that the replacement
  session is the same physical JA11. The delayed permission grant and missing identity co-occurred,
  but the available evidence does not prove that one caused the other. Keep identity fail-closed.
- No second mutation, restoration write, UAC operation, Flash, Reset, profile staging, or physical
  unplug/reconnect was performed. Tests B/C/D remain NOT RUN. The last verified microphone state
  is Off and restoration to the original On state is outstanding. No current device state is
  asserted after the Pixel was released.

## 2026-10-08 corrected software candidate preflight — no new hardware result

The corrected off-phone candidate is application source
`da1f8e25918065667648d676cb669fed4c803f17`, tree
`aa41aba1ddfe37006aab0f1cfdb21c4df63c2481`. Its diagnostic APK is
`opra-eq-ja11diag-0.8.0-source-da1f8e25.apk`, SHA-256
`767b42591adc92f0e1662480112bf9efe87ce15f51060d20a7aa39118b8d8c24`; package
`com.weekssa.opraeqforuapp.ja11diag`, version `0.8.0-ja11diag`/11; debug signer certificate
SHA-256 `73aa7581c8dc7dcc8ccea7586771119a98f9a74d7d8cf23716e09c557c9f6b41`.

The exact-source diagnostic build passed. The focused six-class permission/restart regression suite,
full G3 gates, R8 mapping verification, and all 64 API 35 instrumentation cases passed. Local XML
readback counted 818 JVM tests with zero failures, errors, or skips across 129 suites. The APK was
installed and cold-launched only on isolated API 35 emulator `emulator-5560`; its
`APP_BUILD_INFO` reports the exact source SHA above. No physical device was enumerated, connected,
or modified for this preflight. Exact PR-head CI remains a separate gate.

Host preparation ran without querying ADB devices or mDNS services: SDK adb
`37.0.1-15733141`, isolated ADB mDNS backend check successful, default route present, no active
VPN/proxy, and macOS application firewall disabled. The Pixel's live endpoint remains pending.
Raw host routing/proxy details and emulator logs are private at
`/private/tmp/ja11-v0.8.1-acceptance-da1f8e25/`; no Pixel serial, JA11 fingerprint, or hardware
claim is included here. J020 remains the latest mutation record; J021 below is the latest
read-only physical observation. Mic On restoration remains outstanding.

## 2026-10-08 J021 — read-only initial-session identity observation

J021 is a read-only observation on the prior corrected diagnostic candidate. It is not a
replacement-session result, restart verification, mutation test, or physical pass.

- Candidate source `da1f8e25918065667648d676cb669fed4c803f17`; diagnostic APK
  `opra-eq-ja11diag-0.8.0-source-da1f8e25.apk`, SHA-256
  `767b42591adc92f0e1662480112bf9efe87ce15f51060d20a7aa39118b8d8c24`; package
  `com.weekssa.opraeqforuapp.ja11diag`, version `0.8.0-ja11diag`/11; debug signer certificate
  SHA-256 `73aa7581c8dc7dcc8ccea7586771119a98f9a74d7d8cf23716e09c557c9f6b41`.
- Context: Pixel 9; JA11 firmware `2.20`; VID/PID `0x2972:0x0102`. The owner-only private
  session folder is `/private/tmp/ja11-v0.8.1-acceptance-da1f8e25/owner-phone-session-20261008`.
  Device identifiers and raw logs remain private.
- In the initial connected session, Android USB permission was granted and
  `UsbDevice.serialNumber` returned null (`READABLE_NULL`). Session generation 1 completed a
  full read-only snapshot, then reported `identityAvailable=false` while `sessionCurrent=true`.
  The captured `command=0x12` event is a headset/mic read; J021 contains no write or restart.
- The J021 candidate did not read `UsbDeviceConnection.getSerial()`. Therefore the observation does
  not establish whether the USB descriptor lacks a serial, and it does not establish the result of
  J020's replacement-session lookup. A screenshot showed Mic Off, UAC 2.0, and 384 kHz. The original
  Mic On restoration remains outstanding; no current state is asserted after releasing the phone.
- The next source adds a JA11-only read of the opened connection's standard USB serial descriptor
  only when `UsbDevice.serialNumber` returns null. Blank values and exceptions still leave identity
  unavailable; other shared transports keep this fallback disabled. This is an off-phone candidate
  under test and has no physical result yet.

## 2026-10-08 exact-source software candidate preflight — not physical evidence

The replacement application source is commit `616958037349e2f0e0784a556c6430b0de6ceb18`, tree
`fdb6c8d10c8aa865da1a4818d22a7c14b2559b21`. Its diagnostic APK is
`opra-eq-ja11diag-0.8.0-source-61695803.apk`, SHA-256
`ee0fe4fbfaae7b3f959d4122f0c21c128dffdf21d47586376ee383e2534ceb0d`, package
`com.weekssa.opraeqforuapp.ja11diag`, version `0.8.0-ja11diag`/11, signed with the established
debug certificate SHA-256 `73aa7581c8dc7dcc8ccea7586771119a98f9a74d7d8cf23716e09c557c9f6b41`.

Local G0/G2/G3 and R8 verification passed. The full JVM XML results contain 825 tests, zero
failures/errors/skips. The wiped API 35 emulator cold-launched the exact candidate and reported the
exact source in `APP_BUILD_INFO`; Android instrumentation passed 64/64. Full emulator and
source-bound log evidence is private under `/private/tmp/ja11-v0.8.1-acceptance-61695803/`. These
results do not establish physical serial availability or JA11 behavior. Exact final PR-head checks,
fresh physical identity, restoration of Mic On, and the remaining physical acceptance are pending.
J020 remains the latest mutation record; J021 remains the latest physical observation.

## 2026-10-08 exact diagnostic artifact revalidation — not physical evidence

During the resumed acceptance preflight, the frozen APK at
`/private/tmp/ja11-v0.8.1-acceptance-61695803/opra-eq-ja11diag-0.8.0-source-61695803.apk` was
rechecked. Its SHA-256 remains
`ee0fe4fbfaae7b3f959d4122f0c21c128dffdf21d47586376ee383e2534ceb0d`; package is
`com.weekssa.opraeqforuapp.ja11diag`; version is `0.8.0-ja11diag`/11; and `apksigner` verifies one
v2 signer with certificate SHA-256
`73aa7581c8dc7dcc8ccea7586771119a98f9a74d7d8cf23716e09c557c9f6b41`. The exact source-bound API
35 emulator launch and 64/64 instrumentation results remain recorded above. This readback proves
local artifact identity only; physical serial availability and JA11 behavior remain unverified.

## 2026-10-09 J022 — exact-candidate phone attempt with JA11 disconnected

After the exact app source, emulator, independent review, and PR-head CI gates passed, the frozen
diagnostic APK was installed and runtime-verified on the Pixel 9. Candidate source was
`616958037349e2f0e0784a556c6430b0de6ceb18`; APK SHA-256 was
`ee0fe4fbfaae7b3f959d4122f0c21c128dffdf21d47586376ee383e2534ceb0d`; package/version/code and
debug signer are recorded in the preceding preflight entry.

The read-only `verify-identity` helper exited 14 because its filtered current-logcat snapshot did not
contain the required candidate `APP_BUILD_INFO`. The already-running private continuous log retained
the candidate build event and a `USB_ATTACH` event, but contained no descriptor-status,
`USB_SESSION_OPENED`, complete snapshot, or identity-availability event. The owner confirmed that
the JA11 was disconnected during this attempt. This does not exercise or fail the opened-connection
serial fallback, and it does not establish a JA11 identity. No JA11 write or control command was
sent. The Pixel was released after preserving the local evidence at
`/private/tmp/ja11-v0.8.1-acceptance-61695803/owner-phone-session-20261009/`; the full logs remain
private and are not copied into the repository.

Off-phone diagnosis found that the helper used a new Android logcat ring-buffer dump for each
verification step even though the foreground private capture retained the build event. The helper
now extracts diagnostic events from that retained capture so build and session events share one
evidence window. This host-side change is untested and must pass its applicable local/helper review
and exact-head CI gates before another phone test. The identity gate remains open; J020 remains the
latest mutation record, and Mic On restoration remains outstanding.

## 2026-10-09 J023 — helper-only verification gate closeout

The frozen app source and exact diagnostic APK are unchanged. The private APK SHA-256 still matches
`ee0fe4fbfaae7b3f959d4122f0c21c128dffdf21d47586376ee383e2534ceb0d`. The phone helper now extracts
from the same retained private log, binds verification to the active ADB logcat process and selected
Pixel serial, rechecks capture liveness after extraction and immediately before success, and prints
only curated identity status summaries. No JA11 serial or fingerprint value is printed.

The fake-ADB regression passed for the positive identity fixture, missing/stopped capture, capture
loss during the final verification check, wrong selected serial, blank connection serial, missing
candidate build event, and serial/fingerprint sentinel redaction. Both shell scripts passed `bash
-n`; `git diff --check` passed; the Unlazy helper gate passed; and independent read-only review
approved the final helper/test diff. No Android source changed, so under the current owner instruction
the helper-only delta did not trigger another Android or CI matrix. These checks do not establish
JA11 identity or physical behavior. The authorized physical session may now proceed with read-only
identity first. No JA11 write has occurred since J020; original Mic On restoration remains pending.

## 2026-10-09 J024 — historical exact-candidate read-only identity observation

The authorized session used the exact frozen candidate: source
`616958037349e2f0e0784a556c6430b0de6ceb18`, APK SHA-256
`ee0fe4fbfaae7b3f959d4122f0c21c128dffdf21d47586376ee383e2534ceb0d`, package
`com.weekssa.opraeqforuapp.ja11diag`, version `0.8.0-ja11diag`/11, and debug signer SHA-256
`73aa7581c8dc7dcc8ccea7586771119a98f9a74d7d8cf23716e09c557c9f6b41`. The installed APK hash,
package metadata, signer, and runtime `APP_BUILD_INFO` matched. The Pixel was a Google Pixel 9. The
app displayed FiiO JA11 connected; diagnostics recorded product ID `258` (`0x0102`) and permission
granted. This session did not independently capture VID or firmware.

The identity descriptor event reported `serialStatus=READABLE_NULL`,
`connectionSerialStatus=READABLE_NULL`, and `serialSource=NONE`. Because the device getter was null,
the JA11-only connection fallback did execute; it returned null. The candidate opened session
generation 1 and completed the source-bound snapshot with program Off, global EQ gain `-3.7 dB`, and
five 0 dB Peak/Dip bands at 1000, 2000, 5000, 8000, and 10000 Hz with Q 0.7. The subsequent event
reported `identityAvailable=false` and `sessionCurrent=true`. Thus the connection was usable for
read-only reports, but the required unique serial identity was not available.

No hardware write, restart test, Mic change, UAC change, volume/program change, Flash, or Reset was
sent. The diagnostic trace includes read requests for firmware, sample rate, volume, Mic, and UAC,
but it did not preserve their decoded values. The last decoded Mic state remains Off from J020; no
new DEVICE control value is claimed from J024.
After preserving evidence at
`/private/tmp/ja11-v0.8.1-acceptance-61695803/owner-phone-session-20261009-resume/`, the diagnostic
app was force-stopped and the live log capture was stopped. The captured diagnostics have no
`USB_SESSION_CLOSED` event, so app-level closure was not independently confirmed. The Pixel was
released immediately after the failed read-only gate.

This is a physical observation that both serial readers returned null on this connected unit and
candidate, not proof that the JA11 hardware has no serial descriptor: no raw descriptor index or
string response was captured. The former conclusion that this required stopping all further
physical work is superseded by the owner-approved Model D policy above; J024's no-write boundary
remains unchanged. Android's AOSP implementation obtains `getSerial()` through the USB
`iSerialNumber` string descriptor and can return null when that lookup yields no string
([framework JNI](https://android.googlesource.com/platform/frameworks/base/+/7647091436c45af2d82f12c9ea9ec77fa309b49b/core/jni/android_hardware_UsbDeviceConnection.cpp#197),
[USB host serial lookup](https://android.googlesource.com/platform/system/core/+/ec9e7b1/libusbhost/usbhost.c#421));
the available evidence does not distinguish an absent serial descriptor from an unsuccessful
descriptor read. Do not substitute VID/PID, product name, firmware, or port path for serial
continuity when both sessions provide serial. Future physical work remains gated on the exact Model D
candidate, its off-phone gates, and a new owner-confirmed phone window.

## 2026-10-09 Model D exact candidate — off-phone gates (no physical evidence)

Production app source commit `1d19067c9150aae1b09e01fafb8af3647bf65f71`, tree
`e108d58a6863e594a6dadbc0d2f4fb745583c5a3`, implements the owner-approved Model D policy. Local
qualification on this source includes 856 JVM tests with zero failures/errors/skips, lint,
debug/release/diagnostic and Android-test compile/assembly, R8, and focused JA11 identity/session,
Mic/UAC, Flash/Reset, stale-callback, no-replay, and candidate-cardinality regressions. Updated
`phone-session.sh` candidate/rollback pins pass shell syntax and fake-ADB fixtures, including sole
serialless acceptance, zero/multiple candidate rejection, stale-session rejection, later ambiguity,
and output redaction.

The source-bound diagnostic artifact is
`opra-eq-ja11diag-0.8.0-source-1d19067c.apk`, SHA-256
`23adf9f4955b056f110562362717c706767b7e3ecc50225c07439a3ff5c23711`, package
`com.weekssa.opraeqforuapp.ja11diag`, version `0.8.0-ja11diag`/11, with one Android Debug signer
certificate SHA-256
`73aa7581c8dc7dcc8ccea7586771119a98f9a74d7d8cf23716e09c557c9f6b41`. APK v2 verification passed;
generated BuildConfig and runtime `APP_BUILD_INFO` report the exact source SHA. These artifacts are
private at `/private/tmp/ja11-v0.8.1-model-d-1d19067c/`.

On isolated wiped API 35 AVD `ja11-v081-api35-clean-20261008` at explicit target `emulator-5580`
(Android 15 / SDK 35), the exact `.ja11diag` APK installed and cold-launched successfully. Runtime
`APP_BUILD_INFO` matched the exact candidate source and diagnostics were enabled. All 64 applicable
Android instrumentation tests passed in 95.573 seconds against the same production source snapshot's
`debug` test variant. The project does not generate a JA11-diagnostic Android-test variant; the
separate debug app BuildConfig reports `local-unqualified`, so source binding for the candidate is
established by the exact diagnostic APK's own BuildConfig and runtime log instead.

An independent read-only follow-up review examined JA11 transport, repository, session/token/identity,
Flasher, ViewModel/UI and tests; it answered all ten owner questions and found no remaining actionable
defect. It confirmed the attach-during-open and delayed same-path/PID detach races are fenced by their
regressions. Android cannot distinguish identical same-path/PID serialless units before attach/detach
callbacks are processed; the implementation makes no same-unit claim during that interval.

At this ledger update, PR #80 head is the production source commit above. Five of eight check runs
were successful and three were still in progress on that head; this is not a final exact-head CI pass.
The candidate metadata/helper/docs successor must receive its own eight successful checks before the
phone-window request. No Pixel or JA11 command, read, or write occurred for this candidate. No
physical acceptance, same-device identity, hardware-state readback, or release/support claim is
proved by these software, artifact, review, or emulator results. The physical plan remains gated on
final exact-head CI, host-only preflight, and a fresh explicit owner phone-window confirmation.

## 2026-10-09 Model D source-head CI confirmation

All eight required check runs pass on exact production app-source commit
`1d19067c9150aae1b09e01fafb8af3647bf65f71`: `build`, `emulator-ui-test`, `min-api-smoke`, CodeQL,
Analyze Kotlin, `validate`, `validate-priority-community`, and `submit-gradle`. The Android CI run is
[37901813099](https://github.com/weekssa/OPRA-EQ-for-UAPP/actions/runs/37901813099); separate checks:
[CodeQL](https://github.com/weekssa/OPRA-EQ-for-UAPP/runs/113727793185),
[Analyze Kotlin](https://github.com/weekssa/OPRA-EQ-for-UAPP/actions/runs/37901812737),
[validate](https://github.com/weekssa/OPRA-EQ-for-UAPP/actions/runs/37901812646),
[validate-priority-community](https://github.com/weekssa/OPRA-EQ-for-UAPP/actions/runs/37901812841), and
[submit-gradle](https://github.com/weekssa/OPRA-EQ-for-UAPP/actions/runs/37901809027).

This confirms CI for the production app-source SHA and the candidate APK's source. Candidate metadata,
acceptance-plan, and helper-pin documentation is being committed separately; require all eight
checks again on the exact live PR head after that successor is pushed. No Pixel or JA11 command was
issued, and this CI result does not establish physical behavior or change J020/J021/J024 evidence.

## 2026-10-09 Model D checked docs/helper PR head

Production app-source commit `1d19067c9150aae1b09e01fafb8af3647bf65f71` and its exact diagnostic
APK remain unchanged. Exact docs/helper PR head `69076094811c13365bafecb9a1e0cf50be8d14f0` passed
all eight required checks: Android CI build, emulator UI, API-26 smoke; CodeQL; Analyze Kotlin;
`validate`; `validate-priority-community`; and `submit-gradle`. The Android CI run is
[37904380928](https://github.com/weekssa/OPRA-EQ-for-UAPP/actions/runs/37904380928), the targeted
UI retry job is
[113739278865](https://github.com/weekssa/OPRA-EQ-for-UAPP/actions/runs/37904380928/job/113739278865),
CodeQL is [113736544021](https://github.com/weekssa/OPRA-EQ-for-UAPP/runs/113736544021), Analyze
Kotlin is [113734198393](https://github.com/weekssa/OPRA-EQ-for-UAPP/actions/runs/37904380929/job/113734198393),
`validate` is [113734198229](https://github.com/weekssa/OPRA-EQ-for-UAPP/actions/runs/37904380924/job/113734198229),
priority-community coverage is
[113734198946](https://github.com/weekssa/OPRA-EQ-for-UAPP/actions/runs/37904381046/job/113734198946),
and Gradle submission is
[113734183636](https://github.com/weekssa/OPRA-EQ-for-UAPP/actions/runs/37904374382/job/113734183636).
The first UI attempt reported focus loss after a Nexus Launcher input-dispatch ANR; the targeted retry
completed all 64 instrumentation tests with zero failures, errors, or skips. Retry report artifact
`11604946659` has SHA-256
`c2eaf300f9b2f16f8701e79ca27fb6c3d2a3a7bf3a559b55278197950fe2a0fd`.

Host-only verification reconfirmed the Model D diagnostic APK SHA-256
`23adf9f4955b056f110562362717c706767b7e3ecc50225c07439a3ff5c23711` and rollback APK SHA-256
`ee0fe4fbfaae7b3f959d4122f0c21c128dffdf21d47586376ee383e2534ceb0d`; both report package
`com.weekssa.opraeqforuapp.ja11diag`, version `0.8.0-ja11diag` / 11, one v2 signer with SHA-256
`73aa7581c8dc7dcc8ccea7586771119a98f9a74d7d8cf23716e09c557c9f6b41`, and the expected launchable
activity. At this checkpoint no Pixel or JA11 had been contacted for Model D physical work; the
later physical continuation is recorded below.

## 2026-10-09 Model D physical continuation — pre-Save Flash failure, state restored

Exact app source `1d19067c9150aae1b09e01fafb8af3647bf65f71`; diagnostic APK
`opra-eq-ja11diag-0.8.0-source-1d19067c.apk`, SHA-256
`23adf9f4955b056f110562362717c706767b7e3ecc50225c07439a3ff5c23711`; package
`com.weekssa.opraeqforuapp.ja11diag`, version `0.8.0-ja11diag` / code `11`; debug signer SHA-256
`73aa7581c8dc7dcc8ccea7586771119a98f9a74d7d8cf23716e09c557c9f6b41`. The exact candidate passed
the required read-only identity and baseline checks on the connected JA11 before mutation. Device
serial is intentionally omitted.

The Mic baseline was Off, so one Mic Off-to-On write was issued and fresh readback verified On. A
UAC 2.0-to-1.0 operation and readback passed. The UAC 1.0-to-2.0 restore report was accepted and
the device detached, but permission was granted after the 25-second operation deadline. The
automatic verifier therefore timed out; a later new read-only session verified UAC 2.0. This is
permission-timing inconclusive evidence, not a JA11 UAC failure. No uncertain mutation was replayed.

Starting from program Off, one Flash was submitted. The trace shows five `0x15` band writes, then
`0x17` global gain, then `0x16` User 1 selection and `0x18` Apply. Volatile readback failed at band
1. The app stopped before persistent `0x19` Save; no retry was attempted. After the failure,
read-only refresh showed User 1 active with a bank that did not match the target. This strongly
supports the hypothesis that the data writes occurred before selecting the User 1 bank, but it does
not prove the device's bank-selection semantics or rule out another device-side cause. Do not
describe this as a Save/persistence failure.

The original Off program was restored through the normal selector and freshly verified. Final
captured state was volume 30, Mic On, Off, UAC 2.0, 384 kHz, global gain `-3.7 dB`, and the original
flat five-band snapshot. The temporary profile was removed, the exact previous diagnostic APK was
restored and verified, log capture stopped, wireless ADB disconnected, and the Pixel was released.
Evidence is retained privately at
`/private/tmp/ja11-v0.8.1-acceptance-61695803/owner-phone-session-20261009T161940Z-model-d-resume/`.

## 2026-10-09 User 1 ordering correction — exact software candidate, no physical result

Application source `92c11fb0e41ae11b118b2e7bb105234d6606dbdb`; diagnostic APK
`opra-eq-ja11diag-0.8.0-source-92c11fb0.apk`, SHA-256
`ce3f417f20c275fd4d535cf5e70f658d8430af8fdd3f87ea950705fbfd574637`; package
`com.weekssa.opraeqforuapp.ja11diag`, version `0.8.0-ja11diag` / code `11`; debug signer SHA-256
`73aa7581c8dc7dcc8ccea7586771119a98f9a74d7d8cf23716e09c557c9f6b41`.

G2/G3/G3a pass: 32 focused Flasher tests; 861 JVM tests with zero failures, errors, or skips; lint,
debug/release/diagnostic and Android-test compile/assembly, R8, helper syntax and fake-ADB fixtures.
Independent app-source and helper reviews found no actionable defect. Host verification confirmed
the APK hash, package/version, v2 signature, signer, and BuildConfig source SHA. On isolated API 35
AVD `ja11-v081-api35-clean-20261008`, the exact diagnostic APK cold-launched and its runtime event
reported the matching source SHA. The source-matched debug instrumentation suite passed 64/64 with
zero failures or skips. Private emulator evidence is at `/private/tmp/ja11-v0.8.1-order-92c11fb0/`.

This record is software/emulator evidence only. The Pixel was not queried for this gate. Exact-head
PR #80 CI is pending. No JA11 hardware capability is qualified by this candidate yet; use the
already-authorized physical plan only after its exact-head CI passes and the new candidate's
read-only session/cardinality/baseline gate succeeds.


## 2026-10-09 JA11 workflow lock and Model D helper checkpoint

This checkpoint records off-phone workflow/helper/documentation work only. Production source remains `3d7bc1d91e6c39327477d1341bde80e8a37bfbd4`; the frozen diagnostic APK remains SHA-256 `3b74672a587daeaaf8f562634c5dcecea073f7ad6e01df736f874ee448fc6261`, package `com.weekssa.opraeqforuapp.ja11diag`, version `0.8.0-ja11diag` / code `11`, signer certificate SHA-256 `73aa7581c8dc7dcc8ccea7586771119a98f9a74d7d8cf23716e09c557c9f6b41`. At this checkpoint GitHub PR #80 is still at head `186c43b22490281ba2fbfceae52e7fa5d13b9c3b`, which does not contain the current production source.

The mission Constitution, concise State, and Gate Matrix were created. The acceptance helper and current procedure now follow owner-approved Model D: serial optional, mismatch rejection when both sessions expose a usable serial, one-candidate/session/generation/readback requirements retained, and no same-unit claim without a serial match. G0 ledger lint passed with output SHA-256 `f27cfadc49451ca83380a6087fedcaa5c21ada937f1ff02424f1b506e5c6087b`. G3a shell/Python syntax and fake-ADB fixtures passed with output SHA-256 `109cb5985e007e7d80306a1263680c66d470e270f3dc8af5de48334281622253`. Production-source review passed; corrected-helper independent review remains pending. Existing PR workflows on `186c43b2` do not qualify the `3d7bc1d9` source; exact-head CI remains pending.

No Pixel, physical JA11, Android device, or emulator command was issued for this checkpoint. The prior physical session on source `92c11fb0` restored and freshly verified the original state before release; current live hardware state is unknown, and no restoration is outstanding from that session. Phone needed now: **No**.

## 2026-10-09 JA11 corrected helper review — G4 pass

This off-phone record applies to production source `3d7bc1d91e6c39327477d1341bde80e8a37bfbd4` and diagnostic APK SHA-256 `3b74672a587daeaaf8f562634c5dcecea073f7ad6e01df736f874ee448fc6261`, package `com.weekssa.opraeqforuapp.ja11diag`, version `0.8.0-ja11diag` / code `11`, signer certificate SHA-256 `73aa7581c8dc7dcc8ccea7586771119a98f9a74d7d8cf23716e09c557c9f6b41`. The source review found no actionable issue. The corrected helper/procedure review found no remaining actionable issue after the earlier review identified missing coverage for a prior opened generation and `permissionGranted=false`; both were fixed and G3a was rerun successfully with output SHA-256 `944d79f424382f0f7979d2e85d610c46cbc3030adab5726b8635b7a9faed34be`.

The final review confirmed Model D optional-serial behavior, mismatch rejection when both serials exist, exact-one-candidate gating, opened-generation and detach/close ordering, privacy-safe output, and the fresh phone-window confirmation requirement. The reviewer did not run tests, ADB, emulator, phone, or hardware. PR #80 was still at `186c43b22490281ba2fbfceae52e7fa5d13b9c3b` at review close; exact-head CI remains pending. This record adds no physical evidence and makes no claim about current live device state.

## 2026-10-09 clean v0.8.1 candidate source and host verification — physical gate pending

This record is for implementation source `9493cf030acb440f92e547fc667f6a5399616045`, based on refreshed `origin/main` `9b9a1f610025b3d6233d4ff6f0c2309a668577cc`, on branch `codex/ja11-v0.8.1-minimal`. The production source correction is limited to the approved A-E transaction/session behavior, candidate identity metadata, and JA11-only strict transport behavior. The 2026-10-09 independent reviewer answered all ten required questions PASS against source `32f1006fb1b1edbcba3b0470ed71772448e48886`; a supplemental review against `9493cf030acb440f92e547fc667f6a5399616045` confirmed the sole KDoc-order correction resolved the finding, with no new blocker.

G1-G7 were rerun after that correction and all passed: focused JA11 regressions (output SHA-256 `e97021eab8ed2f2869049496050482604d3080a50100fdf1a2c5c21963079df6`); full JVM suite, 761 tests with zero failures/errors/skips (output SHA-256 `cd5a3a071e329b7e96363cca5f2802c929975da00a9de35d4d1edef6f5030025`); lint (output SHA-256 `24570454d6a1875b7548df988870bfc511206b5be6bff8788612d4149a6a39b1`); debug/release/diagnostic/Android-test APK assembly (output SHA-256 `70a4a5c8632352aa2a763965c0348cdadcf1363ad09b150a4c0c2f472d6d93e6`); R8 mapping verification; API 35 instrumentation, 64/64 with zero failures/errors/skips (output SHA-256 `010f0a5b05d8e3895d25c53fbc951f334100c2c9b2387288930c085526876f5e`); and fake-ADB phone-helper fixtures (output SHA-256 `6b4cb3a2ef8deb76d6954d313b7a8edde47c7402b51e6754de4a37d295e03716`). Emulator use was limited to disposable AVD `ja11-v081-api35-clean-20261008` on explicit serial `emulator-5554` and isolated ADB port 5039, then shut down.

The exact-source diagnostic artifact is `app/build/outputs/apk/ja11Diagnostic/app-ja11Diagnostic.apk`, SHA-256 `98ee2eb876ec0d6d5bcfcebebdf55e1c77f2f7edfbecfd59322a94e5feea48ae`, package `com.weekssa.opraeqforuapp.ja11diag`, version `0.8.1-ja11diag` / versionCode `12`, signer certificate SHA-256 `73aa7581c8dc7dcc8ccea7586771119a98f9a74d7d8cf23716e09c557c9f6b41`. Host verification confirmed that DEX includes the exact source SHA, APK Signature Scheme v2 verifies with one debug signer, and zip alignment passes. This exact APK clean-installed and cold-launched on the isolated API 35 emulator. No Pixel, JA11, physical ADB, or hardware command was used for these gates.

The scope audit maps the seven production files to A-E or candidate identity; test, helper, changelog, and documentation changes remain within the approved mission. Shared defaults for EW300/JM12 remain unchanged; no discarded reconnect architecture, diagnostic-only logging, or unsupported persistence claim was introduced. The new draft PR is not yet open, so exact-head CI and the PR-head portion of the final candidate tuple remain pending. This record does not qualify JA11 hardware or authorize physical mutation.

## 2026-10-09 initial clean-candidate PR #81 exact-head CI — pass for recorded head only

Draft PR [#81](https://github.com/weekssa/OPRA-EQ-for-UAPP/pull/81) was opened without changing production source. On exact PR head `6406443dd401e94fe4fa0f9b4e837ad684ccf045` and production source `9493cf030acb440f92e547fc667f6a5399616045`, all eight required check runs completed successfully: `CodeQL` 114133387709; `emulator-ui-test` 114132259444; `build` 114132259423; `min-api-smoke` 114132259322; `Analyze Kotlin` 114132259157; `validate-priority-community` 114132259126; `validate` 114132258972; and `submit-gradle` 114132223057. The associated workflow runs were Android CI #2000 (38024475012), CodeQL #1886 (38024474980), priority community coverage #1688 (38024474995), and catalog currentness #2221 (38024474994). Every check run's `head_sha` matched `6406443dd401e94fe4fa0f9b4e837ad684ccf045`.

This result qualifies that exact PR head only. A follow-up documentation-only correction is being committed to record the now-open PR and the measured production diff; it creates a new PR head and therefore needs its own exact-head CI before the candidate tuple is frozen. PR #80 remains open/draft/unmerged at head `5f471dfdce93c35f93a4632ca76ff729fcb58a6b` and has not been changed. No Pixel, JA11, physical ADB, or hardware operation was used for this run, and no physical result is inferred from it.
