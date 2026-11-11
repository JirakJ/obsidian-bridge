package com.jakubjirak.obsidianbridge.resolver

import com.jakubjirak.obsidianbridge.core.NoteIndex
import com.jakubjirak.obsidianbridge.core.model.LinkResolution
import com.jakubjirak.obsidianbridge.core.model.VaultConfig
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.io.TempDir
import java.nio.file.Files
import java.nio.file.Path

class LinkResolverTest {

    @TempDir
    lateinit var tempDir: Path

    private val index = NoteIndex()
    private val resolver = LinkResolver(index)

    private fun vault(id: String, files: Map<String, String>) {
        val root = tempDir.resolve(id)
        files.forEach { (rel, content) ->
            val f = root.resolve(rel)
            Files.createDirectories(f.parent)
            Files.writeString(f, content)
        }
        index.indexVault(VaultConfig(id = id, path = root.toString(), name = id))
    }

    @BeforeEach
    fun setUp() {
        vault(
            "v1",
            mapOf(
                "Readme.md" to "root",
                "docs/Readme.md" to "docs",
                "docs/Guide.md" to "---\naliases: [Manual]\n---\nguide",
                "deep/a/b/Readme.md" to "deep",
                "other/Note.md" to "no readme here",
            )
        )
        vault("v2", mapOf("Guide.md" to "other vault"))
    }

    private fun resolvedPath(target: String, from: String? = null): String? {
        val ctx = from?.let { index.findByRelativePath("v1", it) }
        return (resolver.resolve(target, ctx) as? LinkResolution.Resolved)?.note?.relativePath
    }

    @Test
    fun `empty and unknown targets are unresolved`() {
        assertTrue(resolver.resolve("") is LinkResolution.Unresolved)
        assertTrue(resolver.resolve("  |alias") is LinkResolution.Unresolved)
        assertTrue(resolver.resolve("Nope") is LinkResolution.Unresolved)
    }

    @Test
    fun `heading and alias parts are ignored`() {
        assertEquals("docs/Guide.md", resolvedPath("Guide#Setup|the guide", from = "Readme.md"))
    }

    @Test
    fun `explicit path wins`() {
        assertEquals("docs/Readme.md", resolvedPath("docs/Readme", from = "Readme.md"))
        assertEquals("deep/a/b/Readme.md", resolvedPath("deep/a/b/Readme.md", from = "Readme.md"))
    }

    @Test
    fun `ambiguous names prefer the context folder, then the shallowest path`() {
        assertEquals("docs/Readme.md", resolvedPath("Readme", from = "docs/Guide.md"))
        assertEquals("Readme.md", resolvedPath("Readme", from = "other/Note.md"))
        assertEquals("Readme.md", resolvedPath("Readme"))
    }

    @Test
    fun `aliases resolve`() {
        assertEquals("docs/Guide.md", resolvedPath("manual", from = "Readme.md"))
    }

    @Test
    fun `same-vault candidates are preferred over other vaults`() {
        assertEquals("docs/Guide.md", resolvedPath("Guide", from = "Readme.md"))
        val ctx = index.findByRelativePath("v2", "Guide.md")
        val resolved = resolver.resolve("Guide", ctx) as LinkResolution.Resolved
        assertEquals("v2", resolved.note.vaultId)
    }
}
