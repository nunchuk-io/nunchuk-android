package com.nunchuk.android.main.membership.key.desktop

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.Icon
import androidx.compose.material.IconButton
import androidx.compose.material.Scaffold
import androidx.compose.material.Text
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
        ) { innerPadding ->
            Column(modifier = Modifier
                .padding(innerPadding)
                .fillMaxSize()
                .verticalScroll(rememberScrollState())) {
                NcImageAppBar(
                    backgroundRes = R.drawable.bg_waiting_add_desktop_key,
                    title = stringResource(
                        id = R.string.nc_estimate_remain_time,
                        remainTime
                    ),
                    actions = {
                        IconButton(onClick = onMoreClicked) {
                            Icon(
                                painter = painterResource(id = R.drawable.ic_more),
                                contentDescription = "More icon"
                            )
                        }
                    },
                )
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
        title = stringResource(R.string.nc_waiting_for_ledger_to_be_added),
        desc = stringResource(R.string.nc_add_trezor_using_desktop_desc),
        button = stringResource(R.string.nc_i_have_already_added_ledger)
    )
}