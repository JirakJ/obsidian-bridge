# Software Requirements Specification (SRS)

## Obsidian Bridge for JetBrains

**Version:** 1.0  
**Date:** 2026-02-21  
**Author:** Jakub — HTMETA.dev  
**Status:** Draft  
**Classification:** Confidential  
**Based on:** BRS v1.0

---

## 1. Introduction

### 1.1 Purpose

This SRS defines the complete technical requirements for Obsidian Bridge for JetBrains — a JetBrains IDE plugin that integrates Obsidian vault functionality into the IntelliJ Platform. It serves as the authoritative reference for implementation, testing, and acceptance criteria.

### 1.2 Scope

The plugin operates as a local-only JetBrains extension. It reads and indexes Obsidian vault directories, provides navigation and search capabilities, and bridges code artifacts with knowledge base notes. No network connectivity is required or used for core functionality.

### 1.3 Definitions

| Term | Definition |
|------|------------|
| Vault | A directory containing Obsidian Markdown files and `.obsidian/` config |
| Wiki-link | `[[target]]` or `[[target\|alias]]` syntax for inter-note references |
| Frontmatter | YAML metadata block at the top of a Markdown file (between `---` delimiters) |
| Backlink | A reverse reference — note B is a backlink of note A if B contains `[[A]]` |
| Graph | A network visualization of notes (nodes) and their links (edges) |
| PSI | Program Structure Interface — JetBrains' AST representation |
| VFS | Virtual File System — JetBrains' file system abstraction |

### 1.4 Technology Stack

| Component | Technology | Rationale |
|-----------|------------|-----------|
| Language | Kotlin 1.9+ | JetBrains SDK standard, null-safe, coroutines |
| Build | Gradle 8.x + IntelliJ Gradle Plugin | Standard JetBrains plugin build system |
| Platform | IntelliJ Platform SDK 2023.3+ | Covers ~90% active installs |
| Parser | Custom Markdown+Wiki-link parser | Obsidian-specific syntax not in standard parsers |
| YAML | SnakeYAML 2.x | Standard JVM YAML parser, bundled with IntelliJ |
| Index | Custom inverted index + IntelliJ FileBasedIndex | Fast search, persists across restarts |
| Graph | JCEF (Chromium Embedded) + D3.js | Rich interactive visualization in tool window |
| Testing | JUnit 5 + IntelliJ Test Framework | Platform-standard testing |
| CI/CD | GitHub Actions | Build, test, publish to Marketplace |

---

## 2. System Architecture

### 2.1 High-Level Architecture

```
┌─────────────────────────────────────────────────────────┐
│                    JetBrains IDE                         │
│                                                         │
│  ┌──────────────┐  ┌──────────────┐  ┌──────────────┐  │
│  │  Editor       │  │  Tool        │  │  Settings    │  │
│  │  Extensions   │  │  Windows     │  │  Panel       │  │
│  │              │  │              │  │              │  │
│  │ • Link refs  │  │ • Backlinks  │  │ • Vault path │  │
│  │ • Gutter     │  │ • Tag Tree   │  │ • Features   │  │
│  │   icons      │  │ • Graph      │  │ • Shortcuts  │  │
│  │ • Completion │  │ • Search     │  │ • Indexing   │  │
│  └──────┬───────┘  └──────┬───────┘  └──────┬───────┘  │
│         │                 │                  │          │
│  ┌──────┴─────────────────┴──────────────────┴───────┐  │
│  │              Plugin Core Services                  │  │
│  │                                                    │  │
│  │  ┌────────────┐ ┌────────────┐ ┌────────────────┐ │  │
│  │  │ Vault      │ │ Index      │ │ Link           │ │  │
│  │  │ Manager    │ │ Service    │ │ Resolver       │ │  │
│  │  └────────────┘ └────────────┘ └────────────────┘ │  │
│  │  ┌────────────┐ ┌────────────┐ ┌────────────────┐ │  │
│  │  │ Frontmatter│ │ Tag        │ │ Bridge         │ │  │
│  │  │ Parser     │ │ Indexer    │ │ Service        │ │  │
│  │  └────────────┘ └────────────┘ └────────────────┘ │  │
│  │  ┌────────────┐ ┌────────────┐                    │  │
│  │  │ Template   │ │ Graph      │                    │  │
│  │  │ Engine     │ │ Builder    │                    │  │
│  │  └────────────┘ └────────────┘                    │  │
│  └────────────────────────┬───────────────────────────┘  │
│                           │                              │
│  ┌────────────────────────┴───────────────────────────┐  │
│  │              File System Layer                      │  │
│  │  VFS Listener → File Watcher → Incremental Index   │  │
│  └────────────────────────┬───────────────────────────┘  │
│                           │                              │
└───────────────────────────┼──────────────────────────────┘
                            │
                   ┌────────┴────────┐
                   │  Obsidian Vault │
                   │  (Local FS)     │
                   │  ├── .obsidian/ │
                   │  ├── notes/     │
                   │  ├── templates/ │
                   │  └── assets/    │
                   └─────────────────┘
```

