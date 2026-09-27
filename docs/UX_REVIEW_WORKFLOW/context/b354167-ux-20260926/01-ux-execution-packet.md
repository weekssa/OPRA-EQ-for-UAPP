# Approved Flash feedback addendum

Baseline source: `e2afcca38b1b31c333b67af9499bb49d49f0e428`
Baseline signed APK: `EQ-Library-v0.7.0-beta-e2afcca.apk` (SHA-256 `f6ddf8e34a6bb50b68597e01e4c3b086066fb071d60508aefa71286451e937ec`)
OWNER_APPROVAL_REQUIRED = true — **approved** in `02-owner-approval.md` and current owner message.

## One correction

Unify user-visible Flash feedback across Black Pearl, FiiO JA11, and SIMGOT EW300. Keep each existing pre-Flash confirmation and its safety content. Replace the fleeting Black Pearl snackbar and duplicate/lengthy JA11/EW300 success prose with one compact, non-modal status near the top of the active destination, backed by the existing authoritative operation results. Keep the same status available when navigating between the Flash source and My DAC.

State wording: `Flashing to <DAC>…`; `Verifying the DAC…`; `Reconnecting and verifying…` only after an observed replacement-session/reconnect state; `Flash complete · EQ saved and verified` only after device-specific final verification; a persistent plain-language uncertain/failure state with safe Refresh guidance and no automatic retry. Device-specific persistence/readback qualifiers remain available as concise detail. No duplicate Flash success snackbar. Do not claim a disconnect, Save, or success from a timer or from requested local state.

Affected presentation surfaces: My EQs saved/general EQ and headphone detail Flash; EQ Library profile Flash; shared app shell Flash feedback; My DAC EQ operation status for Black Pearl, JA11, and EW300. Expected source files include `EqLibraryApp.kt`, `MyEqsHomeScreen.kt`, `ManagedHeadphoneDetailScreen.kt`, `BrowseOpraScreen.kt`, `FiioJa11MyDacContent.kt`, and `Ew300MyDacContent.kt`, plus a small shared presentation model/composable and focused tests if needed. No hardware/session/domain files are in scope.

Approved Black Pearl resolution: pass the existing typed `BlackPearlFlashResult` through UI presentation state without changing the Flash transaction. `Success` means all reports were sent; it does not establish final readback or verified persistence. Show a distinct sent/unverified state and retain any warning. Show typed transfer failures as failures. The existing `e2afcca` signed APK remains the comparison baseline.

## Standards and validation

Apply `android-skills:android-dev`, `android-skills:android-ux`, `android-skills:compose`, and `android-skills:android-testing`: a dialog is the decision gate, an in-content status covers a long operation, progress is indeterminate without a trustworthy percentage, and concise state changes use TalkBack polite live-region semantics. Test the presentation mapping for progress, verified success, reconnect observed/not observed, failure, stale/uncertain outcome, and no duplicate success message. Review large text and dark/light contrast, exact diff, and `git diff --check`; run only available focused Android checks. Mark unavailable checks `NOT RUN`.

## Exclusions

No navigation or information-architecture change; no new EQ action or hardware control; no protocol, codec, identity, session ownership, automatic reconnect rule, permission flow, Flash/Apply/Save/Reset sequencing, readback, persistence, canonical EQ, or capability change. Do not push, merge, sign, publish, install tooling, create a VM, or run hardware mutations in this implementation pass. The `e2afcca` APK remains the comparison baseline and does not validate changed source.
