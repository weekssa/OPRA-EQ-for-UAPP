# v0.8.0 Beta — Final Physical Session Plan

**Plan revision:** 2.5
**Disposition:** INDEPENDENT REVIEW PASS for the already-attached/current-session path only. The physical session remains attached-resume-only. Continue only if the exact Black Pearl and current production session are already present; otherwise stop and release the Pixel. No new attach/connect flow is qualified. Revision 2.5 retains the verified local Git exclude for raw evidence and checks a prospective file before creating the session directory; it does not change the app/test candidate.
**Master directive:** `docs/implementation/v0.8.0-beta-master-directive.md`, version 1.1; introduction commit and body hash are recorded in the directive and resume manifest.
**Review basis:** frozen app/test candidate plus the exact-source read-path audit. The same independent reviewer returned PASS on 2026-10-06 for revision 2.5 at candidate HEAD `5c7d19cf2f77e448a97a1ee9d520ad4d408f120f`, tree `e11669f0d67f9fb3c3762ea814f16fb7c4bdab8d`. Review covers the already-attached/current-session path only. This revision adds a per-step state/action/USB-effect/UI-result/evidence/abort contract. No product or test change is proposed.

## Purpose and current boundary

This is the one short, read-only Black Pearl C05-C qualification. It checks exact device identity, the active production session, real EQ and DEVICE state reads, attached navigation away and back, truthful current-session presentation, and matching final explicit reads. It does not create a reset fault and does not change DAC settings.

The previous C05-C attempt stopped safely because the old plan incorrectly expected the recovery-only **Read current EQ** action in an ordinary Connected state. That control is conditional on a saved interrupted-reset state. The attempt captured the initial verified-EQ presentation but did not refresh DEVICE, navigate, or make final explicit reads. It is **incomplete and not a C05-C pass**. No Reset, Apply, Flash, Save, DEVICE setter, disconnect, process kill, fault, or restorative write was performed. Preserve the stop report at `.unlazy/v080-beta/evidence/c05c-20261006T182006Z/C05-C-stop-report.txt` (SHA-256 `e32ff687c5574894652a98f1512bdaa3f2147c9e319770f81d656115e6558d6e`); the raw evidence is local and is not to be copied into a commit.

**Manual TalkBack: DEFERRED / NOT REQUIRED FOR v0.8.0 BETA.** Do not request headphones, enable or disable TalkBack, seek a spoken-warning confirmation, or use phone time for a TalkBack check. Existing automated accessibility evidence remains in scope; closed suites do not need to be repeated for this scope amendment. JA11 and EW300 are excluded from this session.

## Frozen candidate and live preflight snapshot

- Repository: `weekssa/OPRA-EQ-for-UAPP`
- App/test branch: `codex/v0.8.0-beta-ux`
- Candidate HEAD: `5c7d19cf2f77e448a97a1ee9d520ad4d408f120f`
- Candidate tree: `e11669f0d67f9fb3c3762ea814f16fb7c4bdab8d`
- App production-source commit: `f24b1582e22b19fc75757a61186361b71eab3974`
- `app/src/main` tree: `857d02a53d0df44fb0bd46e5ddad3b319dc48dab`
- `app/src/test` tree: `470979fb22a91c4514534badda6c2cc2032abfb6`
- `app/src/androidTest` tree: `da2b4f31e18025ebd00f0a84f274e0e8a8624800`
- `app` directory tree: `42977ce481bc76aa85b866610672e35e35d6cde6`
- Candidate APK: `app/build/outputs/apk/debug/app-debug.apk`; SHA-256 `f33818309d55571166d91e501065706ddbb8faf180833b03bb6eb948e6bffed1`
- Package/version: `com.weekssa.opraeqforuapp`, `0.8.0-beta`, version code `10`; debug signer SHA-256 `cbd57d13316c2ca9e59fb810135ea71567fd22132b0d7f4640e92dc4434f0e14`. This is the debug candidate, not the official signed beta.
- PR #70 live read at `2026-10-06T19:46Z`: open/draft, head exactly `5c7d19cf2f77e448a97a1ee9d520ad4d408f120f`, base `main` at `c702ef0149b4c647446639cfcb0ab25c09159db3`, `mergeable=true`, `mergeable_state=clean`. All 8 candidate exact-head check runs are complete and successful.
- Current `origin/main` at the same snapshot: `79d600529621257136adb4d9e1a97e8ae8521d7e` (`Refresh automated source currentness`). Current-main `audit` job `112450451826` and `submit-gradle` job `112437017475` succeeded. The base delta since PR base remains catalog/currentness data in four paths, with no candidate app/test path overlap. The latest currentness refresh adds one community catalog record. Keep **BASE-DRIFT-B**; do not rebase or rebuild for this physical session. Recheck and classify any further delta during pre-merge verification.
- Required API35 evidence is closed: jobs `112171539825`, `112177030919`, and `112181045077` each passed 64/64 consecutively; final JUnit artifact `11400405767`, SHA-256 `3eb0f7ed51db453d95945cd9920ae7ec588dca22102e3920b3d850164a71d757`, has 0 failures/errors/skips. Do not rerun without candidate invalidation.
- API26 populated Library is PASS/CLOSED on repository-default Pixel 2 / API26 / x86_64 at 48 MiB. Do not reopen for this session.
- Stable/latest remains v0.7.2. No merge or publication occurs during this procedure.

