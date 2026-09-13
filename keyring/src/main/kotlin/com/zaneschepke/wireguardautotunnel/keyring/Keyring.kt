package com.zaneschepke.wireguardautotunnel.keyring

class Keyring(private val service: String) {

    init {
        KeyringNative.ensureLoaded()
    }

    fun put(name: String, value: String) {
        val result = KeyringNative.storeSecret(service, name, value)
        check(result == 1) { "Failed to store secret: $name" }
    }

    fun get(name: String): String? {
        return KeyringNative.getSecret(service, name)
    }

    fun delete(name: String) {
        KeyringNative.deleteSecret(service, name)
    }
}
