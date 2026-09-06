package com.zaneschepke.wireguardautotunnel.desktop.ui.common.scaffold

import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.outlined.Save
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import com.zaneschepke.wireguardautotunnel.composeapp.generated.resources.Res
import com.zaneschepke.wireguardautotunnel.composeapp.generated.resources.back
import com.zaneschepke.wireguardautotunnel.composeapp.generated.resources.save
import com.zaneschepke.wireguardautotunnel.desktop.ui.common.LocalNavController
import org.jetbrains.compose.resources.stringResource

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun NestedSettingsScaffold(
    title: String? = null,
    modifier: Modifier = Modifier,
    showSave: Boolean = false,
    onSave: (() -> Unit)? = null,
    titleContent: @Composable (() -> Unit)? = null,
    actions: @Composable (() -> Unit)? = null,
    content: @Composable (PaddingValues) -> Unit,
) {
    val navController = LocalNavController.current
    Scaffold(
        modifier = modifier,
        topBar = {
            TopAppBar(
                title = { titleContent?.invoke() ?: Text(title.orEmpty()) },
                navigationIcon = {
                    IconButton(onClick = { navController.pop() }) {
                        Icon(
                            Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = stringResource(Res.string.back),
                        )
                    }
                },
                actions = {
                    if (showSave && onSave != null) {
                        IconButton(onClick = onSave) {
                            Icon(
                                Icons.Outlined.Save,
                                contentDescription = stringResource(Res.string.save),
                            )
                        }
                    }
                    actions?.invoke()
                },
            )
        },
        content = content,
    )
}
