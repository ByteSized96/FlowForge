package com.flowforge

import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Window
import androidx.compose.ui.window.WindowState
import androidx.compose.ui.window.application
import com.flowforge.ui.FlowForgeApp

fun main() = application {
    Window(
        onCloseRequest = ::exitApplication,
        title = "FlowForge",
        state = WindowState(width = 1450.dp, height = 900.dp)
    ) {
        FlowForgeApp()
    }
}
