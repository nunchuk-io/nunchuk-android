package com.nunchuk.android.core.data.model.membership

import com.google.gson.annotations.SerializedName

internal data class DesktopKeyRequest(
    @SerializedName("tags")
    val tags: List<String>,
    @SerializedName("key_index")
    val keyIndex: Int? = null,
    @SerializedName("key_indices")
    val keyIndices: List<Int>? = null,
    @SerializedName("magic")
    val magic: String? = null,
    // Miniscript slot name to fill (e.g. "user_1"). Used by custom Miniscript flows
    // instead of key_index / key_indices.
    @SerializedName("key_slot")
    val keySlot: String? = null
)

internal data class RequestDesktopKeyResponse(
    @SerializedName("request")
    val request: Request? = null
)

internal data class Request(
    @SerializedName("id")
    val id: String? = null,
    @SerializedName("key")
    val key: SignerServerDto? = null,
    @SerializedName("status")
    val status: String = "",
    @SerializedName("keys")
    val keys: List<SignerServerDto>? = null,
    @SerializedName("wallet_type")
    val walletType: String? = null,
    /** Configurable on-chain draft: the slot this request fills right now (NUN-10320). */
    @SerializedName("key_index")
    val keyIndex: Int? = null,
    /** The account path to import for [keyIndex]; null = pick it from the card's `account_index_by_xfp`. */
    @SerializedName("derivation_path")
    val derivationPath: String? = null,
)