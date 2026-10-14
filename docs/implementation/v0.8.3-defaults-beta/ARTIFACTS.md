# v0.8.3 JA11 defaults beta artifacts

Beta v1 candidate bytes are frozen for owner review. The owner accepted the bounded emulator instrumentation substitution, authorized and completed the physical session, and the exact beta remains installed for manual owner review. Automated physical outcome: PASS WITH LIMITS; see `TEST_RESULTS.md`.

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

APK files and the R8 mapping are build outputs, not committed. Do not put private owner reports, raw coefficients, phone identifiers, logs, or screenshots in Git. Store those under a private evidence directory and record only sanitized outcome, safe paths, checksums, and evidence limits here.
