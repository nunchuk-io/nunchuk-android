package com.nunchuk.android.wallet.shared.components.config

import android.content.Context
import android.content.Intent
import android.os.Bundle
import com.nunchuk.android.arch.args.ActivityArgs
import com.nunchuk.android.model.RoomWalletData

data class SharedWalletConfigArgs(
    val roomWalletData: RoomWalletData?
) : ActivityArgs {

    override fun buildIntent(activityContext: Context): Intent {
        return Intent(activityContext, SharedWalletConfigActivity::class.java).apply {
            putExtras(Bundle().apply {
                putParcelable(EXTRA_ROOM_WALLET_DATA, roomWalletData)
            })
        }
    }

    companion object {
        private const val EXTRA_ROOM_WALLET_DATA = "EXTRA_ROOM_WALLET_DATA"

        fun deserializeFrom(intent: Intent) = SharedWalletConfigArgs(
            intent.extras?.getParcelable(EXTRA_ROOM_WALLET_DATA)
        )
    }
}