# Black Pearl Flash verification — release/hardware handoff

**Status:** `READY_FOR_PIXEL_9`

## Completed software work

- Truthful Black Pearl Flash result boundary: `PASS` in candidate source `ce5efdf7985e4fc48f975b14fcedb1f592d43772`
- Final native-band/global-gain readback: `PASS` in candidate source `ce5efdf7985e4fc48f975b14fcedb1f592d43772`
- Compact cross-DAC-aligned message: `PASS` in candidate source `ce5efdf7985e4fc48f975b14fcedb1f592d43772`
- Focused and regression tests: `PASS`; local unit, lint, release/R8, and emulator instrumentation gates passed

## Exact candidate

- Source SHA: `ce5efdf7985e4fc48f975b14fcedb1f592d43772` (`main`, PR #48 merge)
- Branch/PR: `main` / PR #48
- APK: `EQ-Library-v0.7.0.apk`
- APK SHA-256: `3d723ffa17042fbef7e6e192c14ecce460628d0f08a55eeb30caa59566ff8731`
- Signer: `65c1c1256dae3c49e3548f334c91f0ba991969e9be9e0b223ba4e253d2114747`
- Workflow/artifact: [Signed Release Candidate #9](https://github.com/weekssa/OPRA-EQ-for-UAPP/actions/runs/36299335354), artifact `10924538542`, digest `sha256:e950b34686cb12406e0828cbd138ed7d866c3760b9b7c987a24541410dfbb9c9`
- Package/version: `com.weekssa.opraeqforuapp`, `0.7.0` / `7`

## Hardware boundary

- Physical mutation performed by Luna: `NO`
- Pixel 9 validation required: `YES`
- Accepted prior Black Pearl evidence transferred to this candidate: `NO` until exact candidate review
- New claim this candidate must establish: immediate Black Pearl Flash final readback is both performed and reported truthfully

## Next authorized action

The owner may now run the bounded Pixel 9 checklist in `03-pixel-9-handoff.md`. Do not publish or make a public support claim until the owner records the result and separately approves that action.
