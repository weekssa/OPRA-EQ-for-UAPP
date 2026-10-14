# v0.8.3 release checklist

Status: **BETA CANDIDATE FROZEN; OWNER EMULATOR-EXCEPTION DECISION AND PHONE VALIDATION PENDING**

- [x] Complete explicit-default behavior in the Beta build and shared stable-source implementation.
- [ ] Complete off-phone validation. The minified Beta instrumentation suite crashes before test discovery on API 26 and API 35 because `androidx.tracing.Trace` is absent; a same-source Debug substitute is proposed and awaits the owner's explicit acceptance. Preserve the four unrelated API 26 broad-suite failures in `TEST_RESULTS.md`.
- [ ] Resolve the handoff's User 1 bank-preservation gate with a bounded supported evidence plan or an explicit owner exception tied to the frozen candidate; never infer preservation from the absence of EQ writes.
- [x] Freeze the separate Beta v1 identity and private APK provenance; exact APK install/launch and package coexistence pass on API 26 and 35 emulators.
- [ ] Install/test exact bytes over owner-confirmed wireless ADB after the owner accepts the emulator-gate substitution and confirms phone readiness.
- [ ] Leave that exact beta and data installed after automated tests; present results and await owner confirmation that this candidate passes and instruction to publish.
- [ ] After that instruction, integrate reviewed source on main and pass exact-main checks.
- [ ] Generate the official original-package 0.8.3 signed candidate from main; verify signer, package/version/code, R8, digest, alignment and provenance.
- [ ] Verify stable clean install and v0.8.2 in-place upgrade on disposable emulators.
- [ ] Promote the exact official candidate bytes without rebuild/re-sign; verify public tag, assets, checksums, signer, release notes and update metadata.
- [ ] Update capability/status/validation ledger, runbook, README, changelog, versioned release notes and rollback/install guidance.

The separate beta package/signature cannot be promoted as the stable APK. Do not publish before the owner's exact-candidate pass-and-publish instruction.
