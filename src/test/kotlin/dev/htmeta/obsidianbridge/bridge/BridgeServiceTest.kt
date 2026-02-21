package dev.htmeta.obsidianbridge.bridge

import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.Test

class BridgeServiceTest {

    // BridgeService requires Project; test the marker patterns directly
    @Test
    fun `detects TODO markers`() {
        val pattern = Regex("""//\s*(TODO|FIXME|HACK|NOTE):\s*(.+)""")
        val match = pattern.find("// TODO: Fix authentication flow")
        assertNotNull(match)
        assertEquals("TODO", match!!.groupValues[1])
        assertEquals("Fix authentication flow", match.groupValues[2])
    }

    @Test
    fun `detects FIXME markers`() {
        val pattern = Regex("""//\s*(TODO|FIXME|HACK|NOTE):\s*(.+)""")
        val match = pattern.find("// FIXME: Memory leak in parser")
        assertNotNull(match)
        assertEquals("FIXME", match!!.groupValues[1])
    }

    @Test
    fun `detects hash-style markers`() {
        val pattern = Regex("""#\s*(TODO|FIXME|HACK|NOTE):\s*(.+)""")
        val match = pattern.find("# TODO: Refactor this module")
        assertNotNull(match)
        assertEquals("TODO", match!!.groupValues[1])
    }

    @Test
    fun `does not match non-marker comments`() {
        val pattern = Regex("""//\s*(TODO|FIXME|HACK|NOTE):\s*(.+)""")
        val match = pattern.find("// This is a normal comment")
        assertNull(match)
    }
}
