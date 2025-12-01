package com.nunchuk.android.main.rollover.transferfund

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.systemBarsPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.LocalMinimumInteractiveComponentEnforcement
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.ComposeView
import androidx.compose.ui.platform.ViewCompositionStrategy
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.tooling.preview.PreviewLightDark
import androidx.compose.ui.unit.dp
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.findNavController
import androidx.navigation.fragment.navArgs
import com.nunchuk.android.compose.NcCheckBox
import com.nunchuk.android.compose.NcHighlightText
import com.nunchuk.android.compose.NcIcon
import com.nunchuk.android.compose.NcPrimaryDarkButton
import com.nunchuk.android.compose.NcScaffold
import com.nunchuk.android.compose.NcTopAppBar
import com.nunchuk.android.compose.NunchukTheme
import com.nunchuk.android.compose.fillPink
import com.nunchuk.android.compose.greyLight
import com.nunchuk.android.compose.textPrimary
import com.nunchuk.android.compose.whisper
import com.nunchuk.android.core.util.RollOverWalletSource
import com.nunchuk.android.core.util.getBTCAmount
import com.nunchuk.android.core.util.getCurrencyAmount
import com.nunchuk.android.main.R
import com.nunchuk.android.main.rollover.RollOverWalletUiState
import com.nunchuk.android.main.rollover.RollOverWalletViewModel
import com.nunchuk.android.model.Amount
import com.nunchuk.android.model.Wallet
import com.nunchuk.android.nav.NunchukNavigator
import com.nunchuk.android.type.MiniscriptTimelockBased
import com.nunchuk.android.utils.dateTimeFormat
import dagger.hilt.android.AndroidEntryPoint
import java.util.Date
import javax.inject.Inject
import com.nunchuk.android.transaction.R as TransactionR
import com.nunchuk.android.wallet.R as WalletR

@AndroidEntryPoint
class RollOverTransferFundFragment : Fragment() {
    @Inject
    lateinit var navigator: NunchukNavigator

    private val args: RollOverTransferFundFragmentArgs by navArgs()

    private val rollOverWalletViewModel: RollOverWalletViewModel by activityViewModels()

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        return ComposeView(requireContext()).apply {
            setViewCompositionStrategy(ViewCompositionStrategy.DisposeOnViewTreeLifecycleDestroyed)

            setContent {
                val rollOverWalletState by rollOverWalletViewModel.uiState.collectAsStateWithLifecycle()
                RollOverTransferFundView(
                    source = rollOverWalletViewModel.getSource(),
                    rollOverWalletState = rollOverWalletState,
                    onContinueClicked = {
                        rollOverWalletViewModel.updateReplaceKeyConfig(it)
                        findNavController().navigate(
                            RollOverTransferFundFragmentDirections.actionRollOverTransferFundFragmentToRollOverCoinControlIntroFragment(
                                oldWalletId = args.oldWalletId,
                                newWalletId = args.newWalletId
                            )
                        )
                    },
                )
            }
        }
    }
}

