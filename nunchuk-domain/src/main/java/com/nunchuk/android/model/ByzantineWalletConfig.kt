package com.nunchuk.android.model

import android.os.Parcelable
import com.nunchuk.android.model.byzantine.GroupWalletType
import kotlinx.parcelize.Parcelize

@Parcelize
data class ByzantineWalletConfig(
    val m: Int,
    val n: Int,
    val requiredServerKey: Boolean,
    val allowInheritance: Boolean,
) : Parcelable

fun ByzantineWalletConfig.toGroupWalletType(): GroupWalletType? {
    return GroupWalletType.entries.find { it.m == m && it.n == n && it.requiredServerKey == requiredServerKey && it.allowInheritance == allowInheritance }
}