package com.nunchuk.android.main

import android.app.Dialog
import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.widget.Toast
import androidx.activity.viewModels
import androidx.annotation.IdRes
import androidx.navigation.NavController
import androidx.navigation.findNavController
import androidx.navigation.ui.setupWithNavController
import com.google.android.material.bottomnavigation.BottomNavigationView
import com.nunchuk.android.arch.vm.ViewModelFactory
import com.nunchuk.android.core.base.BaseActivity
import com.nunchuk.android.core.data.model.AppUpdateResponse
import com.nunchuk.android.core.matrix.MatrixEvenBus
import com.nunchuk.android.core.matrix.MatrixEvent
import com.nunchuk.android.core.matrix.MatrixEventListener
import com.nunchuk.android.core.matrix.SessionHolder
import com.nunchuk.android.core.util.*
import com.nunchuk.android.main.databinding.ActivityMainBinding
import com.nunchuk.android.main.di.MainAppEvent
import com.nunchuk.android.main.di.MainAppEvent.DownloadFileSyncSucceed
import com.nunchuk.android.messages.components.list.RoomsViewModel
import com.nunchuk.android.notifications.PushNotificationHelper
import com.nunchuk.android.utils.NotificationUtils
import com.nunchuk.android.widget.NCInfoDialog
import java.io.File
import javax.inject.Inject

class MainActivity : BaseActivity<ActivityMainBinding>() {

    @Inject
    lateinit var factory: ViewModelFactory

    @Inject
    lateinit var pushNotificationHelper: PushNotificationHelper

    private lateinit var navController: NavController

    private val viewModel: MainActivityViewModel by viewModels { factory }

    private val roomViewModel: RoomsViewModel by viewModels { factory }

    private val syncRoomViewModel: SyncRoomViewModel by viewModels { factory }

    private val loginHalfToken
        get() = intent.getStringExtra(EXTRAS_LOGIN_HALF_TOKEN).orEmpty()

    private val deviceId
        get() = intent.getStringExtra(EXTRAS_ENCRYPTED_DEVICE_ID).orEmpty()

    private val bottomNavViewPosition: Int
        get() = intent.getIntExtra(EXTRAS_BOTTOM_NAV_VIEW_POSITION, 0)

    private val matrixEventListener: MatrixEventListener = {
        if (it is MatrixEvent.SignedInEvent) {
            roomViewModel.handleMatrixSignedIn(it.session)
        }
    }

    private val appEventListener: AppEventListener = {
        if (it is AppEvent.AppResumedEvent) {
            viewModel.checkAppUpdateRecommend(true)
        }
    }
    private var dialogUpdateRecommend: Dialog? = null

