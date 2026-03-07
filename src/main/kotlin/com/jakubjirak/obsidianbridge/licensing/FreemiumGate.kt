/*
 * Copyright (c) 2026 Jakub Jirák. All rights reserved.
 * Licensed under the terms of the JetBrains Marketplace EULA.
 */
package com.jakubjirak.obsidianbridge.licensing

import com.intellij.openapi.application.ApplicationManager
import com.intellij.openapi.components.Service
import com.intellij.ui.LicensingFacade

@Service(Service.Level.APP)
class FreemiumGate {

    enum class LicenseStatus { FREE, TRIAL, PAID }

    companion object {
        const val PRODUCT_CODE = "POBSIDBRIDGE"

        const val FEATURE_GRAPH_VIEW = "graph_view"
        const val FEATURE_CODE_BRIDGING = "code_bridging"
        const val FEATURE_TEMPLATES = "templates"
        const val FEATURE_ADVANCED_SEARCH = "advanced_search"
        const val FEATURE_TAG_ANALYTICS = "tag_analytics"
        const val FEATURE_BACKLINK_ANALYTICS = "backlink_analytics"
        const val FEATURE_GRAPH_EXPORT = "graph_export"

        private val PREMIUM_FEATURES = setOf(
            FEATURE_GRAPH_VIEW, FEATURE_CODE_BRIDGING, FEATURE_TEMPLATES,
            FEATURE_ADVANCED_SEARCH, FEATURE_TAG_ANALYTICS, FEATURE_BACKLINK_ANALYTICS,
            FEATURE_GRAPH_EXPORT
        )

        fun getInstance(): FreemiumGate =
            ApplicationManager.getApplication().getService(FreemiumGate::class.java)
    }

    fun getLicenseStatus(): LicenseStatus {
        val facade = LicensingFacade.getInstance() ?: return LicenseStatus.FREE
        val stamp = facade.getConfirmationStamp(PRODUCT_CODE) ?: return LicenseStatus.FREE
        return if (stamp.startsWith("eval:")) LicenseStatus.TRIAL else LicenseStatus.PAID
    }

    fun isPaidFeatureAvailable(featureId: String): Boolean {
        if (featureId !in PREMIUM_FEATURES) return true
        return getLicenseStatus() != LicenseStatus.FREE
    }

    fun isPremiumUser(): Boolean = getLicenseStatus() != LicenseStatus.FREE
}
