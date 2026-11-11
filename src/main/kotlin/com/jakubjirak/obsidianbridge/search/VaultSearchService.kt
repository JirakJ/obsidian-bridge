package com.jakubjirak.obsidianbridge.search

import com.intellij.openapi.components.Service
import com.intellij.openapi.project.Project
import com.jakubjirak.obsidianbridge.core.IndexService
import com.jakubjirak.obsidianbridge.core.model.VaultNote

data class SearchResult(
    val note: VaultNote,
    val matchSnippet: String,
    val score: Double
)

/**
 * Full-text vault search. Every query term must occur somewhere in the note (title, alias,
 * tag, path or body); matches in the title weigh most, body matches least. Pure logic.
 */
object VaultSearch {
    const val TITLE_WEIGHT = 10.0
    const val ALIAS_WEIGHT = 7.0
    const val TAG_WEIGHT = 5.0
    const val PATH_WEIGHT = 2.0
    const val BODY_WEIGHT = 1.0
    private const val SNIPPET_RADIUS = 40

    fun search(notes: Collection<VaultNote>, query: String, maxResults: Int = 20): List<SearchResult> {
        val terms = query.lowercase().split(Regex("\\s+")).filter { it.isNotEmpty() }
        if (terms.isEmpty()) return emptyList()
        return notes.mapNotNull { score(it, terms) }
            .sortedWith(compareByDescending<SearchResult> { it.score }.thenBy { it.note.relativePath })
            .take(maxResults)
    }

    /** Score of [note] for [terms], or `null` when some term matches nowhere. */
    fun score(note: VaultNote, terms: List<String>): SearchResult? {
        var total = 0.0
        val body = note.body.lowercase()
        for (term in terms) {
            var termScore = 0.0
            if (note.title.lowercase().contains(term)) termScore += TITLE_WEIGHT
            if (note.aliases.any { it.lowercase().contains(term) }) termScore += ALIAS_WEIGHT
            if (note.tags.any { it.lowercase().contains(term) }) termScore += TAG_WEIGHT
            if (note.relativePath.lowercase().contains(term)) termScore += PATH_WEIGHT
            if (body.contains(term)) termScore += BODY_WEIGHT
            if (termScore == 0.0) return null
            total += termScore
        }
        return SearchResult(note, snippet(note, terms.first()), total)
    }

    /** A single-line excerpt around the first body match, or the path when the body does not match. */
    fun snippet(note: VaultNote, term: String): String {
        val idx = note.body.lowercase().indexOf(term)
        if (idx < 0) return note.relativePath
        val start = (idx - SNIPPET_RADIUS).coerceAtLeast(0)
        val end = (idx + term.length + SNIPPET_RADIUS).coerceAtMost(note.body.length)
        val prefix = if (start > 0) "…" else ""
        val suffix = if (end < note.body.length) "…" else ""
        return prefix + note.body.substring(start, end).replace(Regex("\\s+"), " ").trim() + suffix
    }
}

@Service(Service.Level.PROJECT)
class VaultSearchService(private val project: Project) {

    fun search(query: String, maxResults: Int = 20): List<SearchResult> =
        VaultSearch.search(IndexService.getInstance(project).getAllNotes(), query, maxResults)

    companion object {
        fun getInstance(project: Project): VaultSearchService =
            project.getService(VaultSearchService::class.java)
    }
}
