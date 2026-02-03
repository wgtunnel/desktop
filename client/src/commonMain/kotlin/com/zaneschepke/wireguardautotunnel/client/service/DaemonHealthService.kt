package com.zaneschepke.wireguardautotunnel.client.service

interface DaemonHealthService {
    suspend fun alive(): Boolean
}