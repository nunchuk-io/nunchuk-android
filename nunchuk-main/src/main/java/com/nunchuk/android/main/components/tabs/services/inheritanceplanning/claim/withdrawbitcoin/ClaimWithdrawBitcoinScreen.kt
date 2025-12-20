package com.nunchuk.android.main.components.tabs.services.inheritanceplanning.claim.withdrawbitcoin

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.colorResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.PreviewLightDark
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.LifecycleResumeEffect
import com.nunchuk.android.compose.NcOptionItem
import com.nunchuk.android.compose.NcPrimaryDarkButton
import com.nunchuk.android.compose.NcScaffold
import com.nunchuk.android.compose.NcSelectableBottomSheetWithIcon
import com.nunchuk.android.compose.NcToastType
import com.nunchuk.android.compose.NcTopAppBar
import com.nunchuk.android.compose.NunchukTheme
import com.nunchuk.android.compose.SelectableItem
import com.nunchuk.android.compose.montserratMedium
import com.nunchuk.android.compose.showNunchukSnackbar
import com.nunchuk.android.core.R
import com.nunchuk.android.core.util.getBTCAmount
import com.nunchuk.android.core.util.getCurrencyAmount
import com.nunchuk.android.core.util.toAmount
import com.nunchuk.android.main.components.tabs.services.inheritanceplanning.withdrawbitcoin.InheritanceClaimWithdrawBitcoinEvent
import com.nunchuk.android.main.components.tabs.services.inheritanceplanning.withdrawbitcoin.InheritanceClaimWithdrawBitcoinViewModel
import com.nunchuk.android.main.R as MainR

