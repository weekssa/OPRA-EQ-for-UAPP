# v0.7.2 governance remediation mission

## Scope

Close six post-release governance findings for the already-published v0.7.2 release. Keep the product source, signed APK, signer, version, and annotated tag target unchanged. The release source is `b8e90b9b53fc63ea00fefa499d7d4bd6ce4d55ea`; the audited public APK SHA-256 is `efdd63ddb305d0624f805cc53e4ce27aae7d1ddeb169e8f965302d0f290ba64a`.

## Required outcomes

1. Retire the stale local Unlazy release checkpoint without rewriting historical gate evidence.
2. Establish stable version-tag protection and enable immutable Releases when the available administration path permits it.
3. Correct the release-note claim about imported filter-gain rejection in maintained documentation and the public Release body.
4. Make promotion's final Release readback verify the exact expected name and body, with regression coverage.
5. Do not recreate historical skill invocation evidence; capture skill provenance for future work.
6. Do not recreate historical tool-install evidence; capture tool provenance for future work.

## Safety constraints

- No application behavior, DSP, USB/DAC protocol, device authorization, version, signing, asset, or tag-target changes.
- No rebuild, replacement, retag, or binary publication.
- Preserve historical evidence and report missing historical provenance honestly.
- Merge only after exact-head review and required CI pass; the owner authorized this merge in the task prompt.

## Initial identity

- Repository: `weekssa/OPRA-EQ-for-UAPP`
- Base: `origin/main` at `daef0560bc120aebfe9d8910a1b867a9f9390c15`
- Worktree: `/Users/stephenweeks/.codex/worktrees/5e58/OPRA-EQ-for-UAPP`
- Branch: `codex/v0.7.2-governance-remediation`
- Public Release ID: `402346895`
- Public APK digest: verified from a fresh download on 2026-10-03; matches the audited SHA-256 above.
