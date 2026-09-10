package com.zaneschepke.wireguardautotunnel.desktop.viewmodel

import androidx.lifecycle.ViewModel
import com.dokar.sonner.ToastType
import com.wgtunnel.backend.model.dns.DnsValidationError
import com.wgtunnel.backend.model.dns.DnsValidator
import com.zaneschepke.wireguardautotunnel.client.domain.enums.BootstrapDnsProtocol
import com.zaneschepke.wireguardautotunnel.client.domain.enums.SplitDnsSuffixTarget
import com.zaneschepke.wireguardautotunnel.client.domain.enums.TransitDnsPolicy
import com.zaneschepke.wireguardautotunnel.client.domain.enums.TunnelDnsMode
import com.zaneschepke.wireguardautotunnel.client.domain.enums.TunnelDnsProtocol
import com.zaneschepke.wireguardautotunnel.client.domain.repository.DnsSettingsRepository
import com.zaneschepke.wireguardautotunnel.composeapp.generated.resources.Res
import com.zaneschepke.wireguardautotunnel.composeapp.generated.resources.dns_error_empty
import com.zaneschepke.wireguardautotunnel.composeapp.generated.resources.dns_error_invalid_host
import com.zaneschepke.wireguardautotunnel.composeapp.generated.resources.dns_error_invalid_ip_or_host
import com.zaneschepke.wireguardautotunnel.composeapp.generated.resources.dns_error_invalid_port
import com.zaneschepke.wireguardautotunnel.composeapp.generated.resources.dns_error_invalid_scheme
import com.zaneschepke.wireguardautotunnel.composeapp.generated.resources.dns_error_invalid_url
import com.zaneschepke.wireguardautotunnel.composeapp.generated.resources.dns_settings_saved
import com.zaneschepke.wireguardautotunnel.desktop.ui.sideeffects.AppSideEffect
import com.zaneschepke.wireguardautotunnel.desktop.ui.state.DnsUiState
import org.jetbrains.compose.resources.getString
import org.orbitmvi.orbit.OrbitContainerHost
import org.orbitmvi.orbit.viewmodel.orbitContainer

