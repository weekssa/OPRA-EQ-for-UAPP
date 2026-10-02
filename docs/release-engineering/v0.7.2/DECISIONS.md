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

Tests validating safety: source and diff review must confirm no protocol/authorization/write path changed; local tests use pure code, mocks, or emulator only.

## D003: stream canonical catalog JSON from disk

Decision: replace whole-file `File.readText()` plus `Json.decodeFromString()` in `CanonicalCatalogRepository.loadSnapshot()` with `Json.decodeFromStream()` over a closed file input stream.

Evidence: a clean API 26 AVD (Android 8.0, heap growth limit 48 MiB) running the minified v0.7.1 APK crashed during first launch with `OutOfMemoryError`, attempting a 33,562,632-byte string-buffer growth while loading the canonical catalog. The stack included `CanonicalCatalogRepository$loadSnapshot$2`; the current catalog endpoint reports 19,375,344 bytes. The failing source expression reads the complete file into a string before JSON decoding.

Alternatives: increase `largeHeap`, reduce minSdk, skip API 26, or catch the allocation failure. Rejected because they mask or weaken the supported low-memory startup path instead of removing the duplicated whole-file string.

Reason chosen: streaming removes the confirmed full-file text allocation while preserving the repository state model, validation, refresh boundaries, and on-disk format.

Tests validating safety: `largeCachedCatalogCanBeLoadedFromDisk` exercises a >20 MiB persisted JSON file; the focused `CanonicalCatalogRepositoryTest` suite passes. A newly wiped API 26 AVD cold-installed and launched a minified local APK after the fix; MainActivity remained resumed, the process remained alive, and the AndroidRuntime error log was empty. Full candidate validation remains pending.

## D004: merge current main without rewriting the v0.7.1 base

Decision: merge current `origin/main` into `codex/v0.7.2-stabilization` with a normal merge commit.

Evidence: fetched main was `afed3dc90b5873218d5e333882528f8c5ddd54a2`. Its changes since v0.7.1 were currentness/catalog data and updated README, changelog, runbook, public release checklist, and signing docs. No production Kotlin changes were present. The merge completed cleanly at `1530d02f`; `c48f6a5daa08a5e03475b2e415fe80b41d3357db` remains an ancestor.

Reason: preserve the current v0.7.1 release/status documentation and live catalog while retaining the exact immutable release source in branch ancestry.

Tests validating safety: fetched branch/file inventory, clean `git merge --no-edit origin/main`, merge-parent inspection, and ancestor check.
