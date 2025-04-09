package com.nunchuk.android.core.data.model

import android.os.Parcelable
import com.nunchuk.android.model.SatsCardSlot
import kotlinx.parcelize.Parcelize

@Parcelize
class QuickWalletParam(
    val claimInheritanceTxParam: ClaimInheritanceTxParam? = null,
    val slots: List<SatsCardSlot> = emptyList(),
) : Parcelable