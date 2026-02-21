# Production Plan

## Obsidian Bridge for JetBrains

**Version:** 1.0  
**Date:** 2026-02-21  
**Author:** Jakub — HTMETA.dev  
**Status:** Draft

---

## 1. Implementation Roadmap

### Phase 0: Foundation (Week 1–2)
**Goal:** Project scaffold, CI/CD pipeline, test infrastructure

| Task | Days | Deliverable |
|------|------|------------|
| Gradle project setup with IntelliJ Plugin SDK | 1 | `build.gradle.kts`, plugin.xml shell |
| CI/CD pipeline (GitHub Actions) | 1 | Build, test, verify across IDE matrix |
| Test infrastructure + TestVault builder | 1 | `TestVault.kt`, JUnit 5 config, fixtures |
| Core data model (Kotlin data classes) | 1 | `model/` package — VaultConfig, VaultNote, WikiLink, TagEntry |
| Settings persistence (PersistentStateComponent) | 1 | Settings panel UI + persistence |
| VFS listener skeleton | 0.5 | VaultFileListener with event routing |
| Logging + error handling framework | 0.5 | Logger utilities, `runCatching` wrappers |

**Milestone:** Plugin loads in IDE, settings panel works, VFS events captured.

---

### Phase 1: Core — Vault + Links (Week 3–5)
**Goal:** Vault indexing, wiki-link navigation, basic backlinks

| Task | Days | Deliverable | SRS Ref |
|------|------|------------|---------|
| VaultManager: registration, validation | 2 | FR-100 complete with tests |
| WikiLinkParser: full Obsidian syntax | 3 | Parser handles all link types (FR-200) |
| FrontmatterParser: YAML extraction | 2 | SnakeYAML integration (FR-400) |
| IndexService: FileBasedIndex implementation | 3 | Forward index: note → links, tags (FR-101) |
| LinkResolver: Obsidian-compatible resolution | 3 | Resolution algorithm with 50+ test cases (FR-201) |
| Editor: wiki-link highlighting + Ctrl+Click | 2 | Annotator + GotoDeclarationHandler (FR-200) |
| Editor: link preview popup (Ctrl+Hover) | 1 | DocumentationProvider (FR-200 AC-3) |
| Backlinks panel (basic) | 2 | Tool window with context snippets (FR-300) |
| Performance benchmarks: indexing | 1 | JMH benchmarks for 1k/10k/50k vaults |

**Milestone:** Users can register a vault, navigate wiki-links with Ctrl+Click, see backlinks. This is the **MVP** for Free tier.

**Checkpoint:** Internal dogfooding begins. Use own Obsidian vault daily in IDE.

---

### Phase 2: Search + Completion (Week 6–7)
**Goal:** Vault search, autocompletion, tag navigation

