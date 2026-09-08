package com.zaneschepke.wireguardautotunnel.desktop.viewmodel

import androidx.lifecycle.ViewModel
import com.dokar.sonner.ToastType
import com.zaneschepke.wireguardautotunnel.client.domain.repository.ProxySettingsRepository
import com.zaneschepke.wireguardautotunnel.composeapp.generated.resources.Res
import com.zaneschepke.wireguardautotunnel.composeapp.generated.resources.proxy_settings_saved
import com.zaneschepke.wireguardautotunnel.desktop.ui.sideeffects.AppSideEffect
import com.zaneschepke.wireguardautotunnel.desktop.ui.state.ProxyUiState
import org.jetbrains.compose.resources.getString
import org.orbitmvi.orbit.OrbitContainerHost
import org.orbitmvi.orbit.viewmodel.orbitContainer

class ProxyViewModel(private val proxySettingsRepository: ProxySettingsRepository) :
    OrbitContainerHost<ProxyUiState, ProxyUiState, AppSideEffect>, ViewModel() {

    override val container =
        orbitContainer<ProxyUiState, AppSideEffect>(ProxyUiState()) {
            intent {
                proxySettingsRepository.flow.collect { settings ->
                    reduce {
                        state.copy(
                            isLoaded = true,
                            draft = settings,
                            saved = settings,
                            socksBindAddress = settings.socks5ProxyBindAddress.orEmpty(),
                            httpBindAddress = settings.httpProxyBindAddress.orEmpty(),
                            username = settings.proxyUsername.orEmpty(),
                            password = settings.proxyPassword.orEmpty(),
                        )
                    }
                }
            }
        }

    fun onSocks5Enabled(enabled: Boolean) = intent {
        reduce { state.copy(draft = state.draft.copy(socks5ProxyEnabled = enabled)) }
    }

    fun onHttpEnabled(enabled: Boolean) = intent {
        reduce { state.copy(draft = state.draft.copy(httpProxyEnabled = enabled)) }
    }

    fun onSocksBindChanged(value: String) = intent {
        reduce {
            state.copy(
                socksBindAddress = value,
                draft = state.draft.copy(socks5ProxyBindAddress = value.ifBlank { null }),
            )
        }
    }

    fun onHttpBindChanged(value: String) = intent {
        reduce {
            state.copy(
                httpBindAddress = value,
                draft = state.draft.copy(httpProxyBindAddress = value.ifBlank { null }),
            )
        }
    }

    fun onUsernameChanged(value: String) = intent {
        reduce {
            state.copy(
                username = value,
                draft = state.draft.copy(proxyUsername = value.ifBlank { null }),
            )
        }
    }

    fun onPasswordChanged(value: String) = intent {
        reduce {
            state.copy(
                password = value,
                draft = state.draft.copy(proxyPassword = value.ifBlank { null }),
            )
        }
    }

    fun onPasswordVisibilityChanged(visible: Boolean) = intent {
        reduce { state.copy(passwordVisible = visible) }
    }

    fun save() = intent {
        proxySettingsRepository.upsert(state.draft)
        reduce { state.copy(saved = state.draft) }
        postSideEffect(AppSideEffect.Toast(getString(Res.string.proxy_settings_saved), ToastType.Success))
    }
}
