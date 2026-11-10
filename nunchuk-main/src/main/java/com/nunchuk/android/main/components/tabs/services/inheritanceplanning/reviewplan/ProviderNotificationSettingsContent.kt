package com.nunchuk.android.main.components.tabs.services.inheritanceplanning.reviewplan

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.nunchuk.android.compose.NunchukTheme
import com.nunchuk.android.compose.controlFillPrimary
import com.nunchuk.android.compose.greyLight
import com.nunchuk.android.compose.strokePrimary
import com.nunchuk.android.core.R
import com.nunchuk.android.main.R as MainR
import com.nunchuk.android.main.components.tabs.services.inheritanceplanning.notificationsettings.unlockNoticesSummary
import com.nunchuk.android.model.inheritance.EmailNotificationSettings

@Composable
fun ProviderNotificationSettingsContent(
    emailSettings: EmailNotificationSettings,
    textColor: Color = MaterialTheme.colorScheme.controlFillPrimary,
    /** On-chain plans with fallbacks list which unlocks this person hears about (REV-01). */
    fallbackCount: Int = 0,
    /** Set on a pending plan change (REV-11): only these values take [changedTextColor]. */
    changes: RecipientNotificationChanges? = null,
    changedTextColor: Color = textColor,
) {
    fun colorFor(changed: RecipientNotificationChanges.() -> Boolean): Color =
        if (changes?.changed() == true) changedTextColor else textColor

    Box(
        modifier = Modifier
            .padding(top = 16.dp)
            .background(
                color = MaterialTheme.colorScheme.greyLight,
                shape = RoundedCornerShape(8.dp)
            )
            .fillMaxWidth()
            .padding(16.dp)
    ) {
        Column(
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Text(
                text = emailSettings.email,
                style = NunchukTheme.typography.title
            )

            HorizontalDivider(
                thickness = 1.dp,
                color = MaterialTheme.colorScheme.strokePrimary
            )

            if (fallbackCount > 0) {
                NotificationSettingValueRow(
                    label = stringResource(id = MainR.string.nc_unlock_notices),
                    value = emailSettings.unlockNoticesSummary(fallbackCount),
                    textColor = colorFor { unlockNotices },
                    // "Primary inheritance, Fallback 1, Fallback 2, …" grows with the plan: give it a
                    // fixed share so it wraps (REV-06) instead of squeezing the label out.
                    isValueWrapping = true,
                )
            } else {
                NotificationSettingRow(
                    label = stringResource(id = R.string.nc_notify_when_timelock_expires),
                    value = emailSettings.notifyOnTimelockExpiry,
                    textColor = colorFor { unlockNotices }
                )
            }

            NotificationSettingRow(
                label = stringResource(id = R.string.nc_notify_when_wallet_changes),
                value = emailSettings.notifyOnWalletChanges,
                textColor = colorFor { walletChanges }
            )

            NotificationSettingRow(
                label = stringResource(id = R.string.nc_also_include_wallet_configuration),
                value = emailSettings.includeWalletConfiguration,
                textColor = colorFor { includeWalletConfiguration }
            )
        }
    }
}

@Composable
fun NotificationSettingRow(
    label: String,
    value: Boolean,
    textColor: Color = MaterialTheme.colorScheme.controlFillPrimary
) {
    NotificationSettingValueRow(
        label = label,
        value = if (value) stringResource(id = MainR.string.nc_text_yes) else stringResource(id = MainR.string.nc_text_no),
        textColor = textColor,
    )
}

@Composable
private fun NotificationSettingValueRow(
    label: String,
    value: String,
    textColor: Color = MaterialTheme.colorScheme.controlFillPrimary,
    isValueWrapping: Boolean = false,
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = label,
            style = NunchukTheme.typography.body,
            modifier = Modifier.weight(1f)
        )
        Text(
            modifier = Modifier
                .then(if (isValueWrapping) Modifier.weight(1.2f, fill = false) else Modifier)
                .padding(start = 12.dp),
            text = value,
            textAlign = TextAlign.End,
            style = NunchukTheme.typography.title.copy(color = textColor)
        )
    }
}