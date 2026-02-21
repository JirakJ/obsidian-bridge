package htmeta.obsidianbridge.index

import java.nio.file.Files
import java.nio.file.Path

/**
 * Simple filesystem-based indexer for Markdown wiki-links (temporary non-VFS implementation).
 */
class FileSystemIndexer {
    private val linkRegex = Regex("\\\\[\\\\[([^\\\\]|#]+)(?:[^\\\\]]*)\\\\]\\\\]")
    val index: MutableMap<Path, Set<String>> = mutableMapOf()

    fun indexVault(root: Path) {
        index.clear()
        Files.walk(root).use { stream ->
            stream.filter { Files.isRegularFile(it) && it.toString().endsWith(".md") }.forEach { path ->
                val content = Files.readString(path)
                val links = linkRegex.findAll(content).map { it.groupValues[1].trim() }.toSet()
                index[path] = links
            }
        }
    }
}
