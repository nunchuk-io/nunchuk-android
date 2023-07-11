package com.nunchuk.android.model

import com.nunchuk.android.model.transaction.AlertPayload

data class Alert(
    val actions: List<AlertAction>,
    val payload: AlertPayload,
    val body: String,
    val createdTimeMillis: Long,
    val id: String,
    val status: String,
    val title: String,
    val type: String
)