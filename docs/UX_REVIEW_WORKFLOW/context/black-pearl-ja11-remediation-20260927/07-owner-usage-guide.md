# Owner usage guide — Black Pearl and JA11 remediation

Use only the exact candidate recorded in `05-release-handoff.md`:

- Source SHA: `acaf4dd32ddd9379ec2860e45e34fb8039219583`
- APK: `EQ-Library-v0.7.0-beta-acaf4dd.apk`
- APK SHA-256: `af83a5e0148263057b1c43e3b775157ab6aedd9d2c1e3ab0558cef8fa3cea665`
- Package/version: `com.weekssa.opraeqforuapp`, `0.7.0` / code `7`
- Signer SHA-256: `65c1c1256dae3c49e3548f334c91f0ba991969e9be9e0b223ba4e253d2114747`
- Workflow: [signed-beta #1371](https://github.com/weekssa/OPRA-EQ-for-UAPP/actions/runs/36336477287)
- Immutable artifact: ID `10937890771`, digest
  `sha256:88b1555e9a14642874110d05d5c5ae3554c25cc64b9385898a78287ccea9f52d`

The signed emulator install and cold launch passed. This is ready for the owner Pixel 9 review;
it is not physical qualification.

Owner boundary: install only the exact candidate named in the handoff; do not use an older APK as a substitute; do not interpret raw USB bytes; do not retry a failed or uncertain hardware operation. Stop and return the app’s interpreted report if there is a permission loop, disconnect, mismatch, missing final readback, unexpected unrelated change, crash, or uncertain result.
# Owner usage guide - Black Pearl and JA11 remediation

## Current state

The branch contains software-verified remediation for exactly two issues. The API 36 emulator,
automated software gates, owner-approved main integration, signed candidate, signed install, and
cold launch passed. Luna did not connect to or mutate a DAC. Do not install the branch debug APK for
hardware testing.

The handoff is at `READY_FOR_PIXEL_9`; software gates, owner-approved main integration, signed
candidate provenance, signed install, and cold launch have passed.

## What the owner should do next

The next action is the final exact-candidate Pixel 9 review. Use the plain-language checklist in
`05-release-handoff.md` and return the candidate provenance plus separate Black Pearl and JA11
reports. Do not send credentials, signing material, or private reports in chat.

## Safety boundaries

- Do not retry a failed or uncertain Black Pearl Flash, JA11 Apply, Save, or Reset automatically.
- Stop on an identity mismatch, permission prompt during mutation, disconnect, session replacement,
  or missing final readback.
- Treat emulator/debug evidence as software evidence only.
- Keep Black Pearl and JA11 results independent; one passing device cannot prove the other.
- Do not infer power-cycle persistence, acoustic fidelity, public support, or release readiness from
  immediate readback alone.
