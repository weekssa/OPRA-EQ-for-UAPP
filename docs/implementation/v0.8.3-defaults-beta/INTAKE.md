# v0.8.3 Beta v1 — planning decisions and remaining gates

Date: 2026-10-14. Status: **PRE-EXECUTION PLANNING. No new task, branch, worktree, build or phone session has been started.**

## Owner direction

Treat the new explicit-default design as the v0.8.3 release track, beginning with a separate private Beta v1. Use a new Luna High implementation task and an isolated clean worktree, rather than continuing the old automatic native-Save implementation. This record captures the decisions; the consolidated executable handoff is [V0_8_3_LUNA_HIGH_EXECUTION_HANDOFF.md](V0_8_3_LUNA_HIGH_EXECUTION_HANDOFF.md). It takes effect when the owner starts that new task with the handoff prompt.

The owner selected **wireless ADB for both installation and automated testing**. The earlier reference to USB installation was clarified: no physical cable install is required. The owner selected **leave final DAC test settings and report them**, rather than restoring original volume/program at session closeout. **Leave the exact beta app installed with its data on PASS or FAIL**, for owner manual testing. Do not uninstall, clear data, silently replace it with another build, or restore a previous APK during cleanup.

Mic restoration remains part of every individual Set as Default transaction. Leaving final volume/program settings does not permit leaving an unintended mic or UAC state, or reporting uncertain restoration as success. If a transaction fails, stop further mutations and report actual/unknown state; do not issue cleanup writes on assumption.

The current chat remains planning-only. Future phone automation belongs to the new implementation task after software/artifact gates and owner readiness. This later planned test scope supersedes the old research-only/no-phone direction for that future authorized session; it does not start a session now.

The owner requires a **release-ready beta candidate**: complete intended v0.8.3 functionality, approved production design language, error/recovery states, release polish and regression coverage before manual acceptance. Do not defer product work until after beta approval. The owner has explicitly pre-authorized the future Luna task to merge/push to main and complete official signing and automated v0.8.3 publication once the owner confirms that the identified beta passes and directs publication. That confirmation activates the existing authorization; do not request the same merge/sign/publish permission again.

## Proposed beta identity

| Field | Proposed value / rule |
| --- | --- |
| Launcher name | **EQ Library 0.8.3 Beta v1** |
| Version name | `0.8.3-beta.1` |
| Application ID | `com.weekssa.opraeqforuapp.v083beta` |
| Namespace / Kotlin packages | Keep `com.weekssa.opraeqforuapp`; no package-wide source rename |
| Beta version code | Start at `14`, then increase for later installs under this private application ID |
| Signer | Stable private test signer across beta revisions; record certificate fingerprint truthfully, never call it the official release signer |
| Candidate identity | Embed full frozen source SHA; record APK SHA-256, signer, version/code, build command and test-APK identity where applicable |
| Public stable target | `0.8.3`, original application ID, permanent official signer, version code `14` if still available (> published v0.8.2 code `13`) |

Confirm name availability and metadata during implementation. A separate application ID permits coexistence with stable and the older `.ja11beta`; display name or ADB transport alone does not prevent signature conflicts. All APKs must be signed. Do not uninstall stable or the old beta to resolve a package/signer conflict. The baseline Manifest has no custom provider authority to collide; new authorities/actions must use application ID if introduced.

Private beta data is independent of stable: Room/preferences/catalog live in app-private storage. Do not copy stable databases or imply automatic migration. DEVICE-control testing does not require the owner's stable My EQs library. If manual tests need local EQs, use supported export/import or a separately agreed test fixture while preserving owner data.

The updater supports prerelease SemVer and currently links GitHub stable releases. A stable `0.8.3` link would install/update the original package, not upgrade `.v083beta`. Recommended private-beta policy: disable stable-release update checks/prompts in this variant only and deliver beta updates explicitly. Preserve production updater behavior. Finalize this routine build distinction in the execution handoff.

## Base and bounded product scope

Proposed base: clean main `575dab59fe774d3683a3714ca490a334f1b20764`, verified to contain the same Android app/build/workflow code as released v0.8.2 `9983edf0b7fe437aeeec9ac3dcb3864705afa16a`; later changes are docs/catalog. Recheck at execution intake. Preserve the old beta branch/worktree, dirty planning documents, draft PR #96, old candidate, J034 failure and private recovery evidence.

