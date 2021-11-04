package com.nunchuk.android.main

import androidx.lifecycle.viewModelScope
import com.nunchuk.android.arch.vm.NunchukViewModel
import com.nunchuk.android.callbacks.DownloadFileCallBack
import com.nunchuk.android.callbacks.SyncFileCallBack
import com.nunchuk.android.callbacks.UploadFileCallBack
import com.nunchuk.android.core.domain.GetPriceConvertBTCUseCase
import com.nunchuk.android.core.domain.ScheduleGetPriceConvertBTCUseCase
import com.nunchuk.android.core.matrix.*
import com.nunchuk.android.core.util.BTC_USD_EXCHANGE_RATE
import com.nunchuk.android.main.components.tabs.wallet.WalletsEvent
import com.nunchuk.android.main.components.tabs.wallet.WalletsState
import com.nunchuk.android.messages.usecase.message.AddTagRoomUseCase
import com.nunchuk.android.messages.usecase.message.CreateRoomUseCase
import com.nunchuk.android.messages.usecase.message.GetRoomSummaryListUseCase
import com.nunchuk.android.messages.util.TimelineListenerAdapter
import com.nunchuk.android.messages.util.isLocalEvent
import com.nunchuk.android.messages.util.isNunchukConsumeSyncEvent
import com.nunchuk.android.messages.util.toNunchukMatrixEvent
import com.nunchuk.android.model.NunchukMatrixEvent
import com.nunchuk.android.model.SyncFileEventHelper
import com.nunchuk.android.usecase.EnableAutoBackupUseCase
import com.nunchuk.android.usecase.GetAllRoomWalletsUseCase
import com.nunchuk.android.utils.CrashlyticsReporter
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.launch
import org.matrix.android.sdk.api.session.room.Room
import org.matrix.android.sdk.api.session.room.timeline.Timeline
import org.matrix.android.sdk.api.session.room.timeline.TimelineEvent
import org.matrix.android.sdk.api.session.room.timeline.TimelineSettings
import timber.log.Timber
import javax.inject.Inject


