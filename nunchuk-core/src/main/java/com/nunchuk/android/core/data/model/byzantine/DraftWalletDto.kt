package com.nunchuk.android.core.data.model.byzantine

import com.google.gson.annotations.SerializedName
import com.nunchuk.android.core.data.model.membership.SignerServerDto
import com.nunchuk.android.model.ByzantineWalletConfig

internal data class DraftWalletResponse(
    @SerializedName("draft_wallet") val draftWallet: DraftWalletDto? = null
)

internal data class DraftWalletDto(
    @SerializedName("finalized") val finalized: Boolean? = null,
    @SerializedName("group_id") val groupId: String? = null,
    @SerializedName("wallet_config") val walletConfig: WalletConfigDto? = null,
    @SerializedName("is_master_security_question_set") val isMasterSecurityQuestionSet: Boolean = false,
    @SerializedName("server_key_id") val serverKeyId: String? = null,
    @SerializedName("signers") val signers: ArrayList<SignerServerDto> = arrayListOf()
)

internal data class WalletConfigDto(
    @SerializedName("m") val m: Int = 0,
    @SerializedName("n") val n: Int = 0,
    @SerializedName("required_server_key") val requiredServerKey: Boolean = false,
    @SerializedName("allow_inheritance") val allowInheritance: Boolean = false,
)

internal fun WalletConfigDto?.toModel(): ByzantineWalletConfig = ByzantineWalletConfig(
    allowInheritance = this?.allowInheritance ?: false,
    m = this?.m ?: 0,
    n = this?.n ?: 0,
    requiredServerKey = this?.requiredServerKey ?: false
)