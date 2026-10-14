# v0.8.3 release checklist

Status: **OWNER ACCEPTED BETA V1; GO-LIVE AUTHORIZED; OFFICIAL RELEASE GATES IN PROGRESS**

- [x] Complete explicit-default behavior in the Beta build and shared stable-source implementation.
- [x] Complete off-phone validation with the owner's bounded exception for the minified Beta instrumentation failure and full API 26 Debug suite; preserve all original failures in `TEST_RESULTS.md`.
- [x] Owner reported that this identified candidate passed manual testing and all other manual tests; acceptance included the recorded limits that app operation counts are not USB bus captures and no native EQ Save, UAC change, or EQ-band write was used. No USB bus capture or unique serial evidence was supplied. Do not make a public EQ-bank-preservation or same-unit-continuity claim.
- [x] Freeze the separate Beta v1 identity and private APK provenance; exact APK install/launch and package coexistence pass on API 26 and 35 emulators.
- [x] Install/test exact bytes over owner-confirmed wireless ADB. Runtime Apply, staged Cancel, changed/unchanged defaults, non-User-1 program default, mic restoration, and true power-cycle readback passed for the exact beta tuple with limits recorded in J032.
- [x] Leave that exact beta and data installed for owner manual testing; owner manual acceptance received 2026-10-14.
- [ ] Integrate reviewed source on main through PR #97 and pass exact-main checks.
- [ ] Generate the official original-package 0.8.3 signed candidate from main; verify signer, package/version/code, R8, digest, alignment and provenance.
- [ ] Verify stable clean install and v0.8.2 in-place upgrade on disposable emulators.
- [ ] Promote the exact official candidate bytes without rebuild/re-sign; verify public tag, assets, checksums, signer, release notes and update metadata.
- [ ] Update capability/status/validation ledger, runbook, README, changelog, versioned release notes and rollback/install guidance.

The separate beta package/signature cannot be promoted as the stable APK. Owner exact-candidate acceptance and explicit go-live authorization were received 2026-10-14. Publish only the official original-package signed candidate built from exact current main and promoted without rebuild/re-sign.
