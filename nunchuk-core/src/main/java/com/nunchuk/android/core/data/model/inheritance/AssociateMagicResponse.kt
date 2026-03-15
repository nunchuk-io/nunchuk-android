package com.nunchuk.android.core.data.model.inheritance

import com.google.gson.annotations.SerializedName
import com.nunchuk.android.core.data.model.membership.InheritanceBeneficiaryDto

data class AssociateMagicResponse(
    @SerializedName("beneficiaries")
    val beneficiaries: List<InheritanceBeneficiaryDto>? = null,
)
