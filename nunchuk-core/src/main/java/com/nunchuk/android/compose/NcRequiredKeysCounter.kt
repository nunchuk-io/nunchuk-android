package com.nunchuk.android.compose

import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.PreviewLightDark
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.nunchuk.android.core.R

/**
 * A titled −/value/+ row, e.g. "Required keys" or "Total number of keys" of an M-of-N policy.
 * Callers own the bounds: pass [enableIncrement] / [enableDecrement] false at the limits.
 * [isValueBoxed] frames the value; [buttonSize] sizes the round − / + buttons; [titleStyle] styles the title.
 */
@Composable
fun NcRequiredKeysCounter(
    title: String,
    description: String,
    value: Int,
    enableIncrement: Boolean = true,
    enableDecrement: Boolean = true,
    isValueBoxed: Boolean = true,
    buttonSize: Dp = 36.dp,
    titleStyle: TextStyle = NunchukTheme.typography.body,
    onIncrement: () -> Unit,
    onDecrement: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Column(
            modifier = Modifier
                .weight(1f)
                .padding(end = 12.dp)
        ) {
            Text(
                text = title,
                style = titleStyle,
                color = MaterialTheme.colorScheme.onSurface
            )
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = description,
                style = NunchukTheme.typography.bodySmall.copy(
                    color = MaterialTheme.colorScheme.textSecondary,
                    fontWeight = FontWeight.W400
                )
            )
        }

        Row(
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(buttonSize)
                    .alpha(if (enableDecrement) 1f else 0.4f)
                    .border(1.dp, MaterialTheme.colorScheme.textPrimary, CircleShape)
                    .clickable(enableDecrement) {
                        onDecrement()
                    },
                contentAlignment = Alignment.Center
            ) {
                NcIcon(
                    painter = painterResource(id = R.drawable.ic_minus),
                    contentDescription = stringResource(R.string.nc_wallet_cd_decrement),
                )
            }

            Box(
                modifier = Modifier
                    .padding(horizontal = 12.dp)
                    .size(48.dp)
                    .then(
                        if (isValueBoxed) Modifier.border(1.dp, Color.Gray, RoundedCornerShape(8.dp)) else Modifier
                    ),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = value.toString(),
                    style = NunchukTheme.typography.body,
                )
            }

            Box(
                modifier = Modifier
                    .size(buttonSize)
                    .alpha(if (enableIncrement) 1f else 0.4f)
                    .border(1.dp, MaterialTheme.colorScheme.textPrimary, CircleShape)
                    .clickable(enableIncrement) { onIncrement() },
                contentAlignment = Alignment.Center
            ) {
                NcIcon(
                    painter = painterResource(id = R.drawable.ic_plus),
                    contentDescription = stringResource(R.string.nc_wallet_cd_increment),
                )
            }
        }
    }
}

@PreviewLightDark
@Composable
private fun NcRequiredKeysCounterPreview() {
    NunchukTheme {
        NcRequiredKeysCounter(
            title = stringResource(R.string.nc_miniscript_required_keys),
            description = stringResource(R.string.nc_miniscript_num_signatures_required),
            value = 2,
            enableDecrement = false,
            onIncrement = {},
            onDecrement = {},
        )
    }
}
