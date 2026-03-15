package com.nunchuk.android.core.data.model.inheritance

import com.google.gson.annotations.SerializedName

data class AssociateMagicRequest(
    @SerializedName("wallet")
    val walletId: String? = null,
    @SerializedName("group_id")
    val groupId: String? = null,
    @SerializedName("beneficiaries")
    val beneficiaries: List<InheritanceBeneficiaryRequest>? = null,
)