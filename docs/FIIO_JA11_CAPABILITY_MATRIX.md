# FiiO JA11 capability matrix

This matrix is the current evidence boundary for the exact FiiO JA11 identity. Software
implementation does not imply physical support. Unknown behavior stays insufficiently evidenced
or unsafe rather than being inherited from another KT02H20-family device.

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

| Capability | Decision | Evidence / boundary |
| --- | --- | --- |
| Exact JA11 USB identity, VID `0x2972`, UAC PIDs `0x0101`/`0x0102` | SUPPORTED_AND_IMPLEMENTED; physical pending | Strict allowlist and dynamic HID interface discovery are implemented. Physical identity/PID for J001 was not captured. |
| Five-band Peak/Low Shelf/High Shelf target representation | SUPPORTED_AND_IMPLEMENTED; physical pending | Shared finite-hardware adapter, complete five-slot target, and codec tests exist. J001 displayed a 9→5 optimized plan but did not provide readback values. |
| Global EQ gain `0x17` encoding/decoding | CORRECTION IMPLEMENTED; PHYSICAL PASS; FULL QUALIFICATION PENDING | Official FiiO Control V4.6.0 evidence establishes signed 16-bit tenths-of-a-dB, high-byte-first encoding. J017 physically records the corrected source writing and reading `FF D9` as `-3.9 dB`; explicit power-cycle persistence remains pending. |
| Apply command and volatile readback | SOFTWARE-SUPPORTED; PHYSICAL SEMANTICS INSUFFICIENTLY_EVIDENCED | A supplied My DAC frame shows the optimized five-band target present while connected, which supports volatile application of the band plan. It does not prove the gain wire value or the exact transaction phase. |
| Save User 1 persistence | FLASH/SAVE/FINAL-READBACK AND OBSERVED RECONNECT PASS; POWER-CYCLE PERSISTENCE PENDING | J017 records exactly one Save and final readback, then a later session/detach generation (`3/2` versus `1/0`) with the flashed target present as the Reset baseline. The observed reconnect path passed; no explicit power-removal marker or duration is recorded. |
| Unplug/reconnect persistence | OBSERVED RECONNECT PATH PASS; FULL POWER-CYCLE PERSISTENCE PENDING | J017's Reset report begins after the session/detach generations advanced and its final readback matches the original flat baseline. This is strong evidence for the observed detach/reconnect history, not proof of a separately identified full power cycle. |
| Fail-closed mismatch handling | SUPPORTED_AND_IMPLEMENTED | A global-gain mismatch prevents Save in the first verification path. Do not weaken the `0.001 dB` check or suppress the error. |
| Same-command stale-response correlation | PARTIALLY NARROWED; NOT PROVEN SAFE | J012 has a valid same-command `0x17` response on an unchanged session with exact event ordering and no detach/reconnect, so session replacement is not the cause of that attempt. The protocol still lacks request identity beyond command matching; delayed same-command responses remain an unresolved risk. |
| Complete baseline capture and failed-operation restoration | EXACT FLAT-STATE RESTORATION PASS; GENERAL RESTORATION PENDING | J017's Reset report captures the flashed target as baseline and ends with a final raw readback matching the Flash report's original flat baseline. This proves the observed flat-state restoration path, not arbitrary-state restoration. No automatic retry is added. |
| Session-generation enforcement across Flash | SUPPORTED_AND_IMPLEMENTED; AUTOMATED GATES PASS; PHYSICAL PENDING | The JA11 Android transport pins reads and ordinary writes to one session/detach generation; Save remains the explicit lifecycle exception and waits for its reconnect boundary. Focused tests, Android CI, signed emulator install/cold launch, and the exact signed candidate all pass. Physical lifecycle behavior remains unqualified. |
| Output volume, presets, headset/UAC controls | SOFTWARE IMPLEMENTED; PHYSICAL PENDING | These controls are outside the failed EQ-gain root-cause boundary; owner reports that general controls work do not qualify Flash or persistence. |
| Headset/mic restart and automatic DEVICE verification | IMPLEMENTED; J020 INCOMPLETE/NEGATIVE ON SUPERSEDED CANDIDATE; CURRENT FIX PHYSICAL PENDING | J020's On-to-Off write completed and a fresh session read Mic Off, but no automatic verifier event was emitted. Permission arrived about 18.5 seconds after request; its interaction with the former 10-second fallback is fixed in software and has automated delayed-grant coverage. Original Mic On restoration remains outstanding. |
| Stable JA11 identity across restart | UNIQUE NONBLANK USB SERIAL REQUIRED; JA11-ONLY OPENED-CONNECTION FALLBACK IMPLEMENTED; PHYSICAL RESULT PENDING | J020's replacement session was current but identity was unavailable. J021 separately confirms that the initial-session `UsbDevice.serialNumber` getter returned null with permission granted; it does not prove the descriptor lacks a serial or resolve J020. Source `616958037349e2f0e0784a556c6430b0de6ceb18` reads the opened connection's serial only when that getter returns null, only for JA11; seven new session regressions and local gates pass. Before any write, the read-only helper must see granted permission, JA11 PID `257`/`258`, `serialStatus=READABLE_NULL`, `connectionSerialStatus=READABLE_NONBLANK`, and `serialSource=USB_CONNECTION` before a matching `USB_SESSION_OPENED`, followed by a complete snapshot and current unique-identity result for the same generation. It confirms the emitting process is still live and rejects a later same-generation detach/close. It reports categories only, never the serial. If that real-device proof is unavailable, stop and release the Pixel. |
| Firmware update, bootloader, cross-flash, raw command console | UNSAFE_OR_OUT_OF_SCOPE | No JA11 firmware mutation or arbitrary command surface is authorized in this task. |
| Public JA11 support/release claim | UNSAFE_OR_OUT_OF_SCOPE | J017 closes the observed reconnect/restoration evidence gap for one session but does not close explicit power-cycle retention or the complete qualification checklist. Keep public support and final-release claims owner-controlled. |

## Matrix rule

J020 remains the latest mutation record for headset restart verification; J021 is a separate
read-only initial-session identity observation, and J017 remains the accepted Flash/Reset and
restoration record. J020's write was not retried; its last verified state is Mic Off and restoration
to the original Mic On baseline is outstanding. J021 proves only that the Android device serial
getter returned null with permission granted. Do not convert either record's evidence gaps into a
weaker identity key, protocol change, tolerance change, retry, or support claim. The next physical
session must use the exact source-bound connection-serial candidate and verify fresh identity before
any write. If the complete fresh baseline reads Mic Off, restore Mic On before other tests; if it
already reads On, record restoration as satisfied and skip Mic writes. Stop if replacement identity
is unavailable. The current candidate is the exact source in the 2026-10-08 section above; verify its
latest PR checks before using the Pixel.

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
