package com.nunchuk.android.main.components.tabs.services.inheritanceplanning.notificationsettings

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.width
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.nunchuk.android.compose.NcSwitch
import com.nunchuk.android.compose.NunchukTheme

@Composable
fun NotificationToggleItem(
    modifier: Modifier = Modifier,
    title: String,
    description: String,
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit
) {
    Row(
        modifier = modifier.fillMaxWidth(),
        verticalAlignment = Alignment.Top
    ) {
        Column(
            modifier = Modifier.weight(1f)
        ) {
            Text(
                text = title,
                style = NunchukTheme.typography.body
            )
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = description,
                style = NunchukTheme.typography.bodySmall
            )
        }
        Spacer(modifier = Modifier.width(12.dp))
        NcSwitch(
            checked = checked,
            onCheckedChange = onCheckedChange,
        )
    }
}