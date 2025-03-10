package com.nunchuk.android.repository

interface NotificationRepository {
    suspend fun deviceRegister(token: String)
    suspend fun deviceUnregister(token: String)
}