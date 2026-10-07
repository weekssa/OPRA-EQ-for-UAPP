# v0.8.0 Beta — Final Physical Session Plan

**Plan revision:** 2.11
**Disposition:** G3 PASS — independent review approves only this restricted cold, disconnected route against the exact frozen candidate. This procedure is for one complete, read-only C05-C session after off-phone preparation and the owner's current Pixel-availability authorization. It uses only the cold, disconnected app route: before launch the app process must be absent; the agent may launch the installed app once, open My DAC, and ensure DEVICE content is composed while the fresh transport is still disconnected; only after this offline preflight settles may the agent use the ordinary Connect action once after exact USB identity is confirmed, with at most one exact app/device permission approval if Android requests it. An already-running app, already-current Connected session, or connected-device-card route is not eligible because saved restore state can resume a DEVICE setter when that content is composed. The agent must not force-stop, kill, disconnect, reconnect, or reattach to create the cold route. No cable change, extra permission action, setting mutation, reset/recovery, fault injection, process kill, or disconnect is authorized. No additional owner approval is required between the listed actions. This does not waive the 10-minute target or 15-minute hard cap. C05-C remains PARTIAL / NOT PASS until a complete compliant run is captured and reconciled.
**Master directive:** `docs/implementation/v0.8.0-beta-master-directive.md`, version 1.2; introduction commit and body hash are recorded in the directive and resume manifest.
**Review basis:** frozen app/test candidate plus the exact-source read-path audit, reconciled partial C05-C attempt, and current narrow authorization. Revisions 2.7–2.10 were independently reviewed and not approved because of actor/authorization mismatch, missing My DAC route, saveable-tab ambiguity, and a conditional setter that could resume when DEVICE content is composed. Revision 2.11 excludes all already-running/current-session and connected-card routes. It requires the package process to be absent, launches once through the ordinary launcher (never an `ACTION_USB_DEVICE_ATTACHED` intent), opens My DAC, and composes DEVICE content while the new transport is disconnected. If saved reset continuation exists, the frozen effect takes its stale/disconnected fail-closed branch before the setter and can emit a stop message; that observation aborts the session. Connect is allowed only after a clean, settled disconnected preflight. The independent reviewer passed the restricted route and the added launcher-entry and five-second observation clarifications on 2026-10-07 against the exact candidate. The reviewer corrected the rationale: the manifest has no USB-attach intent filter, but an explicit `ACTION_USB_DEVICE_ATTACHED` intent can still be consumed and trigger Connect, so that intent is prohibited. G3 is PASS for this route only; it does not establish a C05-C physical pass. Candidate HEAD/tree remain `5c7d19cf2f77e448a97a1ee9d520ad4d408f120f` / `e11669f0d67f9fb3c3762ea814f16fb7c4bdab8d`; no product/test change is proposed.

## Purpose and current boundary

This is one short, read-only Black Pearl C05-C qualification. It checks exact device identity, the active production session, production EQ and DEVICE state reads, attached navigation away and back, truthful current-session presentation, and matching final explicit reads. The Black Pearl must already be physically attached. The app process must be absent at entry. After exact Pixel/APK/USB checks, launch once, reach My DAC, and ensure DEVICE content has composed while the fresh transport is still Disconnected. Continue only if that screen settles without any restore/recovery message, busy operation, error, or uncertainty; otherwise the pending-state guard has correctly stopped the route and this physical session ends before Connect. Never force-stop an existing app or reuse an already-current session. The connection path can trigger production READ requests, but no DAC setting is changed. It does not create a reset fault.

An earlier C05-C attempt on 2026-10-06 at approximately 18:20Z stopped because the old plan incorrectly expected the recovery-only **Read current EQ** action in an ordinary Connected state. That control is conditional on a saved interrupted-reset state. The attempt captured the initial verified-EQ presentation but did not refresh DEVICE, navigate, or make final explicit reads. It is **incomplete and not a C05-C pass** and is superseded by the later partial attempt described above. No Reset, Apply, Flash, Save, DEVICE setter, disconnect, process kill, fault, or restorative write was performed. Preserve the stop report at `.unlazy/v080-beta/evidence/c05c-20261006T182006Z/C05-C-stop-report.txt` (SHA-256 `e32ff687c5574894652a98f1512bdaa3f2147c9e319770f81d656115e6558d6e`); the raw evidence is local and is not to be copied into a commit.

