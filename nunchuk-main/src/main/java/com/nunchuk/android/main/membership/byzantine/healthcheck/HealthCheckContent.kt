package com.nunchuk.android.main.membership.byzantine.healthcheck

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.MaterialTheme
import androidx.compose.material.Scaffold
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.nunchuk.android.compose.NcCircleImage
import com.nunchuk.android.compose.NcOutlineButton
import com.nunchuk.android.compose.NcTag
import com.nunchuk.android.compose.NcTopAppBar
import com.nunchuk.android.compose.NunchukTheme
import com.nunchuk.android.compose.border
import com.nunchuk.android.core.signer.SignerModel
import com.nunchuk.android.core.util.toReadableDrawableResId
import com.nunchuk.android.core.util.toReadableSignerType
import com.nunchuk.android.main.R
import com.nunchuk.android.main.membership.byzantine.groupdashboard.GroupDashboardState
import com.nunchuk.android.main.membership.byzantine.healthCheckLabel
import com.nunchuk.android.main.membership.byzantine.healthCheckTimeColor
import com.nunchuk.android.model.byzantine.KeyHealthStatus
import com.nunchuk.android.model.byzantine.isKeyHolderLimited
import com.nunchuk.android.type.SignerType

@Composable
fun HealthCheckContent(
    state: GroupDashboardState = GroupDashboardState(),
    onRequestHealthCheck: (SignerModel) -> Unit = {},
    onHealthCheck: (SignerModel) -> Unit = {},
) {
    val signers by remember(state.myRole, state.signers) {
        derivedStateOf {
            if (state.myRole.isKeyHolderLimited) {
                state.signers.filter { it.isVisible }.filter { it.type != SignerType.SERVER }
            } else {
                state.signers.filter { it.type != SignerType.SERVER }
            }
        }
    }

    NunchukTheme {
        Scaffold(
            modifier = Modifier
                .navigationBarsPadding()
                .statusBarsPadding(),
            topBar = {
                NcTopAppBar(
                    title = stringResource(R.string.nc_key_health_status),
                    textStyle = NunchukTheme.typography.titleLarge,
                    elevation = 0.dp
                )
            }) { innerPadding ->
            Column(
                modifier = Modifier
                    .padding(innerPadding)
                    .padding(horizontal = 16.dp)
                    .fillMaxHeight(),
            ) {
                Text(
                    text = stringResource(R.string.nc_savings_account),
                    style = NunchukTheme.typography.title
                )
                LazyColumn(
                    modifier = Modifier
                        .padding(top = 16.dp),
                    verticalArrangement = Arrangement.spacedBy(16.dp, Alignment.Top),
                ) {
                    items(signers) {
                        HealthCheckItem(
                            signer = it,
                            status = state.keyStatus[it.fingerPrint],
                            onHealthCheck = onHealthCheck,
                            onRequestHealthCheck = onRequestHealthCheck
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun HealthCheckItem(
    signer: SignerModel,
    status: KeyHealthStatus?,
    onHealthCheck: (SignerModel) -> Unit = {},
    onRequestHealthCheck: (SignerModel) -> Unit = {},
) {
    val context = LocalContext.current
    val label by remember(status?.lastHealthCheckTimeMillis) {
        derivedStateOf {
            status?.lastHealthCheckTimeMillis.healthCheckLabel(context)
        }
    }
    val color = status?.lastHealthCheckTimeMillis.healthCheckTimeColor()
    Column(
        modifier = Modifier
            .border(
                width = 1.dp,
                color = MaterialTheme.colors.border,
                shape = RoundedCornerShape(size = 12.dp)
            )
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp, Alignment.CenterVertically),
        horizontalAlignment = Alignment.Start,
    ) {

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Row(
                modifier = Modifier
                    .background(color = color, shape = RoundedCornerShape(size = 8.dp))
                    .padding(horizontal = 12.dp),
                horizontalArrangement = Arrangement.spacedBy(4.dp, Alignment.Start),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text(
                    text = label,
                    style = NunchukTheme.typography.titleSmall.copy(fontSize = 10.sp)
                )
            }

            Image(
                modifier = Modifier
                    .width(16.dp)
                    .height(16.dp),
                painter = painterResource(id = R.drawable.ic_history),
                contentDescription = "image description",
                contentScale = ContentScale.None
            )
        }

        Row(
            horizontalArrangement = Arrangement.spacedBy(12.dp, Alignment.Start),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            NcCircleImage(
                resId = signer.toReadableDrawableResId(),
                size = 48.dp,
                iconSize = 24.dp,
                color = color
            )

            Column(
                verticalArrangement = Arrangement.spacedBy(4.dp, Alignment.Top),
                horizontalAlignment = Alignment.Start,
            ) {
                Text(
                    text = signer.name,
                    style = NunchukTheme.typography.body
                )
                NcTag(label = signer.toReadableSignerType(context))
                Text(
                    text = signer.getXfpOrCardIdLabel(),
                    style = NunchukTheme.typography.bodySmall
                )
            }
        }

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp, Alignment.Start),
            verticalAlignment = Alignment.Top,
        ) {
            NcOutlineButton(
                modifier = Modifier
                    .weight(1f)
                    .height(36.dp),
                onClick = { onHealthCheck(signer) },
            ) {
                Text(
                    text = stringResource(R.string.nc_health_check),
                    style = NunchukTheme.typography.caption
                )
            }
            NcOutlineButton(
                modifier = Modifier
                    .weight(1f)
                    .height(36.dp),
                onClick = { onRequestHealthCheck(signer) },
                enabled = status?.canRequestHealthCheck == true
            ) {
                Text(
                    text = stringResource(R.string.nc_request_health_check),
                    style = NunchukTheme.typography.caption
                )
            }
        }
    }
}

@Preview
@Composable
private fun HealthCheckScreenPreview() {
    HealthCheckContent()
}