## Source-grounded production read paths

The ordinary Connected UI does not show the recovery-only **Read current EQ** button. Do not open Reset or recovery UI to find it.

- Connecting starts an EQ read from the Black Pearl connection collector (`app/src/main/java/com/weekssa/opraeqforuapp/data/hardware/HardwareEqRepository.kt:94-103`) and a DEVICE read from `EqLibraryViewModel.kt:438-446`. The EQ snapshot is accepted only while the same session remains current (`HardwareEqRepository.kt:343-371`); its production reader reads the filter bands and global gain (`HardwareEqSnapshotReaders.kt:11-35`).
- For explicit EQ reads, My DAC's normal **Edit EQ** button (`ui/screens/MyDacScreen.kt:568-574`) calls `EqLibraryViewModel.openBlackPearlEditor()` (`ui/EqLibraryViewModel.kt:624-676`), which first calls `hardwareRepository.readBlackPearlSnapshot()`. The editor states that edits are local only (`ui/screens/MyDacEqEditorScreen.kt:99-105,135-165`). **All bands** displays the observed values (`ui/screens/MyDacEqEditorScreen.kt:522-562`). **Close** clears only editor state (`ui/EqLibraryViewModel.kt:972-976`). Do not change any value or enter Safe gain, Review, Apply, Reset, or Save.
- For explicit DEVICE reads, the DEVICE tab's **Refresh** button (`ui/screens/DeviceOperationStatus.kt:131-185`, `ui/screens/BlackPearlDeviceBatchControls.kt:66-78`) calls `readBlackPearlQualificationControls()` and the read-only snapshot path (`ui/EqLibraryViewModel.kt:1247-1277`, `data/dac/DacControlRepository.kt:313-362`). It reads firmware, filter, gain mode, amplifier topology, microphone gain, both balance channels, playback gain, and optional USB audio mode. If USB audio mode is unavailable, record the displayed unavailable state; a nullable UAC mode is not by itself a read failure (`DacControlRepository.kt:340-344`, `ui/screens/BlackPearlDeviceBatchControls.kt:146-156`). Do not touch DEVICE setting rows; those invoke setters (`BlackPearlDeviceBatchControls.kt:93-143`).
- Selecting EQ Library then reopening My DAC retains the My DAC subtree in a saveable-state holder and does not call disconnect/reconnect (`ui/EqLibraryApp.kt:869-874,958-975,1103-1106`). Verify the same physical USB identity and truthful active-session status after return, then explicitly refresh both categories.