The latest physical attempt ran 2026-10-06T23:24:31Z–23:44:16Z (19m45s) on the exact candidate/APK and Pixel 9/API 37 over wireless ADB. The Black Pearl fingerprint and baseline/final EQ/DEVICE values were captured, the attached away/back navigation occurred, and the retained 36-file checksum index verifies. The 15-minute hard cap was exceeded by 4m45s; the independent reconciliation found no active read at the 15-minute point and no restoration write was needed. The final post-navigation PID and USB-descriptor output was not saved contemporaneously; the post-session addendum cannot replace it. Therefore the prior evidence is PARTIAL / NOT PASS and cannot be combined with a short supplement. A complete new single-session baseline/navigation/final-read run is required. Preserve the old raw evidence unchanged in its ignored candidate-worktree folder; do not copy it into Git.

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
- PR #70 live read at `2026-10-07T00:46Z`: open/draft, head exactly `5c7d19cf2f77e448a97a1ee9d520ad4d408f120f`, base `main` at `c702ef0149b4c647446639cfcb0ab25c09159db3`, `mergeable=true`, `mergeable_state=clean`. The four PR-triggered workflow runs associated with the exact head are completed/successful. The exact-head check-run snapshot refreshed at `2026-10-07T00:46Z` is 8/8 successful: build `112181047518`, min-api-smoke `112181046209`, emulator-ui-test `112181045077`, CodeQL `112152334761`, Priority community `112150826284`, Catalog `112150826043`, Kotlin analysis `112150825874`, and dependency submission `112150813291`. Refresh exact check runs again immediately before the next physical session and before merge.
- Current `origin/main` at the same snapshot: `79d600529621257136adb4d9e1a97e8ae8521d7e` (`Refresh automated source currentness`). Comparison from the PR base confirms four catalog/currentness paths only (`catalog/catalog.json`, `catalog/discovery/github_candidates.json`, `catalog/discovery/headphone_identity_audit.json`, and `catalog/source_health.json`), with no candidate app/test path overlap. The latest currentness refresh adds one community catalog record. Keep **BASE-DRIFT-B**; do not rebase or rebuild for this physical session. Recheck and classify any further delta during pre-merge verification.
- Required API35 evidence is closed: jobs `112171539825`, `112177030919`, and `112181045077` each passed 64/64 consecutively; final JUnit artifact `11400405767`, SHA-256 `3eb0f7ed51db453d95945cd9920ae7ec588dca22102e3920b3d850164a71d757`, has 0 failures/errors/skips. Do not rerun without candidate invalidation.
- API26 populated Library is PASS/CLOSED on repository-default Pixel 2 / API26 / x86_64 at 48 MiB. Do not reopen for this session.
- Stable/latest remains v0.7.2. No merge or publication occurs during this procedure.

## Source-grounded production read paths

The ordinary Connected UI does not show the recovery-only **Read current EQ** button. Do not open Reset or recovery UI to find it.

For a cold app process, the Black Pearl transport initializes as **Disconnected**; detecting the already-attached device does not open it or send a DAC request, and a fresh ViewModel does not rearm the prior-session reconnect path. Launch through the app's ordinary launcher entry only; do not launch with `ACTION_USB_DEVICE_ATTACHED`, because that path can immediately request Connect (`MainActivity.kt:56`; `EqLibraryApp.kt:384`). The reset section keeps progress in `rememberSaveable`; when DEVICE is composed with a pending saved step but no current snapshot/session, its effect clears the pending step and reports that restore stopped before reaching the setter branch (`ui/screens/BlackPearlDeviceResetSection.kt:100-109,126-157,185-192`). Therefore the procedure requires DEVICE composition while still disconnected and before its one permitted Connect. If a restore/recovery message or operation status appears, or the screen does not settle cleanly, abort without Connect. A connected process, saved connected session, and Connected-device-card route are excluded because DEVICE composition there can resume the setter. After a clean disconnected preflight, My DAC shows the ordinary Connect action for the recognized device. Under the owner's current renewed authorization, the agent may launch this installed app once, use only the visible normal Connect action once after exact identity verification, and approve at most one exact app/device permission prompt if it appears. This authority applies only after the fresh post-release Pixel-availability statement, exact candidate/APK and wireless-ADB checks, and confirmation that the Black Pearl is already attached; it permits no setting mutation or unrelated UI action. The specifically listed EQ/DEVICE tab selections in Steps 7, 9, 12, and 13 are local-only selection callbacks; source verifies they issue no USB request. A Connect opens/claims the already-attached device when permission already exists or raises Android's permission request when it does not; once Connected, production observers issue EQ and DEVICE reads. These are session-entry READs, not C05-C baseline data. No owner confirmation is needed between the specified steps; capture each action and result contemporaneously. Source references: `data/blackpearl/AndroidBlackPearlUsbTransport.kt:49,95-132,284-317`; `data/dac/DacSessionRepository.kt:37,227-250`; `ui/EqLibraryApp.kt:384,958-975,1103-1106`; `ui/screens/MyDacScreen.kt:100-113,118,226-285`; `ui/screens/BlackPearlDeviceResetSection.kt:100-109,126-157,185-192`; and the observers cited below.