| Task | Days | Deliverable | SRS Ref |
|------|------|------------|---------|
| Full-text search index (inverted index) | 3 | BM25 scoring, content + metadata search (FR-500) |
| Search dialog UI (popup) | 2 | Modal with filters, keyboard nav, preview (FR-500) |
| Wiki-link autocompletion (`[[`) | 2 | CompletionContributor with fuzzy matching (FR-202) |
| TagExtractor: inline + frontmatter tags | 1 | Nested tag support (#tag/subtag) (FR-800) |
| Tag tree tool window | 2 | Hierarchical tree with counts, click-to-filter (FR-800) |

**Milestone:** Users can search vault, get autocompletion, browse tags. Free tier feature-complete.

---

### Phase 3: Bridge + Pro Features (Week 8–10)
**Goal:** Code↔Notes bridging, graph view, daily notes, templates

| Task | Days | Deliverable | SRS Ref |
|------|------|------------|---------|
| BridgeService: TODO detection engine | 2 | Configurable patterns, multi-language (FR-600) |
| Bridge: gutter icons + intention action | 2 | "Create Obsidian Note" from TODO (FR-601) |
| Bridge: note reference in comments | 1 | `→ [[note]]` navigation in code (FR-602) |
| Graph view: GraphBuilder data model | 2 | Node/edge model, filtering, local graph (FR-700) |
| Graph view: D3.js visualization in JCEF | 3 | Interactive force-directed graph (FR-700) |
| Daily notes: quick action | 1 | Ctrl+Alt+D, .obsidian config compat (FR-900) |
| Template engine | 1 | Variable substitution, template picker (FR-1000) |
| Multi-vault support | 1 | Independent configs, scoped search (FR-100 multi) |
| Backlinks panel: inline gutter hints | 1 | 🔗 icon with count (FR-301) |

**Milestone:** All Pro features implemented. Licensing integration ready.

---

### Phase 4: Polish + Launch (Week 11–13)
**Goal:** Quality, performance, marketplace launch

| Task | Days | Deliverable |
|------|------|------------|
| Performance optimization (large vaults) | 3 | Meet all NFR targets in SRS §4.1 |
| UI/UX polish + dark/light theme testing | 2 | Consistent with IDE look-and-feel |
| Accessibility audit | 1 | Keyboard nav, screen reader compat |
| IntelliJ Plugin Verifier — full matrix | 1 | Pass for all 9 IDEs × 2 versions |
| Documentation: README, screenshots, GIFs | 2 | Marketplace listing assets |
| JetBrains Marketplace submission | 1 | Plugin approved and live |
| Landing page on htmeta.dev | 1 | Product page, pricing, docs link |
| Launch announcement | 1 | Blog post, Obsidian forum, Reddit, Twitter/X |

**Milestone:** Plugin live on JetBrains Marketplace. Free and Pro tiers available.

---

### Phase 5: Growth + Iteration (Week 14+)
**Goal:** User feedback, iteration, team tier

| Period | Focus |
|--------|-------|
| Month 4 | User feedback triage, bug fixes, performance tuning |
| Month 5 | Top-voted feature requests, community engagement |
| Month 6 | Team tier development (shared configs, team templates) |
| Month 7 | Advanced graph features (clusters, filters, search in graph) |
| Month 8 | Obsidian Publish integration (read-only) |
| Month 9+ | Enterprise features, custom integrations, API |

---

## 2. CI/CD Pipeline

### 2.1 GitHub Actions Workflow

```yaml
name: Build & Test

on:
  push:
    branches: [main, develop]
  pull_request:
    branches: [main]
  release:
    types: [created]

env:
  JAVA_VERSION: '17'
  GRADLE_OPTS: '-Dorg.gradle.daemon=false'

jobs:
  build:
    strategy:
      matrix:
        platform-version:
          - '2023.3'   # Minimum supported
          - '2024.2'   # Current stable
          - '2025.1'   # Latest
    runs-on: ubuntu-latest
    steps:
      - uses: actions/checkout@v4
      - uses: actions/setup-java@v4
        with:
          distribution: 'temurin'
          java-version: ${{ env.JAVA_VERSION }}
      - name: Build
        run: ./gradlew build -PplatformVersion=${{ matrix.platform-version }}
      - name: Test
        run: ./gradlew test
      - name: Verify Plugin
        run: ./gradlew runPluginVerifier
      - name: Upload artifact
        uses: actions/upload-artifact@v4
        with:
          name: plugin-${{ matrix.platform-version }}
          path: build/distributions/*.zip

  performance:
    runs-on: ubuntu-latest
    needs: build
    steps:
      - uses: actions/checkout@v4
      - uses: actions/setup-java@v4
        with:
          distribution: 'temurin'
          java-version: ${{ env.JAVA_VERSION }}
      - name: Run Benchmarks
        run: ./gradlew jmh
      - name: Check Thresholds
        run: python scripts/check_benchmarks.py build/reports/jmh/

  publish:
    if: github.event_name == 'release'
    runs-on: ubuntu-latest
    needs: [build, performance]
    steps:
      - uses: actions/checkout@v4
      - uses: actions/setup-java@v4
        with:
          distribution: 'temurin'
          java-version: ${{ env.JAVA_VERSION }}
      - name: Publish to Marketplace
        run: ./gradlew publishPlugin
        env:
          PUBLISH_TOKEN: ${{ secrets.JETBRAINS_MARKETPLACE_TOKEN }}
```

### 2.2 Quality Gates

Every PR must pass:

| Gate | Tool | Threshold |
|------|------|-----------|
| Unit tests | JUnit 5 | 100% pass |
| Code coverage | JaCoCo | ≥80% branch |
| Plugin verification | IntelliJ Plugin Verifier | 0 errors |
| Static analysis | Detekt | 0 critical/high |
| Performance | JMH benchmarks | No regression >10% |
| Dependency audit | Gradle dependency check | 0 critical CVEs |

### 2.3 Release Process

```
1. Feature branch → develop (PR + review)
2. develop → main (release PR + full CI)
3. Tag v1.x.y on main → triggers publish job
4. Marketplace review (JetBrains, ~24-48h for first release)
5. Automatic update for users via IDE plugin manager
```

### 2.4 Versioning

- **Format:** SemVer `MAJOR.MINOR.PATCH`
- **MAJOR:** Breaking changes (rare, requires migration guide)
- **MINOR:** New features (aligned with phases)
- **PATCH:** Bug fixes, performance improvements

---

## 3. Release Strategy

### 3.1 Release Channels

| Channel | Plugin.xml channel | Audience | Cadence |
|---------|--------------------|----------|---------|
| Stable | `default` | All users | Every 2-4 weeks |
| EAP | `eap` | Beta testers | Weekly |
| Nightly | `nightly` | Contributors | Daily (automated) |

### 3.2 Marketplace Listing

**Plugin Name:** Obsidian Bridge  
**Short Description:** Bring your Obsidian vault into JetBrains. Navigate wiki-links, view backlinks, search notes — all without leaving your IDE.

**Tags:** Obsidian, Markdown, Knowledge Management, Wiki, Notes, Backlinks, PKM

**Screenshots (6 required):**
1. Wiki-link navigation with Ctrl+Click
2. Backlinks panel showing references
3. Vault search dialog with filters
4. Tag tree navigation
5. Graph view visualization
6. Code↔Notes bridge (TODO → Obsidian note)

**Pricing:** Freemium subscription via JetBrains Marketplace

---

## 4. Monitoring & Analytics (Pro Tier, Opt-In Only)

### 4.1 Metrics to Track

| Metric | Source | Purpose |
|--------|--------|---------|
| Daily Active Users | Marketplace stats | Growth tracking |
| Feature usage | Opt-in analytics | Prioritize development |
| Vault size distribution | Opt-in analytics | Performance targeting |
| Error rate | Opt-in error reporting | Stability |
| Search latency (p50, p95, p99) | Opt-in analytics | Performance monitoring |
| Free → Pro conversion | Marketplace stats | Revenue optimization |

### 4.2 Error Reporting

- Opt-in only, clearly disclosed in settings
- Anonymized: no vault content, no file names, no user data
- Stack traces + environment info (IDE version, OS, vault size)
- Sent to self-hosted Sentry instance

---

## 5. Support & Maintenance

### 5.1 Support Tiers

| Tier | Channel | SLA |
|------|---------|-----|
| Free | GitHub Issues | Best-effort, community |
| Pro | GitHub Issues + Email | <24h first response |
| Team | Email + Priority queue | <8h first response |

### 5.2 Maintenance Schedule

| Activity | Frequency |
|----------|-----------|
| Dependency updates | Monthly |
| IntelliJ Platform SDK update | Per major IDE release (~quarterly) |
| Security audit | Quarterly |
| Performance regression check | Per release |
| Obsidian compatibility check | Monthly |

---

## 6. Risk Mitigation Plan

### 6.1 Technical Risks

| Risk | Mitigation | Contingency |
|------|-----------|-------------|
| IntelliJ Platform API breaking change | Monitor EAP releases, abstract platform layer | Conditional code paths for different platform versions |
| Large vault performance degradation | Incremental indexing, lazy loading, pagination | Configurable vault size limits with graceful degradation |
| JCEF (graph view) not available | Feature detection at runtime | Fallback to simple tree-based graph representation |
| Obsidian vault format change | Abstract parser layer, version detection | Rapid parser update + hotfix release |

### 6.2 Business Risks

| Risk | Mitigation | Contingency |
|------|-----------|-------------|
| Low marketplace discovery | SEO optimization, content marketing, community | Paid acquisition via targeted dev communities |
| JetBrains removes marketplace free tier | Monitor marketplace policy changes | Direct sales channel via htmeta.dev |
| Competitor launches similar plugin | Speed to feature completeness, community lock-in | Differentiate with Pro features and quality |
| Low conversion rate (<3%) | A/B test paywall, adjust feature split | Lower Pro price, add more Free features for viral growth |

---

## 7. Go-to-Market Timeline

```
Week 1-2:   Foundation + CI/CD
Week 3-5:   MVP (vault + links + backlinks) → Internal dogfooding
Week 6:     Alpha release on EAP channel → Invite 20 beta testers
Week 7:     Collect feedback, iterate
Week 8-10:  Pro features development
Week 11:    Beta release → Obsidian community announcement
Week 12:    Performance optimization + polish
Week 13:    🚀 PUBLIC LAUNCH on JetBrains Marketplace
Week 14-15: Launch marketing push (blog, Reddit, HN, Twitter/X)
Week 16+:   Iterate based on user feedback
```

---

## 8. Technology Decisions Log

| Decision | Chosen | Alternatives Considered | Rationale |
|----------|--------|------------------------|-----------|
| Language | Kotlin | Java | JetBrains standard, coroutines, null safety |
| Graph rendering | JCEF + D3.js | JavaFX, Swing Canvas, JGraphX | Best visual quality, familiar JS ecosystem |
| YAML parser | SnakeYAML | Jackson YAML, kaml | Already bundled with IntelliJ |
| Search index | Custom + FileBasedIndex | Lucene, SQLite FTS | No extra dependencies, IntelliJ-native lifecycle |
| Build system | Gradle + IntelliJ Gradle Plugin | Maven | JetBrains standard, better plugin support |
| CI/CD | GitHub Actions | JetBrains TeamCity, GitLab CI | Free for open-source, good marketplace integration |
| Licensing | JetBrains Marketplace | Gumroad, Paddle, self-hosted | Native IDE integration, trusted payment |

---

## 9. Budget Estimate (Solo Developer)

| Item | Monthly Cost | Notes |
|------|-------------|-------|
| JetBrains All Products Pack | €28.90 | IDE licenses for testing |
| GitHub Pro | €4 | CI/CD minutes, private repos |
| Domain + hosting (htmeta.dev) | €15 | Landing page, docs |
| Sentry (error tracking) | €0 | Free tier sufficient |
| Marketing budget | €100–500 | Initial launch campaigns |
| **Total** | **~€150–550/month** | |

**Break-even:** ~30 Pro subscribers at €5.90/month = €177/month

---

*Document Control: This Production Plan is a living document. Review monthly and update based on actual progress and market feedback.*
