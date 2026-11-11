package com.jakubjirak.obsidianbridge.search

import com.jakubjirak.obsidianbridge.core.model.VaultNote
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test

class VaultSearchTest {

    @Suppress("LongParameterList") // test fixture builder: every parameter but id/title has a default
    private fun note(
        id: String,
        title: String,
        path: String = "$id.md",
        tags: List<String> = emptyList(),
        aliases: List<String> = emptyList(),
        body: String = "",
    ) = VaultNote(id = id, vaultId = "v", relativePath = path, title = title, tags = tags, aliases = aliases, body = body)

    @Test
    fun `field weights order title, alias, tag, path, body`() {
        val term = listOf("kotlin")
        val title = VaultSearch.score(note("1", "Kotlin"), term)!!.score
        val alias = VaultSearch.score(note("2", "x", aliases = listOf("kotlin")), term)!!.score
        val tag = VaultSearch.score(note("3", "x", tags = listOf("kotlin")), term)!!.score
        val path = VaultSearch.score(note("4", "x", path = "kotlin/x.md"), term)!!.score
        val body = VaultSearch.score(note("5", "x", body = "I like Kotlin"), term)!!.score
        assertTrue(title > alias && alias > tag && tag > path && path > body)
    }

    @Test
    fun `every term must match somewhere`() {
        val n = note("1", "Kotlin guide", body = "coroutines")
        assertTrue(VaultSearch.score(n, listOf("kotlin", "coroutines")) != null)
        assertNull(VaultSearch.score(n, listOf("kotlin", "java")))
    }

    @Test
    fun `search ranks, limits and ignores blank queries`() {
        val notes = listOf(
            note("a", "Other", body = "mentions kotlin once"),
            note("b", "Kotlin"),
            note("c", "Unrelated"),
        )
        assertEquals(listOf("b", "a"), VaultSearch.search(notes, "KOTLIN").map { it.note.id })
        assertEquals(1, VaultSearch.search(notes, "kotlin", maxResults = 1).size)
        assertTrue(VaultSearch.search(notes, "   ").isEmpty())
    }

    @Test
    fun `snippet shows body context or falls back to path`() {
        val long = "a".repeat(100) + " needle here " + "b".repeat(100)
        val snippet = VaultSearch.snippet(note("1", "t", body = long), "needle")
        assertTrue(snippet.startsWith("…") && snippet.endsWith("…") && snippet.contains("needle here"))
        assertEquals("p/x.md", VaultSearch.snippet(note("1", "t", path = "p/x.md"), "needle"))
        assertEquals("short needle", VaultSearch.snippet(note("1", "t", body = "short\nneedle"), "needle"))
    }
}
