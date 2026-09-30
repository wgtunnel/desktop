package com.zaneschepke.wireguardautotunnel.desktop.ui.screens.support

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.Launch
import androidx.compose.material.icons.outlined.Balance
import androidx.compose.material.icons.outlined.Book
import androidx.compose.material.icons.outlined.Favorite
import androidx.compose.material.icons.outlined.InstallDesktop
import androidx.compose.material.icons.outlined.Mail
import androidx.compose.material.icons.outlined.Memory
import androidx.compose.material.icons.outlined.Policy
import androidx.compose.material.icons.outlined.Terminal
import androidx.compose.material.icons.outlined.Web
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalClipboard
import androidx.compose.ui.platform.LocalUriHandler
import androidx.compose.ui.unit.dp
import com.dokar.sonner.Toast
import com.dokar.sonner.ToastType
import com.zaneschepke.wireguardautotunnel.composeApp.BuildConfig
import com.zaneschepke.wireguardautotunnel.composeapp.generated.resources.Res
import com.zaneschepke.wireguardautotunnel.composeapp.generated.resources.about
import com.zaneschepke.wireguardautotunnel.composeapp.generated.resources.app_version
import com.zaneschepke.wireguardautotunnel.composeapp.generated.resources.check_for_update
import com.zaneschepke.wireguardautotunnel.composeapp.generated.resources.checking_for_updates
import com.zaneschepke.wireguardautotunnel.composeapp.generated.resources.contact
import com.zaneschepke.wireguardautotunnel.composeapp.generated.resources.copied_to_clipboard
import com.zaneschepke.wireguardautotunnel.composeapp.generated.resources.docs_description
import com.zaneschepke.wireguardautotunnel.composeapp.generated.resources.docs_url
import com.zaneschepke.wireguardautotunnel.composeapp.generated.resources.donate
import com.zaneschepke.wireguardautotunnel.composeapp.generated.resources.download_update
import com.zaneschepke.wireguardautotunnel.composeapp.generated.resources.downloading_update
import com.zaneschepke.wireguardautotunnel.composeapp.generated.resources.downloading_update_delta
import com.zaneschepke.wireguardautotunnel.composeapp.generated.resources.email_description
import com.zaneschepke.wireguardautotunnel.composeapp.generated.resources.email_subject
import com.zaneschepke.wireguardautotunnel.composeapp.generated.resources.github
import com.zaneschepke.wireguardautotunnel.composeapp.generated.resources.github_url
import com.zaneschepke.wireguardautotunnel.composeapp.generated.resources.installing_update
import com.zaneschepke.wireguardautotunnel.composeapp.generated.resources.join_matrix
import com.zaneschepke.wireguardautotunnel.composeapp.generated.resources.join_telegram
import com.zaneschepke.wireguardautotunnel.composeapp.generated.resources.licenses
import com.zaneschepke.wireguardautotunnel.composeapp.generated.resources.matrix
import com.zaneschepke.wireguardautotunnel.composeapp.generated.resources.matrix_url
import com.zaneschepke.wireguardautotunnel.composeapp.generated.resources.my_email
import com.zaneschepke.wireguardautotunnel.composeapp.generated.resources.open_issue
import com.zaneschepke.wireguardautotunnel.composeapp.generated.resources.privacy_policy
import com.zaneschepke.wireguardautotunnel.composeapp.generated.resources.privacy_policy_url
import com.zaneschepke.wireguardautotunnel.composeapp.generated.resources.ready_to_install_version
import com.zaneschepke.wireguardautotunnel.composeapp.generated.resources.resources
import com.zaneschepke.wireguardautotunnel.composeapp.generated.resources.restart_and_install
import com.zaneschepke.wireguardautotunnel.composeapp.generated.resources.support
import com.zaneschepke.wireguardautotunnel.composeapp.generated.resources.system_information
import com.zaneschepke.wireguardautotunnel.composeapp.generated.resources.system_information_copied
import com.zaneschepke.wireguardautotunnel.composeapp.generated.resources.telegram
import com.zaneschepke.wireguardautotunnel.composeapp.generated.resources.telegram_url
import com.zaneschepke.wireguardautotunnel.composeapp.generated.resources.thank_you
import com.zaneschepke.wireguardautotunnel.composeapp.generated.resources.update_available_version
import com.zaneschepke.wireguardautotunnel.composeapp.generated.resources.update_progress_template
import com.zaneschepke.wireguardautotunnel.composeapp.generated.resources.website
import com.zaneschepke.wireguardautotunnel.composeapp.generated.resources.website_url
import com.zaneschepke.wireguardautotunnel.core.profile.AppVariant
import com.zaneschepke.wireguardautotunnel.desktop.ui.common.LocalNavController
import com.zaneschepke.wireguardautotunnel.desktop.ui.common.LocalToaster
import com.zaneschepke.wireguardautotunnel.desktop.ui.common.button.SurfaceRow
import com.zaneschepke.wireguardautotunnel.desktop.ui.common.label.GroupLabel
import com.zaneschepke.wireguardautotunnel.desktop.ui.common.scroll.ScrollableColumn
import com.zaneschepke.wireguardautotunnel.desktop.ui.common.text.DescriptionText
import com.zaneschepke.wireguardautotunnel.desktop.ui.navigation.Route
import com.zaneschepke.wireguardautotunnel.desktop.update.UpdateState
import com.zaneschepke.wireguardautotunnel.desktop.util.DesktopUtils
import com.zaneschepke.wireguardautotunnel.desktop.util.buildSystemInfoReport
import com.zaneschepke.wireguardautotunnel.desktop.util.formatFileSize
import com.zaneschepke.wireguardautotunnel.desktop.util.toClipEntry
import com.zaneschepke.wireguardautotunnel.desktop.viewmodel.SupportViewModel
import kotlinx.coroutines.launch
import org.jetbrains.compose.resources.stringResource
import org.jetbrains.compose.resources.vectorResource
import org.koin.compose.viewmodel.koinViewModel
import org.orbitmvi.orbit.compose.collectAsState
import org.orbitmvi.orbit.compose.collectSideEffect

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SupportScreen(viewModel: SupportViewModel = koinViewModel()) {
    val navController = LocalNavController.current
    val uriHandler = LocalUriHandler.current
    val clipboard = LocalClipboard.current
    val toaster = LocalToaster.current
    val scope = rememberCoroutineScope()
    val uiState by viewModel.collectAsState()

    viewModel.collectSideEffect { sideEffect ->
        when (sideEffect) {
            is com.zaneschepke.wireguardautotunnel.desktop.ui.sideeffects.AppSideEffect.Toast ->
                toaster.show(Toast(sideEffect.message, sideEffect.type))
            else -> Unit
        }
    }

    val appVersion =
        when (AppVariant.current) {
            AppVariant.DEBUG -> "${BuildConfig.APP_VERSION}-debug"
            else -> BuildConfig.APP_VERSION
        }
    val emailAddress = stringResource(Res.string.my_email)
    val emailSubject = stringResource(Res.string.email_subject)
    val copiedMessage = stringResource(Res.string.copied_to_clipboard, appVersion)

    Scaffold(topBar = { TopAppBar(title = { Text(stringResource(Res.string.support)) }) }) { padding
        ->
        ScrollableColumn(
            modifier = Modifier.fillMaxSize().padding(padding),
            horizontalAlignment = Alignment.Start,
            verticalArrangement = Arrangement.spacedBy(16.dp, Alignment.Top),
        ) {
            GroupLabel(
                stringResource(Res.string.thank_you),
                modifier = Modifier.padding(horizontal = 16.dp),
                MaterialTheme.colorScheme.onSurface,
            )
            Column {
                GroupLabel(
                    stringResource(Res.string.resources),
                    Modifier.padding(horizontal = 16.dp),
                )
                SurfaceRow(
                    leading = { Icon(Icons.Outlined.Favorite, contentDescription = null) },
                    title = stringResource(Res.string.donate),
                    onClick = { navController.push(Route.Donate) },
                )
                val docsUrl = stringResource(Res.string.docs_url)
                SurfaceRow(
                    stringResource(Res.string.docs_description),
                    onClick = { uriHandler.openUri(docsUrl) },
                    leading = { Icon(Icons.Outlined.Book, contentDescription = null) },
                    trailing = { Icon(Icons.AutoMirrored.Outlined.Launch, null) },
                )
                val websiteUrl = stringResource(Res.string.website_url)
                SurfaceRow(
                    stringResource(Res.string.website),
                    onClick = { uriHandler.openUri(websiteUrl) },
                    leading = { Icon(Icons.Outlined.Web, contentDescription = null) },
                    trailing = { Icon(Icons.AutoMirrored.Outlined.Launch, null) },
                )
                SurfaceRow(
                    leading = { Icon(Icons.Outlined.Balance, contentDescription = null) },
                    title = stringResource(Res.string.licenses),
                    onClick = { navController.push(Route.License) },
                )
                val privacyPolicyUrl = stringResource(Res.string.privacy_policy_url)
                SurfaceRow(
                    leading = { Icon(Icons.Outlined.Policy, contentDescription = null) },
                    title = stringResource(Res.string.privacy_policy),
                    trailing = { Icon(Icons.AutoMirrored.Outlined.Launch, null) },
                    onClick = { uriHandler.openUri(privacyPolicyUrl) },
                )
            }
            Column {
                GroupLabel(
                    stringResource(Res.string.contact),
                    modifier = Modifier.padding(horizontal = 16.dp),
                )
                val matrixUrl = stringResource(Res.string.matrix_url)
                SurfaceRow(
                    leading = {
                        Icon(
                            vectorResource(Res.drawable.matrix),
                            contentDescription = null,
                            modifier = Modifier.size(24.dp),
                        )
                    },
                    title = stringResource(Res.string.join_matrix),
                    trailing = { Icon(Icons.AutoMirrored.Outlined.Launch, null) },
                    onClick = { uriHandler.openUri(matrixUrl) },
                )
                val telegramUrl = stringResource(Res.string.telegram_url)
                SurfaceRow(
                    leading = {
                        Icon(
                            vectorResource(Res.drawable.telegram),
                            contentDescription = null,
                            modifier = Modifier.size(24.dp),
                        )
                    },
                    title = stringResource(Res.string.join_telegram),
                    trailing = { Icon(Icons.AutoMirrored.Outlined.Launch, null) },
                    onClick = { uriHandler.openUri(telegramUrl) },
                )
                val githubUrl = stringResource(Res.string.github_url)
                SurfaceRow(
                    leading = {
                        Icon(
                            vectorResource(Res.drawable.github),
                            contentDescription = null,
                            modifier = Modifier.size(24.dp),
                        )
                    },
                    title = stringResource(Res.string.open_issue),
                    trailing = { Icon(Icons.AutoMirrored.Outlined.Launch, null) },
                    onClick = { uriHandler.openUri(githubUrl) },
                )
                SurfaceRow(
                    leading = { Icon(Icons.Outlined.Mail, contentDescription = null) },
                    title = stringResource(Res.string.email_description),
                    trailing = { Icon(Icons.AutoMirrored.Outlined.Launch, null) },
                    onClick = { DesktopUtils.launchEmailClient(emailAddress, emailSubject, "") },
                )
            }
            Column {
                GroupLabel(
                    stringResource(Res.string.about),
                    modifier = Modifier.padding(horizontal = 16.dp),
                )
                SurfaceRow(
                    leading = { Icon(Icons.Outlined.Memory, contentDescription = null) },
                    title = stringResource(Res.string.app_version),
                    description = { DescriptionText(appVersion) },
                    onClick = {
                        val message = copiedMessage
                        scope.launch { clipboard.setClipEntry(appVersion.toClipEntry()) }
                        toaster.show(Toast(message, ToastType.Success))
                    },
                )
                val systemInfoReport = remember { buildSystemInfoReport() }
                val systemInfoCopiedMessage = stringResource(Res.string.system_information_copied)
                SurfaceRow(
                    leading = { Icon(Icons.Outlined.Terminal, contentDescription = null) },
                    title = stringResource(Res.string.system_information),
                    description = { DescriptionText(systemInfoReport) },
                    onClick = {
                        scope.launch { clipboard.setClipEntry(systemInfoReport.toClipEntry()) }
                        toaster.show(Toast(systemInfoCopiedMessage, ToastType.Success))
                    },
                )
                if (uiState.updateSupported) {
                    val state = uiState.updateState
                    val updateTitle =
                        when (state) {
                            is UpdateState.Available -> stringResource(Res.string.download_update)
                            is UpdateState.Downloading ->
                                stringResource(
                                    if (state.isDifferential) Res.string.downloading_update_delta
                                    else Res.string.downloading_update
                                )
                            is UpdateState.ReadyToInstall ->
                                stringResource(Res.string.restart_and_install)
                            UpdateState.Installing -> stringResource(Res.string.installing_update)
                            else -> stringResource(Res.string.check_for_update)
                        }
                    val updateDescription: (@Composable () -> Unit)? =
                        when (state) {
                            UpdateState.Checking -> {
                                { DescriptionText(stringResource(Res.string.checking_for_updates)) }
                            }
                            is UpdateState.Available -> {
                                {
                                    DescriptionText(
                                        stringResource(
                                            Res.string.update_available_version,
                                            state.info.version,
                                        )
                                    )
                                }
                            }
                            is UpdateState.Downloading -> {
                                {
                                    Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                                        LinearProgressIndicator(
                                            progress = {
                                                (state.percent / 100.0).toFloat().coerceIn(0f, 1f)
                                            },
                                            modifier = Modifier.fillMaxWidth(),
                                        )
                                        DescriptionText(
                                            stringResource(
                                                Res.string.update_progress_template,
                                                state.bytesDownloaded.formatFileSize(),
                                                state.totalBytes.formatFileSize(),
                                                state.percent.toInt(),
                                            )
                                        )
                                    }
                                }
                            }
                            is UpdateState.ReadyToInstall -> {
                                {
                                    DescriptionText(
                                        stringResource(
                                            Res.string.ready_to_install_version,
                                            state.version,
                                        )
                                    )
                                }
                            }
                            is UpdateState.Failed -> {
                                { DescriptionText(state.message) }
                            }
                            else -> null
                        }
                    SurfaceRow(
                        leading = {
                            Icon(Icons.Outlined.InstallDesktop, contentDescription = null)
                        },
                        title = updateTitle,
                        description = updateDescription,
                        enabled =
                            state !is UpdateState.Checking &&
                                state !is UpdateState.Downloading &&
                                state !is UpdateState.Installing,
                        onClick = {
                            when (state) {
                                is UpdateState.Available -> viewModel.onDownloadUpdate(state.info)
                                is UpdateState.ReadyToInstall ->
                                    viewModel.onInstallUpdate(state.file)
                                else -> viewModel.onCheckForUpdate()
                            }
                        },
                    )
                }
            }
        }
    }
}
