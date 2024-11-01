package com.nunchuk.android.main.membership.byzantine.groupdashboard

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyListScope
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.nunchuk.android.compose.NcCircleImage
import com.nunchuk.android.compose.NcIcon
import com.nunchuk.android.compose.NunchukTheme
import com.nunchuk.android.compose.border
import com.nunchuk.android.core.signer.SignerModel
import com.nunchuk.android.core.util.toReadableDrawableResId
import com.nunchuk.android.main.R
import com.nunchuk.android.model.byzantine.KeyHealthStatus
import com.nunchuk.android.type.SignerType
import com.nunchuk.android.utils.healthCheckTimeColor

fun LazyListScope.HealthCheckStatusView(onOpenHealthCheckScreen: () -> Unit = {}, signers: List<SignerModel>, status: Map<String, KeyHealthStatus>) {
    item {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 8.dp)
        ) {
            Row {
                NcIcon(
                    painter = painterResource(id = R.drawable.ic_health_check),
                    contentDescription = "image description",
                )
                Text(
                    modifier = Modifier.padding(start = 8.dp),
                    text = stringResource(R.string.nc_key_health_status),
                    style = NunchukTheme.typography.title
                )
            }
            LazyRow(
                modifier = Modifier
                    .padding(top = 12.dp)
                    .border(
                        width = 1.dp,
                        color = MaterialTheme.colorScheme.border,
                        shape = RoundedCornerShape(size = 8.dp)
                    )
                    .fillMaxWidth()
                    .background(
                        color = MaterialTheme.colorScheme.surface,
                        shape = RoundedCornerShape(size = 8.dp)
                    )
                    .clickable(onClick = onOpenHealthCheckScreen)
                    .padding(12.dp),
                horizontalArrangement = Arrangement.spacedBy(4.dp),
            ) {
                items(signers.filter { it.type != SignerType.SERVER }) {
                    NcCircleImage(resId = it.toReadableDrawableResId(), size = 36.dp, iconSize = 18.dp, color = status[it.fingerPrint]?.lastHealthCheckTimeMillis.healthCheckTimeColor())
                }
            }
        }
    }
}