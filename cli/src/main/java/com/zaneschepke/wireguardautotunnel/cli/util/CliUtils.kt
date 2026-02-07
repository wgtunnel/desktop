package com.zaneschepke.wireguardautotunnel.cli.util

import kotlinx.coroutines.*
import picocli.CommandLine

object CliUtils {
    private val frames = listOf("⠋", "⠙", "⠹", "⠸", "⠼", "⠴", "⠦", "⠧", "⠇", "⠏")

    fun String.renderAnsi(): String = CommandLine.Help.Ansi.AUTO.string(this)

    fun printSuccess(message: String) {
        println("@|green,bold SUCCESS:|@ $message".renderAnsi())
    }

    fun printError(message: String) {
        println("@|red,bold ERROR:|@ $message".renderAnsi())
    }

    fun printInfo(message: String) {
        println("@|blue,bold INFO:|@ $message".renderAnsi())
    }

    fun printWarning(message: String) {
        println("@|yellow,bold WARNING:|@ $message".renderAnsi())
    }

    fun confirm(message: String, defaultNo: Boolean = true): Boolean {
        val prompt = if (defaultNo) "[y/N]" else "[Y/n]"
        print("@|yellow,bold WARNING:|@ $message $prompt: ".renderAnsi())
        val input = readlnOrNull()?.trim()?.lowercase()
        return input == "y" || input == "yes"
    }

    fun clearLine() = print("\u001b[2K\r")
    fun cursorUp(n: Int) = print("\u001b[${n}A")

    suspend fun <T> withSpinner(message: String, block: suspend () -> T): T {
        return coroutineScope {
            val job = launch {
                var i = 0
                while (isActive) {
                    print("\r@|cyan ${frames[i % frames.size]}|@ $message ".renderAnsi())
                    i++
                    delay(80)
                }
            }
            try {
                block()
            } finally {
                job.cancelAndJoin()
                print("\r" + " ".repeat(message.length + 10) + "\r")
            }
        }
    }
}