# FiiO JA11 capability matrix

This matrix is the current evidence boundary for the exact FiiO JA11 identity. Software
implementation does not imply physical support. Unknown behavior stays insufficiently evidenced
or unsafe rather than being inherited from another KT02H20-family device. The owner-approved Model
D policy below makes serial optional continuity evidence; J024's physical null-serial result remains
historical evidence from a read-only session.

All 2026-10-08 serial-required recommendations below are historical candidate-specific conclusions.
The current operational policy is the Model D row and matrix rule; do not use older stop text as a
gate for the new candidate.

## v0.9.0-beta.1 candidate boundary (2026-10-30)

The approved [sealed plan](releases/v0.9.0/PLAN.md) supersedes historical no-native-investigation wording for this workstream only. The in-development Set as Default path for an explicitly requested User 1 target uses fresh exact unchanged five-band/global-gain staging, Apply, volatile verification, one Save(3), and final readback without mic writes. Other programs retain the explicitly confirmed microphone round trip. This candidate has **no new physical persistence qualification**; J029 and J032/J033 remain bounded to their exact historical tuples. Neither success simulation nor prior Flash evidence proves native volume persistence for this candidate. Runtime Apply is separate from Set as Default.

| Optional mode | Reference `0x11` value | Current disposition |
| --- | --- | --- |
| FAST-LL | 1 | Insufficiently evidenced; implementation deferred individually |
| FAST-PC | 2 | Insufficiently evidenced; implementation deferred individually |
| Slow-LL | 3 | Insufficiently evidenced; implementation deferred individually |
| Slow-PC | 4 | Insufficiently evidenced; implementation deferred individually |
| NON-OS | 5 | Insufficiently evidenced; implementation deferred individually |

All five mandatory investigations are complete. Source-generated packets are demonstrated; independent exact-JA11 acceptance, firmware/UAC applicability, runtime effect, Save/power-loss persistence and companion preservation remain unproven for each mode. This is not a hardware-unavailable finding. No functioning controls are enabled. See [three-pass evidence and provenance](releases/v0.9.0/REFERENCE_EVIDENCE.md) and the [requirements checklist](releases/v0.9.0/REQUIREMENTS.md).

## 2026-10-14 v0.8.3 private beta defaults observation

Exact beta source `7cea4545dddbee2da0b436e0684a9c489835e3cd`, package
`com.weekssa.opraeqforuapp.v083beta`, APK SHA-256
`ac982eafd76f4db80f48e0f26040bf1f849388c4f0a51f778b51c8c013c59ce5`, and debug signer
certificate SHA-256 `968b15ab4802bba98fbd058d7b81f537da6c2c5542333d0b107b3495135587b4` were used on
one Pixel 9 / FiiO JA11 profile (firmware 2.20, VID/PID `0x2972:0x0102`). Three explicit defaults
operations completed: a changed volume, an unchanged-current volume, and a non-User-1 program.
After the owner-confirmed true power removal, fresh readback on the sole supported returning JA11
showed volume 19/60, program Classic, mic On, and UAC 2.0. See J032 in the validation ledger.

This is **candidate-specific beta evidence**, not a new general firmware or public capability
classification. It shows that this exact app's explicit Set as Default mic-round-trip workflow
persisted the tested volume/program pair on this one JA11 profile. Ordinary runtime Apply remains
a runtime control. No unique serial/fingerprint was captured, and User 1 EQ data could not be
compared for the tested Off/Classic programs; EQ-bank preservation and same-unit continuity are
not claimed. Keep the historical v0.8.2 table below bounded to its original tuple. Any later
production classification requires owner acceptance and exact release evidence.

On 2026-10-14 the owner reported that manual testing of this identified Beta v1 succeeded and that
all other manual tests passed, then authorized go-live (J033). This owner acceptance is not an
independent bus capture or a new User 1/serial measurement. Keep the above User 1 comparison and
identity limits, and do not promote this one candidate result into a general firmware capability
classification. The beta reports final volume 19 / Classic, Mic On, UAC 2.0; its exact package,
version, APK digest and signer remain in J032.

## 2026-10-14 v0.8.3 stable publication

