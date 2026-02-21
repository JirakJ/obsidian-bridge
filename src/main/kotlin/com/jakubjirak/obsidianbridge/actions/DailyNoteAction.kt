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
import java.time.format.DateTimeFormatter

class DailyNoteAction : AnAction() {

    private val log = Logger.getInstance(DailyNoteAction::class.java)

    override fun actionPerformed(e: AnActionEvent) {
        val project = e.project ?: return

        val licenseService = LicenseService.getInstance()
        if (!licenseService.isFeatureAvailable(Feature.DAILY_NOTES)) {
            Messages.showInfoMessage(project, "Daily Notes is a Pro feature. Upgrade to Pro to unlock.", "Obsidian IDE Bridge — Pro Feature")
            return
        }

        val vaults = VaultManager.getInstance(project).getActiveVaults()
        if (vaults.isEmpty()) {
            Messages.showWarningDialog(project, "No vaults configured. Go to Settings → Tools → Obsidian IDE Bridge.", "Obsidian IDE Bridge")
            return
        }

        val vault = vaults.first()
        val dateStr = LocalDate.now().format(DateTimeFormatter.ofPattern(vault.dailyNoteFormat))
        val folder = vault.dailyNoteFolder ?: "daily"
        val notePath = Path.of(vault.path, folder, "$dateStr.md")

        runCatching {
            if (!Files.exists(notePath)) {
                Files.createDirectories(notePath.parent)
                Files.writeString(notePath, "---\ndate: $dateStr\ntags:\n  - daily\n---\n\n# $dateStr\n\n")
                log.info("Created daily note: $notePath")
            }
            // File open handled by IDE VFS refresh
            Messages.showInfoMessage(project, "Daily note: $notePath", "Obsidian IDE Bridge")
        }.onFailure { ex ->
            log.warn("Failed to create daily note: ${ex.message}")
            Messages.showErrorDialog(project, "Failed to create daily note: ${ex.message}", "Obsidian IDE Bridge")
        }
    }
}
