package com.nunchuk.android.model.byzantine

import androidx.annotation.Keep

@Keep
enum class AssistedWalletRole {
    NONE, MASTER, KEYHOLDER, OBSERVER, ADMIN
}

val AssistedWalletRole.isKeyHolder: Boolean
    get() = this == AssistedWalletRole.MASTER || this == AssistedWalletRole.KEYHOLDER || this == AssistedWalletRole.ADMIN

val String?.toRole : AssistedWalletRole
    get() = when(this) {
        AssistedWalletRole.MASTER.name -> AssistedWalletRole.MASTER
        AssistedWalletRole.KEYHOLDER.name -> AssistedWalletRole.KEYHOLDER
        AssistedWalletRole.OBSERVER.name -> AssistedWalletRole.OBSERVER
        AssistedWalletRole.ADMIN.name -> AssistedWalletRole.ADMIN
        else -> AssistedWalletRole.NONE
    }

val String.toTitle: String
    get() = when (this) {
        AssistedWalletRole.ADMIN.name -> "Keyholder (admin)"
        AssistedWalletRole.MASTER.name -> "Master"
        AssistedWalletRole.KEYHOLDER.name -> "Keyholder"
        AssistedWalletRole.OBSERVER.name -> "Observer"
        else -> ""
    }