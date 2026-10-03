# Public release checklist: EQ Library

This document records the GitHub-release gates for EQ Library. Google Play remains intentionally out of scope until a later product decision.

The checklist is organized around the **current release state**. Detailed historical implementation/testing evidence remains in the versioned release notes and hands-on checklists instead of being duplicated indefinitely here.

## Repository release readiness

- [x] Apache-2.0 software license present.
- [x] Software provenance documented in `NOTICE`.
- [x] Source/data licensing and attribution documented separately in `DATA_LICENSE.md` and `NOTICE`.
- [x] Standalone privacy policy present in `PRIVACY.md`.
- [x] Contribution and security-reporting guidance present.
- [x] `.gitignore` excludes Android keystores, APK/AAB outputs, local configuration, Google Services configuration, IDE state, and build artifacts.
- [x] Android manifest requests only the network permission required by the app's public catalog/update checks.
- [x] Repository visibility is public.
- [x] Normal Android CI validates unit tests, Android lint, debug assembly, and unsigned release assembly without publishing development APKs.
- [x] GitHub Actions dependencies are pinned and repository security/dependency checks are enabled.
- [x] One permanent Android release-signing identity is established and its public certificate fingerprint is pinned in `release-signing-cert.sha256`.
- [x] Candidate signing is separate from public publication. The main-only
  `.github/workflows/promote-signed-release.yml` publisher promotes an immutable verified candidate
  and never rebuilds or re-signs the APK.
- [x] The repository front page describes the current **EQ Library** product rather than the original OPRA-only workflow.

## Current v0.7.1 state: source-wide Favorite fix

