package com.nunchuk.android.wallet.shared.components.assign

import android.content.Context
import android.content.Intent
import com.nunchuk.android.arch.args.ActivityArgs
import com.nunchuk.android.core.util.getStringValue
import com.nunchuk.android.type.AddressType
import com.nunchuk.android.type.WalletType

data class AssignSignerArgs(
    val walletName: String,
    val walletType: WalletType,
    val addressType: AddressType,
    val totalSigns: Int,
    val requireSigns: Int
) : ActivityArgs {

    override fun buildIntent(activityContext: Context) = Intent(activityContext, AssignSignerSharedWalletActivity::class.java).apply {
        putExtra(EXTRA_WALLET_NAME, walletName)
        putExtra(EXTRA_WALLET_TYPE, walletType)
        putExtra(EXTRA_ADDRESS_TYPE, addressType)
        putExtra(EXTRA_TOTAL_SIGNS, totalSigns)
        putExtra(EXTRA_REQUIRE_SIGNS, requireSigns)
    }

    companion object {
        private const val EXTRA_WALLET_NAME = "EXTRA_WALLET_NAME"
        private const val EXTRA_WALLET_TYPE = "EXTRA_WALLET_TYPE"
        private const val EXTRA_ADDRESS_TYPE = "EXTRA_ADDRESS_TYPE"
        private const val EXTRA_TOTAL_SIGNS = "EXTRA_TOTAL_SIGNS"
        private const val EXTRA_REQUIRE_SIGNS = "EXTRA_REQUIRE_SIGNS"

        fun deserializeFrom(intent: Intent): AssignSignerArgs = AssignSignerArgs(
            intent.extras.getStringValue(EXTRA_WALLET_NAME),
            intent.getSerializableExtra(EXTRA_WALLET_TYPE) as WalletType,
            intent.getSerializableExtra(EXTRA_ADDRESS_TYPE) as AddressType,
            intent.getIntExtra(EXTRA_TOTAL_SIGNS, 0),
            intent.getIntExtra(EXTRA_REQUIRE_SIGNS, 0)
        )
    }
}
