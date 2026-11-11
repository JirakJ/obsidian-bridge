package com.jakubjirak.obsidianbridge.core

import com.intellij.openapi.components.Service
import com.intellij.openapi.diagnostic.Logger
import com.intellij.openapi.project.Project
import com.jakubjirak.obsidianbridge.core.model.LinkResolution
import com.jakubjirak.obsidianbridge.core.model.VaultConfig
import com.jakubjirak.obsidianbridge.core.model.VaultNote
import com.jakubjirak.obsidianbridge.core.model.WikiLink
import com.jakubjirak.obsidianbridge.parser.FrontmatterParser
import com.jakubjirak.obsidianbridge.parser.TagExtractor
import com.jakubjirak.obsidianbridge.parser.WikiLinkParser
import com.jakubjirak.obsidianbridge.resolver.LinkResolver
import java.io.IOException
import java.nio.file.Files
import java.nio.file.Path
import java.security.MessageDigest
import java.time.Instant
import java.util.concurrent.CopyOnWriteArrayList

/** A wiki-link pointing at a note, together with the note that contains it. */
data class Backlink(val source: VaultNote, val link: WikiLink)

/** A resolved note-to-note edge for the graph view. */
data class NoteEdge(val fromId: String, val toId: String)

/**
 * In-memory index of the notes of all registered vaults. Pure Kotlin (no IDE APIs) so it is
 * unit-testable; [IndexService] exposes it as a project service. All access is synchronised
 * because indexing runs on background threads while tool windows read on the EDT.
 */
open class NoteIndex {

    private val log = Logger.getInstance(NoteIndex::class.java)
    private val lock = Any()
    private val notes = linkedMapOf<String, VaultNote>()
    private val listeners = CopyOnWriteArrayList<() -> Unit>()

    fun addChangeListener(listener: () -> Unit) {
        listeners += listener
    }

    fun removeChangeListener(listener: () -> Unit) {
        listeners -= listener
    }

    private fun fireChanged() = listeners.forEach { it() }

    fun getAllNotes(): List<VaultNote> = synchronized(lock) { notes.values.toList() }

    fun findById(id: String): VaultNote? = synchronized(lock) { notes[id] }

    fun findByRelativePath(vaultId: String, relativePath: String): VaultNote? {
        val normalized = normalize(relativePath)
        return synchronized(lock) { notes.values.find { it.vaultId == vaultId && it.relativePath == normalized } }
    }

    /** Notes whose title or file name equals [name] (case-insensitive, `.md` optional). */
    fun findByName(name: String): List<VaultNote> {
        val key = name.removeSuffix(".md").lowercase()
        return synchronized(lock) {
            notes.values.filter {
                it.title.lowercase() == key || it.relativePath.substringAfterLast('/').removeSuffix(".md").lowercase() == key
            }
        }
    }

    fun findByAlias(alias: String): List<VaultNote> {
        val key = alias.lowercase()
        return synchronized(lock) { notes.values.filter { note -> note.aliases.any { it.lowercase() == key } } }
    }

    /** The note a wiki-link in [source] points to, if it can be resolved. */
    fun resolve(link: WikiLink, source: VaultNote?): VaultNote? =
        (LinkResolver(this).resolve(link.target, source) as? LinkResolution.Resolved)?.note

    /** All links (in any note) that resolve to [noteId]. */
    fun getBacklinks(noteId: String): List<Backlink> = getAllNotes().flatMap { source ->
        source.outgoingLinks.filter { resolve(it, source)?.id == noteId }.map { Backlink(source, it) }
    }

    /** Distinct resolved links between indexed notes (self-links excluded). */
    fun edges(): List<NoteEdge> = getAllNotes().flatMap { source ->
        source.outgoingLinks.mapNotNull { link -> resolve(link, source)?.id?.let { NoteEdge(source.id, it) } }
    }.filter { it.fromId != it.toId }.distinct()

    /** Tag → number of notes carrying it. */
    fun tagCounts(): Map<String, Int> =
        getAllNotes().flatMap { it.tags.distinct() }.groupingBy { it }.eachCount().toSortedMap()

