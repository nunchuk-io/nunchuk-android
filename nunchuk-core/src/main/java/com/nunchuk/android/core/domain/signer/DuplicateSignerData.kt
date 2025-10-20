package com.nunchuk.android.core.domain.signer

import android.os.Parcelable
import com.nunchuk.android.core.signer.SignerModel
import kotlinx.parcelize.Parcelize

@Parcelize
data class DuplicateSignerData(
    val signer: SignerModel,
    val keyName: String
) : Parcelable