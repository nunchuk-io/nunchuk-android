/**************************************************************************
 * This file is part of the Nunchuk software (https://nunchuk.io/)        *
 * Copyright (C) 2022, 2023 Nunchuk                                       *
 *                                                                        *
 * This program is free software; you can redistribute it and/or          *
 * modify it under the terms of the GNU General Public License            *
 * as published by the Free Software Foundation; either version 3         *
 * of the License, or (at your option) any later version.                 *
 *                                                                        *
 * This program is distributed in the hope that it will be useful,        *
 * but WITHOUT ANY WARRANTY; without even the implied warranty of         *
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the          *
 * GNU General Public License for more details.                           *
 *                                                                        *
 * You should have received a copy of the GNU General Public License      *
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.  *
 *                                                                        *
 **************************************************************************/

package com.nunchuk.android.main

import androidx.lifecycle.asFlow
import androidx.lifecycle.viewModelScope
import com.nunchuk.android.arch.vm.NunchukViewModel
import com.nunchuk.android.callbacks.DownloadFileCallBack
import com.nunchuk.android.callbacks.UploadFileCallBack
import com.nunchuk.android.core.account.AccountManager
import com.nunchuk.android.core.data.model.SyncFileModel
import com.nunchuk.android.core.domain.CheckUpdateRecommendUseCase
import com.nunchuk.android.core.domain.CreateOrUpdateSyncFileUseCase
import com.nunchuk.android.core.domain.DeleteSyncFileUseCase
import com.nunchuk.android.core.domain.GetDisplayUnitSettingUseCase
import com.nunchuk.android.core.domain.GetLocalBtcPriceFlowUseCase
import com.nunchuk.android.core.domain.GetRemotePriceConvertBTCUseCase
import com.nunchuk.android.core.domain.GetSyncFileUseCase
import com.nunchuk.android.core.domain.GetSyncSettingUseCase
import com.nunchuk.android.core.domain.ScheduleGetPriceConvertBTCUseCase
import com.nunchuk.android.core.domain.data.CURRENT_DISPLAY_UNIT_TYPE
import com.nunchuk.android.core.matrix.BackupFileUseCase
import com.nunchuk.android.core.matrix.ConsumeSyncFileUseCase
import com.nunchuk.android.core.matrix.ConsumerSyncEventUseCase
import com.nunchuk.android.core.matrix.DownloadFileUseCase
import com.nunchuk.android.core.matrix.RegisterDownloadBackUpFileUseCase
import com.nunchuk.android.core.matrix.SessionHolder
import com.nunchuk.android.core.matrix.UploadFileUseCase
import com.nunchuk.android.core.util.AppUpdateStateHolder
import com.nunchuk.android.core.util.BTC_CURRENCY_EXCHANGE_RATE
import com.nunchuk.android.core.util.PAGINATION
import com.nunchuk.android.core.util.TimelineListenerAdapter
import com.nunchuk.android.core.util.orFalse
import com.nunchuk.android.domain.di.IoDispatcher
import com.nunchuk.android.main.di.MainAppEvent
import com.nunchuk.android.main.di.MainAppEvent.ConsumeSyncEventCompleted
import com.nunchuk.android.main.di.MainAppEvent.SyncCompleted
import com.nunchuk.android.main.di.MainAppEvent.UpdateAppRecommendEvent
import com.nunchuk.android.messages.model.RoomNotFoundException
import com.nunchuk.android.messages.model.SessionLostException
import com.nunchuk.android.messages.util.isLocalEvent
import com.nunchuk.android.messages.util.isNunchukConsumeSyncEvent
import com.nunchuk.android.messages.util.toNunchukMatrixEvent
import com.nunchuk.android.model.NunchukMatrixEvent
import com.nunchuk.android.model.SyncFileEventHelper
import com.nunchuk.android.notifications.PushNotificationManager
import com.nunchuk.android.usecase.BackupDataUseCase
import com.nunchuk.android.usecase.EnableAutoBackupUseCase
import com.nunchuk.android.usecase.RegisterAutoBackupUseCase
import com.nunchuk.android.usecase.free.groupwallet.NotificationDeviceRegisterUseCase
import com.nunchuk.android.utils.onException
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.Dispatchers.IO
import kotlinx.coroutines.Dispatchers.Main
import kotlinx.coroutines.ensureActive
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.onCompletion
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import org.matrix.android.sdk.api.NoOpMatrixCallback
import org.matrix.android.sdk.api.session.Session
import org.matrix.android.sdk.api.session.crypto.keysbackup.KeysBackupState
import org.matrix.android.sdk.api.session.room.Room
import org.matrix.android.sdk.api.session.room.timeline.Timeline
import org.matrix.android.sdk.api.session.room.timeline.TimelineEvent
import org.matrix.android.sdk.api.session.room.timeline.TimelineSettings
import org.matrix.android.sdk.api.session.sync.SyncState
import timber.log.Timber
import javax.inject.Inject