- Connecting starts an EQ read from the Black Pearl connection collector (`app/src/main/java/com/weekssa/opraeqforuapp/data/hardware/HardwareEqRepository.kt:94-103`) and a DEVICE read from `EqLibraryViewModel.kt:438-446`. The EQ snapshot is accepted only while the same session remains current (`HardwareEqRepository.kt:343-371`); its production reader reads the filter bands and global gain (`HardwareEqSnapshotReaders.kt:11-35`).
- For explicit EQ reads, My DAC's normal **Edit EQ** button (`ui/screens/MyDacScreen.kt:568-574`) calls `EqLibraryViewModel.openBlackPearlEditor()` (`ui/EqLibraryViewModel.kt:624-676`), which first calls `hardwareRepository.readBlackPearlSnapshot()`. The editor states that edits are local only (`ui/screens/MyDacEqEditorScreen.kt:99-105,135-165`). **All bands** displays the observed values (`ui/screens/MyDacEqEditorScreen.kt:522-562`). **Close** clears only editor state (`ui/EqLibraryViewModel.kt:972-976`). Do not change any value or enter Safe gain, Review, Apply, Reset, or Save.
- For explicit DEVICE reads, the DEVICE tab's **Refresh** button (`ui/screens/DeviceOperationStatus.kt:131-185`, `ui/screens/BlackPearlDeviceBatchControls.kt:66-78`) calls `readBlackPearlQualificationControls()` and the read-only snapshot path (`ui/EqLibraryViewModel.kt:1247-1277`, `data/dac/DacControlRepository.kt:313-362`). It reads firmware, filter, gain mode, amplifier topology, microphone gain, both balance channels, playback gain, and optional USB audio mode. If USB audio mode is unavailable, record the displayed unavailable state; a nullable UAC mode is not by itself a read failure (`DacControlRepository.kt:340-344`, `ui/screens/BlackPearlDeviceBatchControls.kt:146-156`). Do not touch DEVICE setting rows; those invoke setters (`BlackPearlDeviceBatchControls.kt:93-143`).
- Selecting EQ Library then reopening My DAC retains the My DAC subtree in a saveable-state holder and does not call disconnect/reconnect (`ui/EqLibraryApp.kt:869-874,958-975,1103-1106`). Verify the same physical USB identity and truthful active-session status after return, then explicitly refresh both categories.
- `MyDacScreen` stores `selectedTabIndex` with `rememberSaveable` and its EQ/DEVICE tab callbacks only select local content (`ui/screens/MyDacScreen.kt:113,265-285`). The My DAC saveable-state holder preserves the selected tab across root navigation (`ui/EqLibraryApp.kt:1103-1106`). Therefore every baseline and final read step below explicitly checks and, only when needed, selects the required visible tab once before invoking the production read. Tab selection is **NO USB**; only Edit EQ or Refresh triggers the documented READ.

**Read-only terminology and observable limit:** Black Pearl READ operations transmit protocol read-request output reports (`data/blackpearl/AndroidBlackPearlUsbTransport.kt:201-232`). A timed-out nondestructive read can be automatically reissued once in the same session (500 ms timeout, maximum two request attempts; `AndroidBlackPearlUsbTransport.kt:432-436`, `BlackPearlReadRetry.kt:5-30`). This bounded transport behavior is not a manual retry or a setting write. The procedure must not be described as zero USB OUT reports or zero read-request reissues. C05-C can establish that no setting-changing UI/API path was invoked and that all required displayed values match the baseline at the UI's presentation precision; it cannot establish a zero-transfer USB trace or bit-for-bit equality of raw protocol values. EQ band and playback-gain values are rounded for display to 0.01 dB, while raw EQ gain uses 1/256 dB increments (`strings_v06.xml:39,128`; `BlackPearlReadCodec.kt:19,24`; `BlackPearlProtocol.kt:24,93`). DEVICE Volume is displayed as an integer percent and is not a unit-compatible comparison for EQ Playback gain (`BlackPearlDeviceBatchControls.kt:95-99,497-499`). Compare each field only to the same field's baseline/final displayed value; do not compare EQ Playback gain to DEVICE Volume. If DEVICE reports **Inconsistent channel read** for balance, stop and do not pass because the UI suppresses the separate raw left/right pair (`BlackPearlDeviceControlReadCodec.kt:198-204`; `BlackPearlDeviceBatchControls.kt:121-132`). A verified EQ read can initialize app-local gain-baseline metadata if absent (`BlackPearlFlasher.kt:53-56`); this is not a DAC mutation.

