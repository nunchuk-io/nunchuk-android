package com.nunchuk.android.signer.components.backup

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.res.colorResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.PreviewLightDark
import androidx.compose.ui.unit.dp
import com.nunchuk.android.compose.NcImageAppBar
import com.nunchuk.android.compose.NcPrimaryDarkButton
import com.nunchuk.android.compose.NunchukTheme
import com.nunchuk.android.compose.textPrimary
import com.nunchuk.android.signer.R

@Composable
fun BackingUpContent(
    onContinueClicked: () -> Unit = {},
    percentage: Int = 0,
    isError: Boolean = false,
    remainTime: Int = 0,
    title: String = "",
    description: String = "",
) = NunchukTheme {
    Scaffold(topBar = {
        NcImageAppBar(
            backgroundRes = R.drawable.bg_uploading_backup_illustration,
            title = stringResource(id = R.string.nc_estimate_remain_time, remainTime),
        )
    }) { innerPadding ->
        Column(
            modifier = Modifier
                .padding(innerPadding)
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
        ) {
            Text(
                modifier = Modifier.padding(top = 24.dp, start = 16.dp, end = 16.dp),
                text = title,
                style = NunchukTheme.typography.heading
            )
            Text(
                modifier = Modifier.padding(16.dp),
                text = description,
                style = NunchukTheme.typography.body
            )
            LinearProgressIndicator(
                progress = percentage.div(100f),
                modifier = Modifier
                    .padding(horizontal = 16.dp)
                    .clip(RoundedCornerShape(24.dp))
                    .height(8.dp)
                    .fillMaxWidth(),
                color = MaterialTheme.colorScheme.textPrimary,
                trackColor = colorResource(id = R.color.nc_bg_mid_gray),
            )
            val label = when {
                isError -> stringResource(R.string.nc_upload_failed)
                percentage == 100 -> stringResource(R.string.nc_backup_uploaded_successfully)
                else -> "${percentage}%"
            }
            Text(
                modifier = Modifier
                    .padding(16.dp)
                    .align(alignment = Alignment.CenterHorizontally),
                text = label,
                style = NunchukTheme.typography.body.copy(
                    color = if (isError) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.textPrimary
                )
            )
            Spacer(modifier = Modifier.weight(1.0f))
            NcPrimaryDarkButton(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp),
                enabled = percentage == 100 || isError,
                onClick = onContinueClicked,
            ) {
                Text(
                    text = if (isError) stringResource(R.string.nc_try_again)
                    else stringResource(R.string.nc_text_continue)
                )
            }
        }
    }
}

@PreviewLightDark
@Composable
private fun UploadBackUpTapSignerScreenPreview() {
    BackingUpContent(
        percentage = 75,
        description = "Backing up your wallet will allow you to restore your wallet on another device."
    )
}

@PreviewLightDark
@Composable
private fun UploadBackUpTapSignerScreenFailedPreview() {
    BackingUpContent(
        percentage = 75,
        isError = true,
        description = "Backing up your wallet will allow you to restore your wallet on another device."
    )
}

@PreviewLightDark
@Composable
private fun UploadBackUpTapSignerScreen100Preview() {
    BackingUpContent(
        percentage = 100,
        isError = false,
        description = "Backing up your wallet will allow you to restore your wallet on another device."
    )
}
