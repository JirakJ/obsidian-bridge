package com.jakubjirak.obsidianbridge.actions

import com.intellij.openapi.actionSystem.AnAction
import com.intellij.openapi.actionSystem.AnActionEvent
import com.intellij.openapi.diagnostic.Logger
import com.intellij.openapi.progress.ProgressIndicator
import com.intellij.openapi.progress.ProgressManager
import com.intellij.openapi.progress.Task
import com.jakubjirak.obsidianbridge.core.IndexService
import com.jakubjirak.obsidianbridge.core.VaultManager
import com.jakubjirak.obsidianbridge.notification.ObsidianBridgeNotifications

class ReindexAction : AnAction() {
    private val log = Logger.getInstance(ReindexAction::class.java)

    override fun actionPerformed(e: AnActionEvent) {
        val project = e.project ?: return
        val vaultManager = VaultManager.getInstance(project)
        val indexService = IndexService.getInstance(project)
        val vaults = vaultManager.getActiveVaults()

        if (vaults.isEmpty()) {
            ObsidianBridgeNotifications.notifyWarning(project, "No vaults configured. Go to Settings → Tools → Obsidian IDE Bridge.")
            return
        }

        ProgressManager.getInstance().run(object : Task.Backgroundable(project, "Re-indexing Obsidian vaults", true) {
            override fun run(indicator: ProgressIndicator) {
                indicator.isIndeterminate = false
                indexService.clear()
                for ((i, vault) in vaults.withIndex()) {
                    indicator.text = "Indexing vault: ${vault.name}"
                    indicator.fraction = i.toDouble() / vaults.size
                    indexService.indexVault(vault)
                }
                indicator.fraction = 1.0
            }

            override fun onSuccess() {
                val count = indexService.getAllNotes().size
                log.info("Re-indexed $count notes across ${vaults.size} vault(s)")
                ObsidianBridgeNotifications.notifyInfo(project, "Re-indexed $count notes across ${vaults.size} vault(s)")
            }
        })
    }
}
