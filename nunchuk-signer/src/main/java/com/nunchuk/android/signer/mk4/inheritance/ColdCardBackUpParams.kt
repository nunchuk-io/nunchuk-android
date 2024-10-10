package com.nunchuk.android.signer.mk4.inheritance

import android.os.Parcelable
import com.nunchuk.android.type.SignerType
import kotlinx.android.parcel.Parcelize

@Parcelize
data class ColdCardBackUpParams(
    val isHasPassphrase: Boolean,
    val xfp: String,
    val keyType: SignerType
) : Parcelable