**Read-only terminology and observable limit:** Black Pearl READ operations transmit protocol read-request output reports (`data/blackpearl/AndroidBlackPearlUsbTransport.kt:201-232`). A timed-out nondestructive read can be automatically reissued once in the same session (500 ms timeout, maximum two request attempts; `AndroidBlackPearlUsbTransport.kt:432-436`, `BlackPearlReadRetry.kt:5-30`). This bounded transport behavior is not a manual retry or a setting write. The procedure must not be described as zero USB OUT reports or zero read-request reissues. C05-C can establish that no setting-changing UI/API path was invoked and that all required displayed values match the baseline at the UI's presentation precision; it cannot establish a zero-transfer USB trace or bit-for-bit equality of raw protocol values. EQ band and playback-gain values are rounded for display to 0.01 dB, while raw EQ gain uses 1/256 dB increments (`strings_v06.xml:39,128`; `BlackPearlReadCodec.kt:19,24`; `BlackPearlProtocol.kt:24,93`). DEVICE Volume is displayed as an integer percent and is not a unit-compatible comparison for EQ Playback gain (`BlackPearlDeviceBatchControls.kt:95-99,497-499`). Compare each field only to the same field's baseline/final displayed value; do not compare EQ Playback gain to DEVICE Volume. If DEVICE reports **Inconsistent channel read** for balance, stop and do not pass because the UI suppresses the separate raw left/right pair (`BlackPearlDeviceControlReadCodec.kt:198-204`; `BlackPearlDeviceBatchControls.kt:121-132`). A verified EQ read can initialize app-local gain-baseline metadata if absent (`BlackPearlFlasher.kt:53-56`); this is not a DAC mutation.

## Step-by-step session procedure

Each step specifies the expected UI/state, exact action, USB effect, expected UI result, evidence claim, and abort condition. `NO USB` means that the step issues no DAC protocol request; all ADB commands use wireless ADB only. `READ` is a production protocol read request and may send an outbound HID report; the existing transport may reissue one nondestructive request automatically after timeout. Neither label establishes zero physical USB traffic. This is a fresh availability check for the already-attached resume path, not permission to reconnect.

### A. Mac-side preflight before using the Pixel

#### Step 1 — Verify the frozen local candidate and APK
- **UI/state:** No device session; tracked candidate files remain read-only. Local Git exclude metadata may be verified for raw evidence handling.
- **Action:** Verify candidate branch/HEAD/tree, app/main/test/androidTest/app trees, local APK SHA-256, package/version/code, and signer against the pinned identity above. Before any phone or ADB access, also choose a unique UTC session ID and verify that a prospective file under `.unlazy/v080-beta/evidence/c05c-<UTC-session-id>/` is ignored in this exact candidate checkout: run `git check-ignore -v --no-index` on a prospective filename and require the repository-local exclude rule `/.unlazy/v080-beta/evidence/c05c-*/` from the file resolved by `git rev-parse --git-path info/exclude`. If the rule is absent, add only that rule to the repository-local exclude file returned by that command, from this exact candidate checkout, and repeat the check. This changes local Git metadata only, not the tracked candidate/tree. Do not create the session directory or request/use the Pixel until the prospective path is positively verified as ignored.
- **USB effect:** NO USB.
- **Expected UI result:** Not applicable; exact local identity matches.
- **Expected physical evidence:** None. Record the candidate identity and the verified local evidence destination as preflight metadata; this is not device evidence.
- **Abort:** Any identity/hash mismatch, missing APK, dirty tracked candidate tree, package/signer mismatch, or failure to verify that the prospective evidence path is ignored; do not access the Pixel.

#### Step 2 — Refresh live release prerequisites
- **UI/state:** PR #70 remains open/draft at the frozen candidate; no device is accessed.
- **Action:** Refresh PR head/base/mergeability and exact-head checks, current main delta and policy, issue #71, and latest stable release. Confirm stable/latest remains v0.7.2.
- **USB effect:** NO USB.
- **Expected UI result:** All required exact-head checks successful; no candidate-relevant drift or conflict.
- **Expected physical evidence:** None. This protects candidate provenance only.
- **Abort:** Candidate head differs, a required check fails/is pending, relevant app/test/security overlap appears, a merge conflict is reported, or release identity is ambiguous.

### B. Wireless Pixel and already-attached-session preflight

#### Step 3 — Confirm current owner availability and establish wireless ADB
- **UI/state:** Owner has just made the released Pixel 9 available; Black Pearl is left attached if it remains attached from the stopped session.
- **Action:** Discover/connect through current wireless ADB service; verify `adb devices -l` identifies Pixel 9 (`tokay`, API 37) on a network serial and shows no USB ADB transport.
- **USB effect:** NO USB to the DAC; Android control is wireless.
- **Expected UI result:** One unambiguous Pixel target is available over wireless ADB.
- **Expected physical evidence:** Timestamped Pixel model/API/network transport confirmation.
- **Abort:** No fresh owner availability, ambiguous/missing wireless target, or any USB ADB connection; do not switch transports.