## Step-by-step session procedure

Each step specifies the expected UI/state, exact action, USB effect, expected UI result, evidence claim, and abort condition. `NO USB` means that the step issues no DAC protocol request; all ADB commands use wireless ADB only. `READ` is a production protocol read request and may send an outbound HID report; the existing transport may reissue one nondestructive request automatically after timeout. Neither label establishes zero physical USB traffic. The Black Pearl must already be physically attached. Only when the target package process is absent may the agent launch once, compose DEVICE while disconnected, then use one normal Connect and at most one exact permission approval in Steps 5a–5d. No reattachment, force-stop, connected-card entry, current-session reuse, cable change, repeated Connect, or additional approval is authorized.

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

### B. Wireless Pixel and disconnected-session preflight

#### Step 3 — Confirm current owner availability and establish wireless ADB
- **UI/state:** Owner has just made the released Pixel 9 available; Black Pearl is left attached if it remains attached from the stopped session.
- **Action:** Discover/connect through current wireless ADB service. In `adb devices -l`, select an explicit network endpoint whose `ro.serialno` is `46141FDAQ003KZ`, model is Pixel 9 / `tokay`, and product is `tokay`; verify no Pixel USB ADB transport exists. If multiple network aliases report that same physical serial, use one explicit address endpoint and never send commands to the other alias. An emulator may remain visible but is not selected. Prepare this target-selection script off-phone.
- **USB effect:** NO USB to the DAC; Android control is wireless.
- **Expected UI result:** The exact Pixel 9 hardware identity is verified over one explicit wireless ADB endpoint, with no USB ADB transport used.
- **Expected physical evidence:** Timestamped Pixel model/API/network transport confirmation.
- **Abort:** No fresh owner availability, ambiguous/missing wireless target, or any USB ADB connection; do not switch transports.

#### Step 4 — Verify the installed application identity
- **UI/state:** Correct Pixel is selected over wireless ADB; no DAC read is requested.
- **Action:** Check package/version/code and `pm path`; compute the installed base APK SHA-256 on-device over wireless ADB where readable, and compare it to the locally precomputed APK SHA-256. Capture signer identity from the pinned local APK preflight; if the installed APK cannot be hashed deterministically on-device, pull and hash it over wireless ADB. Prepare and test the short command sequence offline so this is one bounded identity check, not iterative probing.
- **USB effect:** NO USB to the DAC.
- **Expected UI result:** Installed `com.weekssa.opraeqforuapp`, version `0.8.0-beta` / code 10, signer and SHA-256 match the exact debug candidate.
- **Expected physical evidence:** Package metadata and local/installed APK hashes.
- **Abort:** Any mismatch or an installed-artifact hash that cannot be established deterministically; do not open the DAC UI.

#### Step 5 — Verify attached Black Pearl identity and require a cold app process
- **UI/state:** The Black Pearl must already be physically attached. The Pixel is selected over wireless ADB. The app must not be running. Do not change cables, stop an app process, or change Android permissions.
- **Action:** Over wireless ADB inspect Android USB descriptors and process state. Require VID/PID `3302:43e8`, manufacturer `TTGK Technology`, product `TE-C`, serial `330243E8260129`. Capture only the matching device block and an exact process check for `com.weekssa.opraeqforuapp`; do not store or review unrelated USB accessory inventories. Require the target app process to be absent. If a PID exists, even if My DAC appears disconnected or idle, stop and release the Pixel; do not force-stop or enter the app. An already-current session and the connected-device-card route are excluded.
- **USB effect:** NO USB to the DAC; host descriptor and process metadata inspection only.
- **Expected UI result:** Exact attached fingerprint is present, the target app process is absent, and no app session can be current.
- **Expected physical evidence:** Timestamped exact USB fingerprint and wireless ADB output establishing that the target package process is absent.
- **Abort:** DAC absent, fingerprint incomplete/mismatched, package PID exists, USB ADB transport appears, or any uncertainty. No unplug, replug, force-stop, cable change, or manual permission change.

