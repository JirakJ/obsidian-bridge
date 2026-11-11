# Obsidian Bridge for JetBrains

[![Version](https://img.shields.io/badge/version-2026.9.0-blue)](https://plugins.jetbrains.com/plugin/30304)
[![License](https://img.shields.io/badge/license-Apache--2.0-blue)](LICENSE)

Work with an Obsidian vault from JetBrains IDEs.

## Features

- **Vault index** — Markdown notes of the configured vault are indexed on project open and kept up to date as files are created, edited, renamed, moved or deleted (`.obsidian/` and `.trash/` are skipped).
- **`[[wiki-link]]` completion** — note titles are suggested after `[[` in any file.
- **Code comment links** — a gutter icon on code comments containing `[[Note]]` opens the linked note (can be turned off in Settings).
- **Backlinks** tool window — notes that link to the note open in the editor; double-click to open one.
- **Tags** tool window — tags from frontmatter and inline `#tags`, nested by `/`, with note counts.
- **Graph** tool window — notes and resolved links; click a node to open the note.
- **Search Vault** (Ctrl/Cmd+Alt+O) — full-text search over titles, aliases, tags, paths and note content.
- **Open Daily Note** (Ctrl/Cmd+Alt+D), **New Note** (Ctrl/Cmd+Alt+N), **Re-index Vault**, **Vault Diagnostics** under *Tools → Obsidian IDE Bridge*.

Links resolve like in Obsidian: explicit paths first, then note name or alias; for duplicate names the note in the same folder wins, otherwise the shallowest path.

## Configuration

*Settings → Tools → Obsidian IDE Bridge*: vault path (must contain `.obsidian/`), display name and the gutter-icon toggle. Additional vaults can be added to `obsidian-bridge.xml` in the IDE config directory; they are indexed too.

## Building

```bash
./gradlew buildPlugin     # plugin ZIP in build/distributions/
./gradlew detekt test     # static analysis + unit tests
```

## Free & open source

As of version 2026.8.0 this plugin is completely free — every feature is unlocked and no license is needed. The source code is available at https://github.com/JirakJ/obsidian-bridge under the Apache License 2.0.

## Contributing

Issues and pull requests are welcome — please open them on GitHub.

## License

Copyright 2026 Jakub Jirák. Licensed under the [Apache License, Version 2.0](LICENSE).
