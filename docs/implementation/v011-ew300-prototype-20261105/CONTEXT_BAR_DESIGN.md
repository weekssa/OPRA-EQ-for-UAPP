# v0.11 context bar — locked design, findings, and next steps

Status: complete. The bar is implemented, verified on the device, and captured. Test fixtures live in
ContextBarTestFixtures.kt; the six screenshots are in docs/screenshots/v011/. Written 2026-11-06,

## Locked design (do not redesign)

The context bar replaces the old device card. It appears on **My EQs and EQ Library only**; Settings
shows no bar, because it is configuration rather than listening.

1. **Target row first.** The existing target selector, unchanged: the plain list of outputs enabled in
   Settings, no annotations, no per-device status. Selecting an output is the statement of intent.
2. **Device block second, only for a hardware target** (or when a live session/error must stay
   visible). It names the device, states the condition, and offers exactly one action.
3. **Display anchor.** The bar remembers the last hardware target. When Automatic output resolution
   hands the target back to an app because a DAC disappeared, the block stays and reports
   "No DAC attached". The anchor is cleared only by an explicit non-hardware pick in the target
   picker, and replaced by a different hardware pick. Target resolution itself is untouched.
4. **Card body vs action.** The body is one focusable target that opens My DAC, with a content
   description naming the device and state. The trailing action is a separate 48 dp button with its
   own description. They must not merge.

### State and action table (agreed, shipped)

| State | Status line | Action | Lands on |
|---|---|---|---|
| Connected | Connected · State current / Reading / Last read is stale / Waiting for first read | Open My DAC | My DAC |
| Connecting | Connecting… | Open My DAC | My DAC |
| Ready to connect | Not connected | Connect | opens the session |
| USB permission pending | Allow USB access | Connect | requests access |
| Session error | Connection issue | Connect | retries |
| Target device not attached | No DAC attached | Open My DAC | My DAC |
| Direct Flash off | Direct Flash off | Open Settings | Settings |
| Connected, read worked, next read failed | Connection issue | Connect | re-opens the device |

Connect appears only where it can actually connect; nothing here writes to the device.

The table is ordered by precedence, not by independence. A connected device always offers
"Open My DAC", including when Direct Flash is off: the banner then reads "Direct Flash unavailable ·
Direct Flash is off. Enable it in Settings → SIMGOT EW300 DSP." The "Direct Flash off → Open Settings"
row therefore applies only while the device is attached but not connected. Verified on the Pixel on
2026-11-06 in both directions (T-049).

The last row is narrower than it looks. A session that answered once and then stopped answering is a
dead handle, so the bar says "Connection issue" and Connect re-opens the device rather than repeating
a read that just failed. A failed *first* read keeps "Open My DAC": that is the replacement session
that follows a Save, and My DAC is where the explanation lives (T-053).

## Out of scope

- No change to `AppPreferencesRepository`, Automatic/Manual resolution, or the fallback behaviour.
- No My DAC root tab; it stays an in-session workspace reached from the bar.
- No Flash, Save, volume, protocol, session, transport, or firmware changes.
- No new visual language: EqPalette, existing card shape, border, and typography.

## What has shipped

| Commit | Content |
|---|---|
| bb928d1f | Scope amendment: AppContextSurfaces.kt added to the contract, 90 paths, resealed |
| c0cd517a | The bar, the five-state action table, and the four migrated UI tests |
| ebbfc645 | Display anchor + three new tests (unplug keeps block, explicit app pick retires it, target still falls back) |
| 43ea196e | Pixel verification record |

### Verification already recorded

- Local: JVM 883 tests / 0 failures (18:42:52Z); lint 0 errors / 121 warnings / 2 hints; instrumented
  86/86 on emulator-5580 (18:45:33Z). APK 7516516bba42c5683a9a0d78d1554dc7c7456eadf42b7f54603299e56660dc81.
- Pixel: Connected (body and button both open My DAC), Not connected + Connect, Direct Flash off →
  Open Settings lands on Settings, and — after the anchor — unplug shows "No DAC attached" with the
  target row simultaneously fallen back to the app; replug returns to Not connected.
- Permission and error states are emulator-only; they cannot be produced on demand.

## Findings

1. The D-pad route needed two focus targets: with the whole card clickable, the body was merged and
   unreachable. Splitting body and button fixed it.
2. Automatic resolution drops the DAC target on unplug; that is correct and was kept. The anchor is
   the bar's memory, not a resolution change, and both are visible at once on the phone.
3. Test doubles `noOpEqLibraryActions` and `fiioJa11CurrentHardwareState` are members of
   `UiModernizationFlowsTest`, not top-level helpers, so another test file cannot call them. The
   compiler refuses with: `Unresolved reference 'noOpEqLibraryActions'` and
   `Unresolved reference 'fiioJa11CurrentHardwareState'` at ContextBarScreenshotTest.kt.
4. emulator-5554 no longer exists; emulator-5580 is the working AVD. Its package manager occasionally
   aborts the first lifecycle test with DELETE_FAILED_INTERNAL_ERROR; uninstalling both packages
   before the run clears it.

## Next steps (in order)

1. Create `app/src/androidTest/java/com/weekssa/opraeqforuapp/ui/screens/ContextBarTestFixtures.kt`
   (pre-approved as the only new test file) containing top-level `internal` copies of the two
   doubles, then add the path to CONTRACT.md and GATES.md and reseal in its own commit.
2. Rewrite the capture test to call those fixtures and capture the six images into
   `docs/screenshots/v011/`: connected, not connected, Direct Flash off, no DAC attached, EQ Library,
   and 360 dp at font scale 2.0.
3. Run `:app:testDebugUnitTest`, `:app:lintDebug`, and one instrumented run on emulator-5580 after
   uninstalling both packages. Record counts, timestamps, and the APK hash.
4. Update CHECKPOINT.md, SESSION_HANDOFF.md, V0.11_PROTOTYPE_DESIGN_LANGUAGE.md and the beta notes
   with the screenshot paths and the final artifact.
5. `scope-guard --root .` IN SCOPE, then one commit for the fixtures amendment and one for the
   screenshots and docs.

## Acceptance for calling this complete

- The five states above are demonstrated: three on the Pixel, the anchor on the Pixel, the remaining
  two emulator-only and labelled as such.
- Six screenshots exist and come from the committed code.
- JVM, lint and the instrumented suite are green on the final code, and the working tree is clean.
- No Flash, Save, or register write is performed anywhere in this task.

