package com.jakubjirak.obsidianbridge.ui.toolwindow

import com.intellij.openapi.project.Project
import com.intellij.openapi.wm.ToolWindow
import com.intellij.openapi.wm.ToolWindowFactory
import com.intellij.ui.content.ContentFactory
import javax.swing.*
import java.awt.BorderLayout

class BacklinksToolWindowFactory : ToolWindowFactory {
    override fun createToolWindowContent(project: Project, toolWindow: ToolWindow) {
        val panel = BacklinksPanel(project)
        val content = ContentFactory.getInstance().createContent(panel, "", false)
        toolWindow.contentManager.addContent(content)
    }
}

class BacklinksPanel(private val project: Project) : JPanel(BorderLayout()) {
    private val listModel = DefaultListModel<String>()
    private val list = JList(listModel)

    init {
        add(JScrollPane(list), BorderLayout.CENTER)
        add(JLabel("No backlinks found for this note"), BorderLayout.SOUTH)
    }

    fun updateBacklinks(backlinks: List<String>) {
        listModel.clear()
        backlinks.forEach { listModel.addElement(it) }
    }
}
