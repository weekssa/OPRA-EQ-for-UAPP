# OPRA EQ for UAPP

**OPRA EQ for UAPP** is a standalone native Android app that converts user-selected [OPRA](https://github.com/opra-project/OPRA) parametric EQ profiles into USB Audio Player PRO / ToneBoosters XML presets locally on the device.

The app ships with **zero headphone profiles bundled in the APK**. It downloads the supported OPRA runtime catalog, validates and caches it locally, and works offline after the first successful sync.

## Status

Current development version: **0.1.0**

The Android implementation and its primary Pixel 9 device-validation gate are complete. End-to-end testing has covered first launch, offline catalog reuse, Browse/Search, managed headphone selections, XML export through Android's Storage Access Framework, app-owned preset cleanup, accessibility/appearance checks, and successful preset import into USB Audio Player PRO/ToneBoosters.

A signed public binary release has not been published yet. Release signing and GitHub Release packaging are the remaining distribution steps. See [docs/PUBLIC_RELEASE_CHECKLIST.md](docs/PUBLIC_RELEASE_CHECKLIST.md).

## What it does

- Downloads the OPRA `database_v1.jsonl` runtime catalog from `https://opra.roonlabs.net/database_v1.jsonl`.
- Caches a last-known-good catalog locally and supports offline Browse/Search after the first sync.
- Lets you manage selected OPRA profiles under **My Headphones**.
- Supports per-headphone future-profile behavior, exact exclusions, and compatibility filtering.
- Converts supported OPRA parametric EQ profiles to deterministic UAPP/ToneBoosters XML entirely on-device.
- Preserves OPRA preamp, frequency, gain, Q, priority/order, creator, details, source metadata, and attribution.
- Preserves the first 10 OPRA-priority bands when a profile exceeds ToneBoosters' 10-band limit and shows an explicit limitation warning.
- Exports through Android's system folder picker without broad storage permission.
- Tracks only files created by this app and never silently overwrites unknown same-name files.
- Checks for OPRA catalog changes approximately daily without requesting notification permission.
- Can check public GitHub Release metadata for app updates; it never silently downloads or installs APKs.

## Supported conversion filters

The currently proven ToneBoosters mapping supports:

- `peak_dip`
- `low_shelf`
- `high_shelf`

OPRA filter types without a proven faithful mapping remain visible as **Not compatible** and are never silently approximated or dropped.

## Privacy

No account is required. The app contains no analytics or telemetry.

Headphone selections, settings, generated-preset state, and conversion remain on the device. Network access is used for:

1. the OPRA runtime catalog; and
2. public GitHub Release metadata used by the optional update check.

See [PRIVACY.md](PRIVACY.md) for the full privacy statement.

## Android and build requirements

- Application ID: `com.weekssa.opraeqforuapp`
- Minimum Android version: API 26
- Compile/target SDK: API 36
- Java: 17
- Kotlin + Jetpack Compose
- Room for app-owned managed state
- WorkManager for approximately daily catalog reconciliation
- Android Storage Access Framework for export

The repository's Android CI runs:

```text
:app:testDebugUnitTest
:app:lintDebug
:app:assembleDebug
```

## Data, attribution, and trademarks

OPRA manufacturer, product, and EQ data is provided under **CC BY-SA 4.0**. The app preserves individual creator/source information where OPRA provides it. See [DATA_LICENSE.md](DATA_LICENSE.md).

Software provenance and third-party attribution are documented in [NOTICE](NOTICE).

USB Audio Player PRO (UAPP), ToneBoosters, OPRA, Roon Labs, manufacturer names, and headphone/product names are used only for compatibility, attribution, or source identification. **OPRA EQ for UAPP is not affiliated with or endorsed by OPRA, Roon Labs, USB Audio Player PRO/UAPP, ToneBoosters, or headphone manufacturers.**

## License

The application source code, tests, and project documentation are licensed under the **Apache License 2.0**. See [LICENSE](LICENSE).

OPRA-derived data is separately licensed; see [DATA_LICENSE.md](DATA_LICENSE.md).

## Project documentation

- [CHANGELOG.md](CHANGELOG.md) — release history and notable changes
- [PRIVACY.md](PRIVACY.md) — public privacy policy
- [CONTRIBUTING.md](CONTRIBUTING.md) — contribution and validation expectations
- [SECURITY.md](SECURITY.md) — security-reporting guidance
- [docs/PUBLIC_RELEASE_CHECKLIST.md](docs/PUBLIC_RELEASE_CHECKLIST.md) — GitHub public-release/signing checklist
- [docs/DEVICE_TEST_PLAN.md](docs/DEVICE_TEST_PLAN.md) — completed Pixel 9 validation record
- [docs/ARCHITECTURE.md](docs/ARCHITECTURE.md) — implementation architecture and invariants
- [docs/CHATGPT_PROJECT_RUNBOOK.md](docs/CHATGPT_PROJECT_RUNBOOK.md) — maintained product/UX source of truth

## Issues and contributions

Once the repository is public, GitHub Issues can be used for reproducible bugs and feature requests. See [CONTRIBUTING.md](CONTRIBUTING.md).

Please do not post credentials, signing keys, tokens, private files, or other sensitive information in an issue. Security-sensitive reports should follow [SECURITY.md](SECURITY.md).
