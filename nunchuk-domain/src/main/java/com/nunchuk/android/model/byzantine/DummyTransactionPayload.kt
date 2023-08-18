package com.nunchuk.android.model.byzantine

import androidx.annotation.Keep

data class DummyTransactionPayload(
    val dummyTransactionId: String,
    val payload: String,
    val walletId: String,
    val type: DummyTransactionType,
    val requiredSignatures: Int = 0,
    val pendingSignatures: Int = 0,
    val requestByUserId: String = "",
)

@Keep
enum class DummyTransactionType {
    NONE, UPDATE_SERVER_KEY, CREATE_INHERITANCE_PLAN, UPDATE_INHERITANCE_PLAN, CANCEL_INHERITANCE_PLAN
}

fun DummyTransactionType.isInheritanceFlow() =
    this == DummyTransactionType.CREATE_INHERITANCE_PLAN || this == DummyTransactionType.UPDATE_INHERITANCE_PLAN || this == DummyTransactionType.CANCEL_INHERITANCE_PLAN

val String?.toDummyTransactionType: DummyTransactionType
    get() = DummyTransactionType.values().find { it.name == this } ?: DummyTransactionType.NONE