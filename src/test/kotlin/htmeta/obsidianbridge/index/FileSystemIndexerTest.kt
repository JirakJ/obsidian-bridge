package htmeta.obsidianbridge.index

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test
import kotlin.io.path.createTempDirectory
import java.nio.file.Files

class FileSystemIndexerTest {
    @Test
    fun `indexes wiki links`() {
        val tmp = createTempDirectory()
        val file = tmp.resolve("note.md")
        Files.writeString(file, "This is a link to [[Other Note]] and [[Other Note|alias]]")
        val indexer = FileSystemIndexer()
        indexer.indexVault(tmp)
        assertEquals(1, indexer.index.size)
        val links = indexer.index[file] ?: emptySet()
        assertEquals(setOf("Other Note"), links)
    }
}
