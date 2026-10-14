# v0.8.3 stable release provenance

Status: **PUBLISHED IMMUTABLE STABLE RELEASE; ALL AUTHORIZED MERGE, SIGNING, PROMOTION, PUBLIC READBACK, AND DOCUMENTATION GATES COMPLETE**

## Source and physical acceptance

| Item | Identity / evidence |
| --- | --- |
| Integration PR | [#97](https://github.com/weekssa/OPRA-EQ-for-UAPP/pull/97), merged normally at `899755507f812c940f3ab9381a856c80c6030885` |
| Tested application source | `7cea4545dddbee2da0b436e0684a9c489835e3cd`, based on `575dab59fe774d3683a3714ca490a334f1b20764` |
| Application source relationship | No app-source diff between tested Beta v1 source `7cea4545dddbee2da0b436e0684a9c489835e3cd` and stable main source `899755507f812c940f3ab9381a856c80c6030885`; the app changes are documentation/release integration only. Stable package, signing, and version identity differ as expected. |
| Exact-main Android CI | Run `38648012952` on exact main SHA `899755507f812c940f3ab9381a856c80c6030885`; all four jobs passed: build/unit/lint/R8, Kotlin exact-head analysis, API 26 smoke, API 35 connected UI. |
| PR-head analysis | PR #97 final head `96399e902f7390c6f27bfc42bfc412790328457a`: `validate`, `validate-priority-community`, `submit-gradle`, and Analyze Kotlin passed; generic CodeQL annotation was neutral/skipped. |
| First feature exact-head attempt | Run `38646487908` had build, API 26, and API 35 emulator jobs PASS. Its CodeQL exact-head identity guard failed because dispatch was from main while `source_sha` requested the PR commit. Actual PR CodeQL passed on that same SHA, and corrected main exact-head run `38648012952` passed the guard and CodeQL. The failed attempt is preserved; it is not relabeled as a pass. |
| Tested private beta | `com.weekssa.opraeqforuapp.v083beta`, `0.8.3-beta.1` / code 14; APK SHA-256 `ac982eafd76f4db80f48e0f26040bf1f849388c4f0a51f778b51c8c013c59ce5` |
| Beta signer | Local debug certificate SHA-256 `968b15ab4802bba98fbd058d7b81f537da6c2c5542333d0b107b3495135587b4`; testing only, not eligible for stable publication |
| Owner physical acceptance | J032 automated session: PASS WITH LIMITS; J033 owner-reported manual acceptance for identified beta and authorization to publish. Phone retains exact beta and data. No USB bus capture, unique serial/fingerprint, or independent User 1 EQ payload is present. |
| Physical final state | Volume 19, program Classic, Mic On, UAC 2.0. Do not retest or mutate the owner's phone during release. |

The application implementation is frozen at the beta source above. Changes after that source are release documentation only. Candidate equivalence and any later material-change decision must be assessed against the implementation tree, exact beta APK and owner acceptance; the debug beta APK itself is not promotable.

## Official stable candidate

| Field | Expected / result |
| --- | --- |
| Stable application ID | `com.weekssa.opraeqforuapp` |
| Version | `0.8.3`, versionCode `14` |
| Stable signing certificate SHA-256 | `65c1c1256dae3c49e3548f334c91f0ba991969e9be9e0b223ba4e253d2114747` |
| Merged release source SHA | `899755507f812c940f3ab9381a856c80c6030885` |
| Exact-main CI run | `38648012952`, all four jobs successful |
| Signed Release Candidate workflow run | `38649163805`, successful from exact main SHA |
| Immutable Actions artifact ID / archive SHA-256 | `11934142420` / `500c5c693ea41f35e5025041b9fddebab3d44c863151411d871a8255cc6e31bb` |
| Candidate APK SHA-256 / size | `396244ffc26cec108b64c523a49779261656b41ab02f1bc2b63197327c576247` / 2,904,848 bytes |
| APK signature, pinned signer, R8 mapping, package/version/code, alignment | v2/v3 signature, signer `65c1c1256dae3c49e3548f334c91f0ba991969e9be9e0b223ba4e253d2114747`, mapping `45601e0f655cb82f5965b2077faebb310a8009826ceb7bd9cb30966d20ed16d9`, package/version/code as expected, zipalign verified |
| Hosted API 35 clean install and v0.8.2 in-place upgrade | Both passed in promotion run `38650109951` before tag creation |
| Promotion workflow run | `38650109951`, all three jobs successful; no rebuild or re-sign |
| Annotated v0.8.3 tag object / peeled source | `1a584839b699ce2f0ff447582719fbeb19915392` / `899755507f812c940f3ab9381a856c80c6030885` |
| Public release ID / `/releases/latest` readback | Release `413252769`, immutable, non-draft, non-prerelease; latest resolves to `v0.8.3` |
| Public asset names, IDs, sizes and SHA-256 values | Six assets independently downloaded and all six digests match GitHub metadata; full table below |

## Public asset readback

All public downloads matched the asset SHA-256 returned by GitHub. APK download bytes match the signed candidate and checksum sidecar.

| Asset | ID | Size (bytes) | SHA-256 |
| --- | ---: | ---: | --- |
| `EQ-Library-v0.8.3.apk` | `641931064` | `2904848` | `396244ffc26cec108b64c523a49779261656b41ab02f1bc2b63197327c576247` |
| `EQ-Library-v0.8.3.apk.sha256` | `641931163` | `88` | `573897f249a60e9202d0e670102d1b5c303adaf4ef7ad4a01c60f778c0a74a74` |
| `apksigner-verification.txt` | `641931148` | `951` | `ad34f86ffe5fb59711128eb260f44bcce0fad0813d13a73a2a13fd9a711ccccb` |
| `candidate-manifest.json` | `641931189` | `504` | `73dfaa698c588e2c68c86ff0732004d04c5b40f60dc6b4772a69c2370f22c66b` |
| `release-provenance.json` | `641931207` | `687` | `0a16c726a831ec30c07f2e729c6e67ec9fc23c8f5134efb3ad6b9cb3c4930338` |
| `zipalign-verification.txt` | `641931115` | `9983` | `80c534bc00ddee2c7627c117af4f6270ebc36191812e87bada871d66ce4190ec` |

Tag `v0.8.3` is annotated at object `1a584839b699ce2f0ff447582719fbeb19915392` and peels to main source `899755507f812c940f3ab9381a856c80c6030885`. The public release page is https://github.com/weekssa/OPRA-EQ-for-UAPP/releases/tag/v0.8.3; `/releases/latest` returns release ID `413252769`, tag `v0.8.3`, immutable `true`, draft `false`, prerelease `false`. The release body matches `docs/releases/v0.8.3.md`.

Do not create the stable tag or publish manually. Use the main-only Signed Release Candidate and Promote Signed Release Candidate workflows. Promotion must reuse the exact signed artifact and verify that main has not moved. Complete the empty fields from workflow and public readback evidence before marking this record complete.

## Owner authorization and evidence boundary

On 2026-10-14 the owner reported that the identified Beta v1 passed manual testing and that all other manual tests passed, noted that operation counters are not USB bus captures, confirmed original Mic restoration and no native EQ Save/UAC/EQ-band write, and authorized final go-live. J033 preserves the statement as owner-reported acceptance. It does not retroactively add bus traces, a unique hardware identity, or an independent EQ-bank measurement to J032. Do not make User 1 preservation, same-unit continuity, or bus-level count claims.

## Execution provenance

Skills and tools are distinct. Record only resources actually invoked during this release; use `not declared` for unavailable version information and `NO` only for inspected-but-uninvoked resources.

| Skill | Source/version | Purpose in this release | Actually invoked |
| --- | --- | --- | --- |
| `android-skills:android-dev` | Installed Android skills bundle `5.6.0` | Android repository build and release workflow context | YES |
| `unlazy` | `/Users/stephenweeks/.agents/skills/unlazy`; version not declared | Acceptance gates and bounded execution checklist | YES |

| Tool | Version | Trusted source | Purpose in this release | Verification/hash | Actually invoked |
| --- | --- | --- | --- | --- | --- |
| Git | `2.50.1 (Apple Git-155)` | Local system installation | Repository, branch, source and change provenance | Version read during release closeout | YES |
| GitHub CLI | `2.102.0` | `/Users/stephenweeks/.local/bin/gh-v2.102.0` | PR status, checks, and authorized merge/release workflow operations | Local binary; official release workflow owns artifact/signature verification | YES |
| Gradle wrapper | `9.4.1` | Repository-pinned Gradle Wrapper | Beta-source validation was completed before this go-live phase; stable build is delegated to the pinned GitHub workflow | Exact beta source/build command in `../implementation/v0.8.3-defaults-beta/ARTIFACTS.md` | YES |
| Android SDK build tools | `36.0.0` | Official main-only GitHub workflow setup | Stable APK package/signature/alignment verification | Official artifact manifest, signer report, zipalign report; APK independently checked locally | YES |

Do not backfill a missing historical tool digest from current availability. Official workflow actions are commit-pinned in `.github/workflows/github-release.yml`, `.github/workflows/promote-signed-release.yml`, and the exact-head workflow. The official candidate artifact and six public asset digests are recorded above. The private R8 mapping is retained only in the Actions candidate artifact and is not a public release asset.
