package com.nunchuk.android.wallet.confirm

import android.content.Context
import android.content.Intent
import com.nunchuk.android.arch.args.ActivityArgs
import com.nunchuk.android.model.SingleSigner
import com.nunchuk.android.type.AddressType
import com.nunchuk.android.type.WalletType

data class WalletConfirmArgs(
    val walletName: String,
    val walletType: WalletType,
    val addressType: AddressType,
    val totalRequireSigns: Int,
    val signers: List<SingleSigner>
) : ActivityArgs {

    override fun buildIntent(activityContext: Context) = Intent(activityContext, WalletConfirmActivity::class.java).apply {
        putExtra(EXTRA_WALLET_NAME, walletName)
        putExtra(EXTRA_WALLET_TYPE, walletType)
        putExtra(EXTRA_ADDRESS_TYPE, addressType)
        putExtra(EXTRA_TOTAL_REQUIRED_SIGNS, totalRequireSigns)
        putParcelableArrayListExtra(EXTRA_SIGNERS, signers.parcelize())
    }

    companion object {
        private const val EXTRA_WALLET_NAME = "EXTRA_WALLET_NAME"
        private const val EXTRA_WALLET_TYPE = "EXTRA_WALLET_TYPE"
        private const val EXTRA_ADDRESS_TYPE = "EXTRA_ADDRESS_TYPE"
        private const val EXTRA_TOTAL_REQUIRED_SIGNS = "EXTRA_TOTAL_REQUIRED_SIGNS"
        private const val EXTRA_SIGNERS = "EXTRA_SIGNERS"

        fun deserializeFrom(intent: Intent): WalletConfirmArgs = WalletConfirmArgs(
            intent.extras?.getString(EXTRA_WALLET_NAME, "").orEmpty(),
            intent.getSerializableExtra(EXTRA_WALLET_TYPE) as WalletType,
            intent.getSerializableExtra(EXTRA_ADDRESS_TYPE) as AddressType,
            intent.getIntExtra(EXTRA_TOTAL_REQUIRED_SIGNS, 0),
            (intent.getParcelableArrayListExtra<ParcelizeSingleSigner>(EXTRA_SIGNERS) as ArrayList<ParcelizeSingleSigner>).deparcelize()
        )
    }

}