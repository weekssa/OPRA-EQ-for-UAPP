# TRN Black Pearl — v0.6 My DAC read-only DEVICE qualification

Status: **PHYSICAL VALIDATION IN PROGRESS**

This checklist is the first physical gate for additional Black Pearl DEVICE capabilities in v0.6. It validates **read-only** behavior only. Passing this checklist does **not** authorize any new device-control write, normal production exposure, persistence claim, or expansion of the already-qualified EQ/Reset protocol.

The exact candidate below passed the required Android unit/lint/build/security/catalog gates and was signed with the established EQ Library release identity before hands-on testing.

## Scope

The candidate read-only panel currently reads, in one current USB session:

- firmware/version string;
- DAC reconstruction filter;
- gain mode;
- amplifier topology;
- microphone gain;
- left/right balance;
- playback/global gain.

No candidate DEVICE write command exists in this increment. The panel is explicitly labeled **Hardware qualification · read-only** and must not latch, save, or change any setting.

## Test setup

- Android device: Pixel 9
- DAC: TRN Black Pearl
- Install the signed v0.6 candidate built from the exact recorded commit.
- Use a trusted Black Pearl controller/tool only as an independent observation source for the same settings; do not leave two controllers actively connected to the DAC at the same time.
- Stop playback before changing any comparison setting with another controller.
- Keep listening volume conservative before reconnecting or resuming playback.
- Do not perform firmware, bootloader, identity, or raw-register operations.

Recorded candidate provenance:

- Candidate commit SHA: `d3d227de45751f6606b75637a2fd311249afa88d`
- Signed beta source APK: `EQ-Library-v0.6.0-beta-d3d227d.apk`
- Immutable exact-candidate URL: `https://raw.githubusercontent.com/weekssa/OPRA-EQ-for-UAPP/mobile-test-apk/candidates/EQ-Library-v0.6.0-beta-d3d227d.apk`
- Immutable checksum URL: `https://raw.githubusercontent.com/weekssa/OPRA-EQ-for-UAPP/mobile-test-apk/candidates/EQ-Library-v0.6.0-beta-d3d227d.apk.sha256`
- Signed APK SHA-256: `ea8235ee1ba2828873b488054b25d40c917b844c2d76a813bf9e038cf31c8ee5`
- Signer certificate SHA-256: `65c1c1256dae3c49e3548f334c91f0ba991969e9be9e0b223ba4e253d2114747`
- Signer: `CN=OPRA EQ for UAPP, O=weekssa`; RSA 4096; APK Signature Scheme v2/v3 verified; one signer
- Android CI: run #1226 / ID `34431846554` — **PASS**
- CodeQL: run #1107 / ID `34431846550` — **PASS**
- Catalog currentness CI: run #1331 — **PASS**
- Priority community coverage CI: run #819 — **PASS**
- Signed EQ Library Beta Candidate: run #901 / ID `34431843368` — **PASS**
- Signed-beta artifact ID: `10134900164`
- Signed-beta artifact ZIP SHA-256: `04c9c085c5aed42d4b2421391410151291354fe05841600f635445e733fff773`
- Test date: `2026-09-13`
- Tester: Project owner

The `EQ-Library-mobile-test.apk` and `EQ-Library-v0.6.0-mobile-test.apk` convenience filenames move as development advances and are **not** qualification provenance. For this checklist, use the immutable exact-candidate URL above if a reinstall is needed.

This checklist/provenance documentation may receive later documentation-only commits. The hardware candidate remains the exact signed source commit recorded above unless a behavior-changing replacement candidate is explicitly pinned.

## Baseline record

Independent Black Pearl controller observations supplied by the project owner:

- Firmware/version: `0.6` — confirmed
- DAC filter: `Fast-PC` — confirmed active
- Gain mode: `HIGH` — confirmed
- Amp topology: `CLASS AB` — confirmed
- Mic gain: `0 dB` — confirmed
- Balance: `Center` — confirmed
- Playback level presentation: `Volume 63%` — confirmed

EQ Library reports the same playback/global-gain register as `+2.00 dB`, native raw `512`. The reviewed independent Android controller source at commit `491e9d5131562d85b44ce9fd741f3e1ff5c4781c` defines its volume percentage as a linear presentation of raw range `-9472..6440`:

`raw = VOL_MIN_RAW + (percent / 100) * (VOL_MAX_RAW - VOL_MIN_RAW)`

Rearranging that presentation for raw `512` gives `62.745...%`, which is consistent with the controller's displayed rounded `63%`. This establishes that the two applications are presenting the same underlying playback register in different units. It does **not** make percent a dB unit or authorize a playback-volume write control.

## 1. Recognition, connection, and read-only presentation