### 2.2 Component Descriptions

**VaultManager** — Manages vault registration, validation, and lifecycle. Detects `.obsidian/` directory to confirm valid vault. Handles multi-vault scenarios.

**IndexService** — Builds and maintains the inverted index of all vault notes. Uses IntelliJ's `FileBasedIndex` for persistence. Supports incremental updates via VFS listeners.

**LinkResolver** — Resolves `[[wiki-links]]` to actual files following Obsidian's resolution algorithm: shortest path match, then unique filename match, then relative path.

**FrontmatterParser** — Extracts and caches YAML frontmatter from all vault notes. Provides typed access to common fields (tags, aliases, date, custom).

**TagIndexer** — Maintains a hierarchical tag index from both frontmatter `tags:` field and inline `#tag` syntax. Supports nested tags.

**BridgeService** — Connects code comments (TODO/FIXME/HACK/NOTE) with Obsidian notes. Creates and maintains bidirectional references.

**TemplateEngine** — Processes Obsidian-compatible templates with variable substitution for new note creation.

**GraphBuilder** — Constructs the note relationship graph data structure and feeds it to the D3.js visualization layer.

### 2.3 Data Flow

```
File Change (VFS Event)
    │
    ├── Is Markdown? ──→ Re-parse frontmatter, links, tags
    │                        │
    │                        ├── Update IndexService
    │                        ├── Update LinkResolver cache
    │                        ├── Update TagIndexer
    │                        └── Notify UI components
    │
    └── Is Source Code? ──→ Scan for TODO/FIXME markers
                                │
                                └── Update BridgeService references
```

---

## 3. Functional Requirements

### 3.1 Vault Management

#### FR-100: Vault Registration
**Priority:** Critical | **Traces to:** BR-001

The system shall provide a Settings panel (Settings → Tools → Obsidian Bridge) where users can:
- Add vault paths via directory chooser or manual input
- Validate that the selected directory contains `.obsidian/` subdirectory
- Display vault statistics (note count, tag count, link count) after indexing
- Remove registered vaults

**Acceptance Criteria:**
- AC-100-1: Adding a valid vault triggers initial indexing within 2 seconds for vaults ≤1,000 notes
- AC-100-2: Adding a directory without `.obsidian/` shows error: "Not a valid Obsidian vault. Missing .obsidian directory."
- AC-100-3: Vault path persists in IDE settings across restarts
- AC-100-4: Invalid/moved vault paths show warning icon with "Vault not found" tooltip

#### FR-101: Vault Indexing
**Priority:** Critical | **Traces to:** BR-001

The system shall build a complete index of all `.md` files in the vault on registration and maintain it incrementally via file system watchers.

