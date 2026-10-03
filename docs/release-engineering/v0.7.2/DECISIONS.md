# v0.7.2 decisions

## D001: use the immutable v0.7.1 source

Decision: create the release branch from tag v0.7.1, not an arbitrary current worktree commit.

Evidence: fetched remote tag resolves to c48f6a5daa08a5e03475b2e415fe80b41d3357db; tagged app/build.gradle.kts declares versionName 0.7.1 and versionCode 8. The preferred stabilization branch was absent locally/remotely.

Alternatives: use the detached worktree's initial commit or origin/main without checking ancestry.

Reason: the user explicitly requires immutable v0.7.1 as the exact base. The stabilization branch was created directly from the tag. After checkpointing the recovery fix, it merged current `origin/main` at `afed3dc90b5873218d5e333882528f8c5ddd54a2`; the tag remains an ancestor and no history was rewritten.

Tests validating safety: git rev-parse refs/tags/v0.7.1^{commit}; ancestry check after the merge; tagged Gradle metadata inspection.

## D002: no physical hardware transaction

Decision: do not read, flash, save, reset, restore, or otherwise mutate a real DAC in this task.

Evidence: explicit user instruction; the target is release stabilization and DSP sampling verification. Existing device evidence remains bound to historical source/artifact tuples.

Reason: source-only DSP oracle and emulator/repository gates can test the requested concern without consuming or changing a physical DAC state.

Tests validating safety: source and diff review must confirm no protocol bytes, identity authorization, or USB command encoding changed. The existing EW300 qualification path now refuses its first write when durable recovery state cannot be saved. Local tests use pure code, mocks, or emulator only; no physical DAC is touched.

## D003: stream canonical catalog JSON from disk

Decision: replace whole-file `File.readText()` plus `Json.decodeFromString()` in `CanonicalCatalogRepository.loadSnapshot()` with `Json.decodeFromStream()` over a closed file input stream.

Evidence: a clean API 26 AVD (Android 8.0, heap growth limit 48 MiB) running the minified v0.7.1 APK crashed during first launch with `OutOfMemoryError`, attempting a 33,562,632-byte string-buffer growth while loading the canonical catalog. The stack included `CanonicalCatalogRepository$loadSnapshot$2`; the current catalog endpoint reports 19,375,344 bytes. The failing source expression reads the complete file into a string before JSON decoding.

Alternatives: increase `largeHeap`, reduce minSdk, skip API 26, or catch the allocation failure. Rejected because they mask or weaken the supported low-memory startup path instead of removing the duplicated whole-file string.

Reason chosen: streaming removes the confirmed full-file text allocation while preserving the repository state model, validation, refresh boundaries, and on-disk format.

Tests validating safety: `largeCachedCatalogCanBeLoadedFromDisk` exercises a >20 MiB persisted JSON file; the focused `CanonicalCatalogRepositoryTest` suite passes. A newly wiped API 26 AVD cold-installed and launched a minified local APK after the fix; MainActivity remained resumed, the process remained alive, and the AndroidRuntime error log was empty. At the time of this decision record, full candidate validation remained pending; it later passed as recorded in `TEST_MATRIX.md` and `ARTIFACTS.md`.

## D004: merge current main without rewriting the v0.7.1 base

Decision: merge current `origin/main` into `codex/v0.7.2-stabilization` with a normal merge commit.

Evidence: fetched main was `afed3dc90b5873218d5e333882528f8c5ddd54a2`. Its changes since v0.7.1 were currentness/catalog data and updated README, changelog, runbook, public release checklist, and signing docs. No production Kotlin changes were present. The merge completed cleanly at `1530d02f`; `c48f6a5daa08a5e03475b2e415fe80b41d3357db` remains an ancestor.

Reason: preserve the current v0.7.1 release/status documentation and live catalog while retaining the exact immutable release source in branch ancestry.

