# Pinned JA11 reference build and native persistence evidence

## Pass 1: source reconstruction and comparison

Reference: Ircama/ja11-config@affddff6c9808c33ce8b35b0b9759ff0d7f6e405. All eight source blobs were downloaded and independently verified against Git blob IDs. Original files remain untouched privately; only configuration target was built. EUPL-1.2 license retained. The Android project remains Apache-2.0; no reference implementation code has been copied into product source.

Configuration build PASS: `make -B ja11-config-tui CC=/usr/bin/clang CPPFLAGS='-D_DEFAULT_SOURCE -D_GNU_SOURCE -Ihidapi -isysroot /Library/Developer/CommandLineTools/SDKs/MacOSX15.4.sdk' HIDAPI_LDLIBS='hidapi/libhidapi.a -framework IOKit -framework CoreFoundation -framework AppKit'`. Private hidapi-0.15.0 Darwin backend compiled and linked without installation. Initial missing include path and CLT27 SDK/TAPI mismatch were diagnosed; the installed matching 15.4 SDK resolved linking. No global SDK or environment was modified.

A private comparator compiles the original C source with hid_write and usleep intercepted. No hardware handle is opened and no request reaches USB. It verifies five band writes, Apply(1), Save(3), 25ms per band plus 10ms/50ms waits, zero microphone commands, all five filter report vectors, and full Apply's band/Apply/gain/filter/Apply sequence. This proves source-generated command behavior, not JA11 hardware applicability or persistence.

The comparator confirms the reference encodes -3.7dB global gain as `00 DB` (LE/2560). Maintained official-FiiO evidence and accepted Android codec use signed BE/tenths (`FF DB`). The native integration must preserve the qualified Android codec rather than copy the incompatible reference arithmetic.

## Pass 2: corroboration and historical difference

The original settings-only candidate 620e1a46761de2d3621dbcd32135193efb42cf49 sent 0x19 with the active program code; User 1 used 3. Its Off-to-User1 result changed non-flat pre-Save bank to flat final bank and returned FINAL_EQ_MISMATCH. No band/gain staging or Apply was sent. This establishes the failure, not its firmware-level cause.

Independent third-party JA11 PEQ implementations also stage bands before Save(3): adithyasource/fiiocontrol-oss@f38994b3bd51bbc898cfceb5d182a403180df33e src/libs/devices/fiioJa11.js:92-123; Cyfine/ja11-web-control@4d4eb83df6fcdf9e20b52e1bdf59a77f463b2c30 PROTOCOL.md. Cyfine derives PEQ facts from adithyasource and therefore is not a second independent source for those facts. Both third-party gain codecs have the same conflict; neither replaces accepted official gain evidence.

The external reference and maintained physical Flash evidence support staged User 1 persistence. They do not establish that arbitrary program-code Save payloads or fixed Save(3) preserve another active program. A settings-only native path must not infer those semantics, select User1 as a helper for other program saves, or claim undocumented bank readback while another program is active.

## Pass 3: validation decision

Implement and test preserved-current-User1 native Save by exact fresh device-value staging, qualified gain encoding, Apply, volatile readback, one Save, final fresh readback and companion verification. Consider target User1 activation only as the explicit requested target, with complete fresh bank read before staging. Other-program native commit remains insufficiently evidenced; complete source analysis and retain the qualified explicit mic fallback rather than invent payload semantics. This is a bounded implementation decision, not a physical qualification claim. Final candidate must demonstrate no mic writes on native path and preserve the selected target/volume/EQ across true power removal.

## Separate digital-filter status

