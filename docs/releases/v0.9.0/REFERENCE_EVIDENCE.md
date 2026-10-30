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

## Pass 3: validation decision (pending implementation)

Implement and test preserved-current-User1 native Save by exact fresh device-value staging, qualified gain encoding, Apply, volatile readback, one Save, final fresh readback and companion verification. Consider target User1 activation only as the explicit requested target, with complete fresh bank read before staging. Other-program native commit remains insufficiently evidenced; complete source analysis and retain the qualified explicit mic fallback rather than invent payload semantics. This is a bounded implementation decision, not a physical qualification claim. Final candidate must demonstrate no mic writes on native path and preserve the selected target/volume/EQ across true power removal.

## Separate digital-filter status

| Mode | Reference value | Compiled source vector | Independent JA11 applicability/persistence |
|---|---:|---|---|
| FAST-LL | 1 | PASS | Investigation in progress |
| FAST-PC | 2 | PASS | Investigation in progress |
| Slow-LL | 3 | PASS | Investigation in progress |
| Slow-PC | 4 | PASS | Investigation in progress |
| NON-OS | 5 | PASS | Investigation in progress |

Private source/build/comparator files and exact checksums are retained in the execution evidence directory. No firmware/boot utility was built or run; no physical/ADB operation occurred.
