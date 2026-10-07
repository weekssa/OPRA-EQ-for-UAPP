# v0.8.0 Beta — Final Physical Session Plan

**Plan revision:** 2.18
**Disposition:** G3 re-review is pending. Revision 2.11 had independent PASS only for the cold, disconnected route. Revisions 2.12–2.13 add the phase barrier and source-grounded UI route. Revision 2.14 fixed the first recorder gaps; revision 2.15 tightened APK identity, PID error handling, USB-output consumption, and screenshot inspection. Revision 2.16 added wireless endpoint discovery; review found its mDNS endpoint was not attributable to the exact Pixel before connection. Revision 2.17 requires a current ADB service instance with the exact recorded Pixel serial prefix before using its advertised endpoint, handles an already-online matching serial without reconnecting, captures complete scrollable DEVICE evidence, preserves fingerprint-command errors, and prepares a bounded failure-only logcat command. Revision 2.18 makes T0 one-shot and refuses to initialize a reused session reservation with a nonempty command log or prior command outputs, screenshots, or manifest. Stop if the documented serial-prefix convention is absent, duplicated, or ambiguous. Do not use a phone until Phase A is complete, the reviewer passes this revision, and the owner makes the Pixel available after readiness. C05-C remains PARTIAL / NOT PASS until one complete compliant run is captured and reconciled.
**Master directive:** `docs/implementation/v0.8.0-beta-master-directive.md`, version 1.3, body SHA-256 `084bca0df0103701bbed74831152074c6f1283d376a8a1930e51ea39016c52ed`; introduction commit is `e57eb6ee92155d484cdc20c424546a70672b77e6`.
**Review basis:** exact frozen app/test candidate; normal-session EQ/DEVICE source-path audits; navigation/session lifecycle audit; independent review of revision 2.11; reconciled discovery-only workflow stop; and independent NOT PASS reviews of revisions 2.13 and 2.16. Revisions 2.7–2.10 were not approved because of actor/authorization mismatch, missing My DAC route, saveable-tab ambiguity, and a conditional setter that could resume when DEVICE content is composed. Revision 2.11 independently passed only for a fresh process and disconnected DEVICE composition before one normal Connect. Revisions 2.12–2.14 preserve that safety boundary, name exact UI actions, separate off-phone work from occupancy, and close prior recorder findings. Revisions 2.15–2.18 address candidate/APK checks, bounded PID handling, screenshot inspection, mDNS identity, complete DEVICE screenshots, failure evidence, one-shot T0, and single-use evidence reservations without changing production/test code or the frozen app/test candidate at HEAD/tree `5c7d19cf2f77e448a97a1ee9d520ad4d408f120f` / `e11669f0d67f9fb3c3762ea814f16fb7c4bdab8d`.

## Purpose and current boundary

This is one short, read-only Black Pearl C05-C qualification. It checks exact device identity, the active production session, production EQ and DEVICE state reads, attached navigation away and back, truthful current-session presentation, and matching final explicit reads. Here, read-only means that no DAC setting-changing path is invoked: production READs transmit an outbound report and may be automatically reissued once after timeout. The app may persist app-local metadata or saveable UI state during ordinary use; the plan claims only that it writes no screenshot/XML capture file to the Pixel and invokes no DAC setting write. There are two non-overlapping phases:

- **Phase A — off-phone preflight.** Complete candidate/GitHub/PR/check/base/APK/provenance/source/UI/procedure/evidence-ignore/logcat/ADB/abort/release preparation before requesting the Pixel. No Pixel occupancy timer runs and the owner may use the phone normally. The only Phase A terminal states are `PREFLIGHT COMPLETE — READY FOR PHONE` or `PREFLIGHT BLOCKED — PHONE NOT REQUIRED`.
- **Phase B — phone execution.** Begin only after Phase A is complete, this exact procedure is independently approved, and the owner freshly makes the Pixel available. The owner attaches the Black Pearl to USB-C before handing over the Pixel if it is not already attached; the Pixel USB-C port remains reserved for the DAC. The occupancy timer starts at T0 immediately before the first prepared wireless-ADB discovery/connection command. No GitHub, build, source research, or other off-phone preflight remains. At T0+2 minutes, the exact Pixel/APK/DAC identity, absent pre-launch package process, and clean disconnected DEVICE checkpoint must be complete; otherwise stop and release without extending or backfilling the timer.

The app process must be absent at entry. Never force-stop an existing app or reuse an already-current session. Launch once through the ordinary `MAIN`/`LAUNCHER` path, never `ACTION_USB_DEVICE_ATTACHED`. If the app is on a root screen, the only permitted entry is the single exact TRN Black Pearl context card with status **Device detected · Open My DAC to connect** and action **Open My DAC**; if launch restored My DAC automatically, its identity must be exact and visibly Disconnected. Compose DEVICE while the fresh transport remains disconnected and require a clean five-second interval before the single normal Connect. Abort if any restore/recovery notice, busy/error state, active operation, or uncertainty appears. The connection path triggers production READ requests but no DAC setting is changed; this is not a zero-USB-traffic claim and does not create a reset fault.

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
- PR #70 and exact-head checks were refreshed at `2026-10-07T03:27Z`: open/draft, exact candidate head, base `main` at `c702ef0149b4c647446639cfcb0ab25c09159db3`, `mergeable=true`, `mergeable_state=clean`. The live check-run endpoint returns 8/8 completed-success: build `112181047518`, min-api-smoke `112181046209`, emulator-ui-test `112181045077`, CodeQL `112152334761`, Priority community `112150826284`, Catalog `112150826043`, Kotlin analysis `112150825874`, and dependency submission `112150813291`. Refresh exact check runs again immediately before the next physical session and before merge.
- Current `origin/main` at the same snapshot (`2026-10-07T03:27Z`): `79d600529621257136adb4d9e1a97e8ae8521d7e` (`Refresh automated source currentness`). Comparison from the PR base confirms four catalog/currentness paths only (`catalog/catalog.json`, `catalog/discovery/github_candidates.json`, `catalog/discovery/headphone_identity_audit.json`, and `catalog/source_health.json`), with no candidate app/test path overlap. The latest currentness refresh adds one community catalog record. Keep **BASE-DRIFT-B**; do not rebase or rebuild for this physical session. Recheck and classify any further delta during pre-merge verification. Stable/latest release was independently read as `v0.7.2` at the same snapshot.
- Required API35 evidence is closed: jobs `112171539825`, `112177030919`, and `112181045077` each passed 64/64 consecutively; final JUnit artifact `11400405767`, SHA-256 `3eb0f7ed51db453d95945cd9920ae7ec588dca22102e3920b3d850164a71d757`, has 0 failures/errors/skips. Do not rerun without candidate invalidation.
- API26 populated Library is PASS/CLOSED on repository-default Pixel 2 / API26 / x86_64 at 48 MiB. Do not reopen for this session.
- Stable/latest remains v0.7.2. No merge or publication occurs during this procedure.

## Source-grounded production read paths

The ordinary Connected UI does not show the recovery-only **Read current EQ** button. Do not open Reset or recovery UI to find it.

The production startup/root route is source-supported: ordinary `ACTION_MAIN`/`CATEGORY_LAUNCHER` has no USB-attach extra, and the saved-state default root is My EQs (`MainActivity.kt:56-59`; `EqLibraryApp.kt:306`). When exactly one supported DAC is recognized and present, `EqLibraryApp.deviceContextSummary()` supplies TRN Black Pearl with **Device detected · Open My DAC to connect**; `ConnectedDeviceSurface` exposes **Open My DAC** as a button-role card (`EqLibraryApp.kt:126-180,958-975`; `AppContextSurfaces.kt:28-78`). A multiple-device choice surface, connection-error card, connected/stale card, or missing card is not an approved initial entry route. If launch restores My DAC automatically, it is acceptable only while that exact device is visibly Disconnected, before the single normal Connect. After baseline reads, the exact away/back route is My DAC top-bar Back → root **EQ Library** navigation item → the same TRN Black Pearl card showing **Connected · State current** and **Open My DAC** → tap that card once (`EqLibraryApp.kt:869-874,958-975,1103-1106`). This reopens the saveable My DAC workspace; it does not Connect or read. If the card is absent, stale/reading/waiting/error, or would invoke Connect, stop.

The cold app process initializes the Black Pearl transport as **Disconnected**; detecting the attached device does not open it or send a DAC request, and a fresh ViewModel does not rearm the prior-session reconnect path. Use only ordinary Android `ACTION_MAIN` plus `CATEGORY_LAUNCHER`, never `ACTION_USB_DEVICE_ATTACHED` (`MainActivity.kt:56-59`; `EqLibraryApp.kt:384`). The saved-state default root is My EQs (`EqLibraryApp.kt:306`). When exactly one supported DAC is recognized and present, `deviceContextSummary()` renders the exact initial card status **Device detected · Open My DAC to connect** with title **TRN Black Pearl** and button **Open My DAC** (`EqLibraryApp.kt:126-180,958-975`; `AppContextSurfaces.kt:28-78`). Tap only that unique card; if launch restores My DAC directly, accept only the exact Black Pearl with visible **Disconnected**. Abort on a choice/error/Connected/stale/missing/ambiguous route. The reset section saves progress with `rememberSaveable`; when DEVICE is composed without a current snapshot/session, its guard clears a pending restore step and returns before its setter, with a stop message (`BlackPearlDeviceResetSection.kt:100-109,126-157,185-192`). Therefore DEVICE must be composed while Disconnected, then remain clean and idle for five seconds before the one authorized normal Connect. A normal Connect opens/claims the already attached device and may show one exact app/device permission request; after Connected, production observers issue EQ and DEVICE READs. The owner-authorized Connect is only session access, not a setting change. Do not use Reset/recovery controls, setters, or unrelated screens. After baseline reads, the exact away/back route is My DAC top-bar Back → root **EQ Library** navigation → the same TRN Black Pearl context card showing **Connected · State current** and **Open My DAC** → tap that card once (`EqLibraryApp.kt:869-874,958-975,1103-1106`). This reopens the saveable My DAC workspace; it performs no Connect or read. If the current card is absent or stale/reading/waiting/error, stop. EQ/DEVICE tab selections are local-only; only normal Edit EQ and Refresh issue explicit READs. Capture every result contemporaneously. Source references: `data/blackpearl/AndroidBlackPearlUsbTransport.kt:49,95-132,284-317`; `data/dac/DacSessionRepository.kt:37,227-250`; `ui/EqLibraryApp.kt:126-180,306,384,869-874,958-975,1103-1106`; `ui/components/AppContextSurfaces.kt:28-78`; `ui/screens/MyDacScreen.kt:100-113,118,265-285,326-344,438-448,568-574`; `ui/screens/BlackPearlDeviceResetSection.kt:100-109,126-157,185-192`; `ui/EqLibraryViewModel.kt:624-676,1247-1277`; `ui/screens/DeviceOperationStatus.kt:20-48,118-185`; and `data/dac/DacControlRepository.kt:313-362`.

