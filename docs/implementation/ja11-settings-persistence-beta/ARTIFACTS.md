# JA11 settings persistence beta artifacts

Status: **PENDING EXACT CANDIDATE**.

Keep all private APKs and reports out of public GitHub. Record the final artifact only after a
clean executable source commit has passed the required local, emulator, independent-review, and
exact-head gates.

| Field | Value |
| --- | --- |
| Repository / branch | `weekssa/OPRA-EQ-for-UAPP` / `codex/ja11-settings-persistence-beta` |
| Base SHA | `421f03331e45e6c1bdc77e5442db1de467432930` |
| Candidate source SHA | Pending |
| Draft PR / head SHA | Pending |
| Exact-head workflow run | Pending |
| Local build command | `./tools/codex-android :app:assembleJa11Beta -PCANDIDATE_SOURCE_SHA=FULL_FROZEN_CANDIDATE_SHA` |
| Variant / package | `ja11Beta` / `com.weekssa.opraeqforuapp.ja11beta` |
| Version | `0.8.2-ja11beta` / code `13` |
| APK path / SHA-256 | Pending |
| Signer certificate SHA-256 | Pending |
| APK signature / alignment | Pending |
| API 35 exact-APK install and cold launch | Pending |
| Stable-app coexistence on API 35 | Pending |
| API 26 exact-APK install and cold launch | Pending |
| Physical qualification | Not yet performed; owner-led after separate readiness confirmation |

The intended signer is the normal local debug/test signer. This is a private test artifact, not the
officially signed or published beta/release APK. An installed APK's package, version, embedded
source SHA, signature, alignment, and file checksum must be rechecked against these records.
