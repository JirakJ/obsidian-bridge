package com.jakubjirak.obsidianbridge.core.model

data class VaultConfig(
    val id: String = java.util.UUID.randomUUID().toString(),
    val path: String,
    val name: String,
    val excludePatterns: List<String> = listOf(".obsidian/**", ".trash/**"),
    val templateFolder: String? = null,
    val dailyNoteFolder: String? = "daily",
    val dailyNoteFormat: String = "yyyy-MM-dd",
    val isActive: Boolean = true
)
