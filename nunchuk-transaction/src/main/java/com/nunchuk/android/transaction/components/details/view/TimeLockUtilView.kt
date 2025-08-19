package com.nunchuk.android.transaction.components.details.view

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.nunchuk.android.compose.NcIcon
import com.nunchuk.android.compose.NunchukTheme
import com.nunchuk.android.compose.fillPink
import com.nunchuk.android.compose.textPrimary
import com.nunchuk.android.transaction.R
import com.nunchuk.android.type.MiniscriptTimelockBased
import com.nunchuk.android.utils.dateTimeFormat
import java.util.Date

@Composable
fun TimeLockUtilView(
    lockedTime: Long,
    lockedBase: MiniscriptTimelockBased,
) {
    Row(
        modifier = Modifier
            .padding(16.dp)
            .background(
                color = MaterialTheme.colorScheme.fillPink,
                shape = RoundedCornerShape(8.dp)
            )
            .padding(12.dp)
            .fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        NcIcon(
            painter = painterResource(id = R.drawable.ic_timer),
            contentDescription = null,
            modifier = Modifier.size(36.dp),
        )

        val lockText = if (lockedBase == MiniscriptTimelockBased.HEIGHT_LOCK) {
            stringResource(id = R.string.nc_timelocked_until_block, lockedTime)
        } else {
            val date = Date(lockedTime * 1000L)
            stringResource(id = R.string.nc_timelocked_until_date, date.dateTimeFormat())
        }
        Text(
            text = lockText,
            style = NunchukTheme.typography.titleSmall,
            color = MaterialTheme.colorScheme.textPrimary
        )
    }
}