- Connecting starts an EQ read from the Black Pearl connection collector (`app/src/main/java/com/weekssa/opraeqforuapp/data/hardware/HardwareEqRepository.kt:94-103`) and a DEVICE read from `EqLibraryViewModel.kt:438-446`. The EQ snapshot is accepted only while the same session remains current (`HardwareEqRepository.kt:343-371`); its production reader reads the filter bands and global gain (`HardwareEqSnapshotReaders.kt:11-35`).
- For explicit EQ reads, My DAC's normal **Edit EQ** button (`ui/screens/MyDacScreen.kt:568-574`) calls `EqLibraryViewModel.openBlackPearlEditor()` (`ui/EqLibraryViewModel.kt:624-676`), which first calls `hardwareRepository.readBlackPearlSnapshot()`. The editor states that edits are local only (`ui/screens/MyDacEqEditorScreen.kt:99-105,135-165`). **All bands** displays the observed values (`ui/screens/MyDacEqEditorScreen.kt:522-562`). **Close** clears only editor state (`ui/EqLibraryViewModel.kt:972-976`). Do not change any value or enter Safe gain, Review, Apply, Reset, or Save.
- For explicit DEVICE reads, the DEVICE tab's **Refresh** button (`ui/screens/DeviceOperationStatus.kt:131-185`, `ui/screens/BlackPearlDeviceBatchControls.kt:66-78`) calls `readBlackPearlQualificationControls()` and the read-only snapshot path (`ui/EqLibraryViewModel.kt:1247-1277`, `data/dac/DacControlRepository.kt:313-362`). It reads firmware, filter, gain mode, amplifier topology, microphone gain, both balance channels, playback gain, and optional USB audio mode. If USB audio mode is unavailable, record the displayed unavailable state; a nullable UAC mode is not by itself a read failure (`DacControlRepository.kt:340-344`, `ui/screens/BlackPearlDeviceBatchControls.kt:146-156`). Do not touch DEVICE setting rows; those invoke setters (`BlackPearlDeviceBatchControls.kt:93-143`).
- Selecting EQ Library then reopening My DAC retains the My DAC subtree in a saveable-state holder and does not call disconnect/reconnect (`ui/EqLibraryApp.kt:869-874,958-975,1103-1106`). Verify the same physical USB identity and truthful active-session status after return, then explicitly refresh both categories.
- `MyDacScreen` stores `selectedTabIndex` with `rememberSaveable` and its EQ/DEVICE tab callbacks only select local content (`ui/screens/MyDacScreen.kt:113,265-285`). The My DAC saveable-state holder preserves the selected tab across root navigation (`ui/EqLibraryApp.kt:1103-1106`). Therefore every baseline and final read step below explicitly checks and, only when needed, selects the required visible tab once before invoking the production read. Tab selection is **NO USB**; only Edit EQ or Refresh triggers the documented READ.

**Read-only terminology and observable limit:** Black Pearl READ operations transmit protocol read-request output reports (`data/blackpearl/AndroidBlackPearlUsbTransport.kt:201-232`). A timed-out nondestructive read can be automatically reissued once in the same session (500 ms timeout, maximum two request attempts; `AndroidBlackPearlUsbTransport.kt:432-436`, `BlackPearlReadRetry.kt:5-30`). This bounded transport behavior is not a manual retry or a setting write. The procedure must not be described as zero USB OUT reports or zero read-request reissues. C05-C can establish that no setting-changing UI/API path was invoked and that all required displayed values match the baseline at the UI's presentation precision; it cannot establish a zero-transfer USB trace or bit-for-bit equality of raw protocol values. EQ band and playback-gain values are rounded for display to 0.01 dB, while raw EQ gain uses 1/256 dB increments (`strings_v06.xml:39,128`; `BlackPearlReadCodec.kt:19,24`; `BlackPearlProtocol.kt:24,93`). DEVICE Volume is displayed as an integer percent and is not a unit-compatible comparison for EQ Playback gain (`BlackPearlDeviceBatchControls.kt:95-99,497-499`). Compare each field only to the same field's baseline/final displayed value; do not compare EQ Playback gain to DEVICE Volume. If DEVICE reports **Inconsistent channel read** for balance, stop and do not pass because the UI suppresses the separate raw left/right pair (`BlackPearlDeviceControlReadCodec.kt:198-204`; `BlackPearlDeviceBatchControls.kt:121-132`). A verified EQ read can initialize app-local gain-baseline metadata if absent (`BlackPearlFlasher.kt:53-56`); this is not a DAC mutation.

## Step-by-step session procedure

Each step specifies the expected UI/state, exact action, USB effect, expected UI result, evidence claim, and abort condition. `NO USB` means that the step issues no DAC protocol request; all ADB commands use wireless ADB only. `READ` is a production protocol read request and may send an outbound HID report; the existing transport may reissue one nondestructive request automatically after timeout. Neither label establishes zero physical USB traffic. The Black Pearl must already be physically attached before Phase B; do not change cables during Phase B. Only when the target package process is absent may the agent launch once, compose DEVICE while disconnected, then use one normal Connect and at most one exact permission approval in Steps 5a–5d. The disconnected context card is allowed only for initial entry; the current Connected card is used only for the planned post-baseline away/back navigation. No force-stop, current-session reuse, cable change, repeated Connect, or additional approval is authorized.

### A. Phase A — off-phone preflight (complete before requesting the Pixel)

#### Step 1 — Verify the frozen local candidate and APK
- **UI/state:** No device session; tracked candidate files remain read-only. Local Git exclude metadata may be verified for raw evidence handling.
- **Action:** Verify candidate branch/HEAD/tree, app/main/test/androidTest/app trees, local APK SHA-256, package/version/code, and signer against the pinned identity above. Before any phone or ADB access, choose a unique UTC session ID and verify a prospective file under `.unlazy/v080-beta/evidence/c05c-<UTC-session-id>/` is ignored in this exact candidate checkout. Require `git check-ignore -v --no-index` to identify the repository-local `/.unlazy/v080-beta/evidence/c05c-*/` rule in the file resolved by `git rev-parse --git-path info/exclude`; if absent, add only that local Git exclude rule and verify again. Then create the unique ignored directory, `preflight.txt`, and the empty `commands.log`; write the candidate/APK identities and expected files to `preflight.txt`. Prepare and hash the Phase B recorder described below. This changes only ignored local artifacts and Git exclude metadata, not the tracked candidate/tree. Do not access the Pixel.
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

### Phase A command preparation — prepare and syntax-check now; run no ADB before Phase B

Before the owner makes the phone available, initialize the prepared Bash session below with a unique UTC `SESSION_ID` and ignored `SESSION_DIR`; the path must not already exist. Positively verify the prospective path is ignored in the exact candidate checkout, then create only the empty Mac-side `commands.log` plus `preflight.txt`. Never reuse or overwrite an earlier session reservation, including `c05c-20261007T025706Z` or `c05c-20261007T031303Z`; every retry gets a new unique ID. Save this first block as `phase-b-recorder.sh` in the new ignored session directory, syntax-check it with `/bin/bash -n` without executing any device command, and record its SHA-256 in `preflight.txt`. During Phase A, open one interactive Bash session rooted at the candidate checkout, source this helper, pass its local Git/path guards, and leave its functions initialized and idle. Sourcing the helper executes no ADB command. Its guards require an empty `commands.log` and reject any pre-existing command outputs, screenshots, manifest, or other evidence in the reserved directory; only `preflight.txt`, `commands.log`, and the prepared helper may exist. The `run t0-discovery` label is accepted only while both T0 fields are empty; a later attempt is refused without changing the original timer and requires a new session ID. Do not source it or run any preparation after the owner makes the Pixel available. After fresh availability, call `run t0-discovery ./tools/codex-android adb devices -l` as the first ADB invocation; it records UTC and monotonic T0 immediately before spawning that process. The wrapper stores each text command, UTC timestamp, exit code, and text output in `commands.log`; screenshots are uniquely named host PNGs with SHA-256 recorded there. After each `capture_ui`, print `LAST_CAPTURE`, inspect that exact PNG with the Codex local image viewer, and only then call `ack_capture_inspected "$LAST_CAPTURE"`; tap/swipe helpers refuse an image until that acknowledgement is recorded. Keep the same shell open for later prepared blocks so initialized variables/functions and `LAST_OUTPUT` remain available. Never run ADB as a preparation or syntax check; inspect each fresh screenshot before the next already-planned action.