Stable release [v0.8.3](https://github.com/weekssa/OPRA-EQ-for-UAPP/releases/tag/v0.8.3) is source
`899755507f812c940f3ab9381a856c80c6030885`, package `com.weekssa.opraeqforuapp`, versionName
`0.8.3`, versionCode `14`, APK SHA-256
`396244ffc26cec108b64c523a49779261656b41ab02f1bc2b63197327c576247`, signed by the pinned release
certificate. PR #97 merged normally; exact-main Android CI `38648012952` passed, and promotion
`38650109951` passed API 35 clean-install and v0.8.2 in-place-upgrade checks before publishing the
exact candidate. The owner accepted the Beta v1 after manual testing (J033). This publication adds
the explicit JA11 Set as Default product action, while the existing general volume/program
runtime-only classification remains historical firmware evidence. The exact-candidate result does
not change the general classification or substantiate a User 1 preservation, unique same-unit, or
USB bus-capture claim. The final beta/device state and limits remain in J032/J033.

The published 0.8.2 candidate changes app completion ownership and preserves global My EQs rows during output transitions. Hardware controls, native EQ representation, protocol/Save behavior and the capability classifications below remain unchanged. J031 accepted the exact signed candidate for same-process visible EQ/DEVICE convergence, editing/navigation, unchanged observed baseline, and cleanup, with limits recorded in the validation ledger. Raw mutation/replay telemetry, internal generations, ViewModel identity, unique same-unit continuity, and any additional persistence behavior remain unmeasured or unclaimed. J030's separate tracing-build healthy control is preserved and does not qualify the 0.8.2 fix.

## Current capability disposition after v0.8.2 publication

The published v0.8.2 release is source `9983edf0b7fe437aeeec9ac3dcb3864705afa16a`, package
`com.weekssa.opraeqforuapp`, versionName `0.8.2`, versionCode `13`, APK SHA-256
`df1ac78e7462da588e01f4aa39f5f23d0c05797d27eb2222d0650ce319c5d6e3`. J031 adds candidate-bound
evidence for visible same-process EQ/DEVICE convergence and the recorded UI/cleanup outcomes; it
does not change any per-setting capability classification below. J029 remains the accepted ordered
User 1 EQ/global-gain transaction and power-cycle evidence for its own exact tuple.

The 2026-10-12 v0.8.1 capability disposition below is retained as historical release context.

The final proven capability matrix is:

| Capability | Final classification | Boundary |
| --- | --- | --- |
| Mic | Persistent | The accepted physical acceptance established persistence across the true JA11 power cycle. Mic changes still use expected reset/re-enumeration, fresh-session reacquisition, and authoritative readback. |
| USB Audio Class | Persistent | The accepted physical acceptance established persistence across the true JA11 power cycle. UAC changes still use expected reset/re-enumeration, fresh-session reacquisition, and authoritative readback. |
| User 1 custom EQ/global gain | Persistent saved configuration | J029 proves the ordered User 1 selection, five band writes, global-gain write, one Apply, one Save, stale-session invalidation, and fresh-session final readback. The accepted power-cycle evidence confirms the saved User 1 custom EQ/global gain is retained. |
| Playback volume | Runtime-only across true power loss | The official FiiO Control comparison and the exact candidate's power-cycle readback both show return to the device default. The app uses the runtime volume command and does not add Save behavior. |
| Active EQ program/preset | Runtime-only across true power loss | The official FiiO Control comparison and the exact candidate's power-cycle readback both show return to User 1. The app uses the runtime program selector and does not add Save behavior. |

## 2026-10-10 clean v0.8.1 branch status

The clean branch `codex/ja11-v0.8.1-minimal` was at open/draft PR #81 head `78deeddd2cce65c22a98fce7e288a608ceb813fd`, based on `9b9a1f610025b3d6233d4ff6f0c2309a668577cc`, during J029. All eight required checks passed on that exact head. The diagnostic report implementation is commit `fc409b7d038632e67dccdeccbd5144f3d3b53bdd`; production/JA11 transaction source remains `9493cf030acb440f92e547fc667f6a5399616045`. Diagnostic-only lint, assembly, focused API 35 report instrumentation (4/4), and independent review passed. Prior production regressions, the 768-test JVM suite, R8, helper fixtures, and 64/64 production API 35 instrumentation remain valid and were not repeated. This closeout update is documentation-only and does not change executable candidate identity.

The diagnostic APK SHA-256 is `44040493c7263ae7d7e6f41be7389534d07977427cc8002494097fbf3c868848`, package `com.weekssa.opraeqforuapp.ja11diag`, version/code `0.8.1-ja11diag` / `12`, signer certificate SHA-256 `73aa7581c8dc7dcc8ccea7586771119a98f9a74d7d8cf23716e09c557c9f6b41`. It adds a **JA11 Flash Report** launcher in `ja11Diagnostic`, reads the existing completed direct-FLASH trace, retains it in memory until explicit dismissal, and shares readable plus technical JSON reports. It does not change `app/src/main`, transaction behavior, or release behavior. J029 installed and used this exact APK.

J027 remains a historical partial record. J028 is the completed resumed physical session on production source `9493cf030acb440f92e547fc667f6a5399616045` and prior diagnostic APK SHA `98ee2eb876ec0d6d5bcfcebebdf55e1c77f2f7edfbecfd59322a94e5feea48ae`; its final readback matched, but Save count remains unknown. It began under PR #81 head `2e81dc99f4335aaa70645fad33adb391626e7cdc` and closed at `885fe90ec46ba30ffbd2bcef1f28c075eb8546f7`, with no `app/src/main` runtime diff between those heads. J029 then used the new diagnostic APK at exact PR head `78deeddd2cce65c22a98fce7e288a608ceb813fd` with that same production source. Its paired report payloads prove one User 1 select/read before writes, five band writes, one gain write, one Apply, one Save, no replay, an observed detach and fresh generation, and final authoritative readback matching the intended User 1 bank/gain. The full baseline was restored, the temporary profile/source file removed, and the Pixel released. J029 closes the Test C evidence gap for its recorded tuple; it makes no same-unit or general power-cycle PEQ persistence claim.

Model D remains current. Require one supported JA11 candidate, a permissioned current session/generation, expected detach and fresh replacement for restart verification, and authoritative readback. Serial is optional continuity evidence; compare it when both sessions provide usable values and reject mismatch. Without a serial match, report state verified on the sole returning supported JA11, not same-unit proof.

## Per-setting behavior model — owner-reported official FiiO app reference

The owner observed the following behavior in the official FiiO app. This is behavioral/reference
evidence only; it does not prove exact USB packets or command framing.

| Setting | Capability classification | Evidence boundary |
| --- | --- | --- |
| Custom EQ/profile apply | Persistent commit/reset workflow | Applying a custom profile behaves like Flash/Save and resets/re-enumerates. J028's exact wire-level Save count remains unverified; J029 independently captured exactly one Save and the expected detach/fresh-readback sequence on its exact candidate. |
| Mic | Expected-reset control | Mic changes reset/re-enumerate in the official app; keep expected-reset handling and authoritative replacement readback. |
| Built-in preset/program | Runtime control unless contrary protocol evidence appears | Changes do not reset/re-enumerate and no Save-style behavior is observed. Do not add Save behavior. Return to defaults after true power loss may be intended firmware behavior, not an OPRA EQ defect. |
| Volume | Runtime control unless contrary protocol evidence appears | Changes do not reset/re-enumerate and no Save-style behavior is observed. Do not add Save behavior. Return to defaults after true power loss may be intended firmware behavior, not an OPRA EQ defect. |

## Earlier 2026-10-09 Model D physical observation — pre-ordering candidate

On exact Model D source `1d19067c9150aae1b09e01fafb8af3647bf65f71`, a read-only identity gate passed,
Mic Off was restored to On, and UAC was later freshly read as 2.0 after a permission-timing timeout.
One Flash from Off failed band 1 volatile readback before Save; its trace wrote bands/gain before
selecting User 1. Off and the other captured baseline settings were restored, the prior diagnostic
APK was verified back on the Pixel, and the Pixel was released. This is a candidate-specific
pre-Save physical failure and a strong but unproven ordering diagnosis. Candidate source
`92c11fb0e41ae11b118b2e7bb105234d6606dbdb` now selects/verifies User 1 before data writes. G2/G3/G3a,
independent review, artifact provenance, API 35 cold launch, and 64/64 instrumentation pass; exact
head PR #80 CI is pending. Do not reuse the previous APK or use the Pixel before those checks and
the new candidate's read-only session/cardinality/baseline gate pass.

## 2026-09-28 release closeout

The owner authorized final release publication. The software editor/apply capability is complete
and verified in the remediation source; the physical capability decisions below remain bounded by
the exact reports and candidate identities already recorded. In particular, the earlier J016/J017
physical records are not relabeled as tests of combined source `e1ab5fa`, and explicit power-cycle
retention/full JA11 qualification remains pending unless separately evidenced.

The latest owner-tested signed candidate is source `c886fdbb2ae326e562dc110b2b779cb075869798`, with
immutable APK SHA-256 `c390bbd429ce4101ce7fad3aa3820990da0e7ffec7a4f688e5eafe4eb11f6341`, signer
certificate SHA-256 `65c1c1256dae3c49e3548f334c91f0ba991969e9be9e0b223ba4e253d2114747`, and signed-beta
run [#1365](https://github.com/weekssa/OPRA-EQ-for-UAPP/actions/runs/36223017450). J017 records a
successful Flash, observed detach/reconnect history, and exact original-flat-state restoration
on this candidate. The software/artifact gate, corrected same-session transaction, observed
reconnect path, and restoration path are evidenced; explicit power-cycle retention and full
qualification remain pending. J012 and earlier candidates remain historical negative evidence and
must not be reused.

## 2026-10-08 J019 headset restart verification update

J019 used diagnostic source `3f5e0c3a39687e27d962dd7f7f80d2667ff396ae`, APK SHA-256
`85e06ca0db818586a7eb2eab3378a1b21949b3c8593e1318536ec651d8369305`, on a Pixel 9 and the
owner's JA11 (`0x2972:0x0102`, firmware `2.20`). The On-to-Off transition automatically read back
successfully. The Off-to-On write completed, re-enumerated and its fresh User 1 snapshot matched,
but automatic DEVICE verification timed out and UI remained Off. A separate read-only Refresh then
showed On and the exact original state. Tests B/C/D were not run. See J019 in the validation ledger.

The failure is tied to that exact superseded APK. USB descriptor evidence shows HID interface ID
`3 → 2 → 3`, while the app did not log which interface it selected; identity mismatch is plausible
but not proven. A cancellable `collectLatest` verifier was another plausible cause. Current
remediation removes PID/interface from the restart identity and gives verification to a timeout-owned
watchdog, retaining generation checks. Restart writes also require one nonblank serial; J019 did not
capture whether this JA11 exposes one. The new candidate must establish this read-only before any
restart mutation. Do not generalize J019 to a permanent protocol root cause or support claim.

The J019 replacement remediation source was `a78808443c71d688e0f338e96495847569fe12f7`, tree
`87c1879af02ea421032c95363244f0f364924d3a`. Its diagnostic APK is
`opra-eq-ja11diag-0.8.0-source-a7880844.apk`, SHA-256
`7beb5bcebbc0dc40a68b33de911cc8722d76d3f0ff2e98685b1fa25e17caed61`, package
`com.weekssa.opraeqforuapp.ja11diag`, version `0.8.0-ja11diag`/11, debug signer SHA-256
`73aa7581c8dc7dcc8ccea7586771119a98f9a74d7d8cf23716e09c557c9f6b41`. The remediation passed local
JVM/build/lint/R8 gates and 64 API 35 emulator instrumentation cases; the APK was installed and its
source-bound build event verified on the emulator only. All eight required PR checks passed on exact
head `9e9cb4ac6a0f540310139bd347d21a01fb1fb5b1`, followed by host-only ADB preflight. J020 below
then exercised its restart path and found replacement-session identity unavailable; that candidate
is superseded for physical use. No hardware support or public release claim follows from this
software evidence.

## 2026-10-08 J020 delayed permission and identity result

J020 used the exact `a78808443c71d688e0f338e96495847569fe12f7` diagnostic source and APK SHA-256
`7beb5bcebbc0dc40a68b33de911cc8722d76d3f0ff2e98685b1fa25e17caed61` on Pixel 9 / JA11 firmware
`2.20`, VID/PID `0x2972:0x0102`. One Mic On-to-Off write completed. USB permission was granted about
18.5 seconds after the request, and a current replacement session read Mic Off plus the unchanged
baseline. Automatic restart verification did not emit a `RESTART_VERIFY_*` event. That replacement
session reported `sessionCurrent=true` and `identityAvailable=false`.

The restart identity key requires one nonblank USB serial. A pre-permission attach observation had
no readable serial value; the evidence does not show whether the post-permission result was a blank
descriptor, permission-gated access, or an access exception. The delayed permission and unavailable
identity co-occurred, but causation is not established. The next candidate adds privacy-safe
per-session serial-status categories while retaining the unique-serial requirement. Do not replace
the serial with PID, product name, firmware, or port path. The last verified mic state is Off and
restoration to the original On state is outstanding. No second write, UAC, Flash, Reset, or Tests
B/C/D occurred.

## 2026-10-08 corrected software candidate — exact-code-head CI PASS; physical qualification pending

The implemented correction is committed at source
`da1f8e25918065667648d676cb669fed4c803f17`. The permission attempt now survives the initial
10-second retryable UI fallback, while JA11 itself has a bounded 25-second prompt deadline.
Callbacks are fenced by request, device/PID, detach generation, and current permissioned
descriptor; retry/detach/cancel/close retire the attempt. Each session's permission action includes
a unique UUID so a stale callback cannot collide with a recreated session's reused request ID.
Identity remains fail-closed on one unique nonblank serial, and hardware writes are never replayed.

G2/G3 and R8 gates passed on this source; XML output counted 818 JVM tests with zero failures,
errors, or skips, and the clean API 35 emulator passed 64/64 instrumented tests. The exact-source
diagnostic APK SHA-256 is
`767b42591adc92f0e1662480112bf9efe87ce15f51060d20a7aa39118b8d8c24`, signed by debug certificate
`73aa7581c8dc7dcc8ccea7586771119a98f9a74d7d8cf23716e09c557c9f6b41`. This software evidence does
not establish that the owner's JA11 exposes a readable unique serial after re-enumeration. J020
remains the latest physical result; Mic On restoration and all replacement-candidate hardware
acceptance remain pending. All eight required PR check rows passed on corrected-code head
`ef688ca1a2c805c349439eb0e9ac24fb456641ad`; the live check state for any later documentation-only
head is maintained in PR #80. This does not change the physical boundary.

## 2026-10-08 opened-connection fallback candidate

The current application source is `616958037349e2f0e0784a556c6430b0de6ceb18`, tree
`fdb6c8d10c8aa865da1a4818d22a7c14b2559b21`. Its JA11-only fallback reads the serial from the
already opened connection only when the device-level getter returns null. Local full gates pass,
the JVM suite has 825 tests with zero failures/errors/skips, and the clean API 35 emulator passed
64/64 instrumentation. Exact-head PR #80 checks and physical identity/restore testing remain pending.
This does not prove that the connection exposes the JA11 serial or that identity succeeds after
re-enumeration.

## 2026-10-09 J024 opened-connection identity result

J024 used the exact source-bound candidate and verified the installed APK hash, package version,
debug signer, and runtime `APP_BUILD_INFO` before opening the normal My DAC session. The Pixel 9 UI
reported FiiO JA11 connected. Diagnostics recorded permission granted, product ID `0x0102`,
`serialStatus=READABLE_NULL`, `connectionSerialStatus=READABLE_NULL`, and `serialSource=NONE`. The
device getter was null, so the JA11-only opened-connection fallback ran and also returned null. The
app opened session generation 1, completed the source-bound snapshot (`OFF`, `-3.7 dB`, and five
0 dB Peak/Dip bands at 1000/2000/5000/8000/10000 Hz, Q 0.7), then reported
`identityAvailable=false` with `sessionCurrent=true`.

This is a physical negative result for unique identity on the connected unit and this candidate; it
does not prove the JA11 lacks a serial descriptor, because raw descriptor contents and the descriptor
string read result were not captured. VID and firmware were not independently recorded during J024.
Read requests for firmware, sample rate, volume, Mic and UAC occurred, but decoded values were not
preserved; no updated DEVICE value is claimed from J024. No hardware write or restart test was sent.
At the time of J024, J020 was the latest mutation and Mic On restoration remained outstanding;
that candidate was not qualified for physical mutations. J029 later verified the exact diagnostic
candidate and completed one Test C Flash under the sole-candidate/fresh-generation policy. J024's
negative serial-reader result remains historical and does not establish that later descriptor token
was unique. Keep identity fail-closed and do not substitute VID/PID, product name, firmware, or port
path for a usable serial match.

| Capability | Decision | Evidence / boundary |
| --- | --- | --- |
| Current JA11 session identity and expected-reset continuity | PRODUCTION GATES PASS ON `9493cf0`; J029 ONE-SOLE-CANDIDATE FRESH-GENERATION READBACK PASS; NO SAME-UNIT CLAIM | Require exact supported JA11 matching plus one candidate, valid HID interface/endpoints, current permission, fresh open/claim, current generation, expected detach, old-session invalidation, fresh replacement, and authoritative readback. Model D does not require a usable serial when absent or generic; compare usable serials when both exist and reject mismatch. J029 records final reads on generation 2/detach 1; no same-unit continuity claim is made. |
| JA11 USB allowlist and UAC PIDs `0x0101`/`0x0102` | SOFTWARE IMPLEMENTED; J029 READBACK VERIFIED ON PID `0x0102` | Strict VID/PID allowlist and dynamic HID interface discovery are implemented. J024 recorded product ID `258`; J029 verified VID/PID `0x2972:0x0102` and firmware `2.20` in the fresh connected-device read. |
| Five-band Peak/Low Shelf/High Shelf target representation | SUPPORTED_AND_IMPLEMENTED; J029 PHYSICALLY VERIFIED LOW SHELF + PEAK/DIP TARGET; HIGH-SHELF PHYSICAL CONFIRMATION PENDING | Shared finite-hardware adapter, complete five-slot target, and codec tests exist. J029 decoded and verified one low shelf and four peak/dip bands; J001 displayed a 9→5 optimized plan but did not provide readback values. |
| User 1 bank selection before Flash/Reset/Editor writes | PRODUCTION TRANSACTION CODE/REGRESSIONS PASS; J029 PHYSICAL PASS | The J029 report shows one `WRITE 0x16` with User 1 value, followed by successful `READ 0x16` returning User 1 before the first EQ write. Five intended bands, gain, Apply, and Save followed exactly once; final authoritative reads match all target values. |
| Global EQ gain `0x17` encoding/decoding | CORRECTION IMPLEMENTED; PHYSICAL PASS; FULL QUALIFICATION PENDING | Official FiiO Control V4.6.0 evidence establishes signed 16-bit tenths-of-a-dB, high-byte-first encoding. J017 physically records the corrected source writing and reading `FF D9` as `-3.9 dB`; explicit power-cycle persistence remains pending. |
| Apply command and volatile readback | J029 PHYSICAL PASS FOR ONE TRANSACTION | The J029 trace records one Apply followed by successful volatile program, five-band, and gain reads matching the target before Save. This is transaction evidence for the exact candidate/session, not a general firmware contract. |
| Save User 1 persistence | J029 ORDERED ONE-SAVE/FINAL-READBACK PASS; TRUE POWER-CYCLE PERSISTENCE ACCEPTED | J029 records exactly one accepted Save, the expected detach/fresh generation, and matching final authoritative readback. The accepted true-power-cycle evidence confirms that the saved User 1 custom EQ/global gain is retained. |
| Unplug/reconnect persistence | FINAL CAPABILITY MODEL ACCEPTED | True-power-cycle evidence confirms persistence for Mic, UAC, and saved User 1 custom EQ/global gain. The same evidence and the official FiiO Control comparison classify playback volume and active program/preset as runtime-only: volume returns to the device default and the active program returns to User 1. |
| Fail-closed mismatch handling | SUPPORTED_AND_IMPLEMENTED | A global-gain mismatch prevents Save in the first verification path. Do not weaken the `0.001 dB` check or suppress the error. |
| Same-command stale-response correlation | PARTIALLY NARROWED; NOT PROVEN SAFE | J012 has a valid same-command `0x17` response on an unchanged session with exact event ordering and no detach/reconnect, so session replacement is not the cause of that attempt. The protocol still lacks request identity beyond command matching; delayed same-command responses remain an unresolved risk. |
| Complete baseline capture and failed-operation restoration | J028 AND J029 CAPTURED BASELINES RESTORED; GENERAL POLICY REMAINS BOUNDED | J029 confirmed final Mic On, UAC 2.0, volume 60/60, User 1, 384 kHz, -3.70 dB, and all five original bands. Its temporary My EQs profile and unique Downloads source were removed. This proves restoration for these captured states, not every arbitrary state. No automatic retry is added. |
| Session-generation enforcement across Flash | PRODUCTION TRANSACTION CODE/REGRESSIONS PASS; J029 PHYSICAL TEST C PASS | The J029 report records one Save on session 1/detach 0 and all seven successful final reads on session 2/detach 1. No write follows Save and no stale-session read/write was reused. |
| Output volume, presets, headset/UAC controls | UAC PERSISTENT; VOLUME/PRESET RUNTIME-ONLY ACROSS TRUE POWER LOSS | UAC changed 2.0→1.0→2.0 with fresh readbacks and persisted through the accepted power-cycle. Official FiiO Control comparison and the exact candidate's power-cycle readback show volume returning to 60/device default and the active program returning to User 1. Do not add Save behavior to volume or program selection. |
| Headset/mic restart and automatic DEVICE verification | SOFTWARE GATES PASS; NO MIC WRITE NEEDED IN J028/J029; OFFICIAL FiiO APP EXPECTED-RESET REFERENCE | Mic was already On during J028 and J029, so neither session wrote Mic. The owner reports that Mic changes in the official FiiO app reset/re-enumerate; this is behavioral reference evidence, not packet proof. J020 and J025/J026 remain historical results tied to their exact candidates. |
| Serial as physical-unit continuity evidence | OPTIONAL UNDER MODEL D; MATCH REQUIRED WHEN BOTH SESSIONS PROVIDE A USABLE SERIAL; NO SAME-UNIT CLAIM | J020's replacement session was current but its identity was unavailable. J021 observed a null device serial; J024 observed null from both the device and opened-connection readers. In J029 the descriptor token matched a generic JA11 value and the operation report redacted serial, so that token was treated as unusable for uniqueness. Model D permits restart-state verification only on one sole supported returning candidate with fresh-generation authoritative readback. Serial mismatch rejects the replacement. VID/PID, product name, firmware, and port path are never substituted for a serial match. |
| Firmware update, bootloader, cross-flash, raw command console | UNSAFE_OR_OUT_OF_SCOPE | No JA11 firmware mutation or arbitrary command surface is authorized in this task. |
| Public JA11 support/release claim | UNSAFE_OR_OUT_OF_SCOPE | J029 closes Test C for its exact tuple. One Test D observation and one Save/final-readback run do not establish general power-cycle PEQ persistence or authorize public support. Keep merge, publication, and support claims owner-controlled. |

## Matrix rule

As of 2026-10-10, J029 was the latest physical record for unchanged production source `9493cf030acb440f92e547fc667f6a5399616045`, diagnostic APK SHA `44040493c7263ae7d7e6f41be7389534d07977427cc8002494097fbf3c868848`, and PR #81 head `78deeddd2cce65c22a98fce7e288a608ceb813fd`. Its exact one-Save count, ordered writes, detach/fresh-generation transition, and final readback are verified in paired reports. J028 remains a separate historical record for its prior APK; its report is still missing and its Save count remains unknown. J028 Mic/UAC/Test D outcomes remain attributed to J028. J029 restored the recorded baseline and released the Pixel. No same-unit continuity or general PEQ power-cycle persistence was claimed there. J032 is the later private v0.8.3 beta defaults record above; it qualifies only that exact Set as Default workflow/profile and leaves EQ-bank preservation unverified.

## 2026-09-25 independent protocol-oracle matrix

The following pinned sources were inspected as independent behavioral evidence. None documents the
observed `0xD900` write followed by `0xD9FF` readback, and none proves PEQ Save persistence by a
post-restart readback. Their licenses constrain reuse; no external code was copied.

| Oracle | Pinned revision | License | Agreement with Android | Limit |
| --- | --- | --- | --- | --- |
| [Cyfine ja11-web-control](https://github.com/Cyfine/ja11-web-control/tree/4d4eb83df6fcdf9e20b52e1bdf59a77f463b2c30) | `4d4eb83df6fcdf9e20b52e1bdf59a77f463b2c30` | [MIT](https://github.com/Cyfine/ja11-web-control/blob/4d4eb83df6fcdf9e20b52e1bdf59a77f463b2c30/LICENSE) | VID/PID, report ID, five bands, `0x17` signed LE/2560, and Save framing | Current app omits explicit `0x18` Apply and does not show PEQ post-Save verification |
| [Ircama ja11-config](https://github.com/Ircama/ja11-config/tree/affddff6c9808c33ce8b35b0b9759ff0d7f6e405) | `affddff6c9808c33ce8b35b0b9759ff0d7f6e405` | [EUPL-1.2](https://github.com/Ircama/ja11-config/blob/affddff6c9808c33ce8b35b0b9759ff0d7f6e405/LICENSE) | `0x15`, `0x17`, `0x18`, `0x19`, signed LE/2560 | Save helper does not independently prove global-gain persistence |
| [adithyasource fiiocontrol-oss](https://github.com/adithyasource/fiiocontrol-oss/tree/f38994b3bd51bbc898cfceb5d182a403180df33e) | `f38994b3bd51bbc898cfceb5d182a403180df33e` | [Unlicense](https://github.com/adithyasource/fiiocontrol-oss/blob/f38994b3bd51bbc898cfceb5d182a403180df33e/LICENSE) | PID `0x0102`, five bands, `0x17` signed LE/2560, Save | Repository says reverse-engineered/not completely perfect; Save path does not verify readback |

The oracle convergence supports retaining the current codec and fail-closed comparison. It does
not establish whether J012 is device-side transformation, firmware quantization, stale same-command
response, or another protocol-semantic issue.

## 2026-09-25 J012 exact signed-candidate result

J012 is the latest physical evidence: valid JSON, firmware `2.20`, PID `0x0102`, stable session
`1/0`, no permission request, five matching bands, Apply completed, and no Save. It repeats the
same `0xD900 → 0xD9FF` mismatch as J009. Global-gain codec semantics remain **insufficiently
evidenced on hardware**; Save persistence and restoration remain **not established**. Keep the
capability matrix fail-closed and keep JA11 hardware-validation pending.

## 2026-09-25 exact-candidate failure update

The owner has now reproduced the failure on the exact signed `609911e` candidate. This changes
the evidence classification from “candidate provenance not established” to **physical negative
evidence on the current signed candidate**, but it does not identify the protocol root cause.
The current implementation therefore adds a bounded JA11 transaction report at the shared Flash
boundary. It records the canonical preamp, optimized target, device-domain quantized target,
phase-specific decoded readback, raw request/response bytes, Save count, and session generations.
It does not change the signedness, endian order, `2560` scale, tolerance, retry policy, or
fail-closed mismatch behavior. The next candidate is for diagnosis only; JA11 remains physically
unqualified until the report-backed owner session proves the exact transaction and restoration.

## 2026-09-25 J009 exported transaction result

The owner returned the readable and technical reports from J008. The readable report is
authoritative for the operation because the technical export is malformed JSON. The exact trace
shows a stable-session mismatch before Save:

- Intended and quantized target: `-3.9 dB`, raw `0xD900`, write payload `00 D9`.
- Device readback: raw `0xD9FF`, decoded `-3.800390625 dB`.
- Difference: `255` raw units / `0.099609375 dB`; tolerance was `0.001 dB`.
- Five target bands: all read back exactly.
- Session/detach generations: `1/0` throughout; permission requests: `0`.
- Save commands: `0`; terminal stages: `VOLATILE_READBACK`, then `FAILED`.

This is a diagnosed physical failure, not a protocol fix or qualification result. It rules out a
detach/session replacement cause for this transaction and leaves device-side global-gain
quantization/firmware semantics, or a still-uncharacterized same-command response behavior, as
the remaining protocol questions. Independent public implementations corroborate the existing
`0x17` signed little-endian `2560`-scale codec but do not explain this `0xD9FF` response.

The technical export itself is not parser-valid: each nested object begins with `{,`. This is a
separate software export defect and is being corrected with a deterministic JSON parse test. It
does not alter hardware behavior. Do not repeat Flash or Reset until the export correction is
validated and a bounded protocol decision is made.

## 2026-09-25 exact signed diagnostic candidate

The diagnostic implementation is merged on `main` at `af8c68c35d320223a13c635fac69c0f2ebacdb3f`.
The signed candidate is J008 in the validation ledger:

- APK: [`EQ-Library-v0.7.0-beta-af8c68c.apk`](https://raw.githubusercontent.com/weekssa/OPRA-EQ-for-UAPP/mobile-test-apk/candidates/EQ-Library-v0.7.0-beta-af8c68c.apk).
- APK SHA-256: `3b442cbab3cf8be59a9b8e4ddd0d7028e93f8c4067d94dbb833a5bbb60b1d37e`.
- Signed-beta run: [#1363](https://github.com/weekssa/OPRA-EQ-for-UAPP/actions/runs/36180975485), artifact ID `10884371877`, ZIP SHA-256 `77477ad6bd52e9b114cf18e949368424d8d5c1dbc85246679c9c5fd561d5b44d`.
- Signer: `CN=OPRA EQ for UAPP, O=weekssa`, RSA 4096, certificate SHA-256 `65c1c1256dae3c49e3548f334c91f0ba991969e9be9e0b223ba4e253d2114747`; v2/v3 verified.
- R8 mapping SHA-256: `71036cf05464e6b84f07165e75c17c5a5cd5517a843bd1a8efb0bb49add0b374`.

This is a signed diagnostic artifact, not a support-qualified release. The owner must perform
only the one bounded session in the checklist, export the readable and JSON report, and stop on
missing raw evidence, an unknown baseline, an unexpected disconnect, or uncertain restoration.

## Public `v0.7.0` release boundary — 2026-09-28

The public [v0.7.0 release](https://github.com/weekssa/OPRA-EQ-for-UAPP/releases/tag/v0.7.0) is
published from tagged source `4f325d673159b40515086fe5143df12b29ddb076` with public APK SHA-256
`27dada499bcbf9be9bd21d1349164858c93a5d2b83f78fd61134de13b4eb4025`. The release includes the
software-verified User 1 five-band editor/apply path. This publication fact does not alter the
capability classifications: JA11 remains physically evidence-bounded, with no new power-cycle
retention or broader hardware-support claim.
