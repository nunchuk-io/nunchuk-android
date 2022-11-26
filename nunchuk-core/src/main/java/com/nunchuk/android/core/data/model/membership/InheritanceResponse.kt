package com.nunchuk.android.core.data.model.membership

import com.google.gson.annotations.SerializedName

class InheritanceResponse(
    @SerializedName("inheritance")
    val inheritance: InheritanceDto? = null
)

class InheritanceDto(
    @SerializedName("wallet_id")
    val walletId: String? = null,
    @SerializedName("wallet_local_id")
    val walletLocalId: String? = null,
    @SerializedName("magic")
    val magic: String? = null,
    @SerializedName("note")
    val note: String? = null,
    @SerializedName("notification_emails")
    val notificationEmails: List<String> = emptyList(),
    @SerializedName("status")
    val status: String? = null,
    @SerializedName("activation_time_milis")
    val activationTimeMilis: Long = 0L,
    @SerializedName("created_time_milis")
    val createdTimeMilis: Long = 0L,
    @SerializedName("last_modified_time_milis")
    val lastModifiedTimeMilis: Long = 0L,
)