#### Step 5a — Launch once and reach My DAC while disconnected
- **UI/state:** Step 5 verifies the exact attached Black Pearl and an absent app process; a fresh owner availability statement has already been received for this session. The remaining sequence fits the occupancy checkpoints.
- **Action:** Under the owner's direct autonomous-execution authorization, launch the already-installed app once from its ordinary launcher entry over the selected wireless ADB target. Do not dispatch an `ACTION_USB_DEVICE_ATTACHED` intent; `MainActivity` can parse that intent and the app's recognized-device effect can initiate Connect before the safety preflight. Navigate through visible product UI to My DAC. Do not use package install, force-stop, settings, typing, or any unrelated app surface.
- **USB effect:** NO USB — a fresh app process initializes the transport Disconnected; detecting the already-attached device does not open it or send a DAC protocol request.
- **Expected UI result:** My DAC shows the detected Black Pearl as disconnected with its ordinary Connect action. No Connected/current-session state is present.
- **Expected physical evidence:** Timestamped screen/UI capture, launcher command/result, disconnected My DAC identity, and foreground app PID.
- **Abort:** Automatic connection, wrong/stale device surface, unexpected chooser/permission prompt before Connect, active/current session, or absence of a clearly labeled ordinary Connect action. If launch intent resolution is ambiguous, do not guess or retry through a different entry point.

#### Step 5b — Compose DEVICE content before connection
- **UI/state:** My DAC is visibly disconnected. Because Step 5 requires the target package process to be absent, the newly launched process has a fresh ViewModel/transport and no current Black Pearl snapshot/session. The selected tab may be EQ or DEVICE.
- **Action:** If DEVICE is not visibly selected, tap the visible DEVICE tab once. Wait for composition and any one-shot local status/message to settle before any Connect action; observe the full settling interval, with UI captures at entry and after five seconds, for recovery/restore text or status, an active operation, spinner, error, or other uncertainty.
- **USB effect:** NO DAC request under the required disconnected/no-current-snapshot precondition. If a saved pending restore step is restored, the effect's missing/noncurrent-snapshot branch clears the pending indices and returns before its setter branch. That branch can report **Restore stopped because the current Black Pearl state could not be verified**; a recovered pending state is therefore an abort, not permission to continue. A clean screen is required before Connect.
- **Expected UI result:** DEVICE content is selected; My DAC remains clearly Disconnected with no active operation, restore/recovery message, or error after the settling interval.
- **Expected physical evidence:** Timestamped selected DEVICE/disconnected UI plus start/end times for the settling interval and both UI observations before Connect. Record any stop notice if one appears.
- **Abort:** The app becomes Connected, any restore/recovery notice appears, a busy/error state persists, the screen does not settle within the checkpoint, or any DAC request occurs before Connect. Stop and release; do not connect or clear/reset any state.

#### Step 5c — Agent performs one normal Connect
- **UI/state:** Step 5b confirms DEVICE content is selected while My DAC remains Disconnected; the exact USB fingerprint still matches; the same freshly launched app process remains foreground.
- **Action:** Use the visible My DAC **Connect** action exactly once through the authorized UI automation. Do not tap again.
- **USB effect:** READ — if Android permission already exists, the transport opens the verified device and claims the interface; after Connected, production observers start EQ and DEVICE reads. If permission is absent, the tap first requests an Android permission decision, and no protocol read occurs before that decision. Automatic session-entry reads are not the C05-C baseline; they may emit read-request reports and use the bounded automatic nondestructive retry. The saved-step guard was cleared while disconnected, so this Connect cannot resume it.
- **Expected UI result:** Either the exact Android permission request described in Step 5d appears, or the app reaches Connected without a dialog because permission was already granted.
- **Expected physical evidence:** Timestamped pre-action UI, the single agent UI action in the contemporaneous command log, and the immediate resulting UI/system dialog or Connected state.
- **Abort:** Any chooser, other app/device identity, duplicate Connect, error, unexplained state, or write indication. Do not tap again.

#### Step 5d — Handle the exact Android USB permission request only if it appears
- **UI/state:** Step 5c produced a system request that names EQ Library and the exact verified Black Pearl.
- **Action:** If Android shows a permission request, continue only when it names this app and the exact previously verified Black Pearl. The agent may approve that one request under the owner's direct authorization; do not approve any chooser or ambiguous/mismatched request. If no dialog appears, skip only when the same single Connect has already reached Connected.
- **USB effect:** READ after approval completes the ordinary connection and triggers the session-entry EQ/DEVICE reads described in Step 5c; the permission confirmation itself sends no DAC setting write.
- **Expected UI result:** The app reaches Connected and settles its automatic session-entry reads.
- **Expected physical evidence:** Exact dialog identity, the single approved permission action in the contemporaneous command log, resulting Connected UI and timestamps. If skipped, retain Step 5c's Connected-state evidence.
- **Abort:** Ambiguous/mismatched dialog, owner declines, connection error, or failure to reach Connected. If declining, dismiss once and release; never grant twice, retry Connect, or change DAC settings.

