package com.jakubjirak.obsidianbridge.core.model

/**
 * Configuration for a single Obsidian vault.
 * All properties are `var` with defaults so IntelliJ's XML serializer
 * can read/write them via reflection.
 */
data class VaultConfig(
    var id: String = java.util.UUID.randomUUID().toString(),
    var path: String = "",
    var name: String = "",
    var excludePatterns: List<String> = listOf(".obsidian/**", ".trash/**"),
    var dailyNoteFolder: String? = "daily",
    var dailyNoteFormat: String = "yyyy-MM-dd",
    var isActive: Boolean = true
) {
    /** No-arg constructor required by IntelliJ XML deserializer. */
    @Suppress("unused")
    constructor() : this(path = "", name = "")
}
