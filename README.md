# EQ Library

**EQ Library** is a native Android app for finding, saving, converting, exporting, and—on supported hardware—directly applying parametric EQ presets.

The project began as **OPRA EQ for UAPP**. The repository and Android application ID remain `com.weekssa.opraeqforuapp` so existing installations continue to upgrade normally, but the product is now source-agnostic: **OPRA is one attributed EQ source, not the product identity.**

EQ Library ships with **zero headphone or EQ profiles bundled in the APK**. It downloads a validated canonical catalog, keeps a last-known-good local cache, and remains useful offline after the first successful sync.

## Current release

**v0.5.0** is the current public Android release.

[Download the latest signed release](https://github.com/weekssa/OPRA-EQ-for-UAPP/releases/latest)

- Android 8.0 / API 26 or newer
- Signed with the project's permanent Android release identity for in-place upgrades
- No Google Play account, EQ Library account, or cloud account required
- No analytics or telemetry

The public v0.5.0 APK is `EQ-Library-v0.5.0.apk`. Its SHA-256 is:

`58e6ac5c62f9af1caf354f97cf2e7d9e2bcddea9937fac3c35532c279cd429eb`

Android may ask you to allow installation from the browser or file manager used to open the APK because GitHub Releases are installed outside an app store. EQ Library itself does not request package-install permission and never silently installs updates.

## What you can do

### Find and organize EQs

- Browse headphone EQs by **Manufacturer → Model**, with deeper identity only when a source genuinely verifies it.
- Search the canonical library without hiding valid curves merely because the active output cannot represent them.
- Browse standalone **General EQs** in Sound, Genre, and Utility groups when the source itself supports that classification.
- Keep separate **My EQs** collections for each output.
- Favorite saved EQs and locally Hide/Unhide canonical EQ lineages without deleting source history.
- Review new or changed EQs explicitly. **Notify me about new EQs** is attention-only and never silently selects a profile.
- Import personal Equalizer APO / AutoEq-style parametric text from paste or Android file selection.

### Convert and export

EQ Library supports multiple output contexts from one canonical source representation.

| Group | Outputs in v0.5.0 |
| --- | --- |
| **Hardware DACs** | TRN Black Pearl, FiiO JA11, stock-firmware JCALLY JM12 |
| **Apps** | USB Audio Player PRO / ToneBoosters, Poweramp / Poweramp Equalizer, Wavelet, TOPPING Tune, EasyEffects, Equalizer APO |
| **Universal formats** | AutoEq / Equalizer APO Parametric, AutoEq GraphicEQ |

The active output is an **operating context**, not a catalog filter. Switching outputs changes My EQs membership, conversion/fidelity, export behavior, and hardware controls without hiding otherwise valid canonical EQs.

For file-capable outputs, **Add/Save performs the initial export** once a Storage Access Framework folder is available. Normal Export / Export all actions are recovery tools and stay out of the way while app-managed files are current.

Exports use Android's system folder picker. EQ Library does not request broad storage access, does not write into another app's private storage, and manages only files it can prove it created.

## Hardware support

| Device | EQ Library behavior | v0.5.0 status |
| --- | --- | --- |
| **TRN Black Pearl** | `.txt` export, 10-band Direct Flash, playback-gain/headroom handling, Reset EQ to flat | **Hardware-qualified for the v0.5 path** |
| **FiiO JA11** | Hardware-only 5-band Direct Flash, global EQ gain, Apply/Save/readback, Reset EQ to flat | **Hardware validation pending** |
| **JCALLY JM12 (stock firmware)** | Hardware-only 5-band Direct Flash, readback verification, tracked playback-gain adjustment, Reset EQ to flat | **Hardware validation pending · power-cycle persistence unclaimed** |

Direct Flash is OFF by default for newly introduced hardware outputs. Add/Save never automatically writes to a DAC. Flash and Reset require explicit confirmation.

EQ Library does **not** include firmware update, bootloader, cross-flash, or unrelated DAC-management commands in v0.5.0.

### TRN Black Pearl

Black Pearl file export and Direct Flash use the same shared 10-band device representation. Profiles above 10 source bands are adapted from the **complete source response** instead of silently taking the first 10.

Flash replaces EQ Library's prior playback-gain adjustment instead of stacking repeated attenuation. Protocol-encodable filter gains outside the currently validated approximately ±10 dB listening range are preserved rather than clamped and require an explicit exact-value caution before Flash.

**Reset EQ to flat** operates on the DAC's current EQ slot, writes all 10 bands flat, persists the slot, and then removes only the playback-gain adjustment previously tracked as applied by EQ Library. Unrelated DAC settings remain outside the transaction.

Black Pearl `.txt` export uses the verified pyBlackPearl `PK / LS / HS` peak/shelf syntax and preserves the true derived preamp. Third-party importer limitations are disclosed rather than silently changing canonical EQ data or the Direct Flash plan.

### FiiO JA11 and JCALLY JM12

v0.5.0 includes implemented Direct Flash paths for FiiO JA11 on normal FiiO firmware and JCALLY JM12 on stock firmware. Each has its own strict USB identity, device representation, confirmation flow, and readback/failure handling.

The physical devices were not available for the required Pixel 9 hands-on qualification before v0.5.0 publication, so both remain visibly **Hardware validation pending**. Stock JM12 power-cycle persistence is not claimed until it is established on hardware.

## Fidelity and safety

Each canonical EQ is evaluated against the active output as:

- **Exact** — the source is natively representable at the target's actual limits and resolution without target-side acoustic alteration or generated headroom.
- **Optimized** — EQ Library deterministically derives a faithful target representation, such as native target rounding, complete-response fitting, or generated target headroom.
- **Not suitable / Not exportable** — a safe, faithful representation cannot be produced.

Canonical source data remains complete and unchanged even when an output has tighter limits. Unsupported active filters, unsafe values, or quality-gate failures are rejected instead of silently dropped or clamped.

For finite hardware, TRN Black Pearl, FiiO JA11, and stock JCALLY JM12 use a shared deterministic response adapter. It preserves a direct/native representation when possible and otherwise fits the **complete source response** within the target band budget under fixed response-error gates.

The UAPP/ToneBoosters path intentionally remains different: ToneBoosters supports at most 10 bands, so that format keeps its established first-10 source-priority rule while the complete canonical source remains stored locally.

## Catalog, offline behavior, and privacy

EQ Library uses a source-agnostic canonical catalog with provenance, verification state, acoustic deduplication, and immutable revisions. Source lanes include OPRA, AutoEq, qualified creator/repository sources, public community EQs, and qualified General-EQ sources.

The catalog is treated as a **living archive**: a genuinely published canonical EQ or acoustic revision is not silently erased merely because an upstream source moves, pauses, or disappears.

Normal Android runtime does **not** scrape GitHub, Reddit, forums, or other community sites. Source discovery and catalog publication happen upstream; the app consumes the validated published catalog.

Selections, settings, generated-preset state, favorites, hidden-EQ preferences, and conversion remain local on the device. Runtime network access is limited to validated catalog acquisition/currentness and public GitHub Release metadata used for update checks.

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

Compose does not implement DSP fitting, source parsing, storage ownership, or USB wire protocol rules. See [docs/ARCHITECTURE.md](docs/ARCHITECTURE.md) for the maintained technical architecture.

## Validation and release discipline

v0.5.0 completed both its implementation phase and Pixel 9 release-candidate testing before publication. TRN Black Pearl's v0.5 hardware/DSP regression passed; JA11/JM12 remain explicitly pending hardware qualification.

The public v0.5.0 release was rebuilt, tested, signed, and published from exact source commit:

`ff2fa351d5f38f9dcf37a77859f1e988bbdb76a8`

The release workflow verified the same permanent signing identity pinned in [`release-signing-cert.sha256`](release-signing-cert.sha256).

Installable releases use SemVer during the `0.x` development series. The first stable release is reserved for `v1.0.0`.

## Data, attribution, and trademarks

EQ Library preserves creator/source provenance whenever available. OPRA-derived data is attributed and licensed separately; see [DATA_LICENSE.md](DATA_LICENSE.md). Software and third-party provenance are documented in [NOTICE](NOTICE).

USB Audio Player PRO/UAPP, ToneBoosters, OPRA, Roon Labs, TRN, FiiO, JCALLY, TOPPING, Poweramp, Wavelet, Equalizer APO, EasyEffects, AutoEq, ParaEQ, manufacturer names, and headphone/product names are used only for compatibility, attribution, or source identification. **EQ Library is an independent project and is not affiliated with or endorsed by those projects, companies, applications, or manufacturers.**

## Documentation

- [docs/releases/v0.5.0.md](docs/releases/v0.5.0.md) — v0.5.0 release notes and validation record
- [CHANGELOG.md](CHANGELOG.md) — release history and notable changes
- [docs/ARCHITECTURE.md](docs/ARCHITECTURE.md) — Android/MAD architecture and invariants
- [docs/CHATGPT_PROJECT_RUNBOOK.md](docs/CHATGPT_PROJECT_RUNBOOK.md) — maintained product and execution source of truth
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
