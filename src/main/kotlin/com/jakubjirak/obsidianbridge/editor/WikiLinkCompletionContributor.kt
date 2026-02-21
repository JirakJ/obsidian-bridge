package com.jakubjirak.obsidianbridge.editor

import com.intellij.codeInsight.completion.*
import com.intellij.codeInsight.lookup.LookupElementBuilder
import com.intellij.openapi.util.IconLoader
import com.intellij.openapi.util.TextRange
import com.intellij.patterns.PlatformPatterns
import com.intellij.util.ProcessingContext
import com.jakubjirak.obsidianbridge.core.IndexService

class WikiLinkCompletionContributor : CompletionContributor() {
    init {
        extend(
            CompletionType.BASIC,
            PlatformPatterns.psiElement(),
            WikiLinkCompletionProvider()
        )
    }
}

class WikiLinkCompletionProvider : CompletionProvider<CompletionParameters>() {
    private val icon = IconLoader.getIcon("/icons/obsidian.svg", WikiLinkCompletionProvider::class.java)

    override fun addCompletions(
        parameters: CompletionParameters,
        context: ProcessingContext,
        result: CompletionResultSet
    ) {
        val project = parameters.editor.project ?: return
        val document = parameters.editor.document
        val offset = parameters.offset
        val lineNumber = document.getLineNumber(offset)
        val lineStart = document.getLineStartOffset(lineNumber)
        val textBeforeCaret = document.getText(TextRange(lineStart, offset))

        if (!textBeforeCaret.contains("[[") || textBeforeCaret.contains("]]")) return

        val prefix = textBeforeCaret.substringAfterLast("[[")
        val indexService = IndexService.getInstance(project)
        val notes = indexService.getAllNotes()

        for (note in notes) {
            if (prefix.isNotEmpty() && !note.title.lowercase().contains(prefix.lowercase())) continue
            val element = LookupElementBuilder.create(note.title)
                .withIcon(icon)
                .withTypeText(note.relativePath, true)
                .withInsertHandler { ctx, _ ->
                    val doc = ctx.document
                    val tail = ctx.tailOffset
                    doc.insertString(tail, "]]")
                    ctx.editor.caretModel.moveToOffset(tail + 2)
                }
            result.addElement(element)
        }
    }
}
