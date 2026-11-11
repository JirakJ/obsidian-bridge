package com.jakubjirak.obsidianbridge.core

import com.jakubjirak.obsidianbridge.core.model.VaultConfig
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertNotNull
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.io.TempDir
import java.nio.file.Files
import java.nio.file.Path

class NoteIndexTest {

    @TempDir
    lateinit var tempDir: Path

    private lateinit var vault: Path
    private lateinit var config: VaultConfig
    private val index = NoteIndex()

    private fun write(rel: String, content: String): Path {
        val file = vault.resolve(rel)
        Files.createDirectories(file.parent)
        Files.writeString(file, content)
        return file
    }

    @BeforeEach
    fun setUp() {
        vault = tempDir.resolve("vault")
        Files.createDirectories(vault.resolve(".obsidian"))
        config = VaultConfig(id = "v1", path = vault.toString(), name = "Vault")
        write("Home.md", "---\ntags: [hub]\n---\n# Home\nSee [[Projects/Alpha]] and [[Ideas|my ideas]] and [[Missing]].")
        write("Projects/Alpha.md", "---\ntitle: Alpha\naliases:\n  - A1\n---\nBack to [[Home]]. #project/alpha")
        write("Ideas.md", "Ideas list, links to [[A1]] #project")
        write(".obsidian/plugins.md", "[[Home]]")
        write(".trash/old.md", "[[Home]]")
        write("notes.txt", "[[Home]]")
        index.indexVault(config)
    }

    private fun note(title: String) = index.getAllNotes().single { it.title == title }

    @Test
    fun `indexes markdown files and skips excluded folders and other files`() {
        assertEquals(setOf("Home.md", "Projects/Alpha.md", "Ideas.md"), index.getAllNotes().map { it.relativePath }.toSet())
    }

    @Test
    fun `note metadata comes from frontmatter, file name and content`() {
        val alpha = note("Alpha")
        assertEquals(listOf("A1"), alpha.aliases)
        assertTrue("project/alpha" in alpha.tags)
        assertEquals("Projects", alpha.directory)
        assertTrue(alpha.body.startsWith("Back to [[Home]]"))
        assertEquals(listOf("hub"), note("Home").tags)
    }

    @Test
    fun `lookups by name, path and alias`() {
        assertEquals("Alpha", index.findByName("alpha").single().title)
        assertEquals("Alpha", index.findByName("Alpha.md").single().title)
        assertEquals("Alpha", index.findByAlias("a1").single().title)
        assertNotNull(index.findByRelativePath("v1", "Projects\\Alpha.md"))
        assertNull(index.findByRelativePath("other", "Home.md"))
    }

    @Test
    fun `backlinks resolve names, paths and aliases`() {
        val home = note("Home")
        assertEquals(listOf("Alpha"), index.getBacklinks(home.id).map { it.source.title })
        val alpha = note("Alpha")
        assertEquals(setOf("Home", "Ideas"), index.getBacklinks(alpha.id).map { it.source.title }.toSet())
        assertEquals(5, index.getBacklinks(alpha.id).first { it.source.title == "Home" }.link.sourceLineNumber)
    }

    @Test
    fun `edges contain only resolved, distinct links`() {
        val edges = index.edges()
        val title = { id: String -> index.findById(id)!!.title }
        assertEquals(
            setOf("Home->Alpha", "Home->Ideas", "Alpha->Home", "Ideas->Alpha"),
            edges.map { "${title(it.fromId)}->${title(it.toId)}" }.toSet()
        )
        assertEquals(edges.size, edges.distinct().size)
    }

    @Test
    fun `tag counts aggregate notes`() {
        assertEquals(mapOf("hub" to 1, "project" to 1, "project/alpha" to 1), index.tagCounts())
    }

    @Test
    fun `re-indexing replaces the vault's notes and reflects deletions`() {
        Files.delete(vault.resolve("Ideas.md"))
        index.indexVault(config)
        assertEquals(2, index.getAllNotes().size)
        assertTrue(index.findByName("Ideas").isEmpty())
    }

    @Test
    fun `single files can be indexed and removed incrementally`() {
        var changes = 0
        val listener: () -> Unit = { changes++ }
        index.addChangeListener(listener)
        val file = write("New.md", "Points to [[Home]]")
        index.indexFile(config, vault, file)
        assertEquals(2, index.getBacklinks(note("Home").id).size)
        index.indexFile(config, vault, write("New.md", "no links now"))
        assertEquals(1, index.getBacklinks(note("Home").id).size)
        assertEquals(4, index.getAllNotes().size)
        index.removeByPath("v1", "New.md")
        assertEquals(3, index.getAllNotes().size)
        index.removeChangeListener(listener)
        index.clear()
        assertEquals(3, changes)
        assertTrue(index.getAllNotes().isEmpty())
    }

    @Test
    fun `excluded files are not indexed incrementally`() {
        index.indexFile(config, vault, vault.resolve(".trash/old.md"))
        assertEquals(3, index.getAllNotes().size)
    }

    @Test
    fun `missing vault directory is ignored`() {
        val empty = NoteIndex()
        empty.indexVault(VaultConfig(id = "x", path = tempDir.resolve("nope").toString(), name = "x"))
        assertTrue(empty.getAllNotes().isEmpty())
    }

    @Test
    fun `exclusion and path helpers`() {
        val cfg = VaultConfig(excludePatterns = listOf(".obsidian/**", "Archive", ""))
        assertTrue(NoteIndex.isExcluded(cfg, ".obsidian/x.md"))
        assertTrue(NoteIndex.isExcluded(cfg, "Archive/2020/x.md"))
        assertFalse(NoteIndex.isExcluded(cfg, "Archived.md"))
        assertFalse(NoteIndex.isExcluded(cfg, "notes/x.md"))
        assertEquals("a/b.md", NoteIndex.normalize("./a\\b.md"))
    }

    @Test
    fun `note ids are stable per vault and path`() {
        val a = NoteIndex.buildNote("v", "x.md", "one")
        val b = NoteIndex.buildNote("v", "x.md", "two")
        val c = NoteIndex.buildNote("w", "x.md", "one")
        assertEquals(a.id, b.id)
        assertFalse(a.id == c.id)
        assertFalse(a.contentHash == b.contentHash)
    }
}
