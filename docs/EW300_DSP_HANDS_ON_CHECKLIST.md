# EW300 v0.7 bounded hands-on checklist

Use this only after one exact signed candidate has passed every required software, security, signing,
installation, cold-launch, and candidate-publication gate. The APK, source SHA, APK SHA-256,
artifact digest, package/version, and signer must all match that candidate's recorded provenance.
Do not use an older APK and do not repeat the accepted E001 Save qualification.

## Authorized device boundary

Mutation is authorized only for this exact fingerprint:

```text
vid=31b2|pid=111|manufacturer=LE XIAN|product=SIMGOT EW300 DSP|serial=2024-07-03-0000-0000-0000|interface=3
```

Any different VID/PID, manufacturer, product, serial, interface, revision evidence, or unexplained
identity mismatch is a stop condition. Do not test an unknown revision.

## Current authorized signed candidate

Use only this exact candidate for the bounded session:

| Item | Value |
| --- | --- |
| Source commit | `02f2697e20dda0c33f72973cb140a3be5d592cef` |
| APK | `EQ-Library-v0.7.0-beta-02f2697.apk` |
| APK SHA-256 | `39737d9a2aeddc6c2db1e4724c3875286bf2c2236cf858f88f5b98e854ed8314` |
| Package/version | `com.weekssa.opraeqforuapp`, `versionCode 7`, `versionName 0.7.0` |
| Signer SHA-256 | `65C1C1256DAE3C49E3548F334C91F0BA991969E9BE9E0B223BA4E253D2114747` |
| Signed artifact digest | `sha256:0b2460e79d09d915c770f6d609e80706772ed86c0dea3856a36c4188da7e623e` |
| Signed workflow | [#1314](https://github.com/weekssa/OPRA-EQ-for-UAPP/actions/runs/35761742008) |
| Immutable APK | `https://raw.githubusercontent.com/weekssa/OPRA-EQ-for-UAPP/mobile-test-apk/candidates/EQ-Library-v0.7.0-beta-02f2697.apk` |

This candidate passed the complete software, security, signing, installation, cold-launch, and
temporary candidate-publication gates. The accepted physical E037-E040 operation set is tied to
source 381; this current UX/control-visibility follow-up has not been physically retested and
does not alter the EW300 transaction implementation. Use this candidate for one bounded final
session to verify current Flash UI availability and export current operation evidence. Do not
repeat Save qualification or broaden the protocol scope. The EW300 Device tab’s additional state
is read-only; do not use it as evidence for unsupported Black Pearl-style controls or native
shelf hardware support.

## Accepted source-381 evidence (historical; not current-head retest)

The paired readable/JSON reports already accepted in E037-E040 show:
- `APPLY`: 10 writes, one Save, zero permission requests before the first write, replacement identity matched, final readback matched, `outcome=Verified`.
- `FLASH`: 11 writes, one Save, zero permission requests before the first write, replacement identity matched, final readback matched, `outcome=Success`.
- exact-baseline `RESTORE`: 11 writes, one restoration Save, exact replacement/generation, final exact readback, `restorationVerified=true`.
- `RESET`: 11 writes, one Save, exact replacement/generation, final readback matched, `outcome=Success`.

Replay and competing-job values are unmeasured/null. The Reset report does not contain raw before/after
register values, so do not claim independent playback/global-gain preservation from it alone. The
current candidate is the final UX/validation candidate for one bounded owner session; no additional
Save qualification is authorized.

## Before the single mutation

1. Verify the exact candidate provenance: source SHA, APK filename, APK SHA-256, artifact digest,
   package/version, and signer.
2. Install/cold-launch only that candidate.
3. Connect only the exact qualified EW300 and run a read-only capability report.
4. Record the complete pre-test hardware state needed for exact restoration, including all five
   qualified Peak-band register pairs and playback/global-gain register `0x66`.
5. Confirm the report is readable, `stateKnown=true`, and matches the exact authorized identity.
6. If the exact restoration path for that recorded baseline is not available and understood, stop.
   Do not perform the mutation merely to gather more evidence.

## Single operation

Perform exactly one reviewed **Flash** of one already-suitable five-band Peak profile through the
normal EQ Library Flash entry point. Do not run Apply first, do not repeat Save qualification, do not
run a second Flash as a retry, and do not exercise unrelated DEVICE controls.

Expected successful operation evidence:

- exact qualified fingerprint;
- zero permission requests before the first write;
- the expected bounded register-write count for the selected representation;
- exactly one Save command;
- no automatic mutation replay;
- no competing connection job, or an explicitly measured report explaining the value;
- if detach/re-enumeration occurs, `WAITING_FOR_REPLACEMENT` followed by exact replacement identity
  verification and a strictly newer session generation;
- `FINAL_READBACK` reached on the current authorized session;
- final hardware readback matches the requested representation;
- `stateKnown=true`, `outcome=Success`, and terminal `VERIFIED` evidence;
- readable and JSON operation reports tied to the exact APK/source candidate.

A read-only capability PASS is not Flash success. `null`, fixed-zero, or unexplained telemetry is
not measured evidence.

## Stop conditions

Stop immediately and do **not** retry the mutation if any of the following occurs:

- unexpected permission request before the documented post-Save replacement boundary;
- competing-app chooser or another app attempting to claim the device;
- wrong or changed identity/fingerprint;
- warning, crash, exception, disconnect that does not complete the authorized replacement path, or
  `STATE_UNCERTAIN`;
- more than one Save, evidence of replay, stale generation, replacement fingerprint mismatch, or
  missing final readback;
- checksum, signer, package/version, source SHA, or artifact-provenance mismatch;
- any unexplained telemetry value;
- restoration cannot be completed exactly.

After any uncertain mutation, preserve the reports and stop. Do not retry automatically or manually.

## Restoration and session close

Only after the Flash itself reaches verified final readback, use the signed-candidate-only
**Restore exact pre-test baseline** action shown on the EW300 DEVICE tab. This is a separate
architecture-owned restoration transaction, not a second qualification experiment: it writes the
recorded five-band pairs and playback/global gain, sends exactly one restoration Save, handles the
authorized replacement session, and performs complete final readback. Require
`restorationVerified=true`, `stateKnown=true`, `finalReadbackMatched=true`, one restoration Save,
exact replacement identity, and a strictly newer replacement generation when detach occurs. If
the action is absent, the operation report is uncertain, or any byte differs from the recorded
baseline, stop without retry; the physical gate fails and the release remains NO-GO.

Record the exact candidate provenance, device fingerprint, operation report, final readback, and
restoration result. Do not merge PR #23, publish v0.7.0, or make a public EW300 support claim from
this checklist alone; explicit owner approval remains required.
