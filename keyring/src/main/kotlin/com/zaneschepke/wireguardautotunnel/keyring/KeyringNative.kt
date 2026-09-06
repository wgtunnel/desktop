package com.zaneschepke.wireguardautotunnel.keyring

import dev.nucleusframework.core.runtime.NativeLibraryLoader

internal object KeyringNative {
    private val loaded = NativeLibraryLoader.load(LIBRARY_NAME, KeyringNative::class.java)

    @JvmStatic external fun storeSecret(service: String, name: String, value: String): Int

    @JvmStatic external fun getSecret(service: String, name: String): String?

    @JvmStatic external fun deleteSecret(service: String, name: String): Int

    fun ensureLoaded() {
        check(loaded) { "Failed to load native keyring library ($LIBRARY_NAME)" }
    }

    private const val LIBRARY_NAME = "keyring"
}
