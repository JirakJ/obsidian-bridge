package htmeta.obsidianbridge

import com.intellij.openapi.components.Service
import com.intellij.openapi.components.ServiceManager

@Service
class ObsidianBridgeService {
    init {
        // Placeholder for Obsidian Bridge core services
    }

    companion object {
        fun getInstance(): ObsidianBridgeService = ServiceManager.getService(ObsidianBridgeService::class.java)
    }
}
