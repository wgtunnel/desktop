package com.zaneschepke.wireguardautotunnel.client.data.service

import com.zaneschepke.wireguardautotunnel.client.service.DaemonHealthService
import io.ktor.client.*
import io.ktor.client.request.*
import io.ktor.http.*

class UdsDaemonHealthService(
    private val client : HttpClient
) : DaemonHealthService {
    override suspend fun alive(): Boolean {
        return try {
            client.get("/status") {
            }.status.isSuccess()
        } catch (_ : Exception) {
            false
        }
    }
}