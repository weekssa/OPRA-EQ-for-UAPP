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

Resolution/current state: the latest smoke on a freshly wiped API 26 Google APIs ARM64 AVD with a 48 MiB heap-growth limit cold-installed the local minified 0.7.2/code 9 APK. The manufacturer, model, and profile list rendered; `MainActivity` remained resumed and PID 4425 remained alive after a 60-second observation; the AndroidRuntime error log was empty. Dalvik used 47,094 KiB of 49,152 KiB at final inspection. A separate earlier smoke remained alive for more than 13 minutes, but its artifacts and memory snapshot are explicitly separated under `.unlazy/v0.7.2-autonomous-release/evidence/api26-final/` and are not the latest-run result. The latest smoke APK was signed with a temporary local-only key and is not official candidate evidence. The AVD is ARM64 while CI uses x86_64. No unresolved API 26 failure is currently reproduced; final committed-source and CI x86_64 runs remain pending.

## Historical checkpoint: initial detached worktree

Problem: supplied isolated worktree initially pointed to detached commit 6f841ef27cbbb0ad1a2f1126138316be5ad7ecb2.

Recovery: verify clean status, fetch tags, confirm v0.7.1 provenance, then create the requested release branch directly from the tag.

Resolution: branch codex/v0.7.2-stabilization now starts at exact v0.7.1 commit c48f6a5daa08a5e03475b2e415fe80b41d3357db. No owner checkout changes were made.