Tests validating safety: fetched branch/file inventory, clean `git merge --no-edit origin/main`, merge-parent inspection, and ancestor check.

## D005: use dense final response checks while retaining the coarse fit grid

Decision: retain the 96-point response grid for candidate search and UI graph rendering, but calculate generated optimizer/editor headroom with a 12,001-point logarithmic grid plus exact band centers and nearby samples. Require a dense source-to-final-target error check before returning every non-exact `Ready` optimizer result. Advance finite-hardware representation versions so derived plans are rebuilt under the new validation contract.

Evidence: an independent RBJ oracle measured a +12 dB/Q=10 Peak at 978.371245 Hz at 12 dB while the old 96-point graph returned 7.607188487 dB, a 4.392811513 dB miss. Two coincident +12 dB/Q=10 Peaks at 978 Hz produced 24 dB dense response versus a 15.316541726 dB coarse maximum, an 8.683458274 dB miss. A six-band JA11 fit passes the coarse checks but is rejected by the dense final check. Exact maximum-Q +12 dB and −12 dB, 20 Hz and 20 kHz boundaries, coincident boosts, cross-source shelves, and target quantization now have independent tests across EW300, Black Pearl, JA11, and JM12.

Alternatives: leave production unchanged because the concern was only a display approximation; or replace the fitter grid globally with thousands of points. Rejected because generated headroom and fit eligibility materially under-read extrema, while replacing the search grid would needlessly expand optimization cost. The dense grid is used only for safety/final verification.

Reason chosen: the 96-point grid demonstrably underestimates modeled peak response enough to leave +4 dB net response in the single-boost EW300 counterexample and +8.68 dB for coincident filters. The final dense check fails closed without changing device commands or source-authored preamp semantics.

Tests validating safety: independent dense-response tests; editor `useSafeGain()` regression; six-band fit dense rejection; all current shelf corpus Ready cases independently resampled; `./tools/codex-android :app:testDebugUnitTest` passes 724 tests; `./tools/codex-android :app:lintDebug` passes with 0 errors, 111 warnings, and 2 hints. No changed DSP source/test file has a lint diagnostic. No hardware was connected or mutated.

## D006: persist a terminal restoration-attempt marker before baseline writes

Decision: store `RESTORATION_ATTEMPTED` durably before sending the first baseline-restoration register write. Treat that state like `UNCERTAIN` on a later call.

Evidence: the independent review found that if a baseline write failed and the following checkpoint to `UNCERTAIN` also failed, disk could retain `TEMPORARY_COMMITTED`. A later process could then resend the restoration write. The new pre-write marker becomes the stored terminal state before this failure window begins.

Alternatives: rely on the post-failure `UNCERTAIN` write alone, clear the pending record before the write, or retry when `UNCERTAIN` cannot be saved. Rejected because any later failed write could leave an older replayable stage, clearing first loses recovery context, and retrying an ambiguous hardware mutation is unsafe.

Reason chosen: the durable attempted marker closes the replay window without changing protocol commands or requiring successful storage after the hardware operation. If its own write fails, the baseline write is not sent.

Tests validating safety: four simulated-store/transport regressions cover both baseline-restoration paths. They verify that a failed pre-write checkpoint sends no restoration write, a failed `UNCERTAIN` checkpoint after either restoration path remains terminal across qualifier recreation, and transient temporary readback does not permit replay. The focused persistence class passes 17/17 and the full JVM suite passes 731 tests. Physical hardware was not accessed. All eight checks and independent review passed on exact PR head `e7f2fc93`; that tree merged normally as `a60411be`, and the post-merge Gradle/R8 gates passed.

## D007: keep pre-existing Maven advisories as a separate build-tool remediation

Decision: do not turn v0.7.2 into a broad dependency-upgrade project, but record the live build/test dependency risk explicitly.

