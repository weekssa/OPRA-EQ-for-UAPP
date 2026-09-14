# TRN Black Pearl v0.6 — factory-default/reset research

Status: **TRUE TRN FACTORY-DEFAULT SEMANTICS REMAIN UNESTABLISHED; OWNER-SELECTED EQ LIBRARY RESTORE PRESET APPROVED, IMPLEMENTED, AND PHYSICALLY QUALIFIED**

Purpose: preserve the research boundary around a true Black Pearl factory reset while documenting the later project-owner decision to expose a truthful EQ Library-owned **Restore defaults** action using an explicitly selected set of values.

This is clean-room observable-behavior research. Public third-party implementations with incompatible licenses are evidence sources only; implementation code must not be copied.

## Current product decision

The project owner explicitly approved a Black Pearl **EQ Library restore preset** rather than claiming that the values are verified TRN factory defaults.

The v0.6 restore targets are:

- playback volume: **50% presentation** using the already-qualified whole-dB DEVICE write that displays as 50%;
- DAC filter: **FAST-LL**;
- gain: **HIGH**;
- amplifier: **CLASS AB**;
- balance: **centered**;
- microphone gain: **0 dB**.

The confirmation dialog retains an optional **Also reset EQ to flat** checkbox, OFF by default. When selected, it reuses the separately qualified `Reset EQ to flat` transaction. Saved EQs in EQ Library are not changed.

Because this is an owner-selected hybrid preset, the UI must use truthful wording such as **Reset device**, **Restore defaults**, and **EQ Library's Black Pearl defaults**. It must not call this a TRN factory reset or imply that TRN supplied or endorsed these values.

## Restore transaction contract

The restore action does not invent a new Black Pearl opcode. It reuses the already-established individual DEVICE write/readback/persistence path.

The device-settings sequence is intentionally conservative:

1. establish the qualified minimum whole-dB DEVICE level (`-37 dB`) as a fail-safe intermediate playback state;
2. establish FAST-LL;
3. establish centered balance;
4. establish microphone gain **0 dB**;
5. establish CLASS AB;
6. establish HIGH gain;
7. establish the whole-dB playback value (`-6 dB`) that presents as **50%** in the Black Pearl percentage UI.

Each selected target is submitted through the normal fresh-baseline/write/readback path. When a target already matches the fresh hardware read, the normal repository correctly treats it as a verified no-op rather than sending a redundant setting write. When a target differs, it is written once, verified live, persisted with the established device save command, then verified again after save.

A completed restore is guaranteed to exercise DEVICE persistence because the fail-safe playback stage and the final 50%-presentation playback stage are intentionally different values. At least one of those playback targets must differ from the starting/current stage during the completed sequence, and the Black Pearl save command persists the current DEVICE settings block. This preserves the no-redundant-write rule while still establishing a persisted final restore state.

Setting writes are never automatically retried. A disconnect, stale session, transfer failure, readback mismatch, or unrelated-state change stops the restore and must not be presented as success.

The optional EQ-flat action remains a separate transaction with its own already-qualified fail-safe ordering. Microphone gain is explicitly restored through the same qualified normal DEVICE control path at **0 dB**.

## Evidence reviewed

### TRN product page

Official product page:

`https://trn-audio.com/trn-black-pearl.html`

The page establishes Black Pearl product capabilities and Walk Play linkage but does not document a hardware factory-reset command, a complete factory-default value set, or whether a factory reset includes EQ.

### Walk Play public site

Public Walk Play site:

`https://www.szwalkplay.com/`

The site describes EQ/device-setting persistence and normal adjustment features. It does not publish an exact Black Pearl factory-reset transaction/default table that can be used as an implementation contract.

### Independent Black Pearl Android controller

Reviewed repository/commit already used elsewhere as observable-behavior corroboration:

`https://github.com/cheesyserg/BlackPearlControl-Android/tree/491e9d5131562d85b44ce9fd741f3e1ff5c4781c`

Its user-facing **Factory Reset** flow is observable as a sequence of ordinary setting writes rather than a distinct reset opcode. At that reviewed commit the sequence requests approximately:

- playback volume: 50% presentation;
- DAC filter: FAST-LL;
- gain: LOW;
- amplifier: CLASS H;
- balance: centered;
- local Flat EQ followed by the normal EQ latch/save path.

