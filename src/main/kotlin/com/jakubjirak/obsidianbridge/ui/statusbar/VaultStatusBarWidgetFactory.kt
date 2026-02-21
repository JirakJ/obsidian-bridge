package com.jakubjirak.obsidianbridge.ui.statusbar

import com.intellij.openapi.project.Project
import com.intellij.openapi.wm.StatusBar
import com.intellij.openapi.wm.StatusBarWidget
import com.intellij.openapi.wm.StatusBarWidgetFactory
import com.jakubjirak.obsidianbridge.core.IndexService
import com.jakubjirak.obsidianbridge.core.VaultManager

class VaultStatusBarWidgetFactory : StatusBarWidgetFactory {
    override fun getId(): String = "ObsidianBridge.StatusBar"
    override fun getDisplayName(): String = "Obsidian IDE Bridge"
    override fun isAvailable(project: Project): Boolean = true

    override fun createWidget(project: Project): StatusBarWidget = VaultStatusBarWidget(project)
}

class VaultStatusBarWidget(private val project: Project) : StatusBarWidget, StatusBarWidget.TextPresentation {
    private var statusBar: StatusBar? = null

    override fun ID(): String = "ObsidianBridge.StatusBar"

    override fun install(statusBar: StatusBar) {
        this.statusBar = statusBar
    }

    override fun getPresentation(): StatusBarWidget.WidgetPresentation = this

    override fun getText(): String {
        val vaults = VaultManager.getInstance(project).getActiveVaults()
        if (vaults.isEmpty()) return "Obsidian: No vault"
        val noteCount = IndexService.getInstance(project).getAllNotes().size
        return "Obsidian: ${vaults.size} vault(s), $noteCount notes"
    }

    override fun getTooltipText(): String = "Obsidian IDE Bridge vault status"

    override fun getAlignment(): Float = 0.5f

    override fun dispose() {
        statusBar = null
    }
}