class DnsViewModel(private val dnsSettingsRepository: DnsSettingsRepository) :
    OrbitContainerHost<DnsUiState, DnsUiState, AppSideEffect>, ViewModel() {

    override val container =
        orbitContainer<DnsUiState, AppSideEffect>(DnsUiState()) {
            intent {
                dnsSettingsRepository.flow.collect { settings ->
                    reduce { state.copy(isLoaded = true, draft = settings, saved = settings) }
                }
            }
        }

    fun setTunnelDnsMode(mode: TunnelDnsMode) = intent {
        reduce {
            state.copy(draft = state.draft.copy(tunnelDnsMode = mode), localSuffixesError = null)
        }
    }

    fun setTunnelDnsProtocol(protocol: TunnelDnsProtocol) = intent {
        reduce {
            state.copy(
                draft = state.draft.copy(tunnelDnsProtocol = protocol),
                tunnelEndpointError = null,
            )
        }
    }

    fun setTunnelDnsEndpoint(endpoint: String) = intent {
        reduce {
            state.copy(
                draft = state.draft.copy(tunnelDnsEndpoint = endpoint.ifBlank { null }),
                tunnelEndpointError = null,
            )
        }
    }

    fun setBootstrapDnsProtocol(protocol: BootstrapDnsProtocol) = intent {
        reduce {
            state.copy(
                draft = state.draft.copy(bootstrapDnsProtocol = protocol),
                bootstrapEndpointError = null,
            )
        }
    }

    fun setBootstrapDnsEndpoint(endpoint: String) = intent {
        reduce {
            state.copy(
                draft = state.draft.copy(bootstrapDnsEndpoint = endpoint.ifBlank { null }),
                bootstrapEndpointError = null,
            )
        }
    }

    fun setLocalSuffixes(suffixes: String) = intent {
        reduce {
            state.copy(
                draft = state.draft.copy(localSuffixes = suffixes.ifBlank { null }),
                localSuffixesError = null,
            )
        }
    }

    fun setUseTunnelDnsServersInSplit(enabled: Boolean) = intent {
        reduce { state.copy(draft = state.draft.copy(useTunnelDnsServersInSplit = enabled)) }
    }

    fun setGlobalTunnelConfigDnsEnabled(enabled: Boolean) = intent {
        reduce { state.copy(draft = state.draft.copy(isGlobalTunnelConfigDnsEnabled = enabled)) }
    }

    fun setForeignDnsPolicy(policy: TransitDnsPolicy) = intent {
        reduce { state.copy(draft = state.draft.copy(transitDnsPolicy = policy)) }
    }

    fun setSplitSuffixTarget(target: SplitDnsSuffixTarget) = intent {
        reduce { state.copy(draft = state.draft.copy(splitSuffixTarget = target)) }
    }

    fun save() = intent {
        val settings = state.draft

        when (
            val r =
                DnsValidator.validateEndpoint(
                    settings.bootstrapDnsProtocol.toCore(),
                    settings.bootstrapDnsEndpoint,
                )
        ) {
            is DnsValidator.Result.Invalid -> {
                reduce { state.copy(bootstrapEndpointError = r.error) }
                postSideEffect(AppSideEffect.Toast(r.error.asMessage(), ToastType.Error))
                return@intent
            }
            DnsValidator.Result.Valid -> Unit
        }

        val usesTunnelDns =
            settings.tunnelDnsMode.isSplitMode() &&
                settings.tunnelDnsProtocol == TunnelDnsProtocol.Plain &&
                settings.useTunnelDnsServersInSplit

        if (
            (settings.tunnelDnsMode == TunnelDnsMode.Encrypted ||
                settings.tunnelDnsMode.isSplitMode()) && !usesTunnelDns
        ) {
            when (
                val r =
                    DnsValidator.validateEndpoint(
                        settings.tunnelDnsProtocol.toCore(),
                        settings.tunnelDnsEndpoint,
                    )
            ) {
                is DnsValidator.Result.Invalid -> {
                    reduce { state.copy(tunnelEndpointError = r.error) }
                    postSideEffect(AppSideEffect.Toast(r.error.asMessage(), ToastType.Error))
                    return@intent
                }
                DnsValidator.Result.Valid -> Unit
            }
        }

        if (settings.tunnelDnsMode.isSplitMode()) {
            when (
                val r =
                    DnsValidator.validateLocalSuffixes(
                        requiresSuffixes = true,
                        input = settings.localSuffixes,
                    )
            ) {
                is DnsValidator.Result.Invalid -> {
                    reduce { state.copy(localSuffixesError = r.error) }
                    postSideEffect(AppSideEffect.Toast(r.error.asMessage(), ToastType.Error))
                    return@intent
                }
                DnsValidator.Result.Valid -> Unit
            }
        }

        val updated =
            settings.copy(
                bootstrapDnsEndpoint =
                    DnsValidator.normalizeEndpoint(
                            settings.bootstrapDnsProtocol.toCore(),
                            settings.bootstrapDnsEndpoint,
                        )
                        .ifEmpty { null },
                tunnelDnsEndpoint =
                    when (settings.tunnelDnsMode) {
                        TunnelDnsMode.Encrypted,
                        TunnelDnsMode.Split ->
                            if (!usesTunnelDns) {
                                DnsValidator.normalizeEndpoint(
                                    settings.tunnelDnsProtocol.toCore(),
                                    settings.tunnelDnsEndpoint,
                                )
                            } else {
                                null
                            }
                        else -> null
                    },
                localSuffixes =
                    when {
                        settings.tunnelDnsMode.isSplitMode() ->
                            DnsValidator.normalizeLocalSuffixes(settings.localSuffixes).ifEmpty {
                                null
                            }
                        else -> null
                    },
            )

        dnsSettingsRepository.upsert(updated)
        reduce { state.copy(saved = updated, draft = updated) }
        postSideEffect(
            AppSideEffect.Toast(getString(Res.string.dns_settings_saved), ToastType.Success)
        )
    }

    private suspend fun DnsValidationError.asMessage(): String =
        when (this) {
            DnsValidationError.Empty -> getString(Res.string.dns_error_empty)
            DnsValidationError.InvalidUrl -> getString(Res.string.dns_error_invalid_url)
            DnsValidationError.InvalidScheme -> getString(Res.string.dns_error_invalid_scheme)
            DnsValidationError.InvalidHost -> getString(Res.string.dns_error_invalid_host)
            DnsValidationError.InvalidPort -> getString(Res.string.dns_error_invalid_port)
            DnsValidationError.InvalidIpOrHost -> getString(Res.string.dns_error_invalid_ip_or_host)
        }
}
