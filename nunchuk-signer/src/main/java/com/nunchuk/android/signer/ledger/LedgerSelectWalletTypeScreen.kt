package com.nunchuk.android.signer.ledger

import androidx.compose.ui.res.stringResource
import androidx.navigation.NavGraphBuilder
import androidx.navigation.NavHostController
import androidx.navigation.compose.composable
import androidx.navigation.toRoute
import com.nunchuk.android.signer.R
import com.nunchuk.android.signer.trezor.SelectWalletTypeScreen
import com.nunchuk.android.type.AddressType
import kotlinx.serialization.Serializable

@Serializable
internal data class LedgerSelectWalletTypeRoute(val isUsb: Boolean = false)

/**
 * Reuses the shared "Select wallet & address type" screen for Ledger. Unlike Trezor there's no
 * "Open Suite" prompt — Continue proceeds straight to connecting the device. [isUsb] is carried
 * through so the flow knows whether to go to the instruction screen (BLE) or scan directly (USB).
 */
fun NavGraphBuilder.ledgerSelectWalletType(
    onBack: () -> Unit = {},
    onContinue: (isUsb: Boolean, isSingleSig: Boolean, addressType: AddressType, accountIndex: Int) -> Unit = { _, _, _, _ -> },
) {
    composable<LedgerSelectWalletTypeRoute> { entry ->
        val route = entry.toRoute<LedgerSelectWalletTypeRoute>()
        SelectWalletTypeScreen(
            subtitle = stringResource(id = R.string.nc_select_wallet_ledger_desc),
            onBack = onBack,
            onContinue = { isSingleSig, addressType, accountIndex ->
                onContinue(route.isUsb, isSingleSig, addressType, accountIndex)
            },
        )
    }
}

fun NavHostController.navigateToLedgerSelectWalletType(isUsb: Boolean) {
    navigate(LedgerSelectWalletTypeRoute(isUsb = isUsb))
}