```bash
set -euo pipefail
REPO_ROOT="$(git rev-parse --show-toplevel)"
EXPECTED_BRANCH='codex/v0.8.0-beta-ux'
EXPECTED_HEAD='5c7d19cf2f77e448a97a1ee9d520ad4d408f120f'
EXPECTED_TREE='e11669f0d67f9fb3c3762ea814f16fb7c4bdab8d'
EXPECTED_PIXEL_SERIAL='46141FDAQ003KZ'
PACKAGE='com.weekssa.opraeqforuapp'
EXPECTED_APK_SHA='f33818309d55571166d91e501065706ddbb8faf180833b03bb6eb948e6bffed1'
SESSION_ID='20261007T031528Z'
SESSION_DIR="$REPO_ROOT/.unlazy/v080-beta/evidence/c05c-$SESSION_ID"
COMMAND_LOG="$SESSION_DIR/commands.log"
PIXEL=''
PIXEL_ENDPOINT=''
PIXEL_SERVICE_INSTANCE=''
INITIAL_DISCOVERY=''
SELECT_RC=0
INSTALLED_BASE_APK=''
PID=''
STEP=''
X=''
Y=''
RUN_NO=0
CAPTURE_NO=0
LAST_OUTPUT=''
LAST_EXIT_STATUS=''
LAST_CAPTURE=''
CAPTURE_INSPECTED=''
FAILURE_IDLE_INSPECTED=''
T0_UTC=''
T0_MONOTONIC_NS=''
ELAPSED_SECONDS=''

[[ "$(git branch --show-current)" == "$EXPECTED_BRANCH" ]]
[[ "$(git rev-parse HEAD)" == "$EXPECTED_HEAD" ]]
[[ "$(git rev-parse HEAD^{tree})" == "$EXPECTED_TREE" ]]
[[ "$SESSION_ID" =~ ^[0-9]{8}T[0-9]{6}Z$ ]]
[[ -d "$SESSION_DIR" && -f "$SESSION_DIR/preflight.txt" && -f "$COMMAND_LOG" ]]
[[ -f "$SESSION_DIR/phase-b-recorder.sh" ]]
[[ ! -s "$COMMAND_LOG" ]] || { printf 'ABORT: refusing to initialize a session whose command log is already non-empty; reserve a new SESSION_ID\n' >&2; return 1; }
if find "$SESSION_DIR" -mindepth 1 -maxdepth 1 \
    ! -name 'preflight.txt' ! -name 'commands.log' ! -name 'phase-b-recorder.sh' \
    -print -quit | grep -q .; then
  printf 'ABORT: session reservation already contains command output, screenshots, manifest, or other prior evidence; reserve a new SESSION_ID\n' >&2
  return 1
fi
git check-ignore -q --no-index "$SESSION_DIR/commands.log"

utc_now() { date -u '+%Y-%m-%dT%H:%M:%SZ'; }
abort_session() {
  printf 'ABORT: %s\n' "$*" >&2
  exit 1
}
assert_no_pixel_usb_transport() {
  if printf '%s\n' "$LAST_OUTPUT" | awk -v serial="$EXPECTED_PIXEL_SERIAL" \
      '$1 == serial && $0 ~ /usb:/ { found=1 } END { exit !found }'; then
    printf 'ABORT: Pixel USB ADB transport is listed; wireless-only procedure cannot continue\n' >&2
    return 1
  fi
  return 0
}
select_preconnected_pixel_service() {
  local rows instance state
  rows="$(printf '%s\n' "$1" | awk -v prefix="adb-${EXPECTED_PIXEL_SERIAL}-" \
      'index($1, prefix)==1 && $1 ~ /[.]_adb-tls-connect[.]_tcp$/ { print $1 "\t" $2 }')"
  [[ -n "$rows" ]] || return 1
  if [[ "$rows" == *$'\n'* || "$rows" != *$'\t'device ]]; then
    printf 'ABORT: Pixel serial-prefixed ADB Wi-Fi row is ambiguous or not online\n' >&2
    return 2
  fi
  instance="${rows%%$'\t'*}"
  state="${rows#*$'\t'}"
  [[ "$state" == 'device' ]] || return 2
  PIXEL="$instance"
  PIXEL_SERVICE_INSTANCE="${instance%%._adb-tls-connect._tcp}"
  return 0
}
select_pixel_mdns_service() {
  local parsed prefix
  prefix="adb-${EXPECTED_PIXEL_SERIAL}-"
  if ! parsed="$(printf '%s\n' "$LAST_OUTPUT" | awk -v prefix="$prefix" '
      $2 == "_adb-tls-connect._tcp" && index($1, prefix)==1 && length($1)>length(prefix) {
        matches++
        instance=$1
        if ($3 ~ /^[0-9]+\.[0-9]+\.[0-9]+\.[0-9]+:[0-9]+$/) endpoint=$3
        else bad=1
      }
      END { if (bad || matches != 1) exit 1; print instance "\t" endpoint }
    ')"; then
    printf 'ABORT: current mDNS output did not contain exactly one serial-attributable Pixel connect service with a valid IPv4 endpoint\n' >&2
    return 1
  fi
  PIXEL_SERVICE_INSTANCE="${parsed%%$'\t'*}"
  PIXEL_ENDPOINT="${parsed#*$'\t'}"
  printf '[%s] PRECONNECT_MDNS_IDENTITY serial=%s service_instance=%s endpoint=%s basis=AOSP_serial_prefix_convention; stop if absent_or_ambiguous\n' \
    "$(utc_now)" "$EXPECTED_PIXEL_SERIAL" "$PIXEL_SERVICE_INSTANCE" "$PIXEL_ENDPOINT" | tee -a "$COMMAND_LOG"
}
select_online_pixel_alias() {
  local service_serial rows selected
  service_serial="${PIXEL_SERVICE_INSTANCE}._adb-tls-connect._tcp"
  rows="$(printf '%s\n' "$1" | awk -v endpoint="$PIXEL_ENDPOINT" -v service="$service_serial" \
      '$1 == endpoint || $1 == service { print $1 "\t" $2 }')"
  [[ -n "$rows" ]] || return 1
  if [[ "$rows" == *$'\n'* || "$rows" != *$'\t'device ]]; then
    printf 'ABORT: the current Pixel mDNS service did not resolve to exactly one online ADB alias\n' >&2
    return 2
  fi
  selected="${rows%%$'\t'*}"
  PIXEL="$selected"
  return 0
}
run() {
  local label="$1" out cmd rc
  shift
  if [[ "$label" == 't0-discovery' && ( -n "$T0_UTC" || -n "$T0_MONOTONIC_NS" ) ]]; then
    printf '[%s] REFUSED reason=t0-already-started; a new physical session requires a new SESSION_ID\n' \
      "$(utc_now)" | tee -a "$COMMAND_LOG" >&2
    return 64
  fi
  RUN_NO=$((RUN_NO + 1))
  out="$SESSION_DIR/.command-$RUN_NO.out"
  [[ ! -e "$out" ]] || { printf 'ABORT: refusing to overwrite %s\n' "$out" >&2; return 73; }
  printf -v cmd '%q ' "$@"
  printf '\n[%s] BEGIN %s COMMAND=%s\n' "$(utc_now)" "$label" "$cmd" | tee -a "$COMMAND_LOG"
  if [[ "$label" == 't0-discovery' ]]; then
    T0_UTC="$(utc_now)"
    T0_MONOTONIC_NS="$(python3 -c 'import time; print(time.monotonic_ns())')"
    printf 'T0_UTC=%s T0_MONOTONIC_NS=%s\n' "$T0_UTC" "$T0_MONOTONIC_NS" | tee -a "$COMMAND_LOG"
  fi
  if "$@" >"$out" 2>&1; then rc=0; else rc=$?; fi
  cat "$out" | tee -a "$COMMAND_LOG"
  LAST_OUTPUT="$(tr -d '\r' <"$out")"
  rm -f "$out"
  LAST_EXIT_STATUS="$rc"
  printf '[%s] EXIT %s STATUS=%s\n' "$(utc_now)" "$label" "$rc" | tee -a "$COMMAND_LOG"
  return "$rc"
}

record_observation() {
  local label="$1" field
  shift
  [[ "$label" =~ ^(baseline|final)-device-(top|scroll-[1-3])$ && "$#" -gt 0 ]] || {
    printf 'ABORT: invalid DEVICE observation label or empty observation\n' >&2
    return 64
  }
  for field in "$@"; do
    [[ "$field" == *=* ]] || { printf 'ABORT: DEVICE observations must be field=value pairs\n' >&2; return 64; }
  done
  printf '[%s] OBSERVATION %s' "$(utc_now)" "$label" | tee -a "$COMMAND_LOG"
  printf ' %s' "$@" | tee -a "$COMMAND_LOG"
  printf '\n' | tee -a "$COMMAND_LOG"
}

assert_t0_elapsed_at_most() {
  local limit="$1" now_ns elapsed_ns
  [[ "$T0_MONOTONIC_NS" =~ ^[0-9]+$ && "$limit" =~ ^[0-9]+$ ]] || return 64
  now_ns="$(python3 -c 'import time; print(time.monotonic_ns())')"
  elapsed_ns=$((now_ns - T0_MONOTONIC_NS))
  ELAPSED_SECONDS=$(( (elapsed_ns + 999999999) / 1000000000 ))
  printf '[%s] ELAPSED_CHECK elapsed_seconds=%s limit_seconds=%s\n' \
    "$(utc_now)" "$ELAPSED_SECONDS" "$limit" | tee -a "$COMMAND_LOG"
  if (( ELAPSED_SECONDS > limit )); then
    printf '[%s] ABORT reason=entry-checkpoint-exceeded elapsed_seconds=%s limit_seconds=%s\n' \
      "$(utc_now)" "$ELAPSED_SECONDS" "$limit" | tee -a "$COMMAND_LOG"
    return 1
  fi
}

capture_ui() {
  local label="$1" stamp path
  case "$label" in
    entry|device-preconnect-start|device-preconnect-end|connect-action|permission-prompt|connected|baseline-eq|baseline-eq-all-bands|baseline-eq-all-bands-bottom|baseline-device-top|baseline-device-scroll-[1-3]|baseline-device-return-top-[1-3]|library-root|library-card|returned-session|final-eq|final-eq-all-bands|final-eq-all-bands-bottom|final-device-top|final-device-scroll-[1-3]|final-device-return-top-[1-3]|final-session|failure-idle) ;;
    *) printf 'ABORT: unknown capture label %s\n' "$label" >&2; return 64 ;;
  esac
  CAPTURE_NO=$((CAPTURE_NO + 1))
  stamp="$(date -u '+%Y%m%dT%H%M%SZ')"
  path="$SESSION_DIR/$(printf '%02d' "$CAPTURE_NO")-$label-$stamp.png"
    [[ ! -e "$path" ]] || { printf 'ABORT: refusing to overwrite %s\n' "$path" >&2; return 73; }
  printf '[%s] SCREENSHOT label=%s path=%s command=./tools/codex-android adb -s %q exec-out screencap -p\n' \
    "$(utc_now)" "$label" "$path" "$PIXEL" | tee -a "$COMMAND_LOG"
  if ./tools/codex-android adb -s "$PIXEL" exec-out screencap -p >"$path" 2>>"$COMMAND_LOG"; then
    [[ -s "$path" ]]
    shasum -a 256 "$path" | tee -a "$COMMAND_LOG"
    printf '[%s] EXIT screenshot-%s STATUS=0\n' "$(utc_now)" "$label" | tee -a "$COMMAND_LOG"
    LAST_CAPTURE="$path"
    CAPTURE_INSPECTED=''
  else
    printf '[%s] SCREENSHOT_FAILED label=%s STATUS=1\n' "$(utc_now)" "$label" | tee -a "$COMMAND_LOG"
    return 1
  fi
}

ack_capture_inspected() {
  local path="$1"
  [[ -n "$LAST_CAPTURE" && "$path" == "$LAST_CAPTURE" && -s "$path" ]] || {
    printf 'ABORT: screenshot acknowledgement does not match the latest capture\n' >&2
    return 64
  }
  CAPTURE_INSPECTED="$path"
  printf '[%s] SCREENSHOT_INSPECTED path=%s\n' "$(utc_now)" "$path" | tee -a "$COMMAND_LOG"
}

ack_failure_idle_capture() {
  local path="$1"
  [[ -n "$LAST_CAPTURE" && "$path" == "$LAST_CAPTURE" && "$path" == "$CAPTURE_INSPECTED" && -s "$path" ]] || {
    printf 'ABORT: failure diagnostics require the latest screenshot to be visually inspected\n' >&2
    return 64
  }
  FAILURE_IDLE_INSPECTED="$path"
  printf '[%s] FAILURE_DIAGNOSTIC_IDLE_CONFIRMED screenshot=%s\n' "$(utc_now)" "$path" | tee -a "$COMMAND_LOG"
}

capture_failure_logcat() {
  [[ "$PID" =~ ^[0-9]+$ && -n "$PIXEL" && -n "$FAILURE_IDLE_INSPECTED" && \
     "$FAILURE_IDLE_INSPECTED" == "$LAST_CAPTURE" && "$CAPTURE_INSPECTED" == "$LAST_CAPTURE" ]] || {
    printf 'ABORT: failure logcat requires the exact app PID and a freshly inspected idle-state screenshot\n' >&2
    return 64
  }
  run failure-app-logcat ./tools/codex-android adb -s "$PIXEL" logcat -d -t 200 "--pid=$PID"
  FAILURE_IDLE_INSPECTED=''
}

tap_from_capture() {
  local label="$1" x="$2" y="$3" source
  [[ -n "$LAST_CAPTURE" && "$LAST_CAPTURE" == "$CAPTURE_INSPECTED" && -s "$LAST_CAPTURE" && "$x" =~ ^[0-9]+$ && "$y" =~ ^[0-9]+$ ]]
  source="$LAST_CAPTURE"
  X="$x"; Y="$y"
  printf '[%s] UI_TAP source_screenshot=%s x=%s y=%s\n' "$(utc_now)" "$source" "$X" "$Y" | tee -a "$COMMAND_LOG"
  run "tap-$label" ./tools/codex-android adb -s "$PIXEL" shell input tap "$X" "$Y"
  LAST_CAPTURE=''
  CAPTURE_INSPECTED=''
}

swipe_from_capture() {
  local label="$1" x1="$2" y1="$3" x2="$4" y2="$5" ms="$6" source
  [[ -n "$LAST_CAPTURE" && "$LAST_CAPTURE" == "$CAPTURE_INSPECTED" && -s "$LAST_CAPTURE" && "$x1" =~ ^[0-9]+$ && "$y1" =~ ^[0-9]+$ && "$x2" =~ ^[0-9]+$ && "$y2" =~ ^[0-9]+$ && "$ms" =~ ^[0-9]+$ ]]
  source="$LAST_CAPTURE"
  printf '[%s] UI_SWIPE source_screenshot=%s from=%s,%s to=%s,%s duration_ms=%s\n' \
    "$(utc_now)" "$source" "$x1" "$y1" "$x2" "$y2" "$ms" | tee -a "$COMMAND_LOG"
  run "swipe-$label" ./tools/codex-android adb -s "$PIXEL" shell input swipe "$x1" "$y1" "$x2" "$y2" "$ms"
  LAST_CAPTURE=''
  CAPTURE_INSPECTED=''
}

record_usb_fingerprint() {
  local label="$1" out raw err rc
  out="$SESSION_DIR/.usb-$label.out"
  raw="$SESSION_DIR/.usb-$label.raw"
  err="$SESSION_DIR/.usb-$label.err"
  [[ ! -e "$out" && ! -e "$raw" && ! -e "$err" ]] || { printf 'ABORT: refusing to overwrite USB fingerprint evidence for %s\n' "$label" >&2; return 73; }
  printf '[%s] BEGIN usb-fingerprint-%s COMMAND=wireless adb dumpsys usb | host filter for exact Black Pearl fingerprint fields\n' \
    "$(utc_now)" "$label" | tee -a "$COMMAND_LOG"
  if ./tools/codex-android adb -s "$PIXEL" shell dumpsys usb >"$raw" 2>"$err"; then rc=0; else rc=$?; fi
  if (( rc != 0 )); then
    printf '[%s] USB_DUMPSYS_COMMAND_FAILED STATUS=%s\n' "$(utc_now)" "$rc" | tee -a "$COMMAND_LOG"
    if [[ -s "$err" ]]; then
      sed -n '1,12p' "$err" | tee -a "$COMMAND_LOG"
    fi
    rm -f "$raw" "$err"
    LAST_OUTPUT=''
    printf '[%s] EXIT usb-fingerprint-%s STATUS=%s\n' "$(utc_now)" "$label" "$rc" | tee -a "$COMMAND_LOG"
    return "$rc"
  fi
  if awk '
        function inspect_record(    t, a, b, c, d, e) {
          if (record == "") return
          t = record
          a = gsub(/vendor_id=13058/, "&", t)
          t = record; b = gsub(/product_id=17384/, "&", t)
          t = record; c = gsub(/manufacturer_name=TTGK Technology/, "&", t)
          t = record; d = gsub(/product_name=TE-C/, "&", t)
          t = record; e = gsub(/serial_number=330243E8260129/, "&", t)
          if (a+b+c+d+e > 0) {
            if (a!=1 || b!=1 || c!=1 || d!=1 || e!=1) bad=1
            matches++
            selected=record
          }
        }
        /host_manager=/ { host=1 }
        host && /devices=\{/ { in_devices=1; depth=1; next }
        in_devices {
          if (depth==1 && /^[[:space:]]*name=/) { inspect_record(); record=$0 }
          else if (record!="") record=record ORS $0
          opens=gsub(/\{/, "&", $0); closes=gsub(/\}/, "&", $0)
          depth+=opens-closes
          if (depth==0) { inspect_record(); in_devices=0 }
        }
        END {
          if (bad || matches!=1) exit 1
          n=split(selected, lines, ORS)
          for (i=1; i<=n; i++) if (lines[i] ~ /^( *name=\/dev\/bus\/| *vendor_id=| *product_id=| *manufacturer_name=| *product_name=| *serial_number=)/) print lines[i]
        }' "$raw" >"$out"; then rc=0; else rc=$?; fi
  if (( rc != 0 )); then
    printf '[%s] USB_FINGERPRINT_ABORT status=%s reason=expected_exactly_one_complete_Black_Pearl_descriptor_record\n' \
      "$(utc_now)" "$rc" | tee -a "$COMMAND_LOG"
    awk 'tolower($0) ~ /(error|exception|fail|not found|permission denied|cannot|unable)/ { print; n++; if (n >= 8) exit }' \
      "$raw" 2>/dev/null | tee -a "$COMMAND_LOG" || :
  fi
  rm -f "$raw" "$err"
  cat "$out" | tee -a "$COMMAND_LOG"
  LAST_OUTPUT="$(cat "$out")"
  rm -f "$out"
  printf '[%s] EXIT usb-fingerprint-%s STATUS=%s\n' "$(utc_now)" "$label" "$rc" | tee -a "$COMMAND_LOG"
  [[ "$rc" == 0 ]]
}

record_checkpoint() {
  local label="$1" expected="$2" actual activity
  if run "checkpoint-$label-pid" ./tools/codex-android adb -s "$PIXEL" shell pidof "$PACKAGE"; then
    actual="$(printf '%s' "$LAST_OUTPUT" | tr -d '\r\n')"
  else
    if [[ "$LAST_EXIT_STATUS" == 1 && -z "$LAST_OUTPUT" ]]; then
      actual=''
    else
      printf 'ABORT: pidof failed for a reason other than an empty no-process result at %s\n' "$label" >&2
      return 1
    fi
  fi
  printf '[%s] CHECKPOINT label=%s expected_pid=%s observed_pid=%s\n' "$(utc_now)" "$label" "$expected" "${actual:-ABSENT}" | tee -a "$COMMAND_LOG"
  if [[ "$expected" == 'ABSENT' ]]; then
    [[ -z "$actual" ]] || { printf 'ABORT: package process unexpectedly present at %s\n' "$label" >&2; return 1; }
  else
    [[ "$actual" == "$expected" ]] || { printf 'ABORT: PID changed at %s\n' "$label" >&2; return 1; }
  fi
  run "checkpoint-$label-top-activity" ./tools/codex-android adb -s "$PIXEL" shell sh -c 'data=$(dumpsys activity activities) || exit 70; printf "%s\n" "$data" | grep -E "mResumedActivity|topResumedActivity" | grep -F "com.weekssa.opraeqforuapp" || :'
  activity="$LAST_OUTPUT"
  if [[ "$expected" == 'ABSENT' ]]; then
    [[ -z "$activity" ]] || { printf 'ABORT: EQ Library activity unexpectedly resumed at %s\n' "$label" >&2; return 1; }
  else
    [[ "$activity" == *"$PACKAGE"* ]] || { printf 'ABORT: EQ Library is not the resumed activity at %s\n' "$label" >&2; return 1; }
  fi
  record_usb_fingerprint "$label"
  PID="$actual"
}

finalize_evidence_manifest() {
  local temp="$SESSION_DIR/.SHA256SUMS.tmp"
  [[ ! -e "$SESSION_DIR/SHA256SUMS" && ! -e "$temp" ]] || {
    printf 'ABORT: refusing to overwrite the evidence manifest\n' >&2
    return 73
  }
  printf '[%s] BEGIN evidence-manifest files=all-session-files-except-manifest\n' \
    "$(utc_now)" | tee -a "$COMMAND_LOG"
  (
    cd "$SESSION_DIR"
    find . -type f ! -name 'SHA256SUMS' ! -name '.SHA256SUMS.tmp' -print \
      | LC_ALL=C sort \
      | while IFS= read -r path; do shasum -a 256 "$path" || exit; done
  ) >"$temp"
  [[ -s "$temp" ]]
  mv "$temp" "$SESSION_DIR/SHA256SUMS"
  printf 'EVIDENCE_MANIFEST_SHA256='
  shasum -a 256 "$SESSION_DIR/SHA256SUMS"
}

```

