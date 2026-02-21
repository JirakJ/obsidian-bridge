package dev.htmeta.obsidianbridge.core

import com.intellij.openapi.components.Service
import com.intellij.openapi.diagnostic.Logger
import com.intellij.openapi.project.Project
import com.intellij.openapi.vfs.VirtualFile
import com.intellij.openapi.vfs.VirtualFileManager
import dev.htmeta.obsidianbridge.core.model.VaultConfig
import dev.htmeta.obsidianbridge.ui.settings.ObsidianBridgeSettings

@Service(Service.Level.PROJECT)
class VaultManager(private val project: Project) {

    private val log = Logger.getInstance(VaultManager::class.java)

    fun getActiveVaults(): List<VaultConfig> {
        return ObsidianBridgeSettings.getInstance().state.vaults.filter { it.isActive }
    }

    fun isValidVault(path: String): Boolean {
        val vaultDir = VirtualFileManager.getInstance().findFileByUrl("file://$path")
        return vaultDir?.findChild(".obsidian")?.isDirectory == true
    }

    fun addVault(config: VaultConfig): Boolean {
        if (!isValidVault(config.path)) {
            log.warn("Not a valid Obsidian vault: ${config.path}")
            return false
        }
        val settings = ObsidianBridgeSettings.getInstance()
        val state = settings.state.copy(vaults = settings.state.vaults + config)
        settings.loadState(state)
        log.info("Vault registered: ${config.name} at ${config.path}")
        return true
    }

    fun removeVault(vaultId: String) {
        val settings = ObsidianBridgeSettings.getInstance()
        val state = settings.state.copy(vaults = settings.state.vaults.filter { it.id != vaultId })
        settings.loadState(state)
    }

    fun getVaultForFile(file: VirtualFile): VaultConfig? {
        return getActiveVaults().find { file.path.startsWith(it.path) }
    }

    companion object {
        fun getInstance(project: Project): VaultManager =
            project.getService(VaultManager::class.java)
    }
}
