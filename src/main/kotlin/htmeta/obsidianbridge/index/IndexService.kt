package htmeta.obsidianbridge.index

import com.intellij.openapi.components.Service
import com.intellij.openapi.vfs.VirtualFile
import com.intellij.util.indexing.FileBasedIndex

/**
 * IndexService skeleton for VFS/FileBasedIndex-based implementation.
 * Real implementation will register a FileBasedIndexExtension and use VFS listeners
 * to maintain an inverted index of notes and wiki-links.
 */
@Service
class IndexService {
    /**
     * Request reindex of a vault root. This is a lightweight call that delegates to
     * the IntelliJ indexing subsystem (FileBasedIndex) in production.
     */
    fun requestReindex(vaultRoot: VirtualFile) {
        // Placeholder: use FileBasedIndex for production implementation.
        val fileBasedIndex = FileBasedIndex.getInstance()
        // fileBasedIndex.requestReindex( /* index id */ ) -- implemented in real code using a custom index extension
    }
}

