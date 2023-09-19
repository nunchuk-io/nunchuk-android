package com.nunchuk.android.repository

import kotlinx.coroutines.flow.Flow

interface NetworkRepository {
    fun isConnected(): Boolean
    fun networkStatusFlow() : Flow<Boolean>
}