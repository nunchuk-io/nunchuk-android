package com.nunchuk.android.main

import androidx.lifecycle.viewModelScope
import com.nunchuk.android.arch.vm.NunchukViewModel
import com.nunchuk.android.core.data.model.SyncStateMatrixResponse
import com.nunchuk.android.core.domain.LoginWithMatrixUseCase
import com.nunchuk.android.core.matrix.SessionHolder
import com.nunchuk.android.core.matrix.SyncStateHolder
import com.nunchuk.android.core.matrix.SyncStateMatrixUseCase
import com.nunchuk.android.core.profile.GetUserProfileUseCase
import com.nunchuk.android.core.util.orFalse
import com.nunchuk.android.messages.usecase.message.CreateRoomWithTagUseCase
import com.nunchuk.android.messages.util.STATE_NUNCHUK_SYNC
import com.nunchuk.android.utils.onException
import kotlinx.coroutines.Dispatchers.IO
import kotlinx.coroutines.Dispatchers.Main
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.flow.flatMapConcat
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.withLock
import timber.log.Timber
import javax.inject.Inject

internal class SyncRoomViewModel @Inject constructor(
    private val createRoomWithTagUseCase: CreateRoomWithTagUseCase,
    private val syncStateMatrixUseCase: SyncStateMatrixUseCase,
    private val getUserProfileUseCase: GetUserProfileUseCase,
    private val loginWithMatrixUseCase: LoginWithMatrixUseCase,
    private val syncStateHolder: SyncStateHolder
) : NunchukViewModel<Unit, SyncRoomEvent>() {

    override val initialState = Unit

    fun createRoomWithTagSync() {
        viewModelScope.launch {
            syncStateHolder.lockStateSyncRoom.withLock {
                createRoomWithTagUseCase.execute(
                    STATE_NUNCHUK_SYNC,
                    listOf(SessionHolder.activeSession?.sessionParams?.userId.orEmpty()),
                    STATE_NUNCHUK_SYNC
                )
                    .flowOn(IO)
                    .onException { }
                    .flowOn(Main)
                    .collect {
                        event(SyncRoomEvent.CreateSyncRoomSucceedEvent(it.roomId))
                    }
            }
        }
    }

    fun findSyncRoom() {
        viewModelScope.launch {
            syncStateHolder.lockStateSyncRoom.withLock {
                syncStateMatrixUseCase.execute()
                    .flowOn(IO)
                    .onException { }
                    .flowOn(Main)
                    .collect {
                        handleSyncStateMatrix(it)?.let { syncRoomId ->
                            Timber.d("Have sync room: $syncRoomId")
                            event(SyncRoomEvent.FindSyncRoomSuccessEvent(syncRoomId))
                        } ?: run {
                            Timber.d("Don't have sync room")
                            event(SyncRoomEvent.FindSyncRoomFailedEvent(0))
                        }
                    }
            }
        }
    }

    private fun handleSyncStateMatrix(response: SyncStateMatrixResponse): String? {
        val mapSyncRooms = response.rooms?.join?.filter {
            it.value.accountData?.events?.any { event ->
                event.type == EVENT_TYPE_TAG_ROOM && event.content?.tags?.get(EVENT_TYPE_SYNC) != null
            }.orFalse()
        }

        // in the worst case, maybe we will have more than one sync room.
        if ((mapSyncRooms?.size ?: 0) > 1) {
            return mapSyncRooms?.filter {
                it.value.timeline?.events?.any { event ->
                    event.type == EVENT_TYPE_SYNC || event.type == EVENT_TYPE_SYNC_ERROR
                }.orFalse()
            }?.map {
                it.key
            }?.firstOrNull()
        }
        return mapSyncRooms?.map { it.key }?.firstOrNull()
    }

    fun setupMatrix(token: String, encryptedDeviceId: String) {
        viewModelScope.launch {
            getUserProfileUseCase.execute()
                .flowOn(IO)
                .flatMapConcat {
                    loginWithMatrix(
                        userName = it,
                        password = token,
                        encryptedDeviceId = encryptedDeviceId
                    )
                }
                .flowOn(Main)
                .collect {
                    event(SyncRoomEvent.LoginMatrixSucceedEvent(it))
                }
        }
    }

    private fun loginWithMatrix(
        userName: String,
        password: String,
        encryptedDeviceId: String
    ) = loginWithMatrixUseCase.execute(
        userName = userName,
        password = password,
        encryptedDeviceId = encryptedDeviceId
    ).onException {}

    companion object {
        private const val EVENT_TYPE_SYNC = "io.nunchuk.sync"
        private const val EVENT_TYPE_SYNC_ERROR = "io.nunchuk.error"
        private const val EVENT_TYPE_TAG_ROOM = "m.tag"
    }
}