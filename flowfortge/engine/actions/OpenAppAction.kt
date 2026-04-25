package com.flowforge.engine.actions

import java.io.File

class OpenAppAction {

    fun open(path: String) {
        if (path.isBlank()) {
            throw IllegalArgumentException("No app or file path provided")
        }

        val file = File(path)

        if (!file.exists()) {
            throw IllegalArgumentException("Path does not exist: $path")
        }

        ProcessBuilder(file.absolutePath)
            .start()
    }
}