# EW300 v0.7 consolidated hands-on checklist

This is the only planned physical session after the exact signed candidate passes every software
gate. Use the APK, checksum, signer, source SHA, and candidate manifest produced by that one
frozen CI run. Do not use `f8707788531cfdef33cd46d79c5c53e649480232` and do not run Save
qualification again.

## Stop conditions

Stop immediately, do not retry a mutation, and share readable plus JSON reports if there is an
unexpected permission prompt before documented post-Save replacement, competing-app chooser,
wrong identity, warning, crash, missing readback, failed restoration, `UNCERTAIN`, or a mismatch
between the APK checksum/signer and the manifest.

## Single-session sequence

1. Install or upgrade only the exact signed candidate; verify package, version, checksum, signer,
   and cold launch.
2. Connect the exact EW300 and run the read-only capability report. Verify the exact identity,
   five Peak bands, direct-Hz values, gain state, and `stateKnown=true`.
3. Open the editor, make one reviewed safe Peak change, and use Apply. Confirm operation report:
   zero permission requests before first write, one Save at most, no replay, and final readback.
4. Capture the verified readback as a Personal EQ and confirm playback gain is not part of the
   canonical captured EQ.
5. If Flash is enabled, flash one suitable profile. Confirm the same operation invariants and
   share both reports.
6. If the operation visibly re-enumerates, allow only the documented replacement authorization
   and readback path. Do not approve a competing application or repeat the mutation.
7. Remove all power, reconnect, and verify the exact saved state. Then use Reset only if the UI
   marks it available; verify flat readback and exact restoration of the qualified baseline.
8. Record the candidate, device fingerprint, operation outcome, reports, restored-state result,
   and stop. Do not merge, publish, or claim public support from this checklist alone.

The accepted E001 qualification supplies historical Save/persistence evidence and is not repeated;
this session validates the current software candidate and its shared session/reconnect behavior.
