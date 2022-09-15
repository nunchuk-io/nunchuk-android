package com.nunchuk.android.main.membership.model

import android.content.Context
import com.nunchuk.android.core.signer.SignerModel
import com.nunchuk.android.main.R
import com.nunchuk.android.model.MembershipStep

data class AddKeyData(
    val type: MembershipStep,
    val signer: SignerModel? = null,
    val isVerify: Boolean = false
) {
    val isVerifyOrAddKey: Boolean
        get() = signer != null || isVerify
}

val MembershipStep.resId: Int
    get() {
        return when (this) {
            MembershipStep.ADD_TAP_SIGNER_1 -> R.drawable.ic_nfc_card
            MembershipStep.ADD_TAP_SIGNER_2 -> R.drawable.ic_nfc_card
            MembershipStep.ADD_SEVER_KEY -> R.drawable.ic_logo_dark_small
            else -> 0
        }
    }

fun MembershipStep.getLabel(context: Context): String {
    return when (this) {
        MembershipStep.ADD_TAP_SIGNER_1 -> "TAPSIGNER"
        MembershipStep.ADD_TAP_SIGNER_2 -> "TAPSIGNER #2"
        MembershipStep.ADD_SEVER_KEY -> context.getString(R.string.nc_server_key)
        else -> ""
    }
}

fun MembershipStep.getButtonText(context: Context): String {
    return when (this) {
        MembershipStep.ADD_TAP_SIGNER_1 -> context.getString(R.string.nc_add)
        MembershipStep.ADD_TAP_SIGNER_2 -> context.getString(R.string.nc_add)
        MembershipStep.ADD_SEVER_KEY -> context.getString(R.string.nc_configure)
        else -> ""
    }
}