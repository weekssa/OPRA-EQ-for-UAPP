package com.weekssa.opraeqforuapp.data.security

import org.junit.Assert.assertEquals
import org.junit.Test

class ReleaseSignatureGateTest {
    @Test
    fun certificateDigestUsesCanonicalUppercaseSha256() {
        assertEquals(
            "BA7816BF8F01CFEA414140DE5DAE2223B00361A396177A9CB410FF61F20015AD",
            ReleaseSignatureGate.certificateSha256("abc".encodeToByteArray()),
        )
    }
}
