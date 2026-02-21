package dev.htmeta.obsidianbridge.parser

object TagExtractor {

    private val INLINE_TAG_REGEX = Regex("""(?:^|\s)#([a-zA-Z][a-zA-Z0-9_/.-]*)""")

    fun extract(content: String, frontmatter: Map<String, Any>): List<String> {
        val tags = mutableSetOf<String>()

        // Frontmatter tags
        @Suppress("UNCHECKED_CAST")
        val fmTags = frontmatter["tags"] as? List<String>
        fmTags?.forEach { tags.add(it.removePrefix("#")) }

        // Inline tags (skip frontmatter block and code blocks)
        val textAfterFrontmatter = if (content.startsWith("---")) {
            val endIndex = content.indexOf("\n---", 3)
            if (endIndex >= 0) content.substring(endIndex + 4) else content
        } else content

        // Simple code block removal
        val withoutCodeBlocks = textAfterFrontmatter
            .replace(Regex("""```[\s\S]*?```"""), "")
            .replace(Regex("""`[^`]+`"""), "")

        for (match in INLINE_TAG_REGEX.findAll(withoutCodeBlocks)) {
            tags.add(match.groupValues[1])
        }
        return tags.toList()
    }
}
