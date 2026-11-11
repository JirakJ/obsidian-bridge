package com.jakubjirak.obsidianbridge.ui.settings

import com.intellij.openapi.application.ApplicationManager
import com.intellij.openapi.components.PersistentStateComponent
import com.intellij.openapi.components.Service
import com.intellij.openapi.components.State
import com.intellij.openapi.components.Storage
import com.jakubjirak.obsidianbridge.core.model.VaultConfig

data class SettingsState(
    var vaults: List<VaultConfig> = emptyList(),
    var showGutterIcons: Boolean = true,
) {
    // No-arg constructor for serialization
    constructor() : this(vaults = emptyList())
}

@Service(Service.Level.APP)
@State(name = "ObsidianBridgeSettings", storages = [Storage("obsidian-bridge.xml")])
class ObsidianBridgeSettings : PersistentStateComponent<SettingsState> {

    private var myState = SettingsState()

    override fun getState(): SettingsState = myState

    override fun loadState(state: SettingsState) {
        myState = state
    }

    companion object {
        fun getInstance(): ObsidianBridgeSettings =
            ApplicationManager.getApplication().getService(ObsidianBridgeSettings::class.java)

        /**
         * Settings after editing the primary (first) vault in the settings page. A blank path
         * removes it; other vaults (edited in `obsidian-bridge.xml`) are kept unchanged.
         */
        fun withPrimaryVault(state: SettingsState, path: String, name: String, showGutterIcons: Boolean): SettingsState {
            val vaults = state.vaults.toMutableList()
            val trimmedPath = path.trim()
            val displayName = name.trim().ifEmpty { trimmedPath.trimEnd('/', '\\').substringAfterLast('/').substringAfterLast('\\') }
            when {
                trimmedPath.isEmpty() -> if (vaults.isNotEmpty()) vaults.removeAt(0)
                vaults.isEmpty() -> vaults += VaultConfig(path = trimmedPath, name = displayName)
                else -> vaults[0] = vaults[0].copy(path = trimmedPath, name = displayName)
            }
            return state.copy(vaults = vaults, showGutterIcons = showGutterIcons)
        }
    }
}
