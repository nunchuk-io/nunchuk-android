package com.nunchuk.android.model.byzantine

import android.os.Parcelable
import androidx.annotation.Keep
import kotlinx.parcelize.Parcelize

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
@Parcelize
enum class DummyTransactionType : Parcelable {
    NONE,
    DOWNLOAD_KEY_BACKUP,
    UPDATE_SECURITY_QUESTIONS,
    CREATE_INHERITANCE_PLAN,
    UPDATE_INHERITANCE_PLAN,
    CANCEL_INHERITANCE_PLAN,
    UPDATE_SERVER_KEY,
    HEALTH_CHECK_REQUEST,
    HEALTH_CHECK_PENDING,
    REQUEST_INHERITANCE_PLANNING,
    KEY_RECOVERY_REQUEST,
    CREATE_RECURRING_PAYMENT,
    CANCEL_RECURRING_PAYMENT
}

fun DummyTransactionType.isInheritanceFlow() =
    this == DummyTransactionType.CREATE_INHERITANCE_PLAN || this == DummyTransactionType.UPDATE_INHERITANCE_PLAN || this == DummyTransactionType.CANCEL_INHERITANCE_PLAN

val String?.toDummyTransactionType: DummyTransactionType
    get() = DummyTransactionType.values().find { it.name == this } ?: DummyTransactionType.NONE