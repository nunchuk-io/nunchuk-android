package com.nunchuk.android.transaction.components.send.receipt

import androidx.compose.animation.animateContentSize
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.nunchuk.android.compose.NcCircleImage
import com.nunchuk.android.compose.NcIcon
import com.nunchuk.android.compose.NcPrimaryDarkButton
import com.nunchuk.android.compose.NcRadioOption
import com.nunchuk.android.compose.NcScaffold
import com.nunchuk.android.compose.NcTopAppBar
import com.nunchuk.android.compose.NunchukTheme
import com.nunchuk.android.compose.textPrimary
import com.nunchuk.android.core.util.getBTCAmount
import com.nunchuk.android.core.util.getCurrencyAmount
import com.nunchuk.android.core.util.toAmount
import com.nunchuk.android.model.Amount
import com.nunchuk.android.model.SigningPath
import com.nunchuk.android.model.UnspentOutput
import com.nunchuk.android.transaction.R
import com.nunchuk.android.utils.simpleDateFormat
import java.util.Date
import com.nunchuk.android.core.R as CoreR

@Composable
fun TimelockNoticeScreen(
    modifier: Modifier = Modifier,
    timelockCoin: TimelockCoin,
    onContinue: (List<UnspentOutput>) -> Unit = {},
) {
    var isSelectNotLockCoin by remember { mutableStateOf(true) }
    var showDetails by remember { mutableStateOf(false) }
    val notLockCoins = timelockCoin.coins.filter {
        !timelockCoin.lockedCoins.contains(it)
    }
    val totalAmount = timelockCoin.coins.sumOf { it.amount.value }.toAmount()
    val notLockCoinAmount = notLockCoins.sumOf { it.amount.value }.toAmount()

    NunchukTheme {
        NcScaffold(
            modifier = modifier
                .fillMaxSize()
                .navigationBarsPadding(),
            topBar = {
                NcTopAppBar(title = "")
            },
            bottomBar = {
                NcPrimaryDarkButton(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                    onClick = {
                        val selectedCoins = if (isSelectNotLockCoin) {
                            notLockCoins
                        } else {
                            timelockCoin.coins
                        }
                        onContinue(selectedCoins)
                    }
                ) {
                    Text(text = stringResource(R.string.nc_text_continue))
                }
            }
        ) { innerPadding ->
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(innerPadding)
                    .padding(horizontal = 16.dp),
            ) {
                Spacer(modifier = Modifier.height(16.dp))
                // Top circle icon
                NcCircleImage(
                    modifier = Modifier
                        .align(Alignment.CenterHorizontally),
                    size = 120.dp,
                    iconSize = 56.dp,
                    resId = CoreR.drawable.ic_timer,
                )
                Spacer(modifier = Modifier.height(24.dp))
                // Title
                Text(
                    text = stringResource(R.string.nc_timelock_notice_title),
                    style = NunchukTheme.typography.heading,
                    color = MaterialTheme.colorScheme.textPrimary,
                )
                Spacer(modifier = Modifier.height(12.dp))
                // Description (static for now)
                Text(
                    text = stringResource(R.string.nc_timelock_notice_description, notLockCoins.size, timelockCoin.lockedCoins.size, Date(timelockCoin.timelock.times(1000L)).simpleDateFormat()),
                    style = NunchukTheme.typography.body,
                    color = MaterialTheme.colorScheme.textPrimary,
                )
                Spacer(modifier = Modifier.height(24.dp))
                NcRadioOption(
                    modifier = Modifier.fillMaxWidth(),
                    isSelected = isSelectNotLockCoin,
                    onClick = { isSelectNotLockCoin = true }
                ) {
                    Text(
                        text = "Send ${notLockCoinAmount.getBTCAmount()} (\$${notLockCoinAmount.getCurrencyAmount()})",
                        style = NunchukTheme.typography.body.copy(fontWeight = FontWeight.Bold)
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = stringResource(R.string.nc_sign_and_broadcast_now),
                        style = NunchukTheme.typography.bodySmall
                    )
                }
                Spacer(modifier = Modifier.height(12.dp))
                NcRadioOption(
                    modifier = Modifier.fillMaxWidth(),
                    isSelected = !isSelectNotLockCoin,
                    onClick = { isSelectNotLockCoin = false}
                ) {
                    Text(
                        text = "Send full amount ${totalAmount.getBTCAmount()} (\$${totalAmount.getCurrencyAmount()})",
                        style = NunchukTheme.typography.body.copy(fontWeight = FontWeight.Bold)
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = "Sign now, broadcast after ${Date(timelockCoin.timelock * 1000L).simpleDateFormat()}",
                        style = NunchukTheme.typography.bodySmall
                    )
                }
                Spacer(modifier = Modifier.height(8.dp))
                // More details toggle
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable { showDetails = !showDetails }
                        .padding(vertical = 12.dp)
                        .animateContentSize(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = stringResource(R.string.nc_transaction_more_details),
                        style = NunchukTheme.typography.title,
                        color = MaterialTheme.colorScheme.textPrimary
                    )
                    val arrowRes = if (showDetails) R.drawable.ic_caret_up else R.drawable.ic_caret_down
                    NcIcon(painter = painterResource(id = arrowRes), contentDescription = null)
                }
                if (showDetails) {

                }
            }
        }
    }
}

@Preview
@Composable
fun TimelockNoticeScreenPreview() {
    val dummyTimelockCoin = TimelockCoin(
        coins = listOf(
            UnspentOutput(
                txid = "tx1",
                vout = 0,
                amount = Amount(50000000), // 0.5 BTC
                address = "bc1q...",
                isLocked = false
            ),
            UnspentOutput(
                txid = "tx2",
                vout = 1,
                amount = Amount(30000000), // 0.3 BTC
                address = "bc1q...",
                isLocked = true
            )
        ),
        timelock = 1735689600L, // May 15, 2025
        lockedCoins = listOf(
            UnspentOutput(
                txid = "tx2",
                vout = 1,
                amount = Amount(30000000),
                address = "bc1q...",
                isLocked = true
            )
        ),
        signingPath = SigningPath(path = listOf(listOf(1, 1)))
    )

    TimelockNoticeScreen(
        timelockCoin = dummyTimelockCoin,
        onContinue = { coins ->
            // Preview callback
        }
    )
}