**Acceptance Criteria:**
- AC-101-1: Initial index of 10,000 notes completes in ≤5 seconds
- AC-101-2: Incremental update for single file change applies in ≤100ms
- AC-101-3: Index persists across IDE restarts (no re-scan on startup for unchanged vaults)
- AC-101-4: Files matching `.obsidian/`, `.trash/`, and user-configured exclude patterns are skipped
- AC-101-5: Progress indicator shown during initial indexing

#### FR-102: Vault File Watcher
**Priority:** Critical | **Traces to:** BR-001

The system shall use JetBrains VFS (VirtualFileListener) to detect file creation, modification, deletion, and rename events within registered vaults.

**Acceptance Criteria:**
- AC-102-1: New `.md` file detected and indexed within 500ms
- AC-102-2: Deleted file removed from index within 500ms
- AC-102-3: Renamed file updates all referencing backlinks in the index
- AC-102-4: Bulk operations (e.g., git pull with 100+ changes) processed in batch mode

### 3.2 Wiki-Link Navigation

#### FR-200: Link Recognition
**Priority:** Critical | **Traces to:** BR-002

The system shall recognize the following wiki-link patterns in `.md` files:

| Pattern | Example | Behavior |
|---------|---------|----------|
| Basic link | `[[note-name]]` | Navigate to note-name.md |
| Aliased link | `[[note-name\|Display Text]]` | Navigate to note-name.md, show "Display Text" |
| Heading link | `[[note-name#heading]]` | Navigate to note-name.md, scroll to heading |
| Block link | `[[note-name#^block-id]]` | Navigate to note-name.md, scroll to block |
| Embedded | `![[note-name]]` | Show inline preview of note content |
| Image embed | `![[image.png]]` | Show inline image preview |

**Acceptance Criteria:**
- AC-200-1: All patterns above are highlighted as clickable references in the editor
- AC-200-2: Ctrl+Click (Cmd+Click on macOS) navigates to the target
- AC-200-3: Ctrl+Hover shows preview popup of the target note (first 10 lines)
- AC-200-4: Unresolved links shown with warning color (configurable)

#### FR-201: Link Resolution Algorithm
**Priority:** Critical | **Traces to:** BR-002

The system shall resolve wiki-links using Obsidian's algorithm:

```
1. Exact path match (if link contains /)
2. Unique filename match across vault (case-insensitive)
3. Shortest path match if multiple files share the same name
4. Current directory match as tiebreaker
5. If unresolved → mark as broken link
```

**Acceptance Criteria:**
- AC-201-1: Resolution matches Obsidian's behavior for 99%+ of common link patterns
- AC-201-2: Resolution result cached; invalidated only on relevant file changes
- AC-201-3: Performance: resolution of single link ≤1ms

#### FR-202: Link Autocompletion
**Priority:** High | **Traces to:** BR-002

The system shall provide autocompletion when the user types `[[` in a Markdown file:

- Show all vault note names as completion candidates
- Filter as user types
- Show file path and first-line preview in completion popup
- Support heading completion after `#` within a link
- Support alias syntax with `|`

**Acceptance Criteria:**
- AC-202-1: Completion popup appears within 100ms of typing `[[`
- AC-202-2: Popup shows ≤20 results with real-time filtering
- AC-202-3: Tab/Enter completes the link with closing `]]`
- AC-202-4: Completion respects configured vault scope (multi-vault: show vault prefix)

### 3.3 Backlinks

#### FR-300: Backlinks Panel
**Priority:** Critical | **Traces to:** BR-003

The system shall provide a "Backlinks" tool window that displays all notes referencing the currently active note.

**Display format per backlink:**
```
📄 Note Title
   path/to/note.md
   "...context text around the [[link]]..."
```

**Acceptance Criteria:**
- AC-300-1: Backlinks panel updates within 200ms when active editor changes
- AC-300-2: Each backlink entry is clickable, navigating to the referencing note
- AC-300-3: Context shows ±50 characters around the link with the link highlighted
- AC-300-4: Backlink count shown in panel title: "Backlinks (7)"
- AC-300-5: Empty state: "No backlinks found for this note"

#### FR-301: Inline Backlink Hints
**Priority:** Medium | **Traces to:** BR-003

