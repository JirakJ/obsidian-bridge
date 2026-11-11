package com.jakubjirak.obsidianbridge.parser

import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.Test

class FrontmatterParserTest {

    @Test
    fun `parses basic frontmatter`() {
        val content = "---\ntitle: My Note\ndate: 2026-01-01\n---\n\n# Content"
        val fm = FrontmatterParser.parse(content)
        assertEquals("My Note", fm["title"])
        assertEquals("2026-01-01", fm["date"])
    }

    @Test
    fun `parses list tags`() {
        val content = "---\ntags:\n  - tag1\n  - tag2\n  - tag3\n---\n\nContent"
        val fm = FrontmatterParser.parse(content)
        @Suppress("UNCHECKED_CAST")
        val tags = fm["tags"] as List<String>
        assertEquals(listOf("tag1", "tag2", "tag3"), tags)
    }

    @Test
    fun `parses aliases`() {
        val content = "---\naliases:\n  - alias1\n  - alias2\n---\n\nContent"
        val fm = FrontmatterParser.parse(content)
        @Suppress("UNCHECKED_CAST")
        val aliases = fm["aliases"] as List<String>
        assertEquals(2, aliases.size)
    }

    @Test
    fun `returns empty for no frontmatter`() {
        val content = "# Just a heading\n\nSome content"
        val fm = FrontmatterParser.parse(content)
        assertTrue(fm.isEmpty())
    }

    @Test
    fun `returns empty for malformed frontmatter`() {
        val content = "---\nthis is not yaml {{{}}\n---\n"
        val fm = FrontmatterParser.parse(content)
        // Should not crash, returns whatever it can parse
        assertNotNull(fm)
    }

    @Test
    fun `handles empty frontmatter block`() {
        val content = "---\n---\n\n# Content"
        val fm = FrontmatterParser.parse(content)
        assertTrue(fm.isEmpty())
    }

    @Test
    fun `handles content without closing delimiter`() {
        val content = "---\ntitle: Unclosed\n\nContent without closing"
        val fm = FrontmatterParser.parse(content)
        assertTrue(fm.isEmpty())
    }

    @Test
    fun `parses flow lists and quoted values`() {
        val fm = FrontmatterParser.parse("---\ntitle: \"Quoted: title\"\naliases: [One, 'Two']\ntags: [a, \"#b\"]\n---\nbody")
        assertEquals("Quoted: title", fm["title"])
        assertEquals(listOf("One", "Two"), fm["aliases"])
        assertEquals(listOf("a", "b"), fm["tags"])
    }

    @Test
    fun `string tags split on commas and spaces`() {
        assertEquals(listOf("a", "b", "c"), FrontmatterParser.parse("---\ntags: a, #b c\n---\n")["tags"])
    }

    @Test
    fun `windows line endings are supported`() {
        val fm = FrontmatterParser.parse("---\r\ntitle: Win\r\ntags:\r\n  - x\r\n---\r\nbody")
        assertEquals("Win", fm["title"])
        assertEquals(listOf("x"), fm["tags"])
    }

    @Test
    fun `body strips the frontmatter block`() {
        assertEquals("# Heading\ntext", FrontmatterParser.body("---\ntitle: x\n---\n# Heading\ntext"))
        assertEquals("no frontmatter", FrontmatterParser.body("no frontmatter"))
        assertEquals("---\nunclosed", FrontmatterParser.body("---\nunclosed"))
    }
}
