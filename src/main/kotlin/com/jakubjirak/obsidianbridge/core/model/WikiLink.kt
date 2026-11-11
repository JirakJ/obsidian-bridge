package com.jakubjirak.obsidianbridge.core.model

data class WikiLink(
    val raw: String,
    val target: String,
    val alias: String? = null,
    val heading: String? = null,
    val blockRef: String? = null,
    val isEmbed: Boolean = false,
    val sourceNoteId: String = "",
    val sourceLineNumber: Int = 0
)
