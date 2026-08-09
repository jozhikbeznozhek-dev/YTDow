package com.hermes.downloader

import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class VersionComparatorTest {
    @Test
    fun detectsNewerSemanticVersion() {
        assertTrue(VersionComparator.isNewer("2.2.0", "2.1.0"))
        assertTrue(VersionComparator.isNewer("v2.10.0", "2.9.9"))
        assertTrue(VersionComparator.isNewer("3", "2.9.9"))
    }

    @Test
    fun rejectsSameOlderAndInvalidVersions() {
        assertFalse(VersionComparator.isNewer("2.2", "2.2.0"))
        assertFalse(VersionComparator.isNewer("1.1.0", "2.2.0"))
        assertFalse(VersionComparator.isNewer("latest", "2.2.0"))
        assertFalse(VersionComparator.isNewer("", "2.2.0"))
    }
}
