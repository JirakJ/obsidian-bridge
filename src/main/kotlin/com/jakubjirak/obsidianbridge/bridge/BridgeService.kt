package com.jakubjirak.obsidianbridge.bridge

import com.intellij.openapi.components.Service
import com.intellij.openapi.diagnostic.Logger
import com.intellij.openapi.project.Project
import com.jakubjirak.obsidianbridge.core.model.BridgeEntry
import com.jakubjirak.obsidianbridge.license.Feature
import com.jakubjirak.obsidianbridge.license.LicenseService

@Service(Service.Level.PROJECT)
class BridgeService(private val project: Project) {

    private val log = Logger.getInstance(BridgeService::class.java)
    private val entries = mutableMapOf<String, BridgeEntry>()

    private val markerPatterns = listOf(
        Regex("""//\s*(TODO|FIXME|HACK|NOTE):\s*(.+)"""),
        Regex("""/\*\s*(TODO|FIXME|HACK|NOTE):\s*(.+)\*/"""),
        Regex("""#\s*(TODO|FIXME|HACK|NOTE):\s*(.+)""")
    )

    fun scanFile(filePath: String, content: String): List<BridgeEntry> {
        val licenseService = LicenseService.getInstance()
        if (!licenseService.isFeatureAvailable(Feature.BRIDGE_MARKERS)) {
            log.info("Bridge markers is a Pro feature — skipping scan")
            return emptyList()
        }

        val found = mutableListOf<BridgeEntry>()
        content.lines().forEachIndexed { index, line ->
            for (pattern in markerPatterns) {
                val match = pattern.find(line)
                if (match != null) {
                    val entry = BridgeEntry(
                        codeFile = filePath,
                        lineNumber = index + 1,
                        markerType = match.groupValues[1],
                        text = match.groupValues[2].trim()
                    )
                    entries[entry.id] = entry
                    found.add(entry)
                }
            }
        }
        return found
    }

    fun getEntries(): Collection<BridgeEntry> = entries.values

    companion object {
        fun getInstance(project: Project): BridgeService =
            project.getService(BridgeService::class.java)
    }
}