- [x] Public release [v0.7.1](https://github.com/weekssa/OPRA-EQ-for-UAPP/releases/tag/v0.7.1) is published, non-draft, non-prerelease, and returned by `/releases/latest`.
- [x] Release ID `401454172` was published at `2026-10-02T00:32:34Z` with curated v0.7.1 notes.
- [x] Tag `v0.7.1` resolves to exact source `c48f6a5daa08a5e03475b2e415fe80b41d3357db`, the merged PR #62 source.
- [x] PR #62 publisher repair passed independent review and all seven exact-head checks at `e3c890e88e86491aa21759bfdfd2fc4d36bcc269`; it merged under the owner's conditional approval. All six applicable post-merge checks passed.
- [x] Fresh Signed Release Candidate run [#16 / 36945247310](https://github.com/weekssa/OPRA-EQ-for-UAPP/actions/runs/36945247310) succeeded from exact main source `c48f6a5daa08a5e03475b2e415fe80b41d3357db`; artifact ID `11201724340`, ZIP SHA-256 `8716136285cd7d77c15c3e850c319ad121e4d4177338b3c1eb1d59594ac95918`.
- [x] Candidate APK `EQ-Library-v0.7.1.apk` is package `com.weekssa.opraeqforuapp`, version `0.7.1`, code `8`, SHA-256 `abd8837f78aaf72d28abef3db956a1c171f791616effbc8f7875814c2c28002b`. Pinned signer certificate SHA-256 is `65c1c1256dae3c49e3548f334c91f0ba991969e9be9e0b223ba4e253d2114747`; R8 mapping SHA-256 is `a6daea5036e4dbaefbf276f6f087da33c9d1c2b4635460f24e2c3f1a8df0cef5`.
- [x] Release build, unit tests, lint, R8, APK checksum/signature, v2/v3 signing, pinned certificate, package/version, and zip alignment passed. The hosted API 35 promotion gate installed public v0.7.0, upgraded in place to the exact candidate, and cold-launched successfully.
- [x] Promotion run [#4 / 36946254797](https://github.com/weekssa/OPRA-EQ-for-UAPP/actions/runs/36946254797) verified the candidate and repaired the existing asset-free private draft only after exact ancestry and target readback checks, then published and verified the exact tag/source.
- [x] All six public release assets were downloaded and their SHA-256 digests matched GitHub release metadata. The public APK bytes match the exact signed candidate; signer and zip-alignment reports pass; provenance records source, candidate run/artifact, promotion run, APK digest, tag, and signer:
  - `apksigner-verification.txt`: `ad34f86ffe5fb59711128eb260f44bcce0fad0813d13a73a2a13fd9a711ccccb`
  - `candidate-manifest.json`: `886668cdea7590be90f729807bb3b5ae8f5f92517ac86687f32840744fb48699`
  - `EQ-Library-v0.7.1.apk`: `abd8837f78aaf72d28abef3db956a1c171f791616effbc8f7875814c2c28002b`
  - `EQ-Library-v0.7.1.apk.sha256`: `45692400347998e093e929fad428e361dd6f4da4a6a8fe68b562a32265c69622`
  - `release-provenance.json`: `be423c9a97397a2dbb7edfe12d09580334fd96a666792f85677d38d6d2492bd1`
  - `zipalign-verification.txt`: `1cbca796394740ae9826e89e72833b5a04bfa64e55319b25fdd3fda564b68d86`
- [x] Favorite source coverage includes 14 source-ID samples across 13 profiles: 12 headphone samples exercise the shared headphone resolver, 2 General EQ samples exercise the General resolver, and 2 registered no-profile sources have reviewed exclusions. CI requires each new ingested source ID to add an authentic sample or reviewed exclusion and exercises the production resolver.
- [x] No physical device, wireless debugging, DAC, or hardware mutation was required; this fix changes source-neutral catalog Favorite resolution and no DAC behavior.
- [x] After publication, the owner confirmed that the Favorite fix works. This is owner-reported functional confirmation and adds no hardware-support claim.

Status: **Published under the owner's conditional approval; all merge, candidate, promotion, and post-publication gates passed.** The release makes no new hardware-support claim. See [v0.7.1 release notes](releases/v0.7.1.md) and the latest closeout entry in `docs/CHATGPT_PROJECT_RUNBOOK.md`.

## v0.7.2 stabilization preparation

v0.7.1 remains the current public release until the signed v0.7.2 candidate is independently verified, tagged, and published. The stabilization branch targets versionName `0.7.2` and versionCode `9` from the immutable v0.7.1 base.

- [x] Version metadata is set to `0.7.2` / `9` without changing package ID or SDK levels.
- [x] User-facing v0.7.2 notes describe only implemented behavior and retain existing hardware/UAPP boundaries.
- [x] Independent dense DSP tests reproduce the old high-Q response miss and cover the production correction.
- [x] Product source `1c36349ca3edb69061a34b44d385670380f60512` passed local Gradle, R8, API 35 instrumentation, Python/catalog/release, actionlint, and ShellCheck gates. Fresh wipe/install API 26 ARM64 smoke rendered the manufacturer, 1MORE model, and `oratory1990` profile, then remained resumed/alive for 60 seconds without an AndroidRuntime error. It used a temporary signer and left 2,041 KiB free in the 48 MiB Dalvik heap. Later exact-head CI and review on `e7f2fc937e9b265770296dbdc4cbb40a4e5e13c9` passed before PR #65 merged at `a60411bebfdbd1cea4218d3bde45013bb7ed26a9`.
- [x] Exact-head independent review returned PASS with no P0-P2 findings on `e7f2fc937e9b265770296dbdc4cbb40a4e5e13c9`; all eight GitHub checks passed. API35 instrumentation passed 25/25 and API26 x86_64 minified cold-install smoke passed.
- [x] PR #65 merged normally without bypass at exact main SHA `a60411bebfdbd1cea4218d3bde45013bb7ed26a9`. The merged Git tree equals the reviewed and checked PR head.
- [x] Post-merge local Gradle validation on `a60411be` passed 731 JVM tests, lint with 0 errors/111 warnings/2 hints, debug and release assembly, and R8 mapping verification.
- [x] Official Signed Release Candidate run `37095180116` succeeded from exact source SHA `a60411bebfdbd1cea4218d3bde45013bb7ed26a9`. Artifact `11264251526` has ZIP digest `sha256:1da409dcf47368ec254a5432e7c1d316473920c28e27c19416b10cc8e29969d9`; APK `EQ-Library-v0.7.2.apk` SHA-256 is `f3afaa102a31491286828faa37cfe1454853721d1e4aa736da57bf4919e89ded`, package/version `com.weekssa.opraeqforuapp` / `0.7.2` / code `9`, signer matches the pinned certificate, and R8 mapping digest is `2ef382f4838c49c89fbea9985fdba976bedef5202aa59711dcbe263690515021`. Independent local promotion verification passed against the GitHub artifact digest and latest public v0.7.1 APK.
- [!] Promotion run `37096259477` completed its API 35 v0.7.1 install, exact candidate upgrade, and cold launch successfully, but its tag job failed because the publisher compared GitHub's `sha256:`-prefixed digest against a prefix-stripped input. The publish job was skipped; remote v0.7.2 tag and release remain absent. Fix and merge the publisher comparison with a regression test, then create a fresh candidate from the new exact main SHA before retrying promotion. Earlier beta run `37093821378` is only testing evidence; its APK went to temporary `mobile-test-apk` and is not eligible for release promotion.
- [ ] Immutable `v0.7.2` tag and public release assets are verified after publication.

Status: **PR #65 is merged and post-merge tests pass. The official candidate and API 35 upgrade gate passed, but promotion run `37096259477` failed at the annotated-tag digest comparison; publication was skipped. The publisher fix now passes 39 focused tests, the full 238-test Python suite, and the release contract. Independent review and normal PR checks/merge remain before a fresh candidate is built from corrected main. v0.7.1 remains latest and no v0.7.2 tag or release exists.** The beta APK published to temporary `mobile-test-apk` is testing material only. Full evidence, including one contaminated API 26 attempt and the clean exact-source rerun, is maintained in `docs/release-engineering/v0.7.2/`.

## Previous v0.7.0 state

- [x] Exact v0.7.0 software, security, dependency, release, and signed-artifact gates completed.
- [x] Public signed APK, checksum, signer verification, immutable workflow artifact, install, and cold launch recorded.
- [x] The public README and curated `docs/releases/v0.7.0.md` release notes describe the current product and evidence boundaries.
- [x] Owner approval to merge, publish, and complete the bounded release closeout received.
- [x] Public tag `v0.7.0`, release assets, latest-release metadata, and APK digest verified.
- [ ] `actionlint` was unavailable locally. No workflow file changed during final closeout, and the trusted remote release workflow passed.

Status: **Published and owner-accepted.** The public release is complete. Historical physical
evidence remains source-, candidate-, device-, and operation-specific; do not repeat Save,
Apply, Flash, Restore, Reset, or read-only qualification solely because the documentation closeout
created a newer documentation commit. Black Pearl is publicly described only with its exact AFUL
Explorer evidence boundary. JA11 software is verified, while broader power-cycle qualification is
not claimed.

## Historical v0.7.1 candidate and promotion checkpoints

The following chronology records candidate and publisher retry states as they occurred. The
current release status above supersedes any statement here that v0.7.0 was latest or v0.7.1
publication was pending.

The only app-production behavior change since public v0.7.0 is the canonical Favorite product-alias
correction. PR #51 contains the fix; PR #52 adds the current source-kind regression matrix and is
merged at `70a240a458f1e3cb6607d8349bb422d78cc95699`. Release-preparation PR #53 passed all exact-head
checks and merged normally to main at `5c05b0c3ac06e4d8eb868b6232a81651ac060da5`. The signed
v0.7.1 / `versionCode 8` candidate from that exact main commit passed independent artifact checks
and API 36 Favorite save/restart validation. At that early checkpoint, v0.7.0 remained the latest
public release and no v0.7.1 tag or release existed.

At the start of the release sequence, the initial v0.7.1 release candidate and replacement signed
beta #1376 predated then-current `main` and were not eligible for public promotion. The source-wide
fix merged in PR #56; the signed beta workflow correction merged in PR #57; documentation closeout
merged in PR #58. Candidate #1376 remains historical install/cold-launch evidence for unchanged
app code; candidate #16 is the published release artifact.

- [x] Focused Favorite source-kind matrix, full JVM suite, Room persistence tests, and documented
  API 36 Favorite/restart smoke passed; see `docs/AFUL_EXPLORER_FAVORITE_REVIEW.md`.
- [x] PR #52 exact-head Android CI, CodeQL, catalog-currentness, and priority-community checks
  passed; its exact merge commit's main checks also passed.
- [x] Patch release notes exist at `docs/releases/v0.7.1.md`; version assertions in the signed
  release-candidate and signed-beta workflows match `0.7.1` / code `8`.
- [x] Local unsigned v0.7.1 release assembly, 712 JVM tests, and Android lint pass; `aapt` confirms
  package `com.weekssa.opraeqforuapp`, version `0.7.1`, and code `8`.
- [x] Release-preparation PR #53 passed Android CI run `36826035050`, CodeQL run `36826035019`,
  Catalog currentness run `36826035036`, and Priority community coverage run `36826035015` at exact
  head `ceb36062aa7b3dd4e3e8cbb4066892551778d143`; it merged normally after owner approval. The
  exact merge commit's main checks also passed.
- [x] Signed Release Candidate workflow run #11 / run ID `36831766816` succeeded from main SHA
  `5c05b0c3ac06e4d8eb868b6232a81651ac060da5`, tag input `v0.7.1`. Actions artifact ID:
  `11148046368`; artifact ZIP SHA-256:
  `5735a433e52ec34e075aa4a6a5044aae77d9efb8a8a6a7386a614b26d5c87acb`; expiration: 2026-12-30.
- [x] Candidate APK `EQ-Library-v0.7.1.apk`: SHA-256
  `cfdc688a0ff9392f5617b05719b09c9c478cf07390aa7dc79e8d4e33c6eecba3`; package
  `com.weekssa.opraeqforuapp`; version `0.7.1`, code `8`; signer certificate SHA-256
  `65c1c1256dae3c49e3548f334c91f0ba991969e9be9e0b223ba4e253d2114747`; R8 mapping SHA-256
  `715a5a42e51e021a21c7957bb78ed235edcd851bd15cf7c884af8e2745f67a0d`. Independent checksum,
  `apksigner`, signature scheme, `zipalign`, package, and version verification passed.
- [x] The public v0.7.0 APK matched its published SHA-256 and release signer. It reproduced the
  Jaytiss/Hifiguides Favorite failure on a fresh API 36 AVD. After in-place upgrade to the exact
  signed candidate, Jaytiss/Hifiguides saved successfully; a separate Fahryst/AutoEQ Favorite also
  saved. My EQs showed both after force-stop and cold relaunch. This fresh AVD began with an empty
  saved list, so a pre-existing Favorite migration was not exercised. The temporary AVD was deleted
  after testing; the existing `codex-api36` AVD was left unchanged.
- [x] The source-wide Favorite fix passed the one-candidate-per-source review, exact-head
  independent review, and remote automation. PR #56 exact head
  `87842777b4d40f5f95e8d6e30e9f775a736e0b34` merged at
  `66d32756d682405c8617380a4c64f5e25a5699f6`. The fix preserves profile-wide OPRA identity when
  projecting through product aliases and fails closed when complete revision identities conflict.
- [x] Current source coverage includes 14 source IDs and 13 profiles; 12 real profile/revision
  samples exercise the shared headphone Favorite resolver, and 2 General EQ samples exercise the
  separate exact General resolver. Two registered no-profile sources have reviewed exclusions.
  CI requires an authentic sample or reviewed exclusion for each source as new sources are added.
  The checker and its 5 tests passed again against the later catalog-only main SHA
  `68d5e2e745dacbf26ecf15947d199cdd8667951a`.
- [x] The complete local suite passed on the reviewed candidate: 713 Android JVM tests, 199 Python
  tool tests, 6 API 36 Room persistence tests, and temporary API 36 Favorite save/restart/remove UI
  smoke. The exact PR head had 8/8 applicable remote checks and an independent PASS. No physical
  phone, DAC, wireless debugging, or USB mutation was needed for the source-resolution path.
- [x] Initial signed beta run #1375 / ID `36905949756` built from exact merge SHA
  `66d32756d682405c8617380a4c64f5e25a5699f6` and passed build, R8, pinned signer, APK signature,
  alignment, hosted API 35 install, and cold launch. Post-run review found stale
  `merge/publication approval` text in its manifest; run #1375 and its APK are superseded and must
  not be used as the current handoff candidate. APK SHA-256:
  `9c4cf12d8f95525e8ba4b42a640512fdac91ba551583714c49d642f6eaa2639f`. Signer certificate SHA-256:
  `65c1c1256dae3c49e3548f334c91f0ba991969e9be9e0b223ba4e253d2114747`. Artifact ID `11184356919`,
  ZIP SHA-256 `b5fad8c72744ae5fd03e41225db58986070396634a3ceaec3113b5aa7d9fec41`, expires
  2026-10-15. The superseded APK was published as an immutable temporary test candidate at
  [EQ-Library-v0.7.1-beta-66d3275.apk](https://raw.githubusercontent.com/weekssa/OPRA-EQ-for-UAPP/mobile-test-apk/candidates/EQ-Library-v0.7.1-beta-66d3275.apk).
- [x] Initial temporary mobile-test branch publication for superseded run #1375 was present at
  branch commit `b24610d8861b4f57b176b990894c12a465f5bd0a`; its APK matched the #1375 workflow
  artifact and manifest.
- [x] Replacement signed beta run #1376 / ID `36913222922` built from exact main SHA
  `38302d6b880fcb1b384a4c290d8539260bd4a138`, target `ja11`, and passed the corrected manifest
  assertions, unit/lint/release build, R8, pinned signing, signature, alignment, hosted API 35
  install, and cold launch. Artifact ID `11188327665`; ZIP SHA-256
  `26894f2c0a8a490450418693c8b2c34f6217c6e8f64e9a5e1411ac3992cb76d0`; expiration 2026-10-15
  19:27:31 UTC. APK SHA-256:
  `dfdac7782d0545a652cd5eec6e8d6ede60e748da746c0fbec4514c8b3ddaa7d2`; signer SHA-256
  `65c1c1256dae3c49e3548f334c91f0ba991969e9be9e0b223ba4e253d2114747`; R8 mapping SHA-256
  `048f3f5267c387c0a6b4f483c356a52e7334c636eaa0e7b46692762aab3affb8`.
- [x] Immutable testing-channel APK
  [EQ-Library-v0.7.1-beta-38302d6.apk](https://raw.githubusercontent.com/weekssa/OPRA-EQ-for-UAPP/mobile-test-apk/candidates/EQ-Library-v0.7.1-beta-38302d6.apk)
  matches the artifact and manifest APK SHA-256. Its `mobile-test-apk` branch tip is
  `dc3fdae75998f4a74f90581d0f53b3f860841ee7`.
- [x] The owner conditionally authorized public v0.7.1 publication on 2026-10-01, provided all
  software, review, candidate, and promotion checks pass. This approval applies to this source-wide
  Favorite correction; no separate hardware-support claim is authorized.
- [x] The main-only exact-artifact publisher merged through PR #59 as
  `51992318d517bf443aa0cd4fb02b610a9916c133`. PR #60's independently reviewed v4 sidecar contract
  fix passed all 7 exact-head checks and merged as
  `9ad72df85b163fa41ff604eed1d7e6dfc8af3230`; all 6 applicable post-merge checks passed.
- [x] Fresh Signed Release Candidate run #14 / ID `36935224537` succeeded from exact main SHA
  `9ad72df85b163fa41ff604eed1d7e6dfc8af3230`, tag input `v0.7.1`. Artifact ID `11198451366`,
  artifact ZIP SHA-256 `51a029cc065068277faae91a2009f1ef7b299b2d77c94bbf07941c6f9d7d2d1d`, expires
  2026-12-30 22:28:02 UTC. The APK SHA-256 is
  `4a56d09d9e08e4949272c18e980a3f4e673a7d155aa0c51851d371cb3de633ea`.
- [x] Promotion run #2 / ID `36936467429` independently verified that candidate's artifact digest,
  manifest, APK checksum, package/version, signer, signatures, alignment, and R8 mapping. It also
  verified the latest public v0.7.0 baseline APK SHA-256
  `27dada499bcbf9be9bd21d1349164858c93a5d2b83f78fd61134de13b4eb4025`.
- [x] At that checkpoint, promotion run #2 stopped before baseline installation because the API 35 step referenced
  `needs.verify-candidate.outputs.*` from inside that same job; GitHub expanded the expected
  version values to empty strings. The publish job was skipped. No `v0.7.1` tag or release was
  created; latest remains v0.7.0. PR #61 adds a same-job output contract regression and changes the
  emulator step to consume `steps.verify.outputs.*`. Local validation passed all 24
  promotion-verifier tests, the workflow contract check, and `git diff --check`; PR review and
  exact-head automation were subsequently completed by PR #61.
- [x] PR #61 passed independent review, all 7 exact-head checks, and all 6 post-merge checks. It
  merged at exact main SHA `b55b0b29d1f963198c5ca4f724a9303001781cce`.
- [x] Fresh Signed Release Candidate run #15 / ID `36939892786` succeeded from exact main SHA
  `b55b0b29d1f963198c5ca4f724a9303001781cce`, tag input `v0.7.1`. Artifact ID `11199164815`, size
  2,220,892 bytes, ZIP SHA-256
  `d5bc88dcf84297fddea7a14c62d35e418c2b7c0de0acaa4ac1ef491f29423690`, expires 2026-12-30
  23:16:24 UTC. Release build, unit tests, lint, R8, signing, signature, and alignment passed.
- [x] Promotion run #3 / ID `36940581035` reverified candidate #15 and passed the API 35 public
  v0.7.0 install, in-place candidate upgrade, and cold launch. Publishing then stopped because the
  verifier expected a Git tag ref before publishing the private draft. GitHub had created private
  draft release ID `401429211` with `target_commitish` equal to the exact candidate source but no
  tag ref and no assets. The public `v0.7.1` tag remains absent and `/releases/latest` remains
  v0.7.0. The release API permits changing `target_commitish` on a draft; the repaired publisher
  will only retarget this draft when it is asset-free, has no tag ref, and the previous full source
  SHA is an ancestor of the new exact candidate source.
- [x] PR #62 passed independent review and all seven exact-head checks, then merged at
  `c48f6a5daa08a5e03475b2e415fe80b41d3357db`; all six applicable post-merge checks passed.
- [x] Signed candidate #16 and promotion #4 ran on that exact source. The API 35 upgrade and cold
  launch passed, the safe private-draft retarget/readback completed, and the public tag, six assets,
  APK digest, signer, provenance, release notes, and `/releases/latest` were verified.

No DAC hardware mutation or physical-device qualification is needed for this catalog Favorite
identity fix. See the 2026-10-01 entry in `docs/CHATGPT_PROJECT_RUNBOOK.md` for the complete
source SHA, candidate provenance, UI result, and qualification limits. Check each GitHub Actions
artifact's `expires_at` before relying on it; requalify if the artifact expires or its source
changes.

## Continuing release invariants

These apply to every installable GitHub release:

- Keep application ID `com.weekssa.opraeqforuapp` unchanged.
- Keep the permanent release-signing identity unchanged.
- Increment Android `versionCode` for every installable release.
- Use SemVer `0.x` during development; reserve `v1.0.0` for the first stable release.
- Update `CHANGELOG.md` and curated release notes for every release.
- Build, test, and sign from the exact intended source commit.
- Require the applicable automated gates before physical qualification/publication.
- Require hands-on testing whenever the release changes behavior covered by a physical hardware qualification gate.
- Never replace an already-published APK with differently signed or different-content bytes under the same version/tag.
- Never commit signing keys, passwords, tokens, or credentials.

# v0.6.0: COMPLETE

**Published:** 2026-09-15  
**Tag:** `v0.6.0`  
**Release:** https://github.com/weekssa/OPRA-EQ-for-UAPP/releases/tag/v0.6.0  
**Release workflow:** https://github.com/weekssa/OPRA-EQ-for-UAPP/actions/runs/35008912862  
**Release source:** `e5ffa5d00862edc3b79bf52e1508db5244845e94`  
**Behavior evidence source:** `eef5633e18a4ac311f110493e29633bf382675e3`  
**Public APK:** `EQ-Library-v0.6.0.apk`  
**Public APK SHA-256:** `93b5250d7f32b068f24702c9fbfacb697e7921100250ee0652981100e29adb62`  
**Signing certificate:** `65c1c1256dae3c49e3548f334c91f0ba991969e9be9e0b223ba4e253d2114747`

Status: **Published.** Implementation, automated gates, signed-candidate verification, owner-reported Pixel 9 Black Pearl/UI smoke testing, controlled publication, and public asset verification passed.

## Product and documentation

- [x] `docs/releases/v0.6.0.md` contains curated release notes.
- [x] `CHANGELOG.md` records the My DAC, operation-feedback, recovery, validation, and public-release changes.
- [x] `README.md` describes v0.6.0 as the current public release and includes the public APK checksum.
- [x] `docs/V0.6_MY_DAC_STATUS.md` records exact behavior evidence, merge/publication provenance, and owner-reported Pixel 9 PASS.
- [x] FiiO JA11 physical qualification is explicitly deferred and is not a release blocker.

## Automated and physical validation

- [x] Android unit tests and lint passed.
- [x] Debug and unsigned release APK assembly passed.
- [x] CodeQL passed.
- [x] Catalog currentness and priority community coverage passed.
- [x] Signed beta workflow passed certificate verification and published the exact candidate.
- [x] Owner-reported Pixel 9 smoke passed routine Black Pearl DEVICE changes, stable header/bottom feedback, and Restore defaults with the approved EQ-flat behavior.

## Publication

- [x] PR #16 marked ready and merged.
- [x] Signed GitHub Release workflow run in publish mode for `v0.6.0`.
- [x] Public release assets, checksum, signer verification, and latest-release metadata verified.
- [x] README current-release section updated to the published v0.6.0 APK.

# v0.5.0: COMPLETE

**Published:** 2026-09-09  
**Tag:** `v0.5.0`  
**Exact release source:** `ff2fa351d5f38f9dcf37a77859f1e988bbdb76a8`  
**Signed GitHub Release run:** #5 / run ID `34341588059`  
**Public APK SHA-256:** `58e6ac5c62f9af1caf354f97cf2e7d9e2bcddea9937fac3c35532c279cd429eb`

Status: **Phase 1 implementation/release preparation and Phase 2 release-candidate testing are complete. The controlled public v0.5.0 publication and post-publication verification are also complete.**

Phase 2 tested the same v0.5.0 milestone built in Phase 1. Testing did not create a separate installable version and did not require a v0.6.0 version bump.

## Product/source state

- [x] `versionName` is `0.5.0` and `versionCode` is `5`.
- [x] Application ID remains `com.weekssa.opraeqforuapp`.
- [x] `CHANGELOG.md` contains the v0.5.0 feature/change/validation record.
- [x] Curated `docs/releases/v0.5.0.md` release notes are present.
- [x] README/front-page copy describes the shipped v0.5.0 output registry and hardware qualification state.
- [x] The permanent Android signing identity remains pinned and unchanged.

## Phase 1: implementation and automated candidate qualification

Candidate source `30535bd3b1bce9940d23e8735d88a4d9b6a9a4ef` contains the MAD-style architecture refactor while preserving device/DSP/conversion behavior.

- [x] Android CI run #1018 / run ID `34295024047` completed successfully.
- [x] CodeQL run #899 / run ID `34295024125` completed successfully.
- [x] Signed EQ Library Beta Candidate run #691 / run ID `34295020653` completed successfully.
- [x] Catalog currentness passed for the candidate source.
- [x] Priority community coverage passed for the candidate source.
- [x] Automatic Dependency Submission run #1143 / run ID `34295022902` completed successfully.

Pinned signed candidate record:

- APK: `EQ-Library-v0.5.0-beta-30535bd.apk`
- APK SHA-256: `5a2d4ff47097b1ba37b6bd625a4bfd3de444bf1895d4c0d0484fa2075adea042`
- Signer DN: `CN=OPRA EQ for UAPP, O=weekssa`
- Signer certificate SHA-256: `65c1c1256dae3c49e3548f334c91f0ba991969e9be9e0b223ba4e253d2114747`
- Actions artifact ID: `10082967650`
- Artifact ZIP SHA-256: `c55d7b53e7b355e85a5b54b2b9a0da6925448c35563f85796605885ad6de91c3`

## Phase 2: Pixel 9 release-candidate testing

- [x] `docs/V0.5_HANDS_ON_RELEASE_CHECKLIST.md` completed on the primary Pixel 9 with final result **PASS** on 2026-09-09.
- [x] In-place upgrade/state retention passed.
- [x] Navigation/lifecycle and unsaved-selection recreation passed.
- [x] Catalog/search/refresh/offline behavior passed.
- [x] Output-registry and hardware-validation-pending wording passed.
- [x] Fidelity/adaptation-reason presentation passed.
- [x] UAPP/ToneBoosters export/import passed.
- [x] Additional file-output testing passed.
- [x] SAF ownership/recovery invariants exercised by the checklist passed.
- [x] Non-destructive Black Pearl connection/lifecycle/Cancel smoke passed.
- [x] Update/What's New presentation passed.
- [x] Final regression sweep passed.

The Android build string and pre-upgrade app version were not captured during the pass and remain explicitly documented as unknown rather than inferred after the fact.

## Hardware qualification state at v0.5.0 publication

- [x] **TRN Black Pearl**: qualified for the v0.5 path. The destructive/fidelity/persistence regression passed, including Exact Flash, Optimized complete-response Flash, power-cycle persistence, Reset-to-flat persistence, and the explicit out-of-validated-range caution path.
- [ ] **FiiO JA11**: **Hardware validation pending**. Software implementation shipped, but physical qualification remains deferred until hardware is available.
- [ ] **JCALLY JM12 (stock firmware)**: **Hardware validation pending**. Software implementation shipped; power-cycle persistence remains unclaimed until physically established.

JA11/JM12 pending status does not retroactively change the v0.5.0 publication result. Their release/in-app wording must continue to state the pending status accurately until future exact-candidate hardware checklists pass.

## Merge and exact-main validation

- [x] Final PR #14 head `a94aedfd8533e94496908bec3460cf9e5178760f` passed Android CI #1046, CodeQL #927, Catalog currentness #982, and Priority community coverage #470.
- [x] PR #14 final diff was reviewed and the PR was intentionally moved out of draft before merge.
- [x] PR #14 merged to `main` without bypassing validation; merge commit `58d761b5d9677b5605613c6e61cfc06f6ea831d9`.
- [x] Merged main passed Android CI #1047 and CodeQL #928.
- [x] Phase-semantics documentation corrections after that merge changed documentation only.
- [x] Final publication source `ff2fa351d5f38f9dcf37a77859f1e988bbdb76a8` passed Android CI #1049, CodeQL #930, and Automatic Dependency Submission #1174 before publication.
- [x] No `v0.5.0` tag or public release existed before the controlled publication run.

## Controlled public publication

- [x] **Signed GitHub Release #5** was started from `main` with `mode=publish`, `tag=v0.5.0`, and `confirm_publish=PUBLISH`.
- [x] Run ID `34341588059` completed successfully.
- [x] `build-signed-apk` completed successfully, including exact-source checkout, release input validation, signing-secret validation, release gate/build, alignment, signing, and APK verification.
- [x] `publish-release` completed successfully, including publication confirmation, duplicate-tag protection, GitHub Release creation, and immutable version-tag creation.
- [x] Public tag `v0.5.0` points to exact source `ff2fa351d5f38f9dcf37a77859f1e988bbdb76a8`.
- [x] Public release contains `EQ-Library-v0.5.0.apk`.
- [x] Public release contains `EQ-Library-v0.5.0.apk.sha256`.
- [x] Public release contains `apksigner-verification.txt`.
- [x] Public APK SHA-256 is `58e6ac5c62f9af1caf354f97cf2e7d9e2bcddea9937fac3c35532c279cd429eb`.
- [x] Release signer matches the permanently pinned Android release certificate.
- [x] GitHub latest-release metadata exposes **v0.5.0**, allowing installed clients to discover the update.

The beta candidate remains a qualification artifact only. The public v0.5.0 APK was rebuilt and signed from the exact finalized publication source through the controlled workflow.

## Previous public releases

- **v0.4.0**: focused TRN Black Pearl Reset EQ to flat release; published 2026-09-06.
- **v0.3.0**: source-agnostic EQ Library foundation and TRN Black Pearl Direct Flash; published 2026-08-31.
- **v0.2.0**: EQ Library rebrand and device-targeted export foundation; published 2026-08-28.
- **v0.1.0**: first signed public Android release; published 2026-08-16.

Version-specific details belong in `CHANGELOG.md`, `docs/releases/`, and the applicable hands-on/protocol records rather than being copied into the current release gate.

## Explicitly deferred

The following are not required for the current GitHub development-release path:

- Google Play Console setup;
- Play App Signing;
- Play Store listing assets/forms;
- Play testing-track requirements;
- Play-specific update routing.

Those items will be handled separately when Google Play work is intentionally started.

## `v0.7.0` publication record: 2026-09-28

- [x] Public release [v0.7.0](https://github.com/weekssa/OPRA-EQ-for-UAPP/releases/tag/v0.7.0) is latest, published, and non-prerelease.
- [x] Tag `v0.7.0` points to `4f325d673159b40515086fe5143df12b29ddb076`.
- [x] Trusted main-only workflow `36381764266` passed; immutable artifact `10952494777` has ZIP SHA-256 `08cffddad84f4c5648a4ec2e884784188689a24041f020cc9925e7af8040bccc`.
- [x] Public `EQ-Library-v0.7.0.apk` is present with SHA-256 `27dada499bcbf9be9bd21d1349164858c93a5d2b83f78fd61134de13b4eb4025`.
- [x] Public `EQ-Library-v0.7.0.apk.sha256` is present; the GitHub API reports both assets as uploaded.
- [x] Release notes use sentence-case headings, direct language, parallel lists, descriptive links, and explicit accessibility and hardware boundaries following the Google developer documentation style guidance.
- [x] Public hardware wording remains evidence-bounded: Black Pearl is limited to the exact AFUL Explorer pass, and JA11 broader power-cycle qualification is not claimed.
