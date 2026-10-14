# JA11 settings persistence beta artifacts

Status: **EXACT PRIVATE BETA APK AND ALL OFF-PHONE SOFTWARE GATES VERIFIED**.

Keep all private APKs and reports out of public GitHub. Record the final artifact only after a
clean executable source commit has passed the required local, emulator, independent-review, and
exact-head gates.

| Field | Value |
| --- | --- |
| Repository / branch | `weekssa/OPRA-EQ-for-UAPP` / `codex/ja11-settings-persistence-beta` |
| Base SHA | `421f03331e45e6c1bdc77e5442db1de467432930` |
| Candidate source SHA | `620e1a46761de2d3621dbcd32135193efb42cf49` |
| Draft PR / current head | [PR #96](https://github.com/weekssa/OPRA-EQ-for-UAPP/pull/96), open and draft; latest change is documentation-only after executable candidate `620e1a46761de2d3621dbcd32135193efb42cf49` |
| Exact-head workflow run | [38512637427](https://github.com/weekssa/OPRA-EQ-for-UAPP/actions/runs/38512637427), all four jobs passed with exact source SHA |
| Local build command | `GRADLE_USER_HOME=/Users/stephenweeks/.gradle ./tools/codex-android :app:assembleJa11Beta -PCANDIDATE_SOURCE_SHA=620e1a46761de2d3621dbcd32135193efb42cf49 --no-daemon --max-workers=1 -Dorg.gradle.jvmargs=-XX:ActiveProcessorCount=2` |
| Variant / package | `ja11Beta` / `com.weekssa.opraeqforuapp.ja11beta` |
| Version | `0.8.2-ja11beta` / code `13` |
| APK path / SHA-256 | `app/build/outputs/apk/ja11Beta/app-ja11Beta.apk` / `63d6d1b63563f0c9129e45202eb9eec08a1424a6052e0bd9c3d1e767473600a1` |
| APK size | 23,209,361 bytes |
| Embedded candidate source SHA | Verified in `classes11.dex` and `classes14.dex`: `620e1a46761de2d3621dbcd32135193efb42cf49` |
| Signer certificate SHA-256 | `968b15ab4802bba98fbd058d7b81f537da6c2c5542333d0b107b3495135587b4` (Android Debug test signer) |
| APK signature / alignment | `apksigner verify` passed with v2 signature; `zipalign -c -v 4` passed |
| Package / version metadata | `com.weekssa.opraeqforuapp.ja11beta`, `0.8.2-ja11beta`, version code `13`, min SDK `26`, target SDK `36`; label `EQ Library JA11 Beta` |
| API 35 exact-APK install and cold launch | PASS on disposable `emulator-5554`; cold launch completed, app process observed |
| Stable-app coexistence on API 35 | PASS; stable `com.weekssa.opraeqforuapp` and beta `.ja11beta` packages both installed |
| API 26 exact-APK install and cold launch | PASS on disposable `emulator-5556`; cold launch completed, app process observed |
| PR CodeQL | PASS, run `38512320065` against exact candidate SHA |
| PR community/catalog checks | PASS, runs `38512320099` and `38512320115` |
| Exact-head hosted build jobs | PASS: build, exact-head CodeQL, API 26 smoke, and API 35 UI test |
| Legacy standard Android CI push workflow | Failed before creating jobs (run `38512307326`); exact-head fallback run above supplies the required hosted software gates |
| Physical qualification | Not yet performed; owner-led after separate readiness confirmation |

The intended signer is the normal local debug/test signer. This is a private test artifact, not the
officially signed or published beta/release APK. An installed APK's package, version, embedded
source SHA, signature, alignment, and file checksum must be rechecked against these records.
