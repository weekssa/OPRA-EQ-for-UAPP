# JA11 v0.8.1 physical acceptance plan

Status: **blocked on off-phone gates and fresh owner confirmation**. This plan authorizes no current phone action.

## Candidate and window gate

Before requesting the Pixel, finish G0-G11 in [the gate matrix](../JA11_V081_GATE_MATRIX.md), freeze the exact candidate tuple, and report **CLEAN v0.8.1 CANDIDATE READY**. Then send exactly `PHONE WINDOW READY — PIXEL + JA11 NEEDED` with the tuple, planned operations, expected duration, and restoration promise. Wait for the owner's confirmation.

No Pixel or ADB command is permitted before that confirmation. Use a private evidence directory with mode `700`; do not store device serials, fingerprints, screenshots, or raw reports in Git.

## Prepare the exact app

1. Refresh ADB discovery only after the window is confirmed. Select one explicit Pixel serial. Verify `Google` / `Pixel 9` with `phone-session.sh`.
2. Set helper pins from the frozen candidate sidecar: APK path and SHA-256, signer SHA-256, version/versionCode, and the exact expected prior diagnostic APK SHA/signer/version if installed.
3. Run `phone-session.sh <serial> install`. The helper verifies/preserves a known installed diagnostic APK before `adb install -r`; if absent, it uses plain install. Any failed or ambiguous package query or prior-artifact mismatch stops before installation.
4. Run `verify-install`, then `launch`. In the app, open **My DAC** and tap **Connect**. Do not use the notification shade to infer USB state.
5. Verify the app shows the exact candidate identity and the JA11 is connected. Capture one screenshot and retain the app-generated operation reports privately.

## Read-only baseline

Before any JA11 write, complete and save a current read-only snapshot for the exact candidate:

- Exactly one supported JA11 candidate, supported VID/PID, permissioned claimed USB session, and current session generation.
- Optional serial status. If both sessions in a later expected restart expose usable serials, mismatch fails. If serial is absent, proceed only with the sole candidate and every detach/fresh-session/readback gate; do not claim same-unit proof.
- Mic/headset setting, UAC mode, active program, output volume, sample-rate label, firmware if readable, and the complete stored User 1 bands plus global EQ gain.
- Candidate source/app version and a screenshot of the visible values. Keep the complete value record and any app-generated shareable report private.

Do not interpret Off as an empty User 1 bank. If the app cannot provide the complete stored EQ bank, if the session is stale, if candidate count is not one, or if any baseline field required for restoration is unknown, stop before mutation and release the Pixel.

Record the fresh live baseline as the only restoration authority. Historical state values are not assumed current.

## Test A — Mic expected reset and restoration

- If the fresh baseline says Mic is Off, restore Mic to On as the first JA11 mutation. If it is already On, skip the write and record restoration satisfied.
- For an Off-to-On write, require one accepted transfer, an observed expected detach, old-session invalidation, fresh permission/open/claim, a new generation, one supported returning JA11, and authoritative fresh Mic-On readback.
- Compare serials only when both sessions expose them; mismatch stops the run. No uncertain write is retried.
- Verify unrelated DEVICE and EQ baseline values again. A permission prompt should be accepted immediately, using “Always allow” if offered.

## Test B — UAC round trip

Change UAC once to the alternate supported mode, verify the expected PID transition on one fresh session with authoritative UAC readback, then return to the baseline mode and verify again. Stop after any uncertain write, timeout, ambiguity, serial mismatch when both serials exist, or readback failure. Do not resend either write.

## Test C — one Flash from Off

1. Preserve the complete stored User 1 baseline before proceeding. Prepare a temporary EQ target from those exact five bands and the exact global gain. Import/review it in the app and continue only if the target is represented as **Exact** and the six values match the baseline. If that cannot be established, skip Flash and release safely.
2. If the active program is not Off, select Off once and verify it. This establishes the required Flash-from-Off starting state.
3. Start exactly one Flash action. The required order is: select User 1; read back User 1; write five bands and global gain; Apply; verify volatile state; send exactly one Save; handle the bounded post-Save state-event window; perform authoritative final readback.
4. If the JA11 detaches during Save/final readback, invalidate the old session, reacquire one supported candidate, open a fresh generation, compare serials if both exist, and retry final readback only. Never repeat bands, gain, Apply, or Save.
5. Pass this test only when the exact final readback matches the preserved User 1 baseline. Preserve the app-generated report and final-state screenshot privately. If final readback fails or is uncertain, do not Flash again; perform read-only state capture, restore only through a known safe path, and stop.
6. Restore the original active program if Flash changed it, with fresh readback. The temporary Personal EQ/profile is removed only after its evidence is captured and the hardware state is safe.

## Test D — required volume/program power-removal observation

Run only if Test C passed and the fresh baseline plus all earlier restorations remain known. Use one controlled JA11 unplug/reconnect to remove USB power. Before unplugging, set volume and active program to safe values that differ from the original baseline and verify them. After replugging, reconnect through My DAC and read both values from the fresh session. Record the observed values without inferring persistence from another candidate. Restore the exact baseline volume and active program and verify them.

Do not repeat the unplug/reconnect to obtain a preferred outcome. If session identity, permissions, candidate count, or readback is uncertain, stop and do not write restoration values blindly.

## Final restoration and release

1. Read the complete final baseline fields in a fresh current session: Mic, UAC/PID, active program, volume, sample rate, firmware if readable, all five User 1 bands, and global gain.
2. Require every changed value to match the newly recorded original baseline. Do not claim persistence for volume or program beyond the actual observation.
3. Save private reports/screenshots, remove only the temporary test profile, and leave the verified candidate package state recorded. Do not uninstall or roll back software while hardware state is uncertain.
4. Release the Pixel immediately after sufficient evidence and restoration are verified. Report `PHONE RELEASED — YOU CAN TAKE THE PIXEL BACK`.

Expected occupancy is about 25–35 minutes if the existing wireless-debugging connection is already online and permission is granted promptly. If setup or pairing recovery becomes prolonged, stop before a JA11 write, preserve evidence, and release the phone.

## Stop conditions

Stop mutation on a wrong app/source/artifact, incomplete baseline, multiple supported JA11 candidates, stale/closed session, denied or unresolved permission, serial mismatch when both serials exist, uncertain transfer, failed authoritative readback, unexpected state change, or loss of required evidence. Never retry an uncertain write or Save. Restore only from the exact baseline when the hardware state and session are sufficiently known.
