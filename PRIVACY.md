# Privacy Policy — EQ Library

Last updated: 2026-09-14

**EQ Library** is designed to work without an account and without collecting analytics or telemetry. The project began as OPRA EQ for UAPP; the Android application ID remains `com.weekssa.opraeqforuapp` so existing installations can continue to upgrade normally.

## Information stored on your device

The app stores only local app-operation data needed for its features, including:

- your saved headphones, EQ selections, exclusions, and automatic future-profile preferences;
- Favorites, hidden-EQ preferences, review state, Personal EQs, General EQs, and captured DAC EQ metadata;
- app appearance, default/manual EQ-target preferences, and other local settings;
- the last-known-good canonical EQ catalog cache and legacy OPRA fallback cache when needed;
- generated-preset metadata, export ownership/currentness records, and locally generated preset state;
- the Android document-tree or document access you explicitly grant through the system picker when Android allows that access to be retained;
- exact records identifying files created by EQ Library so later regeneration, recovery, or optional cleanup can be performed safely; and
- device-local state needed for supported DAC safety/transaction behavior, such as EQ Library-tracked gain adjustment state and captured hardware provenance.

The persistent **Needs attention** recovery surface is derived from EQ Library's own export-ownership records and retained document access. It does not scan arbitrary external storage.

This information remains on the device under normal app operation. EQ Library does not provide a cloud account or remote synchronization service.

## Supported DAC access

When you connect a supported USB DAC, EQ Library can communicate with that device locally through Android's USB APIs after Android grants the required device access. Current EQ/device-control reads, verified writes, and captured DAC EQ data stay local to the device unless you explicitly export a file yourself.

EQ Library does not upload connected-DAC settings, hardware reads, USB identifiers, or captured EQs to the project, GitHub, OPRA, or another cloud service.

## Network access

The app uses Internet access for limited public-data retrieval:

1. **Canonical EQ catalog:** downloading the validated live catalog from `https://raw.githubusercontent.com/weekssa/OPRA-EQ-for-UAPP/catalog-live/catalog/catalog.json` and checking it for later updates.
2. **Legacy OPRA fallback:** if the canonical catalog path is unavailable or cannot provide a usable catalog, the maintained fallback can read the public OPRA runtime catalog from `https://opra.roonlabs.net/database_v1.jsonl`.
3. **App update metadata:** checking public GitHub Releases metadata for this project so the app can tell you when a newer EQ Library release is available.

Normal Android runtime does not scrape GitHub repositories, Reddit, forums, measurement sites, or other community sources for EQ discovery. Source discovery, qualification, and canonical catalog publication happen outside the installed app; the app consumes the validated published catalog.

EQ Library does not upload your saved headphones, EQ selections, generated presets, export-folder contents, hardware state, or app settings to these services.

The remote services contacted by the app may receive ordinary network information such as your IP address, user-agent string, and request metadata as part of operating their servers. Their handling of that information is governed by their own policies and infrastructure.

## Analytics, telemetry, advertising, and accounts

EQ Library contains:

- no analytics SDK;
- no telemetry system;
- no advertising SDK;
- no in-app advertising;
- no user account or login; and
- no cloud backend operated by this project.

The app does not sell personal information.

## Files and storage

Preset export uses Android's Storage Access Framework. You choose the destination through Android's system folder/document picker.

EQ Library does not request broad storage access and does not write into another app's private storage. It manages only files it can prove it created through its ownership records and retained Android document access.

When you explicitly request cleanup or delete an unresolved managed artifact, the app attempts to remove only the exact ownership-tracked file it created and can still access. It does not recursively delete unrelated external files.

## EQ source data and generated presets

The app downloads public EQ catalog data and caches it locally. Catalog entries can include OPRA, AutoEq, qualified creator/repository sources, public community EQs, and qualified General EQ sources according to the project's source/provenance rules.

Generated presets can contain source-derived EQ parameters and attribution information. EQ Library preserves available creator/source provenance rather than implying that the project authored third-party EQ data. See `DATA_LICENSE.md` and `NOTICE` for data licensing, attribution, and third-party notices.

## Data deletion

You can remove saved headphones, EQ selections, Favorites, Personal EQs, General EQs, and other app-owned library state through the app where the relevant control is provided. Where offered, you can separately choose whether an exact app-created exported preset file should also be removed.

Clearing EQ Library's app storage or uninstalling the app removes its private local database, preferences, and cached catalogs. Files previously exported into user-selected storage are not automatically removed by uninstalling the app.

## Children

EQ Library does not knowingly collect personal information from children or adults because it does not operate a user-account, analytics, telemetry, advertising, or user-data submission service.

## Changes to this policy

Material privacy changes will be documented in the project changelog and this file. The app will not silently add analytics, telemetry, account requirements, advertising, cloud synchronization, or new categories of user-data collection without a deliberate product change.

## Project and support

Project repository: `https://github.com/weekssa/OPRA-EQ-for-UAPP`

Reproducible non-sensitive issues may be reported through GitHub Issues. Do not include credentials, private files, signing keys, tokens, or other sensitive information in a public issue. Security-sensitive reports should follow `SECURITY.md`.
