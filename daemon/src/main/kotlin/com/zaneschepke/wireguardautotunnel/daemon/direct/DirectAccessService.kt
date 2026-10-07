package com.zaneschepke.wireguardautotunnel.daemon.direct

import com.zaneschepke.wireguardautotunnel.core.ipc.dto.request.DirectWhitelistRequest
import java.io.BufferedReader
import java.io.BufferedWriter
import java.nio.file.Files
import java.nio.file.Path
import java.nio.file.attribute.PosixFilePermissions
import java.util.concurrent.TimeUnit
import kotlinx.coroutines.Dispatchers
import java.util.concurrent.locks.ReentrantLock
import kotlin.concurrent.withLock
import kotlinx.coroutines.withContext
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json

/** One privileged child owns only the dedicated whitelist chains and routing table 53. */
class DirectAccessService(private val json: Json) : AutoCloseable {
    private val mutex = ReentrantLock()
    private var process: Process? = null
    private var input: BufferedWriter? = null
    private var output: BufferedReader? = null
    private var directory: Path? = null
    private var committed = ""
    private var closed = false

    val supported: Boolean get() = System.getProperty("os.name").orEmpty().startsWith("Linux")

    suspend fun apply(entries: String): Result<Unit> = withContext(Dispatchers.IO) {
        mutex.withLock {
            runCatching {
                check(!closed) { "Direct-access service is stopped" }
                require(supported) { "Direct access is supported only on Linux" }
                require(entries.length <= 65536) { "Whitelist is too large" }
                if (process?.isAlive != true) {
                    start()
                    if (committed.isNotEmpty()) exchange(committed)
                }
                exchange(entries)
                committed = entries
            }
        }
    }

    private fun exchange(entries: String) {
        input!!.apply { write(json.encodeToString(DirectWhitelistRequest.serializer(), DirectWhitelistRequest(entries))); newLine(); flush() }
        val line = output!!.readLine() ?: error("Direct-access helper stopped")
        val response = json.decodeFromString(Reply.serializer(), line)
        require(response.error.isEmpty()) { response.error }
    }

    private fun start() {
        stopChild()
        val arch = when (System.getProperty("os.arch")) {
            "amd64", "x86_64" -> "amd64"
            "aarch64", "arm64" -> "arm64"
            else -> error("Unsupported Linux architecture")
        }
        val dir = Files.createTempDirectory("wgtunnel-direct-", PosixFilePermissions.asFileAttribute(PosixFilePermissions.fromString("rwx------")))
        directory = dir
        val executable = dir.resolve("manager")
        javaClass.getResourceAsStream("/direct-whitelist/linux-$arch/manager").use { resource ->
            requireNotNull(resource) { "Direct-access helper is missing from the build" }
            Files.copy(resource, executable)
        }
        Files.setPosixFilePermissions(executable, PosixFilePermissions.fromString("rwx------"))
        process = ProcessBuilder(executable.toString()).redirectError(ProcessBuilder.Redirect.INHERIT).start()
        input = process!!.outputStream.bufferedWriter()
        output = process!!.inputStream.bufferedReader()
    }

    override fun close() = mutex.withLock {
        closed = true
        stopChild()
    }

    private fun stopChild() {
        input?.close()
        process?.let { if (!it.waitFor(15, TimeUnit.SECONDS)) { it.destroy(); if (!it.waitFor(5, TimeUnit.SECONDS)) it.destroyForcibly() } }
        output?.close()
        input = null; output = null; process = null
        directory?.let { Files.deleteIfExists(it.resolve("manager")); Files.deleteIfExists(it) }
        directory = null
    }

    @Serializable private data class Reply(val error: String = "")
}