@Composable
fun ClaimWithdrawBitcoinScreen(
    snackState: SnackbarHostState,
    bsms: String?,
    balance: Double,
    modifier: Modifier = Modifier,
    onNavigateToInputAmount: () -> Unit = {},
    onNavigateToSelectWallet: () -> Unit = {},
    onNavigateToWalletIntermediary: () -> Unit = {},
    onNavigateToAddReceipt: () -> Unit = {},
    viewModel: InheritanceClaimWithdrawBitcoinViewModel = hiltViewModel(),
) {
    var showSweepOptions by remember { mutableStateOf(false) }

    // Check for new wallets on resume
    LifecycleResumeEffect(Unit) {
        viewModel.checkNewWallets()
        onPauseOrDispose { }
    }

    // Observe events
    LaunchedEffect(Unit) {
        viewModel.event.collect { event ->
            when (event) {
                is InheritanceClaimWithdrawBitcoinEvent.Error -> {
                    snackState.showNunchukSnackbar(
                        message = event.message,
                        type = NcToastType.ERROR
                    )
                }

                is InheritanceClaimWithdrawBitcoinEvent.CheckHasWallet -> {
                    if (event.isHasWallet) {
                        onNavigateToSelectWallet()
                    } else {
                        onNavigateToWalletIntermediary()
                    }
                }

                is InheritanceClaimWithdrawBitcoinEvent.CheckNewWallet -> {
                    if (event.isNewWallet) {
                        onNavigateToSelectWallet()
                    }
                }
            }
        }
    }

    ClaimWithdrawBitcoinContent(
        modifier = modifier,
        balance = balance,
        snackState = snackState,
        showSweepOptions = showSweepOptions,
        onDismissSweepOptions = {
            showSweepOptions = false
        },
        onContinue = { selectedOption ->
            when (selectedOption) {
                0 -> onNavigateToInputAmount()
                1 -> showSweepOptions = true
            }
        },
        onSweepOptionSelected = { option ->
            if (option == 0) { // Sweep to wallet
                viewModel.checkWallet(bsms)
            } else if (option == 1) { // Sweep to external address
                onNavigateToAddReceipt()
            }
            showSweepOptions = false
        },
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun ClaimWithdrawBitcoinContent(
    modifier: Modifier = Modifier,
    balance: Double = 0.0,
    snackState: SnackbarHostState = remember { SnackbarHostState() },
    onContinue: (Int) -> Unit = {},
    showSweepOptions: Boolean = false,
    onDismissSweepOptions: () -> Unit = {},
    onSweepOptionSelected: (Int) -> Unit = {},
) {
    val selectOption = remember { mutableIntStateOf(0) }

    NcScaffold(
        modifier = modifier
            .navigationBarsPadding(),
        snackState = snackState,
        topBar = {
            NcTopAppBar(
                backgroundColor = colorResource(id = MainR.color.nc_fill_denim),
                title = "",
            )
        },
        bottomBar = {
            NcPrimaryDarkButton(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp),
                onClick = {
                    onContinue(selectOption.intValue)
                },
            ) {
                Text(text = stringResource(id = R.string.nc_text_continue))
            }
        }
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .padding(innerPadding)
                .fillMaxHeight()
                .verticalScroll(rememberScrollState())
        ) {
            Column(
                modifier = Modifier
                    .background(color = colorResource(id = MainR.color.nc_fill_denim))
                    .height(215.dp)
                    .fillMaxWidth(),
                verticalArrangement = Arrangement.Center
            ) {
                Text(
                    modifier = Modifier
                        .padding(horizontal = 16.dp)
                        .fillMaxWidth(),
                    text = stringResource(R.string.nc_your_inheritance),
                    style = NunchukTheme.typography.title,
                    textAlign = TextAlign.Center
                )
                Text(
                    modifier = Modifier
                        .padding(horizontal = 16.dp)
                        .fillMaxWidth(),
                    text = balance.toAmount().getBTCAmount(),
                    style = TextStyle(
                        fontSize = 36.sp,
                        fontWeight = FontWeight.Bold,
                        fontFamily = montserratMedium,
                        color = colorResource(
                            id = MainR.color.nc_text_primary
                        )
                    ),
                    textAlign = TextAlign.Center
                )
                Text(
                    modifier = Modifier
                        .padding(horizontal = 16.dp)
                        .fillMaxWidth(),
                    text = balance.toAmount().getCurrencyAmount(),
                    style = NunchukTheme.typography.title,
                    textAlign = TextAlign.Center
                )
            }
            Text(
                modifier = Modifier.padding(top = 12.dp, start = 16.dp, end = 16.dp),
                text = stringResource(R.string.nc_withdraw_bitcoin),
                style = NunchukTheme.typography.heading
            )
            Text(
                modifier = Modifier.padding(
                    start = 16.dp, end = 16.dp, top = 12.dp
                ),
                text = stringResource(R.string.nc_withdraw_bitcoin_desc),
                style = NunchukTheme.typography.body
            )

            NcOptionItem(
                modifier = Modifier.padding(top = 24.dp, start = 16.dp, end = 16.dp),
                isSelected = selectOption.intValue == 0,
                label = stringResource(R.string.nc_withdraw_a_custom_amount),
                onClick = {
                    selectOption.intValue = 0
                }
            )

            NcOptionItem(
                modifier = Modifier.padding(top = 12.dp, start = 16.dp, end = 16.dp),
                isSelected = selectOption.intValue == 1,
                label = stringResource(R.string.nc_withdraw_full_balance_now),
                onClick = {
                    selectOption.intValue = 1
                }
            )
        }
    }

    // Show sweep options dialog
    if (showSweepOptions) {
        NcSelectableBottomSheetWithIcon(
            sheetState = rememberModalBottomSheetState(),
            items = listOf(
                SelectableItem(
                    resId = R.drawable.ic_wallet_info,
                    text = stringResource(R.string.nc_withdraw_nunchuk_wallet)
                ),
                SelectableItem(
                    resId = R.drawable.ic_sending_bitcoin,
                    text = stringResource(R.string.nc_withdraw_to_an_address)
                )
            ),
            onSelected = { index ->
                onSweepOptionSelected(index)
            },
            onDismiss = onDismissSweepOptions,
        )
    }
}

@PreviewLightDark
@Composable
private fun ClaimWithdrawBitcoinScreenPreview() {
    NunchukTheme {
        ClaimWithdrawBitcoinContent()
    }
}

