package com.jakubjirak.obsidianbridge.ui.settings

import com.intellij.openapi.options.Configurable
import javax.swing.*
import java.awt.BorderLayout
import java.awt.GridBagConstraints
import java.awt.GridBagLayout
import java.awt.Insets

class ObsidianBridgeConfigurable : Configurable {

    private var panel: JPanel? = null
    private var vaultPathField: JTextField? = null
    private var vaultNameField: JTextField? = null
    private var graphDepthSpinner: JSpinner? = null
    private var showGutterCheckbox: JCheckBox? = null
    private var modified = false

    override fun getDisplayName(): String = "Obsidian Bridge"

    override fun createComponent(): JComponent {
        val mainPanel = JPanel(BorderLayout())
        val formPanel = JPanel(GridBagLayout())
        val gbc = GridBagConstraints().apply {
            fill = GridBagConstraints.HORIZONTAL
            insets = Insets(4, 4, 4, 4)
            weightx = 1.0
        }

        val settings = ObsidianBridgeSettings.getInstance().state

        // Vault path
        gbc.gridy = 0; gbc.gridx = 0; gbc.weightx = 0.0
        formPanel.add(JLabel("Vault Path:"), gbc)
        gbc.gridx = 1; gbc.weightx = 1.0
        vaultPathField = JTextField(settings.vaults.firstOrNull()?.path ?: "").also {
            it.addActionListener { modified = true }
            formPanel.add(it, gbc)
        }

        // Vault name
        gbc.gridy = 1; gbc.gridx = 0; gbc.weightx = 0.0
        formPanel.add(JLabel("Vault Name:"), gbc)
        gbc.gridx = 1; gbc.weightx = 1.0
        vaultNameField = JTextField(settings.vaults.firstOrNull()?.name ?: "My Vault").also {
            formPanel.add(it, gbc)
        }

        // Graph depth
        gbc.gridy = 2; gbc.gridx = 0; gbc.weightx = 0.0
        formPanel.add(JLabel("Graph Depth:"), gbc)
        gbc.gridx = 1; gbc.weightx = 1.0
        graphDepthSpinner = JSpinner(SpinnerNumberModel(settings.graphDepth, 1, 10, 1)).also {
            formPanel.add(it, gbc)
        }

        // Show gutter icons
        gbc.gridy = 3; gbc.gridx = 0; gbc.gridwidth = 2; gbc.weightx = 1.0
        showGutterCheckbox = JCheckBox("Show gutter icons", settings.showGutterIcons).also {
            formPanel.add(it, gbc)
        }

        mainPanel.add(formPanel, BorderLayout.NORTH)
        panel = mainPanel
        return mainPanel
    }

    override fun isModified(): Boolean = true // simplified — always check on apply

    override fun apply() {
        val settings = ObsidianBridgeSettings.getInstance()
        val vaultPath = vaultPathField?.text ?: return
        val vaultName = vaultNameField?.text ?: "My Vault"
        val graphDepth = (graphDepthSpinner?.value as? Int) ?: 2
        val showGutter = showGutterCheckbox?.isSelected ?: true

        val existingVaults = settings.state.vaults.toMutableList()
        if (existingVaults.isEmpty() && vaultPath.isNotBlank()) {
            existingVaults.add(com.jakubjirak.obsidianbridge.core.model.VaultConfig(
                path = vaultPath,
                name = vaultName
            ))
        } else if (existingVaults.isNotEmpty()) {
            existingVaults[0] = existingVaults[0].copy(path = vaultPath, name = vaultName)
        }

        settings.loadState(SettingsState(
            vaults = existingVaults,
            graphDepth = graphDepth,
            showGutterIcons = showGutter
        ))
    }

    override fun reset() {
        val settings = ObsidianBridgeSettings.getInstance().state
        vaultPathField?.text = settings.vaults.firstOrNull()?.path ?: ""
        vaultNameField?.text = settings.vaults.firstOrNull()?.name ?: "My Vault"
        graphDepthSpinner?.value = settings.graphDepth
        showGutterCheckbox?.isSelected = settings.showGutterIcons
    }
}