After `run t0-discovery`, immediately save `INITIAL_DISCOVERY="$LAST_OUTPUT"` and call `assert_no_pixel_usb_transport`. If it finds the known Pixel serial on a USB ADB row, stop and release. First call `select_preconnected_pixel_service "$INITIAL_DISCOVERY"`. This accepts only one online ADB Wi-Fi serial of the form `adb-46141FDAQ003KZ-<suffix>._adb-tls-connect._tcp`; if accepted, store that exact ADB serial in `PIXEL` and do not connect. A listed matching service row that is offline/unauthorized or duplicated is an abort. If no such serial row is present, call the single current mDNS discovery below. Its output must contain exactly one `_adb-tls-connect._tcp` service whose instance begins exactly `adb-46141FDAQ003KZ-` with a nonempty suffix and whose IPv4 endpoint is `address:port`; `select_pixel_mdns_service` enforces that mapping before any `adb connect`. The AOSP implementation says the connect-service instance prefix is *usually* `adb-` plus `ro.serialno` plus a backend suffix; this plan uses that documented convention as a bounded pre-connect identity rule and aborts if it is absent, duplicated, malformed, or otherwise ambiguous. The selected service maps to either an already-online service-serial row or the exact endpoint row in `INITIAL_DISCOVERY`. Use that one online alias if present. If neither is online, call `adb connect` once with the current mDNS endpoint, refresh `adb devices -l`, and require exactly one online alias for the mapped service. No old host/port, pairing service, pairing command, or Pixel USB ADB is allowed. The ADB serial is verified against the exact Pixel serial/model/product before further device operations.

