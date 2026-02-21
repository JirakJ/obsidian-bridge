package com.jakubjirak.obsidianbridge.editor

import com.intellij.codeInsight.daemon.LineMarkerInfo
import com.intellij.codeInsight.daemon.LineMarkerProvider
import com.intellij.openapi.editor.markup.GutterIconRenderer
import com.intellij.openapi.util.IconLoader
import com.intellij.psi.PsiComment
import com.intellij.psi.PsiElement
import com.intellij.psi.PsiPlainText
import javax.swing.Icon

class WikiLinkLineMarkerProvider : LineMarkerProvider {
    private val wikiLinkPattern = Regex("""\[\[([^\]]+?)]]""")
    private val icon: Icon = IconLoader.getIcon("/icons/obsidian.svg", WikiLinkLineMarkerProvider::class.java)

    override fun getLineMarkerInfo(element: PsiElement): LineMarkerInfo<*>? {
        if (element !is PsiComment && element !is PsiPlainText) return null
        val text = element.text
        if (!wikiLinkPattern.containsMatchIn(text)) return null

        return LineMarkerInfo(
            element,
            element.textRange,
            icon,
            { "Obsidian wiki-link detected" },
            null,
            GutterIconRenderer.Alignment.LEFT
        ) { "Obsidian wiki-link" }
    }
}