Evidence: the current main SBOM maps the 51 open transitive Maven alerts (3 critical, 20 high, 26 medium, 2 low) to Android build artifacts, Gradle plugins, emulator tooling, and test dependencies. The candidate's resolved app `releaseRuntimeClasspath` contains none of the alerted coordinates; the mapped minified release DEX scan returned zero flagged package-class matches. The candidate app dependency declarations are unchanged from main apart from version metadata.

Reason: the alerts do not establish exposure in the distributed APK runtime, while the scope expressly excludes an unrelated broad dependency refresh. They do establish real build-toolchain risk and must remain visible for a separately tracked critical/high remediation.

Tests validating safety: fresh GitHub alert and SBOM reads, candidate Gradle dependency report, and mapped minified DEX inspection. This finding does not assert a risk-free build environment.

## D008: label the signed testing candidate with the workflow's existing JA11 default

Decision: dispatch the main-only signed candidate with `candidate_target=ja11`.

Evidence: the v0.7.2 task does not request a hardware-target-specific candidate and forbids physical DAC writes. `signed-beta.yml` requires a target in its candidate manifest and declares `ja11` as the existing default. The label binds the test plan and outstanding validation wording in the manifest; it does not change app bytes or grant a new support claim.

Reason: use the workflow's established default while keeping all hardware validation explicitly pending and avoiding a new product choice.

Tests validating safety: candidate workflow source SHA is pinned to merged `main` and its exact artifact must pass independent identity, signer, R8, checksum, signature, alignment, manifest, and install/upgrade checks. No physical device interaction is authorized or planned.

## D009: use the release-candidate workflow for public release promotion

Decision: only promote artifacts produced by `.github/workflows/github-release.yml` (`Signed Release Candidate`). The similarly named `signed-beta.yml` workflow is for the temporary `mobile-test-apk` testing surface and is not eligible for public release promotion.

Evidence: the promotion verifier checks workflow ID, branch, exact source SHA, artifact identity, and immutable artifact digest. Its read-only verification correctly rejected beta run `37093821378` before artifact processing because that run came from the beta workflow. Official candidate run `37095180116` was dispatched with `tag=v0.7.2` from exact merged `main` SHA `a60411bebfdbd1cea4218d3bde45013bb7ed26a9`.

Reason: enforce the repository's distinct candidate and beta artifact contracts without substituting a test artifact for the public release candidate.

Tests validating safety: `tools/promote_release_candidate.py check-contract` passed; local verification of the wrong-workflow beta artifact failed closed with the expected workflow-ID rejection and made no tag or release mutation. The correct official candidate later passed independent verification. No hardware claim was added.

## D010: normalize both sides of the immutable artifact digest comparison

Decision: use one validated SHA-256 equality helper for both tag and publish command inputs, normalizing GitHub's optional `sha256:` prefix on each side.

Evidence: at the time, promotion run `37096259477` completed candidate verification and the API 35 v0.7.1-to-candidate install/upgrade/cold-launch gate. `create-candidate-tag` failed with `candidate tag input digest differs from the immutable Actions artifact digest`; workflow output carried `sha256:<hex>`, while `command_tag()` normalized only the argument. The publish job was skipped, and remote inspection then found no v0.7.2 tag or release. This historical failure was resolved as recorded below.

Reason: GitHub's Actions artifact API and the normalized local representation are both valid formats for the same immutable digest. Both comparison inputs must be normalized while mismatch and malformed-digest rejection remain strict.

Tests validating safety: 39 focused promotion tests passed, including equal digests with and without `sha256:`, malformed/mismatched rejection, and direct `command_tag()` coverage for the GitHub-prefixed digest. The full Python tool suite passed 238/238 under bundled Python 3.12, and the promotion contract passed. PR #66 passed all seven exact-head checks and independent code/documentation review, then merged normally at release source `b8e90b9b53fc63ea00fefa499d7d4bd6ce4d55ea`. Fresh candidate run `37099431204` and promotion run `37099991693` passed; the promotion created the annotated tag and published the public release.
