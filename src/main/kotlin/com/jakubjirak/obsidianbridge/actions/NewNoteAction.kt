package com.jakubjirak.obsidianbridge.actions

import com.intellij.openapi.actionSystem.AnAction
import com.intellij.openapi.actionSystem.AnActionEvent
import com.intellij.openapi.diagnostic.Logger
import com.intellij.openapi.ui.Messages
import com.jakubjirak.obsidianbridge.core.VaultManager
import com.jakubjirak.obsidianbridge.license.Feature
import com.jakubjirak.obsidianbridge.license.LicenseService
import java.nio.file.Files
import java.nio.file.Path
import java.time.LocalDate

class NewNoteAction : AnAction() {

    private val log = Logger.getInstance(NewNoteAction::class.java)

    override fun actionPerformed(e: AnActionEvent) {
        val project = e.project ?: return

        val licenseService = LicenseService.getInstance()
        if (!licenseService.isFeatureAvailable(Feature.TEMPLATES)) {
            Messages.showInfoMessage(project, "Templates is a Pro feature. Upgrade to Pro to unlock.", "Obsidian IDE Bridge — Pro Feature")
            return
        }

        val vaults = VaultManager.getInstance(project).getActiveVaults()
        if (vaults.isEmpty()) {
            Messages.showWarningDialog(project, "No vaults configured.", "Obsidian IDE Bridge")
            return
        }

        val title = Messages.showInputDialog(project, "Note title:", "New Obsidian Note", null)
        if (title.isNullOrBlank()) return

        val vault = vaults.first()
        val filename = title.replace(Regex("[^a-zA-Z0-9_ -]"), "").replace(" ", "-").lowercase()
        val notePath = Path.of(vault.path, "$filename.md")

        runCatching {
            if (Files.exists(notePath)) {
                Messages.showWarningDialog(project, "Note already exists: $notePath", "Obsidian IDE Bridge")
                return
            }
            val dateStr = LocalDate.now().toString()
            Files.writeString(notePath, "---\ntitle: $title\ndate: $dateStr\ntags: []\n---\n\n# $title\n\n")
            log.info("Created note: $notePath")
            Messages.showInfoMessage(project, "Note created: $notePath", "Obsidian IDE Bridge")
        }.onFailure { ex ->
            log.warn("Failed to create note: ${ex.message}")
            Messages.showErrorDialog(project, "Failed to create note: ${ex.message}", "Obsidian IDE Bridge")
        }
    }
}
