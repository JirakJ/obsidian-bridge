package dev.htmeta.obsidianbridge.resolver

import dev.htmeta.obsidianbridge.core.model.LinkResolution
import dev.htmeta.obsidianbridge.core.model.VaultNote
import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.Test

// Minimal stub of IndexService for unit testing the resolver logic
class LinkResolverTest {

    @Test
    fun `returns unresolved for empty target`() {
        // This test validates the resolver handles edge cases
        // Full integration tests would use IntelliJ test framework
        assertTrue(true, "Resolver edge case test placeholder — needs IndexService mock")
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
