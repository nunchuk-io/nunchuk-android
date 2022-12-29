package com.nunchuk.android.main.membership.model

import android.content.Context
import com.nunchuk.android.core.signer.SignerModel
import com.nunchuk.android.core.util.TAPSIGNER_INHERITANCE_NAME
import com.nunchuk.android.main.R
import com.nunchuk.android.model.MembershipStep
import com.nunchuk.android.model.VerifyType

data class AddKeyData(
    val type: MembershipStep,
    val signer: SignerModel? = null,
    val verifyType: VerifyType = VerifyType.NONE
) {
    val isVerifyOrAddKey: Boolean
        get() = signer != null || verifyType != VerifyType.NONE
}

val MembershipStep.resId: Int
    get() {
        return when (this) {
            MembershipStep.ADD_TAP_SIGNER_1 -> R.drawable.ic_nfc_card
            MembershipStep.ADD_TAP_SIGNER_2 -> R.drawable.ic_nfc_card
            MembershipStep.ADD_SEVER_KEY -> R.drawable.ic_server_key_dark
            MembershipStep.HONEY_ADD_TAP_SIGNER -> R.drawable.ic_nfc_card
            MembershipStep.HONEY_ADD_HARDWARE_KEY_1 -> R.drawable.ic_hardware_key
            MembershipStep.HONEY_ADD_HARDWARE_KEY_2 -> R.drawable.ic_hardware_key
            MembershipStep.SETUP_KEY_RECOVERY,
            MembershipStep.SETUP_INHERITANCE,
            MembershipStep.CREATE_WALLET -> throw IllegalArgumentException("Not support")
        }
    }

fun MembershipStep.getLabel(context: Context): String {
    return when (this) {
        MembershipStep.ADD_TAP_SIGNER_1 -> "TAPSIGNER"
        MembershipStep.ADD_TAP_SIGNER_2 -> "TAPSIGNER #2"
        MembershipStep.ADD_SEVER_KEY -> context.getString(R.string.nc_server_key)
        MembershipStep.HONEY_ADD_TAP_SIGNER -> TAPSIGNER_INHERITANCE_NAME
        MembershipStep.HONEY_ADD_HARDWARE_KEY_1 -> "Hardware key #2"
        MembershipStep.HONEY_ADD_HARDWARE_KEY_2 -> "Hardware key #3"
        MembershipStep.SETUP_KEY_RECOVERY,
        MembershipStep.SETUP_INHERITANCE,
        MembershipStep.CREATE_WALLET -> throw IllegalArgumentException("Not support")
    }
}

fun MembershipStep.getButtonText(context: Context): String {
    return when (this) {
        MembershipStep.ADD_SEVER_KEY -> context.getString(R.string.nc_configure)
        else -> context.getString(R.string.nc_add)
    }
}