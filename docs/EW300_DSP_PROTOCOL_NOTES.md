# EW300 DSP cable evidence ledger

Status: discovery only, 2026-09-16. The USB-C cable is the device; headphone identity is separate.

## Owner enumeration evidence

The owner's Pixel 9 screenshot from the first read-only diagnostic reports:

- VID:PID `31B2:0111`, device class/subclass/protocol `0/0/0`.
- Seven Android interface entries (these are enumeration indices, not proven unique USB interface numbers).
- Entry 0: `1/1/0`, no endpoints.
- Entries 1 and 3: `1/2/0`, no endpoints.
- Entry 2: `1/2/0`, isochronous IN `0x81`, maximum packet 96, interval 1.
- Entries 4 and 5: `1/2/0`, isochronous OUT `0x01`, maximum packet 384 and 576 respectively, interval 1.
- Entry 6: `3/0/0`, interrupt IN `0x82` and OUT `0x02`, maximum packet 16, interval 1.

Evidence category: OWNER SCREENSHOT. Raw descriptors, alternate settings, exact HID report format, string/revision identity and stock EQ are pending. Shared VID/PID with another product is not compatibility evidence.

## Current diagnostic boundary

Scan uses Android enumeration only. The separately requested descriptor capture uses standard IN GET_DESCRIPTOR (`bRequest=0x06`, report type `0x22`, interface recipient) after Android permission. It sends no HID report, vendor request, EQ write, save or reset. A descriptor report is not a backup of untouched EQ; that state still must be read and preserved before any write.

Protocol framing, filter count/types, limits, quantization, preamp, bypass, persistence, readback and reset semantics remain unresolved. Public leads in the approved implementation plan are unverified and no third-party protocol code has been adopted by this correction.

## Diagnostic build evidence

The `c008345` signed diagnostic omitted the launcher class and is rejected. See `V0.7_EW300_DSP_STATUS.md`. Android's primary documentation establishes the required Kotlin source-directory API: <https://developer.android.com/build/migrate-to-built-in-kotlin>. This is build-system evidence only.