1. Start EQ Library with the Black Pearl disconnected.
2. Connect the Black Pearl and use the normal Android USB permission flow if prompted.
3. Open **My DAC** and select the **DEVICE** tab.
4. Verify the header identifies **TRN Black Pearl** and reports the connection state textually.
5. Verify the page shows device information plus **Hardware qualification · read-only**.
6. Verify the explanation states that no setting is changed, latched, or saved.
7. Verify there are no new normal Black Pearl DEVICE setting controls for filter, gain mode, topology, balance, microphone gain, or playback level.

PASS / FAIL: **PASS**

Notes: The final 2026-09-13 connected-state screenshot visibly shows **TRN Black Pearl** with textual state **Connected**, the **DEVICE** tab, `Device information → Model → TRN Black Pearl`, and the **Hardware qualification · read-only** section. It also shows the explanatory text that reads candidate settings for validation only and changes, latches, or saves nothing, plus the message that no additional device controls are exposed until their read/write/readback semantics are qualified. This closes the recognition/connection/header and read-only-presentation gate.

## 2. First complete hardware read

1. With the Black Pearl connected, tap **Read candidate controls** once.
2. Verify the panel reports **Current session read**.
3. Compare every displayed value against the independent baseline record:
   - firmware/version;
   - DAC filter;
   - gain mode;
   - amp topology;
   - mic gain;
   - balance;
   - playback/global gain.
4. Verify no field is silently substituted with a default or guessed value.
5. If EQ Library reports an inconsistent left/right balance state, stop and record the exact values rather than treating it as a centered/valid balance.

PASS / FAIL: **PASS — all seven fields independently reconciled**

Observed EQ Library values:

- Firmware/version: `0.6`
- DAC filter: `Fast-PC`
- Gain mode: `HIGH`
- Amp topology: `CLASS AB`
- Mic gain: `+0.00 dB`
- Balance: `Centered`
- Playback/global gain dB: `+2.00 dB`
- Playback/global gain raw: `512`

Independent comparison confirms semantic agreement for firmware `0.6`, active `Fast-PC`, `HIGH`, `CLASS AB`, mic gain `0 dB`, and centered balance (`Center` vs `Centered`). For playback/global gain, the independent controller shows `Volume 63%`; its maintained source maps percent linearly across the same raw range, and raw `512` maps to approximately `62.745%`, reconciling the controller presentation with EQ Library's raw `512` / `+2.00 dB` presentation.

Notes: The supplied Pixel 9 screenshots show **Current session read** and all seven candidate fields populated together. The independent controller evidence and maintained presentation formula now reconcile all seven fields. This section therefore passes. Repeated-read non-destructiveness, external-change freshness, disconnect/reconnect freshness, and final no-write comparison remain separate required gates.

## 3. Repeated reads are stable and non-destructive

1. Without changing any DAC setting, tap **Read candidate controls** at least three times.
2. Verify the same hardware state is reported each time.
3. Re-check the same settings with the independent controller/tool after closing/releasing EQ Library's USB session as needed.
4. Verify EQ Library's reads did not change firmware/version, DAC filter, gain mode, topology, mic gain, balance, or playback/global gain.
5. Verify no save/persistence action was triggered merely by reading.

PASS / FAIL: **PASS**

Notes: On 2026-09-13 the project owner explicitly performed three consecutive **Read candidate controls** actions on the pinned candidate without changing any DAC setting and reported success. All three remained **Current session read** and returned the same complete state: firmware `0.6`, filter `Fast-PC`, gain mode `HIGH`, topology `CLASS AB`, mic gain `+0.00 dB`, balance `Centered`, playback/global gain `+2.00 dB` / raw `512`. Together with the already-supplied later independent-controller state showing the matching baseline and no unexpected persistence change, this satisfies the required stable/non-destructive repeated-read gate.

## 4. Fresh-read proof using one safe external change

This section proves the panel is reading current hardware rather than replaying cached values.

1. Stop playback.
2. Record the current DAC reconstruction filter.
3. Close/release EQ Library's Black Pearl USB session as needed so the independent controller can connect normally.
4. Change **only the DAC reconstruction filter** to a different known filter using the independent controller/tool.
5. Reconnect/open the Black Pearl in EQ Library through the normal USB flow.
6. Open **My DAC → DEVICE** and tap **Read candidate controls**.
7. Verify the displayed DAC filter now matches the externally changed hardware filter.
8. Verify firmware/version and all other unrelated displayed settings still match their prior values.
9. Restore the original DAC filter with the independent controller/tool, reconnect EQ Library, read again, and verify the original filter is reported.

Do not use gain mode, amp topology, or playback/global gain as the comparison change for this first read-only proof.

PASS / FAIL: **PASS**

Original filter: `Fast-PC` — independently confirmed active and restored

