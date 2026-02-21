package dev.htmeta.obsidianbridge.core.model

import java.time.Instant

data class VaultNote(
    val id: String,
    val vaultId: String,
    val relativePath: String,
    val title: String,
    val aliases: List<String> = emptyList(),
    val tags: List<String> = emptyList(),
    val frontmatter: Map<String, Any> = emptyMap(),
    val outgoingLinks: List<WikiLink> = emptyList(),
    val created: Instant = Instant.now(),
    val modified: Instant = Instant.now(),
    val contentHash: String = ""
) {
    val directory: String get() = relativePath.substringBeforeLast('/', "")
}
