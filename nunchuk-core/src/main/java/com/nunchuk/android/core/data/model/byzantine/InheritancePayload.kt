package com.nunchuk.android.core.data.model.byzantine

import com.google.gson.annotations.SerializedName
import com.nunchuk.android.core.data.model.membership.PeriodResponse
import com.nunchuk.android.core.mapper.toPeriod
import com.nunchuk.android.core.util.orFalse
import com.nunchuk.android.model.Period

class InheritancePayloadDto(
    @SerializedName("group_id")
    val groupId: String? = null,
    @SerializedName("wallet_id")
    val walletId: String? = null,
    @SerializedName("wallet_local_id")
    val walletLocalId: String? = null,
    @SerializedName("old_data")
    val oldData: InheritanceDataExtendedDto? = null,
    @SerializedName("new_data")
    val newData: InheritanceDataExtendedDto? = null,
)

class InheritanceDataExtendedDto(
    @SerializedName("wallet")
    val walletId: String? = null,
    @SerializedName("note")
    val note: String? = null,
    @SerializedName("notification_emails")
    val notificationEmails: List<String>? = null,
    @SerializedName("notify_today")
    val notifyToday: Boolean? = null,
    @SerializedName("activation_time_milis")
    val activationTimeMilis: Long? = null,
    @SerializedName("group_id")
    val groupId: String? = null,
    @SerializedName("buffer_period")
    val bufferPeriod: PeriodResponse.Data? = null
)

class InheritancePayload(
    val groupId: String? = null,
    val walletId: String? = null,
    val walletLocalId: String? = null,
    val oldData: InheritanceDataExtended? = null,
    val newData: InheritanceDataExtended? = null,
)

class InheritanceDataExtended(
    val walletId: String,
    val note: String,
    val notificationEmails: List<String>,
    val notifyToday: Boolean,
    val activationTimeMilis: Long,
    val groupId: String,
    val bufferPeriod: Period?
)

fun InheritancePayloadDto.toInheritancePayload(): InheritancePayload {
    return InheritancePayload(
        groupId = groupId,
        walletId = walletId,
        walletLocalId = walletLocalId,
        oldData = oldData?.toInheritanceDataExtended(),
        newData = newData?.toInheritanceDataExtended()
    )
}

fun InheritanceDataExtendedDto.toInheritanceDataExtended(): InheritanceDataExtended {
    return InheritanceDataExtended(
        walletId = walletId.orEmpty(),
        note = note.orEmpty(),
        notificationEmails = notificationEmails.orEmpty(),
        notifyToday = notifyToday.orFalse(),
        activationTimeMilis = activationTimeMilis ?: 0,
        groupId = groupId.orEmpty(),
        bufferPeriod = bufferPeriod?.toPeriod()
    )
}