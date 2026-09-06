package com.zaneschepke.wireguardautotunnel.client.di

import co.touchlab.kermit.Logger
import com.zaneschepke.wireguardautotunnel.client.data.service.DefaultTunnelImportService
import com.zaneschepke.wireguardautotunnel.client.data.service.UdsBackendService
import com.zaneschepke.wireguardautotunnel.client.data.service.UdsDaemonService
import com.zaneschepke.wireguardautotunnel.client.data.service.UdsTunnelService
import com.zaneschepke.wireguardautotunnel.client.domain.error.ClientException
import com.zaneschepke.wireguardautotunnel.client.orchestration.AutoTunnelCoordinator
import com.zaneschepke.wireguardautotunnel.client.orchestration.LogCoordinator
import com.zaneschepke.wireguardautotunnel.client.orchestration.TunnelBackendCoordinator
import com.zaneschepke.wireguardautotunnel.client.orchestration.TunnelCoordinator
import com.zaneschepke.wireguardautotunnel.client.service.BackendService
import com.zaneschepke.wireguardautotunnel.client.service.DaemonService
import com.zaneschepke.wireguardautotunnel.client.service.TunnelImportService
import com.zaneschepke.wireguardautotunnel.client.service.TunnelService
import com.zaneschepke.wireguardautotunnel.core.crypto.HmacProtector
import com.zaneschepke.wireguardautotunnel.core.helper.FilePathsHelper
import com.zaneschepke.wireguardautotunnel.core.ipc.Headers
import com.zaneschepke.wireguardautotunnel.core.ipc.IPC
import com.zaneschepke.wireguardautotunnel.core.ipc.Routes
import dev.nucleusframework.nativehttp.ktor.installNativeSsl
import io.ktor.client.*
import io.ktor.client.engine.cio.*
import io.ktor.client.plugins.*
import io.ktor.client.plugins.contentnegotiation.*
import io.ktor.client.plugins.websocket.*
import io.ktor.client.request.*
import io.ktor.client.statement.*
import io.ktor.client.utils.*
import io.ktor.http.*
import io.ktor.http.content.*
import io.ktor.serialization.kotlinx.*
import io.ktor.serialization.kotlinx.json.*
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.serialization.json.Json
import org.koin.dsl.module

val serviceModule = module {
    single {
        Json {
            ignoreUnknownKeys = true
            encodeDefaults = true
        }
    }
    single {
        val json: Json = get()
        HttpClient(CIO) {
            installNativeSsl()
            defaultRequest {
                contentType(ContentType.Application.Json)
                accept(ContentType.Application.Json)
            }
            install(ContentNegotiation) { json(json) }
            install(WebSockets) {
                contentConverter = KotlinxWebsocketSerializationConverter(json)
                pingIntervalMillis = 20_000
                maxFrameSize = Long.MAX_VALUE
            }
            expectSuccess = true
            HttpResponseValidator {
                handleResponseExceptionWithRequest { cause, _ ->
                    val exception =
                        cause as? ClientRequestException
                            ?: return@handleResponseExceptionWithRequest
                    val response = exception.response

                    val bodyText = response.bodyAsText()
                    Logger.d {
                        "Client: Received error response - status=${response.status}, body='$bodyText'"
                    }

                    throw when (response.status) {
                        HttpStatusCode.NotFound,
                        HttpStatusCode.BadRequest -> ClientException.BadRequestException(bodyText)
                        HttpStatusCode.InternalServerError ->
                            ClientException.BadRequestException(bodyText)
                        HttpStatusCode.Conflict -> ClientException.ConflictException(bodyText)
                        HttpStatusCode.Unauthorized ->
                            ClientException.UnauthorizedException(bodyText)
                        else -> ClientException.UnknownError(bodyText)
                    }
                }
            }
            install("UnixSocket") {
                requestPipeline.intercept(HttpRequestPipeline.Before) {
                    context.unixSocket(FilePathsHelper.getDaemonSocketPath())
                    proceedWith(subject)
                }
            }
            install("HmacSigner") {
                requestPipeline.intercept(HttpRequestPipeline.Render) { payload ->
                    val path = context.url.encodedPath
                    if (path == Routes.DAEMON_BASE) return@intercept

                    val secret = IPC.getIPCSecret()
                    val timestamp = System.currentTimeMillis() / 1000

                    val bodyString =
                        when (payload) {
                            is TextContent -> payload.text
                            is EmptyContent -> ""
                            else -> ""
                        }

                    val signature = HmacProtector.generateSignature(secret, timestamp, bodyString)

                    context.header(Headers.HMAC_TIMESTAMP, timestamp.toString())
                    context.header(Headers.HMAC_SIGNATURE, signature)
                    context.header(Headers.HMAC_KEY_PATH, IPC.getIpcKeyPath())

                    proceedWith(payload)
                }
            }
        }
    }
    single { CoroutineScope(SupervisorJob() + Dispatchers.IO) }
    single<DaemonService> {
        UdsDaemonService(
            client = get(),
            lockdownSettingsRepository = get(),
            generalSettingsRepository = get(),
            json = get(),
            scope = get(),
        )
    }
    single {
        LogCoordinator(
            monitoringRepository = get(),
            daemonService = get(),
            scope = get(),
        )
    }
    single<TunnelService> { UdsTunnelService(get()) }
    single<BackendService> {
        UdsBackendService(
            client = get(),
            json = get(),
            lockdownSettingsRepository = get(),
            daemonService = get(),
            scope = get(),
        )
    }

    single {
        TunnelCoordinator(
            tunnelService = get(),
            backendService = get(),
            tunnelRepository = get(),
            settingsRepository = get(),
            dnsSettingsRepository = get(),
            monitoringSettingsRepository = get(),
            proxyRepository = get(),
            lockdownRepository = get(),
            scope = get(),
        )
    }
    single {
        TunnelBackendCoordinator(
            tunnelCoordinator = get(),
            backendService = get(),
            settingsRepository = get(),
            lockdownRepository = get(),
            tunnelRepository = get(),
        )
    }
    single {
        AutoTunnelCoordinator(
            daemonService = get(),
            tunnelCoordinator = get(),
            autoTunnelRepository = get(),
            tunnelRepository = get(),
            settingsRepository = get(),
            dnsSettingsRepository = get(),
            monitoringSettingsRepository = get(),
            proxyRepository = get(),
            lockdownRepository = get(),
            scope = get(),
        )
    }

    single<TunnelImportService> { DefaultTunnelImportService(get()) }
}
