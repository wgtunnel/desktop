package com.zaneschepke.wireguardautotunnel.parser

class InterfaceScriptsBuilder {

    private val preUp = mutableListOf<String>()
    private val postUp = mutableListOf<String>()
    private val preDown = mutableListOf<String>()
    private val postDown = mutableListOf<String>()

    fun add(key: String, value: String) {
        when (key) {
            "PreUp" -> preUp += value
            "PostUp" -> postUp += value
            "PreDown" -> preDown += value
            "PostDown" -> postDown += value
        }
    }

    fun build(): InterfaceScripts =
        InterfaceScripts(
            preUp = preUp.toList(),
            postUp = postUp.toList(),
            preDown = preDown.toList(),
            postDown = postDown.toList(),
        )

    data class InterfaceScripts(
        val preUp: List<String> = emptyList(),
        val postUp: List<String> = emptyList(),
        val preDown: List<String> = emptyList(),
        val postDown: List<String> = emptyList(),
    )
}
