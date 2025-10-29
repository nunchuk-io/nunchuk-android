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
import androidx.compose.ui.unit.dp
import com.nunchuk.android.compose.NunchukTheme
import com.nunchuk.android.compose.controlFillPrimary
import com.nunchuk.android.compose.greyLight
import com.nunchuk.android.compose.strokePrimary
import com.nunchuk.android.core.R
import com.nunchuk.android.model.inheritance.EmailNotificationSettings

@Composable
fun ProviderNotificationSettingsContent(
    emailSettings: EmailNotificationSettings,
    textColor: Color = MaterialTheme.colorScheme.controlFillPrimary
) {
    Box(
        modifier = Modifier
            .padding(start = 16.dp, end = 16.dp, top = 16.dp)
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

            NotificationSettingRow(
                label = stringResource(id = R.string.nc_notify_when_timelock_expires),
                value = emailSettings.notifyOnTimelockExpiry,
                textColor = textColor
            )

            NotificationSettingRow(
                label = stringResource(id = R.string.nc_notify_when_wallet_changes),
                value = emailSettings.notifyOnWalletChanges,
                textColor = textColor
            )

            NotificationSettingRow(
                label = stringResource(id = R.string.nc_also_include_wallet_configuration),
                value = emailSettings.includeWalletConfiguration,
                textColor = textColor
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
            text = if (value) stringResource(id = com.nunchuk.android.main.R.string.nc_text_yes) else stringResource(id = com.nunchuk.android.main.R.string.nc_text_no),
            style = NunchukTheme.typography.title.copy(color = textColor)
        )
    }
}