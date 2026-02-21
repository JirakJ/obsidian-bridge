package com.jakubjirak.obsidianbridge.parser

import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.Test

class TagExtractorTest {

    @Test
    fun `extracts inline tags`() {
        val tags = TagExtractor.extract("Some text #project #status/active", emptyMap())
        assertTrue(tags.contains("project"))
        assertTrue(tags.contains("status/active"))
    }

    @Test
    fun `extracts frontmatter tags`() {
        val fm = mapOf("tags" to listOf("fromfm", "another"))
        val tags = TagExtractor.extract("Content", fm)
        assertTrue(tags.contains("fromfm"))
        assertTrue(tags.contains("another"))
    }

    @Test
    fun `merges frontmatter and inline tags`() {
        val fm = mapOf("tags" to listOf("fm-tag"))
        val tags = TagExtractor.extract("Text #inline-tag", fm)
        assertTrue(tags.contains("fm-tag"))
        assertTrue(tags.contains("inline-tag"))
    }

    @Test
    fun `does not extract tags from code blocks`() {
        val content = "Normal #real-tag\n```\n#not-a-tag\n```\nMore text"
        val tags = TagExtractor.extract(content, emptyMap())
        assertTrue(tags.contains("real-tag"))
        assertFalse(tags.contains("not-a-tag"))
    }

    @Test
    fun `does not extract from inline code`() {
        val content = "Normal #real and `#not-a-tag` here"
        val tags = TagExtractor.extract(content, emptyMap())
        assertTrue(tags.contains("real"))
        assertFalse(tags.contains("not-a-tag"))
    }

    @Test
    fun `handles empty content`() {
        val tags = TagExtractor.extract("", emptyMap())
        assertTrue(tags.isEmpty())
    }

    @Test
    fun `removes hash prefix from frontmatter tags`() {
        val fm = mapOf("tags" to listOf("#with-hash"))
        val tags = TagExtractor.extract("", fm)
        assertTrue(tags.contains("with-hash"))
        assertFalse(tags.contains("#with-hash"))
    }
}
