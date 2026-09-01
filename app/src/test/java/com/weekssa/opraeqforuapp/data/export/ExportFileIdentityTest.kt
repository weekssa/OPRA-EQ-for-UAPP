package com.weekssa.opraeqforuapp.data.export

import org.junit.Assert.assertEquals
import org.junit.Test

class ExportFileIdentityTest {
    @Test
    fun providerAdjustedDisplayNameBecomesTrackedOwnedName() {
        assertEquals(
            "Edition XS - Creator - Target.txt.txt",
            persistedExportFileName(
                requestedName = "Edition XS - Creator - Target.txt",
                providerName = "Edition XS - Creator - Target.txt.txt",
            ),
        )
    }

    @Test
    fun requestedNameIsUsedOnlyWhenProviderDoesNotReturnOne() {
        assertEquals(
            "Edition XS - Creator - Target.txt",
            persistedExportFileName("Edition XS - Creator - Target.txt", null),
        )
        assertEquals(
            "Edition XS - Creator - Target.txt",
            persistedExportFileName("Edition XS - Creator - Target.txt", ""),
        )
    }
}
