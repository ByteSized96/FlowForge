package com.flowforge.engine.actions

import java.awt.SystemTray
import java.awt.TrayIcon
import java.awt.Toolkit

class NotificationAction {

    fun show(title: String, message: String) {
        if (!SystemTray.isSupported()) {
            throw IllegalStateException("System tray notifications are not supported")
        }

        val tray = SystemTray.getSystemTray()
        val image = Toolkit.getDefaultToolkit().createImage(ByteArray(0))

        val trayIcon = TrayIcon(image, "FlowForge")
        trayIcon.isImageAutoSize = true

        tray.add(trayIcon)

        trayIcon.displayMessage(
            title.ifBlank { "FlowForge" },
            message.ifBlank { "Automation completed" },
            TrayIcon.MessageType.INFO
        )

        Thread {
            Thread.sleep(4000)
            tray.remove(trayIcon)
        }.start()
    }
}