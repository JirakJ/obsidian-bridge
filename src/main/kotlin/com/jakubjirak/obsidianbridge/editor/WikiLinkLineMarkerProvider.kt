package com.jakubjirak.obsidianbridge.editor

import com.intellij.codeInsight.daemon.LineMarkerInfo
import com.intellij.codeInsight.daemon.LineMarkerProvider
import com.intellij.openapi.editor.markup.GutterIconRenderer
import com.intellij.openapi.fileEditor.FileEditorManager
import com.intellij.openapi.ui.Messages
import com.intellij.openapi.util.IconLoader
import com.intellij.openapi.vfs.LocalFileSystem
import com.intellij.psi.PsiComment
import com.intellij.psi.PsiElement
import com.jakubjirak.obsidianbridge.core.IndexService
import com.jakubjirak.obsidianbridge.core.VaultManager
import com.jakubjirak.obsidianbridge.parser.WikiLinkParser
import com.jakubjirak.obsidianbridge.ui.settings.ObsidianBridgeSettings
import java.nio.file.Path
import javax.swing.Icon

/**
 * Gutter icon on code comments that contain `[[wiki-links]]`; clicking it opens the
 * linked note from the vault.
 */
class WikiLinkLineMarkerProvider : LineMarkerProvider {
    private val icon: Icon = IconLoader.getIcon("/icons/obsidian.svg", WikiLinkLineMarkerProvider::class.java)

    override fun getLineMarkerInfo(element: PsiElement): LineMarkerInfo<*>? {
        if (element !is PsiComment) return null
        if (!ObsidianBridgeSettings.getInstance().state.showGutterIcons) return null
        val links = WikiLinkParser.parse(element.text)
        if (links.isEmpty()) return null
        val targets = links.joinToString(", ") { it.target }

        return LineMarkerInfo(
            element,
            element.textRange,
            icon,
            { "Open Obsidian note: $targets" },
            { _, elt -> openFirstLink(elt) },
            GutterIconRenderer.Alignment.LEFT
        ) { "Obsidian wiki-link" }
    }

    private fun openFirstLink(element: PsiElement) {
        val project = element.project
        val index = IndexService.getInstance(project)
        val link = WikiLinkParser.parse(element.text).firstOrNull() ?: return
        val note = index.resolve(link, null)
        val vault = note?.let { n -> VaultManager.getInstance(project).getActiveVaults().find { it.id == n.vaultId } }
        val file = if (note != null && vault != null) {
            LocalFileSystem.getInstance().refreshAndFindFileByNioFile(Path.of(vault.path, note.relativePath))
        } else {
            null
        }
        if (file == null) {
            Messages.showInfoMessage(project, "No indexed note found for [[${link.target}]].", "Obsidian IDE Bridge")
            return
        }
        FileEditorManager.getInstance(project).openFile(file, true)
    }
}
