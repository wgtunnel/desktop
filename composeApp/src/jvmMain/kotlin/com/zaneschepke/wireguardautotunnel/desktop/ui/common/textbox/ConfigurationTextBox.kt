package com.zaneschepke.wireguardautotunnel.desktop.ui.common.textbox

import androidx.compose.foundation.background
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.defaultMinSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextFieldDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp

// Static label notch (no floating/animated label), matching Android's ConfigurationTextBox.
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ConfigurationTextBox(
    value: String,
    onValueChange: (String) -> Unit,
    label: String,
    modifier: Modifier = Modifier,
    hint: String = "",
    isError: Boolean = false,
    keyboardOptions: KeyboardOptions = KeyboardOptions.Default,
    keyboardActions: KeyboardActions = KeyboardActions.Default,
    visualTransformation: VisualTransformation = VisualTransformation.None,
    trailing: @Composable (() -> Unit)? = null,
    supportingText: @Composable (() -> Unit)? = null,
    singleLine: Boolean = true,
    containerColor: Color = MaterialTheme.colorScheme.background,
) {
    val interactionSource = remember { MutableInteractionSource() }

    Box(modifier = modifier.padding(top = 6.dp)) {
        BasicTextField(
            value = value,
            onValueChange = onValueChange,
            textStyle =
                MaterialTheme.typography.bodySmall.copy(
                    color = MaterialTheme.colorScheme.onSurface
                ),
            cursorBrush = SolidColor(MaterialTheme.colorScheme.primary),
            keyboardOptions = keyboardOptions,
            keyboardActions = keyboardActions,
            singleLine = singleLine,
            visualTransformation = visualTransformation,
            interactionSource = interactionSource,
            modifier = Modifier.fillMaxWidth().defaultMinSize(minHeight = 48.dp),
        ) { innerTextField ->
            OutlinedTextFieldDefaults.DecorationBox(
                value = value,
                innerTextField = innerTextField,
                placeholder = {
                    Text(
                        text = hint,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.outline,
                        overflow = TextOverflow.Ellipsis,
                        maxLines = 1,
                    )
                },
                contentPadding =
                    OutlinedTextFieldDefaults.contentPadding(top = 14.dp, bottom = 14.dp),
                trailingIcon = trailing,
                supportingText = supportingText,
                singleLine = singleLine,
                enabled = true,
                label = null,
                visualTransformation = visualTransformation,
                interactionSource = interactionSource,
                colors =
                    TextFieldDefaults.colors()
                        .copy(
                            focusedContainerColor = containerColor,
                            unfocusedContainerColor = containerColor,
                            disabledContainerColor = containerColor,
                            errorContainerColor = containerColor,
                        ),
                container = {
                    OutlinedTextFieldDefaults.Container(
                        enabled = true,
                        isError = isError,
                        interactionSource = interactionSource,
                        colors =
                            TextFieldDefaults.colors()
                                .copy(
                                    focusedContainerColor = containerColor,
                                    unfocusedContainerColor = containerColor,
                                    disabledContainerColor = containerColor,
                                    errorContainerColor = containerColor,
                                    focusedIndicatorColor = MaterialTheme.colorScheme.primary,
                                    unfocusedIndicatorColor = MaterialTheme.colorScheme.outline,
                                ),
                        shape = RoundedCornerShape(8.dp),
                        focusedBorderThickness = 0.5.dp,
                        unfocusedBorderThickness = 0.5.dp,
                        modifier = Modifier.height(48.dp),
                    )
                },
            )
        }

        if (label.isNotEmpty()) {
            Text(
                text = label,
                color = MaterialTheme.colorScheme.onSurface,
                style = MaterialTheme.typography.labelSmall,
                overflow = TextOverflow.Ellipsis,
                maxLines = 1,
                modifier =
                    Modifier.padding(start = 12.dp)
                        .offset(y = (-8).dp)
                        .background(containerColor)
                        .padding(horizontal = 4.dp),
            )
        }
    }
}
