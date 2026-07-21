package com.hermes.downloader.domain.queue

import org.junit.Assert.assertNotEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class TaskIdFactoryTest {

    @Test
    fun `new ids are unique standard UUIDs`() {
        val firstId = TaskIdFactory.newId()
        val secondId = TaskIdFactory.newId()

        assertNotEquals(firstId, secondId)
        assertTrue(firstId.matches(Regex("[0-9a-f]{8}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{12}")))
        assertTrue(secondId.matches(Regex("[0-9a-f]{8}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{12}")))
    }
}
