package dev.htmeta.obsidianbridge.actions

import com.intellij.openapi.actionSystem.AnAction
import com.intellij.openapi.actionSystem.AnActionEvent
import com.intellij.openapi.diagnostic.Logger
import com.intellij.openapi.ui.Messages
import dev.htmeta.obsidianbridge.core.IndexService
import dev.htmeta.obsidianbridge.core.VaultManager

class ReindexAction : AnAction() {

    private val log = Logger.getInstance(ReindexAction::class.java)

    override fun actionPerformed(e: AnActionEvent) {
        val project = e.project ?: return
        val vaultManager = VaultManager.getInstance(project)
        val indexService = IndexService.getInstance(project)

        val vaults = vaultManager.getActiveVaults()
        if (vaults.isEmpty()) {
            Messages.showWarningDialog(project, "No vaults configured.", "Obsidian Bridge")
            return
        }

        indexService.clear()
        for (vault in vaults) {
            indexService.indexVault(vault)
        }
        val count = indexService.getAllNotes().size
        log.info("Re-indexed $count notes across ${vaults.size} vault(s)")
        Messages.showInfoMessage(project, "Re-indexed $count notes.", "Obsidian Bridge")
    }
}
