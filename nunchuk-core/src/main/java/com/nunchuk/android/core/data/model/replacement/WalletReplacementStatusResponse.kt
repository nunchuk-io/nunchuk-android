package com.nunchuk.android.core.data.model.replacement

import com.google.gson.annotations.SerializedName
import com.nunchuk.android.core.data.model.membership.DraftInheritanceCardDto
import com.nunchuk.android.core.data.model.membership.InheritanceValidationErrorDto
import com.nunchuk.android.core.data.model.membership.SignerServerDto
import com.nunchuk.android.core.data.model.membership.TimelockDto
import com.nunchuk.android.core.data.model.membership.toModel
import com.nunchuk.android.model.signer.SignerServer

internal class WalletReplacementStatusResponse(
    @SerializedName("status")
    var status: StatusDto? = null
)

internal data class StatusDto(
    @SerializedName("pending_replace_xfps") val pendingReplaceXfps: List<String>,
    @SerializedName("signers") val signers: List<ReplaceSignerDto>,
    @SerializedName("timelock") val timelock: TimelockDto? = null,
    // Configurable on-chain wallets only (NUN-10320); cards carry no `account_index_by_xfp` here.
    @SerializedName("cards") val cards: List<DraftInheritanceCardDto>? = null,
    @SerializedName("validation_errors") val validationErrors: List<InheritanceValidationErrorDto>? = null,
    @SerializedName("fallback_timelocks") val fallbackTimelocks: List<TimelockDto>? = null,
)

internal data class ReplaceSignerDto(
    @SerializedName("xfp") val xfp: String,
    @SerializedName("replace_by") val replaceBy: SignerServerDto,
    @SerializedName("replacements") val replacements: List<SignerServerDto> = emptyList()
)

internal fun ReplaceSignerDto.replacementsToModel(): List<SignerServer> {
    return replacements.map { it.toModel() }
}
