# GitHub Copilot Instructions — Obsidian Bridge for JetBrains

## Project Context

You are working on **Obsidian Bridge for JetBrains** — a premium JetBrains IDE plugin that brings Obsidian vault functionality into IntelliJ-based IDEs. The plugin enables wiki-link navigation, backlinks, frontmatter parsing, vault search, code↔notes bridging, graph visualization, and tag navigation.

**Owner:** Jakub — HTMETA.dev  
**Tech Stack:** Kotlin 1.9+, Gradle 8.x, IntelliJ Platform SDK 2023.3+, JUnit 5, D3.js (via JCEF)  
**Package Base:** `dev.htmeta.obsidianbridge`  
**License:** Proprietary (Free tier is open-source, Pro tier is licensed)

---

## Architecture Rules

### Module Structure

```
obsidian-bridge/
├── src/main/kotlin/dev/htmeta/obsidianbridge/
│   ├── core/              # Vault management, indexing, file watching
│   │   ├── VaultManager.kt
│   │   ├── IndexService.kt
│   │   ├── VaultFileListener.kt
│   │   └── model/         # Data classes: VaultConfig, VaultNote, WikiLink, etc.
│   ├── parser/            # Markdown, wiki-link, frontmatter parsing
│   │   ├── WikiLinkParser.kt
│   │   ├── FrontmatterParser.kt
│   │   ├── TagExtractor.kt
│   │   └── MarkdownProcessor.kt
│   ├── resolver/          # Link resolution logic
│   │   ├── LinkResolver.kt
│   │   └── ResolutionCache.kt
│   ├── search/            # Full-text search, filtering
│   │   ├── VaultSearchService.kt
│   │   ├── SearchIndex.kt
│   │   └── SearchFilters.kt
│   ├── bridge/            # Code ↔ Notes bridging
│   │   ├── BridgeService.kt
│   │   ├── TodoMarkerProvider.kt
│   │   └── NoteCreator.kt
│   ├── graph/             # Graph data model and D3.js integration
│   │   ├── GraphBuilder.kt
│   │   ├── GraphDataModel.kt
│   │   └── resources/graph.html  # D3.js visualization
│   ├── ui/                # All UI components
│   │   ├── toolwindow/    # Backlinks, Tags, Graph tool windows
│   │   ├── editor/        # Editor extensions, gutter icons, completion
│   │   ├── settings/      # Settings configurable panel
│   │   ├── search/        # Search dialog UI
│   │   └── notifications/ # Balloon notifications
│   ├── actions/           # IDE actions (keyboard shortcuts)
│   │   ├── SearchAction.kt
│   │   ├── DailyNoteAction.kt
│   │   ├── NewNoteAction.kt
│   │   └── ReindexAction.kt
│   ├── extensions/        # Extension points for third-party plugins
│   │   ├── ObsidianLinkResolver.kt
│   │   ├── FrontmatterProcessor.kt
│   │   └── BridgeMarkerProvider.kt
│   └── util/              # Shared utilities
│       ├── PathUtils.kt
│       ├── HashUtils.kt
│       └── CoroutineUtils.kt
├── src/main/resources/
│   ├── META-INF/plugin.xml
│   └── messages/ObsidianBridgeBundle.properties
├── src/test/kotlin/dev/htmeta/obsidianbridge/
│   ├── core/
│   ├── parser/
│   ├── resolver/
│   ├── search/
│   ├── bridge/
│   └── fixtures/          # Test vault fixtures
└── build.gradle.kts
```

### Critical Architecture Principles

1. **All I/O operations must be async.** Use Kotlin coroutines with `Dispatchers.IO`. Never block the EDT (Event Dispatch Thread). All VFS operations, file reads, and index updates run on background threads.

2. **Use IntelliJ Platform APIs first.** Before implementing anything custom, check if IntelliJ Platform provides it: `FileBasedIndex`, `VirtualFileListener`, `PsiElement`, `CompletionContributor`, `GutterIconProvider`, `ToolWindowFactory`, etc.

