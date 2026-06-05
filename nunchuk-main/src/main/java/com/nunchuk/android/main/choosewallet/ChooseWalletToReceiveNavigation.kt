package com.nunchuk.android.main.choosewallet

import androidx.compose.runtime.Composable
import androidx.compose.ui.res.stringResource
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.NavGraphBuilder
import androidx.navigation.compose.composable
import com.nunchuk.android.main.R
import com.nunchuk.android.model.WalletExtended
import kotlinx.serialization.Serializable

@Serializable
internal data object ChooseWalletToReceive

fun NavGraphBuilder.chooseWalletToReceiveScreen(
    onWalletSelected: (WalletExtended) -> Unit = {},
    onClose: () -> Unit = { }
) {
    composable<ChooseWalletToReceive> {
        ChooseWalletToReceiveRoute(onWalletSelected = onWalletSelected, onClose = onClose)
    }
}

@Composable
private fun ChooseWalletToReceiveRoute(
    onWalletSelected: (WalletExtended) -> Unit,
    onClose: () -> Unit
) {
    ChooseWalletToSendRoute(
        viewModel = hiltViewModel(),
        title = stringResource(R.string.nc_choose_wallet_to_receive),
        onWalletSelected = onWalletSelected,
        onClose = onClose
    )
}