internal class MainActivityViewModel @Inject constructor(
    private val getAllRoomWalletsUseCase: GetAllRoomWalletsUseCase,
    private val getRoomSummaryListUseCase: GetRoomSummaryListUseCase,
    private val enableAutoBackupUseCase: EnableAutoBackupUseCase,
    private val createRoomUseCase: CreateRoomUseCase,
    private val addTagRoomUseCase: AddTagRoomUseCase,
    private val uploadFileUseCase: UploadFileUseCase,
    private val downloadFileUseCase: DownloadFileUseCase,
    private val registerDownloadBackUpFileUseCase: RegisterDownloadBackUpFileUseCase,
    private val consumeSyncFileUseCase: ConsumeSyncFileUseCase,
    private val backupFileUseCase: BackupFileUseCase,
    private val consumerSyncEventUseCase: ConsumerSyncEventUseCase,
    private val getPriceConvertBTCUseCase: GetPriceConvertBTCUseCase,
    private val scheduleGetPriceConvertBTCUseCase: ScheduleGetPriceConvertBTCUseCase,
    private val syncStateMatrixUseCase: SyncStateMatrixUseCase
) : NunchukViewModel<WalletsState, WalletsEvent>() {

    override val initialState = WalletsState()

    private var currentRoomSyncId = ""
    private lateinit var timeline: Timeline

    init {
        initSyncEventExecutor()
        //registerDownloadFileBackup()
    }

    fun restoreAndBackUp() {
        Timber.d("restoreAndBackUp")
        //checkRoomSyncExisted()
    }

    private fun registerDownloadFileBackup() {
        viewModelScope.launch {
            registerDownloadBackUpFileUseCase.execute()
                .flowOn(Dispatchers.IO)
                .catch { CrashlyticsReporter.recordException(it) }
                .flowOn(Dispatchers.Main)
                .collect {
                    Timber.d("[App] registerDownloadFileBackup")
                }
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
                uploadFile(fileName, fileJsonInfo, mineType, data)
            }
        }
        SyncFileEventHelper.syncFileExecutor = object : SyncFileCallBack {
            override fun onSync(finished: Boolean, progress: Int) {
                Timber.d("[App] progress: $progress")
            }
        }
        SyncFileEventHelper.downloadFileExecutor = object : DownloadFileCallBack {
            override fun onDownload(
                fileName: String,
                mineType: String,
                fileJsonInfo: String,
                fileUrl: String
            ) {
                Timber.d("[App] download: $fileUrl")
                //downloadFile(fileJsonInfo, fileUrl)
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
            uploadFileUseCase.execute(
                fileName = fileName,
                fileType = mineType,
                fileData = data
            ).flowOn(Dispatchers.IO)
                .catch { CrashlyticsReporter.recordException(it) }
                .flowOn(Dispatchers.Main)
                .collect { response ->
                    Timber.d("[App] fileUploadURL: ${response.contentUri}")
                    //backupFile(fileJsonInfo, response.contentUri.orEmpty())
                }
        }
    }

    private fun backupFile(fileJsonInfo: String, fileUri: String) {
        viewModelScope.launch {
            backupFileUseCase.execute(currentRoomSyncId, fileJsonInfo, fileUri)
                .flowOn(Dispatchers.IO)
                .catch { CrashlyticsReporter.recordException(it) }
                .flowOn(Dispatchers.Main)
                .collect { Timber.d("[App] backupFile success") }
        }
    }

    private fun downloadFile(fileJsonInfo: String, fileUrl: String) {
        val contentUriInfo = fileUrl.removePrefix("mxc://").split("/")

        val serverName = if (contentUriInfo.isEmpty()) "" else contentUriInfo[0]
        val mediaId = if (contentUriInfo.isEmpty()) "" else contentUriInfo[1]
        viewModelScope.launch {
            downloadFileUseCase.execute(
                serverName = serverName,
                mediaId = mediaId,
            ).flowOn(Dispatchers.IO)
                .catch { CrashlyticsReporter.recordException(it) }
                .flowOn(Dispatchers.Main)
                .collect {
                    event(WalletsEvent.Test(fileJsonInfo, it))
                }
        }
    }

    fun consumeSyncFile(fileJsonInfo: String, fileData: ByteArray) {
        viewModelScope.launch {
            consumeSyncFileUseCase.execute(fileJsonInfo, fileData)
                .flowOn(Dispatchers.IO)
                .catch { CrashlyticsReporter.recordException(it) }
                .flowOn(Dispatchers.Main)
                .collect { Timber.d("[App] consumeSyncFile") }
        }
    }

    private fun checkRoomSyncExisted() {
        viewModelScope.launch {
            getRoomSummaryListUseCase.execute()
                .flowOn(Dispatchers.IO)
                .catch { CrashlyticsReporter.recordException(it) }
                .flowOn(Dispatchers.Main)
                .collect { rooms ->
                    val syncWalletRoom =
                        rooms.firstOrNull { room -> room.hasTag(SYNC_TAG_ROOM) }
                    if (syncWalletRoom == null) {
                        Timber.d("Don't have sync room")
                        createRoomWithTagSync()
                    } else {
                        Timber.d("Have sync room: ${syncWalletRoom.roomId}")
                        enableAutoBackup(syncWalletRoom.roomId)
                        SessionHolder.activeSession?.getRoom(syncWalletRoom.roomId)?.retrieveTimelineEvents()
                        currentRoomSyncId = syncWalletRoom.roomId
                    }
                }
        }
    }

    private fun createRoomWithTagSync() {
        viewModelScope.launch {
            createRoomUseCase.execute(
                "SyncRoom340",
                listOf(SessionHolder.activeSession?.sessionParams?.userId.orEmpty())
            )
                .flowOn(Dispatchers.IO)
                .catch { CrashlyticsReporter.recordException(it) }
                .flowOn(Dispatchers.Main)
                .collect {
                    Timber.v("createRoom success ", it)
                    it.addTagRoom(SYNC_TAG_ROOM)
                }
        }

    }

    private fun Room.addTagRoom(tagName: String) {
        viewModelScope.launch {
            addTagRoomUseCase.execute(tagName, roomId).flowOn(Dispatchers.IO)
                .catch { CrashlyticsReporter.recordException(it) }
                .flowOn(Dispatchers.Main)
                .collect {
                    Timber.v("addTagRoom success ", it)
                    enableAutoBackup(roomId)
                }
        }

    }

    private fun enableAutoBackup(syncRoomId: String) {
        viewModelScope.launch {
            enableAutoBackupUseCase.execute(syncRoomId)
                .flowOn(Dispatchers.IO)
                .catch { CrashlyticsReporter.recordException(it) }
                .flowOn(Dispatchers.Main)
                .collect { Timber.v("enableAutoBackup success ", it) }
        }
    }

    private fun Room.retrieveTimelineEvents() {
        timeline = createTimeline(null, TimelineSettings(initialSize = PAGINATION, true))
        timeline.addListener(TimelineListenerAdapter(::handleTimelineEvents))
        timeline.start()
    }

    private fun handleTimelineEvents(events: List<TimelineEvent>) {
        val nunchukEvents = events.filter(TimelineEvent::isNunchukConsumeSyncEvent)
        viewModelScope.launch {
            val sortedEvents = nunchukEvents.map(TimelineEvent::toNunchukMatrixEvent)
                .filterNot(NunchukMatrixEvent::isLocalEvent)
                .sortedBy(NunchukMatrixEvent::time)
            consumerSyncEventUseCase.execute(sortedEvents).flowOn(Dispatchers.IO)
                .catch { CrashlyticsReporter.recordException(it) }
                .flowOn(Dispatchers.Main)
                .collect { Timber.v("consumerSyncEventUseCase success ", it) }
        }
    }

    private fun getBTCConvertPrice() {
        viewModelScope.launch {
            getPriceConvertBTCUseCase.execute()
                .flowOn(Dispatchers.IO)
                .catch {
                    CrashlyticsReporter.recordException(it)
                }
                .flowOn(Dispatchers.Main)
                .collect { btcResponse ->
                    btcResponse?.usd?.let {  BTC_USD_EXCHANGE_RATE = it }

                }
        }
    }

    fun scheduleGetBTCConvertPrice() {
        viewModelScope.launch {
            scheduleGetPriceConvertBTCUseCase.execute()
                .flowOn(Dispatchers.IO)
                .catch {
                    CrashlyticsReporter.recordException(it)
                }
                .flowOn(Dispatchers.Main)
                .collect {
                    getBTCConvertPrice()
                }
        }
    }

    fun syncMatrixState() {
        viewModelScope.launch {
            syncStateMatrixUseCase.execute()
                .flowOn(Dispatchers.IO)
                .catch {
                    CrashlyticsReporter.recordException(it)
                }
                .flowOn(Dispatchers.Main)
                .collect {

                }
        }
    }

    companion object {
        private const val PAGINATION = 50

        private const val SYNC_TAG_ROOM = "io.nunchuk.sync"
    }
}