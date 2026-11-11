package com.jakubjirak.obsidianbridge.ui.toolwindow

import com.intellij.openapi.Disposable
import com.intellij.openapi.application.ApplicationManager
import com.intellij.openapi.fileEditor.FileEditorManager
import com.intellij.openapi.fileEditor.FileEditorManagerEvent
import com.intellij.openapi.fileEditor.FileEditorManagerListener
import com.intellij.openapi.project.Project
import com.intellij.openapi.util.Disposer
import com.intellij.openapi.wm.ToolWindow
import com.intellij.openapi.wm.ToolWindowFactory
import com.intellij.ui.components.JBList
import com.intellij.ui.components.JBScrollPane
import com.intellij.ui.content.ContentFactory
import com.jakubjirak.obsidianbridge.core.Backlink
import com.jakubjirak.obsidianbridge.core.IndexService
import com.jakubjirak.obsidianbridge.core.VaultManager
import java.awt.BorderLayout
import java.awt.event.MouseAdapter
import java.awt.event.MouseEvent
import javax.swing.DefaultListModel
import javax.swing.JLabel
import javax.swing.JPanel

class BacklinksToolWindowFactory : ToolWindowFactory {
    override fun createToolWindowContent(project: Project, toolWindow: ToolWindow) {
        val panel = BacklinksPanel(project)
        Disposer.register(toolWindow.disposable, panel)
        toolWindow.contentManager.addContent(ContentFactory.getInstance().createContent(panel, "", false))
    }
}

/** Lists the notes linking to the Markdown note open in the editor; double-click opens the linking note. */
class BacklinksPanel(private val project: Project) : JPanel(BorderLayout()), Disposable {
    private val listModel = DefaultListModel<Backlink>()
    private val list = JBList(listModel)
    private val status = JLabel()
    private val indexListener: () -> Unit = { ApplicationManager.getApplication().invokeLater({ refresh() }, project.disposed) }

    init {
        list.cellRenderer = javax.swing.ListCellRenderer { _, value, _, selected, _ ->
            JLabel("${value.source.title}  —  line ${value.link.sourceLineNumber}: ${value.link.raw}").apply {
                isOpaque = selected
                if (selected) background = list.selectionBackground
            }
        }
        list.addMouseListener(object : MouseAdapter() {
            override fun mouseClicked(e: MouseEvent) {
                if (e.clickCount == 2) list.selectedValue?.let { NoteOpener.open(project, it.source) }
            }
        })
        add(JBScrollPane(list), BorderLayout.CENTER)
        add(status, BorderLayout.SOUTH)
        IndexService.getInstance(project).addChangeListener(indexListener)
        project.messageBus.connect(this).subscribe(
            FileEditorManagerListener.FILE_EDITOR_MANAGER,
            object : FileEditorManagerListener {
                override fun selectionChanged(event: FileEditorManagerEvent) = refresh()
            }
        )
        refresh()
    }

    fun refresh() {
        listModel.clear()
        val file = FileEditorManager.getInstance(project).selectedFiles.firstOrNull()
        val index = IndexService.getInstance(project)
        val note = file?.let { VaultManager.getInstance(project).noteForFile(it, index) }
        if (note == null) {
            status.text = "Open a note from an Obsidian vault to see its backlinks"
            return
        }
        val backlinks = index.getBacklinks(note.id)
        backlinks.forEach { listModel.addElement(it) }
        status.text = if (backlinks.isEmpty()) "No backlinks to ${note.title}" else "${backlinks.size} backlink(s) to ${note.title}"
    }

    override fun dispose() {
        IndexService.getInstance(project).removeChangeListener(indexListener)
    }
}
