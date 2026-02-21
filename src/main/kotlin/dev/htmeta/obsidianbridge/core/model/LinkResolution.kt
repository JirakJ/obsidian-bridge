package dev.htmeta.obsidianbridge.core.model

sealed class LinkResolution {
    data class Resolved(val note: VaultNote) : LinkResolution()
    data class Ambiguous(val candidates: List<VaultNote>) : LinkResolution()
    data object Unresolved : LinkResolution()
}