```bash
INITIAL_DISCOVERY="$LAST_OUTPUT"
assert_no_pixel_usb_transport || abort_session 'Pixel USB ADB transport is listed'
if select_preconnected_pixel_service "$INITIAL_DISCOVERY"; then
  printf '[%s] PRECONNECTED_PIXEL_SERVICE serial=%s action=no_connect\n' "$(utc_now)" "$PIXEL" | tee -a "$COMMAND_LOG"
else
  SELECT_RC=$?
  [[ "$SELECT_RC" == 1 ]] || abort_session 'ambiguous preconnected Pixel service row'
  run mdns-discovery ./tools/codex-android adb mdns services
  select_pixel_mdns_service || abort_session 'current mDNS services do not identify exactly one Pixel connect endpoint'
  if select_online_pixel_alias "$INITIAL_DISCOVERY"; then
    printf '[%s] PRECONNECTED_PIXEL_ALIAS serial=%s endpoint=%s action=no_connect\n' \
      "$(utc_now)" "$PIXEL" "$PIXEL_ENDPOINT" | tee -a "$COMMAND_LOG"
  else
    SELECT_RC=$?
    [[ "$SELECT_RC" == 1 ]] || abort_session 'ambiguous or non-device Pixel ADB alias'
    run mdns-connect ./tools/codex-android adb connect "$PIXEL_ENDPOINT"
    [[ "$LAST_OUTPUT" == *"connected to $PIXEL_ENDPOINT"* || "$LAST_OUTPUT" == *"already connected to $PIXEL_ENDPOINT"* ]] || abort_session 'current mDNS endpoint did not accept the one allowed connect'
    run connected-discovery ./tools/codex-android adb devices -l
    assert_no_pixel_usb_transport || abort_session 'Pixel USB ADB transport appeared after wireless connect'
    select_online_pixel_alias "$LAST_OUTPUT" || abort_session 'refreshed discovery did not contain exactly one online alias for the mapped Pixel'
  fi
fi
[[ -n "$PIXEL" ]] || abort_session 'no exact Pixel ADB target was selected'
```

For either branch, the later `ro.serialno`, `ro.product.model`, and `ro.product.device` checks must match the exact recorded Pixel 9 before package or app operations. The accepted `PIXEL` value is either the one exact serial-qualified ADB Wi-Fi service serial or the one current mDNS-mapped endpoint alias; it is not an arbitrary host:port. Continue with:

```bash
[[ -n "$PIXEL" && "$PIXEL" != emulator-* ]]
run selected-endpoint-state ./tools/codex-android adb -s "$PIXEL" get-state
[[ "$LAST_OUTPUT" == 'device' ]]
run pixel-serial ./tools/codex-android adb -s "$PIXEL" shell getprop ro.serialno
[[ "$LAST_OUTPUT" == "$EXPECTED_PIXEL_SERIAL" ]]
run pixel-model ./tools/codex-android adb -s "$PIXEL" shell getprop ro.product.model
[[ "$LAST_OUTPUT" == 'Pixel 9' ]]
run pixel-product ./tools/codex-android adb -s "$PIXEL" shell getprop ro.product.device
[[ "$LAST_OUTPUT" == 'tokay' ]]
run pixel-api ./tools/codex-android adb -s "$PIXEL" shell getprop ro.build.version.sdk
[[ "$LAST_OUTPUT" =~ ^[0-9]+$ ]]
run installed-pm-path ./tools/codex-android adb -s "$PIXEL" shell pm path "$PACKAGE"
[[ "$LAST_OUTPUT" == package:* && "$LAST_OUTPUT" != *$'\n'* ]]
INSTALLED_BASE_APK="${LAST_OUTPUT#package:}"
[[ "$INSTALLED_BASE_APK" == */base.apk ]]
[[ ! -e "$SESSION_DIR/installed-base.apk" ]]
run installed-apk-pull ./tools/codex-android adb -s "$PIXEL" pull "$INSTALLED_BASE_APK" "$SESSION_DIR/installed-base.apk"
run installed-apk-sha256 shasum -a 256 "$SESSION_DIR/installed-base.apk"
[[ "${LAST_OUTPUT%% *}" == "$EXPECTED_APK_SHA" ]]
record_checkpoint entry ABSENT
assert_t0_elapsed_at_most 120
```

If Pixel properties do not match serial `46141FDAQ003KZ`, model `Pixel 9`, or product `tokay`, the reported API level is not numeric, the installed APK hash differs from the locally preverified package/version/code/signer artifact, the package path is not a single base APK, the app PID is present, or the exact Black Pearl fingerprint is absent/ambiguous, stop and release. Do not install, perform ADB pairing, repeat ADB connect, request USB permission before the planned normal Connect, connect/reconnect the DAC outside the single named action, or change USB cables. Phase A verifies the local candidate package/version/code/signer. During Phase B the single installed `base.apk` is pulled and its SHA-256 is compared to that exact artifact; equality proves the installed bytes are the preverified package/version/code/signer, avoiding redundant `aapt`/`apksigner` work inside the entry window. The Pixel API level is recorded for context but is not a separate C05-C acceptance constraint.

After the selected wireless endpoint and installed APK are verified, use the exact launcher call `run launcher-start ./tools/codex-android adb -s "$PIXEL" shell am start -W -a android.intent.action.MAIN -c android.intent.category.LAUNCHER -n com.weekssa.opraeqforuapp/.MainActivity`. Then call `run launched-pid ./tools/codex-android adb -s "$PIXEL" shell pidof "$PACKAGE"`, set `PID="$(printf '%s' "$LAST_OUTPUT" | tr -d '\r\n')"`, and require `[[ "$PID" =~ ^[0-9]+$ ]]` before UI work. Capture a fresh `entry` screenshot and, if needed, tap only after inspection and `ack_capture_inspected` with `tap_from_capture entry "$X" "$Y"`. Screenshot labels and helper functions are defined above; all UI tap/swipe coordinates remain unset until calculated from that same fresh image. Never install.

At entry, call `record_checkpoint entry ABSENT` before launch and immediately call `assert_t0_elapsed_at_most 120`; after the clean five-second DEVICE preflight, call it again—the latter is the binding T0+2 checkpoint. After launch, save the one observed PID in `PID`; call `record_checkpoint post-navigation "$PID"` immediately after returning to My DAC and `record_checkpoint final "$PID"` after final reads. Capture each corresponding current app view with `capture_ui` and compare the PID returned by each checkpoint with the original launched PID. To inspect a screenshot, print `LAST_CAPTURE`, load that exact Mac-side PNG with the Codex local image viewer, then call `ack_capture_inspected` with the unchanged path. Tap/swipe helpers require that acknowledgement and consume the capture after acting. `permission-prompt` covers the exact Android dialog if it appears. For All bands, capture `baseline-eq-all-bands` or `final-eq-all-bands`; only if some bands are below the fold, derive one short vertical list swipe from the inspected image, call `swipe_from_capture`, then capture and inspect the unique `*-all-bands-bottom` image. Do not touch a band row or reuse coordinates. For DEVICE, capture the `*-device-top` frame, inspect and record every visible field, then conditionally use up to three screenshot-derived down-swipes and three return-to-top swipes, with a fresh inspected frame and field/value log after every swipe. A screen that needs more is an abort. For a safe failure diagnosis only, first capture and visually inspect `failure-idle`, call `ack_failure_idle_capture "$LAST_CAPTURE"`, then call `capture_failure_logcat`; its exact prepared command is `./tools/codex-android adb -s "$PIXEL" logcat -d -t 200 "--pid=$PID"`. This retains only the latest 200 log entries filtered to the exact app PID, runs only after visual confirmation that no hardware operation is active, and stays within the remaining time. It is not run on a success path or while a READ is active. No screenshot/XML capture file is written to the Pixel; normal app-local metadata or saveable UI state is outside that claim.

### B. Phase B — phone execution (only after Phase A passes and the owner makes the Pixel available)

