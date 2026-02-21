package com.jakubjirak.obsidianbridge.ui

import javax.swing.*
import java.awt.BorderLayout

class ProFeaturePanel(featureName: String) : JPanel(BorderLayout()) {
    init {
        val label = JLabel(
            "<html><center><h3>$featureName</h3><p>This is a Pro feature.</p><p>Upgrade to Obsidian IDE Bridge Pro to unlock.</p></center></html>",
            SwingConstants.CENTER
        )
        add(label, BorderLayout.CENTER)
    }
}
