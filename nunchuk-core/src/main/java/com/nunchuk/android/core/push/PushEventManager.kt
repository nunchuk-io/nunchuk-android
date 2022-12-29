package com.nunchuk.android.core.push

import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.asSharedFlow
import javax.inject.Inject

interface PushEventManager {
    suspend fun push(event: PushEvent)

    val event: Flow<PushEvent>
}

internal class PushEventManagerImpl @Inject constructor() : PushEventManager {
    private val pushEvent = MutableSharedFlow<PushEvent>()

    override suspend fun push(event: PushEvent) {
        pushEvent.emit(event)
    }

    override val event: Flow<PushEvent> = pushEvent.asSharedFlow()
}