@HiltViewModel
internal class MainActivityViewModel @Inject constructor(
    private val backupDataUseCase: BackupDataUseCase,
    getSyncSettingUseCase: GetSyncSettingUseCase,
    private val enableAutoBackupUseCase: EnableAutoBackupUseCase,
    private val registerAutoBackupUseCase: RegisterAutoBackupUseCase,
    private val uploadFileUseCase: UploadFileUseCase,
    private val downloadFileUseCase: DownloadFileUseCase,
    private val registerDownloadBackUpFileUseCase: RegisterDownloadBackUpFileUseCase,
    private val consumeSyncFileUseCase: ConsumeSyncFileUseCase,
    private val backupFileUseCase: BackupFileUseCase,
    private val consumerSyncEventUseCase: ConsumerSyncEventUseCase,
    private val getRemotePriceConvertBTCUseCase: GetRemotePriceConvertBTCUseCase,
    private val scheduleGetPriceConvertBTCUseCase: ScheduleGetPriceConvertBTCUseCase,
    private val getDisplayUnitSettingUseCase: GetDisplayUnitSettingUseCase,
    private val notificationManager: PushNotificationManager,
    private val checkUpdateRecommendUseCase: CheckUpdateRecommendUseCase,
    private val getSyncFileUseCase: GetSyncFileUseCase,
    private val createOrUpdateSyncFileUseCase: CreateOrUpdateSyncFileUseCase,
    private val deleteSyncFileUseCase: DeleteSyncFileUseCase,
    private val getLocalBtcPriceFlowUseCase: GetLocalBtcPriceFlowUseCase,
    private val sessionHolder: SessionHolder,
    private val accountManager: AccountManager,
    private val notificationDeviceRegisterUseCase: NotificationDeviceRegisterUseCase,
    @IoDispatcher private val dispatcher: CoroutineDispatcher,
) : NunchukViewModel<Unit, MainAppEvent>() {

    override val initialState = Unit

    private var timeline: Timeline? = null

    private var syncRoomId: String? = null

    private var checkBootstrap: Boolean = false

    private val timelineListenerAdapter = TimelineListenerAdapter()

    private val syncEnableState = getSyncSettingUseCase(Unit)
        .map { it.getOrElse { false } }
        .stateIn(viewModelScope, SharingStarted.Lazily, false)

    init {
        if (accountManager.shouldShowOnboard() == true) {
            setEvent(MainAppEvent.ShowOnBoardEvent)
        }
        viewModelScope.launch {
            timelineListenerAdapter.data.collect(::handleTimelineEvents)
        }
        viewModelScope.launch {
            syncEnableState
                .collect { isEnableSync ->
                    if (isEnableSync && syncRoomId.isNullOrEmpty().not()) {
                        syncData(syncRoomId.orEmpty())
                    }
                    enableAutoBackup()
                }
        }

    }

    init {
        initSyncEventExecutor()
        registerDownloadFileBackupEvent()
        getDisplayUnitSetting()
        checkMissingSyncFile()
        observeInitialSync()
        listenBtcPrice()
    }

    private fun listenBtcPrice() {
        viewModelScope.launch {
            getLocalBtcPriceFlowUseCase(Unit).collect {
                if (it.isSuccess) {
                    BTC_CURRENCY_EXCHANGE_RATE = it.getOrThrow()
                }
            }
        }
    }

    private fun observeInitialSync() {
        sessionHolder.getSafeActiveSession()?.let {
            it.syncService().getSyncStateLive()
                .asFlow()
                .onEach { status ->
                    when (status) {
                        is SyncState.Idle -> {
                            if (!checkBootstrap) {
                                checkBootstrap = true
                                downloadKeys(it)
                            }
                        }

                        else -> {}
                    }
                }
                .onException { e -> Timber.e(e) }
                .launchIn(viewModelScope)
        }

    }

    private fun downloadKeys(session: Session) {
        viewModelScope.launch(dispatcher) {
            session.cryptoService()
                .downloadKeys(listOf(session.myUserId), true, NoOpMatrixCallback())
        }
    }

    private fun checkMissingSyncFile() {
        if (syncEnableState.value.not()) return
        val userId = sessionHolder.getSafeActiveSession()?.sessionParams?.userId
        if (userId.isNullOrEmpty()) {
            return
        }
        viewModelScope.launch {
            getSyncFileUseCase.execute(userId)
                .flowOn(IO)
                .onException {}
                .collect { files ->
                    files.forEach {
                        if (it.action == "UPLOAD") {
                            uploadFile(
                                fileName = it.fileName.orEmpty(),
                                fileJsonInfo = it.fileJsonInfo,
                                mineType = it.fileMineType.orEmpty(),
                                data = it.fileData ?: byteArrayOf()
                            )
                        } else {
                            downloadFile(
                                fileJsonInfo = it.fileJsonInfo,
                                fileUrl = it.fileUrl.orEmpty()
                            )
                        }
                    }
                }
        }
    }

    fun checkAppUpdateRecommend(isResume: Boolean) {
        viewModelScope.launch {
            checkUpdateRecommendUseCase.execute()
                .flowOn(IO)
                .onException {}
                .flowOn(Main)
                .collect { data ->
                    val count = AppUpdateStateHolder.countShowingRecommend.incrementAndGet()
                    val isUpdateAvailable = data.isUpdateAvailable.orFalse() && count == 1
                    val isForceUpdate =
                        data.isUpdateAvailable.orFalse() && data.isUpdateRequired.orFalse() && isResume
                    if (isUpdateAvailable || isForceUpdate) {
                        event(UpdateAppRecommendEvent(data = data))
                    }
                }
        }
    }

    fun scheduleGetBTCConvertPrice() {
        viewModelScope.launch {
            scheduleGetPriceConvertBTCUseCase.execute()
                .flowOn(IO)
                .onException {}
                .collect { getRemotePriceConvertBTCUseCase(Unit) }
        }
    }

    private fun registerDownloadFileBackupEvent() {
        viewModelScope.launch {
            registerDownloadBackUpFileUseCase.execute()
                .flowOn(IO)
                .onException {}
                .collect { Timber.tag(TAG).d("[App] registerDownloadFileBackup") }
        }
    }

    private fun initSyncEventExecutor() {
        SyncFileEventHelper.executor = object : UploadFileCallBack {
            override fun onUpload(
                fileName: String,
                mineType: String,
                fileJsonInfo: String,
                data: ByteArray,
                dataLength: Int,
            ) {
                Timber.tag(TAG).d("[App] upload: $fileName")
                uploadFile(fileName, fileJsonInfo, mineType, data)
            }
        }
        SyncFileEventHelper.downloadFileExecutor = object : DownloadFileCallBack {
            override fun onDownload(
                fileName: String,
                mineType: String,
                fileJsonInfo: String,
                fileUrl: String,
            ) {
                Timber.tag(TAG).d("[App] download: $fileUrl")
                downloadFile(fileJsonInfo, fileUrl)
            }
        }
    }

    private fun createOrUpdateUploadSyncFile(
        fileName: String,
        fileJsonInfo: String,
        data: ByteArray,
        mineType: String,
    ) {
        viewModelScope.launch {
            createOrUpdateSyncFileUseCase.execute(
                SyncFileModel(
                    userId = sessionHolder.getSafeActiveSession()?.sessionParams?.userId.orEmpty(),
                    action = "UPLOAD",
                    fileName = fileName,
                    fileJsonInfo = fileJsonInfo,
                    fileData = data,
                    fileMineType = mineType,
                )
            )
                .flowOn(IO)
                .onException { Timber.d("createOrUpdateSyncFileUseCase failed: ${it.message.orEmpty()}") }
                .collect { Timber.d("createOrUpdateSyncFileUseCase success") }
        }
    }

    private fun createOrUpdateDownloadSyncFile(
        fileJsonInfo: String,
        fileUrl: String,
    ) {
        viewModelScope.launch {
            createOrUpdateSyncFileUseCase.execute(
                SyncFileModel(
                    userId = sessionHolder.getSafeActiveSession()?.sessionParams?.userId.orEmpty(),
                    action = "DOWNLOAD",
                    fileJsonInfo = fileJsonInfo,
                    fileUrl = fileUrl
                )
            )
                .flowOn(IO)
                .onException { Timber.d("createOrUpdateDownloadSyncFile failed: ${it.message.orEmpty()}") }
                .collect { Timber.d("createOrUpdateDownloadSyncFile success") }
        }
    }

    private fun deleteDownloadSyncFile(
        fileJsonInfo: String,
        fileUrl: String,
    ) {
        viewModelScope.launch {
            deleteSyncFileUseCase.execute(
                SyncFileModel(
                    userId = sessionHolder.getSafeActiveSession()?.sessionParams?.userId.orEmpty(),
                    action = "DOWNLOAD",
                    fileJsonInfo = fileJsonInfo,
                    fileUrl = fileUrl
                )
            )
                .flowOn(IO)
                .onException { Timber.d("deleteDownloadSyncFile failed: ${it.message.orEmpty()}") }
                .collect { Timber.d("deleteDownloadSyncFile success") }
        }
    }

    private fun deleteUploadSyncFile(
        fileName: String,
        fileJsonInfo: String,
        data: ByteArray,
        mineType: String,
    ) {
        viewModelScope.launch {
            deleteSyncFileUseCase.execute(
                SyncFileModel(
                    userId = sessionHolder.getSafeActiveSession()?.sessionParams?.userId.orEmpty(),
                    action = "UPLOAD",
                    fileName = fileName,
                    fileJsonInfo = fileJsonInfo,
                    fileData = data,
                    fileMineType = mineType,
                )
            )
                .flowOn(IO)
                .onException { Timber.d("deleteUploadSyncFile failed: ${it.message.orEmpty()}") }
                .collect { Timber.d("deleteUploadSyncFile success") }
        }
    }

    private fun uploadFile(
        fileName: String,
        fileJsonInfo: String,
        mineType: String,
        data: ByteArray,
    ) {
        viewModelScope.launch {
            uploadFileUseCase.execute(fileName = fileName, fileType = mineType, fileData = data)
                .flowOn(IO)
                .onException {
                    createOrUpdateUploadSyncFile(
                        fileName,
                        fileJsonInfo,
                        data,
                        mineType
                    )
                }
                .flowOn(Main)
                .collect {
                    deleteUploadSyncFile(fileName, fileJsonInfo, data, mineType)
                    Timber.tag(TAG).d("[App] fileUploadURL: ${it.contentUri}")
                    backupFile(fileJsonInfo, it.contentUri.orEmpty())
                }
        }
    }

    private fun backupFile(fileJsonInfo: String, fileUri: String) {
        viewModelScope.launch {
            backupFileUseCase.execute(fileJsonInfo, fileUri)
                .flowOn(IO)
                .onException { }
                .collect { Timber.tag(TAG).d("[App] backupFile success") }
        }
    }

    private fun downloadFile(fileJsonInfo: String, fileUrl: String) {
        val contentUriInfo = fileUrl.removePrefix("mxc://").split("/")

        val serverName = if (contentUriInfo.isEmpty()) "" else contentUriInfo[0]
        val mediaId = if (contentUriInfo.isEmpty()) "" else contentUriInfo[1]
        viewModelScope.launch {
            val result = downloadFileUseCase(
                DownloadFileUseCase.Params(
                    serverName = serverName,
                    mediaId = mediaId
                )
            )
            if (result.isSuccess) {
                deleteDownloadSyncFile(fileJsonInfo, fileUrl)
                Timber.tag(TAG).d("[App] DownloadFileSyncSucceed: $fileJsonInfo")
                consumeSyncFile(fileJsonInfo, result.getOrThrow())
            } else {
                createOrUpdateDownloadSyncFile(fileJsonInfo, fileUrl)
            }
        }
    }

    private fun consumeSyncFile(fileJsonInfo: String, filePath: String) {
        Timber.tag(TAG).d("consumeSyncFile($fileJsonInfo, $filePath)")
        viewModelScope.launch {
            consumeSyncFileUseCase.execute(fileJsonInfo, filePath)
                .flowOn(IO)
                .onException { }
                .collect { event(SyncCompleted) }
        }
    }

    private fun registerAutoBackup(syncRoomId: String, accessToken: String) {
        viewModelScope.launch {
            registerAutoBackupUseCase.execute(syncRoomId, accessToken).collect()
        }
    }

    private fun enableAutoBackup() {
        viewModelScope.launch {
            enableAutoBackupUseCase.execute(syncEnableState.value).onException { }
                .collect()
            if (syncEnableState.value) {
                backupData()
            }
        }
    }

    private fun backupData() {
        // backup missing data if needed
        viewModelScope.launch {
            backupDataUseCase.execute()
                .flowOn(IO)
                .onException { }
                .collect { Timber.v("backupDataUseCase success") }
        }
    }


    private fun retrieveTimelineEvents(room: Room) {
        Timber.tag(TAG).v("retrieveTimelineEvents")
        timeline =
            room.timelineService()
                .createTimeline(null, TimelineSettings(initialSize = PAGINATION, true))
                .apply {
                    removeAllListeners()
                    addListener(timelineListenerAdapter)
                    start()
                }
    }

    private fun handleTimelineEvents(events: List<TimelineEvent>) {
        viewModelScope.launch(dispatcher) {
            checkIfReRequestKeysNeeded(events)
            val nunchukEvents = events.filter(TimelineEvent::isNunchukConsumeSyncEvent)
            val sortedEvents = nunchukEvents.map(TimelineEvent::toNunchukMatrixEvent)
                .filterNot(NunchukMatrixEvent::isLocalEvent)
                .sortedByDescending(NunchukMatrixEvent::time)
            consumerSyncEventUseCase.execute(sortedEvents)
                .onCompletion {
                    ensureActive()
                    if (timeline?.hasMoreToLoad(Timeline.Direction.BACKWARDS).orFalse()) {
                        timeline?.paginate(Timeline.Direction.BACKWARDS, PAGINATION)
                    } else {
                        event(ConsumeSyncEventCompleted)
                    }
                }
                .onException { Timber.e(it) }
                .collect()
        }
    }

    private fun checkIfReRequestKeysNeeded(events: List<TimelineEvent>) {
        events.forEach(::reRequestKeys)
    }

    private fun reRequestKeys(timelineEvent: TimelineEvent) {
        val session = sessionHolder.getSafeActiveSession() ?: return
        if (timelineEvent.isEncrypted() && timelineEvent.root.mCryptoError != null) {
            val cryptoService = session.cryptoService()
            val keysBackupService = cryptoService.keysBackupService()
            if (keysBackupService.getState() == KeysBackupState.NotTrusted || (keysBackupService.getState() == KeysBackupState.ReadyToBackUp && keysBackupService.canRestoreKeys())) {
                Timber.tag(TAG).d("Use backup key flow")
            }
            if (cryptoService.getCryptoDeviceInfo(session.myUserId).size > 1 || timelineEvent.senderInfo.userId != session.myUserId) {
                cryptoService.reRequestRoomKeyForEvent(timelineEvent.root)
            }
        }
    }

    fun syncData(roomId: String) {
        syncRoomId = roomId
        Timber.tag(TAG).d("syncData::$roomId")
        registerAutoBackup(
            syncRoomId = roomId,
            accessToken = sessionHolder.getSafeActiveSession()?.sessionParams?.credentials?.accessToken.orEmpty()
        )
        retrieveTimelines(roomId)
    }

    private fun retrieveTimelines(roomId: String) {
        if (syncEnableState.value.not()) return
        viewModelScope.launch {
            flow {
                val activeSession =
                    sessionHolder.getSafeActiveSession() ?: throw SessionLostException()
                val room =
                    activeSession.roomService().getRoom(roomId) ?: throw RoomNotFoundException(
                        roomId
                    )
                emit(room)
            }.flowOn(IO)
                .onException { }
                .flowOn(Main)
                .collect { retrieveTimelineEvents(it) }
        }
    }

    private fun getDisplayUnitSetting() {
        viewModelScope.launch {
            getDisplayUnitSettingUseCase.execute()
                .flowOn(IO)
                .onException { }
                .collect { CURRENT_DISPLAY_UNIT_TYPE = it.getCurrentDisplayUnitType() }
        }
    }

    fun onTokenRetrieved(token: String) {
        viewModelScope.launch {
            notificationDeviceRegisterUseCase(NotificationDeviceRegisterUseCase.Param(token))
        }
        notificationManager.enqueueRegisterPusherWithFcmKey(token)
    }


    fun getSetupData(): Pair<String, String> {
        val account = accountManager.getAccount()
        val token = account.token
        val deviceId = account.deviceId.orEmpty()
        return Pair(token, deviceId)

    }

    override fun onCleared() {
        timeline?.apply {
            dispose()
            removeAllListeners()
        }
        super.onCleared()
    }

    companion object {
        private const val TAG = "MainActivityViewModel"
    }
}