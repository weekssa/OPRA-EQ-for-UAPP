# v0.8.3 release checklist

Status: **BETA CANDIDATE FROZEN; AUTOMATED PHONE PASS WITH LIMITS; OWNER MANUAL ACCEPTANCE/PUBLISH INSTRUCTION PENDING**

- [x] Complete explicit-default behavior in the Beta build and shared stable-source implementation.
- [x] Complete off-phone validation with the owner's bounded exception for the minified Beta instrumentation failure and full API 26 Debug suite; preserve all original failures in `TEST_RESULTS.md`.
- [ ] User 1 bank comparison remains unavailable for the tested Off/Classic programs. Do not claim bank preservation; the owner must knowingly accept this evidence limit before any broader preservation claim.
- [x] Freeze the separate Beta v1 identity and private APK provenance; exact APK install/launch and package coexistence pass on API 26 and 35 emulators.
- [x] Install/test exact bytes over owner-confirmed wireless ADB. Runtime Apply, staged Cancel, changed/unchanged defaults, non-User-1 program default, mic restoration, and true power-cycle readback passed for the exact beta tuple with limits recorded in J032.
- [x] Leave that exact beta and data installed for owner manual testing. Await the owner's identified-candidate pass and instruction to publish v0.8.3.
- [ ] After that instruction, integrate reviewed source on main and pass exact-main checks.
- [ ] Generate the official original-package 0.8.3 signed candidate from main; verify signer, package/version/code, R8, digest, alignment and provenance.
- [ ] Verify stable clean install and v0.8.2 in-place upgrade on disposable emulators.
- [ ] Promote the exact official candidate bytes without rebuild/re-sign; verify public tag, assets, checksums, signer, release notes and update metadata.
- [ ] Update capability/status/validation ledger, runbook, README, changelog, versioned release notes and rollback/install guidance.

The separate beta package/signature cannot be promoted as the stable APK. Do not publish before the owner's exact-candidate pass-and-publish instruction.