@Composable
private fun RollOverTransferFundView(
    source: Int = 0,
    onContinueClicked: (Boolean) -> Unit = { },
    rollOverWalletState: RollOverWalletUiState,
) {
    RollOverTransferFundContent(
        source = source,
        rollOverWalletState = rollOverWalletState,
        onContinueClicked = onContinueClicked,
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun RollOverTransferFundContent(
    source: Int = 0,
    onContinueClicked: (Boolean) -> Unit = { },
    rollOverWalletState: RollOverWalletUiState = RollOverWalletUiState(),
) {
    var isRemoveUnusedKeys by rememberSaveable { mutableStateOf(false) }
    NunchukTheme {
        NcScaffold(
            modifier = Modifier.systemBarsPadding(),
            topBar = {
                NcTopAppBar(title = "")
            },
            bottomBar = {
                Column {
                    rollOverWalletState.furthestTimelock?.let { (lockBased, lockedTime) ->
                        Row(
                            modifier = Modifier
                                .padding(top = 16.dp, start = 16.dp, end = 16.dp)
                                .fillMaxWidth()
                                .background(
                                    color = MaterialTheme.colorScheme.fillPink,
                                    shape = RoundedCornerShape(8.dp)
                                )
                                .padding(12.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            NcIcon(
                                painter = painterResource(id = TransactionR.drawable.ic_timer),
                                contentDescription = null,
                                modifier = Modifier.size(36.dp),
                            )
                            val lockText = if (lockBased == MiniscriptTimelockBased.HEIGHT_LOCK) {
                                stringResource(
                                    id = TransactionR.string.nc_timelocked_until_block,
                                    lockedTime
                                )
                            } else {
                                val date = Date(lockedTime * 1000L)
                                stringResource(
                                    id = TransactionR.string.nc_timelocked_until_date,
                                    date.dateTimeFormat()
                                )
                            }
                            Text(
                                text = lockText,
                                style = NunchukTheme.typography.titleSmall,
                                color = MaterialTheme.colorScheme.textPrimary
                            )
                        }
                    }
                    NcPrimaryDarkButton(
                        modifier = Modifier
                            .padding(16.dp)
                            .fillMaxWidth(),
                        onClick = { onContinueClicked(isRemoveUnusedKeys) }) {
                        Text(text = stringResource(R.string.nc_text_continue))
                    }
                }
            }
        ) { innerPadding ->
            Column(
                modifier = Modifier
                    .padding(innerPadding)
                    .padding(horizontal = 16.dp)
                    .verticalScroll(rememberScrollState()),
            ) {
                Text(
                    text = stringResource(R.string.nc_transfer_funds),
                    style = NunchukTheme.typography.heading,
                )

                val isMiniscriptWallet = rollOverWalletState.oldWallet.miniscript.isNotEmpty()
                val spendableNowAmount = rollOverWalletState.spendableNowAmount
                val timelockedAmount = rollOverWalletState.timelockedAmount

                // Show different description for miniscript wallets
                if (isMiniscriptWallet && spendableNowAmount != null && timelockedAmount != null) {
                    Column(
                        modifier = Modifier.padding(top = 16.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        NcHighlightText(
                            text = stringResource(
                                R.string.nc_transfer_funds_desc_miniscript,
                                rollOverWalletState.newWallet.name
                            ),
                            style = NunchukTheme.typography.body,
                        )
                        Text(
                            text = stringResource(R.string.nc_transfer_funds_desc_miniscript_hint),
                            style = NunchukTheme.typography.body,
                        )
                    }
                } else {
                    NcHighlightText(
                        text = stringResource(
                            R.string.nc_transfer_funds_desc,
                            rollOverWalletState.newWallet.name
                        ),
                        style = NunchukTheme.typography.body,
                        modifier = Modifier.padding(top = 16.dp)
                    )
                }

                if (isMiniscriptWallet
                    && spendableNowAmount != null
                    && timelockedAmount != null) {
                    // Miniscript wallet - show breakdown
                    Column(
                        modifier = Modifier
                            .padding(top = 16.dp)
                            .fillMaxWidth()
                            .background(
                                color = MaterialTheme.colorScheme.greyLight,
                                shape = NunchukTheme.shape.medium
                            )
                            .padding(16.dp),
                        verticalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        // Spendable now
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = stringResource(WalletR.string.nc_spendable_now_title),
                                style = NunchukTheme.typography.body,
                            )
                            Column(
                                horizontalAlignment = Alignment.End,
                                verticalArrangement = Arrangement.spacedBy(4.dp)
                            ) {
                                Text(
                                    text = spendableNowAmount.getBTCAmount(),
                                    style = NunchukTheme.typography.title,
                                )
                                Text(
                                    text = spendableNowAmount.getCurrencyAmount(),
                                    style = NunchukTheme.typography.bodySmall,
                                )
                            }
                        }

                        HorizontalDivider(
                            color = MaterialTheme.colorScheme.whisper,
                            thickness = 1.dp
                        )

                        // Timelocked
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = "Timelocked",
                                style = NunchukTheme.typography.body,
                            )
                            Column(
                                horizontalAlignment = Alignment.End,
                                verticalArrangement = Arrangement.spacedBy(4.dp)
                            ) {
                                Text(
                                    text = timelockedAmount.getBTCAmount(),
                                    style = NunchukTheme.typography.title,
                                )
                                Text(
                                    text = timelockedAmount.getCurrencyAmount(),
                                    style = NunchukTheme.typography.bodySmall,
                                )
                            }
                        }

                        HorizontalDivider(
                            color = MaterialTheme.colorScheme.whisper,
                            thickness = 1.dp
                        )

                        // Total balance
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = stringResource(R.string.nc_total_balance),
                                style = NunchukTheme.typography.body,
                            )
                            Column(
                                horizontalAlignment = Alignment.End,
                                verticalArrangement = Arrangement.spacedBy(4.dp)
                            ) {
                                Text(
                                    text = rollOverWalletState.oldWallet.getBTCAmount(),
                                    style = NunchukTheme.typography.title,
                                )
                                Text(
                                    text = rollOverWalletState.oldWallet.getCurrencyAmount(),
                                    style = NunchukTheme.typography.bodySmall,
                                )
                            }
                        }
                    }
                } else {
                    // Regular wallet - show simple balance
                    Row(
                        modifier = Modifier
                            .padding(top = 16.dp)
                            .fillMaxWidth()
                            .background(
                                color = MaterialTheme.colorScheme.greyLight,
                                shape = NunchukTheme.shape.medium
                            )
                            .padding(12.dp),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Text(
                            text = stringResource(R.string.nc_existing_balance),
                            style = NunchukTheme.typography.body,
                        )

                        Spacer(modifier = Modifier.weight(1f))

                        Column(
                            verticalArrangement = Arrangement.spacedBy(4.dp),
                            horizontalAlignment = Alignment.End
                        ) {
                            Text(
                                text = rollOverWalletState.oldWallet.getBTCAmount(),
                                style = NunchukTheme.typography.title,
                            )

                            Text(
                                text = rollOverWalletState.oldWallet.getCurrencyAmount(),
                                style = NunchukTheme.typography.bodySmall,
                            )
                        }
                    }
                }

                if (!rollOverWalletState.isFreeWallet && source == RollOverWalletSource.REPLACE_KEY) {
                    // 1dp spacer with top and bottom padding are 24dp and background whisper
                    HorizontalDivider(
                        modifier = Modifier
                            .padding(vertical = 24.dp),
                        color = MaterialTheme.colorScheme.whisper,
                        thickness = 1.dp
                    )

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(12.dp),
                    ) {
                        Column(
                            modifier = Modifier.weight(1f),
                            verticalArrangement = Arrangement.spacedBy(4.dp),
                        ) {
                            Text(
                                text = stringResource(R.string.nc_remove_unused_keys),
                                style = NunchukTheme.typography.body,
                            )

                            Text(
                                text = stringResource(R.string.nc_remove_unused_key_desc),
                                style = NunchukTheme.typography.bodySmall,
                            )
                        }

                        CompositionLocalProvider(LocalMinimumInteractiveComponentEnforcement provides false) {
                            NcCheckBox(
                                checked = isRemoveUnusedKeys,
                                onCheckedChange = { isRemoveUnusedKeys = it })
                        }
                    }
                }
            }
        }
    }
}

