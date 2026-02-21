package com.jakubjirak.obsidianbridge.util

import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.Test

class PathUtilsTest {
    @Test
    fun `ensureMdExtension adds md when missing`() {
        assertEquals("note.md", "note".ensureMdExtension())
    }

    @Test
    fun `ensureMdExtension does not double add`() {
        assertEquals("note.md", "note.md".ensureMdExtension())
    }

    @Test
    fun `ensureMdExtension handles path with dots`() {
        assertEquals("my.note.md", "my.note".ensureMdExtension())
    }

    @Test
    fun `ensureMdExtension handles empty string`() {
        assertEquals(".md", "".ensureMdExtension())
    }
}
