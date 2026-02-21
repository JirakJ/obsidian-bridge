package com.jakubjirak.obsidianbridge.startup

import com.intellij.openapi.diagnostic.Logger
import com.intellij.openapi.project.Project
import com.intellij.openapi.startup.ProjectActivity
import com.jakubjirak.obsidianbridge.core.IndexService
import com.jakubjirak.obsidianbridge.core.VaultManager
import com.jakubjirak.obsidianbridge.notification.ObsidianBridgeNotifications

class ObsidianBridgeStartupActivity : ProjectActivity {
    private val log = Logger.getInstance(ObsidianBridgeStartupActivity::class.java)

    override suspend fun execute(project: Project) {
        val vaultManager = VaultManager.getInstance(project)
        val vaults = vaultManager.getActiveVaults()
        if (vaults.isEmpty()) {
            log.info("No active vaults configured — skipping startup indexing")
            return
        }
        log.info("Obsidian Bridge: starting index for ${vaults.size} vault(s)")
        val indexService = IndexService.getInstance(project)
        for (vault in vaults) {
            indexService.indexVault(vault)
        }
        val count = indexService.getAllNotes().size
        log.info("Obsidian Bridge: indexed $count notes on startup")
        ObsidianBridgeNotifications.notifyInfo(
            project,
            "Indexed $count notes across ${vaults.size} vault(s)"
        )
    }
}
