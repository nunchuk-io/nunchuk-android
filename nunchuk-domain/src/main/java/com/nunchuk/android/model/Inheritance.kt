package com.nunchuk.android.model

import android.os.Parcelable
import kotlinx.parcelize.Parcelize

@Parcelize
class Inheritance(
    val walletId: String,
    val walletLocalId: String,
    val magic: String,
    val note: String,
    val notificationEmails: List<String> = arrayListOf(),
    val status: InheritanceStatus,
    val activationTimeMilis: Long,
    val createdTimeMilis: Long,
    val lastModifiedTimeMilis: Long,
) : Parcelable