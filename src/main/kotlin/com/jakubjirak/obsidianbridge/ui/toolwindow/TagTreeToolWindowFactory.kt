package com.jakubjirak.obsidianbridge.ui.toolwindow

import com.intellij.openapi.project.Project
import com.intellij.openapi.wm.ToolWindow
import com.intellij.openapi.wm.ToolWindowFactory
import com.intellij.ui.content.ContentFactory
import com.jakubjirak.obsidianbridge.license.Feature
import com.jakubjirak.obsidianbridge.license.LicenseService
import com.jakubjirak.obsidianbridge.ui.ProFeaturePanel
import javax.swing.*
import javax.swing.tree.DefaultMutableTreeNode
import javax.swing.tree.DefaultTreeModel
import java.awt.BorderLayout

class TagTreeToolWindowFactory : ToolWindowFactory {
    override fun createToolWindowContent(project: Project, toolWindow: ToolWindow) {
        val licenseService = LicenseService.getInstance()
        val panel = if (licenseService.isFeatureAvailable(Feature.TAG_TREE)) {
            TagTreePanel(project)
        } else {
            ProFeaturePanel("Tag Tree")
        }
        val content = ContentFactory.getInstance().createContent(panel, "", false)
        toolWindow.contentManager.addContent(content)
    }
}

class TagTreePanel(private val project: Project) : JPanel(BorderLayout()) {
    private val rootNode = DefaultMutableTreeNode("Tags")
    private val treeModel = DefaultTreeModel(rootNode)
    private val tree = JTree(treeModel)

    init {
        add(JScrollPane(tree), BorderLayout.CENTER)
    }

    fun updateTags(tags: Map<String, Int>) {
        rootNode.removeAllChildren()
        for ((tag, count) in tags.entries.sortedBy { it.key }) {
            val parts = tag.split("/")
            var parent = rootNode
            for (part in parts) {
                val existing = (0 until parent.childCount)
                    .map { parent.getChildAt(it) as DefaultMutableTreeNode }
                    .find { it.userObject.toString().startsWith(part) }
                parent = existing ?: DefaultMutableTreeNode("$part ($count)").also { parent.add(it) }
            }
        }
        treeModel.reload()
    }
}
