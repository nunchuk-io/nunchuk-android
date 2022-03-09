package com.nunchuk.android.core.matrix

import kotlinx.coroutines.sync.Mutex

object SyncStateHolder {
    val lockStateCreateSyncRoom = Mutex()
}
