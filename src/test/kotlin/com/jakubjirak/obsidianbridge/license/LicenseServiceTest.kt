package com.jakubjirak.obsidianbridge.license

import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test

class LicenseServiceTest {
    private lateinit var service: LicenseService

    @BeforeEach
    fun setUp() {
        service = LicenseService()
    }

    @Test
    fun `default tier is FREE`() {
        assertEquals(LicenseTier.FREE, service.getCurrentTier())
    }

    @Test
    fun `setTier changes current tier`() {
        service.setTier(LicenseTier.PRO)
        assertEquals(LicenseTier.PRO, service.getCurrentTier())
        service.setTier(LicenseTier.TEAM)
        assertEquals(LicenseTier.TEAM, service.getCurrentTier())
    }

    // Free features available on FREE tier
    @Test
    fun `WIKI_LINKS available on FREE tier`() {
        assertTrue(service.isFeatureAvailable(Feature.WIKI_LINKS))
    }

    @Test
    fun `BASIC_BACKLINKS available on FREE tier`() {
        assertTrue(service.isFeatureAvailable(Feature.BASIC_BACKLINKS))
    }

    @Test
    fun `SEARCH available on FREE tier`() {
        assertTrue(service.isFeatureAvailable(Feature.SEARCH))
    }

    @Test
    fun `FRONTMATTER available on FREE tier`() {
        assertTrue(service.isFeatureAvailable(Feature.FRONTMATTER))
    }

    @Test
    fun `SINGLE_VAULT available on FREE tier`() {
        assertTrue(service.isFeatureAvailable(Feature.SINGLE_VAULT))
    }

    // Pro features NOT available on FREE tier
    @Test
    fun `GRAPH_VIEW not available on FREE tier`() {
        assertFalse(service.isFeatureAvailable(Feature.GRAPH_VIEW))
    }

    @Test
    fun `BRIDGE_MARKERS not available on FREE tier`() {
        assertFalse(service.isFeatureAvailable(Feature.BRIDGE_MARKERS))
    }

    @Test
    fun `TAG_TREE not available on FREE tier`() {
        assertFalse(service.isFeatureAvailable(Feature.TAG_TREE))
    }

    @Test
    fun `DAILY_NOTES not available on FREE tier`() {
        assertFalse(service.isFeatureAvailable(Feature.DAILY_NOTES))
    }

    @Test
    fun `TEMPLATES not available on FREE tier`() {
        assertFalse(service.isFeatureAvailable(Feature.TEMPLATES))
    }

    @Test
    fun `MULTI_VAULT not available on FREE tier`() {
        assertFalse(service.isFeatureAvailable(Feature.MULTI_VAULT))
    }

    @Test
    fun `ADVANCED_SEARCH not available on FREE tier`() {
        assertFalse(service.isFeatureAvailable(Feature.ADVANCED_SEARCH))
    }

    // All features available on PRO tier
    @Test
    fun `all features available on PRO tier`() {
        service.setTier(LicenseTier.PRO)
        for (feature in Feature.entries) {
            assertTrue(service.isFeatureAvailable(feature), "Feature $feature should be available on PRO tier")
        }
    }

    // All features available on TEAM tier
    @Test
    fun `all features available on TEAM tier`() {
        service.setTier(LicenseTier.TEAM)
        for (feature in Feature.entries) {
            assertTrue(service.isFeatureAvailable(feature), "Feature $feature should be available on TEAM tier")
        }
    }

    @Test
    fun `Feature enum has 12 values`() {
        assertEquals(12, Feature.entries.size)
    }

    @Test
    fun `LicenseTier enum has 3 values`() {
        assertEquals(3, LicenseTier.entries.size)
    }
}
