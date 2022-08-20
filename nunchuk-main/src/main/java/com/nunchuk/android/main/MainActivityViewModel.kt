package com.nunchuk.android.main

import androidx.lifecycle.asFlow
import androidx.lifecycle.viewModelScope
import com.nunchuk.android.arch.vm.NunchukViewModel
import com.nunchuk.android.callbacks.DownloadFileCallBack
import com.nunchuk.android.callbacks.UploadFileCallBack
import com.nunchuk.android.core.data.model.SyncFileModel
import com.nunchuk.android.core.domain.*
import com.nunchuk.android.core.domain.data.CURRENT_DISPLAY_UNIT_TYPE
import com.nunchuk.android.core.matrix.*
import com.nunchuk.android.core.util.*
import com.nunchuk.android.main.di.MainAppEvent
import com.nunchuk.android.main.di.MainAppEvent.*
import com.nunchuk.android.messages.model.RoomNotFoundException
import com.nunchuk.android.messages.model.SessionLostException
import com.nunchuk.android.messages.util.isLocalEvent
import com.nunchuk.android.messages.util.isNunchukConsumeSyncEvent
import com.nunchuk.android.messages.util.toNunchukMatrixEvent
import com.nunchuk.android.model.ConnectionStatusExecutor
import com.nunchuk.android.model.ConnectionStatusHelper
import com.nunchuk.android.model.NunchukMatrixEvent
import com.nunchuk.android.model.SyncFileEventHelper
import com.nunchuk.android.notifications.PushNotificationManager
import com.nunchuk.android.type.ConnectionStatus
import com.nunchuk.android.usecase.BackupDataUseCase
import com.nunchuk.android.usecase.EnableAutoBackupUseCase
import com.nunchuk.android.usecase.RegisterAutoBackupUseCase
import com.nunchuk.android.utils.onException
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Dispatchers.IO
import kotlinx.coroutines.Dispatchers.Main
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import okhttp3.ResponseBody
import org.matrix.android.sdk.api.session.Session
import org.matrix.android.sdk.api.session.crypto.keysbackup.KeysBackupState
import org.matrix.android.sdk.api.session.crypto.model.CryptoDeviceInfo
import org.matrix.android.sdk.api.session.crypto.model.MXUsersDevicesMap
import org.matrix.android.sdk.api.session.room.Room
import org.matrix.android.sdk.api.session.room.timeline.Timeline
import org.matrix.android.sdk.api.session.room.timeline.TimelineEvent
import org.matrix.android.sdk.api.session.room.timeline.TimelineSettings
import org.matrix.android.sdk.api.session.sync.SyncState
import org.matrix.android.sdk.api.util.awaitCallback
import timber.log.Timber
import javax.inject.Inject

