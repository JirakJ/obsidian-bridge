package com.jakubjirak.obsidianbridge.core

import com.jakubjirak.obsidianbridge.core.model.VaultConfig
import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.io.TempDir
import java.nio.file.Files
import java.nio.file.Path

class IndexServiceTest {
    @TempDir
    lateinit var tempDir: Path

    @Test
    fun `VaultConfig default exclude patterns include obsidian dir`() {
        val config = VaultConfig(path = "/tmp/vault", name = "Test")
        assertTrue(config.excludePatterns.contains(".obsidian/**"))
        assertTrue(config.excludePatterns.contains(".trash/**"))
    }

    @Test
    fun `markdown files are found in vault directory`() {
        val vaultDir = tempDir.resolve("vault")
        Files.createDirectories(vaultDir.resolve(".obsidian"))
        Files.writeString(vaultDir.resolve("note1.md"), "# Note 1\n\nContent")
        Files.writeString(vaultDir.resolve("note2.md"), "# Note 2\n\nContent")
        Files.writeString(vaultDir.resolve("readme.txt"), "Not a note")

        val mdFiles = Files.walk(vaultDir).use { stream ->
            stream.filter { Files.isRegularFile(it) && it.toString().endsWith(".md") }.toList()
        }
        assertEquals(2, mdFiles.size)
    }

    @Test
    fun `exclude patterns filter out obsidian directory`() {
        val vaultDir = tempDir.resolve("vault")
        Files.createDirectories(vaultDir.resolve(".obsidian"))
        Files.writeString(vaultDir.resolve(".obsidian/workspace.json"), "{}")
        Files.writeString(vaultDir.resolve("note.md"), "# Note")

        val config = VaultConfig(path = vaultDir.toString(), name = "Test")
        val mdFiles = Files.walk(vaultDir).use { stream ->
            stream.filter { Files.isRegularFile(it) && it.toString().endsWith(".md") }
                .filter { path ->
                    config.excludePatterns.none { pattern ->
                        val rel = vaultDir.relativize(path).toString()
                        rel.startsWith(pattern.removeSuffix("/**"))
                    }
                }
                .toList()
        }
        assertEquals(1, mdFiles.size)
        assertTrue(mdFiles[0].toString().endsWith("note.md"))
    }

    @Test
    fun `frontmatter is parsed from indexed file`() {
        val content = "---\ntitle: My Note\ntags:\n  - test\n---\n\n# Content\n\n[[other-note]]"
        val fm = com.jakubjirak.obsidianbridge.parser.FrontmatterParser.parse(content)
        assertEquals("My Note", fm["title"])
    }

    @Test
    fun `wiki links are extracted from indexed content`() {
        val content = "Some text\n\n[[Note A]] and [[Note B|alias]]"
        val links = com.jakubjirak.obsidianbridge.parser.WikiLinkParser.parse(content, "source-1")
        assertEquals(2, links.size)
        assertEquals("Note A", links[0].target)
        assertEquals("Note B", links[1].target)
        assertEquals("alias", links[1].alias)
    }

    @Test
    fun `tags are extracted from indexed content`() {
        val content = "---\ntags:\n  - project\n---\n\nSome #inline-tag content"
        val fm = com.jakubjirak.obsidianbridge.parser.FrontmatterParser.parse(content)
        val tags = com.jakubjirak.obsidianbridge.parser.TagExtractor.extract(content, fm)
        assertTrue(tags.contains("project"))
        assertTrue(tags.contains("inline-tag"))
    }

    @Test
    fun `SHA-256 hash is deterministic`() {
        val digest = java.security.MessageDigest.getInstance("SHA-256")
        val hash1 = digest.digest("test".toByteArray()).joinToString("") { "%02x".format(it) }
        val digest2 = java.security.MessageDigest.getInstance("SHA-256")
        val hash2 = digest2.digest("test".toByteArray()).joinToString("") { "%02x".format(it) }
        assertEquals(hash1, hash2)
    }

    @Test
    fun `different content produces different hash`() {
        val digest1 = java.security.MessageDigest.getInstance("SHA-256")
        val hash1 = digest1.digest("content-a".toByteArray()).joinToString("") { "%02x".format(it) }
        val digest2 = java.security.MessageDigest.getInstance("SHA-256")
        val hash2 = digest2.digest("content-b".toByteArray()).joinToString("") { "%02x".format(it) }
        assertNotEquals(hash1, hash2)
    }
}
