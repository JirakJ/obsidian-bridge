package com.jakubjirak.obsidianbridge.core

import com.jakubjirak.obsidianbridge.actions.noteFileName
import com.jakubjirak.obsidianbridge.core.model.VaultConfig
import com.jakubjirak.obsidianbridge.ui.settings.ObsidianBridgeSettings
import com.jakubjirak.obsidianbridge.ui.settings.SettingsState
import com.jakubjirak.obsidianbridge.ui.toolwindow.TagHierarchy
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.io.TempDir
import java.nio.file.Files
import java.nio.file.Path

class VaultHelpersTest {

    private val a = VaultConfig(id = "a", path = "/home/me/vault", name = "A")
    private val nested = VaultConfig(id = "n", path = "/home/me/vault/sub", name = "N")
    private val b = VaultConfig(id = "b", path = "/home/me/vault2/", name = "B")

    @Test
    fun `relative path only for files inside the vault`() {
        assertEquals("notes/x.md", VaultManager.relativeToVault(a, "/home/me/vault/notes/x.md"))
        assertNull(VaultManager.relativeToVault(a, "/home/me/vault2/x.md"))
        assertNull(VaultManager.relativeToVault(a, "/home/me/vault"))
        assertEquals("x.md", VaultManager.relativeToVault(b, "/home/me/vault2/x.md"))
        assertNull(VaultManager.relativeToVault(VaultConfig(path = ""), "/x.md"))
    }

    @Test
    fun `most specific vault wins and sibling prefixes do not match`() {
        val vaults = listOf(a, nested, b)
        assertEquals("n", VaultManager.vaultForPath(vaults, "/home/me/vault/sub/x.md")?.id)
        assertEquals("a", VaultManager.vaultForPath(vaults, "/home/me/vault/x.md")?.id)
        assertEquals("b", VaultManager.vaultForPath(vaults, "/home/me/vault2/x.md")?.id)
        assertNull(VaultManager.vaultForPath(vaults, "/elsewhere/x.md"))
    }

    @Test
    fun `vault validity requires an obsidian folder`(@TempDir dir: Path) {
        assertFalse(VaultManager.isValidVault(dir.toString()))
        Files.createDirectories(dir.resolve(".obsidian"))
        assertTrue(VaultManager.isValidVault(dir.toString()))
        assertFalse(VaultManager.isValidVault(""))
    }

    @Test
    fun `settings page edits only the primary vault`() {
        val extra = VaultConfig(id = "x", path = "/x", name = "X")
        val created = ObsidianBridgeSettings.withPrimaryVault(SettingsState(), " /v/notes/ ", "", false)
        assertEquals("/v/notes/", created.vaults.single().path)
        assertEquals("notes", created.vaults.single().name)
        assertFalse(created.showGutterIcons)

        val edited = ObsidianBridgeSettings.withPrimaryVault(SettingsState(listOf(a, extra)), "/new", "New", true)
        assertEquals(listOf("/new", "/x"), edited.vaults.map { it.path })
        assertEquals("a", edited.vaults[0].id)

        val removed = ObsidianBridgeSettings.withPrimaryVault(SettingsState(listOf(a, extra)), " ", "", true)
        assertEquals(listOf("x"), removed.vaults.map { it.id })
    }

    @Test
    fun `tag hierarchy nests segments and sums counts`() {
        val root = TagHierarchy.build(mapOf("project" to 2, "project/alpha" to 1, "project/beta" to 1, "pro" to 1))
        assertEquals(listOf("pro", "project"), root.children.keys.toList())
        val project = root.children.getValue("project")
        assertEquals(4, project.count)
        assertEquals(listOf("alpha", "beta"), project.children.keys.toList())
        assertEquals("project/alpha", project.children.getValue("alpha").path)
        assertEquals("alpha (1)", project.children.getValue("alpha").toString())
        assertEquals(1, root.children.getValue("pro").count)
    }

    @Test
    fun `note file names keep unicode letters`() {
        assertEquals("poznámky-z-porady", noteFileName("  Poznámky z  porady! "))
        assertEquals("a_b-1", noteFileName("a_b 1"))
        assertEquals("", noteFileName("?!*"))
    }
}
