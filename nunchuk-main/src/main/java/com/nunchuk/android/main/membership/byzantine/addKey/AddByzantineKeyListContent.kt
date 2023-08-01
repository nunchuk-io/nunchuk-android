package com.nunchuk.android.main.membership.byzantine.addKey

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.ExperimentalMaterialApi
import androidx.compose.material.Icon
import androidx.compose.material.IconButton
import androidx.compose.material.Scaffold
import androidx.compose.material.Text
import androidx.compose.material.pullrefresh.PullRefreshIndicator
import androidx.compose.material.pullrefresh.pullRefresh
import androidx.compose.material.pullrefresh.rememberPullRefreshState
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.nunchuk.android.compose.NcPrimaryDarkButton
import com.nunchuk.android.compose.NcSpannedText
import com.nunchuk.android.compose.NcTopAppBar
import com.nunchuk.android.compose.NunchukTheme
import com.nunchuk.android.compose.SpanIndicator
import com.nunchuk.android.core.signer.SignerModel
import com.nunchuk.android.main.R
import com.nunchuk.android.main.membership.key.AddKeyCard
import com.nunchuk.android.main.membership.model.AddKeyData
import com.nunchuk.android.model.MembershipStep
import com.nunchuk.android.model.VerifyType
import com.nunchuk.android.type.SignerType
import java.util.Collections.emptyList

@OptIn(ExperimentalMaterialApi::class)
@Composable
fun AddKeyListContent(
    onAddClicked: (data: AddKeyData) -> Unit = {},
    onVerifyClicked: (data: AddKeyData) -> Unit = {},
    onContinueClicked: () -> Unit = {},
    refresh: () -> Unit = { },
    onMoreClicked: () -> Unit = {},
    keys: List<AddKeyData> = emptyList(),
    remainingTime: Int,
    isRefreshing: Boolean = false,
) {
    val state = rememberPullRefreshState(isRefreshing, refresh)
    NunchukTheme {
        Scaffold(modifier = Modifier
            .statusBarsPadding()
            .navigationBarsPadding(), topBar = {
            NcTopAppBar(
                elevation = 0.dp,
                title = stringResource(R.string.nc_estimate_remain_time, remainingTime),
                actions = {
                    IconButton(onClick = onMoreClicked) {
                        Icon(
                            painter = painterResource(id = R.drawable.ic_more),
                            contentDescription = "More icon"
                        )
                    }
                })
        }, bottomBar = {
            NcPrimaryDarkButton(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp),
                onClick = onContinueClicked,
                enabled = keys.all { it.isVerifyOrAddKey }
            ) {
                Text(text = stringResource(id = R.string.nc_text_continue))
            }
        }) { innerPadding ->
            Box(Modifier.pullRefresh(state)) {
                LazyColumn(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(innerPadding)
                        .padding(top = 16.dp)
                        .padding(horizontal = 16.dp),
                    verticalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    item {
                        Text(
                            text = stringResource(R.string.nc_let_add_your_keys),
                            style = NunchukTheme.typography.heading
                        )
                        NcSpannedText(
                            modifier = Modifier.padding(top = 16.dp, bottom = 4.dp),
                            text = stringResource(R.string.nc_byzantine_add_key_desc),
                            baseStyle = NunchukTheme.typography.body,
                            styles = mapOf(SpanIndicator('B') to SpanStyle(fontWeight = FontWeight.Bold))
                        )
                    }

                    items(keys) { key ->
                        AddKeyCard(
                            item = key,
                            onAddClicked = onAddClicked,
                            onVerifyClicked = onVerifyClicked,
                        )
                    }
                }

                PullRefreshIndicator(isRefreshing, state, Modifier.align(Alignment.TopCenter))
            }
        }
    }
}

@Preview
@Composable
fun AddKeyListScreenHoneyBadgerPreview() {
    AddKeyListContent(
        keys = listOf(
            AddKeyData(
                type = MembershipStep.HONEY_ADD_TAP_SIGNER,
                verifyType = VerifyType.NONE
            ),
            AddKeyData(
                type = MembershipStep.HONEY_ADD_HARDWARE_KEY_1,
                signer = SignerModel(
                    id = "123",
                    type = SignerType.COLDCARD_NFC,
                    name = "TAPSIGNER",
                    derivationPath = "",
                    fingerPrint = "123456"
                ),
                verifyType = VerifyType.NONE
            ),
            AddKeyData(
                type = MembershipStep.HONEY_ADD_HARDWARE_KEY_2,
            ),
            AddKeyData(type = MembershipStep.ADD_SEVER_KEY),
        ),
        remainingTime = 0,
    )
}