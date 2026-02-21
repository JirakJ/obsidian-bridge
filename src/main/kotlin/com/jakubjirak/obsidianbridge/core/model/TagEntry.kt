package com.jakubjirak.obsidianbridge.core.model

data class TagEntry(
    val tag: String,
    val noteIds: MutableSet<String> = mutableSetOf(),
    val source: TagSource = TagSource.INLINE
)

enum class TagSource { FRONTMATTER, INLINE }
