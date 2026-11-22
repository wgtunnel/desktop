package com.zaneschepke.wireguardautotunnel.desktop.util

import co.touchlab.kermit.Logger
import com.zaneschepke.wireguardautotunnel.composeapp.generated.resources.Res
import com.zaneschepke.wireguardautotunnel.composeapp.generated.resources.file_dialog_failed
import com.zaneschepke.wireguardautotunnel.composeapp.generated.resources.file_dialog_failed_linux
import dev.nucleusframework.core.runtime.Platform
import io.github.vinceglb.filekit.FileKit
import io.github.vinceglb.filekit.PlatformFile
import io.github.vinceglb.filekit.dialogs.FileKitDialogSettings
import io.github.vinceglb.filekit.dialogs.FileKitMode
import io.github.vinceglb.filekit.dialogs.FileKitType
import io.github.vinceglb.filekit.dialogs.openFilePicker
import io.github.vinceglb.filekit.dialogs.openFileSaver
import kotlin.coroutines.cancellation.CancellationException
import org.jetbrains.compose.resources.getString

object FileDialogs {

    suspend fun pickFiles(type: FileKitType = FileKitType.File()): Result<List<PlatformFile>?> =
        run("picker") { FileKit.openFilePicker(type = type, mode = FileKitMode.Multiple()) }

    suspend fun pickFile(type: FileKitType = FileKitType.File()): Result<PlatformFile?> =
        run("picker") { FileKit.openFilePicker(type = type) }

    suspend fun saveFile(suggestedName: String, extension: String): Result<PlatformFile?> =
        run("saver") {
            FileKit.openFileSaver(
                suggestedName = suggestedName,
                defaultExtension = extension,
                directory = null,
                dialogSettings = FileKitDialogSettings.createDefault(),
            )
        }

    suspend fun failureMessage(): String =
        getString(
            if (Platform.Current == Platform.Linux) Res.string.file_dialog_failed_linux
            else Res.string.file_dialog_failed,
        )

    private suspend fun <T> run(kind: String, block: suspend () -> T): Result<T> =
        try {
            Result.success(block())
        } catch (e: CancellationException) {
            throw e
        } catch (e: Throwable) {
            Logger.e(e) { "File $kind dialog failed" }
            Result.failure(e)
        }
}
