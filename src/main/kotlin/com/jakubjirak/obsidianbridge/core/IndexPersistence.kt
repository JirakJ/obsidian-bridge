package com.jakubjirak.obsidianbridge.core

import com.intellij.openapi.components.PersistentStateComponent
import com.intellij.openapi.components.Service
import com.intellij.openapi.components.State
import com.intellij.openapi.components.Storage
import com.intellij.openapi.project.Project

data class IndexCacheState(
    var noteHashes: MutableMap<String, String> = mutableMapOf(),
    var lastIndexTimestamp: Long = 0L
) {
    constructor() : this(noteHashes = mutableMapOf())
}

@Service(Service.Level.PROJECT)
@State(name = "ObsidianBridgeIndexCache", storages = [Storage("obsidian-bridge-index-cache.xml")])
class IndexPersistence(private val project: Project) : PersistentStateComponent<IndexCacheState> {

    private var myState = IndexCacheState()

    override fun getState(): IndexCacheState = myState

    override fun loadState(state: IndexCacheState) {
        myState = state
    }

    fun hasChanged(relativePath: String, hash: String): Boolean {
        val cached = myState.noteHashes[relativePath]
        return cached != hash
    }

    fun updateHash(relativePath: String, hash: String) {
        myState.noteHashes[relativePath] = hash
    }

    fun removeHash(relativePath: String) {
        myState.noteHashes.remove(relativePath)
    }

    fun markIndexed() {
        myState.lastIndexTimestamp = System.currentTimeMillis()
    }

    fun getLastIndexTimestamp(): Long = myState.lastIndexTimestamp

    companion object {
        fun getInstance(project: Project): IndexPersistence =
            project.getService(IndexPersistence::class.java)
    }
}
