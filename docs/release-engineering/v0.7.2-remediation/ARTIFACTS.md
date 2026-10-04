# v0.7.2 governance remediation artifacts

## Initial public identity (read before authorized writes)

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
| GitHub CLI | 2.102.0 | Official `cli/cli` GitHub release, macOS arm64 | Authenticated release/tag/ruleset/immutable-setting reads, PR creation/merge/checks, and authorized post-merge API work | SHA-256 `da922c20d1792e5b2cbf375593d7a658acf034c12c84e007e71c76ef959c337e` | YES |
| `apksigner` | Android Build Tools 36.0.0 | Installed Android SDK build-tools | Verify the fresh public APK's signature and signer certificate fingerprint | SDK-installed executable; no manual download | YES |
| `aapt` | Android Build Tools 36.0.0 | Installed Android SDK build-tools | Verify package ID, versionName, and versionCode from the fresh public APK | SDK-installed executable; no manual download | YES |
| GitHub official documentation lookup | GitHub Docs, read 2026-10-04 | `docs.github.com` | Confirm ruleset matching, immutable-release API behavior, endpoint body, and documented future-release scope | Web page citations are recorded in the final report; no binary installation | YES |
| Codex UI browser/CUA | Connected desktop GitHub session | Existing browser tab | Visually verify active tag ruleset and public Release immutable badge/body | Read-only screenshots surfaced in the task; no local screenshot artifact | YES |

## Local check result snapshot

- Focused promotion tests: 48 passed.
- Full `tools` Python suite under bundled Python 3.12.14: 247 passed.
- `tools/promote_release_candidate.py check-contract`: passed (`PROMOTION_WORKFLOW_CONTRACT_PASSED`).
- `actionlint` 1.7.12, `shellcheck tools/*.sh` with ShellCheck 0.11.0, and `git diff --check`: passed; the combined gate emitted `POSTRELEASE_GATES_PASS` after exporting the tool paths for the complete command list.

## Post-merge and post-publication readback (2026-10-04)

- Repository: `weekssa/OPRA-EQ-for-UAPP`; primary PR [#68](https://github.com/weekssa/OPRA-EQ-for-UAPP/pull/68) merged normally to `main` as `468436d8236248bf1f017b1a503113cd41f81d73`. Its exact PR head was `85daba251b85995a77e1675174f4ef6946d427fd`; all seven required checks passed before merge. A documentation-only closeout PR records this final readback separately.
- Active stable-tag ruleset: [Protect stable release tags](https://github.com/weekssa/OPRA-EQ-for-UAPP/rules/24444174), ID `24444174`; stable version-shaped tags are protected against update/deletion, bypass list empty, creation unrestricted. Existing main protection unchanged.
- Repository immutable-release API with `X-GitHub-Api-Version: 2026-03-10`: `enabled=true`, `enforced_by_owner=false`. Release ID `402346895` API and public UI currently report `immutable=true`. GitHub's setup guide describes enabling the setting for future releases only; this record reports the live response without generalizing the observed v0.7.2 result.
- The public Release name remains `EQ Library v0.7.2`; its body exactly equals `docs/releases/v0.7.2.md` at 3,709 UTF-8 characters, SHA-256 `6943b87df850485fa037df0822fbb013a35acf81fb1943d4553157a5abc707aa`.
- The annotated `v0.7.2` tag object remains `d04a10895d56dcc96218ea3056bc5f593ea9b2fe`, peeling to `b8e90b9b53fc63ea00fefa499d7d4bd6ce4d55ea`.
- The six uploaded asset IDs, byte sizes, and GitHub SHA-256 values were identical to the pre-write snapshot. Freshly downloaded APK: 2,835,216 bytes, SHA-256 `efdd63ddb305d0624f805cc53e4ce27aae7d1ddeb169e8f965302d0f290ba64a`; signer certificate SHA-256 `65c1c1256dae3c49e3548f334c91f0ba991969e9be9e0b223ba4e253d2114747`; package `com.weekssa.opraeqforuapp`; versionName `0.7.2`; versionCode `9`.
