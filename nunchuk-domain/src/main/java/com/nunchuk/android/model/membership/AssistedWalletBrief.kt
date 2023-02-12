package com.nunchuk.android.model.membership

import com.nunchuk.android.model.MembershipPlan

data class AssistedWalletBrief(
    val localId: String,
    val plan: MembershipPlan,
    val isSetupInheritance: Boolean
)