#### Step 5e — Confirm the production session before collecting baseline values
- **UI/state:** The agent completed at most one Connect after the offline DEVICE preflight; any permission request received at most one approval after exact identity verification.
- **Action:** Verify My DAC visibly says **TRN Black Pearl / Connected**. Over wireless ADB verify the app is foreground, the exact same USB fingerprint remains attached, and no chooser/stale/error state exists. Wait for connection-triggered reads to settle without triggering another request.
- **USB effect:** NO USB from inspection/wait; session-entry READs are the only effects from the connection path.
- **Expected UI result:** Exact identity paired with a current production Connected presentation and settled initial observations.
- **Expected physical evidence:** Timestamped UI/hierarchy and foreground PID, descriptor record, and read-status capture.
- **Abort:** No exact Connected state, changed/ambiguous fingerprint, stale/error state, reads that do not settle within the session budget, or unexpected write/recovery surface. Stop and release without reconnecting.

#### Step 6 — Establish the baseline read screen and idle state
- **UI/state:** My DAC is open for the same Black Pearl after the cold disconnected preflight and single Connect. No recovery card or active operation is being acted upon.
- **Action:** Confirm the normal EQ status is **Verified current hardware** and DEVICE status is **Current device state** / **Values verified from the connected DAC** where currently displayed; wait for any already-running operation to finish without touching it.
- **USB effect:** NO USB from inspection/wait. Any earlier connection-triggered reads are not counted as this session's fresh baseline.
- **Expected UI result:** Positive current-state labels, no spinner/error, no active read/write.
- **Expected physical evidence:** Timestamped initial UI/session snapshot; fresh baseline is established only by Steps 7–10.
- **Abort:** Stale/Last read, Device settings, missing positive status, read failure, busy operation that does not settle safely, or an unexpected recovery prompt; do not use recovery-only controls.

### C. Production baseline reads

#### Step 7 — Explicit baseline EQ read
- **UI/state:** My DAC is open with the exact session current and no active operation. The pending-restore preflight completed with DEVICE visibly composed while disconnected and a clean settled screen before Connect; the selected tab may now be EQ or DEVICE.
- **Action:** If EQ is not visibly selected, tap the visible **EQ** tab once and verify the EQ content is selected. This callback only updates `selectedTabIndex`; it must not issue a DAC request. If EQ is already selected, do not tap it again. Require **Verified current hardware** and idle status, then tap normal **Edit EQ** once. `openBlackPearlEditor()` calls `readBlackPearlSnapshot()` before opening the local editor.
- **USB effect:** NO USB for a needed EQ-tab selection; then READ for Edit EQ. A current-session check gates snapshot publication. Transport may make its documented single automatic nondestructive reissue after timeout.
- **Expected UI result:** Editor opens only after a successful fresh read; otherwise it reports failure and the session stops.
- **Expected physical evidence:** Timestamped selected EQ state and exact production EQ read result tied to the still-current session.
- **Abort:** EQ tab is absent/ambiguous, tab selection changes session or starts unexpected USB activity, spinner/error persists, editor does not open, session changes, or any mutation/review/apply surface is encountered; no manual retry.

#### Step 8 — Record baseline EQ and close locally
- **UI/state:** Normal EQ editor reached by Step 7; the editor says edits are local only.
- **Action:** Open **All bands**, record each band index/type/frequency/gain/Q and active slot; record visible Playback gain. Do not tap band rows or change values. Tap **Close** once.
- **USB effect:** NO USB; All bands and Close are local presentation/state actions.
- **Expected UI result:** My DAC EQ screen returns with **Verified current hardware**; closing the editor issues no hardware write.
- **Expected physical evidence:** Complete displayed baseline EQ fields and active slot, screenshot/hierarchy timestamps; Playback gain recorded separately from DEVICE Volume.
- **Abort:** Any edited value, Apply/Review/Safe gain action, incomplete band view, missing positive status, or unexpected hardware-operation indication.

