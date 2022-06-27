package com.nunchuk.android.core.matrix

import kotlinx.coroutines.sync.Mutex
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class SyncStateHolder @Inject constructor() {
    val lockStateSyncRoom = Mutex()
}