3. **Index everything, resolve lazily.** The `IndexService` pre-computes forward maps (note → links, tags). Backlinks and resolution are computed on demand from the forward index. Cache aggressively with invalidation via VFS events.

4. **No network calls ever.** This plugin operates 100% locally. No HTTP clients, no telemetry (unless opt-in Pro), no cloud services. Any code adding network calls must be rejected.

5. **Fail gracefully, never crash.** All parsing operations must handle malformed input. Use `runCatching {}` for file operations. Log warnings to IDE log, never show error dialogs for non-critical issues.

6. **Extension points for everything pluggable.** Link resolvers, frontmatter processors, bridge markers — all must be extensible via IntelliJ extension points defined in `plugin.xml`.

---

## Coding Standards

### Kotlin Style

```kotlin
// USE: Data classes for all models
data class VaultNote(
    val id: String,
    val relativePath: String,
    val title: String,
    val tags: List<String> = emptyList(),
    val outgoingLinks: List<WikiLink> = emptyList()
)

// USE: Sealed classes for result types
sealed class LinkResolution {
    data class Resolved(val note: VaultNote) : LinkResolution()
    data class Ambiguous(val candidates: List<VaultNote>) : LinkResolution()
    data object Unresolved : LinkResolution()
}

// USE: Extension functions for IntelliJ API wrappers
fun VirtualFile.isMarkdown(): Boolean = extension == "md"
fun VirtualFile.isInVault(vault: VaultConfig): Boolean =
    path.startsWith(vault.path)

// USE: Coroutines for async operations
suspend fun IndexService.reindexVault(vault: VaultConfig) {
    withContext(Dispatchers.IO) {
        val files = vault.collectMarkdownFiles()
        files.forEach { file ->
            ensureActive() // Check cancellation
            indexFile(file)
        }
    }
}

// NEVER: Blocking calls on EDT
// NEVER: Thread.sleep() or busy-waiting
// NEVER: Raw Thread creation (use coroutines)
// NEVER: Mutable global state (use services)
```

### Naming Conventions

| Element | Convention | Example |
|---------|-----------|---------|
| Services | `*Service` suffix | `IndexService`, `BridgeService` |
| Tool windows | `*ToolWindow` suffix | `BacklinksToolWindow` |
| Actions | `*Action` suffix | `SearchAction`, `DailyNoteAction` |
| Parsers | `*Parser` suffix | `WikiLinkParser`, `FrontmatterParser` |
| Test classes | `*Test` suffix | `LinkResolverTest` |
| Test fixtures | `fixtures/` directory | `fixtures/test-vault/` |
| Constants | UPPER_SNAKE in companion | `MAX_SEARCH_RESULTS` |

### IntelliJ-Specific Patterns

```kotlin
// Service registration — use lightweight services
@Service(Service.Level.PROJECT)
class VaultManager(private val project: Project) {
    // Project-level service: one instance per open project
}

@Service(Service.Level.APP)
class ObsidianBridgeSettings : PersistentStateComponent<SettingsState> {
    // Application-level service: shared across all projects
}

// File-based index — for persistent indexes
class WikiLinkIndex : FileBasedIndexExtension<String, List<WikiLink>>() {
    override fun getName() = ID
    override fun getIndexer() = DataIndexer<String, List<WikiLink>, FileContent> { fileContent ->
        // Parse wiki-links from file content
        WikiLinkParser.parse(fileContent.contentAsText.toString())
            .groupBy { it.target }
    }
    override fun getKeyDescriptor() = EnumeratorStringDescriptor.INSTANCE
    override fun getVersion() = 1
    override fun getInputFilter() = DefaultFileTypeSpecificInputFilter(MarkdownFileType.INSTANCE)

    companion object {
        val ID = ID.create<String, List<WikiLink>>("obsidian.bridge.wikilinks")
    }
}

// Completion contributor — for [[wiki-link]] autocompletion
class WikiLinkCompletionContributor : CompletionContributor() {
    init {
        extend(
            CompletionType.BASIC,
            PlatformPatterns.psiElement(),
            WikiLinkCompletionProvider()
        )
    }
}

// Tool window factory
class BacklinksToolWindowFactory : ToolWindowFactory {
    override fun createToolWindowContent(project: Project, toolWindow: ToolWindow) {
        val panel = BacklinksPanel(project)
        val content = ContentFactory.getInstance().createContent(panel, "", false)
        toolWindow.contentManager.addContent(content)
    }
}
```

