# EQ Library

**EQ Library** is a native Android app for finding, saving, converting, exporting, and—on supported hardware—directly applying parametric EQ presets.

The project began as **OPRA EQ for UAPP**. The repository and Android application ID remain `com.weekssa.opraeqforuapp` so existing installations continue to upgrade normally, but the product is now source-agnostic: **OPRA is one attributed EQ source, not the product identity.**

EQ Library ships with **zero headphone or EQ profiles bundled in the APK**. It downloads a validated canonical catalog, keeps a last-known-good local cache, and remains useful offline after the first successful sync.

## Current release

**v0.7.0** is the current public Android release.

[Download EQ Library v0.7.0](https://github.com/weekssa/OPRA-EQ-for-UAPP/releases/tag/v0.7.0) or [download the signed APK directly](https://github.com/weekssa/OPRA-EQ-for-UAPP/releases/download/v0.7.0/EQ-Library-v0.7.0.apk)

- Android 8.0 / API 26 or newer
- Signed with the project's permanent Android release identity for in-place upgrades
- No Google Play account, EQ Library account, or cloud account required
- No analytics or telemetry

The public v0.7.0 APK is `EQ-Library-v0.7.0.apk`. Its SHA-256 is:

`27dada499bcbf9be9bd21d1349164858c93a5d2b83f78fd61134de13b4eb4025`

The matching [SHA-256 checksum file](https://github.com/weekssa/OPRA-EQ-for-UAPP/releases/download/v0.7.0/EQ-Library-v0.7.0.apk.sha256) is published with the release.

Android may ask you to allow installation from the browser or file manager used to open the APK because GitHub Releases are installed outside an app store. EQ Library itself does not request package-install permission and never silently installs updates.

## v0.7.0 highlights

SIMGOT EW300 DSP has a five-band Peak workflow with readback, local editing, Review → Apply, Flash, Reset, reconnect verification, and evidence-bounded DEVICE state. The path reuses the shared My DAC session, canonical EQ pipeline, response adapter, and capture flow. See the [EW300 DSP status](docs/V0.7_EW300_DSP_STATUS.md) and [v0.7.0 release notes](docs/releases/v0.7.0.md) for the complete evidence record.

- TRN Black Pearl Direct Flash reports success only after final native readback confirms the requested ten-band state and global playback gain.
- FiiO JA11 My DAC → EQ supports a fresh verified User 1 read, local five-band editing, Review, explicit Apply, one Save boundary, and final readback verification.
- Hardware operations remain fail-closed: an uncertain write is reported as unverified and is not retried automatically.

## v0.6.0 history

v0.6.0 is publicly released from merge commit `e5ffa5d00862edc3b79bf52e1508db5244845e94`. The signed release build passed the controlled publication workflow; the focused Pixel 9 Black Pearl/UI smoke test passed on exact behavior evidence source `eef5633e18a4ac311f110493e29633bf382675e3`.

The approved v0.6 direction includes:

- one app-wide supported-DAC session shared by My EQs, My DAC, and EQ Library;
- automatic fresh EQ + supported DEVICE reads after connection/reconnection and after verified hardware changes;
- Flash from the EQ's existing My EQs / EQ Library surface rather than a duplicate My DAC chooser;
- one concise DEVICE settings surface with current verified values and direct row-level editing;
- stable DEVICE connection context with shared bottom operation feedback for Applying, verification/reconnect, success, and recoverable failure;
- Black Pearl **Restore defaults** using the qualified EQ Library-owned preset: 50% volume, FAST-LL, HIGH, CLASS AB, centered balance, and microphone gain 0 dB, with optional EQ reset to flat;
- a single **device-agnostic My EQs** library whose saved headphones/EQs do not change when the output target changes;
- Save/Add separated from **Export** and **Flash**—saving changes local library state only;
- persistent **Needs attention** recovery for exact app-owned preset artifacts that can no longer be confidently associated with a current My EQ item;
- conservative recovery into Personal EQs without scanning or deleting arbitrary external files.

At v0.6.0 publication, TRN Black Pearl automatic physical reattach, Restore defaults, and the final DEVICE feedback/layout flow had passed focused Pixel 9 testing on their exact signed evidence candidates. Restore defaults is deliberately an **EQ Library preset**, not a claim about TRN factory defaults. See the v0.6 status/checklist documents for that historical evidence record.

## What you can do

### Find and organize EQs

- Browse headphone EQs by **Manufacturer → Model**, with deeper identity only when a source genuinely verifies it.
- Search the canonical library without hiding valid curves merely because the active output cannot represent them.
- Browse standalone **General EQs** in Sound, Genre, and Utility groups when the source itself supports that classification.
- Keep one local **My EQs** collection independent of the current output target.
- Favorite saved EQs and locally Hide/Unhide canonical EQ lineages without deleting source history.
- Review new or changed EQs explicitly. **Notify me about new EQs** is attention-only and never silently selects a profile.
- Import personal Equalizer APO / AutoEq-style parametric text from paste or Android file selection.

### Convert and export

EQ Library supports multiple output contexts from one canonical source representation.

| Group | Outputs in v0.7.0 |
| --- | --- |
| **Hardware DACs** | TRN Black Pearl, FiiO JA11, SIMGOT EW300 DSP |
| **Apps** | USB Audio Player PRO / ToneBoosters, Poweramp / Poweramp Equalizer, Wavelet, TOPPING Tune, EasyEffects, Equalizer APO |
| **Universal formats** | AutoEq / Equalizer APO Parametric, AutoEq GraphicEQ |

The active output is an **operating/action context**, not a catalog or My EQs ownership filter. Switching outputs changes target compatibility, conversion/fidelity, export behavior, and hardware actions without hiding otherwise valid canonical EQs or changing which EQs the user saved.

In v0.7.0, **Save/Add changes local My EQs state only**. File export is an explicit Export action and hardware writes are explicit Flash actions. This prevents selecting a target from silently becoming a library-membership or storage operation.

Exports use Android's system folder picker. EQ Library does not request broad storage access, does not write into another app's private storage, and manages only files it can prove it created.

## Hardware support

| Device | EQ Library behavior | v0.7.0 status |
| --- | --- | --- |
| **TRN Black Pearl** | `.txt` export, 10-band Direct Flash, playback-gain/headroom handling, final native readback verification, and Reset EQ to flat | **Verified for the tested AFUL Explorer transaction, unit, starting state, and Pixel 9 session** |
| **FiiO JA11** | Hardware-only five-band User 1 editor, global EQ gain, Apply/Save/readback, Flash, and Reset EQ to flat | **Software path verified · broader power-cycle qualification unclaimed** |
| **SIMGOT EW300 DSP cable** | Five-band Peak readback/capture, guarded editor Apply, persistent Flash, Reset, reconnect verification, and operation reports | **Evidence-bounded exact-device support** |

Peak-only EW300 readback/capture is evidence-backed; non-Peak snapshots fail closed, and ordinary playback gain is excluded from captured EQs. Hardware claims remain limited to the exact device and evidence recorded in the [v0.7.0 release notes](docs/releases/v0.7.0.md) and validation ledgers.

Direct Flash is OFF by default for newly introduced hardware outputs. Add/Save never automatically writes to a DAC. Flash and Reset require explicit confirmation where the maintained device safety contract requires it.

The v0.7.0 hardware scope does **not** include firmware update, bootloader, cross-flash, or
unrelated DAC-management commands. More generally, any such control must be exposed when the
exact hardware profile genuinely supports it and safe protocol evidence is established; an
unverified control is kept absent and unclaimed until then.

### TRN Black Pearl

Black Pearl file export and Direct Flash use the same shared 10-band device representation. Profiles above 10 source bands are adapted from the **complete source response** instead of silently taking the first 10.

Flash replaces EQ Library's prior playback-gain adjustment instead of stacking repeated attenuation. Protocol-encodable filter gains outside the currently validated approximately ±10 dB listening range are preserved rather than clamped and require an explicit exact-value caution before Flash.

**Reset EQ to flat** operates on the DAC's current EQ slot, writes all 10 bands flat, persists the slot, and then removes only the playback-gain adjustment previously tracked as applied by EQ Library. Unrelated DAC settings remain outside the transaction.

My DAC exposes a physically qualified **EQ Library Restore defaults** action for Black Pearl. It restores 50% volume, FAST-LL, HIGH gain, CLASS AB, centered balance, and microphone gain 0 dB. **Also reset EQ to flat** is optional and OFF by default. With it OFF, the current hardware EQ remains unchanged; with it ON, the separately qualified EQ-flat reset is used. The selected values are app-owned restore targets and are **not** presented as verified TRN factory defaults.

Black Pearl `.txt` export uses the verified pyBlackPearl `PK / LS / HS` peak/shelf syntax and preserves the true derived preamp. Third-party importer limitations are disclosed rather than silently changing canonical EQ data or the Direct Flash plan.

### FiiO JA11

v0.7.0 includes a strict FiiO JA11 USB identity, a five-band User 1 editor, explicit Review → Apply controls, one-Save persistence, final readback, and failure handling within the existing transaction boundary.

The software path is verified. Physical evidence remains limited to the exact reports and candidate identities recorded in the repository ledger; broader JA11 power-cycle qualification is not claimed.

Historical JCALLY JM12 protocol material may remain in the repository for reference, but JCALLY is not a current v0.7 hardware-support claim.

## Fidelity and safety

Each canonical EQ is evaluated against the active output as:

- **Exact** — the source is natively representable at the target's actual limits and resolution without target-side acoustic alteration or generated headroom.
- **Optimized** — EQ Library deterministically derives a faithful target representation, such as native target rounding, complete-response fitting, or generated target headroom.
- **Not suitable / Not exportable** — a safe, faithful representation cannot be produced.

Canonical source data remains complete and unchanged even when an output has tighter limits. Unsupported active filters, unsafe values, or quality-gate failures are rejected instead of silently dropped or clamped.

For finite hardware, TRN Black Pearl, FiiO JA11, and the historical stock JCALLY JM12 implementation use a shared deterministic response adapter. It preserves a direct/native representation when possible and otherwise fits the **complete source response** within the target band budget under fixed response-error gates.

The UAPP/ToneBoosters path intentionally remains different: ToneBoosters supports at most 10 bands, so that format keeps its established first-10 source-priority rule while the complete canonical source remains stored locally.

## Catalog, offline behavior, and privacy

EQ Library uses a source-agnostic canonical catalog with provenance, verification state, acoustic deduplication, and immutable revisions. Source lanes include OPRA, AutoEq, qualified creator/repository sources, public community EQs, and qualified General-EQ sources.

The catalog is treated as a **living archive**: a genuinely published canonical EQ or acoustic revision is not silently erased merely because an upstream source moves, pauses, or disappears.

Normal Android runtime does **not** scrape GitHub, Reddit, forums, or other community sites. Source discovery and catalog publication happen upstream; the app consumes the validated published catalog.

Selections, settings, generated-preset state, favorites, hidden-EQ preferences, Needs attention recovery state, and conversion remain local on the device. Runtime network access is limited to validated catalog acquisition/currentness and public GitHub Release metadata used for update checks.

See [PRIVACY.md](PRIVACY.md) for the full privacy statement.

## Android architecture

EQ Library follows modern Android development boundaries so UI, domain rules, storage, networking, and USB hardware behavior remain testable and maintainable:

- Kotlin + Jetpack Compose
- immutable screen state owned by `EqLibraryViewModel`
- `MainActivity` limited to Android lifecycle/platform responsibilities
- Room for durable app-owned state
- Preferences DataStore for local settings
- WorkManager for background catalog/currentness work
- Storage Access Framework for user-controlled file export
- Android USB host/HID integration behind device-specific data/platform adapters
- source-independent EQ and target-specific derivation kept in the domain layer

Compose does not implement DSP fitting, source parsing, storage ownership, or USB wire protocol rules. See [docs/ARCHITECTURE.md](docs/ARCHITECTURE.md) and [docs/V0.6_LIBRARY_OWNERSHIP_AND_RECOVERY.md](docs/V0.6_LIBRARY_OWNERSHIP_AND_RECOVERY.md) for the maintained architecture and My EQs ownership/recovery contract.

## Development setup

EQ Library includes a checked-in Gradle Wrapper, so a globally installed Gradle is not required. The wrapper uses Gradle `9.4.1` with Android Gradle Plugin `9.2.0`.

Install JDK 17 and an Android SDK with API 36, Build Tools 36.0.0, Platform Tools, and an emulator image if instrumented tests are needed. Configure SDK discovery with `ANDROID_HOME` or `ANDROID_SDK_ROOT`, or create the ignored `local.properties` file with an environment-specific `sdk.dir` value. Do not commit SDK paths, Gradle distributions, or caches.

From macOS or Linux, use the wrapper directly when the JDK and SDK are already configured:

```sh
./gradlew :app:testDebugUnitTest
./gradlew lintDebug
./gradlew assembleDebug
```

For Codex on macOS, `tools/codex-android` selects the local JDK and Android SDK and routes Gradle and Android user caches to writable temporary locations. It also exposes `adb`, `android`, `avdmanager`, and `emulator`:

```sh
./tools/codex-android :app:testDebugUnitTest
./tools/codex-android lintDebug
./tools/codex-android assembleDebug
./tools/codex-android adb devices
./tools/codex-android emulator -list-avds
```

Run `:app:connectedDebugAndroidTest` only with an attached device or running emulator. Instrumented-test results are evidence only when the command actually executes against that device or emulator. See [AGENTS.md](AGENTS.md) and the [Codex project runbook](docs/CHATGPT_PROJECT_RUNBOOK.md) for the complete development and validation workflow.

## Validation and release discipline

The public v0.7.0 release was built, tested, signed, and published through the trusted main-only workflow. The public tag points to `4f325d673159b40515086fe5143df12b29ddb076`; the executable behavior was tested on `e1ab5fa5a65dc2d64624d871ac53d436f792ea6a`, and the tagged source adds documentation-only closeout synchronization.

- Signed workflow: [GitHub Actions run 36381764266](https://github.com/weekssa/OPRA-EQ-for-UAPP/actions/runs/36381764266)
- Public release: [EQ Library v0.7.0](https://github.com/weekssa/OPRA-EQ-for-UAPP/releases/tag/v0.7.0)
- Public APK SHA-256: `27dada499bcbf9be9bd21d1349164858c93a5d2b83f78fd61134de13b4eb4025`
- Signer certificate SHA-256: `65c1c1256dae3c49e3548f334c91f0ba991969e9be9e0b223ba4e253d2114747`

The complete release gates and hardware evidence boundaries are recorded in the [v0.7.0 release notes](docs/releases/v0.7.0.md) and [public release checklist](docs/PUBLIC_RELEASE_CHECKLIST.md). `actionlint` was not available locally; no workflow file changed during final closeout, and the trusted remote release workflow passed.

Installable releases use SemVer during the `0.x` development series. The first stable release is reserved for `v1.0.0`.

## Data, attribution, and trademarks

EQ Library preserves creator/source provenance whenever available. OPRA-derived data is attributed and licensed separately; see [DATA_LICENSE.md](DATA_LICENSE.md). Software and third-party provenance are documented in [NOTICE](NOTICE).

USB Audio Player PRO/UAPP, ToneBoosters, OPRA, Roon Labs, TRN, FiiO, JCALLY, TOPPING, Poweramp, Wavelet, Equalizer APO, EasyEffects, AutoEq, ParaEQ, manufacturer names, and headphone/product names are used only for compatibility, attribution, or source identification. **EQ Library is an independent project and is not affiliated with or endorsed by those projects, companies, applications, or manufacturers.**

## Documentation

- [docs/releases/v0.7.0.md](docs/releases/v0.7.0.md) — v0.7.0 release notes and validation record
- [docs/releases/v0.6.0.md](docs/releases/v0.6.0.md) — v0.6.0 release notes and validation record
- [docs/releases/v0.5.0.md](docs/releases/v0.5.0.md) — v0.5.0 release notes and validation record
- [CHANGELOG.md](CHANGELOG.md) — release history and notable changes
- [docs/ARCHITECTURE.md](docs/ARCHITECTURE.md) — Android/MAD architecture and invariants
- [docs/CHATGPT_PROJECT_RUNBOOK.md](docs/CHATGPT_PROJECT_RUNBOOK.md) — maintained product and execution source of truth
- [docs/V0.6_LIBRARY_OWNERSHIP_AND_RECOVERY.md](docs/V0.6_LIBRARY_OWNERSHIP_AND_RECOVERY.md) — v0.6 device-agnostic My EQs / Needs attention authority
- [docs/V0.7_EW300_DSP_IMPLEMENTATION_PLAN.md](docs/V0.7_EW300_DSP_IMPLEMENTATION_PLAN.md) — EW300 DSP scope, evidence gates, implementation sequence, and validation record
- [docs/V0.6_PREMIUM_UX_AUDIT_BLUEPRINT.md](docs/V0.6_PREMIUM_UX_AUDIT_BLUEPRINT.md) — design-only v0.6 polish blueprint; approval required before major UI changes
- [docs/PUBLIC_RELEASE_CHECKLIST.md](docs/PUBLIC_RELEASE_CHECKLIST.md) — GitHub release gates
- [docs/RELEASE_SIGNING.md](docs/RELEASE_SIGNING.md) — permanent APK signing process
- [PRIVACY.md](PRIVACY.md) — privacy policy
- [CONTRIBUTING.md](CONTRIBUTING.md) — contribution and validation expectations
- [SECURITY.md](SECURITY.md) — security-reporting guidance

## Feedback and contributions

Use [GitHub Issues](https://github.com/weekssa/OPRA-EQ-for-UAPP/issues) to report a problem, suggest an improvement, or submit an EQ source. See [CONTRIBUTING.md](CONTRIBUTING.md) for project expectations.

Do not post credentials, signing keys, tokens, private files, or other sensitive information in an issue. Security-sensitive reports should follow [SECURITY.md](SECURITY.md).

## License

The application source code, tests, and project documentation are licensed under the **Apache License 2.0**. See [LICENSE](LICENSE). Third-party/source data may have separate terms documented in [DATA_LICENSE.md](DATA_LICENSE.md) and [NOTICE](NOTICE).
