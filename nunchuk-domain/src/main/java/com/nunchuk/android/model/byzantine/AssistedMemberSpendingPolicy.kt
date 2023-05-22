package com.nunchuk.android.model.byzantine

data class AssistedMemberSpendingPolicy(
    val spendingPolicy: InputSpendingPolicy,
    val member: AssistedMember? = null,
    val isJoinGroup: Boolean = false,
)