package com.nunchuk.android.model.byzantine

import android.os.Parcelable
import kotlinx.parcelize.Parcelize

@Parcelize
data class AssistedMember(
    val role: String,
    val name: String?,
    val email: String,
    val membershipId: String = "",
) : Parcelable