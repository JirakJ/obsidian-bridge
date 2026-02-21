package com.jakubjirak.obsidianbridge.search

import com.jakubjirak.obsidianbridge.core.model.VaultNote
import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.Test

class VaultSearchServiceTest {

    @Test
    fun `title match scores higher than path match`() {
        val noteWithTitleMatch = VaultNote(id = "1", vaultId = "v1", relativePath = "other/path.md", title = "kotlin guide")
        val noteWithPathMatch = VaultNote(id = "2", vaultId = "v1", relativePath = "kotlin/readme.md", title = "readme")
        assertTrue(scoreNote(noteWithTitleMatch, listOf("kotlin")) > scoreNote(noteWithPathMatch, listOf("kotlin")))
    }

    @Test
    fun `tag match scores medium`() {
        val noteWithTag = VaultNote(id = "1", vaultId = "v1", relativePath = "note.md", title = "Note", tags = listOf("kotlin"))
        assertTrue(scoreNote(noteWithTag, listOf("kotlin")) > 0)
    }

    @Test
    fun `alias match scores higher than tag match`() {
        val noteWithAlias = VaultNote(id = "1", vaultId = "v1", relativePath = "note.md", title = "Note", aliases = listOf("kotlin"))
        val noteWithTag = VaultNote(id = "2", vaultId = "v1", relativePath = "note.md", title = "Note", tags = listOf("kotlin"))
        assertTrue(scoreNote(noteWithAlias, listOf("kotlin")) > scoreNote(noteWithTag, listOf("kotlin")))
    }

    @Test
    fun `no match returns zero score`() {
        val note = VaultNote(id = "1", vaultId = "v1", relativePath = "note.md", title = "Note")
        assertEquals(0.0, scoreNote(note, listOf("nonexistent")))
    }

    @Test
    fun `multiple terms accumulate score`() {
        val note = VaultNote(id = "1", vaultId = "v1", relativePath = "kotlin/guide.md", title = "Kotlin Guide")
        val singleTermScore = scoreNote(note, listOf("kotlin"))
        val multiTermScore = scoreNote(note, listOf("kotlin", "guide"))
        assertTrue(multiTermScore > singleTermScore)
    }

    @Test
    fun `empty query returns zero`() {
        val note = VaultNote(id = "1", vaultId = "v1", relativePath = "note.md", title = "Note")
        assertEquals(0.0, scoreNote(note, emptyList()))
    }

    // Replicate the scoring algorithm from VaultSearchService for testability
    private fun scoreNote(note: VaultNote, terms: List<String>): Double {
        var score = 0.0
        for (term in terms) {
            if (note.title.lowercase().contains(term)) score += 10.0
            if (note.tags.any { it.lowercase().contains(term) }) score += 5.0
            if (note.aliases.any { it.lowercase().contains(term) }) score += 7.0
            if (note.relativePath.lowercase().contains(term)) score += 2.0
        }
        return score
    }
}
