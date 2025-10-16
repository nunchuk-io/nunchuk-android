package com.nunchuk.android.main.components.tabs.services.inheritanceplanning.notificationsettings

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.nunchuk.android.compose.NunchukTheme
import com.nunchuk.android.compose.backgroundMidGray
import com.nunchuk.android.main.R

@Composable
fun EmailNotificationSection(
    emailSettings: EmailNotificationSettings,
    onSettingsChange: (EmailNotificationSettings) -> Unit
) {
    var isExpanded by remember { mutableStateOf(true) }

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 8.dp)
            .background(
                color = MaterialTheme.colorScheme.backgroundMidGray,
                shape = RoundedCornerShape(8.dp)
            )
            .padding(12.dp)
    ) {
        // Email dropdown header
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .clickable { isExpanded = !isExpanded },
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Text(
                text = emailSettings.email,
                style = NunchukTheme.typography.body
            )
            Icon(
                painter = painterResource(
                    id = if (isExpanded) R.drawable.ic_collapse else R.drawable.ic_expand
                ),
                contentDescription = null
            )
        }

        // Settings for this email (shown only when expanded)
        if (isExpanded) {
            HorizontalDivider(
                modifier = Modifier.padding(vertical = 16.dp),
                thickness = 1.dp
            )

            Column(
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                NotificationToggleItem(
                    title = stringResource(R.string.nc_notify_when_timelock_expires),
                    description = stringResource(R.string.nc_notify_when_timelock_expires_desc),
                    checked = emailSettings.notifyOnTimelockExpiry,
                    onCheckedChange = {
                        onSettingsChange(emailSettings.copy(notifyOnTimelockExpiry = it))
                    }
                )

                NotificationToggleItem(
                    title = stringResource(R.string.nc_notify_when_wallet_changes),
                    description = stringResource(R.string.nc_notify_when_wallet_changes_desc),
                    checked = emailSettings.notifyOnWalletChanges,
                    onCheckedChange = {
                        onSettingsChange(emailSettings.copy(notifyOnWalletChanges = it))
                    }
                )

                NotificationToggleItem(
                    title = stringResource(R.string.nc_also_include_wallet_configuration),
                    description = stringResource(R.string.nc_also_include_wallet_configuration_desc),
                    checked = emailSettings.includeWalletConfiguration,
                    onCheckedChange = {
                        onSettingsChange(emailSettings.copy(includeWalletConfiguration = it))
                    }
                )
            }
        }
    }
}