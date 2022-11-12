package com.nunchuk.android.model

data class MembershipStepInfo(
    val id: Long = 0,
    val step: MembershipStep,
    val masterSignerId: String = "",
    val keyIdInServer: String = "",
    val isVerify: Boolean = false,
    val extraData: String = "",
    val plan: MembershipPlan,
) {
    val isVerifyOrAddKey: Boolean
        get() = isVerify || masterSignerId.isNotEmpty()
}