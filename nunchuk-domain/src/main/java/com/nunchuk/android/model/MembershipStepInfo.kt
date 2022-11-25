package com.nunchuk.android.model

data class MembershipStepInfo(
    val id: Long = 0,
    val step: MembershipStep,
    val masterSignerId: String = "",
    val keyIdInServer: String = "",
    val verifyType: VerifyType = VerifyType.NONE,
    val extraData: String = "",
    val plan: MembershipPlan,
) {
    val isVerifyOrAddKey: Boolean
        get() = verifyType != VerifyType.NONE || masterSignerId.isNotEmpty()
}