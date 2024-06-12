package com.nunchuk.android.main.membership.wallet

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
import androidx.compose.foundation.layout.systemBarsPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Checkbox
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
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.nunchuk.android.compose.NcPrimaryDarkButton
import com.nunchuk.android.compose.NcScaffold
import com.nunchuk.android.compose.NcTopAppBar
import com.nunchuk.android.compose.NunchukTheme
import com.nunchuk.android.compose.greyLight
import com.nunchuk.android.compose.whisper
import com.nunchuk.android.core.data.model.TxReceipt
import com.nunchuk.android.core.util.getBTCAmount
import com.nunchuk.android.core.util.getCurrencyAmount
import com.nunchuk.android.core.util.pureBTC
import com.nunchuk.android.main.R
import com.nunchuk.android.main.membership.MembershipActivity
import com.nunchuk.android.model.Wallet
import com.nunchuk.android.nav.NunchukNavigator
import dagger.hilt.android.AndroidEntryPoint
import javax.inject.Inject

@AndroidEntryPoint
class TransferFundFragment : Fragment() {
    @Inject
    lateinit var navigator: NunchukNavigator

    private val viewModel: TransferFundViewModel by viewModels()
    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        return ComposeView(requireContext()).apply {
            setViewCompositionStrategy(ViewCompositionStrategy.DisposeOnViewTreeLifecycleDestroyed)

            setContent {
                TransferFundView(
                    viewModel = viewModel,
                    onContinueClicked = { fromWallet, _, removeKey ->
                        val address = viewModel.uiState.value.address
                        val groupId = (activity as MembershipActivity).groupId
                        viewModel.updateReplaceKeyConfig(
                            groupId = groupId,
                            walletId = fromWallet.id,
                            isRemoveKey = removeKey
                        )
                        navigator.openEstimatedFeeScreen(
                            activityContext = requireActivity(),
                            walletId = fromWallet.id,
                            availableAmount = fromWallet.balance.pureBTC(),
                            txReceipts = listOf(
                                TxReceipt(
                                    address = address,
                                    amount = fromWallet.balance.pureBTC()
                                )
                            ),
                            privateNote = "",
                            subtractFeeFromAmount = true,
                            title = getString(R.string.nc_transaction_new)
                        )
                    },
                )
            }
        }
    }
}

@Composable
private fun TransferFundView(
    viewModel: TransferFundViewModel = hiltViewModel(),
    onContinueClicked: (fromWallet: Wallet, toWallet: Wallet, removeKey: Boolean) -> Unit = { _, _, _ -> },
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()

    TransferFundContent(
        uiState = uiState,
        onContinueClicked = onContinueClicked
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun TransferFundContent(
    uiState: TransferFundUiState = TransferFundUiState(),
    onContinueClicked: (fromWallet: Wallet, toWallet: Wallet, removeKey: Boolean) -> Unit = { _, _, _ -> },
) {
    var isRemoveUnusedKeys by rememberSaveable { mutableStateOf(false) }
    NunchukTheme {
        NcScaffold(
            modifier = Modifier.systemBarsPadding(),
            topBar = {
                NcTopAppBar(title = "")
            },
            bottomBar = {
                NcPrimaryDarkButton(
                    modifier = Modifier
                        .padding(16.dp)
                        .fillMaxWidth(),
                    onClick = {
                        onContinueClicked(
                            uiState.replacedWallet,
                            uiState.newWallet,
                            isRemoveUnusedKeys
                        )
                    }) {
                    Text(text = stringResource(R.string.nc_continue_to_transfer_funds))
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

                Text(
                    text = stringResource(
                        R.string.nc_transfer_funds_desc,
                        uiState.newWallet.name
                    ),
                    style = NunchukTheme.typography.body,
                    modifier = Modifier.padding(top = 16.dp)
                )

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
                            text = uiState.replacedWallet.getBTCAmount(),
                            style = NunchukTheme.typography.title,
                        )

                        Text(
                            text = uiState.replacedWallet.getCurrencyAmount(),
                            style = NunchukTheme.typography.bodySmall,
                        )
                    }
                }

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
                        Checkbox(
                            checked = isRemoveUnusedKeys,
                            onCheckedChange = { isRemoveUnusedKeys = it })
                    }
                }
            }
        }
    }
}

@Composable
@Preview
private fun TransferFundScreenContentPreview() {
    TransferFundContent(
        uiState = TransferFundUiState(
            newWallet = Wallet(name = "new wallet name")
        )
    )
}