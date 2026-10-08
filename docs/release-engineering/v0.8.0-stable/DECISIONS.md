# v0.8.0 stable promotion decisions

- **Promote the existing qualified beta source.** The production app tree at current main is identical to the qualified beta production tree; do not reopen beta or physical qualification.
- **Version stable `0.8.0` / code `11`.** Beta already used code 10; code 11 is the next legal upgrade code.
- **Keep beta and stable artifacts separate.** Use the immutable beta only as a baseline for its required in-place upgrade. The stable APK must be newly built and signed from exact merged main.
- **Keep v0.7.2 latest until publication.** README download/latest references remain on v0.7.2 until public release readback verifies v0.8.0.
- **Retain dependency uncertainty.** The current Dependabot inventory and alert-to-runtime mapping are unverified after the earlier endpoint rejection. Do not retry the endpoint and do not claim a clean scan.
- **No physical hardware work.** Stable metadata, docs, UI-test fixtures, and release tooling do not invalidate the existing physical qualification.
