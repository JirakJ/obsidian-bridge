package com.jakubjirak.obsidianbridge.actions

import com.intellij.openapi.actionSystem.AnAction
import com.intellij.openapi.actionSystem.AnActionEvent
import com.intellij.openapi.diagnostic.Logger
import com.intellij.openapi.fileEditor.FileEditorManager
import com.intellij.openapi.ui.Messages
import com.intellij.openapi.vfs.LocalFileSystem
import com.jakubjirak.obsidianbridge.core.VaultManager
import java.nio.file.Files
import java.nio.file.Path
import java.time.LocalDate

class NewNoteAction : AnAction() {

    private val log = Logger.getInstance(NewNoteAction::class.java)

    override fun actionPerformed(e: AnActionEvent) {
        val project = e.project ?: return

        val vaults = VaultManager.getInstance(project).getActiveVaults()
        if (vaults.isEmpty()) {
            Messages.showWarningDialog(project, "No vaults configured.", "Obsidian IDE Bridge")
            return
        }

        val title = Messages.showInputDialog(project, "Note title:", "New Obsidian Note", null)
        if (title.isNullOrBlank()) return

        val vault = vaults.first()
        val filename = noteFileName(title)
        if (filename.isEmpty()) {
            Messages.showWarningDialog(project, "The title must contain at least one letter or digit.", "Obsidian IDE Bridge")
            return
        }
        val notePath = Path.of(vault.path, "$filename.md")

        runCatching {
            if (Files.exists(notePath)) {
                Messages.showWarningDialog(project, "Note already exists: $notePath", "Obsidian IDE Bridge")
                return
            }
            val dateStr = LocalDate.now().toString()
            Files.writeString(notePath, "---\ntitle: $title\ndate: $dateStr\ntags: []\n---\n\n# $title\n\n")
            log.info("Created note: $notePath")
            LocalFileSystem.getInstance().refreshAndFindFileByNioFile(notePath)
                ?.let { FileEditorManager.getInstance(project).openFile(it, true) }
        }.onFailure { ex ->
            log.warn("Failed to create note: ${ex.message}")
            Messages.showErrorDialog(project, "Failed to create note: ${ex.message}", "Obsidian IDE Bridge")
        }
    }
}

/** File name (without `.md`) for a note titled [title]: letters/digits (any script), `_`, `-`; spaces become `-`. */
fun noteFileName(title: String): String =
    title.trim().replace(Regex("[^\\p{L}\\p{N}_ -]"), "").trim().replace(Regex("\\s+"), "-").lowercase()