@HiltViewModel
internal class MainActivityViewModel @Inject constructor(
    private val backupDataUseCase: BackupDataUseCase,
    private val getSyncSettingUseCase: GetSyncSettingUseCase,
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
    private val saveCacheFileUseCase: SaveCacheFileUseCase,
    private val getLocalBtcPriceFlowUseCase: GetLocalBtcPriceFlowUseCase
) : NunchukViewModel<Unit, MainAppEvent>() {

    override val initialState = Unit

    private var timeline: Timeline? = null

    private var checkBootstrap: Boolean = false

    init {
        initSyncEventExecutor()
        registerDownloadFileBackupEvent()
        registerBlockChainConnectionStatusExecutor()
        getDisplayUnitSetting()
        checkMissingSyncFile()
        observeInitialSync()
        enableAutoBackup()
        listenBtcPrice()
    }

    private fun listenBtcPrice() {
        viewModelScope.launch {
            getLocalBtcPriceFlowUseCase(Unit).collect {
                if (it.isSuccess) {
                    BTC_USD_EXCHANGE_RATE = it.getOrThrow()
                }
            }
        }
    }

    private fun observeInitialSync() {
        SessionHolder.activeSession?.let {
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
                .launchIn(viewModelScope)
        }

    }

    private fun downloadKeys(session: Session) {
        viewModelScope.launch {
            awaitCallback<MXUsersDevicesMap<CryptoDeviceInfo>> {
                session.cryptoService().downloadKeys(listOf(session.myUserId), true, it)
                Timber.tag(TAG).d("download keys for user ${session.myUserId}")
            }
        }
    }

    private fun checkMissingSyncFile() {
        val userId = SessionHolder.activeSession?.sessionParams?.userId
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
                    val isForceUpdate = data.isUpdateAvailable.orFalse() && data.isUpdateRequired.orFalse() && isResume
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
                .collect { getBTCConvertPrice() }
        }
    }

    fun saveSyncFileToCache(
        data: ResponseBody,
        path: String,
        fileJsonInfo: String
    ) {
        viewModelScope.launch {
            saveCacheFileUseCase.execute(
                data = data,
                path = path
            ).flowOn(IO)
                .onException {}
                .collect {
                    consumeSyncFile(fileJsonInfo, it)
                }
        }
    }

    private fun getBTCConvertPrice() {
        viewModelScope.launch {
            getRemotePriceConvertBTCUseCase.execute()
                .flowOn(IO)
                .onException {}
                .collect()
        }
    }

    private fun registerBlockChainConnectionStatusExecutor() {
        ConnectionStatusHelper.executor = object : ConnectionStatusExecutor {
            override fun execute(connectionStatus: ConnectionStatus, percent: Int) {
                BLOCKCHAIN_STATUS = connectionStatus
                event(GetConnectionStatusSuccessEvent(connectionStatus))
            }
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
                dataLength: Int
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
                fileUrl: String
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
        mineType: String
    ) {
        viewModelScope.launch {
            createOrUpdateSyncFileUseCase.execute(
                SyncFileModel(
                    userId = SessionHolder.activeSession?.sessionParams?.userId.orEmpty(),
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
        fileUrl: String
    ) {
        viewModelScope.launch {
            createOrUpdateSyncFileUseCase.execute(
                SyncFileModel(
                    userId = SessionHolder.activeSession?.sessionParams?.userId.orEmpty(),
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
        fileUrl: String
    ) {
        viewModelScope.launch {
            deleteSyncFileUseCase.execute(
                SyncFileModel(
                    userId = SessionHolder.activeSession?.sessionParams?.userId.orEmpty(),
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
        mineType: String
    ) {
        viewModelScope.launch {
            deleteSyncFileUseCase.execute(
                SyncFileModel(
                    userId = SessionHolder.activeSession?.sessionParams?.userId.orEmpty(),
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
        data: ByteArray
    ) {
        viewModelScope.launch {
            uploadFileUseCase.execute(fileName = fileName, fileType = mineType, fileData = data)
                .flowOn(IO)
                .onException { createOrUpdateUploadSyncFile(fileName, fileJsonInfo, data, mineType) }
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
            downloadFileUseCase.execute(serverName = serverName, mediaId = mediaId)
                .flowOn(IO)
                .onException {
                    createOrUpdateDownloadSyncFile(fileJsonInfo, fileUrl)
                }
                .flowOn(Main)
                .collect {
                    deleteDownloadSyncFile(fileJsonInfo, fileUrl)
                    Timber.tag(TAG).d("[App] DownloadFileSyncSucceed: $fileJsonInfo")
                    event(DownloadFileSyncSucceed(fileJsonInfo, it))
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
            getSyncSettingUseCase.execute()
                .flatMapConcat {
                    if (it.enable) {
                        registerAutoBackupUseCase.execute(syncRoomId, accessToken).map { true }
                    } else {
                        flow {
                            Timber.tag(TAG).v("can not registerAutoBackup due to disable")
                            emit(false)
                        }
                    }
                }
                .flowOn(IO)
                .onException { }
                .collect { isRegister ->
                    if (isRegister) {
                        Timber.tag(TAG).v("registerAutoBackup success")
                    }
                }
        }
    }

    private fun enableAutoBackup() {
        viewModelScope.launch {
            getSyncSettingUseCase.execute()
                .flatMapConcat {
                    enableAutoBackupUseCase.execute(it.enable)
                }.flowOn(IO)
                .onException { }
                .collect {
                    Timber.tag(TAG).v("enableAutoBackup success")
                    if (it) {
                        backupData()
                    }
                }

        }
    }

    private fun backupData() {
        // backup missing data if needed
        viewModelScope.launch {
            backupDataUseCase.execute()
                .flowOn(Dispatchers.IO)
                .onException { }
                .collect { Timber.v("backupDataUseCase success") }
        }
    }


    private fun Room.retrieveTimelineEvents() {
        Timber.tag(TAG).v("retrieveTimelineEvents")
        timeline = timelineService().createTimeline(null, TimelineSettings(initialSize = PAGINATION, true)).apply {
            removeAllListeners()
            addListener(TimelineListenerAdapter(::handleTimelineEvents))
            start()
        }
    }

    private fun handleTimelineEvents(events: List<TimelineEvent>) {
        Timber.tag(TAG).v("handleTimelineEvents")
        checkIfReRequestKeysNeeded(events)
        val nunchukEvents = events.filter(TimelineEvent::isNunchukConsumeSyncEvent)
        viewModelScope.launch {
            val sortedEvents = nunchukEvents.map(TimelineEvent::toNunchukMatrixEvent)
                .filterNot(NunchukMatrixEvent::isLocalEvent)
                .sortedByDescending(NunchukMatrixEvent::time)
            Timber.tag(TAG).v("sortedEvents::$sortedEvents")
            getSyncSettingUseCase.execute()
                .flatMapConcat {
                    if (it.enable) {
                        consumerSyncEventUseCase.execute(sortedEvents).map { true }
                    } else {
                        flow {
                            Timber.tag(TAG).v("can not consumerSyncEvent due to disable")
                            emit(false)
                        }
                    }
                }
                .flowOn(IO)
                .onException { Timber.tag(TAG).v("consumerSyncEvent fail") }
                .collect { consume ->
                    if (consume) {
                        Timber.tag(TAG).v("consumerSyncEvent success")
                        event(ConsumeSyncEventCompleted)
                    }
                }
        }
    }

    private fun checkIfReRequestKeysNeeded(events: List<TimelineEvent>) {
        events.forEach(::reRequestKeys)
    }

    private fun reRequestKeys(timelineEvent: TimelineEvent) {
        val session = SessionHolder.activeSession ?: return
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
        Timber.tag(TAG).d("syncData::$roomId")
        registerAutoBackup(
            syncRoomId = roomId,
            accessToken = SessionHolder.activeSession?.sessionParams?.credentials?.accessToken.orEmpty()
        )
        retrieveTimelines(roomId)
    }

    private fun retrieveTimelines(roomId: String) {
        viewModelScope.launch {
            flow {
                val activeSession = SessionHolder.activeSession ?: throw SessionLostException()
                val room = activeSession.roomService().getRoom(roomId) ?: throw RoomNotFoundException(roomId)
                emit(room)
            }.flowOn(IO)
                .onException { }
                .flowOn(Main)
                .collect { it.retrieveTimelineEvents() }
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
        notificationManager.enqueueRegisterPusherWithFcmKey(token)
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