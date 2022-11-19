package com.nunchuk.android.model

import com.google.gson.annotations.SerializedName
import com.nunchuk.android.type.SignerType

data class SignerExtra(
    @SerializedName("derivation_path")
    val derivationPath: String,
    @SerializedName("is_add_new")
    val isAddNew: Boolean,
    @SerializedName("signer_type")
    val signerType: SignerType,
)