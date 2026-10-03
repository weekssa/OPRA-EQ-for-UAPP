# v0.7.2 blockers and recovery history

## Resolved setup issue: GitHub CLI missing at intake

Problem: gh is not on PATH, so authenticated PR, Actions, and release state cannot yet be queried through the standard CLI.

Root cause: workstation toolchain is incomplete in this shell.

Recovery: confirm trusted package manager; install GitHub CLI using its official supported route; verify gh version and authentication without printing credentials. Public read-only API queries may supplement, but not replace, authenticated mutation tools.

Resolution: official GitHub CLI was installed outside the repository and authenticated remote state was refreshed without printing credentials.

Current state: resolved.

## Resolved API 26 cold-start OOM chain

Problem: the pristine v0.7.1 minified APK crashed during first launch on a freshly wiped API 26 emulator. Streaming the catalog removed the first OOM, but a three-second follow-up smoke falsely passed and longer cold launches still failed during catalog overlay.

Root causes: `CanonicalCatalogRepository.loadSnapshot()` initially converted the complete 19,375,344-byte catalog file to a `String` before JSON decoding, which attempted a 33,562,632-byte `StringWriter` growth against the 48 MiB heap-growth limit. After streaming fixed that allocation, catalog rendering retained eager lookup indexes and full formatted acoustic signatures for thousands of profiles. A cold trace reached the signature builder during acoustic deduplication with only a few hundred bytes free. Earlier traces also caught memory pressure during product alias resolution and legacy profile normalization.

Recovery: retained direct stream decoding and its large persisted-catalog regression test; made `OpraCatalog` lookup indexes lazy; reduced alias-resolution duplication; made canonical band projection lazy; changed acoustic deduplication to compact 64-bit fingerprints with exact signature checks for collisions; replaced the hot numeric formatter with fixed-precision formatting that falls back to `String.format` around rounding ties and unusual values; and moved catalog adaptation/overlay to `Dispatchers.Default`. Added fixed-precision/negative-zero and formatter-equivalence tests. The API 26 smoke now waits for the manufacturer list and observes for 60 seconds after catalog readiness.

Resolution/current state: exact candidate source `1c36349ca3edb69061a34b44d385670380f60512` passed a fresh wipe/install smoke on API 26 Google APIs ARM64 with the 48 MiB heap-growth limit. The manufacturer, model, and profile list rendered; `MainActivity` remained resumed and PID 4267 remained alive after 60 seconds; AndroidRuntime had no app error. Dalvik used 47,111 KiB of 49,152 KiB, leaving 2,041 KiB free. A first attempt on this commit logged an app OOM while UIAutomation dumps overlapped and caused a system-process `UiAutomationService already registered` failure. That contaminated run is preserved separately; the clean serialized rerun passed. The temporary smoke signer is not official candidate evidence. The AVD is ARM64 while CI uses x86_64. Exact PR head `3ccf6728` also passed the API 26 x86_64 cold-install/catalog-readiness/survival lane; artifact `11261474879`.

## Historical checkpoint: initial detached worktree

Problem: supplied isolated worktree initially pointed to detached commit 6f841ef27cbbb0ad1a2f1126138316be5ad7ecb2.

Recovery: verify clean status, fetch tags, confirm v0.7.1 provenance, then create the requested release branch directly from the tag.

Resolution: branch codex/v0.7.2-stabilization now starts at exact v0.7.1 commit c48f6a5daa08a5e03475b2e415fe80b41d3357db. No owner checkout changes were made.

## Resolved exact-head review finding: EW300 restoration checkpoint replay in post-cycle path

Problem: if a baseline-restoration register write failed and persisting the follow-up `UNCERTAIN` state also failed, the durable record could remain `TEMPORARY_COMMITTED`. A later call after process restart could send the restoration write again.

Root cause: the first durable transition after `TEMPORARY_COMMITTED` occurred after the hardware restoration write. The failure of that later checkpoint therefore left the previous retryable state on disk.

Recovery: persist `RESTORATION_ATTEMPTED` before the first baseline-restoration write in `verifyTemporaryAndRestore()` and treat it as terminal on subsequent calls. If that pre-write checkpoint fails, no baseline write is sent. Tests cover pre-write checkpoint failure and failed `UNCERTAIN` persistence after an ambiguous restoration write, followed by qualifier recreation.


Resolution/current state: source commit `40b4f5d8c7b255ebed7cc886158d6e6621a911c4` added the durable marker to this path. Independent review of PR head `13bf1f20` then found the same replay risk in a separate helper path; that follow-up is recorded below. Exact-head review on `e63fc4bf` is required before merge.

## Resolved exact-head review finding: EW300 restoration checkpoint replay in pre-commit helper

Problem: `restoreBeforeCommitOrFail()` also sent baseline-restoration writes after a temporary write rejection or transient temporary readback mismatch. It did so before persisting an attempt marker. If its first baseline write failed and saving `UNCERTAIN` failed, the durable state could remain `TEMPORARY_COMMITTED`; after detach and qualifier recreation, a later call could resend restoration.

