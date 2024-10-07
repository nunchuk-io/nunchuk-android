package com.nunchuk.android.model

import android.os.Parcelable
import com.nunchuk.android.model.byzantine.GroupWalletType
import kotlinx.parcelize.Parcelize

@Parcelize
data class WalletConfig(
    val m: Int,
    val n: Int,
    val requiredServerKey: Boolean,
    val allowInheritance: Boolean,
) : Parcelable

fun WalletConfig.toGroupWalletType(): GroupWalletType? {
    return GroupWalletType.entries.find { it.m == m && it.n == n && it.requiredServerKey == requiredServerKey && it.allowInheritance == allowInheritance }
}