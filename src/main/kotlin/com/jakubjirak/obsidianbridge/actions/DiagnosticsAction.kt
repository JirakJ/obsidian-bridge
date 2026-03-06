package com.jakubjirak.obsidianbridge.actions

import com.intellij.openapi.actionSystem.AnAction
import com.intellij.openapi.actionSystem.AnActionEvent
import com.intellij.openapi.ui.Messages
import com.jakubjirak.obsidianbridge.core.IndexService
import com.jakubjirak.obsidianbridge.core.VaultManager

class DiagnosticsAction : AnAction() {
    override fun actionPerformed(e: AnActionEvent) {
        val project = e.project ?: return
        val vaultManager = VaultManager.getInstance(project)
        val indexService = IndexService.getInstance(project)
        val vaults = vaultManager.getActiveVaults()
        val notes = indexService.getAllNotes()
        val totalTags = notes.flatMap { it.tags }.distinct().size
        val totalLinks = notes.sumOf { it.outgoingLinks.size }

        val sb = StringBuilder()
        sb.appendLine("Obsidian IDE Bridge Diagnostics")
        sb.appendLine("─".repeat(40))
        sb.appendLine("Active Vaults: ${vaults.size}")
        for (vault in vaults) {
            sb.appendLine("  • ${vault.name} (${vault.path})")
            val vaultNotes = notes.filter { it.vaultId == vault.id }
            sb.appendLine("    Notes: ${vaultNotes.size}")
        }
        sb.appendLine("Total Notes: ${notes.size}")
        sb.appendLine("Unique Tags: $totalTags")
        sb.appendLine("Total Wiki-Links: $totalLinks")
        sb.appendLine("Plugin Version: 2026.2.4")

        Messages.showInfoMessage(project, sb.toString(), "Obsidian IDE Bridge — Diagnostics")
    }
}
