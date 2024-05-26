package com.nunchuk.android.main.membership.byzantine.healthcheck

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
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
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
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
import androidx.compose.ui.text.style.TextDecoration
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
import com.nunchuk.android.model.byzantine.KeyHealthStatus
import com.nunchuk.android.model.byzantine.isKeyHolderLimited
import com.nunchuk.android.type.SignerType
import com.nunchuk.android.utils.healthCheckLabel
import com.nunchuk.android.utils.healthCheckTimeColor

@Composable
fun HealthCheckContent(
    state: GroupDashboardState = GroupDashboardState(),
    onRequestHealthCheck: (SignerModel) -> Unit = {},
    onHealthCheck: (SignerModel) -> Unit = {},
    onNavigateToHealthCheckReminder: () -> Unit = {},
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
                    actions = {
                        Text(
                            text = "Reminders",
                            style = NunchukTheme.typography.title,
                            textDecoration = TextDecoration.Underline,
                            modifier = Modifier
                                .padding(end = 16.dp)
                                .clickable {
                                    onNavigateToHealthCheckReminder()
                                }
                        )
                    }
                )
            }) { innerPadding ->
            Column(
                modifier = Modifier
                    .padding(innerPadding)
                    .padding(horizontal = 16.dp)
                    .fillMaxHeight(),
            ) {
                Text(
                    text = state.wallet.name,
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
                            onRequestHealthCheck = onRequestHealthCheck,
                            isShowRequestHealthCheck = state.groupId.isNotEmpty()
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
    isShowRequestHealthCheck: Boolean,
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
                color = MaterialTheme.colorScheme.border,
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
                Row {
                    NcTag(label = signer.toReadableSignerType(context))
                    if (signer.isShowAcctX()) {
                        NcTag(
                            modifier = Modifier.padding(start = 4.dp),
                            label = stringResource(id = R.string.nc_acct_x, signer.index)
                        )
                    }
                }
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
                enabled = signer.type != SignerType.FOREIGN_SOFTWARE,
                onClick = { onHealthCheck(signer) },
            ) {
                Text(
                    text = stringResource(R.string.nc_health_check),
                    style = NunchukTheme.typography.captionTitle
                )
            }

            if (isShowRequestHealthCheck) {
                NcOutlineButton(
                    modifier = Modifier
                        .weight(1f)
                        .height(36.dp),
                    onClick = { onRequestHealthCheck(signer) },
                    enabled = status?.canRequestHealthCheck == true
                ) {
                    Text(
                        text = stringResource(R.string.nc_request_health_check),
                        style = NunchukTheme.typography.captionTitle
                    )
                }
            }
        }
    }
}

@Preview
@Composable
private fun HealthCheckScreenPreview() {
    HealthCheckContent()
}