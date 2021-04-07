package com.nunchuk.android.wallet.assign

import android.content.Context
import android.content.Intent
import com.nunchuk.android.arch.args.ActivityArgs
import com.nunchuk.android.type.AddressType
import com.nunchuk.android.type.WalletType

data class AssignSignerArgs(
    val walletName: String,
    val walletType: WalletType,
    val addressType: AddressType
) : ActivityArgs {

    override fun buildIntent(activityContext: Context) = Intent(activityContext, AssignSignerActivity::class.java).apply {
        putExtra(EXTRA_WALLET_NAME, walletName)
        putExtra(EXTRA_WALLET_TYPE, walletType)
        putExtra(EXTRA_ADDRESS_TYPE, addressType)
    }

    companion object {
        private const val EXTRA_WALLET_NAME = "EXTRA_WALLET_NAME"
        private const val EXTRA_WALLET_TYPE = "EXTRA_WALLET_TYPE"
        private const val EXTRA_ADDRESS_TYPE = "EXTRA_ADDRESS_TYPE"

        fun deserializeFrom(intent: Intent): AssignSignerArgs = AssignSignerArgs(
            intent.extras?.getString(EXTRA_WALLET_NAME, "").orEmpty(),
            intent.getSerializableExtra(EXTRA_WALLET_TYPE) as WalletType,
            intent.getSerializableExtra(EXTRA_ADDRESS_TYPE) as AddressType
        )
    }
}