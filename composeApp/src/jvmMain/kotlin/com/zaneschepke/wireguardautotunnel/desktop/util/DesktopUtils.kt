package com.zaneschepke.wireguardautotunnel.desktop.util

import java.awt.Desktop
import java.net.URI
import java.net.URLEncoder
import java.nio.charset.StandardCharsets

object DesktopUtils {

    fun launchEmailClient(to: String, subject: String, body: String): Result<Unit> {
        return runCatching {
            val desktop = if (Desktop.isDesktopSupported()) Desktop.getDesktop() else null

            if (desktop == null || !desktop.isSupported(Desktop.Action.MAIL)) {
                throw IllegalStateException("Mail actions are not supported on this platform.")
            }

            val encodedSubject =
                URLEncoder.encode(subject, StandardCharsets.UTF_8.name()).replace("+", "%20")
            val encodedBody =
                URLEncoder.encode(body, StandardCharsets.UTF_8.name()).replace("+", "%20")

            val uriString = "mailto:$to?subject=$encodedSubject&body=$encodedBody"

            desktop.mail(URI(uriString))
        }
    }
}
