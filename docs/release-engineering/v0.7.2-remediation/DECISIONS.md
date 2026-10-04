# v0.7.2 governance remediation decisions

## 2026-10-03

- Keep the published v0.7.2 tag target, APK, signer, package, versionName, and versionCode fixed.
- Correct historical prose transparently; do not rewrite the annotated Git tag or release assets.
- The strict imported filter-gain rejection predates v0.7.2. The parser diff for v0.7.2 is limited to the null-safe finite-value guard for an explicitly supplied `Preamp:` line; an absent preamp remains null.
- GitHub documents repository rulesets and immutable Releases as available to public repositories. Enabling immutable Releases only affects future releases, so it cannot convert the already-published v0.7.2 Release to immutable.
- Historical F5/F6 evidence will be reported as not retroactively recoverable unless original records are independently found. Future records will name skills and tools, versions/sources, purposes, and whether each was actually invoked.

## 2026-10-04

- Re-read the remote tag, annotated tag object, public Release, assets, latest Release, applicable rulesets, and repository immutable-release setting immediately before any authorized GitHub write. The tag still peels to `b8e90b9b53fc63ea00fefa499d7d4bd6ce4d55ea`, APK digest remains `efdd63ddb305d0624f805cc53e4ce27aae7d1ddeb169e8f965302d0f290ba64a`, only `Protect main` applies, and the repository immutability setting reads disabled.
- F1's installed Unlazy CLI has no close/archive action. The stale resume was replaced with a closed-state notice and preserved verbatim at a second path; no historical gates or results were altered.
- Release-body comparison allows only one optional final LF because both current source notes and original public API body ended with exactly one LF. Internal line endings and all substantive text remain exact.
- The repository immutable-release setting is enabled, and current API/UI readback reports the already-published v0.7.2 release as immutable. This is an observed live state that differs from GitHub's current guide saying the setting applies only to future releases; do not generalize retroactive behavior beyond this readback.
