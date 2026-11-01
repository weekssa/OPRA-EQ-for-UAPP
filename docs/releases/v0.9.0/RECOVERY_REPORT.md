# v0.9.0-beta.2 recovery execution report

Status: **INDEPENDENT SOFTWARE WORK COMPLETE — private beta candidate ready for the owner's
phone-readiness review. Physical persistence NOT RUN; nothing here is a hardware claim.**
This report covers only the recovery workstream defined by
[RECOVERY_AMENDMENT.md](RECOVERY_AMENDMENT.md). The sealed [PLAN.md](PLAN.md) and
[SEAL.sha256](SEAL.sha256) remain unmodified at
`a108a2182a28a82252c64956216d31b6dcba9c67bf522a51d6d86a073d6e729b` and
`b258bc43c156f247cb13ef4372286aafe20e643b52980bd6a7bf4a035f912a6b`.

## Identity

| Item | Value |
|---|---|
| Repository | `weekssa/OPRA-EQ-for-UAPP` |
| Branch | `codex/v0.9.0-recovery` |
| Base | `origin/main` `8233d87b1ed7ccb59d6df8bec864da3af761b21b` |
| Implementation source | `fd63a81e5061959ce44ebb65478b7f582fd307de` |
| Version | `0.9.0-beta.2` / versionCode `16` |
| Private beta package | `com.weekssa.opraeqforuapp.v090beta` |
| Launcher label | **EQ Library 0.9 Beta** |
| APK | `app/build/outputs/apk/privateBeta/app-privateBeta.apk` |
| APK SHA-256 | `bbe1c78956737ac94fec0069b8accd4ca9419e8f0806578af3ce2274277c0dd1` |
| APK size | 2,891,725 bytes |
| Signer certificate SHA-256 | `0f89797ba477006eea3e1ee837eaf654e687a14fc50b5a213b40312cdd1f869f` (local development test signer; **not** the official release signer) |
| Public stable | v0.8.3 / code 14 (unchanged) |

Any later source change creates a new candidate identity; the version code advances and the
affected gates rerun.

The APK embeds `BuildConfig.CANDIDATE_SOURCE_SHA = fd63a81e…`. Commits after that one on this branch
are documentation and release-tooling reconciliation only; they do not change application code.
An intermediate build (`03e797c1…`) preceded the per-session filter-probe cache and is superseded.

## Implemented behavior

1. **Native program/volume persistence for all five programs.** The defaults transaction no longer
   requires User 1. It writes the requested program (`0x16`) and volume (`0x02`) at runtime,
   verifies them, reads the now-active five bands (`0x15`) and global gain (`0x17`) fresh, restages
   those exact values, applies once (`0x18`), verifies the working state, sends the single reference
   Save(3) (`0x19`), and verifies a fresh final state while tolerating one bounded re-enumeration.
   Microphone and UAC writes are zero by construction and by a report invariant.
2. **No automatic microphone cycling.** The explicit microphone round trip remains implemented but is
   unreachable in this candidate: production wiring leaves the fallback switch off. It is the basis
   for a separately identified fallback candidate if the native path fails on hardware.
3. **Digital filter selector.** `0x11` read/write with typed values 1–5 and strict reply parsing. The
   row sits directly below EQ program. A JA11 that does not return a valid reply shows "Not available
   on this JA11", the row is disabled, and no filter command is sent. A valid reply enables one
   mutually exclusive selector; writes verify readback and companions.
4. **Approved save policy.** Completing a volume or program edit applies it at runtime. Because Save
   may re-enumerate the device, the screen then offers **Set as Default…** for the current pair and
   discloses the brief disconnect before the persistent write. Staging is local; Cancel sends nothing.
