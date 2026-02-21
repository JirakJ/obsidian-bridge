package com.jakubjirak.obsidianbridge.extensions

interface FrontmatterProcessor {
    fun process(frontmatter: Map<String, Any>, note: com.jakubjirak.obsidianbridge.core.model.VaultNote)
    fun supportedFields(): List<String>
}
