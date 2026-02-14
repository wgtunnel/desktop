package com.zaneschepke.wireguardautotunnel.ui.screens.support.donate.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.zaneschepke.wireguardautotunnel.composeapp.generated.resources.Res
import com.zaneschepke.wireguardautotunnel.composeapp.generated.resources.dev_name
import com.zaneschepke.wireguardautotunnel.composeapp.generated.resources.donation_closing
import com.zaneschepke.wireguardautotunnel.composeapp.generated.resources.donation_dev_message
import com.zaneschepke.wireguardautotunnel.composeapp.generated.resources.donation_signoff
import com.zaneschepke.wireguardautotunnel.composeapp.generated.resources.donation_thanks_intro
import org.jetbrains.compose.resources.stringResource

@Composable
fun DonationHeroSection() {
    Surface(modifier = Modifier.fillMaxWidth()) {
        Column(
            modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp),
            horizontalAlignment = Alignment.Start,
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            Text(
                text = stringResource(Res.string.donation_thanks_intro),
                style =
                    MaterialTheme.typography.bodySmall.copy(
                        color = MaterialTheme.colorScheme.onSurface
                    ),
                textAlign = TextAlign.Start,
            )

            Text(
                text = stringResource(Res.string.donation_dev_message),
                style =
                    MaterialTheme.typography.bodySmall.copy(
                        color = MaterialTheme.colorScheme.onSurface
                    ),
                textAlign = TextAlign.Start,
            )

            Text(
                text = stringResource(Res.string.donation_closing),
                style =
                    MaterialTheme.typography.bodySmall.copy(
                        color = MaterialTheme.colorScheme.onSurface
                    ),
                textAlign = TextAlign.Start,
            )

            Text(
                text = stringResource(Res.string.donation_signoff),
                style =
                    MaterialTheme.typography.bodySmall.copy(
                        color = MaterialTheme.colorScheme.onSurface
                    ),
                fontWeight = FontWeight.Bold,
                textAlign = TextAlign.Start,
            )
            Text(
                text = stringResource(Res.string.dev_name),
                style =
                    MaterialTheme.typography.bodySmall.copy(
                        color = MaterialTheme.colorScheme.onSurface
                    ),
                fontWeight = FontWeight.Bold,
                textAlign = TextAlign.Start,
            )
        }
    }
}