    /** Re-indexes every Markdown file of [config] (replacing that vault's previous entries). */
    fun indexVault(config: VaultConfig) {
        val root = Path.of(config.path)
        if (!Files.isDirectory(root)) {
            log.warn("Vault path not found: ${config.path}")
            return
        }
        val parsed = try {
            Files.walk(root).use { stream ->
                stream.filter { Files.isRegularFile(it) && it.fileName.toString().endsWith(".md") }
                    .filter { !isExcluded(config, relativePath(root, it)) }
                    .toList()
            }.mapNotNull { readNote(config, root, it) }
        } catch (e: IOException) {
            log.warn("Error indexing vault ${config.name}: ${e.message}")
            return
        }
        synchronized(lock) {
            notes.values.removeIf { it.vaultId == config.id }
            parsed.forEach { notes[it.id] = it }
        }
        log.info("Indexed ${parsed.size} notes in vault ${config.name}")
        fireChanged()
    }

    /** Indexes (or re-indexes) a single file of [config]. */
    fun indexFile(config: VaultConfig, root: Path, path: Path) {
        if (isExcluded(config, relativePath(root, path))) return
        val note = readNote(config, root, path) ?: return
        synchronized(lock) { notes[note.id] = note }
        fireChanged()
    }

    /** Removes the note at [relativePath] of vault [vaultId], if indexed. */
    fun removeByPath(vaultId: String, relativePath: String) {
        val removed = synchronized(lock) {
            val id = notes.values.find { it.vaultId == vaultId && it.relativePath == normalize(relativePath) }?.id
            id != null && notes.remove(id) != null
        }
        if (removed) fireChanged()
    }

    fun removeNote(noteId: String) {
        val removed = synchronized(lock) { notes.remove(noteId) != null }
        if (removed) fireChanged()
    }

    fun clear() {
        synchronized(lock) { notes.clear() }
        fireChanged()
    }

    private fun readNote(config: VaultConfig, root: Path, path: Path): VaultNote? = try {
        val content = Files.readString(path)
        buildNote(config.id, relativePath(root, path), content, Files.getLastModifiedTime(path).toInstant())
    } catch (e: IOException) {
        log.warn("Error indexing file $path: ${e.message}")
        null
    }

    companion object {
        /** Builds the index entry for one note's [content]. */
        fun buildNote(vaultId: String, relativePath: String, content: String, modified: Instant = Instant.EPOCH): VaultNote {
            val rel = normalize(relativePath)
            val id = sha256("$vaultId:$rel")
            val frontmatter = FrontmatterParser.parse(content)
            val title = (frontmatter["title"] as? String)?.takeIf { it.isNotBlank() }
                ?: rel.substringAfterLast('/').removeSuffix(".md")
            val aliases = when (val raw = frontmatter["aliases"] ?: frontmatter["alias"]) {
                is List<*> -> raw.map { it.toString() }
                is String -> listOf(raw)
                else -> emptyList()
            }
            return VaultNote(
                id = id,
                vaultId = vaultId,
                relativePath = rel,
                title = title,
                aliases = aliases,
                tags = TagExtractor.extract(content, frontmatter),
                frontmatter = frontmatter,
                outgoingLinks = WikiLinkParser.parse(content, id),
                created = modified,
                modified = modified,
                contentHash = sha256(content),
                body = FrontmatterParser.body(content),
            )
        }

        /**
         * `true` if [relativePath] falls under one of the vault's exclude patterns
         * (a folder glob such as `.trash/` followed by two stars, or a plain folder prefix).
         */
        fun isExcluded(config: VaultConfig, relativePath: String): Boolean {
            val rel = normalize(relativePath)
            return config.excludePatterns.any { pattern ->
                val prefix = pattern.removeSuffix("/**").removeSuffix("/").trim()
                prefix.isNotEmpty() && (rel == prefix || rel.startsWith("$prefix/"))
            }
        }

        fun relativePath(root: Path, path: Path): String = normalize(root.relativize(path).toString())

        /** Vault-relative paths always use `/`, regardless of OS. */
        fun normalize(path: String): String = path.replace('\\', '/').removePrefix("./").trimStart('/')

        private fun sha256(input: String): String =
            MessageDigest.getInstance("SHA-256").digest(input.toByteArray()).joinToString("") { "%02x".format(it) }
    }
}

@Service(Service.Level.PROJECT)
class IndexService : NoteIndex() {
    companion object {
        fun getInstance(project: Project): IndexService = project.getService(IndexService::class.java)
    }
}
