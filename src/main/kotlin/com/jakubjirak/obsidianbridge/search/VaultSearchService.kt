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

@Service(Service.Level.PROJECT)
class VaultSearchService(private val project: Project) {

    fun search(query: String, maxResults: Int = 20): List<SearchResult> {
        if (query.isBlank()) return emptyList()

        val indexService = IndexService.getInstance(project)
        val terms = query.lowercase().split(Regex("\\s+"))

        return indexService.getAllNotes()
            .mapNotNull { note -> scoreNote(note, terms) }
            .sortedByDescending { it.score }
            .take(maxResults)
    }

    private fun scoreNote(note: VaultNote, terms: List<String>): SearchResult? {
        var score = 0.0

        for (term in terms) {
            if (note.title.lowercase().contains(term)) score += 10.0
            if (note.tags.any { it.lowercase().contains(term) }) score += 5.0
            if (note.aliases.any { it.lowercase().contains(term) }) score += 7.0
            if (note.relativePath.lowercase().contains(term)) score += 2.0
        }

        if (score <= 0) return null
        return SearchResult(note, note.relativePath, score)
    }

    companion object {
        fun getInstance(project: Project): VaultSearchService =
            project.getService(VaultSearchService::class.java)
    }
}
