package com.zaneschepke.wireguardautotunnel.daemon.tunnel

import com.wgtunnel.backend.network.NetworkInfoDto
import com.wgtunnel.backend.system.DesktopNativeNetworkMonitor
import com.wgtunnel.backend.system.NetworkMonitor
import com.wgtunnel.backend.system.NetworkSnapshot
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.StateFlow

class DesktopNetworkMonitor(scope: CoroutineScope) : NetworkMonitor {
    private val impl = DesktopNativeNetworkMonitor(scope)

    val info: StateFlow<NetworkInfoDto> = impl.info

    override val networkState: StateFlow<NetworkSnapshot?> = impl.networkState

    fun stop() = impl.stop()
}
