package com.nunchuk.android.nav.args

import android.content.Intent
import android.os.Bundle
import com.nunchuk.android.core.data.model.GroupWalletDataComposer
import com.nunchuk.android.core.data.model.QuickWalletParam
import com.nunchuk.android.utils.parcelable

data class AddWalletArgs(
    val decoyPin: String = "",
    val groupWalletId: String = "",
    val hasGroupSigner: Boolean = false,
    val groupWalletComposer: GroupWalletDataComposer? = null,
    val quickWalletParam: QuickWalletParam? = null,
    val isCreateMiniscriptWallet: Boolean = false,
) {

    fun buildBundle() = Bundle().apply {
        putString(DECOY_PIN, decoyPin)
        putString(GROUP_WALLET_ID, groupWalletId)
        putBoolean(HAS_GROUP_SIGNER, hasGroupSigner)
        putParcelable(GROUP_WALLET_COMPOSER, groupWalletComposer)
        putParcelable(QUICK_WALLET_PARAM, quickWalletParam)
        putBoolean(IS_CREATE_MINISCRIPT_WALLET, isCreateMiniscriptWallet)
    }

    companion object {
        private const val DECOY_PIN = "decoy_wallet"
        const val GROUP_WALLET_ID = "group_wallet_id"
        private const val HAS_GROUP_SIGNER = "has_group_signer"
        private const val GROUP_WALLET_COMPOSER = "view_only_composer"
        private const val QUICK_WALLET_PARAM = "quick_wallet_param"
        private const val IS_CREATE_MINISCRIPT_WALLET = "is_create_miniscript_wallet"

        fun deserializeFrom(intent: Intent): AddWalletArgs = AddWalletArgs(
            decoyPin = intent.extras?.getString(DECOY_PIN, "").orEmpty(),
            groupWalletId = intent.extras?.getString(GROUP_WALLET_ID, "").orEmpty(),
            hasGroupSigner = intent.extras?.getBoolean(HAS_GROUP_SIGNER, false) == true,
            groupWalletComposer = intent.parcelable<GroupWalletDataComposer>(
                GROUP_WALLET_COMPOSER
            ),
            quickWalletParam = intent.parcelable<QuickWalletParam>(QUICK_WALLET_PARAM),
            isCreateMiniscriptWallet = intent.extras?.getBoolean(
                IS_CREATE_MINISCRIPT_WALLET,
                false
            ) == true
        )
    }
}