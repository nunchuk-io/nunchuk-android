package com.nunchuk.android.core.data.model.byzantine

import com.google.gson.annotations.SerializedName
import com.nunchuk.android.core.data.model.membership.SignerServerDto
import com.nunchuk.android.model.ByzantineWalletConfig

internal data class DraftWalletResponse(
    @SerializedName("draft_wallet") val draftWallet: DraftWalletDto? = null
)

internal data class DraftWalletDto(
    @SerializedName("finalized") var finalized: Boolean? = null,
    @SerializedName("group_id") var groupId: String? = null,
    @SerializedName("wallet_config") var walletConfig: WalletConfigDto? = null,
    @SerializedName("server_key_id") var serverKeyId: String? = null,
    @SerializedName("signers") var signers: ArrayList<SignerServerDto> = arrayListOf()
)

internal data class WalletConfigDto(
    @SerializedName("m") val m: Int = 0,
    @SerializedName("n") val n: Int = 0,
    @SerializedName("required_server_key") val requiredServerKey: Boolean = false,
    @SerializedName("allow_inheritance") val allowInheritance: Boolean = false,
)

internal fun WalletConfigDto?.toModel() : ByzantineWalletConfig = ByzantineWalletConfig(
    allowInheritance = this?.allowInheritance ?: false,
    m = this?.m ?: 0,
    n = this?.n ?: 0,
    requiredServerKey = this?.requiredServerKey ?: false
)