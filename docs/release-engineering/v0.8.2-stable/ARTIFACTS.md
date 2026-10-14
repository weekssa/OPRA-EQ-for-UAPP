# v0.8.2 stable release artifacts and verification

## Published release

- Public release: [EQ Library v0.8.2](https://github.com/weekssa/OPRA-EQ-for-UAPP/releases/tag/v0.8.2), release ID `412274351`.
- State read back from the GitHub Releases API on 2026-10-13 America/Chicago: immutable, published, non-draft, non-prerelease. `/releases/latest` returned the same release and tag.
- Production source: `9983edf0b7fe437aeeec9ac3dcb3864705afa16a`; exact-main CI run `38473044839` passed. PR #94 is merged.
- Annotated tag object: `e5d3d1150c1d981f63f7e2eaa50d5bd9a1caf939`; it peels to the exact production source above. Its message binds tag `v0.8.2`, source SHA, candidate run `38474740277`, candidate artifact `11853910967`, and artifact ZIP SHA-256 `c82b5490286d27cd3616740d6f9d8ce51c9f3ac598b1f40dac87ecaac1a76b88`.
- Package/version: `com.weekssa.opraeqforuapp`, versionName `0.8.2`, versionCode `13`.
- Candidate APK SHA-256: `df1ac78e7462da588e01f4aa39f5f23d0c05797d27eb2222d0650ce319c5d6e3`.
- Pinned signer certificate SHA-256: `65c1c1256dae3c49e3548f334c91f0ba991969e9be9e0b223ba4e253d2114747`.
- Private R8 mapping SHA-256: `7afd37afd57430b52233a1efb3c1545540544649314381b275c7892698dae00a`; the mapping was verified in the signed candidate and is intentionally not a public asset.
- Official signed candidate run `38474740277` produced artifact `11853910967`; artifact ZIP SHA-256 is `c82b5490286d27cd3616740d6f9d8ce51c9f3ac598b1f40dac87ecaac1a76b88`, expiry `2027-01-11T22:00:29Z`.
- Exact signed candidate clean install and v0.8.1 in-place upgrade passed on API 35 in run `38475878287` (validator workflow source `c5be0d9af579a8ba264884db2f13f81ad6785e1a`, distinct from APK source `9983edf…`). Promotion run `38499666593` repeated candidate identity and hosted API 35 signed install/upgrade verification, then created the tag and published. No rebuild or re-sign occurred.

## Public asset byte verification

Each of the six uploaded release assets was downloaded from its public GitHub release URL. The SHA-256 computed over each downloaded byte sequence matched the release API's asset digest.

| Public asset | Size (bytes) | SHA-256 |
| --- | ---: | --- |
| `apksigner-verification.txt` | 951 | `ad34f86ffe5fb59711128eb260f44bcce0fad0813d13a73a2a13fd9a711ccccb` |
| `candidate-manifest.json` | 504 | `69826368edc3c504c394b8aa866fd6908c6a7f98bf6b823c3fceb253a37dcfc5` |
| `EQ-Library-v0.8.2.apk` | 2,888,464 | `df1ac78e7462da588e01f4aa39f5f23d0c05797d27eb2222d0650ce319c5d6e3` |
| `EQ-Library-v0.8.2.apk.sha256` | 88 | `4d68356ef97caba9de758fc61b5e61f374916206ba0d5767cbacbe9e6206c48c` |
| `release-provenance.json` | 687 | `9a48215ed9c0e27155f6985be1c215bada664792ecaa6b348c5dcaca72b616eb` |
| `zipalign-verification.txt` | 9,983 | `f9723193be85b413af5adf7d85ea613a3bae3b36481dd119278ec224412a57c9` |

The public APK digest equals the signed candidate APK digest. The checksum sidecar, manifest, signer-verification output, alignment output, and provenance file are included in the independently verified asset set. The public release body SHA-256 is `3844791cfddd839ff4655c13c7e325b05bbdfa7e67b9ba6f28576bd1eb37050e`, matching `docs/releases/v0.8.2.md` byte-for-byte.

The app's existing release-update path reads the GitHub latest release; the public `/releases/latest` API returned tag `v0.8.2` and release ID `412274351`. No separate update feed or static version metadata was changed.

## Install, checksum, and rollback guidance

For an upgrade, install the signed v0.8.2 APK over v0.8.1 to retain app data. Verify the downloaded APK against the published SHA-256 sidecar before installation. If rollback to v0.8.1 is needed, use its [public stable release](https://github.com/weekssa/OPRA-EQ-for-UAPP/releases/tag/v0.8.1) and its signed APK/checksum. Export or otherwise preserve user data through supported app flows first; a downgrade or uninstall/reinstall rollback was not exercised in this release, and uninstalling can remove local app data. Do not treat the v0.8.2 candidate workflow artifact as a replacement for the public release assets.

## Physical acceptance boundary

J031 is PASS_WITH_LIMITS on the exact v0.8.2 candidate and the original Explorer/LoboNautics managed-profile route. One owner-controlled Flash showed same-process visible EQ and DEVICE convergence; editing and navigation worked; observed baseline values were unchanged; cleanup completed; and the phone was released. The profile fingerprint is non-unique and is not a unit identity. Raw packet/write/Save/replay counts, internal session generations, and ViewModel identity were not measured. This record makes no broader hardware capability or power-cycle persistence claim. Private raw captures, device identity, and reports remain outside the repository.
