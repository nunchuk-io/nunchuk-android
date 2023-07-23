package com.nunchuk.android.model.byzantine

import androidx.annotation.Keep

data class DummyTransactionPayload(
    val payload: String,
    val type: DummyTransactionType,
    val requiredSignatures: Int = 0,
)

@Keep
enum class DummyTransactionType {
    NONE, UPDATE_SERVER_KEY, CREATE_INHERITANCE, UPDATE_INHERITANCE, CANCEL_INHERITANCE
}

val String?.toDummyTransactionType: DummyTransactionType
    get() = DummyTransactionType.values().find { it.name == this } ?: DummyTransactionType.NONE