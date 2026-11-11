package com.jakubjirak.obsidianbridge.core

import com.intellij.openapi.application.ApplicationManager
import com.intellij.openapi.project.ProjectManager
import com.intellij.openapi.vfs.AsyncFileListener
import com.intellij.openapi.vfs.VirtualFile
import com.intellij.openapi.vfs.newvfs.events.VFileContentChangeEvent
import com.intellij.openapi.vfs.newvfs.events.VFileCreateEvent
import com.intellij.openapi.vfs.newvfs.events.VFileDeleteEvent
import com.intellij.openapi.vfs.newvfs.events.VFileEvent
import com.intellij.openapi.vfs.newvfs.events.VFileMoveEvent
import com.intellij.openapi.vfs.newvfs.events.VFilePropertyChangeEvent
import com.jakubjirak.obsidianbridge.core.model.VaultConfig
import java.nio.file.Path

/** Keeps the note index in sync with Markdown files created, edited, renamed, moved or deleted in a vault. */
class VaultFileListener : AsyncFileListener {

    override fun prepareChange(events: MutableList<out VFileEvent>): AsyncFileListener.ChangeApplier? {
        val relevant = events.filter { isMarkdown(it.path) || isMarkdown(oldPath(it) ?: "") }
        if (relevant.isEmpty()) return null

        return object : AsyncFileListener.ChangeApplier {
            override fun afterVfsChange() {
                // Runs inside the VFS write action: do the file reading on a pooled thread.
                ApplicationManager.getApplication().executeOnPooledThread {
                    ProjectManager.getInstance().openProjects.filterNot { it.isDisposed }.forEach { project ->
                        val vaults = VaultManager.getInstance(project).getActiveVaults()
                        val index = IndexService.getInstance(project)
                        if (vaults.isNotEmpty()) relevant.forEach { apply(it, vaults, index) }
                    }
                }
            }
        }
    }

    private fun apply(event: VFileEvent, vaults: List<VaultConfig>, index: NoteIndex) {
        oldPath(event)?.let { remove(it, vaults, index) }
        when (event) {
            is VFileDeleteEvent -> remove(event.path, vaults, index)
            is VFileCreateEvent, is VFileContentChangeEvent, is VFileMoveEvent, is VFilePropertyChangeEvent ->
                event.file?.let { index(it, vaults, index) }
        }
    }

    private fun index(file: VirtualFile, vaults: List<VaultConfig>, index: NoteIndex) {
        if (!isMarkdown(file.path) || !file.isValid) return
        val vault = VaultManager.vaultForPath(vaults, file.path) ?: return
        index.indexFile(vault, Path.of(vault.path), file.toNioPath())
    }

    private fun remove(path: String, vaults: List<VaultConfig>, index: NoteIndex) {
        val vault = VaultManager.vaultForPath(vaults, path) ?: return
        VaultManager.relativeToVault(vault, path)?.let { index.removeByPath(vault.id, it) }
    }

    private fun oldPath(event: VFileEvent): String? = when {
        event is VFileMoveEvent -> event.oldPath
        event is VFilePropertyChangeEvent && event.propertyName == VirtualFile.PROP_NAME -> event.oldPath
        else -> null
    }

    private fun isMarkdown(path: String) = path.endsWith(".md", ignoreCase = true)
}
