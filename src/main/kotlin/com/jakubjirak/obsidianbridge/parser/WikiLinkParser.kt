package com.jakubjirak.obsidianbridge.parser

import com.jakubjirak.obsidianbridge.core.model.WikiLink

object WikiLinkParser {

    private val WIKI_LINK_REGEX = Regex("""(!?)\[\[([^\]]+?)]]""")

    fun parse(content: String, sourceNoteId: String = ""): List<WikiLink> {
        val links = mutableListOf<WikiLink>()
        val lines = content.lines()

        for ((lineIndex, line) in lines.withIndex()) {
            for (match in WIKI_LINK_REGEX.findAll(line)) {
                val isEmbed = match.groupValues[1] == "!"
                val inner = match.groupValues[2]
                val aliasSplit = inner.split("|", limit = 2)
                val targetPart = aliasSplit[0]
                val alias = aliasSplit.getOrNull(1)

                val headingSplit = targetPart.split("#", limit = 2)
                val target = headingSplit[0].trim()
                val headingOrBlock = headingSplit.getOrNull(1)?.trim()

                val heading = headingOrBlock?.takeIf { !it.startsWith("^") }
                val blockRef = headingOrBlock?.removePrefix("^")?.takeIf { headingOrBlock.startsWith("^") }

                links.add(
                    WikiLink(
                        raw = match.value,
                        target = target,
                        alias = alias,
                        heading = heading,
                        blockRef = blockRef,
                        isEmbed = isEmbed,
                        sourceNoteId = sourceNoteId,
                        sourceLineNumber = lineIndex + 1
                    )
                )
            }
        }
        return links
    }
}
