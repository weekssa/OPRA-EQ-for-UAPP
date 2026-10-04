# v0.7.2 governance remediation artifacts

## Initial public identity

- Release ID: `402346895`
- Release URL: https://github.com/weekssa/OPRA-EQ-for-UAPP/releases/tag/v0.7.2
- Release name: `EQ Library v0.7.2`
- Release state: published, non-draft, non-prerelease, latest
- Git tag object: `d04a10895d56dcc96218ea3056bc5f593ea9b2fe`
- Peeled tag commit: `b8e90b9b53fc63ea00fefa499d7d4bd6ce4d55ea`
- APK: `EQ-Library-v0.7.2.apk`, 2,835,216 bytes
- APK SHA-256: `efdd63ddb305d0624f805cc53e4ce27aae7d1ddeb169e8f965302d0f290ba64a`
- Signer SHA-256: `65c1c1256dae3c49e3548f334c91f0ba991969e9be9e0b223ba4e253d2114747`
- Release immutability: `false`
- Visible repository ruleset: `Protect main` (branch target, active); no tag ruleset was returned.
- Current body still contains the filter-gain wording identified for correction.

## Remediation evidence

Append exact branch, commit, PR, CI, settings readbacks, body comparison, and downloaded asset checksums as the work proceeds. Do not copy authorization headers, tokens, signed download URLs, or other credentials into this record.

## Remediation execution provenance

This table records only this remediation session's directly observed invocations. It is separate from the historical v0.7.2 release execution and does not backfill its missing F5/F6 records.

### Skills

| Skill | Source/version | Purpose in this remediation | Actually invoked |
|---|---|---|---|
| `unlazy` | `/Users/stephenweeks/.agents/skills/unlazy`, package version 2.1.0 | Mission gates, dispatch review, parent verification, checkpoint status | YES |
| `android-skills:android-dev` | Android skills plugin cache, version 5.6.0 | Bound APK/package/version/signer verification to existing release evidence without changing Android source | YES |

### Tools

| Tool | Version | Trusted source | Purpose in this remediation | Verification/hash | Actually invoked |
|---|---|---|---|---|---|
| Bundled Python | 3.12.14 | Configured Codex primary runtime at `/Users/stephenweeks/.cache/codex-runtimes/codex-primary-runtime/dependencies/python/bin/python3` | Focused/full Python promotion tests and release contract | Runtime-reported version; no independent binary digest published to this task | YES |
| `actionlint` | 1.7.12 | Official `rhysd/actionlint` GitHub release, Darwin arm64 | Workflow syntax/lint | SHA-256 `aba9ced2dee8d27fecca3dc7feb1a7f9a52caefa1eb46f3271ea66b6e0e6953f` | YES |
| ShellCheck | 0.11.0 | Official `koalaman/shellcheck` GitHub release, Darwin aarch64 | Shell script lint | SHA-256 `339b930feb1ea764467013cc1f72d09cd6b869ebf1013296ba9055ab2ffbd26f` | YES |
| GitHub CLI | 2.102.0 | Official `cli/cli` GitHub release, macOS arm64 | Authenticated release/tag/ruleset/immutable-setting reads; PR and authorized post-merge API work will be recorded at closeout | SHA-256 `da922c20d1792e5b2cbf375593d7a658acf034c12c84e007e71c76ef959c337e` | YES |

## Local check result snapshot

- Focused promotion tests: 48 passed.
- Full `tools` Python suite under bundled Python 3.12.14: 247 passed.
- `tools/promote_release_candidate.py check-contract`: passed (`PROMOTION_WORKFLOW_CONTRACT_PASSED`).
- `actionlint` 1.7.12, `shellcheck tools/*.sh` with ShellCheck 0.11.0, and `git diff --check`: passed; the combined gate emitted `POSTRELEASE_GATES_PASS` after exporting the tool paths for the complete command list.