#### Step 4 — Verify the installed application identity
- **UI/state:** Correct Pixel is selected over wireless ADB; no DAC read is requested.
- **Action:** Check installed package/version/code/signer and pull/hash the installed APK where Android permits; compare it and the local APK with the pinned candidate identity.
- **USB effect:** NO USB to the DAC.
- **Expected UI result:** Installed `com.weekssa.opraeqforuapp`, version `0.8.0-beta` / code 10, signer and SHA-256 match the exact debug candidate.
- **Expected physical evidence:** Package metadata and local/installed APK hashes.
- **Abort:** Any mismatch or an installed-artifact hash that cannot be established deterministically; do not open the DAC UI.

#### Step 5 — Verify existing physical identity and production session
- **UI/state:** The Black Pearl must still be attached from the stopped session; do not change cables or permissions.
- **Action:** Over wireless ADB inspect Android USB descriptors and app state. Require VID/PID `3302:43e8`, manufacturer `TTGK Technology`, product `TE-C`, serial `330243E8260129`; require production UI identity **TRN Black Pearl** and already-current **Connected** state. If the app is on a root screen, open only the existing connected-device surface; do not initiate connection.
- **USB effect:** NO USB to the DAC; host descriptor/session metadata inspection only.
- **Expected UI result:** Exact prior fingerprint is present and the existing My DAC session is already current. The UI does not expose serial or session-generation ID.
- **Expected physical evidence:** USB fingerprint paired with production identity/current-session presentation, Pixel PID/activity, screenshot/hierarchy timestamp.
- **Abort:** DAC absent, fingerprint incomplete/mismatched, chooser/different device, **Device detected**, **Connect**, permission prompt, inactive/stale session, or any uncertainty. Do not Connect, reconnect, regrant permission, unplug, or replug.

#### Step 6 — Establish the baseline read screen and idle state
- **UI/state:** My DAC is open for the already-current Black Pearl. No recovery card or active operation is being acted upon.
- **Action:** Confirm the normal EQ status is **Verified current hardware** and DEVICE status is **Current device state** / **Values verified from the connected DAC** where currently displayed; wait for any already-running operation to finish without touching it.
- **USB effect:** NO USB from inspection/wait. Any earlier connection-triggered reads are not counted as this session's fresh baseline.
- **Expected UI result:** Positive current-state labels, no spinner/error, no active read/write.
- **Expected physical evidence:** Timestamped initial UI/session snapshot; fresh baseline is established only by Steps 7–10.
- **Abort:** Stale/Last read, Device settings, missing positive status, read failure, busy operation that does not settle safely, or an unexpected recovery prompt; do not use recovery-only controls.

### C. Production baseline reads

#### Step 7 — Explicit baseline EQ read
- **UI/state:** My DAC → EQ, with **Verified current hardware** and no active operation.
- **Action:** Tap normal **Edit EQ** once. `openBlackPearlEditor()` calls `readBlackPearlSnapshot()` before opening the local editor.
- **USB effect:** READ; a current-session check gates snapshot publication. Transport may make its documented single automatic nondestructive reissue after timeout.
- **Expected UI result:** Editor opens only after a successful fresh read; otherwise it reports failure and the session stops.
- **Expected physical evidence:** Exact production EQ read result tied to the still-current session and timestamp.
- **Abort:** Spinner/error persists, the editor does not open, session changes, or any mutation/review/apply surface is encountered; no manual retry.

#### Step 8 — Record baseline EQ and close locally
- **UI/state:** Normal EQ editor reached by Step 7; the editor says edits are local only.
- **Action:** Open **All bands**, record each band index/type/frequency/gain/Q and active slot; record visible Playback gain. Do not tap band rows or change values. Tap **Close** once.
- **USB effect:** NO USB; All bands and Close are local presentation/state actions.
- **Expected UI result:** My DAC EQ screen returns with **Verified current hardware**; closing the editor issues no hardware write.
- **Expected physical evidence:** Complete displayed baseline EQ fields and active slot, screenshot/hierarchy timestamps; Playback gain recorded separately from DEVICE Volume.
- **Abort:** Any edited value, Apply/Review/Safe gain action, incomplete band view, missing positive status, or unexpected hardware-operation indication.

