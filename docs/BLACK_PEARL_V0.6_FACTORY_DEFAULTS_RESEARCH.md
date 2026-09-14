# TRN Black Pearl v0.6 — factory-default/reset research

Status: **RESET UX APPROVED; FACTORY-DEFAULT SEMANTICS NOT YET ESTABLISHED — DO NOT EXPOSE THE ACTION**

Purpose: determine whether EQ Library can truthfully expose the approved My DAC -> DEVICE **Reset to factory defaults** action without inventing a reset command or default values.

This is clean-room observable-behavior research. Public third-party implementations with incompatible licenses are evidence sources only; implementation code must not be copied.

## Approved UI gate

The common reset UX is defined in `docs/V0.6_DEVICE_UX_POLISH_APPROVED.md`. Black Pearl must not show that action until this document can establish the exact model/firmware reset semantics strongly enough for a safe transaction and truthful wording.

Existing `Reset EQ to flat` is separate and remains qualified.

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

## Current conclusion

There is not yet enough evidence to claim an exact Black Pearl **factory reset**.

Specifically:

1. no distinct hardware factory-reset command has been independently established;
2. the strongest public controller called `Factory Reset` appears to implement a convenience sequence of normal writes;
3. that sequence conflicts with independent reports of the Black Pearl's shipped/initial gain and amplifier state;
4. the sequence does not clearly reset every currently supported DEVICE control;
5. official public material reviewed so far does not resolve the complete value set or EQ inclusion semantics.

Therefore:

- **Do not expose `Reset to factory defaults` on Black Pearl yet.**
- Do not use the qualification baseline (`63%`, Fast-PC, HIGH, CLASS AB, etc.) as defaults merely because those values were observed during testing.
- Do not use the third-party controller's 50% / FAST-LL / LOW / CLASS H sequence as factory defaults merely because its button is labeled Factory Reset.
- Keep the approved generic reset UX/capability architecture ready for a future exact model contract.

## What would clear the gate

Any one sufficiently strong path, preferably corroborated, may clear the product gate:

- official TRN/Walk Play documentation or observable official-app behavior that defines Black Pearl factory reset/default values and EQ behavior;
- an independently verified hardware reset operation with complete before/after reads across all supported controls and EQ;
- a documented exact default table plus a safe EQ Library-owned multi-control restore transaction, if there is no native reset command.

When evidence clears the gate, implementation still requires a fresh complete baseline, a prevalidated reset plan, safe device-specific ordering, no automatic write retry, complete post-reset readback, and success only after the requested reset state is verified.
