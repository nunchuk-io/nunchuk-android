package com.nunchuk.android.main.choosewallet

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.tooling.preview.PreviewParameter
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.nunchuk.android.compose.NcScaffold
import com.nunchuk.android.compose.NcTopAppBar
import com.nunchuk.android.compose.NunchukTheme
import com.nunchuk.android.compose.provider.WalletExtendedListProvider
import com.nunchuk.android.core.wallet.AssistedWallet
import com.nunchuk.android.core.wallet.WalletUiModel
import com.nunchuk.android.main.R
import com.nunchuk.android.model.WalletExtended
import com.nunchuk.android.model.byzantine.AssistedWalletRole

@Composable
internal fun ChooseWalletToSendRoute(
    viewModel: ChooseWalletToSendViewModel = hiltViewModel(),
    onWalletSelected: (WalletExtended) -> Unit = {},
    onClose: () -> Unit = { }
) {
    val uiState by viewModel.state.collectAsStateWithLifecycle()

    LaunchedEffect(uiState.wallets.size) {
        if (uiState.wallets.size == 1) {
            onWalletSelected(uiState.wallets.first())
        }
    }

    LaunchedEffect(uiState.hasNoWallets) {
        if (uiState.hasNoWallets) {
            onClose()
        }
    }

    if (uiState.wallets.isNotEmpty()) {
        ChooseWalletToSendContent(
            uiState = uiState,
            onWalletSelected = onWalletSelected
        )
    }
}

@Composable
internal fun ChooseWalletToSendContent(
    uiState: ChooseWalletToSendUiState,
    onWalletSelected: (WalletExtended) -> Unit
) {
    NcScaffold(
        modifier = Modifier
            .navigationBarsPadding(),
        topBar = {
            NcTopAppBar(
                title = stringResource(R.string.nc_choose_wallet_to_send),
                textStyle = NunchukTheme.typography.titleLarge,
            )
        }
    ) { paddingValues ->
        LazyColumn(
            modifier = Modifier.fillMaxSize().padding(paddingValues),
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            items(uiState.walletUiModels, key = { it.wallet.wallet.id }) { ui ->
                AssistedWallet(
                    walletsExtended = ui.wallet,
                    isAssistedWallet = ui.isAssistedWallet,
                    walletStatus = ui.walletStatus,
                    isFreeGroupWallet = ui.isGroupWallet,
                    role = ui.role.name,
                    group = ui.group,
                    onWalletClick = {
                        onWalletSelected(ui.wallet)
                    }
                )
            }
        }
    }
}

@Preview
@Composable
fun ChooseWalletToSendContentPreview(
    @PreviewParameter(WalletExtendedListProvider::class) wallets: List<WalletExtended>,
) {
    val walletUiModels = wallets.mapIndexed { idx, wallet ->
        WalletUiModel(
            wallet = wallet,
            assistedWallet = null,
            isAssistedWallet = false,
            group = null,
            role = AssistedWalletRole.NONE,
            walletStatus = "",
            isGroupWallet = false
        )
    }
    NunchukTheme {
        ChooseWalletToSendContent(
            uiState = ChooseWalletToSendUiState(
                walletUiModels = walletUiModels
            ),
            onWalletSelected = {}
        )
    }
} 