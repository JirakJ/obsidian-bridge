package com.jakubjirak.obsidianbridge.license

import com.intellij.openapi.application.ApplicationManager
import com.intellij.openapi.components.Service

enum class Feature {
    WIKI_LINKS,
    BASIC_BACKLINKS,
    SEARCH,
    FRONTMATTER,
    SINGLE_VAULT,
    GRAPH_VIEW,
    BRIDGE_MARKERS,
    TAG_TREE,
    DAILY_NOTES,
    TEMPLATES,
    MULTI_VAULT,
    ADVANCED_SEARCH
}

enum class LicenseTier { FREE, PRO, TEAM }

@Service(Service.Level.APP)
class LicenseService {
    private var currentTier: LicenseTier = LicenseTier.FREE

    private val freeFeatures = setOf(
        Feature.WIKI_LINKS,
        Feature.BASIC_BACKLINKS,
        Feature.SEARCH,
        Feature.FRONTMATTER,
        Feature.SINGLE_VAULT
    )

    private val proFeatures = freeFeatures + setOf(
        Feature.GRAPH_VIEW,
        Feature.BRIDGE_MARKERS,
        Feature.TAG_TREE,
        Feature.DAILY_NOTES,
        Feature.TEMPLATES,
        Feature.MULTI_VAULT,
        Feature.ADVANCED_SEARCH
    )

    fun isFeatureAvailable(feature: Feature): Boolean {
        return when (currentTier) {
            LicenseTier.FREE -> feature in freeFeatures
            LicenseTier.PRO, LicenseTier.TEAM -> feature in proFeatures
        }
    }

    fun getCurrentTier(): LicenseTier = currentTier

    fun setTier(tier: LicenseTier) {
        currentTier = tier
    }

    companion object {
        fun getInstance(): LicenseService =
            ApplicationManager.getApplication().getService(LicenseService::class.java)
    }
}
