package dev.htmeta.obsidianbridge.ui.toolwindow

import com.intellij.openapi.project.Project
import com.intellij.openapi.wm.ToolWindow
import com.intellij.openapi.wm.ToolWindowFactory
import com.intellij.ui.content.ContentFactory
import javax.swing.*
import java.awt.BorderLayout

class GraphToolWindowFactory : ToolWindowFactory {
    override fun createToolWindowContent(project: Project, toolWindow: ToolWindow) {
        val panel = GraphPanel(project)
        val content = ContentFactory.getInstance().createContent(panel, "", false)
        toolWindow.contentManager.addContent(content)
    }
}

class GraphPanel(private val project: Project) : JPanel(BorderLayout()) {
    init {
        add(JLabel("Graph view — requires JCEF. Connect a vault to see the graph.", SwingConstants.CENTER), BorderLayout.CENTER)
    }
}
