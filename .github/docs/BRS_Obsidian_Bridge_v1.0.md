# Business Requirements Specification (BRS)

## Obsidian Bridge for JetBrains

**Version:** 1.0  
**Date:** 2026-02-21  
**Author:** Jakub — HTMETA.dev  
**Status:** Draft  
**Classification:** Confidential

---

## 1. Executive Summary

Obsidian Bridge for JetBrains is a premium IDE plugin that brings Obsidian vault functionality directly into JetBrains IDEs (IntelliJ IDEA, WebStorm, PyCharm, CLion, Rider, GoLand, PhpStorm, RubyMine). The plugin targets a validated gap in the developer tooling market: the disconnect between knowledge management (Obsidian) and code development (JetBrains). No production-grade solution currently exists.

The plugin enables developers to navigate `[[wiki-links]]`, view backlinks, parse YAML frontmatter, search vault metadata, and bridge TODO/FIXME comments in code with Obsidian notes — all without leaving the IDE.

**Revenue Target:** €100k+ MRR within 18 months via freemium model on JetBrains Marketplace.

---

## 2. Business Context

### 2.1 Market Opportunity

- **Obsidian users:** 1M+ active users (2025), growing ~40% YoY. Strong developer demographic.
- **JetBrains IDEs:** 12M+ developers across all IDEs (JetBrains 2024 report).
- **Overlap segment:** Estimated 500k–1.5M developers who actively use both Obsidian and a JetBrains IDE.
- **JetBrains Marketplace:** Proven distribution channel. Top plugins generate $50k–$500k+/year. Marketplace handles licensing, distribution, and payments.

### 2.2 Competitive Landscape

| Solution | Status | Gaps |
|----------|--------|------|
| Native JetBrains Markdown | Built-in | No wiki-links, no backlinks, no vault awareness |
| Markdown Navigator Enhanced | Active plugin | Obsidian-agnostic, no frontmatter, no graph |
| VS Code Foam / Dendron | VS Code only | Not available for JetBrains ecosystem |
| Obsidian Git plugin | Obsidian-side only | No IDE integration |

**Key insight:** There is zero competition in this specific niche. First-mover advantage is significant because JetBrains plugin ecosystems create strong lock-in through user habits and workflows.

### 2.3 Target Personas

**Persona 1: "The Knowledge-Driven Developer"**
- Senior/Staff engineer, 5–15 years experience
- Uses Obsidian for architecture decisions, meeting notes, learning logs
- Frustrated by context-switching between IDE and Obsidian
- Willing to pay €5–10/month for seamless integration

**Persona 2: "The Technical Lead"**
- Manages team knowledge, onboarding docs, runbooks
- Needs to link code artifacts to documentation
- Values traceability between code and decisions
- Budget authority for team licenses (€8–15/user/month)

**Persona 3: "The Solo Builder / Indie Dev"**
- Uses Obsidian as a second brain for projects
- Wants TODO ↔ notes bridging
- Price-sensitive but loyal to good tools
- Free tier user, potential conversion to Pro

### 2.4 Value Proposition

**For individual developers:** Stop context-switching. Your knowledge base lives where your code lives.

**For teams:** Traceability between code decisions and documentation. Onboarding becomes self-documenting.

**For organizations:** Reduce knowledge silos. Bridge tribal knowledge in Obsidian vaults with active codebases.

---

## 3. Business Requirements

### BR-001: Obsidian Vault Integration
**Priority:** Critical  
The system shall allow users to register one or more Obsidian vaults as knowledge sources within any JetBrains IDE. The vault path configuration shall persist across IDE restarts and project switches.

### BR-002: Wiki-Link Navigation
**Priority:** Critical  
The system shall recognize `[[wiki-links]]` and `[[link|alias]]` syntax in any Markdown file within the IDE and provide click-to-navigate functionality, consistent with Obsidian's link resolution rules.

### BR-003: Backlinks Discovery
**Priority:** Critical  
The system shall provide a tool window showing all notes that reference the currently open note, enabling reverse navigation and knowledge graph traversal.

### BR-004: YAML Frontmatter Support
**Priority:** High  
The system shall parse YAML frontmatter in Markdown files and expose metadata (tags, aliases, dates, custom properties) for search, filtering, and display.

### BR-005: Vault-Wide Search
**Priority:** High  
The system shall provide full-text search across the entire Obsidian vault with filtering by tags, frontmatter properties, and file metadata. Search shall be accessible via IDE action (Cmd/Ctrl+Shift+O equivalent).

### BR-006: Code ↔ Notes Bridging
**Priority:** High  
The system shall detect TODO, FIXME, HACK, and NOTE comments in source code and allow users to create linked Obsidian notes from these comments, with bidirectional references.

