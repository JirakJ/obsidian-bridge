package com.jakubjirak.obsidianbridge.core

import com.intellij.openapi.components.Service
import com.intellij.openapi.project.Project
import com.intellij.openapi.vfs.VirtualFile
import com.jakubjirak.obsidianbridge.core.model.VaultConfig
import com.jakubjirak.obsidianbridge.ui.settings.ObsidianBridgeSettings
import java.nio.file.Files
import java.nio.file.Path

@Service(Service.Level.PROJECT)
class VaultManager {

    fun getActiveVaults(): List<VaultConfig> = ObsidianBridgeSettings.getInstance().state.vaults.filter { it.isActive }

    fun getVaultForFile(file: VirtualFile): VaultConfig? = vaultForPath(getActiveVaults(), file.path)

    /** The note for [file], if it lies in an active vault and is indexed. */
    fun noteForFile(file: VirtualFile, index: NoteIndex): com.jakubjirak.obsidianbridge.core.model.VaultNote? {
        val vault = getVaultForFile(file) ?: return null
        val rel = relativeToVault(vault, file.path) ?: return null
        return index.findByRelativePath(vault.id, rel)
    }

    companion object {
        fun getInstance(project: Project): VaultManager = project.getService(VaultManager::class.java)

        /** `true` if [path] is an Obsidian vault (contains a `.obsidian` folder). */
        fun isValidVault(path: String): Boolean = path.isNotBlank() && Files.isDirectory(Path.of(path, ".obsidian"))

        /** The vault containing [filePath]; the most specific (longest) vault path wins. */
        fun vaultForPath(vaults: List<VaultConfig>, filePath: String): VaultConfig? =
            vaults.filter { relativeToVault(it, filePath) != null }.maxByOrNull { it.path.length }

        /** [filePath] relative to the vault root (with `/` separators), or `null` when outside the vault. */
        fun relativeToVault(vault: VaultConfig, filePath: String): String? {
            val root = NoteIndex.normalize(vault.path).trimEnd('/')
            val file = NoteIndex.normalize(filePath)
            if (root.isEmpty() || !file.startsWith("$root/")) return null
            return file.removePrefix("$root/")
        }
    }
}
