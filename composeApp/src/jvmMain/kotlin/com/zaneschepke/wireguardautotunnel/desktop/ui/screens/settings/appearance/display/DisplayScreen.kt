package com.zaneschepke.wireguardautotunnel.desktop.ui.screens.settings.appearance.display

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.outlined.Check
import androidx.compose.material.icons.outlined.Palette
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import com.zaneschepke.wireguardautotunnel.client.data.model.AccentStyle
import com.zaneschepke.wireguardautotunnel.client.data.model.Theme
import com.zaneschepke.wireguardautotunnel.composeapp.generated.resources.*
import com.zaneschepke.wireguardautotunnel.desktop.ui.common.LocalNavController
import com.zaneschepke.wireguardautotunnel.desktop.ui.common.button.SurfaceRow
import com.zaneschepke.wireguardautotunnel.desktop.ui.common.button.ThemedSwitch
import com.zaneschepke.wireguardautotunnel.desktop.ui.common.dialog.AccentColorPickerDialog
import com.zaneschepke.wireguardautotunnel.desktop.ui.common.dropdown.LabeledDropdown
import com.zaneschepke.wireguardautotunnel.desktop.ui.common.label.GroupLabel
import com.zaneschepke.wireguardautotunnel.desktop.util.asLabel
import com.zaneschepke.wireguardautotunnel.desktop.util.decodeImageBitmap
import com.zaneschepke.wireguardautotunnel.desktop.viewmodel.AppViewModel
import io.github.vinceglb.filekit.PlatformFile
import io.github.vinceglb.filekit.dialogs.FileKitMode
import io.github.vinceglb.filekit.dialogs.FileKitType
import io.github.vinceglb.filekit.dialogs.compose.rememberFilePickerLauncher
import io.github.vinceglb.filekit.readBytes
import kotlin.enums.enumEntries
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import org.jetbrains.compose.resources.stringResource
import org.orbitmvi.orbit.compose.collectAsState

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DisplayScreen(appViewModel: AppViewModel) {

    val navController = LocalNavController.current

    val uiState by appViewModel.collectAsState()

    Scaffold(
        topBar = {
            TopAppBar(
                colors =
                    TopAppBarDefaults.topAppBarColors().copy(containerColor = Color.Transparent),
                title = { Text(stringResource(Res.string.display_theme)) },
                navigationIcon = {
                    IconButton(onClick = { navController.pop() }) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = stringResource(Res.string.back),
                        )
                    }
                },
            )
        }
    ) { padding ->
        Column(
            horizontalAlignment = Alignment.Start,
            verticalArrangement = Arrangement.Top,
            modifier =
                Modifier.fillMaxSize().padding(padding).verticalScroll(rememberScrollState()),
        ) {
            Column {
                GroupLabel(
                    stringResource(Res.string.colors),
                    modifier = Modifier.padding(horizontal = 16.dp),
                )
                SurfaceRow(
                    stringResource(Res.string.use_system_colors),
                    onClick = { appViewModel.setUseSystemColors(!uiState.useSystemColors) },
                    trailing = {
                        ThemedSwitch(
                            checked = uiState.useSystemColors,
                            onClick = { appViewModel.setUseSystemColors(it) },
                        )
                    },
                )
            }
            Column {
                GroupLabel(
                    stringResource(Res.string.themes),
                    modifier = Modifier.padding(horizontal = 16.dp),
                )
                enumEntries<Theme>().forEach {
                    val title =
                        when (it) {
                            Theme.DEFAULT -> stringResource(Res.string._default)
                            Theme.DARK -> stringResource(Res.string.dark)
                            Theme.LIGHT -> stringResource(Res.string.light)
                            Theme.AMOLED -> stringResource(Res.string.amoled)
                            Theme.SYSTEM -> stringResource(Res.string.system)
                        }
                    SurfaceRow(
                        title = title,
                        trailing =
                            if (uiState.theme == it) {
                                {
                                    Icon(
                                        Icons.Outlined.Check,
                                        stringResource(Res.string.selected),
                                        tint = MaterialTheme.colorScheme.primary,
                                    )
                                }
                            } else null,
                        onClick = { appViewModel.setTheme(it) },
                    )
                }
            }
            Column {
                GroupLabel(
                    stringResource(Res.string.accent_color),
                    modifier = Modifier.padding(horizontal = 16.dp),
                )

                val scope = rememberCoroutineScope()
                var showColorPickerDialog by remember { mutableStateOf(false) }
                val imagePickerLauncher =
                    rememberFilePickerLauncher(
                        mode = FileKitMode.Single,
                        type = FileKitType.Image,
                    ) { platformFile: PlatformFile? ->
                        platformFile?.let { file ->
                            scope.launch(Dispatchers.Default) {
                                runCatching { decodeImageBitmap(file.readBytes()) }
                                    .onSuccess { appViewModel.setSeedColorFromImage(it) }
                            }
                        }
                    }

                SurfaceRow(
                    title = stringResource(Res.string.choose_from_image),
                    onClick = { imagePickerLauncher.launch() },
                )
                SurfaceRow(
                    title = stringResource(Res.string.pick_a_color),
                    onClick = { showColorPickerDialog = true },
                    trailing = {
                        Box(
                            modifier =
                                Modifier.size(24.dp)
                                    .background(
                                        color =
                                            uiState.customSeedColor?.let { Color(it) }
                                                ?: MaterialTheme.colorScheme.primary,
                                        shape = CircleShape,
                                    )
                        )
                    },
                )
                if (uiState.customSeedColor != null) {
                    SurfaceRow(
                        title = stringResource(Res.string.reset_to_default_color),
                        onClick = { appViewModel.setCustomSeedColor(null) },
                    )
                }

                if (showColorPickerDialog) {
                    AccentColorPickerDialog(
                        initialColor =
                            uiState.customSeedColor?.let { Color(it) }
                                ?: MaterialTheme.colorScheme.primary,
                        onConfirm = { color ->
                            appViewModel.setCustomSeedColor(color)
                            showColorPickerDialog = false
                        },
                        onDismiss = { showColorPickerDialog = false },
                    )
                }

                LabeledDropdown(
                    title = stringResource(Res.string.palette_style),
                    leading = { Icon(Icons.Outlined.Palette, contentDescription = null) },
                    currentValue = uiState.accentStyle,
                    onSelected = { selected -> selected?.let { appViewModel.setAccentStyle(it) } },
                    options = AccentStyle.entries,
                    optionToString = { (it ?: AccentStyle.TONAL_SPOT).asLabel() },
                )
            }
        }
    }
}
