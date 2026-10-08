# v0.8.0 stable promotion — final closeout

Updated: 2026-10-08T12:13:02Z

## Current state

- **Complete.** Stable v0.8.0 is public, immutable, non-draft, non-prerelease, and returned by `/releases/latest`. The immutable v0.8.0-beta prerelease remains preserved; v0.7.2 is the previous stable.
- Stable release source SHA/tree: `54823e1a464f8b716a32c4f4808037fc0cdd99bd` / `e20330e2c4921823ecdf2881db0bf6fa1b3c3c0d`. The stable source retains the qualified beta production app-source tree. Current main after documentation PR #78 is `4a15516ccc9852657716325cba629da16df27d26` / `bd2cd70964b1a3bb705dc5b20a22e79a19c26828`.
- Official main-only signing run `37769090436`, artifact `11546549425`, APK SHA-256 `2ea1d4b76a840e7448fddba556c3dc03aba19870eeb2499182af7d5759cce54b`, signer SHA-256 `65c1c1256dae3c49e3548f334c91f0ba991969e9be9e0b223ba4e253d2114747`; package/version/code `com.weekssa.opraeqforuapp` / `0.8.0` / `11`.
- Promotion run `37770238047` passed signed v0.7.2 and beta persisted-state upgrades plus signed clean-install/core smoke. Independent artifact review and public asset verification passed. See `ARTIFACTS.md`, `TEST_MATRIX.md`, and `../../PUBLIC_RELEASE_CHECKLIST.md`.
- Documentation closeout PR [#78](https://github.com/weekssa/OPRA-EQ-for-UAPP/pull/78), head `b63bab0afd8255748a978e82a9e2f032f8af2fe5`, merged normally as `4a15516ccc9852657716325cba629da16df27d26` / tree `bd2cd70964b1a3bb705dc5b20a22e79a19c26828`. All six applicable exact-head checks passed. All six post-merge checks on exact main passed: Android CI build/API-26/API-35 UI (`37773634404`), CodeQL (`37773634383`), dependency submission (`37773634447`), and automatic Gradle dependency submission (`37773634595`).
- Release issue #71 is closed as completed. No open v0.8.0 tracking issue or PR remains. Ten older open records remain for unrelated v0.7/EW300 history; they were preserved rather than closed as release cleanup.
- The retired beta physical-qualification automation was already absent: its deletion request returned `not_found` (“already does not exist”), and no local automation TOML exists. No active stable-promotion automation was found.
- The final compact evidence archive is `.unlazy/v080-stable-promotion/review-bundle-v0.8.0.zip`; its verified membership manifest and final checksum are retained beside the archive in the task-local closeout record.

## Preserved caveat

The 2026-10-08 dependency review found 56 open transitive Maven alerts in build/test tooling. The refreshed release runtime dependency graph contained no alert-bearing coordinates and no vulnerable app-runtime path was identified in that graph. This is not a clean vulnerability scan; no DEX scan was performed.

## Scope and evidence boundary

No physical phone, DAC, ADB/USB, TalkBack, headphones, or optional hardware work was performed for stable promotion. Existing C05-C read-only qualification remains applicable to the stable release because the production app-source tree and hardware behavior are unchanged; Class B remains the documented classification.
