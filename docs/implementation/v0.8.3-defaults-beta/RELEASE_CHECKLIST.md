# v0.8.3 release checklist

Status: **NOT RELEASE READY**

- [ ] Complete explicit-default behavior in beta and stable configurations from the same implementation.
- [ ] Pass software, security, minified-path, API 26 and supported modern emulator gates.
- [ ] Resolve the handoff's User 1 bank-preservation gate with a bounded supported evidence plan or an explicit owner exception tied to the frozen candidate; never infer preservation from the absence of EQ writes.
- [ ] Freeze the separate Beta v1 identity and private APK provenance; install/test exact bytes over owner-confirmed wireless ADB.
- [ ] Leave that exact beta and data installed after automated tests; present results and await owner confirmation that this candidate passes and instruction to publish.
- [ ] After that instruction, integrate reviewed source on main and pass exact-main checks.
- [ ] Generate the official original-package 0.8.3 signed candidate from main; verify signer, package/version/code, R8, digest, alignment and provenance.
- [ ] Verify stable clean install and v0.8.2 in-place upgrade on disposable emulators.
- [ ] Promote the exact official candidate bytes without rebuild/re-sign; verify public tag, assets, checksums, signer, release notes and update metadata.
- [ ] Update capability/status/validation ledger, runbook, README, changelog, versioned release notes and rollback/install guidance.

The separate beta package/signature cannot be promoted as the stable APK. Do not publish before the owner's exact-candidate pass-and-publish instruction.
