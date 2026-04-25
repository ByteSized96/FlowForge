package com.flowforge.engine.actions

import java.awt.Desktop
import java.net.URI

class OpenWebsiteAction {

    fun open(url: String) {
        if (!Desktop.isDesktopSupported()) {
            throw IllegalStateException("Desktop actions are not supported on this system")
        }

        val finalUrl = if (url.startsWith("http://") || url.startsWith("https://")) {
            url
        } else {
            "https://$url"
        }

        Desktop.getDesktop().browse(URI(finalUrl))
    }
}