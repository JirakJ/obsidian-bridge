package htmeta.obsidianbridge.vault

import java.nio.file.Path
import java.nio.file.Files

/**
 * Minimal VaultManager: validates vault paths and provides lifecycle hooks (placeholder).
 */
class VaultManager {
    fun isValidVault(path: Path): Boolean {
        return Files.isDirectory(path.resolve(".obsidian"))
    }
}
