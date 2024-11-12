package com.nunchuk.android.signer.components.backup

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.nunchuk.android.compose.HighlightMessageType
import com.nunchuk.android.compose.NcHintMessage
import com.nunchuk.android.compose.NcPrimaryDarkButton
import com.nunchuk.android.compose.NcTopAppBar
import com.nunchuk.android.compose.NunchukTheme
import com.nunchuk.android.core.util.ClickAbleText
import com.nunchuk.android.signer.R
import com.nunchuk.android.signer.tapsigner.backup.verify.components.TsVerifyBackUpOption

@Composable
fun VerifyBackUpOptionContent(
    onContinueClicked: (BackUpOption) -> Unit = {},
    remainingTime: Int = 0,
    options: List<BackUpOption> = emptyList(),
) = NunchukTheme {

    var selectedOption by remember { mutableStateOf(options.first()) }

    Scaffold(topBar = {
        NcTopAppBar(
            title = stringResource(R.string.nc_estimate_remain_time, remainingTime),
        )
    }) { innerPadding ->
        Column(
            modifier = Modifier.padding(innerPadding),
        ) {
            Text(
                modifier = Modifier.padding(top = 24.dp, start = 16.dp, end = 16.dp),
                text = stringResource(R.string.nc_verify_your_backup),
                style = NunchukTheme.typography.heading
            )
            Text(
                modifier = Modifier.padding(start = 16.dp, end = 16.dp, top = 16.dp),
                text = stringResource(R.string.nc_verify_back_up_desc),
                style = NunchukTheme.typography.body,
            )
            LazyColumn(
                contentPadding = PaddingValues(16.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                items(options) { item ->
                    TsVerifyBackUpOption(
                        modifier = Modifier.fillMaxWidth(),
                        isSelected = selectedOption == item,
                        label = stringResource(id = item.labelId),
                        isRecommend = item.type == BackUpOptionType.BY_MYSELF
                    ) {
                        selectedOption = item
                    }
                }
            }
            Spacer(modifier = Modifier.weight(1.0f))
            NcHintMessage(
                modifier = Modifier.padding(horizontal = 16.dp),
                messages = listOf(ClickAbleText(content = stringResource(R.string.nc_verify_backup_hint))),
                type = HighlightMessageType.HINT,
            )
            NcPrimaryDarkButton(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp),
                enabled = options.any { it.isSelected },
                onClick = {
                    onContinueClicked(selectedOption)
                },
            ) {
                Text(text = stringResource(id = R.string.nc_text_continue))
            }
        }
    }
}

@Preview
@Composable
private fun UploadBackUpTapSignerScreenPreview() {
    NunchukTheme {
        VerifyBackUpOptionContent(
            options = BACKUP_OPTIONS
        )
    }
}

internal val BACKUP_OPTIONS = listOf(
    BackUpOption(
        type = BackUpOptionType.BY_MYSELF,
        labelId = R.string.nc_verify_backup_myself,
        isSelected = true
    ),
    BackUpOption(
        type = BackUpOptionType.BY_APP,
        labelId = R.string.nc_verify_backup_via_the_app,
    ),
    BackUpOption(
        type = BackUpOptionType.SKIP,
        labelId = R.string.nc_skip_verification
    )
)
