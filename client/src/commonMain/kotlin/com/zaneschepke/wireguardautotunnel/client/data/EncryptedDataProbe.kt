package com.zaneschepke.wireguardautotunnel.client.data

import androidx.sqlite.SQLiteConnection
import androidx.sqlite.driver.bundled.BundledSQLiteDriver
import androidx.sqlite.driver.bundled.SQLITE_OPEN_READONLY
import java.io.File

/**
 * Whether the database holds anything encrypted with the secret key.
 */
internal fun databaseHasEncryptedData(dbFile: File): Boolean {
    if (!dbFile.exists()) return false
    return runCatching {
            BundledSQLiteDriver().open(dbFile.absolutePath, SQLITE_OPEN_READONLY).use { connection
                ->
                connection.hasRows("tunnel_config") ||
                    connection.hasRows(
                        "proxy_settings",
                        "proxy_username IS NOT NULL OR proxy_password IS NOT NULL",
                    )
            }
        }
        // Can't tell, so assume there is and keep the protection
        .getOrDefault(true)
}

private fun SQLiteConnection.hasRows(table: String, where: String? = null): Boolean {
    val tableExists =
        prepare("SELECT 1 FROM sqlite_master WHERE type = 'table' AND name = '$table'").use {
            it.step()
        }
    if (!tableExists) return false
    val condition = where?.let { " WHERE $it" }.orEmpty()
    return prepare("SELECT 1 FROM $table$condition LIMIT 1").use { it.step() }
}