No microphone-gain reset is apparent in that sequence even though the dialog describes broad hardware reset behavior. This makes the sequence insufficient by itself as a complete hardware factory-default contract.

Do not copy implementation code from this GPL-licensed project.

### pyBlackPearl

Reviewed repository/current source:

`https://github.com/cheesyserg/pyBlackPearl`

No separate Black Pearl factory-reset operation/default-value contract was found in the reviewed source. Existing EQ/apply/save functionality does not establish whole-device factory defaults.

### Independent reports of shipped/initial state

Independent Black Pearl reviews conflict with the controller reset sequence on important values. Examples:

- `https://bisonicr.ldblog.jp/archives/56048413.html` reports the unit's initial setting as **HIGH** gain and **CLASS AB**.
- `https://note.com/moco0422/n/n65bbc0162f97` independently reports initial **HIGH** gain.

These reports are not sufficient to define every factory default, but the conflict is enough to reject the LOW / CLASS H third-party reset sequence as a proven factory-default set.

## Research conclusion

There is still not enough evidence to claim an exact Black Pearl **factory reset**.

Specifically:

1. no distinct hardware factory-reset command has been independently established;
2. the strongest public controller called `Factory Reset` appears to implement a convenience sequence of normal writes;
3. that sequence conflicts with independent reports of the Black Pearl's shipped/initial gain and amplifier state;
4. the sequence does not clearly reset every currently supported DEVICE control;
5. official public material reviewed so far does not resolve the complete value set or EQ inclusion semantics.

Therefore the historical restrictions still apply to any claim about TRN factory state:

- do not label the EQ Library preset as `TRN factory defaults`;
- do not use the qualification baseline (`63%`, Fast-PC, HIGH, CLASS AB, etc.) as factory evidence merely because those values were observed during testing;
- do not describe the third-party controller's 50% / FAST-LL / LOW / CLASS H sequence as verified factory state merely because its button is labeled Factory Reset.

The later owner decision changes the **product action**, not the evidence classification: EQ Library may expose its explicitly defined restore preset with truthful app-owned wording.

## Focused physical qualification — PASS

Exact signed source and physical-test pin:

`b0340842dd88dc85613d9441fcc72ceadf877b20`

Immutable signed APK:

`https://raw.githubusercontent.com/weekssa/OPRA-EQ-for-UAPP/mobile-test-apk/candidates/EQ-Library-v0.6.0-beta-b034084.apk`

APK SHA-256:

`dcae4f54881976af70e93c2c796b6993697d1a0a8f333698e9d925933c61ff37`

Signer certificate SHA-256:

`65c1c1256dae3c49e3548f334c91f0ba991969e9be9e0b223ba4e253d2114747`

On **2026-09-14**, the project owner completed the focused Pixel 9 / TRN Black Pearl test and reported **SUCCESS**. Evidence category: **OWNER-REPORTED**.

The owner-reported PASS covered:

- Cancel performed no DEVICE or EQ change;
- with **Also reset EQ to flat** OFF, the device ended at displayed 50% volume, FAST-LL, HIGH, CLASS AB, centered balance, and **0 dB microphone gain**;
- the current non-flat hardware EQ remained unchanged with the checkbox OFF;
- after disconnect/reconnect, the restored DEVICE values remained present after fresh automatic read;
- with the checkbox ON, the same DEVICE targets were established and the hardware EQ became Flat;
- saved EQs in EQ Library remained untouched;
- the restored DEVICE state and Flat EQ persisted through reconnect.

**Result: PASS.** This qualifies the **EQ Library Restore defaults** behavior on exact signed `b0340842`. It still does not prove that the selected values are TRN's original factory defaults.

Documentation-only commits after this pass do not replace the hardware evidence pin unless they change connection ownership, read timing, write sequencing, persistence, reset values/order, EQ-reset behavior, verification, or session semantics.

## What would establish a true factory contract later

Any one sufficiently strong path, preferably corroborated, may establish a separate true-factory capability in the future:

- official TRN/Walk Play documentation or observable official-app behavior that defines Black Pearl factory reset/default values and EQ behavior;
- an independently verified native hardware reset operation with complete before/after reads across all supported controls and EQ;
- a documented exact factory default table plus a safe EQ Library-owned restore transaction, if there is no native reset command.

Any future true-factory implementation would still require a fresh complete baseline, a prevalidated reset plan, safe device-specific ordering, no automatic write retry, complete post-reset readback, and success only after the requested reset state is verified.
