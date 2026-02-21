package dev.htmeta.obsidianbridge.extensions

interface FrontmatterProcessor {
    fun process(frontmatter: Map<String, Any>, note: dev.htmeta.obsidianbridge.core.model.VaultNote)
    fun supportedFields(): List<String>
}
