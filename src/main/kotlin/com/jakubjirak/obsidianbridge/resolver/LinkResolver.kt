package com.jakubjirak.obsidianbridge.resolver

import com.jakubjirak.obsidianbridge.core.IndexService
import com.jakubjirak.obsidianbridge.core.model.LinkResolution
import com.jakubjirak.obsidianbridge.core.model.VaultNote

class LinkResolver(private val indexService: IndexService) {

    fun resolve(target: String, contextNote: VaultNote? = null): LinkResolution {
        val cleanTarget = target
            .substringBefore('|')
            .substringBefore('#')
            .trim()

        if (cleanTarget.isEmpty()) return LinkResolution.Unresolved

        // 1. Exact path match
        if ('/' in cleanTarget) {
            val withMd = cleanTarget.ensureMdExtension()
            contextNote?.let { ctx ->
                val note = indexService.findByRelativePath(ctx.vaultId, withMd)
                if (note != null) return LinkResolution.Resolved(note)
            }
        }

        // 2. Find all candidates by name (case-insensitive)
        val candidates = (indexService.findByName(cleanTarget) +
                indexService.findByAlias(cleanTarget)).distinctBy { it.id }

        return when {
            candidates.isEmpty() -> LinkResolution.Unresolved
            candidates.size == 1 -> LinkResolution.Resolved(candidates.single())
            else -> {
                // 3. Shortest path wins
                val shortest = candidates.minByOrNull { it.relativePath.count { c -> c == '/' } }!!
                // 4. Same directory as context note as tiebreaker
                val sameDir = contextNote?.let { ctx ->
                    candidates.find { it.directory == ctx.directory }
                }
                LinkResolution.Resolved(sameDir ?: shortest)
            }
        }
    }

    private fun String.ensureMdExtension(): String =
        if (endsWith(".md")) this else "$this.md"
}
