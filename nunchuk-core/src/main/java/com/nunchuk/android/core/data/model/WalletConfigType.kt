package com.nunchuk.android.core.data.model

import androidx.annotation.Keep
import androidx.compose.runtime.Composable
import androidx.compose.ui.res.stringResource
import com.nunchuk.android.core.R

@Keep
enum class WalletConfigType {
    TOW_OF_THREE,
    THREE_OF_FIVE,
    CUSTOM,
    MINISCRIPT
}

@Composable
fun WalletConfigType.toOptionName(): String {
    return when (this) {
        WalletConfigType.TOW_OF_THREE -> stringResource(R.string.nc_wallet_config_m_of_n_multisig, 2, 3)
        WalletConfigType.THREE_OF_FIVE -> stringResource(R.string.nc_wallet_config_m_of_n_multisig, 3, 5)
        WalletConfigType.CUSTOM -> stringResource(R.string.nc_wallet_config_customize_multisig)
        WalletConfigType.MINISCRIPT -> stringResource(R.string.nc_miniscript)
    }
}

fun WalletConfigType.getMN(): Pair<Int, Int> {
    return when (this) {
        WalletConfigType.TOW_OF_THREE -> Pair(2, 3)
        WalletConfigType.THREE_OF_FIVE -> Pair(3, 5)
        WalletConfigType.CUSTOM -> Pair(0, 0)
        WalletConfigType.MINISCRIPT -> Pair(-1, -1)
    }
}

fun getWalletConfigTypeBy(n: Int, m: Int): WalletConfigType {
    if (n == 3 && m == 2) {
        return WalletConfigType.TOW_OF_THREE
    } else if (n == 5 && m == 3) {
        return WalletConfigType.THREE_OF_FIVE
    } else if (n == -1 && m == -1) {
        return WalletConfigType.MINISCRIPT
    }
    return WalletConfigType.CUSTOM
}