5. **Truthful startup state.** `UNKNOWN`, `RUNTIME_ONLY` ("Set for this session only. Unplugging JA11
   restores its saved startup settings.") and `SAVED` ("Saved for startup and verified by readback.")
   are separate. Session loss returns the indicator to `UNKNOWN`; no cached state is replayed.
6. **Private beta identity.** New `privateBeta` build type: release-derived R8 minification, local
   test signing, `UPDATE_CHECKS_ENABLED=false`, package suffix `.v090beta`, and its own launcher
   label. Stable and the older `.v083beta` app keep their packages and data.

## Evidence limits

- "Saved for startup" means the value was committed and verified by readback in the same power cycle.
  Only the physical session can prove it survives power removal.
- The acoustic meaning of a built-in program's coefficient set is **not** claimed. The transaction
  round-trips what the device reports; it never substitutes a flat curve.
- Filter modes and their FAST/Slow/LL/PC/NON-OS meanings remain the reference author's claims.
  Runtime capability negotiation reduces, but does not eliminate, the risk of an unsupported write.
- Emulator and JVM evidence is never hardware persistence evidence.

## Computer gates

| Gate | Result and actual evidence |
|---|---|
| JVM unit tests | **PASS** 830 tests, 0 failures/errors/skips (`:app:testDebugUnitTest`, source `fd63a81e`) |
| Native-defaults suite | **PASS** 16 tests: all programs × volume 0/60/current × both mic states; runtime-vs-saved power-cycle model; changed bank after Apply; changed bank at final readback; Save failure; incomplete bank read; session change mid-staging; cancellation; mic/UAC drift; bounded reconnect; stale review; filter write/unsupported/ignored-write |
| Protocol codec | **PASS** `0x11` read/write frames and strict response parsing (short reply, unknown command, out-of-range value rejected) |
| Lint | **PASS** `lintDebug`, `lintRelease`, `lintPrivateBeta` |
| Builds | **PASS** `assembleDebug`, `assembleRelease`, `assemblePrivateBeta`, `assembleDebugAndroidTest` |
| R8 | **PASS** `tools/verify-r8-mapping.sh` (application classes renamed) |
| Python/repository contracts | **PASS** 306 tests; repository foundation, stable promotion, legacy beta publisher, v0.9 release profile; registry 15 sources; Favorite samples 14/13/2 |
| API 35 instrumentation | **PASS** 78/78 on `emulator-5580` (Android 15) |
| API 36 instrumentation | **PASS** 78/78 on `opra_api36` (Android 16) |
| API 26 compatibility | **PASS** minified private beta installed and launched on `opra_api26` (SDK 26); screenshot captured |
| API 35 private beta | **PASS** exact APK installed beside other packages, launched, resumed, force-stopped/relaunched; launcher label "EQ Library 0.9 Beta" |
| API 36 lifecycle/scaling | **PASS** background/foreground, force-stop/relaunch, font scale 1.5 + density 480 rendered and restored (font 1.0, physical density 420) |
| Private-beta minified instrumentation | **NOT ACHIEVED — diagnosed.** See below. |

## Minified-beta instrumentation diagnosis (bounded corrections used: 3 + 1 test fix)

The historical beta crash "before discovery" is reproduced and explained rather than inherited:

1. `minifyPrivateBetaAndroidTestWithR8` failed first on `java.lang.reflect.AnnotatedType` (Guava via
   Truth). Adding the generated `-dontwarn` let the test APK build.
2. The runner then failed with `ClassNotFoundException: androidx.tracing.Trace`, then
   `kotlin.LazyKt`, then `kotlinx.coroutines.CompletableDeferred`, then
   `NoSuchMethodError: IntObjectMapKt.mutableIntObjectMapOf()`. Root cause: AGP does **not** package
   application dependencies into the androidTest APK; test classes resolve app and library classes
   from the app APK's classloader. R8 tree-shaking and inlining remove those members.
3. Keeping the library packages made the suite start, but application-only constructors and codecs
   (`ManagedProfileSnapshotCodec`, Room DAO constructors) were also removed, and 22 UI tests could not
   resolve the app activity. Keeping or disabling shrinking cannot model the test APK's references;
   `-dontshrink` and `-dontoptimize` both make R8 fail on `androidx.core` synthetic lambdas
   (observed), so they are not a viable route either.

Therefore the shipped private beta keeps **full R8 minification** (the artifact the owner will
install), and the exhaustive instrumentation suite runs on the debug variant on API 35 and 36. The
minified artifact is verified by exact install, package/version/label/signature/hash identity, launch,
background/foreground, force-stop/relaunch and large-font checks on API 26, 35 and 36. This is a
**recorded limitation**, not a silent substitution: minified-app instrumentation remains an open
tooling gap.

## Not run / open

## Independent review

One bounded read-only review (Luna, inspection only) found no release-blocking defect and four
medium items, all corrected in this candidate:

1. A saved-startup report could remain visible after a reconnect. Session change, stale and failure
   states now clear both the report and the startup claim; a regression test covers it.
2. The recovery checklist block initially contained a malformed sentence; corrected.
3. The runbook's beta.1 section still read as the active boundary; it is now marked historical.
4. The filter probe cache had no direct test; it is now a small unit-tested type covering per-session
   reuse, cached unsupported results, write invalidation and the no-session case.

The reviewer did not run builds or tests; its findings were re-verified here by rerunning the affected
gates (830 JVM tests, both instrumentation targets, builds, lint, R8).

- Physical Pixel 9 + JA11 persistence, filter behavior, restoration and go/no-go (owner-gated).
- Official merge, main-only signing, publication (separately owner-gated; not inferred).
- Hosted GitHub CI for this branch (no push performed).

## Next authorized action

Present the readiness packet, then run the owner-confirmed wireless-ADB physical session against the
frozen APK above, using [HARDWARE_CHECKLIST.md](HARDWARE_CHECKLIST.md) plus the recovery save-UX steps.
