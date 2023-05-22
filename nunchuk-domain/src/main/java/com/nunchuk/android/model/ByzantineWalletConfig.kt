package com.nunchuk.android.model

data class ByzantineWalletConfig(
    val m: Int,
    val n: Int,
    val requiredServerKey: Boolean,
    val allowInheritance: Boolean,
)