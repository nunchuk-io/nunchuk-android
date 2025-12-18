package com.nunchuk.android.main.membership.key.desktop

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.nunchuk.android.compose.NcImageAppBar
import com.nunchuk.android.compose.NcPrimaryDarkButton
import com.nunchuk.android.compose.NunchukTheme
import com.nunchuk.android.main.R

@Composable
fun WaitingDesktopKeyContent(
    onConfirmAddLedger: () -> Unit = {},
    onMoreClicked: () -> Unit = {},
    remainTime: Int = 0,
    title: String = "",
    desc: String = "",
    button: String = "",
    isMembershipFlow: Boolean = true,
) {
    NunchukTheme {
        Scaffold(
            modifier = Modifier.navigationBarsPadding(),
            bottomBar = {
                NcPrimaryDarkButton(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                    onClick = onConfirmAddLedger
                ) {
                    Text(
                        text = button
                    )
                }
            },
            topBar = {
                NcImageAppBar(
                    backgroundRes = R.drawable.bg_waiting_add_desktop_key,
                    title = stringResource(
                        id = R.string.nc_estimate_remain_time,
                        remainTime
                    ).takeIf { isMembershipFlow },
                    actions = {
                        if (isMembershipFlow) {
                            IconButton(onClick = onMoreClicked) {
                                Icon(
                                    painter = painterResource(id = R.drawable.ic_more),
                                    contentDescription = "More icon"
                                )
                            }
                        }
                    },
                )
            }
        ) { innerPadding ->
            Column(modifier = Modifier
                .padding(innerPadding)
                .fillMaxSize()
                .verticalScroll(rememberScrollState())) {
                Text(
                    modifier = Modifier.padding(16.dp),
                    text = title,
                    style = NunchukTheme.typography.heading
                )
                Text(
                    modifier = Modifier.padding(horizontal = 16.dp),
                    text = desc,
                    style = NunchukTheme.typography.body
                )
            }
        }
    }
}

@Preview
@Composable
private fun AddLedgerScreenPreview() {
    WaitingDesktopKeyContent(
        title = stringResource(R.string.nc_waiting_for_desktop_key_to_be_added, stringResource(id = R.string.nc_ledger)),
        desc = stringResource(R.string.nc_add_key_using_desktop_desc, stringResource(id = R.string.nc_ledger)),
        button = stringResource(R.string.nc_i_have_already_added_desktop_key, stringResource(id = R.string.nc_ledger))
    )
}