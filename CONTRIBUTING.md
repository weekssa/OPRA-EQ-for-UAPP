# Contributing to OPRA EQ for UAPP

Thanks for helping improve OPRA EQ for UAPP.

## Before opening an issue

For bugs, please include:

- Android device/model and Android version;
- app version;
- the exact screen/action that triggered the problem;
- expected result and actual result;
- whether the problem reproduces after restarting the app; and
- a screenshot when it helps and contains no sensitive information.

Do not post passwords, tokens, signing keys, private files, or other credentials.

## Product invariants

Changes must preserve the project's established behavior unless a deliberate product decision changes it. In particular:

- the app ships with zero bundled headphone/EQ records;
- normal catalog operation uses the OPRA `database_v1.jsonl` runtime feed rather than scraping GitHub;
- user selections, settings, and conversion remain local;
- unsupported filters or invalid acoustic values are never silently approximated, clamped, or dropped;
- ToneBoosters export is limited to 10 bands while preserving OPRA priority/order and warning the user;
- export uses Android's Storage Access Framework rather than broad storage permission;
- unknown same-name files are never silently overwritten;
- app-created file cleanup is ownership-tracked and explicit; and
- OPRA and individual EQ creator/source attribution must be preserved.

See `docs/CHATGPT_PROJECT_RUNBOOK.md`, `docs/ARCHITECTURE.md`, and `docs/PHASE1_DECISIONS.md` for the maintained product and architecture rules.

## Development baseline

- Java 17
- Android API 36 compile/target
- minSdk 26
- Kotlin + Jetpack Compose

The automated validation gate is:

```text
:app:testDebugUnitTest
:app:lintDebug
:app:assembleDebug
```

Do not weaken validation merely to make a change pass.

## Conversion changes

The Kotlin converter is intentionally parity-oriented with the established reference behavior. Conversion changes should include deterministic regression/golden coverage for the affected normalization, filter mapping, naming/encoding, or XML output.

A new OPRA filter type must not be marked compatible until a faithful UAPP/ToneBoosters mapping is proven and tested.

## Licensing and attribution

By contributing to this repository, contributions are submitted under the repository's Apache License 2.0 terms unless explicitly stated otherwise.

OPRA-derived data remains separately licensed under CC BY-SA 4.0. Do not copy OPRA data into the APK or repository in a way that changes the app's zero-bundled-headphones design.
