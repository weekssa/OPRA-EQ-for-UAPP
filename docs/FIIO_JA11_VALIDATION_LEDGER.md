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

JA11 has historical physical Flash/Save/final-readback and observed reconnect/restoration passes on
the exact J016/J017 candidates; explicit power-cycle retention and full hardware qualification
remain pending. The latest physical record is J020, an incomplete/negative restart-verification
result on source `a78808443c71d688e0f338e96495847569fe12f7`. One Mic On-to-Off command completed and
the replacement session freshly read Mic Off, but the app did not emit restart-verifier events.
USB permission arrived about 18.5 seconds after the request, and the replacement session's
identity was unavailable even though that session was current. The last verified device state is
Mic Off; restoration to the original Mic On state remains outstanding. J020 does not invalidate
J016/J017's separate EQ evidence and does not qualify the v0.8.1 fix. Do not reuse its diagnostic
APK, infer the identity failure's underlying cause, or make a public JA11 support claim.

The `0x17` global-gain codec correction remains proven by the official FiiO Control JA11 codec and
historical physical evidence. J012 remains valid negative evidence for the superseded codec.

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
