package com.flowforge.engine.actions

import java.io.File

class FileExistsCondition {

    fun check(path: String): Boolean {
        if (path.isBlank()) return false
        return File(path).exists()
    }
}