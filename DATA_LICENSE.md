# Data licensing and attribution

This Android application contains software and consumes OPRA-derived data with different licensing.

## Application software

The application source code, tests, and project documentation are distributed under the Apache License, Version 2.0. See `LICENSE` and `NOTICE`.

The ToneBoosters/UAPP normalization and XML mapping is based in part on `SiliconExarch/EqConverter`, licensed under Apache-2.0. The native Kotlin implementation is ported from the established `weekssa/opra-uapp-converter` behavior; provenance is preserved in `NOTICE`.

## OPRA-derived data and generated presets

Headphone/product/EQ data consumed from OPRA is licensed by OPRA under Creative Commons Attribution-ShareAlike 4.0 International (CC BY-SA 4.0).

The app ships with zero headphone/EQ records. OPRA data is downloaded at runtime from the supported OPRA distribution feed and cached locally after validation.

Generated ToneBoosters/UAPP XML presets are format conversions and/or reproductions of selected OPRA dataset material. To the extent copyright or database rights apply to OPRA-derived portions, those portions remain subject to CC BY-SA 4.0 rather than the application's Apache-2.0 software license.

The app retains OPRA profile identity, creator/author when provided, details, source link, OPRA IDs, acoustic parameters, and generated-preset state locally. Exported preset names prominently preserve the creator slot; when OPRA creator information is missing, the approved literal label `Creator information missing` is used without inventing a creator identity.

OPRA source repository: https://github.com/opra-project/OPRA

OPRA distribution feed: https://opra.roonlabs.net/database_v1.jsonl

CC BY-SA 4.0: https://creativecommons.org/licenses/by-sa/4.0/

## Compatibility names and trademarks

USB Audio Player PRO (UAPP), ToneBoosters, OPRA, Roon Labs, manufacturer names, and headphone/product names are used only to describe compatibility, attribution, or identify source data. Their respective trademarks and product names remain the property of their owners.

OPRA EQ for UAPP is not affiliated with or endorsed by OPRA, Roon Labs, USB Audio Player PRO/UAPP, ToneBoosters, or headphone manufacturers.
