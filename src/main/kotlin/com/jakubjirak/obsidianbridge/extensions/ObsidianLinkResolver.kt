package com.jakubjirak.obsidianbridge.extensions

import com.jakubjirak.obsidianbridge.core.model.VaultNote

interface ObsidianLinkResolver {
    fun resolve(link: String, context: VaultNote): VaultNote?
    fun priority(): Int
}
