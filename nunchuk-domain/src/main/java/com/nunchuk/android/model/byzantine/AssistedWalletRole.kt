package com.nunchuk.android.model.byzantine

import androidx.annotation.Keep

@Keep
enum class AssistedWalletRole {
    NONE, MASTER, ADMIN, KEYHOLDER, KEYHOLDER_LIMITED, FACILITATOR_ADMIN, OBSERVER
}

@Keep
enum class AssistedWalletRoleOrder {
    MASTER, ADMIN, FACILITATOR_ADMIN, KEYHOLDER, KEYHOLDER_LIMITED, OBSERVER
}

val AssistedWalletRole.isMasterOrAdmin: Boolean
    get() = this == AssistedWalletRole.MASTER || this == AssistedWalletRole.ADMIN

val AssistedWalletRole.isMasterOrAdminOrFacilitatorAdmin: Boolean
    get() = this == AssistedWalletRole.MASTER || this == AssistedWalletRole.ADMIN || this == AssistedWalletRole.FACILITATOR_ADMIN

val AssistedWalletRole.isKeyHolder: Boolean
    get() = this == AssistedWalletRole.MASTER || this == AssistedWalletRole.KEYHOLDER || this == AssistedWalletRole.ADMIN || this == AssistedWalletRole.KEYHOLDER_LIMITED

val AssistedWalletRole.isKeyHolderWithoutKeyHolderLimited: Boolean
    get() = this == AssistedWalletRole.MASTER || this == AssistedWalletRole.KEYHOLDER || this == AssistedWalletRole.ADMIN

val AssistedWalletRole.isKeyHolderLimited: Boolean
    get() = this == AssistedWalletRole.KEYHOLDER_LIMITED

val AssistedWalletRole.isObserver: Boolean
    get() = this == AssistedWalletRole.OBSERVER

val AssistedWalletRole.isFacilitatorAdmin: Boolean
    get() = this == AssistedWalletRole.FACILITATOR_ADMIN

val AssistedWalletRole.isMaster: Boolean
    get() = this == AssistedWalletRole.MASTER

val AssistedWalletRole.isAdmin: Boolean
    get() = this == AssistedWalletRole.ADMIN

val AssistedWalletRole.isNone: Boolean
    get() = this == AssistedWalletRole.NONE

val String?.toRole: AssistedWalletRole
    get() = AssistedWalletRole.entries.find { it.name == this } ?: AssistedWalletRole.NONE

fun String.toTitle(defaultText: String = ""): String
    = when (this) {
        AssistedWalletRole.ADMIN.name -> "Keyholder (admin)"
        AssistedWalletRole.MASTER.name -> "Master"
        AssistedWalletRole.KEYHOLDER.name -> "Keyholder"
        AssistedWalletRole.OBSERVER.name -> "Observer"
        AssistedWalletRole.KEYHOLDER_LIMITED.name -> "Keyholder (limited)"
        AssistedWalletRole.FACILITATOR_ADMIN.name -> "Facilitator admin"
        else -> defaultText
    }

fun AssistedWalletRole.getOrdinalInOrder(): Int = AssistedWalletRoleOrder.valueOf(this.name).ordinal