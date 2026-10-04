# v0.7.2 governance remediation final report

STATUS: IN PROGRESS

Application behavior changed: NO (implementation touches only release tooling and documentation; committed diff review pending)

v0.7.2 tag target: `b8e90b9b53fc63ea00fefa499d7d4bd6ce4d55ea` (reverify at closeout)

Public APK SHA-256: `efdd63ddb305d0624f805cc53e4ce27aae7d1ddeb169e8f965302d0f290ba64a` (reverify at closeout)

Findings F1/F3/F4 are implemented; F2 server changes and public-body correction await merge. F5/F6 are not retroactively recoverable and future capture is documented. See `FINDINGS.md`.

PR: None yet.

CI: Not run yet.

GitHub stable-tag protection: Not yet established; fresh pre-write readback found only `Protect main`.

GitHub Release immutability: Repository setting currently disabled and v0.7.2 object `immutable=false`; enabling affects future releases only.

Public release erratum: Maintained notes are corrected; public body update awaits merge.

Evidence bundle: Not yet assembled.

Local verification so far: 48 focused promotion tests; 247 Python tests; promotion contract, actionlint, ShellCheck, and `git diff --check` pass.

Human action required: None identified; use the already authenticated GitHub CLI within the owner's authorized scope.
