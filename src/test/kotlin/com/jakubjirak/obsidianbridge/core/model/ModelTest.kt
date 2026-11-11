package com.jakubjirak.obsidianbridge.core.model

import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.Test

class ModelTest {
    @Test
    fun `VaultNote directory returns parent path`() {
        val note = VaultNote(id = "1", vaultId = "v1", relativePath = "folder/sub/note.md", title = "note")
        assertEquals("folder/sub", note.directory)
    }

    @Test
    fun `VaultNote directory returns empty for root note`() {
        val note = VaultNote(id = "1", vaultId = "v1", relativePath = "note.md", title = "note")
        assertEquals("", note.directory)
    }

    @Test
    fun `VaultNote defaults are sensible`() {
        val note = VaultNote(id = "1", vaultId = "v1", relativePath = "note.md", title = "Test")
        assertTrue(note.aliases.isEmpty())
        assertTrue(note.tags.isEmpty())
        assertTrue(note.frontmatter.isEmpty())
        assertTrue(note.outgoingLinks.isEmpty())
    }

    @Test
    fun `WikiLink stores all fields`() {
        val link = WikiLink(
            raw = "[[target|alias]]", target = "target", alias = "alias",
            heading = "h1", blockRef = null, isEmbed = false,
            sourceNoteId = "s1", sourceLineNumber = 5
        )
        assertEquals("target", link.target)
        assertEquals("alias", link.alias)
        assertEquals("h1", link.heading)
        assertNull(link.blockRef)
        assertFalse(link.isEmbed)
        assertEquals(5, link.sourceLineNumber)
    }

    @Test
    fun `WikiLink embed flag works`() {
        val link = WikiLink(raw = "![[image.png]]", target = "image.png", isEmbed = true)
        assertTrue(link.isEmbed)
    }

    @Test
    fun `LinkResolution sealed class variants`() {
        val note = VaultNote(id = "1", vaultId = "v1", relativePath = "note.md", title = "Test")
        val resolved = LinkResolution.Resolved(note)
        val unresolved = LinkResolution.Unresolved
        val ambiguous = LinkResolution.Ambiguous(listOf(note))

        assertTrue(resolved is LinkResolution)
        assertTrue(unresolved is LinkResolution)
        assertTrue(ambiguous is LinkResolution)
        assertEquals(note, (resolved as LinkResolution.Resolved).note)
    }

    @Test
    fun `VaultConfig daily note defaults`() {
        val config = VaultConfig(path = "/vault", name = "Test")
        assertEquals("yyyy-MM-dd", config.dailyNoteFormat)
        assertEquals("daily", config.dailyNoteFolder)
        assertTrue(config.isActive)
    }

    @Test
    fun `VaultConfig custom exclude patterns`() {
        val config = VaultConfig(path = "/vault", name = "Test", excludePatterns = listOf("private/**"))
        assertEquals(1, config.excludePatterns.size)
        assertEquals("private/**", config.excludePatterns[0])
    }

    @Test
    fun `VaultConfig no-arg constructor for XML serialization`() {
        val config = VaultConfig()
        assertEquals("", config.path)
        assertEquals("", config.name)
        assertTrue(config.id.isNotEmpty())
        assertEquals("yyyy-MM-dd", config.dailyNoteFormat)
        assertEquals("daily", config.dailyNoteFolder)
        assertTrue(config.isActive)
    }

    @Test
    fun `VaultConfig fields are mutable for serialization`() {
        val config = VaultConfig()
        config.path = "/my/vault"
        config.name = "My Vault"
        config.dailyNoteFolder = "journal"
        config.isActive = false
        assertEquals("/my/vault", config.path)
        assertEquals("My Vault", config.name)
        assertEquals("journal", config.dailyNoteFolder)
        assertFalse(config.isActive)
    }
}
