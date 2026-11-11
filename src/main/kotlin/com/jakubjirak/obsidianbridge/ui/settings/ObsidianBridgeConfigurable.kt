package com.jakubjirak.obsidianbridge.ui.settings

import com.intellij.openapi.options.Configurable
import com.intellij.openapi.options.ConfigurationException
import com.intellij.openapi.project.ProjectManager
import com.jakubjirak.obsidianbridge.core.IndexService
import com.jakubjirak.obsidianbridge.core.VaultManager
import java.awt.BorderLayout
import java.awt.GridBagConstraints
import java.awt.GridBagLayout
import java.awt.Insets
import javax.swing.JCheckBox
import javax.swing.JComponent
import javax.swing.JLabel
import javax.swing.JPanel
import javax.swing.JTextField

/** Settings → Tools → Obsidian IDE Bridge: primary vault path/name and gutter icons. */
class ObsidianBridgeConfigurable : Configurable {

    private val vaultPathField = JTextField()
    private val vaultNameField = JTextField()
    private val showGutterCheckbox = JCheckBox("Show gutter icons for [[wiki-links]] in code comments")

    override fun getDisplayName(): String = "Obsidian IDE Bridge"

    override fun createComponent(): JComponent {
        val formPanel = JPanel(GridBagLayout())
        val gbc = GridBagConstraints().apply {
            fill = GridBagConstraints.HORIZONTAL
            insets = Insets(INSET, INSET, INSET, INSET)
        }
        fun row(y: Int, label: String, field: JComponent) {
            gbc.gridy = y; gbc.gridx = 0; gbc.weightx = 0.0; gbc.gridwidth = 1
            formPanel.add(JLabel(label), gbc)
            gbc.gridx = 1; gbc.weightx = 1.0
            formPanel.add(field, gbc)
        }
        row(0, "Vault path:", vaultPathField)
        row(1, "Vault name:", vaultNameField)
        gbc.gridy = 2; gbc.gridx = 0; gbc.gridwidth = 2; gbc.weightx = 1.0
        formPanel.add(showGutterCheckbox, gbc)
        reset()
        return JPanel(BorderLayout()).apply { add(formPanel, BorderLayout.NORTH) }
    }

    override fun isModified(): Boolean {
        val state = ObsidianBridgeSettings.getInstance().state
        val primary = state.vaults.firstOrNull()
        return vaultPathField.text.trim() != (primary?.path ?: "") ||
            vaultNameField.text.trim() != (primary?.name ?: "") ||
            showGutterCheckbox.isSelected != state.showGutterIcons
    }

    override fun apply() {
        val path = vaultPathField.text.trim()
        if (path.isNotEmpty() && !VaultManager.isValidVault(path)) {
            throw ConfigurationException("'$path' is not an Obsidian vault (no .obsidian folder found).")
        }
        val settings = ObsidianBridgeSettings.getInstance()
        settings.loadState(
            ObsidianBridgeSettings.withPrimaryVault(settings.state, path, vaultNameField.text, showGutterCheckbox.isSelected)
        )
        reindexOpenProjects()
    }

    override fun reset() {
        val state = ObsidianBridgeSettings.getInstance().state
        vaultPathField.text = state.vaults.firstOrNull()?.path ?: ""
        vaultNameField.text = state.vaults.firstOrNull()?.name ?: ""
        showGutterCheckbox.isSelected = state.showGutterIcons
    }

    private fun reindexOpenProjects() {
        for (project in ProjectManager.getInstance().openProjects) {
            val index = IndexService.getInstance(project)
            com.intellij.openapi.application.ApplicationManager.getApplication().executeOnPooledThread {
                index.clear()
                VaultManager.getInstance(project).getActiveVaults().forEach { index.indexVault(it) }
            }
        }
    }

    private companion object {
        const val INSET = 4
    }
}
