package dev.htmeta.obsidianbridge.actions

import com.intellij.openapi.actionSystem.AnAction
import com.intellij.openapi.actionSystem.AnActionEvent
import com.intellij.openapi.ui.Messages
import dev.htmeta.obsidianbridge.search.VaultSearchService

class SearchAction : AnAction() {
    override fun actionPerformed(e: AnActionEvent) {
        val project = e.project ?: return
        val query = Messages.showInputDialog(project, "Search across vault notes:", "Obsidian Bridge — Search Vault", null)
        if (query.isNullOrBlank()) return

        val results = VaultSearchService.getInstance(project).search(query)
        if (results.isEmpty()) {
            Messages.showInfoMessage(project, "No results found for: $query", "Obsidian Bridge")
        } else {
            val message = results.joinToString("\n") { "• ${it.note.title} (${it.note.relativePath})" }
            Messages.showInfoMessage(project, message, "Obsidian Bridge — ${results.size} Results")
        }
    }
}
