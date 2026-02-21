package com.jakubjirak.obsidianbridge.util

import com.intellij.openapi.vfs.VirtualFile

fun VirtualFile.isMarkdown(): Boolean = extension == "md"

fun String.ensureMdExtension(): String =
    if (endsWith(".md")) this else "$this.md"
