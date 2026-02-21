package htmeta.obsidianbridge.settings

import com.intellij.openapi.options.Configurable
import javax.swing.JComponent
import javax.swing.JPanel
import javax.swing.JLabel

class ObsidianSettingsConfigurable : Configurable {
    private var panel: JPanel? = null

    override fun createComponent(): JComponent? {
        if (panel == null) {
            panel = JPanel()
            panel!!.add(JLabel("Obsidian Bridge settings (placeholder)"))
        }
        return panel
    }

    override fun isModified(): Boolean = false
    override fun apply() {}
    override fun getDisplayName(): String = "Obsidian Bridge"
}
