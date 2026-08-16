# Privacy Policy — OPRA EQ for UAPP

Last updated: 2026-08-15

OPRA EQ for UAPP is designed to work without an account and without collecting analytics or telemetry.

## Information stored on your device

The app stores only app-operation data needed for its local features, including:

- your managed headphones and selected OPRA EQ profiles;
- profile exclusions and automatic future-profile preferences;
- app appearance and compatibility-visibility preferences;
- the last-known-good OPRA runtime catalog cache;
- generated preset metadata and locally generated XML state;
- the Android document-tree location you explicitly choose for preset export, when Android allows that access to be retained; and
- records identifying exported files created by this app so that later updates or optional cleanup can be performed safely.

This information remains on the device under normal app operation. OPRA EQ for UAPP does not provide a cloud account or remote synchronization service.

## Network access

The app uses Internet access for two purposes:

1. **OPRA catalog:** downloading the supported runtime OPRA catalog from `https://opra.roonlabs.net/database_v1.jsonl` and checking it for later updates.
2. **App update metadata:** checking the public GitHub Releases metadata for this project so the app can tell you when a newer release is available.

The app does not upload your headphone selections, generated presets, export-folder contents, or app settings to these services.

The remote services contacted by the app may receive ordinary network information such as your IP address and request metadata as part of operating their servers. Their handling of that information is governed by their own policies and infrastructure.

## Analytics, telemetry, advertising, and accounts

OPRA EQ for UAPP contains:

- no analytics SDK;
- no telemetry system;
- no advertising SDK;
- no in-app advertising;
- no user account or login; and
- no cloud backend operated by this project.

The app does not sell personal information.

## Files and storage

Preset export uses Android's Storage Access Framework. You choose the destination through Android's system folder picker.

The app does not request broad storage access and does not write into another app's private storage. It tracks only exported files that it created itself. When you explicitly request saved-file cleanup, the app attempts to remove only ownership-tracked files it created and can still access through Android's retained document permission.

## OPRA data and generated presets

The app downloads public OPRA headphone and EQ data and caches it locally. Generated UAPP/ToneBoosters preset files may contain OPRA-derived EQ parameters and attribution information. See `DATA_LICENSE.md` for data licensing and attribution details.

## Data deletion

You can remove managed headphones and profiles inside the app. Where offered, you can separately choose whether app-created exported preset files should also be removed.

Clearing the app's storage or uninstalling the app removes its private local database, settings, and cached catalog. Files previously exported into user-selected storage are not automatically removed by uninstalling the app.

## Children

The app does not knowingly collect personal information from children or adults because it does not operate a user-account, analytics, telemetry, or data-submission service.

## Changes to this policy

Material privacy changes will be documented in the project changelog and this file. The app will not silently add analytics, telemetry, account requirements, or new categories of data collection without a deliberate product change.

## Project and support

Project repository: `https://github.com/weekssa/OPRA-EQ-for-UAPP`

Once the repository is public, reproducible non-sensitive issues may be reported through GitHub Issues. Do not include credentials, private files, signing keys, tokens, or other sensitive information in a public issue.
