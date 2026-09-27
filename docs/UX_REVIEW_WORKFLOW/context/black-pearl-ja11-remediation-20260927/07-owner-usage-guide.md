# Owner usage guide — Black Pearl and JA11 remediation

## Current source boundary

The current source is `9f5cb852994e1c88fce80598f249a97fae047429`. No exact signed APK exists for
this source, so do not install any local debug or unsigned release APK on the Pixel 9 and do not
perform hardware testing yet. Current status is `MERGE_APPROVAL_REQUIRED`; the next owner action
is separate authorization of the minimum main integration or a narrowly reviewed combined signing
path. The prior `acaf4dd` candidate and Pixel evidence are historical and do not prove this source.

The Android UAPP routing prompt also occurs in the stock app and is expected platform/device
behavior. It was deliberately left unchanged and must not be treated as the app-owned terminal
result defect.

Historical candidate only — do not use it for the current source:

- Source SHA: `acaf4dd32ddd9379ec2860e45e34fb8039219583`
- APK: `EQ-Library-v0.7.0-beta-acaf4dd.apk`
- APK SHA-256: `af83a5e0148263057b1c43e3b775157ab6aedd9d2c1e3ab0558cef8fa3cea665`
- Package/version: `com.weekssa.opraeqforuapp`, `0.7.0` / code `7`
- Signer SHA-256: `65c1c1256dae3c49e3548f334c91f0ba991969e9be9e0b223ba4e253d2114747`
- Workflow: [signed-beta #1371](https://github.com/weekssa/OPRA-EQ-for-UAPP/actions/runs/36336477287)
- Immutable artifact: ID `10937890771`, digest
  `sha256:88b1555e9a14642874110d05d5c5ae3554c25cc64b9385898a78287ccea9f52d`

The signed emulator install and cold launch passed for the historical source only. This is not a
candidate for the current repair and is not physical qualification.

Owner boundary: install only the exact candidate named in the handoff; do not use an older APK as a substitute; do not interpret raw USB bytes; do not retry a failed or uncertain hardware operation. Stop and return the app’s interpreted report if there is a permission loop, disconnect, mismatch, missing final readback, unexpected unrelated change, crash, or uncertain result.
# Owner usage guide - Black Pearl and JA11 remediation

## Current state

The branch contains software-verified remediation for exactly two issues plus the later JA11
terminal-result repair. The API 36 emulator, automated software gates, and local minified release
build pass for the current source, but the owner-approved main integration and signed candidate
belong to the historical source. Luna did not connect to or mutate a DAC in this follow-up. Do not
install the branch debug APK or unsigned release APK for hardware testing.

The previous handoff was `READY_FOR_PIXEL_9` for the historical source; the current handoff is
`MERGE_APPROVAL_REQUIRED` pending exact signed provenance for the repaired source.

## What the owner should do next

The next action is the final exact-candidate Pixel 9 review. Use the plain-language checklist in
`05-release-handoff.md` and return the candidate provenance plus separate Black Pearl and JA11
reports. Do not send credentials, signing material, or private reports in chat.

## Recommended Mac mini automation path

The Mac mini is the controller and evidence-capture host; the Pixel 9 remains the Android USB host.
Connect the DAC to the Pixel 9 through the appropriate USB-OTG path, not directly to macOS, for
app-level validation. Test one DAC at a time because the app owns one authoritative DAC session.

- `actionlint` is `NOT RUN` for the current source because it is unavailable locally.
- Google Android CLI `1.0.16406183` is installed at `/Users/stephenweeks/.local/bin/android`; the
  checked-in wrapper remains the project command of record and the shell profile was not changed.
- The checked-in `tools/codex-android` wrapper already provides the Android SDK, `adb`, emulator,
  screenshots, UI inspection, and log capture. No additional Android SDK installation is required.
- Mac-side automation may verify the exact APK checksum and signer, install and launch the app,
  capture UI/screens/logs, navigate through read-only and Review steps, collect readable/JSON
  reports, and timestamp the evidence.
- Keep Flash, Apply, Save, Reset, and restoration as owner-observed actions. Automation must not
  retry a failed or uncertain mutation, choose a different device, or continue after disconnect,
  permission loop, session replacement, missing readback, or mismatch.
- A DAC plugged directly into the Mac mini can be inspected for read-only USB enumeration, but a
  macOS USB-audio or libusb transaction is not evidence for the Android app and must not be used to
  send device writes in this review.

When a future exact candidate exists, the resulting evidence bundle should identify the exact candidate source/APK/checksum/signer,
Pixel serial, DAC identity, baseline, operation timestamps, app-readable and technical reports,
final result, and restoration status separately for Black Pearl and JA11.

## Safety boundaries

- Do not retry a failed or uncertain Black Pearl Flash, JA11 Apply, Save, or Reset automatically.
- Stop on an identity mismatch, permission prompt during mutation, disconnect, session replacement,
  or missing final readback.
- Treat emulator/debug evidence as software evidence only.
- Keep Black Pearl and JA11 results independent; one passing device cannot prove the other.
- Do not infer power-cycle persistence, acoustic fidelity, public support, or release readiness from
  immediate readback alone.
