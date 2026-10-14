# v0.8.3 stable release provenance

Status: **OWNER ACCEPTED; RELEASE PREPARATION IN PROGRESS; NO OFFICIAL CANDIDATE OR PUBLIC RELEASE YET**

## Source and physical acceptance

| Item | Identity / evidence |
| --- | --- |
| Integration PR | [#97](https://github.com/weekssa/OPRA-EQ-for-UAPP/pull/97), exact documentation head and checks to be recorded at merge |
| Tested application source | `7cea4545dddbee2da0b436e0684a9c489835e3cd`, based on `575dab59fe774d3683a3714ca490a334f1b20764` |
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
| Merged release source SHA | Pending normal merge and exact-main checks |
| Exact-main CI run | Pending |
| Signed Release Candidate workflow run | Pending |
| Immutable Actions artifact ID / archive SHA-256 | Pending |
| Candidate APK SHA-256 / size | Pending |
| APK signature, pinned signer, R8 mapping, package/version/code, alignment | Pending official workflow and independent readback |
| Hosted API 35 clean install and v0.8.2 in-place upgrade | Pending exact candidate promotion gate |
| Promotion workflow run | Pending |
| Annotated v0.8.3 tag object / peeled source | Pending |
| Public release ID / `/releases/latest` readback | Pending |
| Public asset names, IDs, sizes and SHA-256 values | Pending public readback |

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
| Git | Record from execution environment | Local system installation | Repository, branch, source and change provenance | Pending final version readback | YES |
| GitHub CLI | `2.102.0` | `/Users/stephenweeks/.local/bin/gh-v2.102.0` | PR status, checks, and authorized merge/release workflow operations | Local binary; official release workflow owns artifact/signature verification | YES |
| Gradle wrapper | `9.4.1` | Repository-pinned Gradle Wrapper | Beta-source validation was completed before this go-live phase; stable build is delegated to the pinned GitHub workflow | Exact beta source/build command in `../implementation/v0.8.3-defaults-beta/ARTIFACTS.md` | YES |
| Android SDK build tools | `36.0.0` | Official main-only GitHub workflow setup | Stable APK package/signature/alignment verification | Pending workflow evidence | NO; workflow not yet run |

Do not backfill a missing historical tool digest from current availability. Record official action/source pins and the immutable artifact and public asset digests with the final closeout.
