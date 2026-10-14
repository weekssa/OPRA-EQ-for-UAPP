# v0.8.3 release checklist

Status: **v0.8.3 PUBLISHED IMMUTABLY; RELEASE AND DOCUMENTATION CLOSEOUT COMPLETE**

- [x] Complete explicit-default behavior in the Beta build and shared stable-source implementation.
- [x] Complete off-phone validation with the owner's bounded exception for the minified Beta instrumentation failure and full API 26 Debug suite; preserve all original failures in `TEST_RESULTS.md`.
- [x] Owner reported that this identified candidate passed manual testing and all other manual tests; acceptance included the recorded limits that app operation counts are not USB bus captures and no native EQ Save, UAC change, or EQ-band write was used. No USB bus capture or unique serial evidence was supplied. Do not make a public EQ-bank-preservation or same-unit-continuity claim.
- [x] Freeze the separate Beta v1 identity and private APK provenance; exact APK install/launch and package coexistence pass on API 26 and 35 emulators.
- [x] Install/test exact bytes over owner-confirmed wireless ADB. Runtime Apply, staged Cancel, changed/unchanged defaults, non-User-1 program default, mic restoration, and true power-cycle readback passed for the exact beta tuple with limits recorded in J032.
- [x] Leave that exact beta and data installed for owner manual testing; owner manual acceptance received 2026-10-14.
- [x] Integrate reviewed source on main through PR #97 and pass exact-main checks.
- [x] Generate and verify the official original-package 0.8.3 signed candidate from main.
- [x] Verify stable clean install and v0.8.2 in-place upgrade on disposable API 35 emulator lanes.
- [x] Promote exact official candidate bytes without rebuild/re-sign; verify public tag, six assets, checksums, signer, release notes and `/releases/latest` metadata.
- [x] Update capability/status/validation ledger, runbook, README, changelog, versioned release notes, installation and rollback guidance.

The separate beta package/signature was not promoted. Owner exact-candidate acceptance and go-live authorization were received 2026-10-14. The original-package signed v0.8.3 candidate was promoted from exact main without rebuild/re-sign. The phone retains the beta and its data for continued owner use.
