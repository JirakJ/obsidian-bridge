# Changelog

## [Unreleased]

### Fixed

- **Backlinks, Tags and Graph tool windows were never populated**: links are now resolved (by path, name or alias), the Backlinks window follows the note open in the editor, the Tags window shows nested tags with counts and the Graph draws edges.
- Wiki-link gutter icons on code comments now open the linked note and honour the "Show gutter icons" setting.
- Search Vault searches note content (previously only titles/tags/paths) and shows a snippet.
- Re-indexing no longer duplicates notes; renamed/moved/deleted files update the index; the index is thread-safe.
- Frontmatter: inline lists (`tags: [a, b]`), quoted values and Windows line endings are parsed correctly.
- Settings: changes are detected properly, the vault path is validated and the vault is re-indexed on apply.
- New Note keeps non-ASCII letters in file names and opens the created note; Diagnostics shows the real plugin version.
- A vault at `/notes` no longer claims files of `/notes2`.

### Removed

- Unimplemented options and code: graph depth, unresolved-link colour, bridge marker patterns, an unused TODO/FIXME bridging service, an unused index cache and three extension points that were never queried. The description no longer advertises templates, D3.js graphs or code↔notes bridging.

## [2026.8.0] - 2026-11-10

### Changed

- The plugin is now completely free — all features unlocked, no license needed.
- Source code is now open source under Apache-2.0: https://github.com/JirakJ/obsidian-bridge

