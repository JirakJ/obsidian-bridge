package com.jakubjirak.obsidianbridge.ui.toolwindow

import com.intellij.openapi.project.Project
import com.intellij.openapi.wm.ToolWindow
import com.intellij.openapi.wm.ToolWindowFactory
import com.intellij.ui.content.ContentFactory
import com.intellij.ui.components.JBScrollPane
import com.jakubjirak.obsidianbridge.core.IndexService
import com.jakubjirak.obsidianbridge.core.VaultManager
import javax.swing.*
import java.awt.*
import java.awt.event.MouseAdapter
import java.awt.event.MouseEvent
import java.awt.geom.Ellipse2D
import kotlin.math.cos
import kotlin.math.sin
import kotlin.math.sqrt

class GraphToolWindowFactory : ToolWindowFactory {
    override fun createToolWindowContent(project: Project, toolWindow: ToolWindow) {
        val panel = GraphPanel(project)
        val content = ContentFactory.getInstance().createContent(panel, "", false)
        toolWindow.contentManager.addContent(content)
    }
}

class GraphPanel(private val project: Project) : JPanel(BorderLayout()) {

    private val graphCanvas = GraphCanvas(project)

    init {
        val toolbar = JPanel(FlowLayout(FlowLayout.LEFT, 8, 4))
        val refreshBtn = JButton("↻ Refresh")
        refreshBtn.addActionListener { graphCanvas.rebuild() }
        toolbar.add(refreshBtn)


        add(toolbar, BorderLayout.NORTH)
        add(JBScrollPane(graphCanvas), BorderLayout.CENTER)

        graphCanvas.rebuild()
    }
}

/** Lightweight force-directed–style graph rendered via Swing 2D. */
private class GraphCanvas(private val project: Project) : JPanel() {

    private data class GNode(val id: String, val label: String, var x: Double, var y: Double)
    private data class GEdge(val from: String, val to: String)

    private var nodes = mutableListOf<GNode>()
    private var edges = mutableListOf<GEdge>()
    private var hoveredNode: GNode? = null
    private val nodeRadius = 6.0

    private val bgColor = Color(15, 15, 35)
    private val nodeColor = Color(102, 126, 234)
    private val edgeColor = Color(102, 126, 234, 50)
    private val labelColor = Color(200, 200, 200)
    private val hoverColor = Color(102, 187, 106)

    init {
        preferredSize = Dimension(800, 600)
        background = bgColor

        addMouseMotionListener(object : MouseAdapter() {
            override fun mouseMoved(e: MouseEvent) {
                val prev = hoveredNode
                hoveredNode = nodes.find { dist(e.x.toDouble(), e.y.toDouble(), it.x, it.y) < nodeRadius + 4 }
                if (prev != hoveredNode) {
                    cursor = if (hoveredNode != null) Cursor.getPredefinedCursor(Cursor.HAND_CURSOR) else Cursor.getDefaultCursor()
                    repaint()
                }
            }
        })

        addMouseListener(object : MouseAdapter() {
            override fun mouseClicked(e: MouseEvent) {
                val node = nodes.find { dist(e.x.toDouble(), e.y.toDouble(), it.x, it.y) < nodeRadius + 4 } ?: return
                IndexService.getInstance(project).findById(node.id)?.let { NoteOpener.open(project, it) }
            }
        })
    }

    fun rebuild() {
        nodes.clear()
        edges.clear()

        val vaults = VaultManager.getInstance(project).getActiveVaults()
        if (vaults.isEmpty()) {
            repaint()
            return
        }

        val indexService = IndexService.getInstance(project)
        val allNotes = indexService.getAllNotes()

        if (allNotes.isEmpty()) {
            repaint()
            return
        }

        allNotes.forEach { nodes.add(GNode(id = it.id, label = it.title, x = 0.0, y = 0.0)) }
        indexService.edges().forEach { edges.add(GEdge(from = it.fromId, to = it.toId)) }

        layoutCircular()
        repaint()
    }

    private fun layoutCircular() {
        val n = nodes.size
        if (n == 0) return
        val cx = 400.0
        val cy = 300.0
        val radius = (n * 12.0).coerceIn(100.0, 280.0)
        for ((i, node) in nodes.withIndex()) {
            val angle = 2.0 * Math.PI * i / n
            node.x = cx + radius * cos(angle)
            node.y = cy + radius * sin(angle)
        }
    }

    override fun paintComponent(g: Graphics) {
        super.paintComponent(g)
        val g2 = g as Graphics2D
        g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON)

        g2.color = bgColor
        g2.fillRect(0, 0, width, height)

        val vaults = VaultManager.getInstance(project).getActiveVaults()
        if (vaults.isEmpty()) {
            g2.color = labelColor
            g2.font = Font("SansSerif", Font.PLAIN, 14)
            val msg = "No vault configured. Go to Settings → Tools → Obsidian IDE Bridge."
            val fm = g2.fontMetrics
            g2.drawString(msg, (width - fm.stringWidth(msg)) / 2, height / 2)
            return
        }

        if (nodes.isEmpty()) {
            g2.color = labelColor
            g2.font = Font("SansSerif", Font.PLAIN, 14)
            val msg = "No notes indexed. Use Tools → Obsidian IDE Bridge → Re-index Vault."
            val fm = g2.fontMetrics
            g2.drawString(msg, (width - fm.stringWidth(msg)) / 2, height / 2)
            return
        }

        val nodeById = nodes.associateBy { it.id }

        // Draw edges
        g2.color = edgeColor
        g2.stroke = BasicStroke(1.0f)
        for (edge in edges) {
            val from = nodeById[edge.from]
            val to = nodeById[edge.to]
            if (from != null && to != null) g2.drawLine(from.x.toInt(), from.y.toInt(), to.x.toInt(), to.y.toInt())
        }

        // Draw nodes
        g2.font = Font("SansSerif", Font.PLAIN, 10)
        for (node in nodes) {
            val isHover = node === hoveredNode
            val r = if (isHover) nodeRadius + 2 else nodeRadius
            g2.color = if (isHover) hoverColor else nodeColor
            g2.fill(Ellipse2D.Double(node.x - r, node.y - r, r * 2, r * 2))

            if (isHover || nodes.size <= 50) {
                g2.color = labelColor
                g2.drawString(node.label, (node.x + r + 3).toInt(), (node.y + 4).toInt())
            }
        }

        // Legend
        g2.color = Color(100, 100, 100)
        g2.font = Font("SansSerif", Font.PLAIN, 11)
        g2.drawString("${nodes.size} notes · ${edges.size} links", 8, height - 8)
    }

    private fun dist(x1: Double, y1: Double, x2: Double, y2: Double): Double {
        val dx = x1 - x2; val dy = y1 - y2
        return sqrt(dx * dx + dy * dy)
    }
}
