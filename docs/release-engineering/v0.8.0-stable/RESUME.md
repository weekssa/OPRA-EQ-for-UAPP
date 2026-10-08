# v0.8.0 stable promotion — resume

Updated: 2026-10-08T08:43:31Z

## Current state

- Worktree: `/Users/stephenweeks/.codex/worktrees/v080-stable-promotion/OPRA-EQ-for-UAPP`
- Follow-up branch: `codex/v0.8.0-stable-import-screen-fix`, based on exact current `main` `984477c773f9f388e93583a61f6459dbc8e27ae1` (tree `eede4100c1917019358fdb2c1afc7b1b656f2c61`). It corrects only the stable-release emulator helper and regression tests; production app source and version metadata are unchanged.
- PR #74 merged normally as `984477c773f9f388e93583a61f6459dbc8e27ae1`; its reviewed head was `c90e94a08bf1f3440051d5f518b08210921196f3`. The published beta's qualified source remains `4190c6ca51694ea0a80583a83fd3cb09b5088a7d`; both retain app source tree `857d02a53d0df44fb0bd46e5ddad3b319dc48dab`.
- Live GitHub `/releases/latest` is still stable `v0.7.2`; `v0.8.0-beta` remains an immutable prerelease. Lookup for stable tag/release `v0.8.0` returns 404. Do not move latest until all stable gates pass.
- Stable metadata remains package `com.weekssa.opraeqforuapp`, versionName `0.8.0`, versionCode `11` (beta code `10`). No beta qualification or physical gate is invalidated.

## Completed

- Stable release notes, changelog entry, and What's New formatting coverage are prepared. README/current download references remain on v0.7.2 until verified publication.
- PR #74's exact-head CI and one independent review passed; it merged to main as above. The stable signed candidate workflow run #20 / `37748076250` succeeded on that main. Artifact `11536942838` (ZIP SHA-256 `6347f3a141d829df9d0848cd729099f36116799a3f0c09e1145880f799d827d3`) was independently inspected: package/version/code `com.weekssa.opraeqforuapp` / `0.8.0` / `11`, APK SHA-256 `ce9ee7f1cef006b9c02e9236144504505547789fc5adad05bf8ec8e812d9f7ff`, pinned signer SHA-256 `65c1c1256dae3c49e3548f334c91f0ba991969e9be9e0b223ba4e253d2114747`, and provenance/alignment checks passed. This artifact is tied to main `984477c...` and must be refreshed if the follow-up is merged.
- Promotion run #8 / `37749785239` verified candidate provenance, installed and launched the v0.7.2 baseline successfully, then failed in API 35 job `113219908517` while opening/seeding persisted-state fixture data. Its diagnostics artifact `11537199862` (SHA-256 `7a9d962855e84c4dd054ce6b380ba8ebe234642388f3d2165c7af7e7b98f515b`) captures the v0.7.2 `Import personal EQ` form, including `Paste PEQ text`, Manufacturer, Headphone model, EQ name, and `Equalizer APO / AutoEq text`. The harness expected a different title and a nonexistent `Parametric EQ text` label. No fixture was saved; the v0.8.0 stable candidate was not installed, so no upgrade to stable was attempted and this run proves neither upgrade success nor app failure. Tag and publication jobs were skipped, and no v0.8.0 stable tag/release was created.
- Draft PR #75 contains the follow-up release-smoke correction and current failure status. It recognizes both legacy heading spellings only when the full form signature is visible, then types into the captured `Equalizer APO / AutoEq text` field. Focused release tests pass 56/56; `tools/promote_release_candidate.py check-contract` passes. The code/test review passed; its factual clarification is included so the one review can finish against the final diff. Require exact-head checks and final review on the amended PR head. See the local `.unlazy/v080-stable-promotion/GATES.md` for the live run IDs and checks.
- Refreshed dependency caveat remains: GitHub reported 56 repository vulnerabilities, while current item-level alerts/runtime mapping could not be obtained. Do not claim a clean scan or that current runtime alert exposure was ruled out.

## Next action

Monitor exact-head checks for draft PR #75 and finish the single independent review on the final diff. After all applicable checks/review pass, synchronize the PR body, mark it ready, merge under repository rules, and verify the new main SHA/tree. Because the previous signed artifact binds main `984477c...`, dispatch a fresh official main-only stable signing workflow for the new exact main and independently verify its artifact. Then run the maintained promotion workflow once on that exact artifact; it must pass the signed v0.7.2 upgrade, signed beta-to-stable upgrade, and stable clean-install/core smoke before it can create the v0.8.0 tag or publish. Inspect any failure's exact logs/artifacts before a bounded correction; do not retry unchanged.

Do not merge/publish before ordered gates pass. Do not use Pixel, ADB/USB, TalkBack, headphones, or DAC hardware; none is required because production app source remains unchanged. Preserve ignored `.unlazy/locks/` and `.unlazy/v080-stable-promotion/` evidence, and do not delete unrelated untracked files.
