package com.flowforge.engine.actions

import java.awt.Rectangle
import java.awt.Robot
import java.awt.Toolkit
import java.io.File
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter
import javax.imageio.ImageIO

class ScreenshotAction {

    fun capture(): File {
        val screenSize = Toolkit.getDefaultToolkit().screenSize
        val screenshot = Robot().createScreenCapture(
            Rectangle(screenSize)
        )

        val folder = File(System.getProperty("user.home"), "Pictures/FlowForge")
        if (!folder.exists()) {
            folder.mkdirs()
        }

        val timestamp = LocalDateTime.now()
            .format(DateTimeFormatter.ofPattern("yyyy-MM-dd_HH-mm-ss"))

        val file = File(folder, "screenshot_$timestamp.png")

        ImageIO.write(screenshot, "png", file)

        return file
    }
}