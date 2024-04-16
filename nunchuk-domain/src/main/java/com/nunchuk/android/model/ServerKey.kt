package com.nunchuk.android.model

import android.os.Parcelable
import kotlinx.parcelize.Parcelize

@Parcelize
data class ServerKey(
    val name: String,
    val xfp: String,
    val derivationPath: String,
    val xpub: String,
    val pubkey: String,
    val id: String,
    val type: String,
    val tapsigner: TapSigner,
    val tags: List<String> = emptyList(),
    val index: Int = 0,
): Parcelable