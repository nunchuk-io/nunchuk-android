package com.nunchuk.android.nav.args

import android.content.Context
import android.content.Intent
import android.os.Parcelable
import com.nunchuk.android.arch.args.ActivityArgs
import com.nunchuk.android.core.data.model.QuickWalletParam
import com.nunchuk.android.model.Wallet
import com.nunchuk.android.utils.parcelable
import kotlinx.parcelize.Parcelize

object BackUpWalletType {
    const val NORMAL = 0
    const val ASSISTED_CREATED = 1
}

@Parcelize
data class BackUpWalletArgs(
    val wallet: Wallet,
    val quickWalletParam: QuickWalletParam? = null,
    val isDecoyWallet: Boolean = false,
    val backUpWalletType: Int = BackUpWalletType.NORMAL,
    val isFromWalletDetails: Boolean = false,
    val isFromAlert: Boolean = false,
) : Parcelable, ActivityArgs {

    override fun buildIntent(activityContext: Context) =
        Intent().apply {
            putExtra(EXTRA_WALLET, wallet)
            putExtra(EXTRA_IS_DECOY_WALLET, isDecoyWallet)
            putExtra(EXTRA_QUICK_WALLET_PARAM, quickWalletParam)
            putExtra(EXTRA_BACKUP_WALLET_TYPE, backUpWalletType)
            putExtra(EXTRA_IS_FROM_WALLET_DETAILS, isFromWalletDetails)
            putExtra(EXTRA_IS_FROM_ALERT, isFromAlert)
        }

    companion object {
        private const val EXTRA_WALLET = "EXTRA_WALLET"
        private const val EXTRA_IS_DECOY_WALLET = "EXTRA_IS_DECOY_WALLET"
        private const val EXTRA_QUICK_WALLET_PARAM = "EXTRA_QUICK_WALLET_PARAM"
        private const val EXTRA_BACKUP_WALLET_TYPE = "EXTRA_BACKUP_WALLET_TYPE"
        private const val EXTRA_IS_FROM_WALLET_DETAILS = "EXTRA_IS_FROM_WALLET_DETAILS"
        private const val EXTRA_IS_FROM_ALERT = "EXTRA_IS_FROM_ALERT"

        fun deserializeFrom(intent: Intent): BackUpWalletArgs = BackUpWalletArgs(
            wallet = intent.parcelable<Wallet>(EXTRA_WALLET) ?: Wallet(),
            isDecoyWallet = intent.getBooleanExtra(EXTRA_IS_DECOY_WALLET, false),
            quickWalletParam = intent.parcelable<QuickWalletParam>(EXTRA_QUICK_WALLET_PARAM),
            backUpWalletType = intent.getIntExtra(
                EXTRA_BACKUP_WALLET_TYPE,
                BackUpWalletType.NORMAL
            ),
            isFromWalletDetails = intent.getBooleanExtra(EXTRA_IS_FROM_WALLET_DETAILS, false),
            isFromAlert = intent.getBooleanExtra(EXTRA_IS_FROM_ALERT, false),
        )
    }
}