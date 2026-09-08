package com.zaneschepke.wireguardautotunnel.desktop.viewmodel

import androidx.lifecycle.ViewModel
import com.dokar.sonner.ToastType
import com.zaneschepke.wireguardautotunnel.client.domain.enums.BootstrapDnsProtocol
import com.zaneschepke.wireguardautotunnel.client.domain.enums.SplitDnsSuffixTarget
import com.zaneschepke.wireguardautotunnel.client.domain.enums.TransitDnsPolicy
import com.zaneschepke.wireguardautotunnel.client.domain.enums.TunnelDnsMode
import com.zaneschepke.wireguardautotunnel.client.domain.enums.TunnelDnsProtocol
import com.zaneschepke.wireguardautotunnel.client.domain.repository.DnsSettingsRepository
import com.zaneschepke.wireguardautotunnel.composeapp.generated.resources.Res
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
        reduce { state.copy(draft = state.draft.copy(tunnelDnsMode = mode)) }
    }

    fun setTunnelDnsProtocol(protocol: TunnelDnsProtocol) = intent {
        reduce { state.copy(draft = state.draft.copy(tunnelDnsProtocol = protocol)) }
    }

    fun setTunnelDnsEndpoint(endpoint: String) = intent {
        reduce {
            state.copy(draft = state.draft.copy(tunnelDnsEndpoint = endpoint.ifBlank { null }))
        }
    }

    fun setBootstrapDnsProtocol(protocol: BootstrapDnsProtocol) = intent {
        reduce { state.copy(draft = state.draft.copy(bootstrapDnsProtocol = protocol)) }
    }

    fun setBootstrapDnsEndpoint(endpoint: String) = intent {
        reduce {
            state.copy(draft = state.draft.copy(bootstrapDnsEndpoint = endpoint.ifBlank { null }))
        }
    }

    fun setLocalSuffixes(suffixes: String) = intent {
        reduce { state.copy(draft = state.draft.copy(localSuffixes = suffixes.ifBlank { null })) }
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
        dnsSettingsRepository.upsert(state.draft)
        reduce { state.copy(saved = state.draft) }
        postSideEffect(AppSideEffect.Toast(getString(Res.string.dns_settings_saved), ToastType.Success))
    }
}