Temporary comparison filter: a different known reconstruction filter selected in the independent controller; exact label was not required for the pass because both changed-state and restored-state reads were explicitly verified by the project owner.

Notes: On 2026-09-13 the project owner stopped playback, changed only the DAC reconstruction filter outside EQ Library, reconnected the pinned candidate, and confirmed **Read candidate controls** reported the externally changed filter while firmware `0.6`, gain mode `HIGH`, topology `CLASS AB`, mic gain `+0.00 dB`, balance `Centered`, and playback/global gain `+2.00 dB` / raw `512` remained unchanged. The owner then restored the filter externally to `Fast-PC`, reconnected EQ Library, and confirmed a fresh read again reported `Fast-PC` with the unrelated values unchanged. This proves the panel is reading current hardware rather than replaying a cached filter value and confirms the baseline filter was restored.

## 5. Disconnect marks retained values stale

1. Complete a successful current-session read.
2. Disconnect the Black Pearl physically.
3. Verify **My DAC** remains available for the current app session rather than making navigation jump.
4. Verify the connection state becomes **Disconnected**.
5. Verify retained qualification values, if shown, are labeled **Last read · USB session changed** rather than current.
6. Verify **Read candidate controls** cannot perform a current read while disconnected.
7. Verify no cached value is written back to hardware because of disconnect/reconnect.

PASS / FAIL: **PASS**

Notes: On 2026-09-13 the project owner completed a current-session qualification read, physically disconnected the Black Pearl without closing EQ Library, and reported success for the required stale-state behavior. **My DAC** remained available, the device state changed to **Disconnected**, retained values were no longer presented as current and were marked as the prior/changed USB session, and **Read candidate controls** could not produce a successful current hardware read while the DAC was unplugged. No reconnect was performed as part of this step, so reconnect freshness remains a separate gate below.

## 6. Reconnect creates a new current session

1. Reconnect the same Black Pearl.
2. Complete the normal Android USB permission/open flow if required.
3. Before tapping the qualification read, verify prior retained values are not silently relabeled current merely because the DAC reappeared.
4. Tap **Read candidate controls**.
5. Verify the panel returns to **Current session read** only after the new read succeeds.
6. Compare the values again with the expected hardware state, including firmware/version.

PASS / FAIL: **PASS**

Notes: The reconnect sequence is now fully evidenced. A 4:10 screenshot supplied immediately after the project owner followed the explicit **Connect, but do not tap Read candidate controls** instruction shows the retained baseline still labeled **Last read · USB session changed** rather than current. A prior 4:03 screenshot then shows **Current session read** only after a fresh qualification read, with the expected baseline: firmware `0.6`, `Fast-PC`, `HIGH`, `CLASS AB`, mic `+0.00 dB`, `Centered`, playback/global `+2.00 dB` / raw `512`. Opening/reopening the USB session therefore does not itself promote old data to current; only the successful new hardware read does.

## 7. Read interruption / failure behavior

This is optional on hardware because automated tests cover session-generation change and field-read failure. Exercise only if it is convenient and safe.

1. With playback stopped, begin **Read candidate controls**.
2. If practical, disconnect the DAC during the read sequence.
3. Verify the app reports a failed/session-changed read rather than presenting a partial set as a successful current snapshot.
4. Reconnect and perform a fresh read.
5. Verify the app recovers without pushing cached settings to the DAC.

PASS / FAIL / NOT EXERCISED: **NOT EXERCISED**

Notes: This optional destructive-timing hardware exercise was intentionally not required. Automated read-only candidate/session tests already cover session-generation changes and field-read failures, including rejecting partial/current snapshots when the session changes. Those tests are green at the recorded behavior/software checkpoints.

## 8. Final no-write comparison

After all read-only tests, compare the Black Pearl with the independent controller/tool one final time.

Required result:

- firmware/version unchanged;
- DAC filter equals the intentionally restored baseline;
- gain mode unchanged;
- amp topology unchanged;
- mic gain unchanged;
- balance unchanged;
- playback/global gain unchanged except for an intentional external user change made during testing;
- no unexpected persistence/state change occurred because EQ Library read the device.

PASS / FAIL: **TBD**

Notes: `TBD`

## Qualification decision

A **read-only qualification PASS** requires:

- Sections 1–6 and 8 pass;
- Section 7 either passes or is explicitly marked **Not exercised**, with automated session/failure tests green;
- every displayed value agrees with independently observed hardware behavior;
- the external filter-change test proves the panel performs a fresh read;
- reconnect correctly invalidates old-session freshness;
- no Black Pearl setting changes as a side effect of EQ Library reads;
- the exact tested commit and signed APK are recorded above.

Overall read-only qualification: **IN PROGRESS**
