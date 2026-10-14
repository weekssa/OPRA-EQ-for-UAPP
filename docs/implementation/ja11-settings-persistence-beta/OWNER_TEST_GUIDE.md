# JA11 settings persistence beta owner guide

Status: **DRAFT — use only after the exact private beta APK and owner phone window are confirmed.**

This separate beta app tests two explicit actions: My DAC → DEVICE → Device volume → Apply, and My
DAC → DEVICE → EQ program → choose a different entry. Each changed action writes that one requested
setting, verifies it, sends one native Save for the verified active program, observes the DAC's
expected reconnect, and verifies the final device state. Save commits the current volume and
program together. The UI says **“Saves the current volume and EQ program. The DAC briefly
reconnects.”**

The beta is separate from stable EQ Library. Do not remove stable or clear its data. Installation
does not authorize a hardware mutation; wait until the app is installed and the owner is ready to
conduct the steps below. Keep FiiO and other apps that might claim the JA11 closed during a test.

## Owner-led session

1. Start with the owner-selected, supported JA11 in its original known state. In the beta's My DAC
   → DEVICE view, connect and read the current state. Stop before any change if the baseline is not
   current and complete. Note the visible original volume, program, microphone, and UAC values.
2. In Device volume, lower the volume conservatively from its baseline and tap Apply once. Wait for
   the beta's terminal result. Stop if the result is not verified or if the state differs from the
   displayed request.
3. Manually unplug and reconnect the JA11. Reopen/read DEVICE state. Confirm the new volume remains
   and the original program, microphone, and UAC state remain unchanged.
4. Select Vocal once, or Classic if Vocal was already active. Wait for terminal verification, then
   manually unplug/reconnect and confirm the selected program and current volume remain.
5. Test Off once if that specific payload still needs coverage and the prior volume case did not
   already Save while Off. Do not repeat an already accepted observation.
6. Restore the original volume and program through the same beta controls, avoiding unchanged
   actions. Manually unplug/reconnect and verify the original volume, program, microphone, and UAC
   state. Compare User 1 EQ/gain only when the beta report identifies the stored bank; otherwise
   retain `INSUFFICIENT_EVIDENCE` and do not infer restoration from built-in raw values.
7. Keep each readable or technical report, especially on failure. The report's counts are
   software-recorded; they do not establish USB bus packet counts or the number of firmware
   reboots. A USB detach does not itself prove that the DAC lost power.

Stop after the first failed verification, unexpected state change, repeated reconnect, or uncertain
result. Do not retry the operation or perform recovery writes. Preserve the report and actual
state for diagnosis before considering a separately authorized recovery action.
