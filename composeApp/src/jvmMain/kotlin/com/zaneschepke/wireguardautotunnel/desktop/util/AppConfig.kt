package com.zaneschepke.wireguardautotunnel.desktop.util

import java.util.Properties

data class AppConfig(val renderingMode: RenderingMode = RenderingMode.HARDWARE) {

    companion object {
        fun fromProperties(props: Properties): AppConfig {
            val rendering = props.getProperty("rendering", "hardware")
            return AppConfig(renderingMode = RenderingMode.fromString(rendering))
        }
    }

    fun toProperties(): Properties {
        val props = Properties()
        props.setProperty("rendering", renderingMode.name.lowercase())
        return props
    }
}
