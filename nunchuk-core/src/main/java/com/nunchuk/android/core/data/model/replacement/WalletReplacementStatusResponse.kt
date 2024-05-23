package com.nunchuk.android.core.data.model.replacement

import com.google.gson.annotations.SerializedName
import com.nunchuk.android.core.data.model.membership.SignerServerDto

internal class WalletReplacementStatusResponse(
    @SerializedName("status")
    var status: StatusDto? = null
)

internal data class StatusDto(
    @SerializedName("pending_replace_xfps") val pendingReplaceXfps: List<String>,
    @SerializedName("signers") val signers: List<ReplaceSignerDto>
)

internal data class ReplaceSignerDto(
    @SerializedName("xfp") val xfp: String,
    @SerializedName("replace_by") val replaceBy: SignerServerDto
)