Carry a concise new implementation plan into the new task. Do not cherry-pick the old feature commit or execute its historical prompt. Proposed branch name: `codex/v0.8.3-defaults-beta`; it has not been created.

Design agreed in discussion, with exact implementation details still to be consolidated:

- Volume and EQ dialogs offer Cancel, runtime-only Apply and explicit Set as Default. EQ row selection stages locally; Cancel sends nothing.
- Apply writes/verifies the requested runtime setting only. It causes no native Save or mic/UAC toggle.
- Set as Default works even for a currently active value, opens a confirmation showing the resulting volume/program pair, and performs no hardware action until confirmation.
- If no independently supported settings commit is isolated, use the proposed mic round trip: target write only if needed, opposite original mic state, verified replacement, restore original mic state, verified final state. No `0x19`, UAC, band/gain, EQ Apply or extra-reset writes belong to that default transaction.
- Continue independent settings-only-commit research off-phone; do not expand into speculative firmware writes or allow unbounded research to silently change the selected fallback contract.
- Preserve the stable v0.8.2 connection/session rules, shared gate and ViewModel lifetime/convergence fixes. Preserve qualified custom-EQ Flash/editor/Reset and other DAC behavior.
- Keep the private beta and eventual stable behavior equivalent for the accepted feature. Differences should concern identity, signing, provenance and beta-update policy, not accidentally leave the stable feature disabled behind an old beta-only auto-Save flag.

Detailed design/transaction/failure references are in `ja11-settings-persistence-beta/SAVE_COMPARISON_RESEARCH_PLAN.md`. Those are research/proposal inputs, not an invitation to copy old native-Save code. Record the new mechanism separately in reports with zero native Save requests and both mic phases.

## Automated software gates before requesting the phone

Complete focused regressions, full applicable JVM tests, lint, debug/release/private-beta builds, R8 verification, relevant disposable-emulator UI/install/coexistence checks, and independent source/scope review on a coherent candidate. Test both actions, changed/unchanged values, all programs, volume boundaries, both original mic states, UAC preservation, permission/reconnect failures, stale/ambiguous sessions, companion-state changes, rapid taps and navigation. No write replay or automatic mic fallback after a failed native Save.

The inherited normal Android workflow is malformed; retain its actual failure and use the established exact-head fallback plus private-variant coverage. Do not repair CI as hidden feature scope or call the normal workflow green. Freeze exact source and private app/test APK bytes before phone work. New code/artifacts do not inherit the earlier beta's CI or hardware pass.

## Planned wireless phone session

After all off-phone gates and provenance pass, ask the owner when the phone is ready, unlocked, and wireless debugging is enabled. Request only missing pairing/address details. Do not probe remembered endpoints. Use the explicit confirmed wireless serial for every install/test/log command; ensure one JA11 and one authoritative USB app owner. Stable and older beta remain installed but must not compete for the DAC.

Install the separately identified Beta v1 over wireless ADB; verify installed bytes/package/version/source/signer before the first mutation. No stable uninstall, data wipe, or package replacement. Collect private app/platform logs and the candidate-bound operation reports automatically; do not mislabel app-recorded command counts as a bus capture. Automate UI and permission handling where Android permits; ask the owner only for an unavoidable unlock/pairing/physical action.

Capture actual starting DEVICE and applicable native User 1 EQ state privately. The older original User 1 restoration remains unverified; do not assume a current flat view is the original EQ or invent a restoration pass. Establish a safe coherent baseline and preserve recovery data before mutation. Any necessary recovery/setup outside the agreed tests must have a concrete supported plan; do not replay J034 or rewrite the custom bank as a shortcut.

Batch the live tests around the product decisions: ordinary Apply without persistence, explicit Set as Default with the mic mechanism, current-value default saving, requested/companion verification, unchanged UAC/firmware and original mic restoration, and preservation of established non-flat User 1 evidence. Exhaust program/range/error matrices off-phone; do not manufacture USB failures or maximum-volume live tests to duplicate software coverage.

True unplug/replug persistence cannot be manufactured by merely restarting the app or simulating USB detach. If needed for the new candidate's acceptance, pause after capturing the pre-cycle state and give exact instructions to unplug only the JA11, reconnect it, and confirm completion. Do not represent wireless ADB automation as physical power removal. Use existing accepted evidence to avoid redundant physical loops; do not add repeated tests merely because the evidence is distributed.

