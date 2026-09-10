# TRN Black Pearl — v0.6 My DAC read-only DEVICE qualification

Status: **PENDING PHYSICAL VALIDATION**

This checklist is the first physical gate for additional Black Pearl DEVICE capabilities in v0.6. It validates **read-only** behavior only. Passing this checklist does **not** authorize any new device-control write, normal production exposure, persistence claim, or expansion of the already-qualified EQ/Reset protocol.

Use this checklist only after the exact candidate commit has passed the required Android unit/lint/build/security/catalog gates and a signed APK has been produced from that exact source. Record the exact commit and signed artifact before testing.

## Scope

The candidate read-only panel currently reads, in one current USB session:

- DAC reconstruction filter;
- gain mode;
- amplifier topology;
- microphone gain;
- left/right balance;
- playback/global gain.

Firmware readback is **not** part of this first transaction and is not a pass criterion here.

No candidate DEVICE write command exists in this increment. The panel is explicitly labeled **Hardware qualification · read-only** and must not latch, save, or change any setting.

## Test setup

- Android device: Pixel 9
- DAC: TRN Black Pearl
- Install the signed v0.6 candidate built from the exact recorded commit.
- Use a trusted Black Pearl controller/tool only as an independent observation source for the same settings; do not leave two controllers actively connected to the DAC at the same time.
- Stop playback before changing any comparison setting with another controller.
- Keep listening volume conservative before reconnecting or resuming playback.
- Do not perform firmware, bootloader, identity, or raw-register operations.

Record before testing:

- Candidate commit SHA: `TBD`
- Signed APK/artifact name: `TBD`
- Signed APK SHA-256: `TBD`
- Signer certificate SHA-256: `TBD`
- Android CI run: `TBD`
- CodeQL run: `TBD`
- Test date: `TBD`
- Tester: Project owner

## Baseline record

Before opening the qualification panel, record the current values with the independent controller/tool:

- DAC filter: `TBD`
- Gain mode: `TBD`
- Amp topology: `TBD`
- Mic gain: `TBD`
- Balance: `TBD`
- Playback/global gain: `TBD`

If the independent tool expresses playback level in a different unit, record both its displayed value and any raw value it exposes. EQ Library currently displays playback/global gain in dB plus the native raw integer.

## 1. Recognition, connection, and read-only presentation

1. Start EQ Library with the Black Pearl disconnected.
2. Connect the Black Pearl and use the normal Android USB permission flow if prompted.
3. Open **My DAC** and select the **DEVICE** tab.
4. Verify the header identifies **TRN Black Pearl** and reports the connection state textually.
5. Verify the page shows device information plus **Hardware qualification · read-only**.
6. Verify the explanation states that no setting is changed, latched, or saved.
7. Verify there are no new normal Black Pearl DEVICE setting controls for filter, gain mode, topology, balance, microphone gain, or playback level.

PASS / FAIL: **TBD**

Notes: `TBD`

## 2. First complete hardware read

1. With the Black Pearl connected, tap **Read candidate controls** once.
2. Verify the panel reports **Current session read**.
3. Compare every displayed value against the independent baseline record:
   - DAC filter;
   - gain mode;
   - amp topology;
   - mic gain;
   - balance;
   - playback/global gain.
4. Verify no field is silently substituted with a default or guessed value.
5. If EQ Library reports an inconsistent left/right balance state, stop and record the exact values rather than treating it as a centered/valid balance.

PASS / FAIL: **TBD**

Observed EQ Library values:

- DAC filter: `TBD`
- Gain mode: `TBD`
- Amp topology: `TBD`
- Mic gain: `TBD`
- Balance: `TBD`
- Playback/global gain dB: `TBD`
- Playback/global gain raw: `TBD`

Notes: `TBD`

## 3. Repeated reads are stable and non-destructive

1. Without changing any DAC setting, tap **Read candidate controls** at least three times.
2. Verify the same hardware state is reported each time.
3. Re-check the same settings with the independent controller/tool after closing/releasing EQ Library's USB session as needed.
4. Verify EQ Library's reads did not change DAC filter, gain mode, topology, mic gain, balance, or playback/global gain.
5. Verify no save/persistence action was triggered merely by reading.

PASS / FAIL: **TBD**

Notes: `TBD`

## 4. Fresh-read proof using one safe external change

This section proves the panel is reading current hardware rather than replaying cached values.

1. Stop playback.
2. Record the current DAC reconstruction filter.
3. Close/release EQ Library's Black Pearl USB session as needed so the independent controller can connect normally.
4. Change **only the DAC reconstruction filter** to a different known filter using the independent controller/tool.
5. Reconnect/open the Black Pearl in EQ Library through the normal USB flow.
6. Open **My DAC → DEVICE** and tap **Read candidate controls**.
7. Verify the displayed DAC filter now matches the externally changed hardware filter.
8. Verify all unrelated displayed settings still match their prior values.
9. Restore the original DAC filter with the independent controller/tool, reconnect EQ Library, read again, and verify the original filter is reported.

Do not use gain mode, amp topology, or playback/global gain as the comparison change for this first read-only proof.

PASS / FAIL: **TBD**

Original filter: `TBD`

Temporary comparison filter: `TBD`

Notes: `TBD`

## 5. Disconnect marks retained values stale

1. Complete a successful current-session read.
2. Disconnect the Black Pearl physically.
3. Verify **My DAC** remains available for the current app session rather than making navigation jump.
4. Verify the connection state becomes **Disconnected**.
5. Verify retained qualification values, if shown, are labeled **Last read · USB session changed** rather than current.
6. Verify **Read candidate controls** cannot perform a current read while disconnected.
7. Verify no cached value is written back to hardware because of disconnect/reconnect.

PASS / FAIL: **TBD**

Notes: `TBD`

## 6. Reconnect creates a new current session

1. Reconnect the same Black Pearl.
2. Complete the normal Android USB permission/open flow if required.
3. Before tapping the qualification read, verify prior retained values are not silently relabeled current merely because the DAC reappeared.
4. Tap **Read candidate controls**.
5. Verify the panel returns to **Current session read** only after the new read succeeds.
6. Compare the values again with the expected hardware state.

PASS / FAIL: **TBD**

Notes: `TBD`

## 7. Read interruption / failure behavior

This is optional on hardware because automated tests cover session-generation change and field-read failure. Exercise only if it is convenient and safe.

1. With playback stopped, begin **Read candidate controls**.
2. If practical, disconnect the DAC during the read sequence.
3. Verify the app reports a failed/session-changed read rather than presenting a partial set as a successful current snapshot.
4. Reconnect and perform a fresh read.
5. Verify the app recovers without pushing cached settings to the DAC.

PASS / FAIL / NOT EXERCISED: **TBD**

Notes: `TBD`

## 8. Final no-write comparison

After all read-only tests, compare the Black Pearl with the independent controller/tool one final time.

Required result:

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

Overall read-only qualification: **TBD**

Blocking observations: `TBD`

## What passing this checklist permits

A pass raises only these exact read semantics from corroborated candidate to physically observed read-only behavior on the tested Pixel 9 / TRN Black Pearl combination. It does **not** automatically make the corresponding controls writable in production.

Any later write-capable control must be qualified individually with its own safe sequence:

`fresh read → explicit user intent → validate → write only the owned value → readback → verify → persistence/reconnect check where relevant`

Level-sensitive controls require additional volume-safety handling. Production wording must not claim persistence until it has been specifically proven.
