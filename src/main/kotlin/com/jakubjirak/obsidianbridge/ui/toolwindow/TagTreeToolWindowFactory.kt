package com.jakubjirak.obsidianbridge.ui.toolwindow

import com.intellij.openapi.Disposable
import com.intellij.openapi.application.ApplicationManager
import com.intellij.openapi.project.Project
import com.intellij.openapi.util.Disposer
import com.intellij.openapi.wm.ToolWindow
import com.intellij.openapi.wm.ToolWindowFactory
import com.intellij.ui.components.JBScrollPane
import com.intellij.ui.content.ContentFactory
import com.intellij.ui.treeStructure.Tree
import com.jakubjirak.obsidianbridge.core.IndexService
import java.awt.BorderLayout
import javax.swing.JPanel
import javax.swing.tree.DefaultMutableTreeNode
import javax.swing.tree.DefaultTreeModel

class TagTreeToolWindowFactory : ToolWindowFactory {
    override fun createToolWindowContent(project: Project, toolWindow: ToolWindow) {
        val panel = TagTreePanel(project)
        Disposer.register(toolWindow.disposable, panel)
        toolWindow.contentManager.addContent(ContentFactory.getInstance().createContent(panel, "", false))
    }
}

/** A tag node: the tag segment, its full path and how many notes carry the tag or a nested tag. */
data class TagNode(val name: String, val path: String, var count: Int = 0) {
    val children = sortedMapOf<String, TagNode>()
    override fun toString(): String = "$name ($count)"
}

/** Builds the nested tag hierarchy (`project/alpha` → `project` › `alpha`). Pure logic. */
object TagHierarchy {
    fun build(tagCounts: Map<String, Int>): TagNode {
        val root = TagNode("Tags", "")
        for ((tag, count) in tagCounts) {
            var node = root
            root.count += count
            for (segment in tag.split('/').filter { it.isNotEmpty() }) {
                val path = if (node.path.isEmpty()) segment else "${node.path}/$segment"
                node = node.children.getOrPut(segment) { TagNode(segment, path) }
                node.count += count
            }
        }
        return root
    }
}

class TagTreePanel(private val project: Project) : JPanel(BorderLayout()), Disposable {
    private val treeModel = DefaultTreeModel(DefaultMutableTreeNode("Tags"))
    private val tree = Tree(treeModel)
    private val indexListener: () -> Unit = { ApplicationManager.getApplication().invokeLater({ refresh() }, project.disposed) }

    init {
        add(JBScrollPane(tree), BorderLayout.CENTER)
        IndexService.getInstance(project).addChangeListener(indexListener)
        refresh()
    }

    fun refresh() {
        val root = TagHierarchy.build(IndexService.getInstance(project).tagCounts())
        treeModel.setRoot(toSwing(root))
    }

    private fun toSwing(node: TagNode): DefaultMutableTreeNode =
        DefaultMutableTreeNode(node).also { swing -> node.children.values.forEach { swing.add(toSwing(it)) } }

    override fun dispose() {
        IndexService.getInstance(project).removeChangeListener(indexListener)
    }
}
