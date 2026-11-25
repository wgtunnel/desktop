package com.zaneschepke.wireguardautotunnel.daemon.data

import co.touchlab.kermit.Logger
import com.wgtunnel.backend.system.SystemSecret
import com.zaneschepke.wireguardautotunnel.core.helper.FilePathsHelper
import com.zaneschepke.wireguardautotunnel.core.ipc.dto.AutoTunnelConfigDto
import com.zaneschepke.wireguardautotunnel.core.ipc.dto.request.StartTunnelRequest
import com.zaneschepke.wireguardautotunnel.core.profile.AppVariant
import com.zaneschepke.wireguardautotunnel.daemon.util.BootId
import java.nio.file.Files
import java.nio.file.Path
import java.nio.file.Paths
import java.nio.file.StandardCopyOption
import java.nio.file.attribute.PosixFilePermissions
import java.util.concurrent.TimeUnit
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json

/** What the daemon was running, so a restart within the same boot can bring it back. */
@Serializable
data class DaemonSession(
    val bootId: String,
    val tunnels: Map<Int, StartTunnelRequest> = emptyMap(),
    val autoTunnelPlan: AutoTunnelConfigDto? = null,
)

/**
 * Keeps [DaemonSession] across daemon restarts but never across a reboot. It holds private keys, so
 * it lives where only root or SYSTEM can read it, and is never used after the boot that wrote it.
 * Linux: /run, which is on RAM and cleared on boot. macOS: /var/run, cleared on boot. Windows:
 * ProgramData, restricted to SYSTEM and Administrators and DPAPI encrypted for SYSTEM. The boot id
 * check discards it after a reboot.
 */
class DaemonSessionStore(private val json: Json, private val path: Path = defaultPath()) {
    private val log = Logger.withTag("DaemonSession")
    private val lock = Any()
    private var session = DaemonSession(bootId = BootId.current.orEmpty())

    /** The session saved earlier in this boot, or null. Discards anything else. */
    fun loadFromThisBoot(): DaemonSession? =
        synchronized(lock) {
            val saved = runCatching { read() }.onFailure { log.w(it) { "Discarding session" } }
            val current = BootId.current
            val loaded = saved.getOrNull()?.takeIf { current != null && it.bootId == current }
            if (loaded == null) {
                if (saved.getOrNull() != null) log.i { "Discarding session from an earlier boot" }
                deleteLocked()
                return null
            }
            session = loaded
            loaded
        }

    fun putTunnel(id: Int, request: StartTunnelRequest) = update {
        it.copy(tunnels = it.tunnels + (id to request))
    }

    fun removeTunnel(id: Int) = update { it.copy(tunnels = it.tunnels - id) }

    /** Keeps only [ids], the tunnels actually running when the daemon stops. */
    fun retainTunnels(ids: Set<Int>) = update {
        it.copy(tunnels = it.tunnels.filterKeys(ids::contains))
    }

    fun setAutoTunnelPlan(plan: AutoTunnelConfigDto) = update { it.copy(autoTunnelPlan = plan) }

    fun clear() =
        synchronized(lock) {
            session = DaemonSession(bootId = BootId.current.orEmpty())
            deleteLocked()
        }

    private fun update(change: (DaemonSession) -> DaemonSession) =
        synchronized(lock) {
            val bootId = BootId.current ?: return@synchronized
            session = change(session).copy(bootId = bootId)
            runCatching { write(session) }.onFailure { log.e(it) { "Failed to save session" } }
        }

    private fun read(): DaemonSession? {
        if (Files.notExists(path)) return null
        check(isTrusted(path)) { "$path is not owned and restricted to the daemon's account" }
        val raw = Files.readAllBytes(path)
        val plain =
            if (isWindows) SystemSecret.unprotect(raw) ?: error("DPAPI could not decrypt $path")
            else raw
        return json.decodeFromString(DaemonSession.serializer(), plain.decodeToString())
    }

    private fun write(value: DaemonSession) {
        val plain = json.encodeToString(DaemonSession.serializer(), value).encodeToByteArray()
        val bytes =
            if (isWindows) {
                // Not stored at all rather than stored in the clear.
                SystemSecret.protect(plain) ?: error("DPAPI unavailable, session not saved")
            } else plain
        Files.createDirectories(path.parent)
        val tmp = path.resolveSibling("${path.fileName}.tmp")
        Files.deleteIfExists(tmp)
        if (isWindows) {
            Files.write(tmp, bytes)
            restrictWindows(tmp)
        } else {
            Files.createFile(tmp, PosixFilePermissions.asFileAttribute(OWNER_ONLY))
            Files.write(tmp, bytes)
        }
        Files.move(tmp, path, StandardCopyOption.REPLACE_EXISTING, StandardCopyOption.ATOMIC_MOVE)
    }

    private fun deleteLocked() {
        runCatching { Files.deleteIfExists(path) }
            .onFailure { log.w(it) { "Failed to delete $path" } }
    }

    private fun isTrusted(file: Path): Boolean {
        if (isWindows) return true // ACL set on write and DPAPI fails for anything we didn't write
        val owner = Files.getOwner(file).name
        val perms = Files.getPosixFilePermissions(file)
        return owner == "root" && perms == OWNER_ONLY
    }

    private fun restrictWindows(file: Path) {
        val process =
            ProcessBuilder(
                    "icacls",
                    file.toString(),
                    "/inheritance:r",
                    "/grant:r",
                    "*S-1-5-18:F",
                    "/grant:r",
                    "*S-1-5-32-544:F",
                )
                .redirectErrorStream(true)
                .start()
        process.inputStream.readAllBytes()
        check(process.waitFor(10, TimeUnit.SECONDS) && process.exitValue() == 0) {
            "icacls could not restrict $file"
        }
    }

    companion object {
        private val isWindows = System.getProperty("os.name").orEmpty().lowercase().contains("win")
        private val isMac = System.getProperty("os.name").orEmpty().lowercase().contains("mac")
        private val OWNER_ONLY = PosixFilePermissions.fromString("rw-------")

        fun defaultPath(): Path {
            val name = AppVariant.current.linuxFsName
            return when {
                isWindows -> FilePathsHelper.getDaemonCacheBaseDir().resolve("session.bin")
                isMac -> Paths.get("/var/run", "$name-session.json")
                else -> FilePathsHelper.getDaemonRuntimeDir().resolve("session.json")
            }
        }
    }
}
