package com.weekssa.opraeqforuapp.data.security

import android.content.Context
import android.content.pm.PackageManager
import android.os.Build
import java.security.MessageDigest

/** Refuses signed-candidate-only hardware operations under any other APK signing identity. */
object ReleaseSignatureGate {
    private const val PINNED_RELEASE_CERTIFICATE_SHA256 =
        "65C1C1256DAE3C49E3548F334C91F0BA991969E9BE9E0B223BA4E253D2114747"

    @Suppress("DEPRECATION")
    fun isPinnedReleaseSigner(context: Context): Boolean = runCatching {
        val signatures = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
            context.packageManager.getPackageInfo(
                context.packageName,
                PackageManager.GET_SIGNING_CERTIFICATES,
            ).signingInfo?.apkContentsSigners
        } else {
            context.packageManager.getPackageInfo(
                context.packageName,
                PackageManager.GET_SIGNATURES,
            ).signatures
        }
        signatures?.size == 1 &&
            certificateSha256(signatures.single().toByteArray()) == PINNED_RELEASE_CERTIFICATE_SHA256
    }.getOrDefault(false)

    internal fun certificateSha256(certificate: ByteArray): String =
        MessageDigest.getInstance("SHA-256")
            .digest(certificate)
            .joinToString("") { "%02X".format(it.toInt() and 0xFF) }
}
