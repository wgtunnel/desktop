package com.zaneschepke.wireguardautotunnel.keyring

import com.sun.jna.Native
import com.sun.jna.Pointer

class Keyring(private val service: String) {

    private val native = NativeKeyring.INSTANCE

    fun put(name: String, value: String) {
        val result = native.storeSecret(service, name, value)
        check(result == 1) {
            "Failed to store secret: $name"
        }
    }

    fun get(name: String): String? {
        val ptr: Pointer = native.getSecret(service, name) ?: return null
        return try {
            ptr.getString(0)
        } finally {
            Native.free(Pointer.nativeValue(ptr))
        }
    }

    fun delete(name: String) {
        native.deleteSecret(service, name)
    }
}