    override fun initializeBinding() = ActivityMainBinding.inflate(layoutInflater)

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        pushNotificationHelper.retrieveFcmToken(
            NotificationUtils.areNotificationsEnabled(this),
            onTokenRetrieved = ::onTokenRetrieved,
            Toast.makeText(
                this,
                "No valid Google Play Services found. Cannot use FCM.",
                Toast.LENGTH_SHORT
            )::show
        )
        setupData()
        setupNavigationView()
        setBottomNavViewPosition(bottomNavViewPosition)
        subscribeEvents()
        MatrixEvenBus.instance.subscribe(matrixEventListener)
        AppEvenBus.instance.subscribe(appEventListener)
        viewModel.checkAppUpdateRecommend(false)
    }

    override fun onDestroy() {
        MatrixEvenBus.instance.unsubscribe(matrixEventListener)
        AppEvenBus.instance.unsubscribe(appEventListener)
        super.onDestroy()
    }

    private fun onTokenRetrieved(token: String) {
        viewModel.onTokenRetrieved(token)
    }

    private fun setupData() {
        if (loginHalfToken.isNotEmpty() && deviceId.isNotEmpty()) {
            syncRoomViewModel.setupMatrix(loginHalfToken, deviceId)
        }
        if (SessionHolder.activeSession != null) {
            syncRoomViewModel.findSyncRoom()
        }
        viewModel.scheduleGetBTCConvertPrice()
    }

    private fun subscribeEvents() {
        viewModel.event.observe(this, ::handleEvent)
        syncRoomViewModel.event.observe(this, ::handleEvent)
    }

    private fun handleEvent(event: MainAppEvent) {
        when (event) {
            is DownloadFileSyncSucceed -> handleDownloadedSyncFile(event)
            is MainAppEvent.UpdateAppRecommendEvent -> handleAppUpdateEvent(event.data)
            MainAppEvent.CrossSigningUnverified -> showUnverifiedDeviceWarning()
            else -> {}
        }
    }

    private fun handleEvent(event: SyncRoomEvent) {
        when (event) {
            is SyncRoomEvent.FindSyncRoomSuccessEvent -> viewModel.syncData(event.syncRoomId)
            is SyncRoomEvent.CreateSyncRoomSucceedEvent -> viewModel.syncData(event.syncRoomId)
            is SyncRoomEvent.LoginMatrixSucceedEvent -> {
                viewModel.checkCrossSigning(event.session)
                syncRoomViewModel.findSyncRoom()
            }
            is SyncRoomEvent.FindSyncRoomFailedEvent -> if (event.syncRoomSize == 0) {
                syncRoomViewModel.createRoomWithTagSync()
            }
        }
    }

    private fun showUnverifiedDeviceWarning() {
        NCInfoDialog(this).showDialog(
            message = getString(R.string.nc_unverified_device),
            cancelable = true
        )
    }

    private fun handleDownloadedSyncFile(event: DownloadFileSyncSucceed) {
        viewModel.saveSyncFileToCache(
            data = event.responseBody,
            path = externalCacheDir.toString() + File.separator + "FileBackup",
            fileJsonInfo = event.jsonInfo
        )
    }

    private val listener = NavController.OnDestinationChangedListener { _, destination, _ ->
        binding.toolbarTitle.text = destination.label
    }

    private fun setupNavigationView() {
        val navView: BottomNavigationView = binding.navView
        navController = findNavController(R.id.nav_host_fragment_activity_main)
        navView.setupWithNavController(navController)
    }

    override fun onResume() {
        super.onResume()
        navController.addOnDestinationChangedListener(listener)
    }

    override fun onPause() {
        navController.removeOnDestinationChangedListener(listener)
        super.onPause()
        if (!NotificationUtils.areNotificationsEnabled(this)) {
            NotificationUtils.openNotificationSettings(this)
        }
    }

    private fun setBottomNavViewPosition(@IdRes id: Int) {
        if (id != 0) {
            binding.navView.selectedItemId = id
        }
    }

    private fun showUpdateRecommendedDialog(
        title: String,
        message: String,
        btnCTAText: String
    ) {
        if (dialogUpdateRecommend == null) {
            dialogUpdateRecommend = NCInfoDialog(this).init(
                title = title,
                message = message,
                btnYes = btnCTAText,
                cancelable = true
            )
        }

        if (dialogUpdateRecommend?.isShowing.orFalse()) {
            return
        }

        dialogUpdateRecommend?.show()
    }

    private fun handleAppUpdateEvent(data: AppUpdateResponse) {
        showUpdateRecommendedDialog(
            title = data.title.orEmpty(),
            message = data.message.orEmpty(),
            btnCTAText = data.btnCTA.orEmpty()
        )
    }

    companion object {
        const val EXTRAS_LOGIN_HALF_TOKEN = "EXTRAS_LOGIN_HALF_TOKEN"
        const val EXTRAS_ENCRYPTED_DEVICE_ID = "EXTRAS_ENCRYPTED_DEVICE_ID"
        const val EXTRAS_BOTTOM_NAV_VIEW_POSITION = "EXTRAS_BOTTOM_NAV_VIEW_POSITION"

        fun start(
            activityContext: Context,
            loginHalfToken: String? = null,
            deviceId: String? = null,
            position: Int? = null
        ) {
            activityContext.startActivity(
                createIntent(
                    activityContext = activityContext,
                    loginHalfToken = loginHalfToken,
                    deviceId = deviceId,
                    bottomNavViewPosition = position
                )
            )
        }

        // TODO replace with args
        fun createIntent(
            activityContext: Context,
            loginHalfToken: String? = null,
            deviceId: String? = null,
            @IdRes bottomNavViewPosition: Int? = null
        ): Intent {
            return Intent(activityContext, MainActivity::class.java).apply {
                addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP)
                putExtra(EXTRAS_LOGIN_HALF_TOKEN, loginHalfToken)
                putExtra(EXTRAS_ENCRYPTED_DEVICE_ID, deviceId)
                putExtra(EXTRAS_BOTTOM_NAV_VIEW_POSITION, bottomNavViewPosition)
            }
        }
    }

}