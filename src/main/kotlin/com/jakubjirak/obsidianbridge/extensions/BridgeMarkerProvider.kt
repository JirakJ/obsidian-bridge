package com.jakubjirak.obsidianbridge.extensions

interface BridgeMarkerProvider {
    fun patterns(): List<Regex>
    fun markerType(): String
}
