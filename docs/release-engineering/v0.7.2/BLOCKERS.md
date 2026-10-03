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

Resolution/current state: exact candidate source `1c36349ca3edb69061a34b44d385670380f60512` passed a fresh wipe/install smoke on API 26 Google APIs ARM64 with the 48 MiB heap-growth limit. The manufacturer, model, and profile list rendered; `MainActivity` remained resumed and PID 4267 remained alive after 60 seconds; AndroidRuntime had no app error. Dalvik used 47,111 KiB of 49,152 KiB, leaving 2,041 KiB free. A first attempt on this commit logged an app OOM while UIAutomation dumps overlapped and caused a system-process `UiAutomationService already registered` failure. That contaminated run is preserved separately; the clean serialized rerun passed. The temporary smoke signer is not official candidate evidence. The AVD is ARM64 while CI uses x86_64. No API 26 failure is reproduced on the clean run; exact PR CI x86_64 remains pending.

## Historical checkpoint: initial detached worktree

Problem: supplied isolated worktree initially pointed to detached commit 6f841ef27cbbb0ad1a2f1126138316be5ad7ecb2.

Recovery: verify clean status, fetch tags, confirm v0.7.1 provenance, then create the requested release branch directly from the tag.

Resolution: branch codex/v0.7.2-stabilization now starts at exact v0.7.1 commit c48f6a5daa08a5e03475b2e415fe80b41d3357db. No owner checkout changes were made.

## Active exact-head review finding: EW300 restoration checkpoint replay

Problem: if a baseline-restoration register write failed and persisting the follow-up `UNCERTAIN` state also failed, the durable record could remain `TEMPORARY_COMMITTED`. A later call after process restart could send the restoration write again.

Root cause: the first durable transition after `TEMPORARY_COMMITTED` occurred after the hardware restoration write. The failure of that later checkpoint therefore left the previous retryable state on disk.

Recovery: persist `RESTORATION_ATTEMPTED` before the first baseline-restoration write and treat it as terminal on subsequent calls. If that pre-write checkpoint fails, no baseline write is sent. Added tests for both pre-write checkpoint failure and failed `UNCERTAIN` persistence after an ambiguous restoration write, followed by qualifier recreation.

Resolution/current state: the local working tree passes 729 JVM tests, lint, debug/release assembly, and R8 verification; the EW300 persistence class passes 15/15. No physical DAC was accessed. The exact PR head `af3f4c596e99e0cf8dce408b982c641c6898de6a` predates the fix and had all checks green, but its independent review returned FAIL on this P2. The fix must be committed and pushed, then pass fresh exact-head CI and independent review before merge.