#### Step 9 — Explicit baseline DEVICE read
- **UI/state:** My DAC → DEVICE for the same already-current Black Pearl.
- **Action:** Tap normal **Refresh** once and wait for completed success.
- **USB effect:** READ through `readBlackPearlQualificationControls()` and its production DEVICE snapshot path; existing bounded automatic read retry only.
- **Expected UI result:** **Current device state** / **Values verified from the connected DAC**. Record volume, DAC filter, gain mode, amplifier topology, balance, microphone gain, and USB audio mode or its truthful unavailable state.
- **Expected physical evidence:** Complete displayed baseline DEVICE values with timestamp. Optional unavailable UAC mode alone is not failure.
- **Abort:** Read failure, missing positive state, **Inconsistent channel read**, session change, or any need to open/edit a setting row; no manual retry or setter.

#### Step 10 — Capture firmware only if needed
- **UI/state:** Successful baseline DEVICE view.
- **Action:** If firmware is part of the capture record, expand **About this DAC**; do not touch any setting control.
- **USB effect:** NO USB; this is a local display toggle.
- **Expected UI result:** Firmware detail expands without changing current DEVICE values.
- **Expected physical evidence:** Firmware text/screenshot only if shown.
- **Abort:** Any action that presents a setter, write, or device command; close/navigate back without activating it.

### D. Attached navigation and explicit final observations

#### Step 11 — Navigate away and back while attached
- **UI/state:** Both baseline reads completed; no active operation; exact Black Pearl remains attached.
- **Action:** Use visible root navigation **My DAC → EQ Library**, then the existing visible route back to the already-connected **My DAC** surface. Do not select another output/device. If the existing My DAC surface cannot be reopened without a Connect/reconnect path, stop.
- **USB effect:** NO USB; the route change retains presentation and does not itself disconnect/reconnect or trigger the final read.
- **Expected UI result:** TRN Black Pearl and current-session state remain truthful after return. Navigation alone is not treated as a fresh physical read; final observations are Steps 12 and 14.
- **Expected physical evidence:** Before/after Pixel PID, USB fingerprint, product identity, screenshots/activity and visible current-session status; internal session generation is not exposed and is not claimed.
- **Abort:** DAC/session/PID changes, stale or false presentation, chooser, missing route, or any Connect/reconnect/permission prompt. Do not reconnect.

#### Step 12 — Explicit final EQ read
- **UI/state:** Returned to the existing My DAC → EQ surface; require current **Verified current hardware** and idle status.
- **Action:** Tap normal **Edit EQ** once, wait for fresh read and successful editor, open **All bands**, record all fields/slot, then **Close** without editing.
- **USB effect:** READ for Edit EQ; All bands/Close issue NO USB.
- **Expected UI result:** Successful final snapshot and return to **Verified current hardware**.
- **Expected physical evidence:** Final production EQ observation. Compare each band, slot, and displayed Playback gain to its matching baseline field.
- **Abort:** Failed/stale read, changed session, incomplete values, or any setting/mutation action; no manual retry.

#### Step 13 — Explicit final DEVICE read
- **UI/state:** Same returned My DAC session, DEVICE tab, no active operation.
- **Action:** Tap **Refresh** once and wait for completed success; capture all displayed DEVICE fields and truthful optional UAC state.
- **USB effect:** READ through the production DEVICE snapshot path; existing bounded automatic read retry only.
- **Expected UI result:** **Current device state** / **Values verified from the connected DAC**.
- **Expected physical evidence:** Final DEVICE observation compared field-to-same-field with baseline at displayed precision. An **Inconsistent channel read** is not passable.
- **Abort:** Read failure, missing current indicator, changed session/value, balance inconsistency, or any need to change settings; no manual retry.

