package dev.htmeta.obsidianbridge.extensions

import dev.htmeta.obsidianbridge.core.model.VaultNote

interface ObsidianLinkResolver {
    fun resolve(link: String, context: VaultNote): VaultNote?
    fun priority(): Int
}
