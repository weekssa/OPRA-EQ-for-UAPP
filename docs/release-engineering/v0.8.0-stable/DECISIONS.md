# v0.8.0 stable promotion decisions

- **Promote the existing qualified beta source.** The production app tree at current main is identical to the qualified beta production tree; do not reopen beta or physical qualification.
- **Version stable `0.8.0` / code `11`.** Beta already used code 10; code 11 is the next legal upgrade code.
- **Keep beta and stable artifacts separate.** Use the immutable beta only as a baseline for its required in-place upgrade. The stable APK must be newly built and signed from exact merged main.
- **Keep v0.7.2 latest until publication.** README download/latest references remain on v0.7.2 until public release readback verifies v0.8.0.
- **Retain dependency uncertainty.** GitHub's branch-push response on 2026-10-08 reported 56 repository vulnerabilities. The public Dependabot page returned 404 that day, and item-level alerts/runtime mapping remain unavailable after the earlier endpoint rejection. Do not claim a clean scan or assert that current runtime exposure is ruled out.
- **No physical hardware work.** Stable metadata, docs, UI-test fixtures, and release tooling do not invalidate the existing physical qualification.


### Stable publication closeout — 2026-10-08

The condition above is now satisfied: v0.8.0 was published stable from exact main `54823e1a464f8b716a32c4f4808037fc0cdd99bd`, after signing, both upgrade paths, clean-install smoke, and independent artifact review passed. Public readback confirms immutable release `406758581`, `draft=false`, `prerelease=false`, and latest. The v0.8.0-beta release/tag remain preserved as immutable prerelease history; v0.7.2 is now previous stable. The dependency caveat remains: 56 open Maven build/test-tool alerts, with no alert-bearing coordinate in the refreshed release runtime graph; no clean scan or DEX scan is claimed.
