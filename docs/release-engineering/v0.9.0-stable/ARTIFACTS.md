# v0.9.0 stable release artifacts and provenance

**Release status: RELEASED — VERIFIED.** This record ties the public v0.9.0 release to the approved source and official signed candidate.

## Public release

- Release: [EQ Library v0.9.0](https://github.com/weekssa/OPRA-EQ-for-UAPP/releases/tag/v0.9.0)
- Release ID: `430455005`
- Published: `2026-11-01T09:38:36Z` (2026-11-01 03:38:36 CDT)
- State: public, immutable, non-draft, non-prerelease, current latest
- Source commit: `1eebfda363a580591abdb3b109b8af5df5b82a90` (merged PR #124)
- Application: `com.weekssa.opraeqforuapp`, version `0.9.0`, version code `20`

## Official signed candidate

- Workflow: [Signed Release Candidate run 41288639063](https://github.com/weekssa/OPRA-EQ-for-UAPP/actions/runs/41288639063), successful on the exact source above
- Artifact ID: `13076990020`
- Artifact name: `EQ-Library-v0.9.0-signed-1eebfda363a580591abdb3b109b8af5df5b82a90`
- Artifact archive SHA-256: `ba5937e0191175539115df9e253c456364b6d3a17eae50ed54f381c1139bb131`
- APK: `EQ-Library-v0.9.0.apk`, 2,904,848 bytes
- APK SHA-256: `8930dfd4dac7bf97b68bbedb14e89f2cf88da0bcf927be10be8333038c0f8029`
- Signer SHA-256: `65c1c1256dae3c49e3548f334c91f0ba991969e9be9e0b223ba4e253d2114747` (`CN=OPRA EQ for UAPP, O=weekssa`)
- R8 mapping SHA-256: `40892ce76c2d0cd2fe1b8f5f22524aa648a31033e3663a296490610d4377cf96`
- APK v2 and v3 signatures, pinned certificate, package/version, manifest/checksum agreement, R8 and zip alignment verification passed.

## Exact-artifact promotion and public verification

- Workflow: [Promote Signed Release Candidate run 41289359379](https://github.com/weekssa/OPRA-EQ-for-UAPP/actions/runs/41289359379), successful; candidate verification, tag creation and publication jobs passed.
- Promotion verified the public v0.8.3 baseline APK (version code 14; SHA-256 `396244ffc26cec108b64c523a49779261656b41ab02f1bc2b63197327c576247`).
- The hosted stable clean-install and v0.8.3-to-v0.9.0 in-place upgrade smoke checks both passed. The upgrade retained app data.
- Annotated tag object: `1c4e3fdabbc7c2a97f93685abb5758263595b46b`; it peels to `1eebfda363a580591abdb3b109b8af5df5b82a90` and binds candidate run/artifact/archive identity.
- A fresh public APK download was independently SHA-256 hashed. Its bytes match the signed candidate digest above.
- GitHub release page showed the release as Latest. Release notes were edited after publication to remove stale pending-approval wording and state the completed release status. The tag, APK and assets were not changed.

## Scope and evidence boundaries

- **Goal A delivered:** J035 physically verified all five preset selections and saved volume across genuine power removal; the User 1 bank/global gain remained intact, there were zero microphone/UAC writes, and restoration was verified. J035 is limited to one Pixel 9, one JA11 at firmware 2.20, and the documented transaction.
- **Goal B deferred:** digital reconstruction-filter functionality was not delivered. The vendor implementation did not expose the control for the tested firmware; no filter write was sent. The complete research and reopening criteria remain in [`../../research/FIIO_JA11_DIGITAL_FILTER_RESEARCH_HANDOFF.md`](../../research/FIIO_JA11_DIGITAL_FILTER_RESEARCH_HANDOFF.md).
- J034 and J035 in the append-only validation ledger were not modified during release closeout.
- No physical-device action was required or performed during release closeout.
