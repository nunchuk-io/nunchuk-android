package com.nunchuk.android.model

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
)