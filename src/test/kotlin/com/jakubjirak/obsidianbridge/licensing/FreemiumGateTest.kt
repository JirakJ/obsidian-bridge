/*
 * Copyright (c) 2026 Jakub Jirák. All rights reserved.
 * Licensed under the terms of the JetBrains Marketplace EULA.
 */
package com.jakubjirak.obsidianbridge.licensing

import com.jakubjirak.obsidianbridge.licensing.FreemiumGate.Companion.FEATURE_ADVANCED_SEARCH
import com.jakubjirak.obsidianbridge.licensing.FreemiumGate.Companion.FEATURE_BACKLINK_ANALYTICS
import com.jakubjirak.obsidianbridge.licensing.FreemiumGate.Companion.FEATURE_CODE_BRIDGING
import com.jakubjirak.obsidianbridge.licensing.FreemiumGate.Companion.FEATURE_GRAPH_EXPORT
import com.jakubjirak.obsidianbridge.licensing.FreemiumGate.Companion.FEATURE_GRAPH_VIEW
import com.jakubjirak.obsidianbridge.licensing.FreemiumGate.Companion.FEATURE_TAG_ANALYTICS
import com.jakubjirak.obsidianbridge.licensing.FreemiumGate.Companion.FEATURE_TEMPLATES
import com.jakubjirak.obsidianbridge.licensing.FreemiumGate.Companion.PRODUCT_CODE
import com.jakubjirak.obsidianbridge.licensing.FreemiumGate.LicenseStatus
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test

class FreemiumGateTest {

    @Test
    fun `product code is correct`() {
        assertEquals("POBSIDIANIDEBRI", PRODUCT_CODE)
    }

    @Test
    fun `LicenseStatus enum has three values`() {
        val values = LicenseStatus.entries
        assertEquals(3, values.size)
        assertTrue(values.contains(LicenseStatus.FREE))
        assertTrue(values.contains(LicenseStatus.TRIAL))
        assertTrue(values.contains(LicenseStatus.PAID))
    }

    @Test
    fun `premium feature constants are defined`() {
        assertEquals("graph_view", FEATURE_GRAPH_VIEW)
        assertEquals("code_bridging", FEATURE_CODE_BRIDGING)
        assertEquals("templates", FEATURE_TEMPLATES)
        assertEquals("advanced_search", FEATURE_ADVANCED_SEARCH)
        assertEquals("tag_analytics", FEATURE_TAG_ANALYTICS)
        assertEquals("backlink_analytics", FEATURE_BACKLINK_ANALYTICS)
        assertEquals("graph_export", FEATURE_GRAPH_EXPORT)
    }

    @Test
    fun `getLicenseStatus returns FREE when no LicensingFacade`() {
        val gate = FreemiumGate()
        assertEquals(LicenseStatus.FREE, gate.getLicenseStatus())
    }

    @Test
    fun `isPaidFeatureAvailable returns false for premium feature when FREE`() {
        val gate = FreemiumGate()
        assertFalse(gate.isPaidFeatureAvailable(FEATURE_GRAPH_VIEW))
        assertFalse(gate.isPaidFeatureAvailable(FEATURE_CODE_BRIDGING))
        assertFalse(gate.isPaidFeatureAvailable(FEATURE_TEMPLATES))
        assertFalse(gate.isPaidFeatureAvailable(FEATURE_ADVANCED_SEARCH))
        assertFalse(gate.isPaidFeatureAvailable(FEATURE_TAG_ANALYTICS))
        assertFalse(gate.isPaidFeatureAvailable(FEATURE_BACKLINK_ANALYTICS))
        assertFalse(gate.isPaidFeatureAvailable(FEATURE_GRAPH_EXPORT))
    }

    @Test
    fun `isPaidFeatureAvailable returns true for non-premium feature when FREE`() {
        val gate = FreemiumGate()
        assertTrue(gate.isPaidFeatureAvailable("basic_navigation"))
        assertTrue(gate.isPaidFeatureAvailable("some_other_feature"))
    }

    @Test
    fun `isPremiumUser returns false when FREE`() {
        val gate = FreemiumGate()
        assertFalse(gate.isPremiumUser())
    }
}
