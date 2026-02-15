package com.zaneschepke.wireguardautotunnel.tunnel.native

import com.sun.jna.Library
import com.sun.jna.Native
import com.sun.jna.Pointer

interface AwgTunnel : Library {

    // Normal tunnel methods
    fun awgTurnOn(cfg: String?, callback: StatusCodeCallback?): Int

    fun awgTurnOff(handle: Int)

    fun awgGetConfig(handle: Int): Pointer?

    fun awgTurnOffAll()

    // Proxy tunnel methods
    fun awgProxyTurnOn(cfg: String?, callback: StatusCodeCallback?): Int

    fun awgProxyGetConfig(handle: Int): Pointer?

    fun awgProxyTurnOffAll()

    fun awgProxyTurnOff(handle: Int)

    fun setKillSwitch(value: Int): Int // 1 for enable, 0 for disable, return 1 or -1 for error

    fun setKillSwitchLanBypass(value: Int): Int

    fun getKillSwitchLanBypassStatus(): Int

    fun getKillSwitchStatus(): Int // 1 for enabled, 0 for disabled

    companion object {
        val INSTANCE: AwgTunnel = Native.load("wg", AwgTunnel::class.java)
    }
}