#### Step 9 — Explicit baseline DEVICE read
- **UI/state:** My DAC is open for the same Black Pearl after the disconnected DEVICE preflight and one successful Connect; the selected tab may be EQ or DEVICE.
- **Action:** If DEVICE is not visibly selected, tap the visible **DEVICE** tab once and verify the DEVICE content is selected. This callback only updates local tab selection and closes the local editor if it is open; it must not issue a DAC request. If DEVICE is already selected, do not tap it again. Tap normal **Refresh** once and wait for completed success.
- **USB effect:** NO USB for a needed DEVICE-tab selection; then READ through `readBlackPearlQualificationControls()` and its production DEVICE snapshot path; existing bounded automatic read retry only.
- **Expected UI result:** **Current device state** / **Values verified from the connected DAC**. Record volume, DAC filter, gain mode, amplifier topology, balance, microphone gain, and USB audio mode or its truthful unavailable state.
- **Expected physical evidence:** Timestamped selected DEVICE state and complete displayed baseline DEVICE values. Optional unavailable UAC mode alone is not failure.
- **Abort:** DEVICE tab is absent/ambiguous, tab selection changes session or starts unexpected USB activity, read failure, missing positive state, **Inconsistent channel read**, session change, or any need to open/edit a setting row; no manual retry or setter.

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
- **UI/state:** Returned to My DAC for the same current Black Pearl session. The disconnected DEVICE preflight completed before Connect, so pending reset continuation was either absent or caused a stop before the session began. Because saveable navigation may restore DEVICE from Step 9, the selected tab is not assumed.
- **Action:** If EQ is not visibly selected, tap the visible **EQ** tab once and verify the EQ content is selected. This callback only updates `selectedTabIndex`; it must not issue a DAC request. If EQ is already selected, do not tap it again. Require current **Verified current hardware** and idle status, then tap normal **Edit EQ** once, wait for fresh read and successful editor, open **All bands**, record all fields/slot, then **Close** without editing.
- **USB effect:** NO USB for a needed EQ-tab selection; READ for Edit EQ; All bands/Close issue NO USB.
- **Expected UI result:** Successful final snapshot and return to **Verified current hardware**.
- **Expected physical evidence:** Timestamped selected EQ state and final production EQ observation. Compare each band, slot, and displayed Playback gain to its matching baseline field.
- **Abort:** EQ tab is absent/ambiguous, tab selection changes session or starts unexpected USB activity, failed/stale read, changed session, incomplete values, or any setting/mutation action; no manual retry.

#### Step 13 — Explicit final DEVICE read
- **UI/state:** Same returned My DAC session; the selected tab may be EQ or DEVICE, including saveable DEVICE state restored from Step 9 after the clean disconnected preflight.
- **Action:** If DEVICE is not visibly selected, tap the visible **DEVICE** tab once and verify the DEVICE content is selected. This callback only updates local selection and closes the local editor if open; it must not issue a DAC request. If DEVICE is already selected, do not tap it again. Tap **Refresh** once and wait for completed success; capture all displayed DEVICE fields and truthful optional UAC state.
- **USB effect:** NO USB for a needed DEVICE-tab selection; then READ through the production DEVICE snapshot path; existing bounded automatic read retry only.
- **Expected UI result:** **Current device state** / **Values verified from the connected DAC**.
- **Expected physical evidence:** Timestamped selected DEVICE state and final DEVICE observation compared field-to-same-field with baseline at displayed precision. An **Inconsistent channel read** is not passable.
- **Abort:** DEVICE tab is absent/ambiguous, tab selection changes session or starts unexpected USB activity, read failure, missing current indicator, changed session/value, balance inconsistency, or any need to change settings; no manual retry.

#### Step 14 — Capture final evidence and complete safely
- **UI/state:** Final reads are complete and app shows no active read/write.
- **Action:** Before creating the session directory or writing any artifact, re-run `git check-ignore -v --no-index` on a prospective file at the exact destination (for example, `.unlazy/v080-beta/evidence/c05c-<UTC-session-id>/commands.txt`) and require the repository-local exclude rule `/.unlazy/v080-beta/evidence/c05c-*/`. This check is valid before the file or directory exists; do not use a missing-path `git status` result as evidence. Then capture final PID/top activity, only the exact USB descriptor block over wireless ADB, the required bounded UI screenshots/hierarchies, UTC timestamps, commands, and artifact SHA-256 values. Do not collect broad/full logcat on a successful pass; if a failure needs diagnosis, capture only a short time-bounded package-filtered excerpt after the device is idle and within the remaining occupancy limit. Store raw evidence in the new unique `.unlazy/v080-beta/evidence/c05c-<UTC-session-id>/` directory under the verified repository-local exclude rule; never overwrite the stopped report or commit raw captures.
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

## Occupancy budget and checkpoints

