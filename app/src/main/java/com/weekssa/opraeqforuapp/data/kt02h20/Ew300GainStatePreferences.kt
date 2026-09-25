package com.weekssa.opraeqforuapp.data.kt02h20

import android.content.Context
import com.weekssa.opraeqforuapp.domain.ew300.Ew300GainStateStore
import com.weekssa.opraeqforuapp.domain.ew300.Ew300PersistencePending
import com.weekssa.opraeqforuapp.domain.ew300.Ew300PersistenceStage

class Ew300GainStatePreferences(context: Context) : Ew300GainStateStore {
    private val preferences = context.applicationContext.getSharedPreferences(
        PREFERENCES_NAME,
        Context.MODE_PRIVATE,
    )

    override fun isGlobalGainQualified(deviceFingerprintKey: String): Boolean =
        preferences.getBoolean(qualifiedKey(deviceFingerprintKey), false)

    override fun markGlobalGainQualified(deviceFingerprintKey: String, qualified: Boolean) {
        preferences.edit().putBoolean(qualifiedKey(deviceFingerprintKey), qualified).apply()
    }

    override fun readAppliedGainDeltaSteps(deviceFingerprintKey: String): Int =
        preferences.getInt(deltaKey(deviceFingerprintKey), 0)

    override fun writeAppliedGainDeltaSteps(deviceFingerprintKey: String, steps: Int) {
        preferences.edit().putInt(deltaKey(deviceFingerprintKey), steps).apply()
    }

    override fun readUserBaselineGainSteps(deviceFingerprintKey: String): Int? =
        if (preferences.contains(userBaselineKey(deviceFingerprintKey))) {
            preferences.getInt(userBaselineKey(deviceFingerprintKey), 0)
        } else {
            null
        }

    override fun writeUserBaselineGainSteps(deviceFingerprintKey: String, steps: Int) {
        preferences.edit().putInt(userBaselineKey(deviceFingerprintKey), steps).apply()
    }

    override fun isPersistenceQualified(deviceFingerprintKey: String): Boolean =
        preferences.getBoolean(persistenceQualifiedKey(deviceFingerprintKey), false)

    override fun markPersistenceQualified(deviceFingerprintKey: String, qualified: Boolean) {
        preferences.edit().putBoolean(persistenceQualifiedKey(deviceFingerprintKey), qualified).apply()
    }

    override fun readPersistencePending(deviceFingerprintKey: String): Ew300PersistencePending? =
        preferences.getString(pendingKey(deviceFingerprintKey), null)?.let(::decodePending)

    override fun writePersistencePending(deviceFingerprintKey: String, pending: Ew300PersistencePending?) {
        preferences.edit().apply {
            if (pending == null) remove(pendingKey(deviceFingerprintKey))
            else putString(pendingKey(deviceFingerprintKey), encodePending(pending))
        }.commit()
    }

    companion object {
        private const val PREFERENCES_NAME = "ew300_flash_state"

        private fun safeKey(prefix: String, fingerprint: String): String =
            "$prefix:${fingerprint.take(160)}"

        private fun qualifiedKey(fingerprint: String) = safeKey("global_gain_qualified", fingerprint)
        private fun deltaKey(fingerprint: String) = safeKey("applied_gain_delta_steps", fingerprint)
        private fun userBaselineKey(fingerprint: String) = safeKey("user_baseline_gain_steps", fingerprint)
        private fun persistenceQualifiedKey(fingerprint: String) = safeKey("persistence_qualified", fingerprint)
        private fun pendingKey(fingerprint: String) = safeKey("persistence_pending", fingerprint)

        private fun encodePending(pending: Ew300PersistencePending): String = listOf(
            "v2",
            pending.stage.name,
            pending.baseline.toSortedMap().entries.joinToString(",") { (register, value) ->
                "${register.toString(16)}=${value.hex()}"
            },
            pending.temporaryBandGain.hex(),
            pending.temporaryPlaybackGain.hex(),
            pending.powerCycleMarker.toString(),
        ).joinToString("|")

        private fun decodePending(encoded: String): Ew300PersistencePending? = runCatching {
            val fields = encoded.split('|')
            require(fields.size == 6 && fields[0] == "v2")
            val baseline = fields[2].split(',').associate { entry ->
                val parts = entry.split('=')
                require(parts.size == 2)
                parts[0].toInt(16) to parts[1].hexBytes()
            }
            Ew300PersistencePending(
                stage = Ew300PersistenceStage.valueOf(fields[1]),
                baseline = baseline,
                temporaryBandGain = fields[3].hexBytes(),
                temporaryPlaybackGain = fields[4].hexBytes(),
                powerCycleMarker = fields[5].toLong(),
            ).also { pending ->
                require(pending.baseline.isNotEmpty())
                require(pending.baseline.values.all { it.size == 4 })
                require(pending.temporaryBandGain.size == 4)
                require(pending.temporaryPlaybackGain.size == 4)
            }
        }.getOrNull()

        private fun ByteArray.hex(): String = joinToString("") { "%02X".format(it.toInt() and 0xFF) }

        private fun String.hexBytes(): ByteArray {
            require(length % 2 == 0)
            return chunked(2).map { it.toInt(16).toByte() }.toByteArray()
        }
    }
}
