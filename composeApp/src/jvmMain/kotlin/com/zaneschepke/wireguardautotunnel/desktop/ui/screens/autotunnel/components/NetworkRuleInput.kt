package com.zaneschepke.wireguardautotunnel.desktop.ui.screens.autotunnel.components

import androidx.compose.animation.animateContentSize
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.outlined.Add
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.unit.dp
import com.zaneschepke.wireguardautotunnel.composeapp.generated.resources.Res
import com.zaneschepke.wireguardautotunnel.composeapp.generated.resources.add
import com.zaneschepke.wireguardautotunnel.desktop.ui.common.button.ClickableIconButton
import com.zaneschepke.wireguardautotunnel.desktop.ui.common.textbox.ConfigurationTextBox
import org.jetbrains.compose.resources.stringResource

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun NetworkRuleInput(
    inputTitle: String,
    placeholder: String,
    rules: List<String>,
    onDelete: (String) -> Unit,
    currentText: String,
    onValueChange: (String) -> Unit,
    onSave: (String) -> Unit,
    modifier: Modifier = Modifier,
    supportingContent: @Composable (() -> Unit)? = null,
) {
    Column(
        modifier =
            modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 8.dp)
                .animateContentSize(),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        if (rules.isNotEmpty()) {
            FlowRow(
                modifier = Modifier.fillMaxWidth().padding(bottom = 4.dp),
                horizontalArrangement = Arrangement.spacedBy(6.dp),
                verticalArrangement = Arrangement.spacedBy(6.dp),
            ) {
                rules.forEach { rule ->
                    ClickableIconButton(
                        onClick = { onDelete(rule) },
                        text = rule,
                        icon = Icons.Filled.Close,
                    )
                }
            }
        }
        ConfigurationTextBox(
            value = currentText,
            onValueChange = onValueChange,
            label = inputTitle,
            hint = placeholder,
            keyboardOptions = KeyboardOptions(imeAction = ImeAction.Done),
            keyboardActions = KeyboardActions(onDone = { onSave(currentText) }),
            trailing = {
                androidx.compose.material3.IconButton(onClick = { onSave(currentText) }) {
                    androidx.compose.material3.Icon(
                        Icons.Outlined.Add,
                        contentDescription = stringResource(Res.string.add),
                    )
                }
            },
        )
        supportingContent?.invoke()
    }
}
