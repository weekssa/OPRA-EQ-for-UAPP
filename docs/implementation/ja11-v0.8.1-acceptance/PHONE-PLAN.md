# JA11 v0.8.1 physical acceptance plan

Status: **J029 Test C passed; Pixel released; no Test C restoration outstanding**. J029 captured both report payloads for one direct Flash on production source `9493cf030acb440f92e547fc667f6a5399616045` using the exact diagnostic APK SHA `44040493c7263ae7d7e6f41be7389534d07977427cc8002494097fbf3c868848`. The reports prove User 1 selection/readback before writes, exactly five band writes, one gain write, one Apply, one Save, no replay, fresh generation after detach, and authoritative final readback. The starting Mic On, UAC 2.0, volume 60/60, sample rate 384 kHz, and full User 1 EQ/gain were restored. The temporary profile/source file were removed; Pixel was released. J028 remains a separate historical record with an unverified Save count; do not relabel it or repeat Flash to recover its missing report. No general power-cycle PEQ persistence or same-unit continuity claim follows.

## Candidate and window gate

J029 completed the previously planned Test-C-only session. The physical window is closed. Any future hardware session requires a concrete evidence invalidation, applicable gates, a frozen exact candidate tuple, and a new owner-confirmed phone window. Do not resume J028 or J029 to reconstruct or improve evidence.

The phone was authorized and released after J029. Keep the private evidence directory outside Git; do not store device serials, fingerprints, screenshots, or raw reports in the repository.

## Prepare the exact app

1. Refresh ADB discovery only after the window is confirmed. Select one explicit Pixel serial. Verify `Google` / `Pixel 9` with `phone-session.sh`.
2. Set helper pins from the frozen candidate sidecar: APK path and SHA-256, signer SHA-256, version/versionCode, and the exact expected prior diagnostic APK SHA/signer/version if installed.
3. Run `phone-session.sh <serial> install`. The helper verifies/preserves a known installed diagnostic APK before `adb install -r`; if absent, it uses plain install. Any failed or ambiguous package query or prior-artifact mismatch stops before installation.
4. Run `verify-install`, then `launch`. In the app, open **My DAC** and tap **Connect**. Do not use the notification shade to infer USB state.
5. Verify the app shows the exact candidate identity and the JA11 is connected. Capture one baseline screenshot. Before any mutation, confirm that a local/private share destination is ready for both the readable report and JSON report; if not, stop before Flash.

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

## Test C — executed once in J029; retain procedure for invalidation only

J029 followed this one-Flash procedure. Do not repeat Test A (Mic), Test B (UAC), Test D (power removal/volume/program), or Test C absent a concrete invalidation. The report hashes and verification summary are in the compact handoff and validation ledger; full report payloads remain in the private evidence directory listed there.

