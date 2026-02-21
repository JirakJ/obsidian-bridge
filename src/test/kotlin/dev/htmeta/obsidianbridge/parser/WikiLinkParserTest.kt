package dev.htmeta.obsidianbridge.parser

import dev.htmeta.obsidianbridge.core.model.WikiLink
import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.Test

class WikiLinkParserTest {

    @Test
    fun `parses basic wiki-link`() {
        val links = WikiLinkParser.parse("See [[Note A]] for details")
        assertEquals(1, links.size)
        assertEquals("Note A", links[0].target)
        assertNull(links[0].alias)
        assertFalse(links[0].isEmbed)
    }

    @Test
    fun `parses aliased wiki-link`() {
        val links = WikiLinkParser.parse("See [[Note A|my alias]] here")
        assertEquals(1, links.size)
        assertEquals("Note A", links[0].target)
        assertEquals("my alias", links[0].alias)
    }

    @Test
    fun `parses heading link`() {
        val links = WikiLinkParser.parse("[[Note A#Section 1]]")
        assertEquals(1, links.size)
        assertEquals("Note A", links[0].target)
        assertEquals("Section 1", links[0].heading)
    }

    @Test
    fun `parses block reference`() {
        val links = WikiLinkParser.parse("[[Note A#^block-id]]")
        assertEquals(1, links.size)
        assertEquals("Note A", links[0].target)
        assertEquals("block-id", links[0].blockRef)
    }

    @Test
    fun `parses embed link`() {
        val links = WikiLinkParser.parse("![[image.png]]")
        assertEquals(1, links.size)
        assertEquals("image.png", links[0].target)
        assertTrue(links[0].isEmbed)
    }

    @Test
    fun `parses multiple links on same line`() {
        val links = WikiLinkParser.parse("Links to [[A]] and [[B]] and [[C]]")
        assertEquals(3, links.size)
        assertEquals(listOf("A", "B", "C"), links.map { it.target })
    }

    @Test
    fun `parses links across multiple lines`() {
        val content = """
            Line 1 [[First]]
            Line 2
            Line 3 [[Second]]
        """.trimIndent()
        val links = WikiLinkParser.parse(content)
        assertEquals(2, links.size)
        assertEquals(1, links[0].sourceLineNumber)
        assertEquals(3, links[1].sourceLineNumber)
    }

    @Test
    fun `returns empty for no links`() {
        val links = WikiLinkParser.parse("No links here")
        assertTrue(links.isEmpty())
    }

    @Test
    fun `handles empty content`() {
        val links = WikiLinkParser.parse("")
        assertTrue(links.isEmpty())
    }

    @Test
    fun `parses path-style link`() {
        val links = WikiLinkParser.parse("[[folder/subfolder/note]]")
        assertEquals(1, links.size)
        assertEquals("folder/subfolder/note", links[0].target)
    }
}
