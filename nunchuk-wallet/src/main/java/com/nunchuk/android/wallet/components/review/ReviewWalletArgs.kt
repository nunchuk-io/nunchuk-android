package com.nunchuk.android.wallet.components.review

import android.content.Context
import android.content.Intent
import com.nunchuk.android.arch.args.ActivityArgs
import com.nunchuk.android.core.util.getStringValue
import com.nunchuk.android.model.MasterSigner
import com.nunchuk.android.model.SingleSigner
import com.nunchuk.android.type.AddressType
import com.nunchuk.android.type.WalletType

data class ReviewWalletArgs(
    val walletName: String,
    val walletType: WalletType,
    val addressType: AddressType,
    val totalRequireSigns: Int,
    val masterSigners: List<MasterSigner>,
    val remoteSigners: List<SingleSigner>
) : ActivityArgs {

    override fun buildIntent(activityContext: Context) = Intent(activityContext, ReviewWalletActivity::class.java).apply {
        putExtra(EXTRA_WALLET_NAME, walletName)
        putExtra(EXTRA_WALLET_TYPE, walletType)
        putExtra(EXTRA_ADDRESS_TYPE, addressType)
        putExtra(EXTRA_TOTAL_REQUIRED_SIGNS, totalRequireSigns)
        putParcelableArrayListExtra(EXTRA_MASTER_SIGNERS, ArrayList(masterSigners))
        putParcelableArrayListExtra(EXTRA_REMOTE_SIGNERS, ArrayList(remoteSigners))
    }

    companion object {
        private const val EXTRA_WALLET_NAME = "EXTRA_WALLET_NAME"
        private const val EXTRA_WALLET_TYPE = "EXTRA_WALLET_TYPE"
        private const val EXTRA_ADDRESS_TYPE = "EXTRA_ADDRESS_TYPE"
        private const val EXTRA_TOTAL_REQUIRED_SIGNS = "EXTRA_TOTAL_REQUIRED_SIGNS"
        private const val EXTRA_MASTER_SIGNERS = "EXTRA_MASTER_SIGNERS"
        private const val EXTRA_REMOTE_SIGNERS = "EXTRA_REMOTE_SIGNERS"

        fun deserializeFrom(intent: Intent): ReviewWalletArgs = ReviewWalletArgs(
            intent.extras.getStringValue(EXTRA_WALLET_NAME),
            intent.getSerializableExtra(EXTRA_WALLET_TYPE) as WalletType,
            intent.getSerializableExtra(EXTRA_ADDRESS_TYPE) as AddressType,
            intent.getIntExtra(EXTRA_TOTAL_REQUIRED_SIGNS, 0),
            intent.getParcelableArrayListExtra<MasterSigner>(EXTRA_MASTER_SIGNERS).orEmpty(),
            intent.getParcelableArrayListExtra<SingleSigner>(EXTRA_REMOTE_SIGNERS).orEmpty()
        )
    }

}