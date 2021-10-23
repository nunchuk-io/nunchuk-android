package com.nunchuk.android.main

import androidx.lifecycle.viewModelScope
import com.nunchuk.android.arch.vm.NunchukViewModel
import com.nunchuk.android.callbacks.UploadFileCallBack
import com.nunchuk.android.main.components.tabs.wallet.WalletsEvent
import com.nunchuk.android.main.components.tabs.wallet.WalletsState
import com.nunchuk.android.messages.usecase.message.GetRoomSummaryListUseCase
import com.nunchuk.android.model.SyncFileEventHelper
import com.nunchuk.android.usecase.EnableAutoBackupUseCase
import com.nunchuk.android.usecase.GetAllRoomWalletsUseCase
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import timber.log.Timber
import javax.inject.Inject

internal class MainActivityViewModel @Inject constructor(
    private val getAllRoomWalletsUseCase: GetAllRoomWalletsUseCase,
    private val getRoomSummaryListUseCase: GetRoomSummaryListUseCase,
    private val enableAutoBackupUseCase: EnableAutoBackupUseCase
) : NunchukViewModel<WalletsState, WalletsEvent>() {

    override val initialState = WalletsState()

    init {
        initSendEventExecutor()
    }

    fun restoreAndBackUp() {
        getAllRooms()
    }

    private fun initSendEventExecutor() {
        SyncFileEventHelper.executor = object : UploadFileCallBack {
            override fun onUpload(fileName: String, mineType: String, fileJsonInfo: String, dataLength: Int) {
                Timber.d("[App] fileName: $fileName")
                Timber.d("[App] mineType: $mineType")
                Timber.d("[App] fileJsonInfo: $fileJsonInfo")
                Timber.d("[App] dataLength: $dataLength")
            }
        }
    }

    private fun getAllRooms() {
        viewModelScope.launch {
            getRoomSummaryListUseCase.execute()
                .zip(getAllRoomWalletsUseCase.execute()) { rooms, wallets -> rooms to wallets }
                .flowOn(Dispatchers.IO)
                .catch {}
                .flowOn(Dispatchers.Main)
                .collect { wallet ->
                    wallet.first.firstOrNull { room -> room.hasTag(SYNC_TAG_ROOM) }?.let {
                        enableAutoBackup(it.roomId)
                    }
                }
        }
    }

    private fun enableAutoBackup(syncRoomId: String) {
        viewModelScope.launch {
            enableAutoBackupUseCase.execute(syncRoomId)
                .flowOn(Dispatchers.IO)
                .catch { Timber.e("enableAutoBackup error ", it) }
                .flowOn(Dispatchers.Main)
                .collect { Timber.v("enableAutoBackup success ", it) }
        }
    }

    companion object {
        private const val SYNC_TAG_ROOM = "io.nunchuk.sync"
    }
}