Root cause: the first fix guarded `verifyTemporaryAndRestore()` but missed a second helper that writes the same baseline registers.

Recovery: commit `e63fc4bf5be629135b0fd56449bad4c1b0bfd1b4` persists `RESTORATION_ATTEMPTED` before the helper's first baseline write. Failure to save that marker sends no baseline write. The marker remains terminal if the later `UNCERTAIN` checkpoint fails. Added regressions for transient readback plus failed uncertain persistence across qualifier recreation, and for failure of the helper's pre-write checkpoint.

Resolution/current state: focused EW300 persistence tests pass 17/17. Fresh full local Gradle validation on `e63fc4bf` passes 731 JVM tests, lint with 0 errors, 111 warnings, and 2 hints, debug/release assembly, and R8 mapping verification. No physical DAC was accessed. All eight GitHub checks and independent review passed on exact PR head `e7f2fc937e9b265770296dbdc4cbb40a4e5e13c9`; that tree merged normally as `a60411be`, and post-merge local Gradle/R8 gates passed.

## Current build-tool dependency risk

Live main has 51 open transitive Maven Dependabot alerts: 3 critical, 20 high, 26 medium, and 2 low. The default-branch SBOM maps alerted libraries to Android build artifacts, Gradle plugins, emulator tooling, and test components. On the candidate, no alerted coordinates appear in `releaseRuntimeClasspath`, and a mapped minified DEX scan found zero flagged package-class matches. These alerts remain real build/test-toolchain exposure; they are not a claim of zero supply-chain risk. Broad dependency upgrades are outside this stabilization scope. Track critical/high build-tool remediation separately.

## Current release gate

PR #65 exact head `e7f2fc937e9b265770296dbdc4cbb40a4e5e13c9` passed all eight checks and independent review; UI instrumentation passed 25/25 and API 26 x86_64 cold-install/readiness/survival passed. It merged normally as `a60411bebfdbd1cea4218d3bde45013bb7ed26a9`; merged main's tree is identical to the reviewed head. Post-merge local Gradle validation passed 731 tests, lint, debug/release assembly, and R8 mapping. Beta run `37093821378` is testing evidence only and is not promotion-eligible. Official candidate run `37095180116` and artifact `11264251526` passed independent verification. Promotion run `37096259477` passed its verify/API 35 job, then failed in tag job `111127079868` because a SHA-256 prefix was normalized on only one side of a comparison; publication job was skipped. No v0.7.2 tag or release was created. No physical DAC writes were performed and no owner action is required.

## Resolved release workflow selection error

Problem: the first signed workflow dispatched after merge was Signed EQ Library Beta Candidate run `37093821378`. Its signed APK passed the beta workflow's emulator smoke and was published to the temporary `mobile-test-apk` branch, but it could not be used by the release promoter.

Root cause: the main-only `Promote Signed Release Candidate` contract accepts only successful runs of `.github/workflows/github-release.yml`; it rejects `.github/workflows/signed-beta.yml` by workflow ID before candidate validation.

Recovery: keep the beta run and mobile-test publication as testing evidence, do not pass that artifact to promotion, and dispatch the correct `Signed Release Candidate` workflow from exact merged main. Run `37095180116` completed for tag `v0.7.2` at source `a60411bebfdbd1cea4218d3bde45013bb7ed26a9`; artifact `11264251526` passed independent verification.

Resolution/current state: the wrong-workflow artifact remained testing evidence only. The official candidate passed independent verification and its API 35 install/upgrade gate passed during promotion run `37096259477`. No immutable tag or release was created because a later digest-comparison defect stopped the tag job.

## Active release-promotion code defect: artifact digest normalization

Problem: promotion run `37096259477` completed the candidate verification and API 35 baseline install, in-place upgrade, and cold launch, then failed before tag creation. The log reports `candidate tag input digest differs from the immutable Actions artifact digest`; the public publish job was skipped.

Root cause: `validate_github_candidate()` returns GitHub's artifact digest as `sha256:<64 hex>`. `command_tag()` compares that prefixed value directly with `normalize_sha256(args.artifact_digest)`, which has no prefix. `command_publish()` also compares raw forms and should use the same normalized digest contract.

Recovery: a shared digest equality helper now normalizes and validates both sides in the tag and publish commands. Tests cover prefixed/unprefixed equality, malformed and mismatched values, and the direct tag-command path. The focused publisher suite passes 39/39, full Python tools pass 238/238 with bundled Python 3.12, and the promotion contract passes. Complete independent review and normal PR/CI merge, post-merge verify, build a new exact-source signed candidate, and rerun promotion with the new candidate tuple.

Current state: main remains `a60411bebfdbd1cea4218d3bde45013bb7ed26a9`; `v0.7.2` tag and release are absent. The local correction is not yet on main. The existing candidate is tied to the current main only and must not be reused after the code correction merges.
