package com.nunchuk.android.main

import androidx.lifecycle.viewModelScope
import com.nunchuk.android.arch.vm.NunchukViewModel
import com.nunchuk.android.callbacks.DownloadFileCallBack
import com.nunchuk.android.callbacks.UploadFileCallBack
import com.nunchuk.android.core.data.model.AppUpdateResponse
import com.nunchuk.android.core.data.model.SyncStateMatrixResponse
import com.nunchuk.android.core.domain.*
import com.nunchuk.android.core.entities.CURRENT_DISPLAY_UNIT_TYPE
import com.nunchuk.android.core.matrix.*
import com.nunchuk.android.core.profile.GetUserProfileUseCase
import com.nunchuk.android.core.retry.DEFAULT_RETRY_POLICY
import com.nunchuk.android.core.retry.RetryPolicy
import com.nunchuk.android.core.retry.SYNC_RETRY_POLICY
import com.nunchuk.android.core.retry.retryDefault
import com.nunchuk.android.core.util.*
import com.nunchuk.android.main.di.MainAppEvent
import com.nunchuk.android.main.di.MainAppEvent.*
import com.nunchuk.android.messages.model.RoomNotFoundException
import com.nunchuk.android.messages.model.SessionLostException
import com.nunchuk.android.messages.usecase.message.CreateRoomWithTagUseCase
import com.nunchuk.android.messages.util.STATE_NUNCHUK_SYNC
import com.nunchuk.android.messages.util.isLocalEvent
import com.nunchuk.android.messages.util.isNunchukConsumeSyncEvent
import com.nunchuk.android.messages.util.toNunchukMatrixEvent
import com.nunchuk.android.model.ConnectionStatusExecutor
import com.nunchuk.android.model.ConnectionStatusHelper
import com.nunchuk.android.model.NunchukMatrixEvent
import com.nunchuk.android.model.SyncFileEventHelper
import com.nunchuk.android.notifications.PushNotificationManager
import com.nunchuk.android.type.ConnectionStatus
import com.nunchuk.android.usecase.EnableAutoBackupUseCase
import com.nunchuk.android.utils.CrashlyticsReporter
import com.nunchuk.android.utils.onException
import kotlinx.coroutines.Dispatchers.IO
import kotlinx.coroutines.Dispatchers.Main
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.withLock
import org.matrix.android.sdk.api.session.room.Room
import org.matrix.android.sdk.api.session.room.timeline.Timeline
import org.matrix.android.sdk.api.session.room.timeline.TimelineEvent
import org.matrix.android.sdk.api.session.room.timeline.TimelineSettings
import timber.log.Timber
import javax.inject.Inject
import javax.inject.Named

