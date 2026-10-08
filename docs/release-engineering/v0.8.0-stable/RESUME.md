# v0.8.0 stable promotion — closeout

Updated: 2026-10-08T11:46:08Z

## Current state

- Stable release promotion is complete. Public v0.8.0 is immutable, non-draft, non-prerelease, and returned by `/releases/latest`. The immutable v0.8.0-beta prerelease remains preserved; v0.7.2 is previous stable.
- Stable main SHA/tree: `54823e1a464f8b716a32c4f4808037fc0cdd99bd` / `e20330e2c4921823ecdf2881db0bf6fa1b3c3c0d`. The production app-source tree matches the qualified beta.
- Main-only signing run `37769090436`, artifact `11546549425`, APK SHA-256 `2ea1d4b76a840e7448fddba556c3dc03aba19870eeb2499182af7d5759cce54b`, signer SHA-256 `65c1c1256dae3c49e3548f334c91f0ba991969e9be9e0b223ba4e253d2114747`.
- Promotion run `37770238047` passed exact candidate verification, signed v0.7.2 and beta in-place upgrades, and signed clean-install/core smoke; annotated tag creation and public publication passed.
- Independent artifact review and independent public asset verification passed. Full details are in `ARTIFACTS.md`, `TEST_MATRIX.md`, `docs/PUBLIC_RELEASE_CHECKLIST.md`, and `.unlazy/v080-stable-promotion/GATES.md`.
- The post-publication documentation pass is complete in the documentation-only closeout branch. Its diff changes documentation files only; no app/runtime/hardware source, tag, APK, signer, or release was changed. Current main is `54823e1a464f8b716a32c4f4808037fc0cdd99bd` / tree `e20330e2c4921823ecdf2881db0bf6fa1b3c3c0d`; the exact post-publication doc delta is ready for PR and exact-head checks. G16 closes when that delta merges normally.
- Live GitHub cleanup check: beta issue #71 is closed as completed, no open issue exists, and no v0.8.0 release-tracking issue remains. The unrelated open EW300 documentation PR is historical and is preserved.
- Next: push the docs-only delta, verify exact-head CI, and merge normally. Then create and verify the compact local review bundle, retire the obsolete physical-qualification heartbeat, and close G17/Unlazy status. No release gate or physical work remains.

## Preserved caveat

The 2026-10-08 dependency refresh found 56 open Maven alerts in build/test tooling. The refreshed release runtime dependency graph contained no alert-bearing coordinates. This is not a clean vulnerability scan, and no DEX scan was performed.

## Scope

No physical phone, DAC, ADB/USB, TalkBack, headphone, or optional hardware work was performed for stable promotion. The beta's exact C05 read-only physical evidence remains applicable because the production app-source tree is unchanged.
