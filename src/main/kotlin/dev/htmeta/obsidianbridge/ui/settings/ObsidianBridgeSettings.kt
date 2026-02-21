package dev.htmeta.obsidianbridge.ui.settings

import com.intellij.openapi.application.ApplicationManager
import com.intellij.openapi.components.PersistentStateComponent
import com.intellij.openapi.components.Service
import com.intellij.openapi.components.State
import com.intellij.openapi.components.Storage
import dev.htmeta.obsidianbridge.core.model.VaultConfig

data class SettingsState(
    var vaults: List<VaultConfig> = emptyList(),
    var graphDepth: Int = 2,
    var unresolvedLinkColor: String = "#FF6B6B",
    var showGutterIcons: Boolean = true,
    var bridgeMarkerPatterns: String = "TODO|FIXME|HACK|NOTE"
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
    }
}
