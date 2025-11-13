package com.nunchuk.android.nav.args

import android.content.Intent
import android.os.Bundle
import com.nunchuk.android.core.data.model.QuickWalletParam
import com.nunchuk.android.core.util.getStringValue
import com.nunchuk.android.utils.parcelable

data class UploadConfigurationArgs(
    val walletId: String,
    val isOnChainFlow: Boolean = false,
    val groupId: String? = null,
    val replacedWalletId: String? = null,
    val quickWalletParam: QuickWalletParam? = null,
    val type: UploadConfigurationType = UploadConfigurationType.None,
) {

    fun buildBundle() = Bundle().apply {
        putString(EXTRA_WALLET_ID, walletId)
        putBoolean(EXTRA_IS_ON_CHAIN_FLOW, isOnChainFlow)
        putString(EXTRA_GROUP_ID, groupId)
        putString(EXTRA_REPLACED_WALLET_ID, replacedWalletId)
        putParcelable(EXTRA_QUICK_WALLET_PARAM, quickWalletParam)
        putSerializable(EXTRA_TYPE, type)
    }

    companion object {
        private const val EXTRA_WALLET_ID = "EXTRA_WALLET_ID"
        private const val EXTRA_IS_ON_CHAIN_FLOW = "EXTRA_IS_ON_CHAIN_FLOW"
        private const val EXTRA_GROUP_ID = "EXTRA_GROUP_ID"
        private const val EXTRA_REPLACED_WALLET_ID = "EXTRA_REPLACED_WALLET_ID"
        private const val EXTRA_QUICK_WALLET_PARAM = "EXTRA_QUICK_WALLET_PARAM"
        private const val EXTRA_TYPE = "EXTRA_TYPE"

        fun deserializeFrom(intent: Intent): UploadConfigurationArgs = UploadConfigurationArgs(
            walletId = intent.extras?.getStringValue(EXTRA_WALLET_ID).orEmpty(),
            isOnChainFlow = intent.extras?.getBoolean(EXTRA_IS_ON_CHAIN_FLOW, false) ?: false,
            groupId = intent.extras?.getString(EXTRA_GROUP_ID),
            replacedWalletId = intent.extras?.getString(EXTRA_REPLACED_WALLET_ID),
            quickWalletParam = intent.parcelable(EXTRA_QUICK_WALLET_PARAM),
            type = intent.extras?.getSerializable(EXTRA_TYPE) as? UploadConfigurationType ?: UploadConfigurationType.None
        )
    }
}

enum class UploadConfigurationType {
    RegisterOnly,
    None,
}