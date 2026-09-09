# EQ Library

**EQ Library** is a standalone native Android app for finding, saving, converting, exporting, and—where supported—directly applying parametric EQ presets.

The project began as **OPRA EQ for UAPP**, and the repository keeps that historical name and the Android application ID `com.weekssa.opraeqforuapp` so existing installs continue to upgrade normally. The product itself is now source-agnostic: **OPRA is one attributed EQ source, not the product identity.**

EQ Library ships with **zero headphone/EQ profiles bundled in the APK**. It downloads a validated canonical catalog, keeps a last-known-good local cache, and remains usable offline after the first successful sync.

## Download

Public Android builds are distributed through [GitHub Releases](https://github.com/weekssa/OPRA-EQ-for-UAPP/releases/latest).

- Android 8.0 / API 26 or newer
- Signed with one permanent project release identity for in-place upgrades
- No Google Play account, EQ Library account, or cloud account required

Android may ask you to allow installation from the browser or file manager used to open the APK because GitHub releases are installed outside an app store. EQ Library itself does not request package-install permission and never silently installs updates.

## What EQ Library does

### Find and manage EQs

- Browse headphone EQs by **Manufacturer → Model**, with deeper identity only when a source genuinely verifies it.
- Search the canonical library without filtering valid curves just because the current output cannot represent them.
- Browse standalone **General EQs** in Sound, Genre, and Utility groups when the source itself supports that classification.
- Keep separate **My EQs** collections for each output.
- Favorite saved EQs directly from My EQs or EQ Library.
- Hide canonical EQ lineages locally without deleting them from the public archive or disturbing already-saved presets.
- Review new or changed EQs explicitly. **Notify me about new EQs** is attention-only: it never silently selects a profile.

### Canonical multi-source catalog

EQ Library uses a source-agnostic canonical catalog with provenance, verification state, acoustic deduplication, and immutable revisions. Current source lanes include OPRA, AutoEq, qualified creator/repository sources, public community EQs, and qualified General-EQ sources such as the MIT-licensed ParaEQ built-ins.

The published catalog is treated as a **living archive**: once a genuine canonical EQ or genuine acoustic revision has been validly published, ordinary source movement, disappearance, pausing, or retirement does not erase that acoustic history.

Normal Android runtime does **not** scrape GitHub, Reddit, forums, or other community sites. Source discovery and catalog publication happen upstream; the app downloads only the validated published catalog.

### Output contexts

The active output is an operating context, not a library filter. Selectable v0.5 outputs are grouped as:

**Hardware DACs**

- **TRN Black Pearl** — `.txt` export plus qualified optional Direct Flash / Reset EQ to flat.
- **FiiO JA11** — hardware-only 5-band Direct Flash / Reset path; **Hardware validation pending**.
- **JCALLY JM12** on stock firmware — hardware-only 5-band Direct Flash / Reset path; **Hardware validation pending · persistence pending**.

**Apps**

- **USB Audio Player PRO / ToneBoosters**
- **Poweramp / Poweramp Equalizer**
- **Wavelet**
- **TOPPING Tune**
- **EasyEffects**
- **Equalizer APO**

**Universal formats**

- **AutoEq / Equalizer APO Parametric**
- **AutoEq GraphicEQ**

Each canonical EQ is evaluated for the active output as **Exact**, **Optimized**, or **Not suitable / Not exportable**. Canonical source data remains intact even when an output has tighter limits.

### Hardware qualification status

**TRN Black Pearl is hardware-qualified for the v0.5 path.** The v0.5 regression covered Exact Flash, complete-response Optimized Flash, power-cycle persistence, Reset-to-flat persistence, and the explicit caution path for protocol-encodable filter gains outside the currently validated listening range.

**FiiO JA11 and JCALLY JM12 remain Hardware validation pending.** Their software paths are implemented, but the physical devices are not yet available for the required Pixel 9 hands-on qualification. That physical qualification is deferred to the next incremental release and is not a v0.5.0 publication blocker. Stock JM12 power-cycle persistence is not claimed until it is physically established.

Direct Flash is OFF by default for newly introduced hardware outputs. Add/Save never automatically writes hardware. No firmware update, bootloader, cross-flash, cloud/share-code, or unrelated DAC-control functionality is included.

### Add, export, and file ownership

For file-based outputs, **Add/Save performs the initial export**. Normal Export / Export all controls are recovery actions and stay out of the way while app-managed files are current.

Exports use Android's Storage Access Framework/system folder picker. EQ Library does not request broad storage access and does not write into another app's private storage.

Export ownership follows stable preset identity plus the exact document URI returned by Android. Provider-adjusted filenames are tracked safely, unknown same-name files are never overwritten or deleted, and cleanup is limited to files the app can prove it created.

Hardware-only outputs do not invent export files; their derived representation remains local until the user explicitly confirms Direct Flash.

### UAPP / ToneBoosters

The native Kotlin UAPP/ToneBoosters converter preserves source preamp, frequency, gain, Q, filter order/priority, creator/details, and attribution. ToneBoosters output is limited to 10 bands, so profiles above that limit use the first 10 source-priority bands and are reported as Optimized while the complete canonical source remains local.

ToneBoosters XML is generated deterministically and kept ISO-8859-1-safe while full Unicode source metadata remains stored locally.

### Finite-hardware adaptation

TRN Black Pearl, FiiO JA11, and stock JCALLY JM12 use one deterministic finite-hardware response adapter.

If a source is exactly representable at the target's real limits and quantization, EQ Library preserves it as **Exact**. If only native target rounding is required, EQ Library preserves the source filter structure and reports **Optimized**. Otherwise it fits the **complete source response** to the available target bands and requires fixed response-error gates. A profile that cannot be represented safely and faithfully becomes **Not suitable** instead of being silently truncated or clamped.

This finite-hardware behavior is separate from the established UAPP/ToneBoosters first-10 source-priority contract.

### TRN Black Pearl Direct Flash

EQ Library includes optional **Direct Flash** for the TRN Black Pearl when that output is active and Direct Flash is enabled in Settings.

The v0.5 path derives both Black Pearl file export and Direct Flash from the same shared 10-band hardware representation. Profiles above 10 source bands are adapted from the **complete source response** rather than simply taking the first 10.

Flash always requires confirmation before writing. EQ Library replaces its prior playback-gain adjustment rather than stacking repeated attenuation. Protocol-encodable filter gains outside the currently validated approximately ±10 dB range are never silently clamped; they require an explicit exact-value caution and **Flash anyway** confirmation. Unrelated DAC settings are outside the Flash path.

My EQs also provides **Reset EQ to flat** beside the Black Pearl connection control. The confirmed reset operates on the DAC's current EQ slot, writes all 10 bands flat, latches/saves the slot, and then removes only the playback-gain adjustment previously tracked as applied by EQ Library. The fail-safe ordering keeps the prior attenuation in place until the slot is confirmed flat, and unrelated DAC settings remain outside the reset path.

Black Pearl `.txt` export uses the verified pyBlackPearl `PK / LS / HS` shelf/peak syntax and preserves the true derived preamp. Third-party importer limitations are disclosed instead of being silently baked into the canonical EQ or Direct Flash path.

### FiiO JA11 and JCALLY JM12 Direct Flash

v0.5 includes separate hardware-only Direct Flash paths for **FiiO JA11** on normal FiiO firmware and **JCALLY JM12** on stock firmware. Each has its own strict USB identity, readback verification, five-band target representation, confirmation flow, and Reset EQ to flat path.

Both remain visibly **Hardware validation pending** until their future exact-candidate Pixel 9 hands-on checklists pass. JM12 persistence across unplug/full power cycle is explicitly unclaimed. EQ Library does not invent a JA11/JM12 preset-file format and does not include firmware or cross-flash controls.

### TOPPING Tune

**TOPPING Tune** is a selectable Apps output using standard AutoEq-style parametric `.txt` with a 10-band target. Over-budget profiles use the shared complete-response fitting path rather than first-10 truncation, and an out-of-range source preamp is rejected rather than silently clamped.

TOPPING's public documentation does not establish the downstream hardware storage quantization applied after import, so v0.5 conservatively reports TOPPING Tune output as **Optimized** rather than claiming unproven Exact downstream fidelity.

### Personal PEQ import

My EQs includes a compact **+ Import** flow for explicit paste or Android file selection of **Equalizer APO / AutoEq parametric text**.

The importer recognizes contents rather than trusting the filename extension, previews the canonical interpretation, preserves an omitted preamp as null, keeps the complete supported filter set, and blocks malformed or unsupported active filters rather than silently importing a partial EQ. Successful Save & export never automatically flashes hardware.

## Offline behavior and privacy

No account is required. EQ Library contains **no analytics or telemetry**.

Selections, settings, generated-preset state, favorites, hidden-EQ preferences, and conversion remain local on the device. Runtime network access is limited to the validated EQ Library catalog and public GitHub Release metadata used for update checks.

The app uses its last-known-good cached catalog when offline or when a refresh fails. Manual **Refresh now** remains available, with approximately daily background/currentness checks as a backup.

See [PRIVACY.md](PRIVACY.md) for the full privacy statement.

## Current development status

The current release-preparation line is **v0.5.0** (`versionCode 5`). Its major changes are the unified output registry, complete-response finite-hardware adapter, additional app/universal export targets, TOPPING Tune import support, implemented JA11/JM12 Direct Flash paths, Black Pearl v0.5 adaptation improvements, and the internal MAD-style Android architecture refactor.

The pinned v0.5 software/signing candidate containing the MAD refactor passed Android CI, CodeQL, signed-beta verification, catalog currentness, priority community coverage, and dependency submission. TRN Black Pearl's v0.5 hardware/DSP regression is complete. JA11/JM12 physical qualification is deferred to the next incremental release and remains clearly labeled pending rather than blocking v0.5.0.

The final public v0.5.0 APK is not published merely from the beta artifact. Release preparation still requires the final PR head to pass its applicable checks, merge to `main`, final `main` validation, and the controlled signed-release workflow from the exact finalized source.

The application ID remains `com.weekssa.opraeqforuapp`, and installable releases must keep the pinned permanent signing identity recorded in [`release-signing-cert.sha256`](release-signing-cert.sha256).

## Android and build baseline

- Native Kotlin + Jetpack Compose
- Application ID: `com.weekssa.opraeqforuapp`
- minSdk: 26
- compileSdk / targetSdk: 36
- Java: 17
- Room for durable app-owned saved/export state
- Preferences DataStore for local settings/visibility state
- WorkManager for approximately daily catalog reconciliation backup
- Android Storage Access Framework for file export
- Android USB host/HID integration for supported Direct Flash targets

Normal Android CI covers unit tests, Android lint, debug assembly, and unsigned release assembly. Separate release workflows build and verify permanently signed candidate/public APKs.

## Data, attribution, and trademarks

EQ Library preserves creator/source provenance whenever available. OPRA-derived data is attributed and licensed separately; see [DATA_LICENSE.md](DATA_LICENSE.md). Software and third-party provenance are documented in [NOTICE](NOTICE).

USB Audio Player PRO/UAPP, ToneBoosters, OPRA, Roon Labs, TRN, FiiO, JCALLY, TOPPING, Poweramp, Wavelet, Equalizer APO, EasyEffects, AutoEq, ParaEQ, manufacturer names, and headphone/product names are used only for compatibility, attribution, or source identification. **EQ Library is an independent project and is not affiliated with or endorsed by those projects, companies, applications, or manufacturers.**

## License

The application source code, tests, and project documentation are licensed under the **Apache License 2.0**. See [LICENSE](LICENSE). Third-party/source data may have separate terms documented in [DATA_LICENSE.md](DATA_LICENSE.md) and [NOTICE](NOTICE).

## Project documentation

- [CHANGELOG.md](CHANGELOG.md) — release history and notable changes
- [docs/releases/v0.5.0.md](docs/releases/v0.5.0.md) — prepared v0.5.0 release notes
- [docs/V0.5_HANDS_ON_RELEASE_CHECKLIST.md](docs/V0.5_HANDS_ON_RELEASE_CHECKLIST.md) — Pixel 9 v0.5 release-candidate hands-on checklist
- [docs/releases/v0.4.0.md](docs/releases/v0.4.0.md) — v0.4.0 release notes
- [docs/releases/v0.3.0.md](docs/releases/v0.3.0.md) — v0.3.0 release notes
- [PRIVACY.md](PRIVACY.md) — public privacy policy
- [CONTRIBUTING.md](CONTRIBUTING.md) — contribution and validation expectations
- [SECURITY.md](SECURITY.md) — security-reporting guidance
- [docs/PUBLIC_RELEASE_CHECKLIST.md](docs/PUBLIC_RELEASE_CHECKLIST.md) — GitHub release gates
- [docs/RELEASE_SIGNING.md](docs/RELEASE_SIGNING.md) — permanent APK signing process
- [docs/ARCHITECTURE.md](docs/ARCHITECTURE.md) — architecture and invariants
- [docs/CHATGPT_PROJECT_RUNBOOK.md](docs/CHATGPT_PROJECT_RUNBOOK.md) — maintained product/UX source of truth

## Feedback and contributions

Use [GitHub Issues](https://github.com/weekssa/OPRA-EQ-for-UAPP/issues) to report a problem, suggest an improvement, or submit an EQ source. See [CONTRIBUTING.md](CONTRIBUTING.md) for project expectations.

Do not post credentials, signing keys, tokens, private files, or other sensitive information in an issue. Security-sensitive reports should follow [SECURITY.md](SECURITY.md).