1. Require the newly gated diagnostic APK with the separate **JA11 Flash Report** launcher icon. The diagnostic Application observes the existing MainActivity ViewModel and retains the completed direct-`FLASH` trace in memory until explicitly dismissed. The report Activity is a separate launcher entry, not a My DAC screen control. The existing trace already contains ordered raw request/response events, Save count, and per-event session/detach generations. Do not force-stop, clear data, or otherwise end the app process before exporting.
2. Save the live, read-only baseline, including active program, complete User 1 bands and global gain, Mic, UAC, volume, and candidate/session identity. Prepare the intended Test C target from that exact User 1 bank and gain; require **Exact** representation and confirm all six target values equal the fresh readback. If any required baseline or identity value is unknown, the target cannot represent the bank exactly, or the target differs from the readback, stop before mutation. If fresh Mic readback is Off, restore Mic to On as the first JA11 mutation under Test A's expected-reset gate: invalidate the old session, accept any permission promptly, open/claim one supported JA11 on a fresh current generation, and compare serials only if both sessions expose usable serials. After reconnect, require authoritative fresh Mic-On readback and re-read the full unrelated DEVICE and EQ baseline (UAC, program, volume, sample rate, firmware if readable, five User 1 bands, and global gain) before continuing. If identity/session is ambiguous, any unrelated value changed, or any value cannot be verified, stop. This is required state restoration, not a repeated Mic test; **Mic On becomes the required final Mic state and must not be changed back to Off**. If Mic is already On, make no Mic write. Do not perform a UAC or power-cycle test.
3. If active program is not Off, select Off once and verify it. Record the original active program so it can be restored after the test.
4. Start exactly one direct Flash. Do not tap Flash again. In the exported JSON event list, verify one `WRITE 0x16` User 1 selection, followed by a successful `READ 0x16` returning User 1 before the first EQ data write; five `WRITE 0x15` band writes with indices 0–4 exactly once each; one `WRITE 0x17` global-gain write; one `WRITE 0x18` Apply; and one successful `WRITE 0x19` Save with `saveCommandCount=1`. Require one operation ID, contiguous event sequence numbers, and no repeated EQ write, Apply, or Save events.
5. Immediately after Flash completes, go Home and open the separate **JA11 Flash Report** launcher icon. Export **Share technical report (JSON)** and **Share operation report** to the preselected private destination. Keep both artifacts paired by `operationId`; do not store raw reports in Git. Require `outcome=Success`, `stateKnown=true`, `comparisonPhase=FINAL_READBACK`, and `FINAL_READBACK` plus `VERIFIED` stages. A missing report is not recovered by repeating Flash.
6. Verify reconnect evidence in the event list. If Save caused a detach, the Save event must be on the old generation and the final successful readback events must be on the replacement generation/detach count. Any failed old-session reads must remain visible; no post-Save write may appear. Require successful final program, five-band, and global-gain reads; decode the raw final band responses and compare all six values with the intended User 1 target. A successful report without the required raw events is incomplete.
7. If the report is missing, incomplete, or fails any count/order/readback check, preserve it and stop. Do not repeat Flash or Save. Read current state; restore the original active program only if it changed and the active session/value are known, then verify it. If the final EQ differs from baseline or cannot be authoritatively verified, do not attempt a second EQ write/Flash/Save in this one-run session; record EQ restoration as outstanding, preserve evidence, and release after read-only capture. If any other state is uncertain, do not write blindly. On a passing Test C, also verify the target EQ still matches the recorded User 1 baseline. On every outcome, verify Mic is On if it began Off (never restore it to Off), otherwise require it still matches the On baseline; remove the temporary profile if its safe removal is known; and release the Pixel. No UAC, Mic round-trip, volume, or power-cycle test is part of this plan.

## Test D — completed volume/program power-removal observation (no rerun planned)

Owner-reported official FiiO app reference behavior classifies volume and built-in program changes as runtime controls: they do not reset/re-enumerate and show no Save-style behavior. Do not add Save behavior to these controls. Their return to defaults after true power loss may be intended firmware behavior rather than an OPRA EQ defect. This observation is behavioral reference only, not USB packet evidence; Test D records observed values and does not infer a persistence contract.

Run only if Test C passed and the fresh baseline plus all earlier restorations remain known. Use one controlled JA11 unplug/reconnect to remove USB power. Before unplugging, set volume and active program to safe values that differ from the original baseline and verify them. After replugging, reconnect through My DAC and read both values from the fresh session. Record the observed values without inferring persistence from another candidate. Restore the exact baseline volume and active program and verify them.

Do not repeat the unplug/reconnect to obtain a preferred outcome. If session identity, permissions, candidate count, or readback is uncertain, stop and do not write restoration values blindly.

## Final restoration and release

1. Read the complete final state in a fresh current session: Mic, UAC/PID, active program, volume, sample rate, firmware if readable, all five User 1 bands, and global gain.
2. Require every changed value to match its authorized final target: Mic On if the fresh starting state was Off, otherwise the unchanged On baseline; every other setting must match the newly recorded original baseline. Do not claim persistence for volume or program beyond the actual observation.
3. Save private reports/screenshots, remove only the temporary test profile, and leave the verified candidate package state recorded. Do not uninstall or roll back software while hardware state is uncertain.
4. Release the Pixel immediately after sufficient evidence and restoration are verified. Report `PHONE RELEASED — YOU CAN TAKE THE PIXEL BACK`.

Expected occupancy is about 15–20 minutes after the exact diagnostic candidate is installed and verified, with the wireless-debugging connection and permission ready. This covers one read-only baseline, a Mic restoration only if fresh state requires it, one Flash/Save reconnect, two report exports, and restoration. If setup, permission, or reconnect recovery becomes prolonged, stop without replaying a mutation, preserve evidence, and release the phone.

## Stop conditions

Stop mutation on a wrong app/source/artifact, incomplete baseline, multiple supported JA11 candidates, stale/closed session, denied or unresolved permission, serial mismatch when both serials exist, uncertain transfer, failed authoritative readback, unexpected state change, or loss of required evidence. Never retry an uncertain write or Save. Restore only from the exact baseline when the hardware state and session are sufficiently known.
