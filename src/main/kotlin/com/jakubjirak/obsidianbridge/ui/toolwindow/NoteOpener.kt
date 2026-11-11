package com.jakubjirak.obsidianbridge.ui.toolwindow

import com.intellij.openapi.fileEditor.FileEditorManager
import com.intellij.openapi.project.Project
import com.intellij.openapi.vfs.LocalFileSystem
import com.jakubjirak.obsidianbridge.core.VaultManager
import com.jakubjirak.obsidianbridge.core.model.VaultNote
import java.nio.file.Path

/** Opens an indexed note in the editor. */
object NoteOpener {
    fun open(project: Project, note: VaultNote) {
        val vault = VaultManager.getInstance(project).getActiveVaults().find { it.id == note.vaultId } ?: return
        val file = LocalFileSystem.getInstance().refreshAndFindFileByNioFile(Path.of(vault.path, note.relativePath)) ?: return
        FileEditorManager.getInstance(project).openFile(file, true)
    }
}