#### Step 3 — Phase A completion and entry-timer start
- **UI/state:** Steps 1–2, exact source/UI-action review, evidence-ignore verification, wireless-ADB and logcat command preparation, abort/release preparation, and the independent review of this plan revision have all passed. The owner has freshly made the Pixel available after readiness and attached the Black Pearl before handoff if it was not already attached.
- **Action:** With the Phase A-initialized helper idle in the candidate-rooted Bash session, call `run t0-discovery ./tools/codex-android adb devices -l`; the wrapper records UTC and monotonic T0 immediately before starting it. This first ADB invocation begins Pixel occupancy. Immediately save `INITIAL_DISCOVERY="$LAST_OUTPUT"`, reject any Pixel USB ADB row, and execute the prepared fail-closed target-selection block. It accepts an already-online ADB Wi-Fi serial only when its service instance carries the exact recorded Pixel serial prefix; otherwise it reads one current mDNS service table, accepts only one serial-prefixed TLS connect service and its live IPv4 endpoint, and either selects that endpoint's already-online alias or connects to that current endpoint once. No unassociated host:port, old address/port, pairing service, pairing code, emulator, or USB ADB transport is selectable. Then require exact on-device serial/model/product in Step 4.
- **USB effect:** NO USB to the DAC; Android control is wireless.
- **Expected UI result:** The exact Pixel 9 hardware identity is verified over one explicit wireless ADB endpoint, with no USB ADB transport used. T0 and the monotonic value are contemporaneously recorded.
- **Expected physical evidence:** T0, timestamped Pixel model/API/network transport confirmation, and the command/result record.
- **Abort:** Owner has not made the Pixel available after Phase A, ambiguous/missing wireless target, any USB ADB connection, or failure to reach the two-minute entry checkpoint; stop and immediately release.

#### Step 4 — Verify the installed application identity

Use the exact commands in the prepared block after an exact serial-qualified wireless target or its uniquely mDNS-mapped current endpoint alias has been selected. Require the device state to be `device`, `ro.serialno` to equal the previously evidenced Pixel 9 serial `46141FDAQ003KZ`, `ro.product.model` to be `Pixel 9`, `ro.product.device` to be `tokay`, the API level to be numeric and recorded, and `pm path` to return exactly one installed `base.apk`. Compare the pulled APK's full SHA-256 to the locally preverified APK; exact byte equality binds package/version/code/signer without rerunning local artifact tools on the phone clock. The package PID must be absent at this checkpoint. This is a bounded artifact/device match check, not a new source or GitHub review.
- **UI/state:** Exact Pixel selected over wireless ADB; no app launch or DAC protocol request yet.
- **Action:** Run the prepared model/product/API, single `pm path`, one base-APK pull and SHA-256 comparison, then call `record_checkpoint entry ABSENT` to capture exact Black Pearl fingerprint, target PID absence, and top activity.
- **USB effect:** NO USB to the DAC.
- **Expected UI result:** Installed package identity equals the local debug candidate and the exact Black Pearl is attached; the EQ Library process is absent.
- **Expected physical evidence:** T0-stamped command/results, Pixel serial/model/product/API checks, single `pm path` line, installed base APK SHA-256 match, entry PID/top-activity and exact DAC fingerprint fields.
- **Abort:** Any mismatch, split/multiple installed path, missing/ambiguous fingerprint, existing PID, or entry time at/after T0+2 minutes. Stop and immediately release; never install or force-stop.

- **UI/state:** Correct Pixel is selected over wireless ADB; no DAC read is requested.
#### Step 5 — Verify attached Black Pearl identity and require a cold app process
- **UI/state:** The owner attached the Black Pearl to Pixel USB-C before Phase B if it was not already attached. The Pixel is selected over wireless ADB. The app must not be running. Do not change cables, stop an app process, or change Android permissions.
- **Action:** Use the completed Step 4 `record_checkpoint entry ABSENT` result. It contains the package-PID result, resumed/top activity fields, and only the exact matching Black Pearl fingerprint fields. Require VID/PID `3302:43e8`, manufacturer `TTGK Technology`, product `TE-C`, serial `330243E8260129`, and an absent `com.weekssa.opraeqforuapp` PID. If the record does not contain exactly one matching device block or a PID exists, even if My DAC appears disconnected or idle, stop and release; do not force-stop or enter the app. An already-current session and the connected-device-card route are excluded.
- **USB effect:** NO USB to the DAC; host descriptor and process metadata inspection only.
- **Expected UI result:** Exact attached fingerprint is present, the target app process is absent, and no app session can be current.
- **Expected physical evidence:** T0-correlated command-log entries for the exact USB fingerprint, absent package PID, and top/resumed activity output.
- **Abort:** DAC absent, fingerprint incomplete/mismatched, package PID exists, USB ADB transport appears, or any uncertainty. No unplug, replug, force-stop, cable change, or manual permission change.

#### Step 5a — Launch once and reach My DAC while disconnected
- **UI/state:** Step 5 verifies the exact attached Black Pearl and an absent app process. The ordinary launcher starts the installed app without USB-attach extras. The expected entry is either exact Black Pearl My DAC visibly Disconnected, or one exact disconnected context card on a root screen.
- **Action:** Under the owner's direct autonomous-execution authorization, use the exact `.MainActivity` `ACTION_MAIN` plus `CATEGORY_LAUNCHER` call in the prepared command set to launch the already-installed app once. Never dispatch `ACTION_USB_DEVICE_ATTACHED`; that path can initiate Connect before the safety preflight. Capture a fresh `entry` screenshot. If launch opens My DAC automatically, continue only if the screenshot identifies the exact TRN Black Pearl and visibly says Disconnected. Otherwise, on a root screen, capture/inspect the exact card, require exactly one TRN Black Pearl context card with status **Device detected · Open My DAC to connect** and action **Open My DAC**, set the center coordinates from that image, and invoke `tap_from_capture` once. Save the exact launched PID. Do not guess or reuse coordinates. Do not install, force-stop, open Android Settings, type text, select another card, or use any unrelated app surface.
- **USB effect:** NO USB — a fresh app process initializes the transport Disconnected; detecting the already-attached device does not open it or send a DAC protocol request.
- **Expected UI result:** The ordinary My DAC screen identifies TRN Black Pearl and is visibly Disconnected with its normal Connect action. No Connected/current-session state is present. A missing card, multiple-device choice, connection-error card, stale state, or mismatched device is not an equivalent route.
- **Expected physical evidence:** Unique timestamped `entry` screenshot and SHA-256, launcher command/result, disconnected My DAC identity, and foreground app PID in `commands.log`.
- **Abort:** Any Connected/Connecting state, wrong/stale identity, multiple-device choice, connection-error card, unexpected chooser/permission prompt before Connect, active/current session, absence of the exact disconnected card (unless exact disconnected My DAC opened automatically), or absence of a clearly labeled normal Connect action. If the screenshot or visible target is ambiguous, do not guess or retry through another entry point.

#### Step 5b — Compose DEVICE content before connection
- **UI/state:** My DAC is visibly disconnected. Because Step 5 requires the target package process to be absent, the newly launched process has a fresh ViewModel/transport and no current Black Pearl snapshot/session. The selected tab may be EQ or DEVICE.
- **Action:** If DEVICE is not visibly selected, capture a fresh screenshot, inspect the exact DEVICE tab, then use `tap_from_capture` once. Call `capture_ui device-preconnect-start`; record its UTC timestamp. Wait the full five seconds without touching the screen; call `capture_ui device-preconnect-end` and record its UTC timestamp. Inspect both images for recovery/restore text or status, active operation, spinner, error, or other uncertainty. If clean, call `record_checkpoint device-preconnect "$PID"` to confirm the launched process, resumed EQ Library activity, and exact USB fingerprint are unchanged, then call `assert_t0_elapsed_at_most 120`. This is the binding T0+2 checkpoint. Do not Connect unless it passes.
- **USB effect:** NO DAC request under the required disconnected/no-current-snapshot precondition. If a saved pending restore step is restored, the effect's missing/noncurrent-snapshot branch clears the pending indices and returns before its setter branch. That branch can report **Restore stopped because the current Black Pearl state could not be verified**; a recovered pending state is therefore an abort, not permission to continue. A clean screen is required before Connect.
- **Expected UI result:** DEVICE content is selected; My DAC remains clearly Disconnected with no active operation, restore/recovery message, or error after the settling interval.
- **Expected physical evidence:** Unique `device-preconnect-start` and `device-preconnect-end` images, SHA-256 and capture UTCs in the host log, plus the package PID/activity and exact fingerprint checkpoint with elapsed seconds, showing selected DEVICE/disconnected UI across the full five seconds; include any stop notice if present.
- **Abort:** The app becomes Connected, any restore/recovery notice appears, a busy/error state persists, the screen does not settle within the checkpoint, or any DAC request occurs before Connect. Stop and release; do not connect or clear/reset any state.

#### Step 5c — Agent performs one normal Connect
- **UI/state:** Step 5b confirms DEVICE content is selected while My DAC remains Disconnected; the exact USB fingerprint still matches; the same freshly launched app process remains foreground.
- **Action:** Capture and inspect a new image with `capture_ui connect-action` immediately before the tap. Use screenshot-derived center coordinates to call `tap_from_capture connect "$X" "$Y"` exactly once. Do not tap again.
- **USB effect:** READ — if Android permission already exists, the transport opens the verified device and claims the interface; after Connected, production observers start EQ and DEVICE reads. If permission is absent, the tap first requests an Android permission decision, and no protocol read occurs before that decision. Automatic session-entry reads are not the C05-C baseline; they may emit read-request reports and use the bounded automatic nondestructive retry. The saved-step guard was cleared while disconnected, so this Connect cannot resume it.
- **Expected UI result:** Either the exact Android permission request described in Step 5d appears, or the app reaches Connected without a dialog because permission was already granted.
- **Expected physical evidence:** Unique pre-action screenshot/hash, coordinate-sourced single agent UI action in the contemporaneous command log, and the immediate permission-prompt or Connected screenshot/hash.
- **Abort:** Any chooser, other app/device identity, duplicate Connect, error, unexplained state, or write indication. Do not tap again.