The system shall show a gutter icon (🔗) on line 1 of each Markdown file indicating backlink count. Clicking the icon opens the Backlinks panel.

### 3.4 Frontmatter Support

#### FR-400: Frontmatter Parsing
**Priority:** High | **Traces to:** BR-004

The system shall parse YAML frontmatter (between `---` delimiters) from all vault `.md` files and expose:

| Field | Type | Usage |
|-------|------|-------|
| `tags` | String[] | Tag indexing, search filtering |
| `aliases` | String[] | Alternative names for link resolution |
| `date` / `created` | Date | Sort, filter, display |
| `title` | String | Display name override |
| Custom fields | Any | Search, filter, display in note info popup |

**Acceptance Criteria:**
- AC-400-1: Frontmatter parsed using SnakeYAML with error tolerance (malformed YAML doesn't crash indexer)
- AC-400-2: `aliases` field values added to link resolution candidates
- AC-400-3: Frontmatter changes detected via VFS listener and index updated incrementally
- AC-400-4: Frontmatter preview shown in note info popup (Ctrl+Hover on wiki-link)

#### FR-401: Frontmatter Editor
**Priority:** Low | **Traces to:** BR-004

The system shall provide syntax highlighting and autocompletion for known frontmatter fields when editing the YAML block.

### 3.5 Vault Search

#### FR-500: Omnibar Search
**Priority:** High | **Traces to:** BR-005

The system shall register a custom action (default: Ctrl+Alt+O / Cmd+Alt+O) that opens a search dialog with:

- Full-text search across all vault notes
- Filter chips: `tag:`, `has:frontmatter`, `path:`, `created:`, `modified:`
- Results sorted by relevance (BM25 scoring)
- Keyboard navigation with preview panel

**Acceptance Criteria:**
- AC-500-1: Search results appear within 200ms for vaults ≤10,000 notes
- AC-500-2: Results show note title, path, and matching snippet
- AC-500-3: Enter opens selected note in editor
- AC-500-4: Search indexes note content, frontmatter values, and tags

#### FR-501: Search Scope Configuration
**Priority:** Medium | **Traces to:** BR-005

Users can configure search scope: specific vaults, specific folders, or specific tags.

### 3.6 Code ↔ Notes Bridge

#### FR-600: TODO Detection
**Priority:** High | **Traces to:** BR-006

The system shall scan source code files for comment markers matching configurable patterns:

```
Default patterns:
// TODO: <text>
// FIXME: <text>
// HACK: <text>
// NOTE: <text>
/* TODO: <text> */
# TODO: <text>
```

**Acceptance Criteria:**
- AC-600-1: Markers detected in real-time as user types
- AC-600-2: Gutter icon (📝) shown next to detected markers
- AC-600-3: Patterns configurable in Settings (regex)
- AC-600-4: Languages supported: Java, Kotlin, Python, JavaScript, TypeScript, Go, Rust, C/C++, Ruby, PHP, Swift

#### FR-601: Note Creation from Code
**Priority:** High | **Traces to:** BR-006

Clicking the gutter icon or using intention action (Alt+Enter) on a TODO comment shall offer "Create Obsidian Note":

- Opens a dialog with pre-filled title (from comment text) and configurable vault/folder
- Created note contains backlink to source file: `Source: [[project/src/File.java#L42]]`
- Original comment updated with note reference: `// TODO: fix auth flow → [[todo-fix-auth-flow]]`

**Acceptance Criteria:**
- AC-601-1: Note created in configured vault/folder
- AC-601-2: Note uses configured template (if set)
- AC-601-3: Bidirectional link established and indexed

#### FR-602: Note Reference in Code
**Priority:** Medium | **Traces to:** BR-006

The system shall recognize `→ [[note-name]]` patterns in code comments and provide:
- Ctrl+Click navigation to the referenced note
- Hover preview of note content
- Warning if referenced note doesn't exist

### 3.7 Graph View

#### FR-700: Graph Visualization
**Priority:** Medium | **Traces to:** BR-007

The system shall provide an interactive graph view in a dedicated tool window:

- Nodes = notes (sized by link count)
- Edges = wiki-link connections (directed)
- Color coding by folder or tag (configurable)
- Force-directed layout (D3.js force simulation)
- Zoom, pan, node drag
- Click node → open note in editor
- Hover node → show note title and backlink count

**Implementation:** JCEF (JetBrains Chromium Embedded Framework) rendering D3.js graph.

**Acceptance Criteria:**
- AC-700-1: Graph renders within 2 seconds for vaults ≤5,000 notes
- AC-700-2: Smooth 60fps interaction for vaults ≤1,000 notes
- AC-700-3: "Local graph" mode shows only notes within N links of current note (default: 2)
- AC-700-4: Graph data updates in real-time as notes are edited

### 3.8 Tag Navigation

#### FR-800: Tag Tree Panel
**Priority:** Medium | **Traces to:** BR-008

The system shall provide a "Tags" tool window displaying a hierarchical tree of all tags:

```
📁 Tags (142)
├── #project (23)
│   ├── #project/backend (12)
│   └── #project/frontend (11)
├── #status (45)
│   ├── #status/active (18)
│   ├── #status/done (22)
│   └── #status/blocked (5)
└── #type (34)
    ├── #type/meeting (15)
    └── #type/decision (19)
```

**Acceptance Criteria:**
- AC-800-1: Tree updates in real-time as tags are added/removed
- AC-800-2: Click on tag shows all notes with that tag in a list
- AC-800-3: Tag counts include both frontmatter and inline tags
- AC-800-4: Search/filter within tag tree

### 3.9 Daily Notes

#### FR-900: Daily Note Quick Action
**Priority:** Medium | **Traces to:** BR-009

The system shall register an action (default: Ctrl+Alt+D / Cmd+Alt+D) that:
- Opens today's daily note if it exists
- Creates it from template if it doesn't exist
- Respects Obsidian's daily note settings from `.obsidian/daily-notes.json`

**Acceptance Criteria:**
- AC-900-1: Date format configurable (default: YYYY-MM-DD)
- AC-900-2: Target folder configurable (default: `daily/`)
- AC-900-3: Template configurable (default: none)

### 3.10 Templates

#### FR-1000: Template Engine
**Priority:** Low | **Traces to:** BR-010

The system shall support Obsidian-compatible templates with variable substitution:

| Variable | Expansion |
|----------|-----------|
| `{{title}}` | Note filename without extension |
| `{{date}}` | Current date (configurable format) |
| `{{time}}` | Current time (configurable format) |
| `{{date:FORMAT}}` | Date with custom format (Moment.js compatible) |

**Acceptance Criteria:**
- AC-1000-1: Templates loaded from configured template folder
- AC-1000-2: New Note action shows template picker if templates exist
- AC-1000-3: Variables substituted on note creation

---

## 4. Non-Functional Requirements

### 4.1 Performance

| Metric | Target | Measurement |
|--------|--------|-------------|
| Initial vault index (10k notes) | ≤5s | Benchmark test |
| Incremental index update | ≤100ms | VFS event to index update |
| Link resolution | ≤1ms per link | Profiler |
| Search results | ≤200ms | Keystroke to results |
| Backlinks panel update | ≤200ms | Editor switch to panel update |
| Graph render (1k notes) | ≤2s | Tool window open to render |
| IDE startup overhead | ≤500ms | IDE startup profiler |
| Memory footprint (10k notes) | ≤100MB | Heap profiler |

### 4.2 Scalability

- Support vaults up to 50,000 notes without degradation beyond 2x performance targets
- Support individual notes up to 1MB
- Support link counts up to 100,000 per vault
- Graceful degradation for vaults exceeding limits (disable graph, paginate search)

### 4.3 Reliability

- Plugin crash must never crash the host IDE
- Corrupt/malformed Markdown files handled gracefully (skip, log warning)
- Malformed YAML frontmatter handled gracefully (skip frontmatter, parse rest)
- File system permission errors reported to user, not thrown as exceptions

### 4.4 Compatibility

| IDE | Minimum Version | Support Level |
|-----|-----------------|---------------|
| IntelliJ IDEA | 2023.3 | Full |
| WebStorm | 2023.3 | Full |
| PyCharm | 2023.3 | Full |
| CLion | 2023.3 | Full |
| GoLand | 2023.3 | Full |
| PhpStorm | 2023.3 | Full |
| Rider | 2023.3 | Full |
| RubyMine | 2023.3 | Full |
| Android Studio | 2023.3 (Iguana) | Best-effort |

### 4.5 Security

- No network access required or performed
- No data leaves the local machine
- No telemetry collected (optional opt-in analytics for Pro tier)
- Vault paths stored in IDE settings (encrypted on platforms that support it)
- No modification of vault files unless explicitly requested by user

### 4.6 Accessibility

- All UI components support keyboard navigation
- Tool windows compatible with screen readers via IntelliJ accessibility API
- Configurable font sizes in plugin panels
- High-contrast theme support

---

## 5. Data Model

### 5.1 Core Entities

```kotlin
data class VaultConfig(
    val id: String,                    // UUID
    val path: String,                  // Absolute path to vault root
    val name: String,                  // Display name
    val excludePatterns: List<String>, // Glob patterns to exclude
    val templateFolder: String?,       // Path relative to vault root
    val dailyNoteFolder: String?,      // Path relative to vault root
    val dailyNoteFormat: String,       // Date format string
    val isActive: Boolean              // Enabled/disabled toggle
)

data class VaultNote(
    val id: String,                    // SHA-256 of relative path
    val vaultId: String,               // FK to VaultConfig
    val relativePath: String,          // Path relative to vault root
    val title: String,                 // Filename without .md or frontmatter title
    val aliases: List<String>,         // From frontmatter
    val tags: List<String>,            // Merged frontmatter + inline tags
    val frontmatter: Map<String, Any>, // Full parsed YAML
    val outgoingLinks: List<WikiLink>, // Links in this note
    val created: Instant,              // File creation time
    val modified: Instant,             // File modification time
    val contentHash: String            // For change detection
)

data class WikiLink(
    val raw: String,                   // Original text: "[[target|alias]]"
    val target: String,                // Resolved target: "target"
    val alias: String?,                // Display alias
    val heading: String?,              // #heading part
    val blockRef: String?,             // ^block-id part
    val isEmbed: Boolean,              // Starts with !
    val sourceNoteId: String,          // FK to VaultNote
    val sourceLineNumber: Int,         // Line in source file
    val resolvedNoteId: String?        // FK to VaultNote (null if unresolved)
)

data class TagEntry(
    val tag: String,                   // Full tag path: "project/backend"
    val noteIds: Set<String>,          // Notes containing this tag
    val source: TagSource              // FRONTMATTER or INLINE
)

enum class TagSource { FRONTMATTER, INLINE }

data class BridgeEntry(
    val id: String,                    // UUID
    val codeFile: String,              // Absolute path to source file
    val lineNumber: Int,               // Line number in source
    val markerType: String,            // TODO, FIXME, HACK, NOTE
    val text: String,                  // Comment text
    val linkedNoteId: String?,         // FK to VaultNote
    val created: Instant
)
```

### 5.2 Index Structure

```
vault-index/
├── notes.idx          # VaultNote records (binary, mmap'd)
├── links.idx          # WikiLink records with forward/reverse maps
├── tags.idx           # Tag → NoteId set mapping
├── fulltext.idx       # Inverted index for content search
├── names.idx          # Filename/alias → NoteId mapping (for resolution)
└── meta.json          # Vault hash, last indexed timestamp, version
```

The index uses IntelliJ's `FileBasedIndex` API for persistence and IDE-managed lifecycle.

---

## 6. API Specifications

### 6.1 Extension Points

The plugin exposes extension points for third-party plugin integration:

```kotlin
// Extension point: Custom link resolvers
interface ObsidianLinkResolver {
    fun resolve(link: String, context: VaultNote): VaultNote?
    fun priority(): Int // Higher = tried first
}

// Extension point: Custom frontmatter processors
interface FrontmatterProcessor {
    fun process(frontmatter: Map<String, Any>, note: VaultNote)
    fun supportedFields(): List<String>
}

// Extension point: Custom bridge markers
interface BridgeMarkerProvider {
    fun patterns(): List<Regex>
    fun markerType(): String
}

// Service API: Public services for other plugins
interface ObsidianBridgeService {
    fun getVaults(): List<VaultConfig>
    fun getNote(vaultId: String, relativePath: String): VaultNote?
    fun search(query: String, filters: SearchFilters): List<SearchResult>
    fun getBacklinks(noteId: String): List<WikiLink>
    fun getTags(): Map<String, TagEntry>
    fun resolveLink(link: String, contextNote: VaultNote?): VaultNote?
}
```

### 6.2 Actions

| Action ID | Default Shortcut | Description |
|-----------|------------------|-------------|
| `obsidian.bridge.search` | Ctrl+Alt+O | Open vault search dialog |
| `obsidian.bridge.dailyNote` | Ctrl+Alt+D | Open/create today's daily note |
| `obsidian.bridge.newNote` | Ctrl+Alt+N | Create new note (with template) |
| `obsidian.bridge.backlinks` | Ctrl+Alt+B | Toggle backlinks panel |
| `obsidian.bridge.graph` | Ctrl+Alt+G | Toggle graph view |
| `obsidian.bridge.tags` | Ctrl+Alt+T | Toggle tag tree panel |
| `obsidian.bridge.createFromTodo` | Alt+Enter (intention) | Create note from TODO comment |
| `obsidian.bridge.reindex` | — | Force re-index current vault |

### 6.3 Settings Schema

```xml
<!-- plugin.xml registration -->
<applicationConfigurable
    instance="dev.htmeta.obsidianbridge.settings.ObsidianBridgeConfigurable"
    id="obsidian.bridge.settings"
    displayName="Obsidian Bridge"
    parentId="tools"/>

<!-- Persistent state -->
<applicationService
    serviceImplementation="dev.htmeta.obsidianbridge.settings.ObsidianBridgeSettings"
    serviceInterface="dev.htmeta.obsidianbridge.settings.ObsidianBridgeSettings"/>
```

Settings stored via `PersistentStateComponent` in `obsidian-bridge.xml`:

```xml
<application>
  <component name="ObsidianBridgeSettings">
    <vaults>
      <vault id="..." path="/Users/jakub/vault" name="Main Vault" active="true">
        <exclude pattern=".obsidian/**"/>
        <exclude pattern=".trash/**"/>
        <template-folder>templates</template-folder>
        <daily-note-folder>daily</daily-note-folder>
        <daily-note-format>yyyy-MM-dd</daily-note-format>
      </vault>
    </vaults>
    <bridge>
      <marker pattern="TODO|FIXME|HACK|NOTE" enabled="true"/>
      <auto-link enabled="false"/>
    </bridge>
    <ui>
      <graph-depth>2</graph-depth>
      <unresolved-link-color>#FF6B6B</unresolved-link-color>
      <show-gutter-icons>true</show-gutter-icons>
    </ui>
  </component>
</application>
```

---

## 7. UI Specifications

### 7.1 Tool Windows

| Window | Position | Default State | Content |
|--------|----------|---------------|---------|
| Backlinks | Right | Hidden | Backlink list with context |
| Tags | Left (secondary) | Hidden | Hierarchical tag tree |
| Graph | Bottom | Hidden | D3.js force graph |
| Vault Search | Popup (modal) | — | Search input + results |

### 7.2 Editor Enhancements

| Enhancement | Location | Trigger |
|-------------|----------|---------|
| Wiki-link highlighting | Editor | Automatic for .md files |
| Link preview popup | Editor | Ctrl+Hover on wiki-link |
| Autocompletion | Editor | Type `[[` |
| Gutter backlink icon | Gutter | Automatic if backlinks exist |
| Gutter bridge icon | Gutter | TODO/FIXME detected |
| Unresolved link warning | Editor | Automatic for broken links |

### 7.3 Notifications

| Event | Type | Message |
|-------|------|---------|
| Vault indexed | Info balloon | "Obsidian vault indexed: 1,234 notes, 456 tags" |
| Vault not found | Warning | "Vault at /path not found. Check settings." |
| Index error | Error | "Failed to index vault: [reason]" |
| Note created from TODO | Info | "Created note: [[note-name]] linked to File.java:42" |

---

## 8. Testing Requirements

### 8.1 Test Categories

| Category | Count (est.) | Framework |
|----------|-------------|-----------|
| Unit tests | 200+ | JUnit 5 |
| Integration tests | 80+ | IntelliJ Test Framework |
| Performance benchmarks | 20+ | JMH |
| UI tests | 30+ | IntelliJ UI Test Framework |
| Compatibility tests | 9 IDEs × 2 versions | CI matrix |

### 8.2 Critical Test Scenarios

1. **Link Resolution:** 50+ test cases covering all Obsidian resolution edge cases
2. **Large Vault Performance:** 10k, 25k, 50k note benchmarks for all operations
3. **Concurrent Modification:** Multiple files changing simultaneously during indexing
4. **Malformed Input:** Broken YAML, circular links, empty files, binary files in vault
5. **Multi-Vault Conflicts:** Same note name in different vaults
6. **IDE Restart:** Index persistence and recovery after clean/dirty shutdown

### 8.3 Acceptance Test Protocol

Each functional requirement must have:
- ≥1 automated test per acceptance criterion
- Manual QA sign-off for UI-related criteria
- Performance benchmark for performance-related criteria

---

## 9. Deployment

### 9.1 Build Pipeline (GitHub Actions)

```yaml
# .github/workflows/build.yml
triggers: [push to main, PR, release tag]
matrix: [IC-2023.3, IC-2024.1, IC-2024.2, IC-2024.3, IC-2025.1]

steps:
  1. Checkout + setup JDK 17
  2. Gradle build + test
  3. Run IntelliJ Plugin Verifier
  4. Run performance benchmarks
  5. Build plugin ZIP
  6. Publish to JetBrains Marketplace (release tags only)
  7. Create GitHub Release
```

### 9.2 Release Channels

| Channel | Audience | Update Frequency |
|---------|----------|-----------------|
| Stable | All users | Every 2-4 weeks |
| EAP (Beta) | Opt-in testers | Weekly |
| Nightly | Contributors | Daily (automated) |

### 9.3 Plugin Distribution

- **Primary:** JetBrains Marketplace (built-in IDE plugin manager)
- **Secondary:** GitHub Releases (ZIP download for manual install)
- **Free tier:** Open-source on GitHub
- **Pro tier:** Licensed via JetBrains Marketplace subscription

---

## 10. Traceability Matrix

| BRS Req | SRS Req(s) | Priority |
|---------|------------|----------|
| BR-001 | FR-100, FR-101, FR-102 | Critical |
| BR-002 | FR-200, FR-201, FR-202 | Critical |
| BR-003 | FR-300, FR-301 | Critical |
| BR-004 | FR-400, FR-401 | High |
| BR-005 | FR-500, FR-501 | High |
| BR-006 | FR-600, FR-601, FR-602 | High |
| BR-007 | FR-700 | Medium |
| BR-008 | FR-800 | Medium |
| BR-009 | FR-900 | Medium |
| BR-010 | FR-1000 | Low |
| BR-011 | FR-100 (multi-vault) | Low |
| BR-012 | NFR 4.4 Compatibility | Critical |

---

*Document Control: This SRS is a living document. Changes require version increment and updated traceability matrix.*
