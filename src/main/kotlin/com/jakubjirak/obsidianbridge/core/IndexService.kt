package com.jakubjirak.obsidianbridge.core

import com.intellij.openapi.components.Service
import com.intellij.openapi.diagnostic.Logger
import com.intellij.openapi.project.Project
import com.jakubjirak.obsidianbridge.core.model.*
import com.jakubjirak.obsidianbridge.parser.FrontmatterParser
import com.jakubjirak.obsidianbridge.parser.WikiLinkParser
import com.jakubjirak.obsidianbridge.parser.TagExtractor
import java.nio.file.Files
import java.nio.file.Path
import java.security.MessageDigest

@Service(Service.Level.PROJECT)
class IndexService(private val project: Project) {

    private val log = Logger.getInstance(IndexService::class.java)
    private val notes = mutableMapOf<String, VaultNote>()
    private val nameIndex = mutableMapOf<String, MutableList<VaultNote>>()

    fun getAllNotes(): Collection<VaultNote> = notes.values

    fun findById(id: String): VaultNote? = notes[id]

    fun findByRelativePath(vaultId: String, relativePath: String): VaultNote? {
        return notes.values.find { it.vaultId == vaultId && it.relativePath == relativePath }
    }

    fun findByName(name: String): List<VaultNote> {
        val normalised = name.removeSuffix(".md").lowercase()
        return nameIndex[normalised] ?: emptyList()
    }

    fun findByAlias(alias: String): List<VaultNote> {
        val normalised = alias.lowercase()
        return notes.values.filter { note ->
            note.aliases.any { it.lowercase() == normalised }
        }
    }

    fun getBacklinks(noteId: String): List<WikiLink> {
        return notes.values.flatMap { note ->
            note.outgoingLinks.filter { it.resolvedNoteId == noteId }
        }
    }

    fun indexVault(config: VaultConfig) {
        log.info("Indexing vault: ${config.name}")
        val root = Path.of(config.path)
        if (!Files.isDirectory(root)) {
            log.warn("Vault path not found: ${config.path}")
            return
        }
        runCatching {
            Files.walk(root).use { stream ->
                stream
                    .filter { Files.isRegularFile(it) && it.toString().endsWith(".md") }
                    .filter { path ->
                        config.excludePatterns.none { pattern ->
                            val rel = root.relativize(path).toString()
                            rel.startsWith(pattern.removeSuffix("/**"))
                        }
                    }
                    .forEach { path -> indexFile(config, root, path) }
            }
        }.onFailure { e ->
            log.warn("Error indexing vault ${config.name}: ${e.message}")
        }
        log.info("Indexed ${notes.size} notes in vault ${config.name}")
    }

    fun indexFile(config: VaultConfig, root: Path, path: Path) {
        runCatching {
            val content = Files.readString(path)
            val relativePath = root.relativize(path).toString()
            val id = sha256("${config.id}:$relativePath")
            val frontmatter = FrontmatterParser.parse(content)
            val links = WikiLinkParser.parse(content, id)
            val tags = TagExtractor.extract(content, frontmatter)
            val title = (frontmatter["title"] as? String)
                ?: path.fileName.toString().removeSuffix(".md")
            @Suppress("UNCHECKED_CAST")
            val aliases = (frontmatter["aliases"] as? List<String>) ?: emptyList()

            val note = VaultNote(
                id = id,
                vaultId = config.id,
                relativePath = relativePath,
                title = title,
                aliases = aliases,
                tags = tags,
                frontmatter = frontmatter,
                outgoingLinks = links,
                created = Files.getAttribute(path, "creationTime")?.let {
                    (it as java.nio.file.attribute.FileTime).toInstant()
                } ?: java.time.Instant.now(),
                modified = Files.getLastModifiedTime(path).toInstant(),
                contentHash = sha256(content)
            )
            notes[id] = note
            val nameKey = title.lowercase()
            nameIndex.getOrPut(nameKey) { mutableListOf() }.add(note)
        }.onFailure { e ->
            log.warn("Error indexing file $path: ${e.message}")
        }
    }

    fun removeNote(noteId: String) {
        val note = notes.remove(noteId) ?: return
        val nameKey = note.title.lowercase()
        nameIndex[nameKey]?.removeIf { it.id == noteId }
    }

    fun clear() {
        notes.clear()
        nameIndex.clear()
    }

    private fun sha256(input: String): String {
        val digest = MessageDigest.getInstance("SHA-256")
        return digest.digest(input.toByteArray()).joinToString("") { "%02x".format(it) }
    }

    companion object {
        fun getInstance(project: Project): IndexService =
            project.getService(IndexService::class.java)
    }
}
