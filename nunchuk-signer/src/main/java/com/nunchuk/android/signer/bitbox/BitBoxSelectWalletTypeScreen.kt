package com.nunchuk.android.signer.bitbox

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
internal data class BitBoxSelectWalletTypeRoute(val isUsb: Boolean = false)

/**
 * Design screen 03 — reuses the shared "Select wallet & address type" screen. It sits **before**
 * the device picker: the wallet config decides which derivation path the xpub is read at, so it
 * has to be known before the device conversation starts.
 *
 * [isUsb] rides along because the transport decides how the scan begins — list attached USB
 * devices, or ask for the Bluetooth permission first.
 */
fun NavGraphBuilder.bitBoxSelectWalletType(
    onBack: () -> Unit = {},
    onContinue: (isUsb: Boolean, isSingleSig: Boolean, addressType: AddressType, accountIndex: Int) -> Unit = { _, _, _, _ -> },
) {
    composable<BitBoxSelectWalletTypeRoute> { entry ->
        val route = entry.toRoute<BitBoxSelectWalletTypeRoute>()
        SelectWalletTypeScreen(
            subtitle = stringResource(id = R.string.nc_select_wallet_bitbox_desc),
            onBack = onBack,
            onContinue = { isSingleSig, addressType, accountIndex ->
                onContinue(route.isUsb, isSingleSig, addressType, accountIndex)
            },
        )
    }
}

fun NavHostController.navigateToBitBoxSelectWalletType(isUsb: Boolean) {
    navigate(BitBoxSelectWalletTypeRoute(isUsb = isUsb))
}
