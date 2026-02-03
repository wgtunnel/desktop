package com.zaneschepke.wireguardautotunnel.tunnel.native

import com.sun.jna.Callback

interface StatusCodeCallback : Callback{
    fun onTunnelStatusCode(handle: Int, statusCode: Int)
}