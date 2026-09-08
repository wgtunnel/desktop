package com.zaneschepke.wireguardautotunnel.desktop.viewmodel

import androidx.lifecycle.ViewModel
import com.dokar.sonner.ToastType
import com.wgtunnel.parser.Config
import com.wgtunnel.parser.ConfigQuickInclude
import com.wgtunnel.parser.ConfigReconciler
import com.zaneschepke.wireguardautotunnel.client.domain.model.TunnelConfig
import com.zaneschepke.wireguardautotunnel.client.domain.repository.DnsSettingsRepository
import com.zaneschepke.wireguardautotunnel.client.domain.repository.GeneralSettingRepository
import com.zaneschepke.wireguardautotunnel.client.domain.repository.TunnelRepository
import com.zaneschepke.wireguardautotunnel.composeapp.generated.resources.Res
import com.zaneschepke.wireguardautotunnel.composeapp.generated.resources.config_changes_saved
import com.zaneschepke.wireguardautotunnel.desktop.ui.sideeffects.AppSideEffect
import com.zaneschepke.wireguardautotunnel.desktop.ui.state.GlobalConfigUiState
import com.zaneschepke.wireguardautotunnel.desktop.util.toConfigErrorMessage
import kotlinx.coroutines.flow.combine
import org.jetbrains.compose.resources.getString
import org.orbitmvi.orbit.OrbitContainerHost
import org.orbitmvi.orbit.viewmodel.orbitContainer

class GlobalConfigViewModel(
    private val tunnelRepository: TunnelRepository,
    private val dnsSettingsRepository: DnsSettingsRepository,
    private val settingsRepository: GeneralSettingRepository,
) : OrbitContainerHost<GlobalConfigUiState, GlobalConfigUiState, AppSideEffect>, ViewModel() {

    override val container =
        orbitContainer<GlobalConfigUiState, AppSideEffect>(
            GlobalConfigUiState(),
            buildSettings = { repeatOnSubscribedStopTimeout = 5000L },
        ) {
            intent {
                combine(
                        tunnelRepository.globalTunnelFlow,
                        dnsSettingsRepository.flow,
                        settingsRepository.flow,
                    ) { global, dns, settings ->
                        Triple(
                            global,
                            dns.isGlobalTunnelConfigDnsEnabled,
                            settings.isGlobalAmneziaEnabled,
                        )
                    }
                    .collect { (global, dnsEnabled, amneziaEnabled) ->
                        reduce {
                            if (!state.isLoaded || !state.isDirty) {
                                val stored = global ?: state.stored
                                state.copy(
                                    isLoaded = global != null,
                                    stored = stored,
                                    dnsEnabled = dnsEnabled,
                                    amneziaEnabled = amneziaEnabled,
                                    storedDnsEnabled = dnsEnabled,
                                    storedAmneziaEnabled = amneziaEnabled,
                                    editorText = editorTextFor(stored, dnsEnabled, amneziaEnabled),
                                )
                            } else {
                                state.copy(
                                    stored = global ?: state.stored,
                                    storedDnsEnabled = dnsEnabled,
                                    storedAmneziaEnabled = amneziaEnabled,
                                )
                            }
                        }
                    }
            }
        }

    fun setDnsEnabled(enabled: Boolean) = applyToggle(dnsEnabled = enabled, amneziaEnabled = null)

    fun setAmneziaEnabled(enabled: Boolean) =
        applyToggle(dnsEnabled = null, amneziaEnabled = enabled)

    fun onEditorChange(text: String) = intent { reduce { state.copy(editorText = text) } }

    fun saveChanges() = intent {
        val storedConfig = runCatching {
            state.stored.asConfig()
        }
            .getOrElse {
                postSideEffect(AppSideEffect.Toast(it.toConfigErrorMessage(), ToastType.Error))
                return@intent
            }

        val overlay =
            if (state.showEditor) {
                runCatching { Config.parseInterfaceQuickString(state.editorText) }
                    .getOrElse {
                        postSideEffect(
                            AppSideEffect.Toast(it.toConfigErrorMessage(), ToastType.Error)
                        )
                        return@intent
                    }
            } else {
                storedConfig
            }

        val reconciled =
            ConfigReconciler.reconcileConfig(
                storedConfig,
                overlay,
                ConfigReconciler.ConfigReconcilePolicy(
                    dns = state.dnsEnabled,
                    splitTunnel = false,
                    amnezia = state.amneziaEnabled,
                ),
            )

        runCatching { reconciled.validate() }
            .onFailure {
                postSideEffect(AppSideEffect.Toast(it.toConfigErrorMessage(), ToastType.Error))
                return@intent
            }

        val saved = state.stored.copy(quickConfig = reconciled.asQuickString())
        tunnelRepository.save(saved)
        dnsSettingsRepository.upsert(
            dnsSettingsRepository.get().copy(isGlobalTunnelConfigDnsEnabled = state.dnsEnabled)
        )
        settingsRepository.updateGlobalAmneziaEnabled(state.amneziaEnabled)
        reduce {
            state.copy(
                stored = saved,
                storedDnsEnabled = state.dnsEnabled,
                storedAmneziaEnabled = state.amneziaEnabled,
                editorText = editorTextFor(saved, state.dnsEnabled, state.amneziaEnabled),
            )
        }
        postSideEffect(
            AppSideEffect.Toast(getString(Res.string.config_changes_saved), ToastType.Success)
        )
    }

    private fun applyToggle(dnsEnabled: Boolean?, amneziaEnabled: Boolean?) = intent {
        val nextDns = dnsEnabled ?: state.dnsEnabled
        val nextAmnezia = amneziaEnabled ?: state.amneziaEnabled
        val storedConfig = runCatching {
            state.stored.asConfig()
        }
            .getOrElse {
                postSideEffect(AppSideEffect.Toast(it.toConfigErrorMessage(), ToastType.Error))
                return@intent
            }
        val overlay = runCatching {
            Config.parseInterfaceQuickString(state.editorText)
        }
            .getOrElse {
                postSideEffect(AppSideEffect.Toast(it.toConfigErrorMessage(), ToastType.Error))
                return@intent
            }
        val merged =
            ConfigReconciler.reconcileConfig(
                storedConfig,
                overlay,
                ConfigReconciler.ConfigReconcilePolicy(
                    dns = state.dnsEnabled,
                    splitTunnel = false,
                    amnezia = state.amneziaEnabled,
                ),
            )
        reduce {
            state.copy(
                dnsEnabled = nextDns,
                amneziaEnabled = nextAmnezia,
                editorText = merged.asQuickString(ConfigQuickInclude.global(nextDns, nextAmnezia)),
            )
        }
    }

    private fun editorTextFor(
        stored: TunnelConfig,
        dnsEnabled: Boolean,
        amneziaEnabled: Boolean,
    ): String {
        if (!dnsEnabled && !amneziaEnabled) return ""
        val config = runCatching { stored.asConfig() }.getOrNull() ?: return ""
        return config.asQuickString(ConfigQuickInclude.global(dnsEnabled, amneziaEnabled))
    }
}