#### Step 5d — Handle the exact Android USB permission request only if it appears
- **UI/state:** Step 5c produced a system request that names EQ Library and the exact verified Black Pearl.
- **Action:** If Android shows a permission request, capture `permission-prompt` and continue only when it names this app and the exact previously verified Black Pearl. The agent may approve that one request under the owner's direct authorization; derive the center coordinates from the captured dialog and use `tap_from_capture permission-allow "$X" "$Y"`. Do not approve any chooser or ambiguous/mismatched request. If no dialog appears, skip only when the same single Connect has already reached Connected.
- **USB effect:** READ after approval completes the ordinary connection and triggers the session-entry EQ/DEVICE reads described in Step 5c; the permission confirmation itself sends no DAC setting write.
- **Expected UI result:** The app reaches Connected and settles its automatic session-entry reads.
- **Expected physical evidence:** Unique permission-dialog image/hash, exact dialog identity, the single approved permission action and derived coordinates in the contemporaneous command log, resulting Connected screenshot and timestamps. If skipped, retain Step 5c's Connected-state evidence.
- **Abort:** Ambiguous/mismatched dialog, owner declines, connection error, or failure to reach Connected. If declining, dismiss once and release; never grant twice, retry Connect, or change DAC settings.

#### Step 5e — Confirm the production session before collecting baseline values
- **UI/state:** The agent completed at most one Connect after the offline DEVICE preflight; any permission request received at most one approval after exact identity verification.
- **Action:** Verify My DAC visibly says **TRN Black Pearl / Connected**. Call `record_checkpoint connected "$PID"` and `capture_ui connected`; verify the app PID/top activity and exact same USB fingerprint remain present, and no chooser/stale/error state exists. Wait for connection-triggered reads to settle without triggering another request.
- **USB effect:** NO USB from inspection/wait; session-entry READs are the only effects from the connection path.
- **Expected UI result:** Exact identity paired with a current production Connected presentation and settled initial observations.
- **Expected physical evidence:** Unique `connected` screenshot/hash, foreground PID/top activity, exact descriptor fields and read-status capture in the host log.
- **Abort:** No exact Connected state, changed/ambiguous fingerprint, stale/error state, reads that do not settle within the session budget, or unexpected write/recovery surface. Stop and release without reconnecting.

#### Step 6 — Establish the baseline read screen and idle state
- **UI/state:** My DAC is open for the same Black Pearl after the cold disconnected preflight and single Connect. No recovery card or active operation is being acted upon.
- **Action:** Confirm the normal EQ status is **Verified current hardware** and DEVICE status is **Current device state** / **Values verified from the connected DAC** where currently displayed; wait for any already-running operation to finish without touching it.
- **USB effect:** NO USB from inspection/wait. Any earlier connection-triggered reads are not counted as this session's fresh baseline.
- **Expected UI result:** Positive current-state labels, no spinner/error, no active read/write.
- **Expected physical evidence:** Timestamped initial screenshot/session snapshot; fresh baseline is established only by Steps 7–10.
- **Abort:** Stale/Last read, Device settings, missing positive status, read failure, busy operation that does not settle safely, or an unexpected recovery prompt; do not use recovery-only controls.

### C. Production baseline reads

#### Step 7 — Explicit baseline EQ read
- **UI/state:** My DAC is open with the exact session current and no active operation. The pending-restore preflight completed with DEVICE visibly composed while disconnected and a clean settled screen before Connect; the selected tab may now be EQ or DEVICE.
- **Action:** If EQ is not visibly selected, capture/inspect a fresh image and use `tap_from_capture` on the exact **EQ** tab once; this callback only updates `selectedTabIndex`. If EQ is already selected, do not tap again. Require **Verified current hardware** and idle status, capture/inspect a fresh image immediately before normal **Edit EQ** and tap once, then capture `baseline-eq` after `openBlackPearlEditor()` completes its fresh `readBlackPearlSnapshot()` successfully.
- **USB effect:** NO USB for a needed EQ-tab selection; then READ for Edit EQ. A current-session check gates snapshot publication. Transport may make its documented single automatic nondestructive reissue after timeout.
- **Expected UI result:** Editor opens only after a successful fresh read; otherwise it reports failure and the session stops.
- **Expected physical evidence:** Unique `baseline-eq` screenshot/hash of selected EQ state and exact production EQ read result tied to the still-current session.
- **Abort:** EQ tab is absent/ambiguous, tab selection changes session or starts unexpected USB activity, spinner/error persists, editor does not open, session changes, or any mutation/review/apply surface is encountered; no manual retry.

#### Step 8 — Record baseline EQ and close locally
- **UI/state:** Normal EQ editor reached by Step 7; the editor says edits are local only.
- **Action:** Capture and inspect a fresh image immediately before opening **All bands**; tap the exact label once. Capture `baseline-eq-all-bands` and record each visible band field. If all ten bands do not fit, derive one short vertical swipe contained within the list from that image, call `swipe_from_capture baseline-eq-all-bands-scroll ...`, then capture/inspect `baseline-eq-all-bands-bottom` and record remaining fields. Record the active slot and visible Playback gain. Do not tap band rows or change values. Capture and inspect a fresh image before tapping **Close** once.
- **USB effect:** NO USB; All bands and Close are local presentation/state actions.
- **Expected UI result:** My DAC EQ screen returns with **Verified current hardware**; closing the editor issues no hardware write.
- **Expected physical evidence:** Complete displayed baseline EQ fields, active slot, screenshot timestamps/hashes; Playback gain recorded separately from DEVICE Volume.
- **Abort:** Any edited value, Apply/Review/Safe gain action, incomplete band view, missing positive status, or unexpected hardware-operation indication.

#### Step 9 — Explicit baseline DEVICE read
- **UI/state:** My DAC is open for the same Black Pearl after the disconnected DEVICE preflight and one successful Connect; the selected tab may be EQ or DEVICE.
- **Action:** If DEVICE is not visibly selected, capture/inspect a fresh image and use `tap_from_capture` on the exact **DEVICE** tab once; this changes only local tab selection and closes the local editor if open. If DEVICE is already selected, do not tap again. Capture/inspect a fresh screenshot immediately before normal **Refresh**, tap once, and wait for completed success. Capture and inspect `baseline-device-top`; record every fully visible field with `record_observation baseline-device-top FIELD='visible value' ...`. The panel is inside the vertically scrollable My DAC column (`MyDacScreen.kt:171-176`) and includes the Audio rows, Microphone, USB & System, optional unavailable-mode text, and UAC guidance (`BlackPearlDeviceBatchControls.kt:93-167`). If any required snapshot field is below the fold, derive a short vertical scroll swipe entirely inside the list from the latest inspected capture; call `swipe_from_capture baseline-device-scroll-$n ...`, then capture and inspect `baseline-device-scroll-$n` and record newly visible field/value pairs. Repeat only as needed, at most three downward swipes; stop if all fields are not captured. After recording all required fields, return to the top only as needed: derive each upward swipe from the latest inspected image, use `swipe_from_capture baseline-device-return-top-$n ...`, capture/inspect `baseline-device-return-top-$n`, and stop when DEVICE Refresh and the tabs are visible again. At most three upward swipes are allowed; otherwise stop. Keep all setting rows untouched.
- **USB effect:** NO USB for a needed DEVICE-tab selection; then READ through `readBlackPearlQualificationControls()` and its production DEVICE snapshot path; existing bounded automatic read retry only.
- **Expected UI result:** **Current device state** / **Values verified from the connected DAC**. Record volume, DAC filter, gain mode, amplifier topology, balance, microphone gain, and USB audio mode or its truthful unavailable state.
- **Expected physical evidence:** Unique ordered screenshot/hash series beginning `baseline-device-top`, plus only the conditional `baseline-device-scroll-1..3` and `baseline-device-return-top-1..3` captures used, and contemporaneous `record_observation` field/value log entries covering the complete baseline DEVICE snapshot. Optional unavailable UAC mode alone is not failure.
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
- **Action:** Capture/inspect a fresh My DAC image immediately before **Back** and tap it once. Capture/inspect the root and tap the visible **EQ Library** item once. Capture/inspect `library-card`; verify the same single TRN Black Pearl context card reports **Connected · State current** and **Open My DAC**; derive coordinates from that image and tap the card once with `tap_from_capture`. Do not tap Connect or select another output/device. Immediately after My DAC returns call `record_checkpoint post-navigation "$PID"`, then capture `returned-session`. If this exact current card is absent or its state is stale/reading/waiting/error, stop.
- **USB effect:** NO USB; the route change retains presentation and does not itself disconnect/reconnect or trigger the final read.
- **Expected UI result:** TRN Black Pearl and current-session state remain truthful after return. Navigation alone is not treated as a fresh physical read; final observations are Steps 12 and 14.
- **Expected physical evidence:** Entry PID/top-activity/fingerprint checkpoint, then immediate post-navigation PID/top-activity/fingerprint checkpoint, unique `library-root`, `library-card`, and `returned-session` screenshots/hashes; internal session generation is not exposed and is not claimed.
- **Abort:** DAC/session/PID changes, stale or false presentation, chooser, missing route, or any Connect/reconnect/permission prompt. Do not reconnect.

#### Step 12 — Explicit final EQ read
- **UI/state:** Returned to My DAC for the same current Black Pearl session. The disconnected DEVICE preflight completed before Connect, so pending reset continuation was either absent or caused a stop before the session began. Because saveable navigation may restore DEVICE from Step 9, the selected tab is not assumed.
- **Action:** If EQ is not visibly selected, capture/inspect and tap the **EQ** tab once using `tap_from_capture`; this callback only updates `selectedTabIndex`. If already selected, do not tap again. Require current **Verified current hardware** and idle status; capture/inspect immediately before normal **Edit EQ**, tap once, wait for the fresh read and successful editor, then capture/inspect before **All bands**. Record all fields/slot from `final-eq-all-bands`; if some are below the fold, derive one short vertical list swipe from that image with `swipe_from_capture`, then inspect `final-eq-all-bands-bottom`. Capture/inspect before **Close** and close without editing.
- **USB effect:** NO USB for a needed EQ-tab selection; READ for Edit EQ; All bands/Close issue NO USB.
- **Expected UI result:** Successful final snapshot and return to **Verified current hardware**.
- **Expected physical evidence:** Unique `final-eq` and All-bands screenshots/hashes, final production EQ observation, and same-field comparison to baseline.
- **Abort:** EQ tab is absent/ambiguous, tab selection changes session or starts unexpected USB activity, failed/stale read, changed session, incomplete values, or any setting/mutation action; no manual retry.

