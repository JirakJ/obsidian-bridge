package com.jakubjirak.obsidianbridge.parser

/**
 * Minimal YAML frontmatter reader covering what Obsidian writes: `key: value`,
 * block lists (`key:` followed by `- item` lines), flow lists (`key: [a, b]`) and
 * quoted scalars. Nested mappings are ignored. `tags` are normalised to a list
 * without leading `#`.
 */
object FrontmatterParser {

    private val LIST_ITEM = Regex("""^\s*-\s+(.+)""")
    private const val QUOTES = "\"'"

    /** Parsed frontmatter, or an empty map when the content has none. */
    fun parse(content: String): Map<String, Any> {
        val block = frontmatterBlock(content) ?: return emptyMap()
        return parseSimpleYaml(block)
    }

    /** The text after the frontmatter block (or the whole content when there is none). */
    fun body(content: String): String {
        val normalized = content.replace("\r\n", "\n")
        if (!normalized.startsWith("---\n")) return content
        val end = normalized.indexOf("\n---", 3)
        if (end < 0) return content
        return normalized.substring(end + 4).removePrefix("\n")
    }

    private fun frontmatterBlock(content: String): String? {
        val normalized = content.replace("\r\n", "\n")
        if (!normalized.startsWith("---\n")) return null
        val end = normalized.indexOf("\n---", 3)
        if (end < 4) return null
        return normalized.substring(4, end).trim().takeIf { it.isNotEmpty() }
    }

    private fun parseSimpleYaml(yaml: String): Map<String, Any> {
        val result = linkedMapOf<String, Any>()
        var listKey: String? = null
        val currentList = mutableListOf<String>()

        fun flushList() {
            val key = listKey ?: return
            if (currentList.isNotEmpty()) result[key] = currentList.toList()
            currentList.clear()
            listKey = null
        }

        for (line in yaml.lines()) {
            val item = LIST_ITEM.find(line)
            when {
                item != null && listKey != null -> currentList += unquote(item.groupValues[1].trim())
                line.contains(':') && !line.startsWith(" ") && !line.startsWith("\t") -> {
                    flushList()
                    val key = line.substringBefore(':').trim()
                    val value = line.substringAfter(':').trim()
                    when {
                        value.isEmpty() -> listKey = key
                        value.startsWith("[") && value.endsWith("]") -> result[key] = flowList(value)
                        else -> result[key] = unquote(value)
                    }
                }
            }
        }
        flushList()
        normalizeTags(result)
        return result
    }

    private fun normalizeTags(result: MutableMap<String, Any>) {
        when (val tags = result["tags"]) {
            is String -> result["tags"] = tags.split(',', ' ').map { it.trim().removePrefix("#") }.filter { it.isNotEmpty() }
            is List<*> -> result["tags"] = tags.map { it.toString().trim().removePrefix("#") }.filter { it.isNotEmpty() }
        }
    }

    private fun flowList(value: String): List<String> =
        value.removePrefix("[").removeSuffix("]")
            .split(',')
            .map { unquote(it.trim()) }
            .filter { it.isNotEmpty() }

    private fun unquote(value: String): String =
        if (value.length >= 2 && value.first() in QUOTES && value.last() == value.first()) {
            value.substring(1, value.length - 1)
        } else {
            value
        }
}
