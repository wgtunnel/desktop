package com.zaneschepke.wireguardautotunnel.client.data

import co.touchlab.kermit.Logger
import com.zaneschepke.wireguardautotunnel.core.helper.FileSecretStore
import com.zaneschepke.wireguardautotunnel.keyring.Keyring

/**
 * Wraps the native OS keyring with a raw file fallback (see [FileSecretStore]) for environments
 * where no keyring daemon is available
 */
class ResilientSecretStore(private val service: String) {
    private val log = Logger.withTag("ResilientSecretStore")

    private val keyring: Keyring? by lazy {
        runCatching { Keyring(service) }
            .onFailure { log.w(it) { "Native keyring unavailable, using file fallback" } }
            .getOrNull()
    }

    fun get(name: String): String? {
        FileSecretStore.get(service, name)?.let { return it }
        return keyring?.let { k -> runCatching { k.get(name) }.getOrNull() }
    }

    fun put(name: String, value: String) {
        val storedNatively =
            keyring?.let { k -> runCatching { k.put(name, value) }.isSuccess } ?: false
        if (!storedNatively) {
            log.w { "Falling back to file secret store for $name" }
            FileSecretStore.put(service, name, value)
        }
    }
}
