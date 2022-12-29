package com.nunchuk.android.model

import com.google.gson.annotations.SerializedName

class ServerKeyExtra(
    @SerializedName("name")
    val name: String,
    @SerializedName("xfp")
    val xfp: String,
    @SerializedName("derivation_path")
    val derivationPath: String,
    @SerializedName("xpub")
    val xpub: String,
)