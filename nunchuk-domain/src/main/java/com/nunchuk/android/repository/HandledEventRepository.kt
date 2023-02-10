package com.nunchuk.android.repository

interface HandledEventRepository {
    suspend fun save(eventId: String)
    suspend fun isHandled(eventId: String): Boolean
}