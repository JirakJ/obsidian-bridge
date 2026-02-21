package dev.htmeta.obsidianbridge.core

import dev.htmeta.obsidianbridge.core.model.VaultConfig
import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.io.TempDir
import java.nio.file.Files
import java.nio.file.Path

class VaultManagerTest {

    @TempDir
    lateinit var tempDir: Path

    @Test
    fun `VaultConfig defaults are sensible`() {
        val config = VaultConfig(path = "/tmp/vault", name = "Test")
        assertTrue(config.isActive)
        assertEquals("yyyy-MM-dd", config.dailyNoteFormat)
        assertEquals("daily", config.dailyNoteFolder)
        assertTrue(config.excludePatterns.contains(".obsidian/**"))
    }

    @Test
    fun `valid vault has dot-obsidian directory`() {
        val vaultDir = tempDir.resolve("my-vault")
        Files.createDirectories(vaultDir.resolve(".obsidian"))
        Files.writeString(vaultDir.resolve("note.md"), "# Test")
        assertTrue(Files.isDirectory(vaultDir.resolve(".obsidian")))
    }

    @Test
    fun `invalid vault lacks dot-obsidian directory`() {
        val notVault = tempDir.resolve("not-a-vault")
        Files.createDirectories(notVault)
        assertFalse(Files.isDirectory(notVault.resolve(".obsidian")))
    }
}
