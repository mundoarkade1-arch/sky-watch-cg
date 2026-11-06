package com.skywatch.cg.util

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class AppVersionComparatorTest {
    @Test
    fun comparesPatchVersionsNumerically() {
        assertEquals(1, AppVersionComparator.compare("v1.0.17", "1.0.16"))
        assertEquals(-1, AppVersionComparator.compare("1.0.9", "1.0.10"))
    }

    @Test
    fun treatsEquivalentVersionsAsEqual() {
        assertEquals(0, AppVersionComparator.compare("v1.0.17", "1.0.17"))
        assertEquals(0, AppVersionComparator.compare("1.2", "1.2.0"))
    }

    @Test
    fun rejectsNonNumericVersions() {
        assertNull(AppVersionComparator.compare("v1.0-preview", "1.0.0"))
        assertNull(AppVersionComparator.compare("v1.0.1", "release"))
    }
}