---

## Wiki-Link Resolution Algorithm

This is the most critical algorithm in the plugin. Follow Obsidian's behavior exactly:

```kotlin
/**
 * Resolves a wiki-link target to a VaultNote.
 *
 * Resolution order (Obsidian-compatible):
 * 1. Exact path match (if target contains '/')
 * 2. Unique filename match (case-insensitive, across entire vault)
 * 3. Shortest path match (if multiple files share the same name)
 * 4. Current directory match (as tiebreaker)
 * 5. Unresolved (if no match found)
 *
 * Edge cases:
 * - Target without .md extension: append .md before matching
 * - Target with alias: strip alias before resolution (split on '|')
 * - Target with heading: strip heading before resolution (split on '#')
 * - Aliases from frontmatter: included in name matching pool
 */
fun resolve(target: String, contextNote: VaultNote?): LinkResolution {
    val cleanTarget = target
        .substringBefore('|')  // Remove alias
        .substringBefore('#')  // Remove heading
        .trim()

    // 1. Exact path match
    if ('/' in cleanTarget) {
        val note = index.findByRelativePath(cleanTarget.ensureMdExtension())
        if (note != null) return LinkResolution.Resolved(note)
    }

    // 2. Find all candidates by name (case-insensitive)
    val candidates = index.findByName(cleanTarget) +
                     index.findByAlias(cleanTarget)

    return when {
        candidates.isEmpty() -> LinkResolution.Unresolved
        candidates.size == 1 -> LinkResolution.Resolved(candidates.single())
        else -> {
            // 3. Shortest path wins
            val shortest = candidates.minByOrNull { it.relativePath.count { c -> c == '/' } }
            // 4. Same directory as context note as tiebreaker
            val sameDir = contextNote?.let { ctx ->
                candidates.find { it.directory == ctx.directory }
            }
            LinkResolution.Resolved(sameDir ?: shortest!!)
        }
    }
}
```

---

## Frontmatter Parsing Rules

```kotlin
/**
 * Parse YAML frontmatter from Markdown content.
 *
 * Rules:
 * - Frontmatter must start on line 1 with exactly "---"
 * - Ends with "---" on its own line
 * - Content between delimiters parsed as YAML
 * - Malformed YAML: log warning, return empty map
 * - No frontmatter: return empty map
 * - Known fields: tags (List<String>), aliases (List<String>),
 *   title (String), date/created (Date), cssclass (String)
 * - Tags can be strings or list of strings
 * - All other fields preserved as-is in Map<String, Any>
 */
```

---

## Testing Guidelines

### Test Structure

