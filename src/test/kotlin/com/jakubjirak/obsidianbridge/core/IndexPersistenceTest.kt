package com.jakubjirak.obsidianbridge.core

import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.Test

class IndexPersistenceTest {
    @Test
    fun `IndexCacheState default values`() {
        val state = IndexCacheState()
        assertTrue(state.noteHashes.isEmpty())
        assertEquals(0L, state.lastIndexTimestamp)
    }

    @Test
    fun `noteHashes can be populated`() {
        val state = IndexCacheState()
        state.noteHashes["note1.md"] = "abc123"
        state.noteHashes["note2.md"] = "def456"
        assertEquals(2, state.noteHashes.size)
        assertEquals("abc123", state.noteHashes["note1.md"])
    }

    @Test
    fun `timestamp can be updated`() {
        val state = IndexCacheState()
        state.lastIndexTimestamp = 1234567890L
        assertEquals(1234567890L, state.lastIndexTimestamp)
    }
}
