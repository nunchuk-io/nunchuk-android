package com.nunchuk.android.transaction.components.send.receipt

import androidx.compose.animation.animateContentSize
import androidx.compose.foundation.background
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
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.colorResource
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.nunchuk.android.compose.MODE_VIEW_ONLY
import com.nunchuk.android.compose.NcCircleImage
import com.nunchuk.android.compose.NcIcon
import com.nunchuk.android.compose.NcPrimaryDarkButton
import com.nunchuk.android.compose.NcRadioOption
import com.nunchuk.android.compose.NcScaffold
import com.nunchuk.android.compose.NcTopAppBar
import com.nunchuk.android.compose.NunchukTheme
import com.nunchuk.android.compose.PreviewCoinCard
import com.nunchuk.android.compose.fillDenim
import com.nunchuk.android.compose.fillPink
import com.nunchuk.android.compose.strokePrimary
import com.nunchuk.android.compose.textPrimary
import com.nunchuk.android.core.util.getBTCAmount
import com.nunchuk.android.core.util.getCurrencyAmount
import com.nunchuk.android.core.util.toAmount
import com.nunchuk.android.model.Amount
import com.nunchuk.android.model.CoinTag
import com.nunchuk.android.model.SigningPath
import com.nunchuk.android.model.UnspentOutput
import com.nunchuk.android.transaction.R
import com.nunchuk.android.type.CoinStatus
import com.nunchuk.android.utils.dateTimeFormat
import com.nunchuk.android.utils.simpleDateFormat
import java.util.Date
import com.nunchuk.android.core.R as CoreR

@Composable
fun TimelockNoticeScreen(
    modifier: Modifier = Modifier,
    timelockCoin: TimelockCoin,
    walletId: String,
    viewModel: TimelockNoticeViewModel = hiltViewModel(),
    onContinue: (Boolean, List<UnspentOutput>) -> Unit = { _, _ -> },
) {
    val uiState by viewModel.uiState.collectAsState()
    
    LaunchedEffect(walletId) {
        viewModel.init(walletId)
    }

    TimelockNoticeScreenContent(
        modifier = modifier,
        timelockCoin = timelockCoin,
        tags = uiState.tags,
        onContinue = onContinue
    )
}

