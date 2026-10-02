# v0.7.2 blockers and recovery history

## Resolved setup issue: GitHub CLI missing at intake

Problem: gh is not on PATH, so authenticated PR, Actions, and release state cannot yet be queried through the standard CLI.

Root cause: workstation toolchain is incomplete in this shell.

Recovery: confirm trusted package manager; install GitHub CLI using its official supported route; verify gh version and authentication without printing credentials. Public read-only API queries may supplement, but not replace, authenticated mutation tools.

Resolution: official GitHub CLI was installed outside the repository and authenticated remote state was refreshed without printing credentials.

Current state: resolved.

## Resolved baseline defect: API 26 startup OOM

Problem: the pristine v0.7.1 minified APK crashed during first launch on a freshly wiped API 26 emulator.

Root cause: `CanonicalCatalogRepository.loadSnapshot()` converted the complete 19,375,344-byte catalog file to a `String` before JSON decoding. On the emulator's 48 MiB heap growth limit this caused `OutOfMemoryError` while `StringWriter` was growing by 33,562,632 bytes.

Recovery: captured the AndroidRuntime stack, confirmed the API level and heap limit, inspected the R8 mapping/source path and catalog read implementation, added a large persisted-catalog regression test, and changed decoding to consume a file `InputStream` directly.

Resolution/current state: the focused repository tests passed; a fresh minified release build cold-installed and launched successfully on the wiped API 26 AVD. `MainActivity` remained resumed, the app process was alive, and the AndroidRuntime error log was empty. Run full candidate validation again after all remaining changes.

## Historical checkpoint: initial detached worktree

Problem: supplied isolated worktree initially pointed to detached commit 6f841ef27cbbb0ad1a2f1126138316be5ad7ecb2.

Recovery: verify clean status, fetch tags, confirm v0.7.1 provenance, then create the requested release branch directly from the tag.

Resolution: branch codex/v0.7.2-stabilization now starts at exact v0.7.1 commit c48f6a5daa08a5e03475b2e415fe80b41d3357db. No owner checkout changes were made.
