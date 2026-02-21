Obsidian Bridge — Freemium plan

Free features:
- Vault indexing and incremental updates
- Wiki-link navigation and backlinks
- Basic graph visualization

Premium features (requires activation / license):
- JCEF graph export and high-quality image export
- Advanced sync and conflict resolution
- Workspace linking across projects

Implementation notes:
- Runtime gating: implement simple feature-flag check for premium features (stub in com.htmeta.obsidianbridge.premium).
- Marketplace: publish free core; set up paid licensing flow and docs.