@Composable
private fun TimelockNoticeScreenContent(
    modifier: Modifier = Modifier,
    timelockCoin: TimelockCoin,
    tags: Map<Int, CoinTag>,
    onContinue: (Boolean, List<UnspentOutput>) -> Unit
) {
    var isSelectNotLockCoin by remember { mutableStateOf(true) }
    var showDetails by remember { mutableStateOf(false) }

    val notLockCoins = timelockCoin.coins.filter {
        !timelockCoin.lockedCoins.contains(it)
    }
    val totalAmount = timelockCoin.coins.sumOf { it.amount.value }.toAmount()
    val notLockCoinAmount = notLockCoins.sumOf { it.amount.value }.toAmount()
    // Determine if it's a time lock or block lock based on the timelock value
    // If timelock is very large (like a timestamp), it's a time lock
    // If timelock is smaller (like a block number), it's a block lock
    val isTimeLock = timelockCoin.timelock > 1000000000L // Threshold to distinguish between timestamp and block number
    val timelockDate = Date(timelockCoin.timelock * 1000L).simpleDateFormat()
    val timelockDateTime = Date(timelockCoin.timelock * 1000L).dateTimeFormat()

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
                        onContinue(!isSelectNotLockCoin, selectedCoins)
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
                    .padding(horizontal = 16.dp)
                    .verticalScroll(rememberScrollState()),
            ) {
                Spacer(modifier = Modifier.height(16.dp))
                // Top circle icon
                NcCircleImage(
                    modifier = Modifier
                        .align(Alignment.CenterHorizontally),
                    size = 96.dp,
                    iconSize = 60.dp,
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
                    text = stringResource(
                        R.string.nc_timelock_notice_description,
                        pluralStringResource(R.plurals.nc_coins_with_count, notLockCoins.size, notLockCoins.size),
                        pluralStringResource(R.plurals.nc_coins_with_count, timelockCoin.lockedCoins.size, timelockCoin.lockedCoins.size),
                        if (isTimeLock) timelockDate else "block ${timelockCoin.timelock}"
                    ),
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
                        text = "Send ${notLockCoinAmount.getBTCAmount()} (${notLockCoinAmount.getCurrencyAmount()})",
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
                    onClick = { isSelectNotLockCoin = false }
                ) {
                    Text(
                        text = "Send full amount ${totalAmount.getBTCAmount()} (${totalAmount.getCurrencyAmount()})",
                        style = NunchukTheme.typography.body.copy(fontWeight = FontWeight.Bold)
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = if (isTimeLock) {
                            "Sign now, broadcast after $timelockDate"
                        } else {
                            "Sign now, broadcast after block ${timelockCoin.timelock}"
                        },
                        style = NunchukTheme.typography.bodySmall
                    )
                }
                HorizontalDivider(
                    modifier = Modifier.padding(top = 24.dp),
                    color = MaterialTheme.colorScheme.strokePrimary,
                    thickness = 1.dp
                )
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable { showDetails = !showDetails }
                        .padding(vertical = 16.dp)
                        .animateContentSize(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = if (showDetails) {
                            stringResource(R.string.nc_transaction_less_details)
                        } else {
                            stringResource(R.string.nc_transaction_more_details)
                        },
                        style = NunchukTheme.typography.title,
                        color = MaterialTheme.colorScheme.textPrimary
                    )
                    val arrowRes =
                        if (showDetails) R.drawable.ic_caret_up else R.drawable.ic_caret_down
                    NcIcon(painter = painterResource(id = arrowRes), contentDescription = null)
                }
                if (showDetails) {
                    // Locked Coins Section (Pink)
                    if (timelockCoin.lockedCoins.isNotEmpty()) {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .background(
                                    color = MaterialTheme.colorScheme.fillPink,
                                    shape = RoundedCornerShape(12.dp)
                                )
                                .padding(16.dp),
                            verticalArrangement = Arrangement.spacedBy(16.dp)
                        ) {
                            Row {
                                NcIcon(
                                    modifier = Modifier
                                        .padding(end = 4.dp)
                                        .size(20.dp)
                                        .align(Alignment.CenterVertically),
                                    painter = painterResource(CoreR.drawable.ic_timer),
                                    contentDescription = "Timer",
                                )

                                Text(
                                    text = if (isTimeLock) {
                                        "Ready to broadcast after $timelockDateTime"
                                    } else {
                                        "Ready to broadcast after block ${timelockCoin.timelock}"
                                    },
                                    style = NunchukTheme.typography.titleSmall,
                                    modifier = Modifier.align(Alignment.CenterVertically)
                                )
                            }
                            timelockCoin.lockedCoins.forEach { coin ->
                                PreviewCoinCard(
                                    modifier = Modifier
                                        .background(
                                            color = colorResource(R.color.nc_background_primary),
                                            shape = RoundedCornerShape(12.dp)
                                        ),
                                    output = coin,
                                    tags = tags,
                                    mode = MODE_VIEW_ONLY
                                )
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(20.dp))

                    // Non-Locked Coins Section (Blue)
                    if (notLockCoins.isNotEmpty()) {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .background(
                                    color = MaterialTheme.colorScheme.fillDenim, // Light blue background
                                    shape = RoundedCornerShape(12.dp)
                                )
                                .padding(16.dp),
                            verticalArrangement = Arrangement.spacedBy(16.dp)
                        ) {
                            Row {
                                NcIcon(
                                    modifier = Modifier
                                        .padding(end = 4.dp)
                                        .size(20.dp)
                                        .align(Alignment.CenterVertically),
                                    painter = painterResource(CoreR.drawable.ic_check_circle_2),
                                    contentDescription = "Checked",
                                )

                                Text(
                                    text = "Can sign and broadcast immediately",
                                    style = NunchukTheme.typography.titleSmall,
                                    modifier = Modifier.align(Alignment.CenterVertically)
                                )
                            }
                            notLockCoins.forEach { coin ->
                                PreviewCoinCard(
                                    modifier = Modifier
                                        .background(
                                            color = colorResource(R.color.nc_background_primary),
                                            shape = RoundedCornerShape(12.dp)
                                        ),
                                    output = coin,
                                    tags = tags,
                                    mode = MODE_VIEW_ONLY
                                )
                            }
                        }
                    }
                } else {
                    HorizontalDivider(
                        color = MaterialTheme.colorScheme.strokePrimary,
                        thickness = 1.dp
                    )
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
                isLocked = false,
                time = System.currentTimeMillis(),
                tags = setOf(),
                memo = "",
                status = CoinStatus.CONFIRMED
            ),
            UnspentOutput(
                txid = "tx2",
                vout = 1,
                amount = Amount(30000000), // 0.3 BTC
                address = "bc1q...",
                isLocked = true,
                time = System.currentTimeMillis(),
                tags = setOf(),
                memo = "",
                status = CoinStatus.CONFIRMED
            ),
            UnspentOutput(
                txid = "tx3",
                vout = 0,
                amount = Amount(20000000), // 0.2 BTC
                address = "bc1q...",
                isLocked = false,
                time = System.currentTimeMillis(),
                tags = setOf(),
                memo = "",
                status = CoinStatus.CONFIRMED
            )
        ),
        timelock = 1735689600L, // May 15, 2025
        lockedCoins = listOf(
            UnspentOutput(
                txid = "tx2",
                vout = 1,
                amount = Amount(30000000),
                address = "bc1q...",
                isLocked = true,
                time = System.currentTimeMillis(),
                tags = setOf(),
                memo = "",
                status = CoinStatus.CONFIRMED
            )
        ),
        signingPath = SigningPath(path = listOf(listOf(1, 1)))
    )

    // Static preview without ViewModel for better performance
    TimelockNoticeScreenContent(
        timelockCoin = dummyTimelockCoin,
        tags = emptyMap(),
        onContinue = { isSendAll, coins ->
            // Preview callback
        }
    )
}