@Composable
@Preview
private fun RollOverTransferFundScreenContentPreview() {
    val mockOldWallet = Wallet(
        id = "old_wallet_id",
        name = "Old Wallet",
        balance = Amount(value = 20000000L) // 0.2 BTC
    )

    val mockNewWallet = Wallet(
        id = "new_wallet_id",
        name = "New Wallet",
        balance = Amount(value = 0L)
    )

    val mockState = RollOverWalletUiState(
        oldWallet = mockOldWallet,
        newWallet = mockNewWallet,
        isFreeWallet = false
    )

    RollOverTransferFundContent(
        source = 0,
        rollOverWalletState = mockState,
        onContinueClicked = {}
    )
}

@PreviewLightDark
@Composable
private fun RollOverTransferFundMiniscriptPreview() {
    NunchukTheme {
        val mockOldWallet = Wallet(
            id = "old_wallet_id",
            name = "Old Wallet",
            miniscript = "andor(thresh(2,pk(key_0_0),pk(key_1_0)),older(4320))",
            balance = Amount(value = 20000000L) // 0.2 BTC
        )
        
        val mockNewWallet = Wallet(
            id = "new_wallet_id",
            name = "New Wallet",
            balance = Amount(value = 0L)
        )
        
        val mockSpendableNowAmount = Amount(value = 15000000L) // 0.15 BTC
        val mockTimelockedAmount = Amount(value = 5000000L) // 0.05 BTC
        val mockFurthestTimelock = MiniscriptTimelockBased.TIME_LOCK to (System.currentTimeMillis() / 1000 + 86400 * 30) // 30 days from now
        
        val mockState = RollOverWalletUiState(
            oldWallet = mockOldWallet,
            newWallet = mockNewWallet,
            spendableNowAmount = mockSpendableNowAmount,
            timelockedAmount = mockTimelockedAmount,
            furthestTimelock = mockFurthestTimelock,
            isFreeWallet = false
        )
        
        RollOverTransferFundContent(
            source = 0,
            rollOverWalletState = mockState,
            onContinueClicked = {}
        )
    }
}