```kotlin
class LinkResolverTest {
    // Use IntelliJ's test fixture for realistic environment
    @JvmField @Rule
    val projectRule = ProjectRule()

    private lateinit var resolver: LinkResolver
    private lateinit var testVault: TestVault

    @Before
    fun setup() {
        testVault = TestVault.create(projectRule.project) {
            note("index.md", "# Index\n[[note-a]]\n[[folder/note-b]]")
            note("note-a.md", "# Note A\n[[index]]")
            note("folder/note-b.md", "# Note B")
            note("folder/note-a.md", "# Note A (in folder)")
        }
        resolver = LinkResolver(testVault.index)
    }

    @Test
    fun `resolves unique filename across vault`() {
        val result = resolver.resolve("note-b", contextNote = null)
        assertThat(result).isInstanceOf(LinkResolution.Resolved::class.java)
        assertThat((result as LinkResolution.Resolved).note.relativePath)
            .isEqualTo("folder/note-b.md")
    }

    @Test
    fun `prefers current directory for ambiguous names`() {
        val contextNote = testVault.getNote("folder/note-b.md")
        val result = resolver.resolve("note-a", contextNote = contextNote)
        assertThat((result as LinkResolution.Resolved).note.relativePath)
            .isEqualTo("folder/note-a.md") // Same directory as context
    }

    @Test
    fun `returns Unresolved for non-existent target`() {
        val result = resolver.resolve("does-not-exist", contextNote = null)
        assertThat(result).isEqualTo(LinkResolution.Unresolved)
    }
}
```

### Test Vault Fixtures

Always use the `TestVault` builder pattern (defined in `test/fixtures/TestVault.kt`) to create realistic vault structures for tests. Never use raw file I/O in tests.

### Performance Benchmarks

All performance-critical operations must have JMH benchmarks:

```kotlin
@State(Scope.Benchmark)
class IndexServiceBenchmark {
    private lateinit var vault: TestVault

    @Setup
    fun setup() {
        vault = TestVault.generateLarge(noteCount = 10_000)
    }

    @Benchmark
    fun benchmarkFullIndex(): IndexResult {
        return IndexService().indexVault(vault.config)
    }

    @Benchmark
    fun benchmarkIncrementalUpdate(): IndexResult {
        return IndexService().updateNote(vault.randomNote())
    }
}
```

---

## Plugin.xml Structure

```xml
<idea-plugin>
    <id>dev.htmeta.obsidianbridge</id>
    <name>Obsidian Bridge</name>
    <vendor email="jakub@htmeta.dev" url="https://htmeta.dev">HTMETA</vendor>

    <depends>com.intellij.modules.platform</depends>

    <!-- Services -->
    <extensions defaultExtensionNs="com.intellij">
        <applicationService
            serviceImplementation="dev.htmeta.obsidianbridge.settings.ObsidianBridgeSettings"/>
        <projectService
            serviceImplementation="dev.htmeta.obsidianbridge.core.VaultManager"/>
        <projectService
            serviceImplementation="dev.htmeta.obsidianbridge.core.IndexService"/>
        <projectService
            serviceImplementation="dev.htmeta.obsidianbridge.search.VaultSearchService"/>
        <projectService
            serviceImplementation="dev.htmeta.obsidianbridge.bridge.BridgeService"/>

        <!-- File-based indexes -->
        <fileBasedIndex
            implementation="dev.htmeta.obsidianbridge.core.index.WikiLinkIndex"/>
        <fileBasedIndex
            implementation="dev.htmeta.obsidianbridge.core.index.FrontmatterIndex"/>
        <fileBasedIndex
            implementation="dev.htmeta.obsidianbridge.core.index.TagIndex"/>

        <!-- Editor enhancements -->
        <completion.contributor language="Markdown"
            implementationClass="dev.htmeta.obsidianbridge.ui.editor.WikiLinkCompletionContributor"/>
        <annotator language="Markdown"
            implementationClass="dev.htmeta.obsidianbridge.ui.editor.WikiLinkAnnotator"/>
        <gotoDeclarationHandler
            implementation="dev.htmeta.obsidianbridge.ui.editor.WikiLinkGotoHandler"/>
        <codeInsight.lineMarkerProvider language="Markdown"
            implementationClass="dev.htmeta.obsidianbridge.ui.editor.BacklinkGutterProvider"/>
        <documentationProvider
            implementation="dev.htmeta.obsidianbridge.ui.editor.WikiLinkDocProvider"/>

        <!-- Tool windows -->
        <toolWindow id="Backlinks" anchor="right" secondary="true"
            factoryClass="dev.htmeta.obsidianbridge.ui.toolwindow.BacklinksToolWindowFactory"
            icon="/icons/backlinks.svg"/>
        <toolWindow id="Tags" anchor="left" secondary="true"
            factoryClass="dev.htmeta.obsidianbridge.ui.toolwindow.TagTreeToolWindowFactory"
            icon="/icons/tags.svg"/>
        <toolWindow id="Graph" anchor="bottom"
            factoryClass="dev.htmeta.obsidianbridge.ui.toolwindow.GraphToolWindowFactory"
            icon="/icons/graph.svg"/>

        <!-- Settings -->
        <applicationConfigurable
            instance="dev.htmeta.obsidianbridge.ui.settings.ObsidianBridgeConfigurable"
            id="obsidian.bridge.settings" displayName="Obsidian Bridge" parentId="tools"/>

        <!-- VFS listener -->
        <vfs.asyncListener
            implementation="dev.htmeta.obsidianbridge.core.VaultFileListener"/>
    </extensions>

    <!-- Actions -->
    <actions>
        <group id="ObsidianBridge.MainMenu" text="Obsidian Bridge" popup="true">
            <add-to-group group-id="ToolsMenu" anchor="last"/>
            <action id="obsidian.bridge.search" class="...SearchAction"
                text="Search Vault" icon="/icons/search.svg">
                <keyboard-shortcut first-keystroke="ctrl alt O" keymap="$default"/>
                <keyboard-shortcut first-keystroke="meta alt O" keymap="Mac OS X 10.5+"/>
            </action>
            <!-- ... more actions -->
        </group>
    </actions>

    <!-- Extension points for third-party plugins -->
    <extensionPoints>
        <extensionPoint name="linkResolver" interface="...ObsidianLinkResolver"/>
        <extensionPoint name="frontmatterProcessor" interface="...FrontmatterProcessor"/>
        <extensionPoint name="bridgeMarkerProvider" interface="...BridgeMarkerProvider"/>
    </extensionPoints>
</idea-plugin>
```