| Mode | Reference value | Compiled source vector | Independent JA11 applicability/persistence |
|---|---:|---|---|
| FAST-LL | 1 | PASS | DEFERRED: insufficient evidence; no independent value-1 reply, firmware bound, runtime effect or persistence |
| FAST-PC | 2 | PASS | DEFERRED: insufficient evidence; no independent value-2 reply, phase behavior, firmware bound or persistence |
| Slow-LL | 3 | PASS | DEFERRED: insufficient evidence; no independent value-3 reply, roll-off behavior, firmware bound or persistence |
| Slow-PC | 4 | PASS | DEFERRED: insufficient evidence; no independent value-4 reply, phase behavior, firmware bound or persistence |
| NON-OS | 5 | PASS | DEFERRED: insufficient evidence; no independent value-5 reply, disabled-oversampling behavior, firmware bound or persistence |

Private source/build/comparator files and exact checksums are retained in the execution evidence directory. No firmware/boot utility was built or run; no physical/ADB operation occurred.

### Completed three-pass filter investigation

Read-only researcher Maxwell completed source reconstruction, official/upstream comparison, and reconciliation with the intercepted-HID comparator and failure paths. Primary separately inspected the strongest upstream setter. Each mode's mandatory investigation is complete; the optional implementation is deferred individually. These are evidence gaps, not a finding that the hardware lacks filters. No functioning filter control or speculative JA11 command is enabled.

The [pinned Ircama header](https://github.com/Ircama/ja11-config/blob/affddff6c9808c33ce8b35b0b9759ff0d7f6e405/ja11-config-tui.h#L46-L76) labels `0x11` a Savitech command adapted for JA11. Its names claim fast/slow roll-off, linear-phase/low-latency or phase compensation, and non-oversampling. These meanings are author claims, without independent exact-device confirmation. The setter does not confirm readback. The reader accepts `ret > 0` without complete length/footer/correlation validation; invalid or absent replies retain the previous local value, initially FAST-LL. A displayed default therefore is not hardware evidence. The expected read packet is `02 BB 0B 00 00 11 00 00 EE`; the five generated writes are `02 AA 0A 00 00 11 01 vv 00 EE` for `vv=01..05`.

The attributed upstream [Audiocular-Aura setter](https://github.com/mandy321/Audiocular-Aura/blob/2ea1896287448fc8d6170994cf32048185917b8d/src/dsp.ts#L1538-L1553) explicitly targets Savitech CB5100: shared names map to 1..5, report `0x4B` sends `[01,11,01,vv]`, then Savitech Save. Its JA11 receive branch handles gain/PEQ and returns before the generic filter parser. Devices/constants separately classify JA11 as FIIO_JA11. Shared menu wiring is not an independent JA11 implementation. Inference: the common mapping is source lineage rather than independent corroboration. Neither the pinned adithyasource JA11 driver nor Cyfine protocol supplies the missing filter proof; Cyfine's PEQ lineage is not independent of adithyasource.

The official [JA11 FAQ](https://www.jadeaudio.com/details?_l=en&article_id=178) describes five-band PEQ and saved EQ across hosts, without these reconstruction filters. The [firmware history](https://forum.fiio.com/note/showNoteContent.do?id=202406211150090736905) through V2.2 does not establish their mappings or applicability; V1.9 web-PEQ/gain-save improvements do not prove filter support. The inspected [official web bundle](https://fiiocontrol.fiio.com/static/js/index-7hb30NUo.js), SHA-256 `4bc6204460a14434d9790fa1b016182fbf28bc2ec35d810f60abf091b9913d1e`, has generic command `0x04`/enum 0..4 helpers without a demonstrated JA11 binding. It is not a qualified alternative to `0x11`/1..5.

Ircama full Apply includes the filter, but its Save helper stages bands, Apply, Save(3) without staging a pending filter. Neither verifies filter readback or power-loss retention. For each mode, unresolved proof is: an independent JA11-specific valid command/reply; exact firmware/UAC applicability and unsupported behavior; Apply sequencing and runtime effect; filter-specific flash/power-loss persistence; and preservation of bank/gain/program/volume/mic/UAC. Existing PEQ evidence cannot discharge these. EUPL attribution/provenance is retained; no third-party source is incorporated. No hardware probe was performed to manufacture confirmation.
