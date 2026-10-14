# v0.8.3 JA11 defaults beta artifacts

Beta v1 candidate bytes are frozen and were accepted by the owner. The official v0.8.3 stable release was built and promoted as a separate original-package candidate. The exact beta remains installed. Automated physical outcome: PASS WITH LIMITS; owner acceptance and its limits are recorded in J032/J033 and `TEST_RESULTS.md`.

| Field | Beta v1 |
| --- | --- |
| Source SHA | `7cea4545dddbee2da0b436e0684a9c489835e3cd` |
| Base SHA | `575dab59fe774d3683a3714ca490a334f1b20764` (`origin/main`, refreshed 2026-10-14) |
| Branch | `codex/v0.8.3-defaults-beta` |
| Build variant / command | `beta`; `./tools/codex-android :app:assembleBeta :app:assembleBetaAndroidTest -PCANDIDATE_SOURCE_SHA=7cea4545dddbee2da0b436e0684a9c489835e3cd --no-daemon --max-workers=1` |
| Package | `com.weekssa.opraeqforuapp.v083beta` |
| Version name / code | `0.8.3-beta.1` / 14 |
| APK path / SHA-256 / size | `app/build/outputs/apk/beta/app-beta.apk` / `ac982eafd76f4db80f48e0f26040bf1f849388c4f0a51f778b51c8c013c59ce5` / 2,875,504 bytes |
| Test APK path / SHA-256 / size | `app/build/outputs/apk/androidTest/beta/app-beta-androidTest.apk` / `e89c4533b3ee2676442194cb3d9d01c5fd2bc3d22fa7f132aeeb7a0908898cac` / 3,617,920 bytes |
| R8 mapping path / SHA-256 | `app/build/outputs/mapping/beta/mapping.txt` / `44d9d5da79f34c72b533a473251b260e10884847a33601da3e8f0a330bd12f2f` |
| Signer certificate SHA-256 | `968b15ab4802bba98fbd058d7b81f537da6c2c5542333d0b107b3495135587b4` (local Android debug signer; beta only) |
| Signature / alignment / R8 | APK v2 signature verified; `zipalign -c -p 4` passed; `minifyBetaWithR8` and `minifyBetaAndroidTestWithR8` completed |
| Embedded source identity | APK DEX string and both generated app/test `BuildConfig` files contain `7cea4545dddbee2da0b436e0684a9c489835e3cd` |
| API 26 / modern install and coexistence | API 26 emulator `emulator-5556` and API 35 emulator `emulator-5554`: exact final beta installed, activity launch returned `Status: ok`; stable and beta package IDs coexist on both |
| Wireless owner installation and JA11 test | Installed 2026-10-14 on Pixel 9 / API 37 after owner-approved gate exception; `Status: ok` cold launch. Three defaults operations plus owner-confirmed physical power-cycle readback passed with limits. Final pair 19 / Classic; mic On; UAC 2.0; firmware 2.20; VID:PID 2972:0102. User 1 EQ comparison unavailable; no bank-preservation claim. Stable 0.8.2 and earlier JA11 beta remain installed. Private evidence: `/private/tmp/ja11-v083-session-20261014/`. |

## Official stable release

The owner manually accepted the exact installed Beta v1 and authorized go-live on 2026-10-14 (J033). PR #97 merged normally as `899755507f812c940f3ab9381a856c80c6030885`; app code and runtime behavior are unchanged from the tested Beta source. Exact-main Android CI `38648012952` passed all jobs.

The main-only Signed Release Candidate workflow `38649163805` produced immutable artifact `11934142420`, archive SHA-256 `500c5c693ea41f35e5025041b9fddebab3d44c863151411d871a8255cc6e31bb`. The candidate APK SHA-256 is `396244ffc26cec108b64c523a49779261656b41ab02f1bc2b63197327c576247`; size 2,904,848 bytes; package `com.weekssa.opraeqforuapp`; versionName `0.8.3`; versionCode `14`; signer certificate SHA-256 `65c1c1256dae3c49e3548f334c91f0ba991969e9be9e0b223ba4e253d2114747`; R8 mapping SHA-256 `45601e0f655cb82f5965b2077faebb310a8009826ceb7bd9cb30966d20ed16d9` (private candidate artifact only). Independent v2/v3 signature, package/version/source, pinned signer, mapping and alignment checks passed.

Promotion run `38650109951` independently passed stable clean-install and v0.8.2 in-place-upgrade checks on API 35, created annotated tag object `1a584839b699ce2f0ff447582719fbeb19915392`, and published the exact candidate without rebuild or re-sign. The tag peels to the same source. Public immutable release [v0.8.3](https://github.com/weekssa/OPRA-EQ-for-UAPP/releases/tag/v0.8.3), release ID `413252769`, is non-draft, non-prerelease, and returned by `/releases/latest`. All six public assets were downloaded and each SHA-256 matched GitHub metadata; the APK download matches the candidate. Full public digests are recorded in the stable release-engineering record.

The exact Beta v1 and data remain installed on the owner's Pixel 9. J032/J033 preserve PASS WITH LIMITS, final settings 19 / Classic, Mic On, UAC 2.0, and the limits around unavailable User 1 comparison, unique serial identity, and app-reported counters. No new public hardware capability classification is made.

APK files and the R8 mapping are build outputs, not committed. Do not put private owner reports, raw coefficients, phone identifiers, logs, or screenshots in Git. Store those under a private evidence directory and record only sanitized outcome, safe paths, checksums, and evidence limits here.
