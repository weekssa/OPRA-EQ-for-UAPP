# v0.7.2 governance remediation checklist

- [x] F1: Inspect the exact active Unlazy checkpoint, use a supported close/archive mechanism if present, and preserve its historical gates. The CLI has no close operation; its stale RESUME was safely retired with a byte-preserving snapshot, and old gate outcomes remain unchanged.
- [x] F2: Protect stable version tags against update/deletion; enable immutable Releases; read settings and tag ruleset back. Stable tag creation remains unrestricted.
- [x] F3: Correct the changelog and maintained v0.7.2 release notes transparently; update the public Release body without replacing assets.
- [x] F4: Compare final Release name and body to the expected version-derived values and test matching, mismatching, draft-creation, and newline cases; publication-path mismatch cases are covered.
- [x] F5: Mark historical skill invocation evidence not retroactively recoverable; define future skill capture.
- [x] F6: Mark historical tool-install evidence not retroactively recoverable; define future tool capture.
- [x] Run the requested Python, release-contract, actionlint, ShellCheck, diff, and focused promotion tests locally; repeat the root gate on the final PR head.
- [x] Verify the diff contains no app-functional, signing, version, or release asset changes.
- [x] Push the branch, open the requested PR, wait for checks, resolve failures, and merge after gates pass. PR #68 merged after all seven exact-head checks passed.
- [x] Recheck main, v0.7.2 tag, public Release body/assets, APK bytes, signer, version, and checksum.
- [x] Create the requested evidence directory, manifest, and ZIP. The directory contains the requested 12 files and stays untracked.
