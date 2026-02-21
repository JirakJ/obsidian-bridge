package com.jakubjirak.obsidianbridge.core.model

import java.time.Instant

data class BridgeEntry(
    val id: String = java.util.UUID.randomUUID().toString(),
    val codeFile: String,
    val lineNumber: Int,
    val markerType: String,
    val text: String,
    val linkedNoteId: String? = null,
    val created: Instant = Instant.now()
)
