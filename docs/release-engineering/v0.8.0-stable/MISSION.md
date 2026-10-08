# v0.8.0 stable promotion mission

## Goal

Promote the exact qualified v0.8.0-beta production source to the official stable v0.8.0 release. Preserve the beta release, its artifact provenance, and all physical qualification evidence. Do not change production/runtime or hardware behavior without a demonstrated defect.

## Source and version contract

- Initial live main: `0f4236b64c6642b4cd7c1a0ffe2b0d9778330f01`, tree `e31fe181c012a5cd48d0573efe7e8a3d88e383a8`.
- Qualified beta source: `4190c6ca51694ea0a80583a83fd3cb09b5088a7d`.
- Both revisions have app source tree `857d02a53d0df44fb0bd46e5ddad3b319dc48dab`.
- Stable target: package `com.weekssa.opraeqforuapp`, versionName `0.8.0`, versionCode `11`.
- Beta baseline: immutable `v0.8.0-beta`, code `10`, APK SHA-256 `b352c4d10a91f9c378a1a18012054a803a23807d4c454fb9b2dde613b7729782`.
- Previous stable baseline: `v0.7.2`, code `9`, APK SHA-256 `efdd63ddb305d0624f805cc53e4ce27aae7d1ddeb169e8f965302d0f290ba64a`.
- Both existing artifacts use pinned signer SHA-256 `65c1c1256dae3c49e3548f334c91f0ba991969e9be9e0b223ba4e253d2114747`.

## Ordered release gates

1. Verify live main, exact beta ancestry, release state, app-source identity, and the post-beta delta.
2. Prepare stable metadata, release notes, readable in-app What's New coverage, and truthful current documentation.
3. Pass exact-head PR checks and one independent pre-merge review.
4. Merge the exact reviewed tree and verify the merged main SHA/tree.
5. Create an official main-only signed candidate and verify source, package, version, checksum, signer, manifest, alignment, and provenance.
6. On the exact signed candidate, pass signed v0.7.2-to-stable persisted-state upgrade, signed beta-to-stable persisted-state upgrade, and stable clean-install/core smoke on API 35.
7. Complete one independent final artifact review.
8. Publish stable `v0.8.0` only after gates 1–7; verify tag, source, public APK and sidecar bytes, signer, provenance, and `/releases/latest`.
9. Update current-version documentation, preserve historical beta/v0.7.2 evidence, close only release tracking created for this mission, create the final review bundle, and retire mission automation if any was created.

No Pixel, DAC, ADB/USB, TalkBack, headphones, JA11, or EW300 work is authorized or needed for this stable promotion. Existing beta physical evidence remains applicable because the production app source tree is unchanged.

## Dependency caveat

One successful `releaseRuntimeClasspath` dependency graph refresh was completed for this mission. During the 2026-10-08 branch push, GitHub reported 56 repository vulnerabilities. A direct read of the repository's public Dependabot page returned 404 on 2026-10-08, and the earlier alert endpoint request was rejected, so item-level current alerts and a current alert-to-runtime mapping remain unavailable. A beta-era report traced alerts through build/test tooling, but it cannot substitute for a current mapping. Do not claim a clean scan or that current runtime alert exposure was ruled out.


## Mission completion — 2026-10-08

**COMPLETE.** Exact main `54823e1a464f8b716a32c4f4808037fc0cdd99bd` was signed, independently verified, passed the signed v0.7.2 upgrade, beta-to-stable upgrade and clean-install/core smoke, then was published as immutable stable v0.8.0 and verified as GitHub latest. Public assets match the verified candidate; beta remains an immutable prerelease. Current checklist and final artifact evidence are in `../../PUBLIC_RELEASE_CHECKLIST.md` and `ARTIFACTS.md`. The remaining repository documentation PR and review-bundle/automation cleanup are closeout records only; they do not alter the published app tree or artifact.
