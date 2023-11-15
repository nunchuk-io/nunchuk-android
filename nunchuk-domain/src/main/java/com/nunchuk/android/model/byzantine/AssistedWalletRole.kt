package com.nunchuk.android.model.byzantine

import androidx.annotation.Keep

@Keep
enum class AssistedWalletRole {
    NONE, MASTER, ADMIN, KEYHOLDER, KEYHOLDER_LIMITED, OBSERVER
}

val AssistedWalletRole.isMasterOrAdmin: Boolean
    get() = this == AssistedWalletRole.MASTER || this == AssistedWalletRole.ADMIN

val AssistedWalletRole.isKeyHolder: Boolean
    get() = this == AssistedWalletRole.MASTER || this == AssistedWalletRole.KEYHOLDER || this == AssistedWalletRole.ADMIN || this == AssistedWalletRole.KEYHOLDER_LIMITED

val AssistedWalletRole.isKeyHolderWithoutKeyHolderLimited: Boolean
    get() = this == AssistedWalletRole.MASTER || this == AssistedWalletRole.KEYHOLDER || this == AssistedWalletRole.ADMIN

val AssistedWalletRole.isKeyHolderLimited: Boolean
    get() = this == AssistedWalletRole.KEYHOLDER_LIMITED

val String?.toRole : AssistedWalletRole
    get() = when(this) {
        AssistedWalletRole.MASTER.name -> AssistedWalletRole.MASTER
        AssistedWalletRole.KEYHOLDER.name -> AssistedWalletRole.KEYHOLDER
        AssistedWalletRole.OBSERVER.name -> AssistedWalletRole.OBSERVER
        AssistedWalletRole.ADMIN.name -> AssistedWalletRole.ADMIN
        AssistedWalletRole.KEYHOLDER_LIMITED.name -> AssistedWalletRole.KEYHOLDER_LIMITED
        else -> AssistedWalletRole.NONE
    }

fun String.toTitle(defaultText: String = ""): String
    = when (this) {
        AssistedWalletRole.ADMIN.name -> "Keyholder (admin)"
        AssistedWalletRole.MASTER.name -> "Master"
        AssistedWalletRole.KEYHOLDER.name -> "Keyholder"
        AssistedWalletRole.OBSERVER.name -> "Observer"
        AssistedWalletRole.KEYHOLDER_LIMITED.name -> "Keyholder (limited)"
        else -> defaultText
    }