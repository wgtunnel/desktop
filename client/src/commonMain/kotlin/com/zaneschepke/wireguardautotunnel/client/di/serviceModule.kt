package com.zaneschepke.wireguardautotunnel.client.di

import com.zaneschepke.wireguardautotunnel.client.data.service.DefaultTunnelImportService
import com.zaneschepke.wireguardautotunnel.client.data.service.UdsDaemonHealthService
import com.zaneschepke.wireguardautotunnel.client.data.service.UdsTunnelCommandService
import com.zaneschepke.wireguardautotunnel.client.service.DaemonHealthService
import com.zaneschepke.wireguardautotunnel.client.service.TunnelCommandService
import com.zaneschepke.wireguardautotunnel.client.service.TunnelImportService
import com.zaneschepke.wireguardautotunnel.core.crypto.HmacProtector
import com.zaneschepke.wireguardautotunnel.core.ipc.IPC
import com.zaneschepke.wireguardautotunnel.core.ipc.dto.SecureCommand
import io.ktor.client.*
import io.ktor.client.engine.cio.*
import io.ktor.client.plugins.*
import io.ktor.client.plugins.contentnegotiation.*
import io.ktor.client.plugins.websocket.*
import io.ktor.client.request.*
import io.ktor.http.*
import io.ktor.http.content.*
import io.ktor.serialization.kotlinx.json.*
import kotlinx.serialization.json.Json
import org.koin.dsl.module

val serviceModule = module {
    single {
        // so daemon knows where to look for secret
        val user = System.getProperty("user.name")
        HttpClient(CIO) {
            defaultRequest {
                unixSocket(IPC.getDaemonSocketPath())
            }
            install(ContentNegotiation) {
                json(Json {
                    ignoreUnknownKeys = true
                    encodeDefaults = true
                })
            }
            install(WebSockets)
            install("HmacSigner") {
                requestPipeline.intercept(HttpRequestPipeline.Before) {

                    if (subject is SecureCommand) {
                        return@intercept
                    }

                    val payload = when (val body = subject) {
                        is String -> body
                        is TextContent -> body.text
                        else -> ""
                    }

                    val timestamp = System.currentTimeMillis() / 1000
                    val signature = HmacProtector.generateSignature(
                        IPC.getIPCSecret(),
                        timestamp,
                        payload
                    )

                    val secureCommand = SecureCommand(timestamp, signature, user, payload)
                    context.contentType(ContentType.Application.Json)
                    context.setBody(secureCommand)

                    proceedWith(secureCommand)
                }
            }
        }
    }
    single<DaemonHealthService> { UdsDaemonHealthService(get()) }
    single<TunnelCommandService> { UdsTunnelCommandService(get(), tunnelRepository = get()) }
    single<TunnelImportService> {
        DefaultTunnelImportService(get())
    }
}