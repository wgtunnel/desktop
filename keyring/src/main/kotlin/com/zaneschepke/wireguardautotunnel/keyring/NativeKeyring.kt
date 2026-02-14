package com.zaneschepke.wireguardautotunnel.keyring

import com.sun.jna.Library
import com.sun.jna.Native
import com.sun.jna.Pointer

interface NativeKeyring : Library {

    fun storeSecret(service: String, name: String, value: String): Int

    fun getSecret(service: String, name: String): Pointer?

    fun deleteSecret(service: String, name: String): Int

    companion object {
        val INSTANCE: NativeKeyring = Native.load("keyring", NativeKeyring::class.java)
    }
}
