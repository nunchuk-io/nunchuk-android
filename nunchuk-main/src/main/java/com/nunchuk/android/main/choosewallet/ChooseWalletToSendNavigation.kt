package com.nunchuk.android.main.choosewallet

import androidx.navigation.NavGraphBuilder
import androidx.navigation.compose.composable
import com.nunchuk.android.model.WalletExtended
import kotlinx.serialization.Serializable

@Serializable
internal data object ChooseWalletToSend

fun NavGraphBuilder.chooseWalletToSendScreen(
    onWalletSelected: (WalletExtended) -> Unit = {},
) {
    composable<ChooseWalletToSend> {
        ChooseWalletToSendRoute(onWalletSelected = onWalletSelected)
    }
} 