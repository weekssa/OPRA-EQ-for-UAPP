package com.weekssa.opraeqforuapp.domain.ew300

/**
 * Decoder for the public KT Micro fallback's observed EW300 field layout.
 *
 * Every value here is explicitly provisional: the raw transport is qualified on one cable, but
 * the acoustic units and filter semantics still require independent confirmation. Keep this
 * decoder separate from production EQ conversion so a future correction cannot silently alter a
 * supported DAC.
 */
object Ew300ProvisionalDecoder {
    data class BandValues(
        val bandIndex: Int,
        val gainDb: Double,
        val frequencyRaw: Int,
        /** Direct-Hz interpretation described by the public KT02H20 reference; still unverified. */
        val referenceFrequencyHz: Double,
        val q: Double,
        val filterTypeRaw: Int,
        val filterType: Ew300QualifiedVolatileProtocol.ProvisionalFilterType?,
    )

    fun decode(gainField: Ew300VolatileTransaction.RawField, qField: Ew300VolatileTransaction.RawField): BandValues {
        require(gainField.address in 0x26..0x2E && gainField.address % 2 == 0) {
            "EW300 gain/frequency field must be an even register from 0x26 through 0x2E."
        }
        require(qField.address == gainField.address + 1) {
            "EW300 Q/type field must immediately follow its gain/frequency field."
        }

        val gainBytes = gainField.data()
        val qBytes = qField.data()
        val gainRaw = signedLe16(gainBytes[0], gainBytes[1])
        val frequencyRaw = unsignedLe16(gainBytes[2], gainBytes[3])
        val qRaw = unsignedLe16(qBytes[0], qBytes[1])
        val filterTypeRaw = qBytes[2].toInt() and 0xFF

        return BandValues(
            bandIndex = (gainField.address - 0x26) / 2 + 1,
            gainDb = gainRaw / 10.0,
            frequencyRaw = frequencyRaw,
            referenceFrequencyHz = frequencyRaw.toDouble(),
            q = qRaw / 1000.0,
            filterTypeRaw = filterTypeRaw,
            filterType = Ew300QualifiedVolatileProtocol.provisionalFilterType(filterTypeRaw),
        )
    }

    private fun unsignedLe16(low: Byte, high: Byte): Int =
        (low.toInt() and 0xFF) or ((high.toInt() and 0xFF) shl 8)

    private fun signedLe16(low: Byte, high: Byte): Int {
        val unsigned = unsignedLe16(low, high)
        return if (unsigned >= 0x8000) unsigned - 0x10000 else unsigned
    }
}