#### Step 13 — Explicit final DEVICE read
- **UI/state:** Same returned My DAC session; the selected tab may be EQ or DEVICE, including saveable DEVICE state restored from Step 9 after the clean disconnected preflight.
- **Action:** If DEVICE is not visibly selected, capture/inspect and tap the **DEVICE** tab once with `tap_from_capture`; this changes only local selection and closes the local editor if open. If DEVICE is already selected, do not tap again. Capture/inspect a fresh screenshot immediately before **Refresh**, tap once, and wait for success; capture/inspect `final-device-top` and record every fully visible field with `record_observation final-device-top FIELD='visible value' ...`. If required values remain below the fold, use only screenshot-derived in-list swipes and captures labeled `final-device-scroll-1..3`, recording newly visible fields after each inspected image. Stop if all required snapshot fields are not captured within three swipes. Restore the top only as needed using screenshot-derived upward swipes and inspected captures `final-device-return-top-1..3`; stop unless DEVICE Refresh and the tabs are visibly back at the top within three swipes.
- **USB effect:** NO USB for a needed DEVICE-tab selection; then READ through the production DEVICE snapshot path; existing bounded automatic read retry only.
- **Expected UI result:** **Current device state** / **Values verified from the connected DAC**.
- **Expected physical evidence:** Unique ordered `final-device-top` screenshot/hash and conditional scroll/return-top screenshots, contemporaneous field/value log entries, and final DEVICE observation compared field-to-same-field with baseline at displayed precision. An **Inconsistent channel read** is not passable.
- **Abort:** DEVICE tab is absent/ambiguous, tab selection changes session or starts unexpected USB activity, read failure, missing current indicator, changed session/value, balance inconsistency, or any need to change settings; no manual retry.

#### Step 14 — Capture final evidence and complete safely
- **UI/state:** Final reads are complete and app shows no active read/write.
- **Action:** Use the exact session ID and evidence destination prepared and positively ignore-verified in Phase A; do not run GitHub/build/source/ignore-path research during phone occupancy. Call `record_checkpoint final "$PID"` and capture `final-session`; the helper records final PID/top activity and only the exact USB fingerprint fields, while screenshot and text-command SHA-256 values remain in the Mac log. Once every physical observation and screenshot is complete, call `finalize_evidence_manifest` exactly once; it writes `SHA256SUMS` over every session file except that manifest. Do not append or modify any evidence file afterward. Do not collect broad/full logcat on a successful pass; if a failure needs diagnosis, capture only the latest 200 log entries for the exact app PID after the device is idle and within the remaining occupancy limit. Store raw evidence only in that unique `.unlazy/v080-beta/evidence/c05c-<UTC-session-id>/` directory; never overwrite a prior stop report or commit raw captures.
- **USB effect:** NO USB to the DAC from these observations; wireless ADB only.
- **Expected UI result:** Final positive current-session presentation; no active operation.
- **Expected physical evidence:** A SHA256SUMS manifest over the completed session files, establishing exact identity, baseline/final displayed values, attached navigation, and no state-changing control invoked. It does not establish zero USB reports or raw bit-for-bit equality. The manifest excludes itself; the post-session sanitized reconciliation records its SHA-256 and phone-release time outside the raw session folder.
- **Abort:** Ignore verification fails, any unexpected value difference, missing evidence, or ambiguous state; preserve what exists, do no repair/restoration write, stop, and release the Pixel.

#### Step 15 — Release phone and report outcome
- **UI/state:** No hardware operation is active; all collected evidence is saved locally.
- **Action:** Perform no further device action. Immediately report **PHONE RELEASED — YOU CAN TAKE THE PIXEL BACK**, with C05-C pass/stopped and any discrepancy.
- **USB effect:** NO USB.
- **Expected UI result:** Not applicable; phone is released.
- **Expected physical evidence:** Session outcome and release timestamp in the post-session sanitized reconciliation; the raw session manifest has already been finalized and is not changed after release.
- **Abort:** If a READ is still active, do not disconnect or kill anything; allow the nondestructive read to settle safely, then release. No setting restoration is needed or permitted because the plan is read-only.

## Occupancy budget and checkpoints

Complete repository/APK/PR/check/evidence-ignore/source/procedure/command/logcat/abort/release preparation in Phase A. The owner may use the Pixel normally until Phase B. T0 is captured immediately before the first fresh wireless-ADB discovery/connection command after a new availability statement; that invocation starts occupancy. No off-phone preflight may occur during the session. Do not begin unless the complete sequence is practical within the 10-minute target. At T0+2 minutes require exact Pixel/APK/USB identity, absent pre-launch package process, ordinary launch, and clean disconnected DEVICE preflight including the complete five-second observation. Baseline EQ and DEVICE by minute 5; attached navigation plus contemporaneous PID/fingerprint capture by minute 7; final EQ/DEVICE and final PID/fingerprint capture by minute 10. The hard limit is 15 minutes. If an essential checkpoint is missed, capture the reason and minimum safe evidence, stop at a safe idle point, and report PARTIAL / NOT PASS; never extend or backfill T0. Beyond the limit, wait only for an already-active nondestructive read to settle safely; this read-only plan requires no restoration write. Do not exceed the cap to gather optional screenshots or finish an inactive step. JA11, EW300, and manual TalkBack are excluded.

Capture screenshots only for the My DAC identity/session state, baseline and final EQ All bands (including only the scroll needed to show all ten bands), baseline and final DEVICE values, and the away/back Library state. Stream each PNG directly from the Pixel to the Mac with `adb exec-out screencap -p`; this does not create a capture file on the Pixel. Inspect each screenshot before acting, and tap only a unique, fully visible exact-label control at coordinates derived from that screenshot. Use the same minimal capture method throughout; do not repeat a capture because of presentation preference, save unrelated home-screen content, or dump unrelated USB devices. Save every required PID/activity and exact USB fingerprint observation directly to the ignored session log at the start, immediately after navigation back, and after final reads; a chat/tool transcript or post-session recollection is not a substitute.

## C05-C acceptance

**Pass only if all of the following are evidenced:**

- Exact frozen candidate and APK verified before DAC access; Pixel is controlled through wireless ADB only; PR/check state is still valid.
- Phase B began only after Phase A and independent review passed and the owner freshly made the Pixel available; T0 was recorded immediately before the first prepared wireless-ADB discovery command. The two-minute checkpoint was met without off-phone preparation. Android USB descriptors match the exact known Black Pearl fingerprint where exposed. At session entry the target package process was absent; after one ordinary launch, DEVICE content was composed while the fresh transport was visibly Disconnected and the screen settled with no restore/recovery notice, error, or active work. Only then may the app identify TRN Black Pearl with a current Connected session established through the single agent Connect and, only if shown, one exact permission approval under the owner's current authorization. Existing-process/current-session entry and the Connected card at initial entry are excluded. The one exact disconnected card is permitted for initial entry; the same Connected/current card is permitted only for the planned post-baseline return. No physical attachment/cable change or app Connect retry is used.
- Baseline EQ and DEVICE reads succeed through the production paths and are completely recorded; optional UAC absence is represented truthfully.
- EQ is visibly selected before each baseline/final Edit EQ read, and DEVICE is visibly selected before each baseline/final Refresh. Tab selection is local-only and issues no DAC request; the evidence records the selected tab and confirms the current session remained unchanged.
- My DAC → EQ Library → My DAC navigation occurs while attached; the observed USB identity and PID remain the same, and production UI reports a current session at the recorded checkpoints. The app does not expose internal session generation, so no stronger continuity claim is made between reads.
- Final explicit EQ and DEVICE reads succeed on the same current session; every required displayed field matches its corresponding baseline field at the UI's presentation precision. An **Inconsistent channel read** balance state is not passable. This comparison does not claim bit-for-bit raw protocol equality where the UI rounds or coarsens values.
- No state-changing control/API path, Reset, Flash, Save, Apply, DEVICE setter, induced fault, process kill, or disconnect was used. All required PID/activity, USB fingerprint, screen, read, and timing evidence is saved contemporaneously and the complete file set is hash-indexed. The conclusion is bounded: no state-changing control/API path was invoked, and the required displayed values remained equal across captured production reads at the UI's presentation precision. This does not claim raw bit-for-bit equality or zero USB transfer count.

**Stop / not pass** on any Phase A failure, absent fresh owner availability after readiness, wireless ADB ambiguity, Black Pearl not attached before T0, unknown USB identity, package process present at entry, any restore/recovery notice during disconnected preflight, failure to show DEVICE selected and clearly Disconnected before Connect, any busy/error/uncertain preflight, failure to meet the exact T0+2-minute checkpoint, failed single launcher/Connect/exact permission step, failed/incomplete read, changed session/PID, stale or false presentation, chooser/choice surface, changed value, unexpected setting mutation, `UNKNOWN` USB effect, or other unclear state. Preserve the evidence, do not retry manually or attempt repair/restoration, release the phone, and continue Mac-side diagnosis. No physical attach/cable change during Phase B, force-stop, Connected-card entry at initial entry, existing-session reuse, or alternate/repeated app connection flow is allowed.

## Maintained references

- Master execution contract: `docs/implementation/v0.8.0-beta-master-directive.md`
- Product/design baseline: `docs/implementation/v0.8.0-beta-mission.md` §48
- Black Pearl physical procedure background: `docs/BLACK_PEARL_V0.6_RESTORE_DEFAULTS_HANDS_ON_CHECKLIST.md` (identity/read and stop rules only; reset/write checklist is out of scope)
- The exact host-only screenshot stream command is documented by Android's [ADB screenshot guide](https://developer.android.com/tools/adb#capture-a-screenshot).
- The current mDNS identity rule is bounded to the documented AOSP service-instance convention: [AOSP ADB Wi-Fi instance naming and `adb mdns services`](https://android.googlesource.com/platform/packages/modules/adb/+/refs/heads/main/docs/dev/adb_wifi.md#51). If the serial prefix is absent or ambiguous, the endpoint is not used.
- Google's [ADB Wi-Fi discovery example](https://developer.android.com/tools/adb#resolve-wireless-connection-issues) documents richer service fields, but the prepared plan deliberately uses the finite `adb mdns services` table and a strict exact-serial-prefix check rather than the continuous `track-services` command.
- Canonical status: `docs/implementation/v0.8.0-beta-autonomy-status.md`
- Acceptance evidence: `docs/implementation/v0.8.0-beta-acceptance-evidence.md`
- Watchdog issue: https://github.com/weekssa/OPRA-EQ-for-UAPP/issues/71
