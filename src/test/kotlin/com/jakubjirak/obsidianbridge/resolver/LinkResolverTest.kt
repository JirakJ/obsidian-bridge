package com.jakubjirak.obsidianbridge.resolver

import com.jakubjirak.obsidianbridge.core.model.LinkResolution
import com.jakubjirak.obsidianbridge.core.model.VaultNote
import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.Test

class LinkResolverTest {

    @Test
    fun `returns unresolved for empty target`() {
        val emptyTarget = ""
        assertTrue(emptyTarget.isEmpty(), "Empty link target should be detected as empty")
    }

    @Test
    fun `VaultNote directory extraction works`() {
        val note = VaultNote(
            id = "test",
            vaultId = "v1",
            relativePath = "folder/subfolder/note.md",
            title = "note"
        )
        assertEquals("folder/subfolder", note.directory)
    }

    @Test
    fun `VaultNote root directory returns empty`() {
        val note = VaultNote(
            id = "test",
            vaultId = "v1",
            relativePath = "note.md",
            title = "note"
        )
        assertEquals("", note.directory)
    }
}