### BR-007: Graph View
**Priority:** Medium  
The system shall provide a simplified interactive graph visualization of vault note connections, rendered within a JetBrains tool window.

### BR-008: Tag Navigation
**Priority:** Medium  
The system shall index all Obsidian tags (#tag and nested #tag/subtag) and provide a navigable tag tree with note counts.

### BR-009: Daily Notes Integration
**Priority:** Medium  
The system shall support Obsidian daily notes conventions, providing quick-create and quick-navigate for today's daily note.

### BR-010: Template Support
**Priority:** Low  
The system shall support Obsidian-compatible templates for new note creation, including variable substitution ({{date}}, {{title}}, etc.).

### BR-011: Multi-Vault Support
**Priority:** Low  
The system shall support multiple simultaneous vault registrations with independent indexing and configuration per vault.

### BR-012: Cross-Platform Compatibility
**Priority:** Critical  
The system shall function identically on macOS, Windows, and Linux across all JetBrains IDEs based on the IntelliJ Platform (2023.3+).

---

## 4. Revenue Model

### 4.1 Pricing Strategy (Freemium)

| Tier | Price | Features |
|------|-------|----------|
| **Free** | €0 | Wiki-link navigation, basic backlinks, single vault, vault search |
| **Pro** | €5.90/month or €49/year | Full backlinks, graph view, code↔notes bridge, tag tree, daily notes, templates, multi-vault |
| **Team** | €11.90/user/month or €99/user/year | Pro features + shared vault config, team templates, priority support |

### 4.2 Revenue Projections

| Metric | Month 6 | Month 12 | Month 18 |
|--------|---------|----------|----------|
| Free installs | 5,000 | 20,000 | 50,000 |
| Pro conversions (5%) | 250 | 1,000 | 2,500 |
| Team licenses | 20 | 100 | 300 |
| MRR | €1,710 | €8,090 | €18,320 |
| ARR | €20,520 | €97,080 | €219,840 |

**Note:** Conservative estimates. JetBrains Marketplace takes 0% commission for the first year and a flat fee thereafter. Revenue projections assume organic growth from Marketplace discovery + targeted content marketing.

### 4.3 Distribution Channels

1. **JetBrains Marketplace** — Primary distribution. Handles licensing, payments, updates.
2. **Obsidian Community** — Forum posts, Discord engagement, plugin directory cross-promotion.
3. **Content Marketing** — Blog posts on thinkdifferent.blog, YouTube demos, conference talks.
4. **GitHub** — Open-source Free tier to build trust and community contributions.

---

## 5. Success Metrics (KPIs)

| KPI | Target (12 months) |
|-----|---------------------|
| Total installs | 20,000+ |
| Monthly Active Users (MAU) | 8,000+ |
| Free → Pro conversion rate | ≥5% |
| Pro churn rate | <3% monthly |
| NPS score | ≥50 |
| JetBrains Marketplace rating | ≥4.5/5 |
| Support ticket resolution time | <24h |
| ARR | €100k+ |

---

## 6. Risks and Mitigations

| Risk | Impact | Probability | Mitigation |
|------|--------|-------------|------------|
| JetBrains changes plugin API | High | Low | Abstract platform layer, follow EAP releases |
| Obsidian changes vault format | Medium | Low | Parser abstraction layer, vault format versioning |
| VS Code competitor emerges | Medium | Medium | First-mover advantage, deep JetBrains integration |
| Low conversion rate | High | Medium | A/B test paywall placement, optimize free→pro funnel |
| Performance issues with large vaults | High | Medium | Async indexing, incremental updates, file watchers |

---

## 7. Constraints

- **Technology:** Must use Kotlin/JVM (JetBrains plugin SDK requirement).
- **Compatibility:** IntelliJ Platform 2023.3+ (covers ~90% of active installs).
- **Licensing:** JetBrains Marketplace licensing framework (annual/monthly subscriptions).
- **Data:** Plugin operates entirely locally. No cloud services, no data collection. Vault data never leaves the user's machine.
- **Performance:** Index operation must complete within 5 seconds for vaults up to 10,000 notes.

---

## 8. Stakeholders

| Role | Responsibility |
|------|----------------|
| Product Owner (Jakub) | Vision, roadmap, business decisions |
| Lead Developer (Jakub) | Architecture, core implementation |
| Community | Beta testing, feature requests, bug reports |
| JetBrains | Marketplace review, plugin API support |
| Obsidian Community | Validation, adoption, feedback |

---

## 9. Approval

| Name | Role | Date | Signature |
|------|------|------|-----------|
| Jakub | Product Owner & CTO | 2026-02-21 | _______________ |

---

*Document Control: This BRS is a living document. Changes require Product Owner approval and version increment.*