Complete all repository/APK/PR/check/evidence-ignore verification and prepare the session log before the owner makes the phone available. Start the occupancy clock at the first Pixel ADB operation. Do not begin unless the full sequence is still practical within a 10-minute target. Use these internal checkpoints: exact Pixel/APK/USB identity, absent package process, clean disconnected DEVICE preflight, and single-session entry decision by minute 2; baseline EQ and DEVICE by minute 5; attached navigation plus contemporaneous PID/fingerprint capture by minute 7; final EQ/DEVICE and final PID/fingerprint capture by minute 10. The hard limit is 15 minutes. If an essential checkpoint is missed, preserve completed evidence, stop at a safe idle point, and report PARTIAL / NOT PASS. Beyond the limit, wait only for an already-active nondestructive read to settle safely; this read-only plan requires no restoration write. Do not exceed the cap to gather optional screenshots or to finish a step that is not active. JA11, EW300, and manual TalkBack are excluded.

Capture UI hierarchy/screenshot only for the My DAC identity/session state, baseline and final EQ All bands (including only the scroll needed to show all ten bands), baseline and final DEVICE values, and the away/back Library state. Use the same minimal capture method throughout; do not repeat a capture because of presentation preference, save unrelated home-screen content, or dump unrelated USB devices. Save every required PID/activity and exact USB fingerprint observation directly to the ignored session log at the start, immediately after navigation back, and after final reads; a chat/tool transcript or post-session recollection is not a substitute.

## C05-C acceptance

**Pass only if all of the following are evidenced:**

- Exact frozen candidate and APK verified before DAC access; Pixel is controlled through wireless ADB only; PR/check state is still valid.
- Android USB descriptors match the exact known Black Pearl fingerprint where exposed. At session entry the target package process was absent; after one ordinary launch, DEVICE content was composed while the fresh transport was visibly Disconnected and the screen settled with no restore/recovery notice, error, or active work. Only then may the app identify TRN Black Pearl with a current Connected session established through the single agent Connect and, only if shown, one exact permission approval under the owner's current authorization. Already-running/current-session and connected-device-card routes are excluded. No physical attachment/cable change or app Connect retry is used.
- Baseline EQ and DEVICE reads succeed through the production paths and are completely recorded; optional UAC absence is represented truthfully.
- EQ is visibly selected before each baseline/final Edit EQ read, and DEVICE is visibly selected before each baseline/final Refresh. Tab selection is local-only and issues no DAC request; the evidence records the selected tab and confirms the current session remained unchanged.
- My DAC → EQ Library → My DAC navigation occurs while attached; the observed USB identity and PID remain the same, and production UI reports a current session at the recorded checkpoints. The app does not expose internal session generation, so no stronger continuity claim is made between reads.
- Final explicit EQ and DEVICE reads succeed on the same current session; every required displayed field matches its corresponding baseline field at the UI's presentation precision. An **Inconsistent channel read** balance state is not passable. This comparison does not claim bit-for-bit raw protocol equality where the UI rounds or coarsens values.
- No state-changing control/API path, Reset, Flash, Save, Apply, DEVICE setter, induced fault, process kill, or disconnect was used. All required PID/activity, USB fingerprint, screen, read, and timing evidence is saved contemporaneously and the complete file set is hash-indexed. The conclusion is bounded: no state-changing control/API path was invoked, and the required displayed values remained equal across captured production reads at the UI's presentation precision. This does not claim raw bit-for-bit equality or zero USB transfer count.

**Stop / not pass** on any candidate/APK mismatch, absent fresh owner availability, wireless ADB ambiguity, Black Pearl not already physically attached, unknown USB identity, package process present at entry, any restore/recovery notice during disconnected preflight, failure to show DEVICE selected and clearly Disconnected before Connect, any busy/error/uncertain preflight, failed single agent launch/Connect/exact permission step, failed/incomplete read, changed session/PID, stale or false presentation, chooser/choice surface, changed value, unexpected setting mutation, `UNKNOWN` USB effect, or other unclear state. Preserve the evidence, do not retry manually or attempt repair/restoration, release the phone, and continue Mac-side diagnosis. No physical attach/cable change, force-stop, Connected-card entry, existing-session reuse, or alternate/repeated app connection flow is allowed.

## Maintained references

- Master execution contract: `docs/implementation/v0.8.0-beta-master-directive.md`
- Product/design baseline: `docs/implementation/v0.8.0-beta-mission.md` §48
- Black Pearl physical procedure background: `docs/BLACK_PEARL_V0.6_RESTORE_DEFAULTS_HANDS_ON_CHECKLIST.md` (identity/read and stop rules only; reset/write checklist is out of scope)
- Canonical status: `docs/implementation/v0.8.0-beta-autonomy-status.md`
- Acceptance evidence: `docs/implementation/v0.8.0-beta-acceptance-evidence.md`
- Watchdog issue: https://github.com/weekssa/OPRA-EQ-for-UAPP/issues/71
