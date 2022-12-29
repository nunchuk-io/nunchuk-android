package com.nunchuk.android.signer.mk4.info.component

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.Scaffold
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.nunchuk.android.compose.*
import com.nunchuk.android.core.util.ClickAbleText
import com.nunchuk.android.signer.R

@Composable
internal fun Mk4InfoContent(
    remainTime: Int = 0,
    onContinueClicked: () -> Unit = {},
    onOpenGuideClicked: () -> Unit = {},
    isMembershipFlow: Boolean = true,
) {
    NunchukTheme {
        Scaffold { innerPadding ->
            Column(
                modifier = Modifier
                    .fillMaxHeight()
                    .padding(innerPadding)
                    .navigationBarsPadding()
                    .verticalScroll(rememberScrollState())
            ) {
                NcImageAppBar(
                    backgroundRes = R.drawable.bg_add_coldcard_view_nfc_intro,
                    title = if (isMembershipFlow) stringResource(
                        id = R.string.nc_estimate_remain_time,
                        remainTime
                    ) else ""
                )
                Text(
                    modifier = Modifier.padding(top = 24.dp, start = 16.dp, end = 16.dp),
                    text = stringResource(R.string.nc_add_your_coldcard),
                    style = NunchukTheme.typography.heading
                )
                Text(
                    modifier = Modifier.padding(16.dp),
                    text = stringResource(R.string.nc_ensure_to_following_step),
                    style = NunchukTheme.typography.body
                )
                LabelNumberAndDesc(
                    modifier = Modifier.padding(start = 16.dp, end = 16.dp, top = 16.dp),
                    index = 1,
                    title = stringResource(id = R.string.nc_init_coldcard),
                    titleStyle = NunchukTheme.typography.title,
                ) {
                    NcClickableText(
                        modifier = Modifier.padding(top = 8.dp, start = 36.dp),
                        messages = listOf(
                            ClickAbleText(content = stringResource(id = R.string.nc_refer_to)),
                            ClickAbleText(
                                content = stringResource(id = R.string.nc_this_starter_guide),
                                onOpenGuideClicked
                            )
                        ),
                        style = NunchukTheme.typography.body
                    )
                }
                LabelNumberAndDesc(
                    modifier = Modifier.padding(start = 16.dp, end = 16.dp, top = 16.dp),
                    index = 2,
                    title = stringResource(id = R.string.nc_unlock_coldcard),
                    titleStyle = NunchukTheme.typography.title,
                ) {
                    Text(
                        modifier = Modifier.padding(top = 8.dp, start = 36.dp),
                        text = stringResource(id = R.string.nc_unlock_device_desc),
                        style = NunchukTheme.typography.body
                    )
                }
                LabelNumberAndDesc(
                    modifier = Modifier.padding(start = 16.dp, end = 16.dp, top = 16.dp),
                    index = 3,
                    title = stringResource(id = R.string.nc_enable_nfc),
                    titleStyle = NunchukTheme.typography.title,
                ) {
                    Text(
                        modifier = Modifier.padding(top = 8.dp, start = 36.dp),
                        text = stringResource(id = R.string.nc_enable_mk4_nfc_desc),
                        style = NunchukTheme.typography.body
                    )
                }
                Spacer(modifier = Modifier.weight(1.0f))
                NcPrimaryDarkButton(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                    onClick = onContinueClicked,
                ) {
                    Text(text = stringResource(id = R.string.nc_text_continue))
                }
            }
        }
    }
}

@Preview
@Composable
private fun Mk4InfoContentPreview() {
    NunchukTheme {
        Mk4InfoContent()
    }
}