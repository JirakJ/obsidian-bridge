package com.jakubjirak.obsidianbridge.resolver

import com.jakubjirak.obsidianbridge.core.NoteIndex
import com.jakubjirak.obsidianbridge.core.model.LinkResolution
import com.jakubjirak.obsidianbridge.core.model.VaultNote

/**
 * Resolves a `[[target]]` the way Obsidian does: explicit vault paths first, then note
 * name / alias. Candidates in the context note's vault are preferred; among several,
 * the one in the context note's folder wins, otherwise the shallowest path.
 */
class LinkResolver(private val index: NoteIndex) {

    fun resolve(target: String, contextNote: VaultNote? = null): LinkResolution {
        val cleanTarget = target.substringBefore('|').substringBefore('#').trim()
        if (cleanTarget.isEmpty()) return LinkResolution.Unresolved

        if ('/' in cleanTarget && contextNote != null) {
            index.findByRelativePath(contextNote.vaultId, cleanTarget.ensureMdExtension())
                ?.let { return LinkResolution.Resolved(it) }
        }

        val all = (index.findByName(cleanTarget.substringAfterLast('/')) + index.findByAlias(cleanTarget)).distinctBy { it.id }
        val candidates = contextNote?.let { ctx -> all.filter { it.vaultId == ctx.vaultId }.ifEmpty { all } } ?: all

        return when {
            candidates.isEmpty() -> LinkResolution.Unresolved
            candidates.size == 1 -> LinkResolution.Resolved(candidates.single())
            else -> {
                val sameDir = contextNote?.let { ctx -> candidates.find { it.directory == ctx.directory } }
                LinkResolution.Resolved(sameDir ?: candidates.minBy { it.relativePath.count { c -> c == '/' } })
            }
        }
    }

    private fun String.ensureMdExtension(): String = if (endsWith(".md")) this else "$this.md"
}