internal class MainActivityViewModel @Inject constructor(
    private val enableAutoBackupUseCase: EnableAutoBackupUseCase,
    private val createRoomWithTagUseCase: CreateRoomWithTagUseCase,
    private val uploadFileUseCase: UploadFileUseCase,
    private val downloadFileUseCase: DownloadFileUseCase,
    private val registerDownloadBackUpFileUseCase: RegisterDownloadBackUpFileUseCase,
    private val consumeSyncFileUseCase: ConsumeSyncFileUseCase,
    private val backupFileUseCase: BackupFileUseCase,
    private val consumerSyncEventUseCase: ConsumerSyncEventUseCase,
    private val syncStateMatrixUseCase: SyncStateMatrixUseCase,
    private val getPriceConvertBTCUseCase: GetPriceConvertBTCUseCase,
    private val scheduleGetPriceConvertBTCUseCase: ScheduleGetPriceConvertBTCUseCase,
    private val getUserProfileUseCase: GetUserProfileUseCase,
    private val loginWithMatrixUseCase: LoginWithMatrixUseCase,
    private val getDisplayUnitSettingUseCase: GetDisplayUnitSettingUseCase,
    private val notificationManager: PushNotificationManager,
    @Named(DEFAULT_RETRY_POLICY) private val retryPolicy: RetryPolicy,
    @Named(SYNC_RETRY_POLICY) private val syncRetryPolicy: RetryPolicy,
    private val checkUpdateRecommendUseCase: CheckUpdateRecommendUseCase
    ) : NunchukViewModel<Unit, MainAppEvent>() {

    override val initialState = Unit

    private var syncRoomId: String? = null

    private lateinit var timeline: Timeline

    init {
        initSyncEventExecutor()
        registerDownloadFileBackupEvent()
        registerBlockChainConnectionStatusExecutor()
        getDisplayUnitSetting()
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
                        event(
                            UpdateAppRecommendEvent(
                                data = data
                            )
                        )
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

    private fun getBTCConvertPrice() {
        viewModelScope.launch {
            getPriceConvertBTCUseCase.execute()
                .flowOn(IO)
                .onException {}
                .collect { btcResponse -> btcResponse?.usd?.let { BTC_USD_EXCHANGE_RATE = it } }
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
                .flowOn(Main)
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

    private fun uploadFile(
        fileName: String,
        fileJsonInfo: String,
        mineType: String,
        data: ByteArray
    ) {
        viewModelScope.launch {
            uploadFileUseCase.execute(fileName = fileName, fileType = mineType, fileData = data)
                .retryDefault(retryPolicy)
                .flowOn(IO)
                .onException {}
                .flowOn(Main)
                .collect {
                    Timber.tag(TAG).d("[App] fileUploadURL: ${it.contentUri}")
                    backupFile(fileJsonInfo, it.contentUri.orEmpty())
                }
        }
    }

    private fun backupFile(fileJsonInfo: String, fileUri: String) {
        if (syncRoomId == null) {
            CrashlyticsReporter.recordException(Throwable("Sync room null. Can't backup file"))
            return
        }

        syncRoomId?.let {
            viewModelScope.launch {
                backupFileUseCase.execute(it, fileJsonInfo, fileUri)
                    .retryDefault(retryPolicy)
                    .flowOn(IO)
                    .onException { }
                    .flowOn(Main)
                    .collect { Timber.tag(TAG).d("[App] backupFile success") }
            }
        }
    }

    private fun downloadFile(fileJsonInfo: String, fileUrl: String) {
        val contentUriInfo = fileUrl.removePrefix("mxc://").split("/")

        val serverName = if (contentUriInfo.isEmpty()) "" else contentUriInfo[0]
        val mediaId = if (contentUriInfo.isEmpty()) "" else contentUriInfo[1]
        viewModelScope.launch {
            downloadFileUseCase.execute(serverName = serverName, mediaId = mediaId)
                .retryDefault(retryPolicy)
                .flowOn(IO)
                .onException { }
                .flowOn(Main)
                .collect {
                    Timber.tag(TAG).d("[App] DownloadFileSyncSucceed: $fileJsonInfo")
                    event(DownloadFileSyncSucceed(fileJsonInfo, it))
                }
        }
    }

    fun consumeSyncFile(fileJsonInfo: String, fileData: ByteArray) {
        Timber.tag(TAG).d("consumeSyncFile($fileJsonInfo, $fileData)")
        viewModelScope.launch {
            consumeSyncFileUseCase.execute(fileJsonInfo, fileData)
                .retryDefault(retryPolicy)
                .flowOn(IO)
                .onException { }
                .flowOn(Main)
                .collect {
                    event(SyncCompleted)
                    Timber.tag(TAG).d("[App] consumeSyncFile")
                }
        }
    }

    private fun createRoomWithTagSync() {
        viewModelScope.launch {
            createRoomWithTagUseCase.execute(
                STATE_NUNCHUK_SYNC,
                listOf(SessionHolder.activeSession?.sessionParams?.userId.orEmpty()),
                STATE_NUNCHUK_SYNC
            )
                .retryDefault(retryPolicy)
                .flowOn(IO)
                .onException { }
                .flowOn(Main)
                .collect {
                    syncRoomId = it.roomId
                    syncRoomId?.let(::syncData)
                }
        }
    }

    private fun enableAutoBackup(syncRoomId: String) {
        viewModelScope.launch {
            enableAutoBackupUseCase.execute(syncRoomId)
                .retryDefault(retryPolicy)
                .flowOn(IO)
                .onException { }
                .flowOn(Main)
                .collect { Timber.tag(TAG).v("enableAutoBackup success") }
        }
    }

    private fun Room.retrieveTimelineEvents() {
        Timber.tag(TAG).v("retrieveTimelineEvents")
        timeline = createTimeline(null, TimelineSettings(initialSize = PAGINATION, true))
        timeline.removeAllListeners()
        timeline.addListener(TimelineListenerAdapter(::handleTimelineEvents))
        timeline.start()
    }

    private fun handleTimelineEvents(events: List<TimelineEvent>) {
        Timber.tag(TAG).v("handleTimelineEvents")
        val nunchukEvents = events.filter(TimelineEvent::isNunchukConsumeSyncEvent)
        viewModelScope.launch {
            val sortedEvents = nunchukEvents.map(TimelineEvent::toNunchukMatrixEvent)
                .filterNot(NunchukMatrixEvent::isLocalEvent)
                .sortedByDescending(NunchukMatrixEvent::time)
            Timber.tag(TAG).v("sortedEvents::$sortedEvents")
            consumerSyncEventUseCase.execute(sortedEvents)
                .retryDefault(retryPolicy)
                .flowOn(IO)
                .onException { }
                .flowOn(Main)
                .collect {
                    Timber.tag(TAG).v("consumerSyncEventUseCase success")
                }
        }
    }

    fun setupSyncing() {
        Timber.tag(TAG).d("Bearer ${SessionHolder.activeSession?.sessionParams?.credentials?.accessToken.orEmpty()}")
        viewModelScope.launch {
            SyncStateHolder.lockStateCreateSyncRoom.withLock {
                syncStateMatrixUseCase.execute()
                    .retryDefault(retryPolicy)
                    .flowOn(IO)
                    .onException { }
                    .flowOn(Main)
                    .collect {
                            event(SyncCompleted)
                            syncRoomId = findSyncRoom(it)
                            syncRoomId?.let { syncRoomId ->
                                Timber.tag(TAG).d("Have sync room: $syncRoomId")
                                syncData(roomId = syncRoomId)
                            } ?: run {
                                Timber.tag(TAG).d("Don't have sync room")
                                createRoomWithTagSync()
                            }
                        }
            }
        }
    }

    private fun findSyncRoom(response: SyncStateMatrixResponse): String? {
        return response.rooms?.join?.filter {
            it.value.accountData?.events?.any { event ->
                event.type == EVENT_TYPE_TAG_ROOM && event.content?.tags?.get(EVENT_TYPE_SYNC) != null
            }.orFalse()
        }?.map {
            it.key
        }?.firstOrNull()
    }

    private fun syncData(roomId: String) {
        Timber.tag(TAG).d("syncData::$roomId")
        enableAutoBackup(roomId)
        retrieveTimelines(roomId)
    }

    private fun retrieveTimelines(roomId: String) {
        viewModelScope.launch {
            flow {
                val activeSession = SessionHolder.activeSession ?: throw SessionLostException()
                val room = activeSession.getRoom(roomId) ?: throw RoomNotFoundException(roomId)
                emit(room)
            }.retryDefault(syncRetryPolicy)
                .flowOn(IO)
                .onException { }
                .flowOn(Main)
                .collect {
                    it.retrieveTimelineEvents()
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
    ).retryDefault(retryPolicy)
        .onException {}

    fun setupMatrix(token: String, encryptedDeviceId: String) {
        getUserProfileUseCase.execute()
            .flowOn(IO)
            .retryDefault(retryPolicy)
            .flatMapConcat {
                loginWithMatrix(
                    userName = it,
                    password = token,
                    encryptedDeviceId = encryptedDeviceId
                )
            }
            .onEach {
                setupSyncing()
            }
            .flowOn(Main)
            .launchIn(viewModelScope)
    }

    private fun getDisplayUnitSetting() {
        viewModelScope.launch {
            getDisplayUnitSettingUseCase.execute()
                .flowOn(IO)
                .onException { }
                .flowOn(Main)
                .collect {
                    CURRENT_DISPLAY_UNIT_TYPE = it.getCurrentDisplayUnitType()
                }
        }
    }

    fun onTokenRetrieved(token: String) {
        notificationManager.enqueueRegisterPusherWithFcmKey(token)
    }

    companion object {
        private const val TAG = "MainActivityViewModel"
        private const val EVENT_TYPE_SYNC = "io.nunchuk.sync"
        private const val EVENT_TYPE_TAG_ROOM = "m.tag"
    }
}