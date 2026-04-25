package com.flowforge.engine.actions

import java.io.File

class CreateFileAction {

    fun create(filename: String, content: String): File {
        val folder = File(System.getProperty("user.home"), "Documents/FlowForge")

        if (!folder.exists()) {
            folder.mkdirs()
        }

        val safeFilename = filename
            .ifBlank { "flowforge-note.txt" }
            .replace(Regex("[\\\\/:*?\"<>|]"), "_")

        val file = File(folder, safeFilename)

        file.writeText(content.ifBlank { "Created by FlowForge" })

        return file
    }
}