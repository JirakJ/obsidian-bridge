package com.jakubjirak.obsidianbridge.core

import com.intellij.openapi.diagnostic.Logger
import com.intellij.openapi.project.ProjectManager
import com.intellij.openapi.vfs.AsyncFileListener
import com.intellij.openapi.vfs.newvfs.events.*

class VaultFileListener : AsyncFileListener {

    private val log = Logger.getInstance(VaultFileListener::class.java)

    override fun prepareChange(events: MutableList<out VFileEvent>): AsyncFileListener.ChangeApplier? {
        val relevantEvents = events.filter { event ->
            val path = event.path
            path.endsWith(".md")
        }
        if (relevantEvents.isEmpty()) return null

        return object : AsyncFileListener.ChangeApplier {
            override fun afterVfsChange() {
                for (project in ProjectManager.getInstance().openProjects) {
                    val vaultManager = VaultManager.getInstance(project)
                    val indexService = IndexService.getInstance(project)

                    for (event in relevantEvents) {
                        try {
                            when (event) {
                                is VFileCreateEvent, is VFileContentChangeEvent -> {
                                    val file = event.file ?: continue
                                    val config = vaultManager.getVaultForFile(file) ?: continue
                                    val root = java.nio.file.Path.of(config.path)
                                    indexService.indexFile(config, root, file.toNioPath())
                                }
                                is VFileDeleteEvent -> {
                                    val file = event.file
                                    val config = vaultManager.getVaultForFile(file) ?: continue
                                    val relativePath = file.path.removePrefix(config.path).trimStart('/')
                                    val id = indexService.findByRelativePath(config.id, relativePath)?.id
                                    if (id != null) indexService.removeNote(id)
                                }
                                else -> {} // ignore move/rename for now
                            }
                        } catch (e: Exception) {
                            log.warn("Error processing VFS event: ${e.message}")
                        }
                    }
                }
            }
        }
    }
}
