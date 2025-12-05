package com.nunchuk.android.main.components.tabs.services.inheritanceplanning.reviewplan

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.colorResource
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.unit.dp
import com.nunchuk.android.compose.NunchukTheme
import com.nunchuk.android.main.R

@Composable
fun ActivationDateItem(
    activationDate: String = "January 1, 2024",
    timeZoneId: String = "",
    editable: Boolean = false,
    onClick: () -> Unit = {}
) {
    val timeZoneDisplay = getTimezoneDisplay(timeZoneId)

    Row(
        modifier = Modifier
            .background(
                color = Color.Companion.Black.copy(alpha = 0.2f),
                shape = RoundedCornerShape(12.dp)
            )
            .padding(12.dp)
            .fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        Icon(
            painter = painterResource(id = R.drawable.ic_calendar_light),
            tint = Color.Companion.White,
            contentDescription = ""
        )
        Column(
            modifier = Modifier.weight(1f)
        ) {
            Text(
                text = activationDate,
                style = NunchukTheme.typography.title,
                color = Color.Companion.White
            )
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = timeZoneDisplay,
                style = NunchukTheme.typography.bodySmall,
                color = Color.Companion.White.copy(alpha = 0.8f)
            )
        }
        if (editable) {
            Text(
                modifier = Modifier.clickable {
                    onClick()
                },
                text = stringResource(id = R.string.nc_edit),
                color = colorResource(id = R.color.nc_white_color),
                style = NunchukTheme.typography.title,
                textDecoration = TextDecoration.Companion.Underline,
            )
        }
    }
}