#### Step 14 — Capture final evidence and complete safely
- **UI/state:** Final reads are complete and app shows no active read/write.
- **Action:** Before creating the session directory or writing any artifact, re-run `git check-ignore -v --no-index` on a prospective file at the exact destination (for example, `.unlazy/v080-beta/evidence/c05c-<UTC-session-id>/commands.txt`) and require the repository-local exclude rule `/.unlazy/v080-beta/evidence/c05c-*/`. This check is valid before the file or directory exists; do not use a missing-path `git status` result as evidence. Then capture final PID/activity, USB descriptors via wireless ADB, screenshots/UI hierarchy, non-cleared app logcat, UTC timestamps, commands, and artifact SHA-256 values. Store raw evidence in the new unique `.unlazy/v080-beta/evidence/c05c-<UTC-session-id>/` directory under the verified repository-local exclude rule; never overwrite the stopped report or commit raw captures.
- **USB effect:** NO USB to the DAC from these observations; wireless ADB only.
- **Expected UI result:** Final positive current-session presentation; no active operation.
- **Expected physical evidence:** Hash-indexed evidence establishing exact identity, baseline/final displayed values, attached navigation, and no state-changing control invoked. It does not establish zero USB reports or raw bit-for-bit equality.
- **Abort:** Ignore verification fails, any unexpected value difference, missing evidence, or ambiguous state; preserve what exists, do no repair/restoration write, stop, and release the Pixel.

#### Step 15 — Release phone and report outcome
- **UI/state:** No hardware operation is active; all collected evidence is saved locally.
- **Action:** Perform no further device action. Immediately report **PHONE RELEASED — YOU CAN TAKE THE PIXEL BACK**, with C05-C pass/stopped and any discrepancy.
- **USB effect:** NO USB.
- **Expected UI result:** Not applicable; phone is released.
- **Expected physical evidence:** Session outcome and release timestamp in the sanitized record.
- **Abort:** If a READ is still active, do not disconnect or kill anything; allow the nondestructive read to settle safely, then release. No setting restoration is needed or permitted because the plan is read-only.

Target total Pixel occupancy is 8–10 minutes; hard planning limit is 15 minutes, except only as necessary to allow an already-active nondestructive read to settle safely. Drop optional screenshots/log details first; do not omit final EQ/DEVICE observations. JA11, EW300, and manual TalkBack are excluded.

## C05-C acceptance

**Pass only if all of the following are evidenced:**

- Exact frozen candidate and APK verified before DAC access; Pixel is controlled through wireless ADB only; PR/check state is still valid.
- Android USB descriptors match the exact known Black Pearl fingerprint where exposed, and the app identifies TRN Black Pearl with an already-current Connected session; no Connect/reconnect or permission action is used.
- Baseline EQ and DEVICE reads succeed through the production paths and are completely recorded; optional UAC absence is represented truthfully.
- My DAC → EQ Library → My DAC navigation occurs while attached; the observed USB identity and PID remain the same, and production UI reports a current session at the recorded checkpoints. The app does not expose internal session generation, so no stronger continuity claim is made between reads.
- Final explicit EQ and DEVICE reads succeed on the same current session; every required displayed field matches its corresponding baseline field at the UI's presentation precision. An **Inconsistent channel read** balance state is not passable. This comparison does not claim bit-for-bit raw protocol equality where the UI rounds or coarsens values.
- No state-changing control/API path, Reset, Flash, Save, Apply, DEVICE setter, induced fault, process kill, or disconnect was used. All artifacts and their hashes are recorded. The conclusion is bounded: no state-changing control/API path was invoked, and the required displayed values remained equal across captured production reads at the UI's presentation precision. This does not claim raw bit-for-bit equality or zero USB transfer count.

**Stop / not pass** on any candidate/APK mismatch, absent fresh owner availability, wireless ADB ambiguity, Black Pearl no longer attached from the stopped attempt, unknown USB identity, no already-current production session, failed/incomplete read, changed session/PID, stale or false presentation, changed value, unexpected setting mutation, `UNKNOWN` USB effect, or other unclear state. Preserve the evidence, do not retry manually or attempt repair/restoration, release the phone, and continue Mac-side diagnosis. Do not substitute a fresh attach/connect flow without a separate plan revision and independent review.

## Maintained references

- Master execution contract: `docs/implementation/v0.8.0-beta-master-directive.md`
- Product/design baseline: `docs/implementation/v0.8.0-beta-mission.md` §48
- Black Pearl physical procedure background: `docs/BLACK_PEARL_V0.6_RESTORE_DEFAULTS_HANDS_ON_CHECKLIST.md` (identity/read and stop rules only; reset/write checklist is out of scope)
- Canonical status: `docs/implementation/v0.8.0-beta-autonomy-status.md`
- Acceptance evidence: `docs/implementation/v0.8.0-beta-acceptance-evidence.md`
- Watchdog issue: https://github.com/weekssa/OPRA-EQ-for-UAPP/issues/71
