package dev.htmeta.obsidianbridge.parser

import com.intellij.openapi.diagnostic.Logger

object FrontmatterParser {

    private val log = Logger.getInstance(FrontmatterParser::class.java)

    fun parse(content: String): Map<String, Any> {
        if (!content.startsWith("---")) return emptyMap()

        val endIndex = content.indexOf("\n---", 3)
        if (endIndex < 0 || endIndex < 4) return emptyMap()

        val yamlBlock = content.substring(4, endIndex).trim()
        if (yamlBlock.isEmpty()) return emptyMap()

        return runCatching {
            parseSimpleYaml(yamlBlock)
        }.getOrElse { e ->
            log.warn("Failed to parse frontmatter: ${e.message}")
            emptyMap()
        }
    }

    private fun parseSimpleYaml(yaml: String): Map<String, Any> {
        val result = mutableMapOf<String, Any>()
        var currentKey: String? = null
        val currentList = mutableListOf<String>()

        for (line in yaml.lines()) {
            when {
                line.matches(Regex("""^\s*-\s+(.+)""")) && currentKey != null -> {
                    val value = line.trim().removePrefix("- ").trim()
                    currentList.add(value)
                }
                line.contains(":") && !line.startsWith(" ") && !line.startsWith("\t") -> {
                    if (currentKey != null && currentList.isNotEmpty()) {
                        result[currentKey] = currentList.toList()
                        currentList.clear()
                    }
                    val parts = line.split(":", limit = 2)
                    currentKey = parts[0].trim()
                    val value = parts[1].trim()
                    if (value.isNotEmpty()) {
                        result[currentKey!!] = value
                        currentKey = null
                    }
                }
                else -> {
                    // continuation or empty
                }
            }
        }
        if (currentKey != null && currentList.isNotEmpty()) {
            result[currentKey] = currentList.toList()
        }

        // Normalize tags field
        val tagsRaw = result["tags"]
        if (tagsRaw is String) {
            result["tags"] = tagsRaw.split(",").map { it.trim().removePrefix("#") }.filter { it.isNotEmpty() }
        }
        if (tagsRaw is List<*>) {
            @Suppress("UNCHECKED_CAST")
            result["tags"] = (tagsRaw as List<String>).map { it.removePrefix("#").trim() }
        }

        return result
    }
}