---

## Common Pitfalls (DO NOT)

1. **DO NOT** read files on EDT. Always use `ReadAction.nonBlocking()` or coroutines with `Dispatchers.IO`.
2. **DO NOT** use `java.io.File` directly. Use IntelliJ's `VirtualFile` and `VirtualFileManager`.
3. **DO NOT** store absolute paths in persistent state. Store vault-relative paths.
4. **DO NOT** parse Markdown with regex for complex patterns. Use the dedicated `WikiLinkParser` which handles edge cases (nested brackets, code blocks, escaped links).
5. **DO NOT** modify vault files without explicit user action. The plugin is read-only by default.
6. **DO NOT** assume single-vault. All services must accept `vaultId` parameter.
7. **DO NOT** use `println` or `System.out`. Use `com.intellij.openapi.diagnostic.Logger`.
8. **DO NOT** throw exceptions from index operations. Catch, log, and return empty/default.
9. **DO NOT** create heavyweight services. Use `@Service(Service.Level.PROJECT)` for project-scoped state and `@Service(Service.Level.APP)` for global settings only.
10. **DO NOT** hardcode keyboard shortcuts. Always use `plugin.xml` action declarations.

---

## Definition of Done

A feature is complete when:

- [ ] Implementation follows architecture rules above
- [ ] Unit tests cover happy path + edge cases (≥80% branch coverage)
- [ ] Integration test verifies end-to-end behavior in IDE environment
- [ ] Performance benchmark exists for any operation touching index or search
- [ ] `plugin.xml` updated if new extension points, services, or actions added
- [ ] No EDT blocking (verified with `ThreadingAssertions`)
- [ ] Error handling: malformed input doesn't crash, warnings logged
- [ ] Settings exposed in UI if feature is user-configurable
- [ ] Changelog updated
- [ ] PR description references SRS requirement ID (e.g., "Implements FR-200")