Stop at the first genuine failed/uncertain hardware transaction. Export evidence before any further mutation; do not blindly restore, replay or install a patched candidate. Diagnostic read-only reacquisition remains bounded by established rules. Leave the exact tested beta installed on either outcome and stop the test runner so the owner can use it.

## End-of-session deliverable

Leave final test settings, not original-volume/program cleanup. Report:

- exact installed build, package/name/version/source/APK checksum and signer;
- PASS/FAIL/INCONCLUSIVE for each product decision and evidence limits;
- actual current volume, program, mic, UAC/PID, connection state and applicable User 1 status;
- the last confirmed default pair, separately from runtime settings if they differ; mark startup persistence unconfirmed when no true power-cycle evidence exists;
- whether original mic restoration is verified, and any uncertain or unexpected state;
- private log/report paths and concise owner manual-test guidance.

Retain the beta and its data on PASS or FAIL. No uninstall, clear-data, rollback APK installation or automatic original-settings restore. Intentional retained test volume/program is not a restoration failure; unintended/unknown mic or EQ state still is a defect to report. Keep private owner screenshots/video, raw coefficients, identifiers and logs out of GitHub.

## Confirmed acceptance and automated stable v0.8.3 rollout

Acceptance normally requires applicable automated gates plus the owner's manual signoff. The owner reserves the right to replace a specific faulty automated test with their own testing. Diagnose the failure first and fix supported test/harness defects where feasible. For an owner override, retain the original failed result, identify the exact gate and candidate, record the explanation, manual evidence, limits and explicit owner decision, and mark it as an approved exception rather than an automated pass. An override is not a blanket waiver of other gates or permission to change hardware retry/readback rules. Luna must not infer an override from silence or from a general statement that manual testing is possible.

Required sequence: finish the complete product and release preparation → freeze and test the private beta → leave that exact beta installed for owner testing → resolve defects in separately identified beta revisions when needed → owner confirms the accepted candidate passes and directs v0.8.3 publication → autonomously integrate/merge/push to main, run exact-main gates, generate the official stable candidate, validate clean install/upgrade and provenance, promote the exact signed candidate, verify public assets/update metadata, and finish documentation closeout. Do not stop at an APK, PR or draft release once publication authority is activated and safe work remains. Pause only for an actual unresolved failure, material change invalidating acceptance, unavailable required access, or unavoidable owner action; do not introduce a redundant approval checkpoint.

Build beta and stable from the same accepted feature implementation. Freeze release versioning, source, UX, protocol behavior, release notes and release configuration before owner acceptance wherever possible. Limit planned beta/stable differences to package/label, signer, version suffix, embedded provenance and beta-specific update policy. Verify release/minified production behavior before acceptance. Record the reviewed source relationship if merging changes the commit SHA; do not silently include new product behavior, dependency changes or unrelated changes after acceptance. Material behavior changes require renewed candidate qualification and owner acceptance.

The private beta and official stable APK cannot be byte-identical because they have distinct application IDs and signing identities. Do not rename or re-sign the private beta into the public release. Stable requires the original application ID and official signer, version `0.8.3` / increasing production code, release notes, capability/evidence updates, exact candidate provenance, applicable gates or recorded specific owner exceptions, and original-package clean-install plus v0.8.2 in-place-upgrade validation. The existing official workflow is main-only and accepts strict stable versions. Build/sign the official candidate once through that maintained process; publication promotes those exact verified bytes without rebuilding or re-signing. A separate beta install does not prove the stable upgrade path. Do not repeat physical tests merely because package/signing identity differs; decide any necessary additional qualification from actual source/build differences and evidence.

The known malformed normal CI result remains failed. Preserve established exact-head fallback coverage, and document any owner exception explicitly. Do not bypass workflow validation or branch protection implicitly; if a required release path cannot honor the recorded exception, report the concrete blocker and propose a supported remedy.

This is conditional authority for the future implementation/release mission, not an instruction to execute in this planning chat. Keep the beta private until the owner's explicit pass-and-publish instruction. No task creation, public prerelease, stable tag, merge, official signing or publication is started by this intake record.
