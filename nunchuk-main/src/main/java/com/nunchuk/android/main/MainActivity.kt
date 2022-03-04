package com.nunchuk.android.main

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
import com.nunchuk.android.core.matrix.SessionHolder
import com.nunchuk.android.core.util.saveToFile
import com.nunchuk.android.main.databinding.ActivityMainBinding
import com.nunchuk.android.main.di.MainAppEvent
import com.nunchuk.android.main.di.MainAppEvent.DownloadFileSyncSucceed
import com.nunchuk.android.main.di.MainAppEvent.GetConnectionStatusSuccessEvent
import com.nunchuk.android.notifications.PushNotificationHelper
import com.nunchuk.android.utils.NotificationUtils
import java.io.File
import javax.inject.Inject

class MainActivity : BaseActivity<ActivityMainBinding>() {

    @Inject
    lateinit var factory: ViewModelFactory

    @Inject
    lateinit var pushNotificationHelper: PushNotificationHelper

    private lateinit var navController: NavController

    private val viewModel: MainActivityViewModel by viewModels { factory }

    private val loginHalfToken
        get() = intent.getStringExtra(EXTRAS_LOGIN_HALF_TOKEN).orEmpty()

    private val deviceId
        get() = intent.getStringExtra(EXTRAS_ENCRYPTED_DEVICE_ID).orEmpty()

    private val bottomNavViewPosition: Int
        get() = intent.getIntExtra(EXTRAS_BOTTOM_NAV_VIEW_POSITION, 0)


    override fun initializeBinding() = ActivityMainBinding.inflate(layoutInflater)

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        pushNotificationHelper.retrieveFcmToken(
            NotificationUtils.areNotificationsEnabled(this),
            onTokenRetrieved = ::onTokenRetrieved,
            Toast.makeText(this, "No valid Google Play Services found. Cannot use FCM.", Toast.LENGTH_SHORT)::show
        )
        setupData()
        setupNavigationView()
        setBottomNavViewPosition(bottomNavViewPosition)
        subscribeEvents()
    }

    private fun onTokenRetrieved(token: String) {
        viewModel.onTokenRetrieved(token)
    }

    private fun setupData() {
        if (loginHalfToken.isNotEmpty() && deviceId.isNotEmpty()) {
            viewModel.setupMatrix(loginHalfToken, deviceId)
        }
        if (SessionHolder.activeSession != null) {
            viewModel.setupSyncing()
        }
        viewModel.scheduleGetBTCConvertPrice()
    }

    private fun subscribeEvents() {
        viewModel.event.observe(this, ::handleEvent)
    }

    private fun handleEvent(event: MainAppEvent) {
        when (event) {
            is DownloadFileSyncSucceed -> handleDownloadedSyncFile(event)
            is GetConnectionStatusSuccessEvent -> {
            }
        }
    }

    private fun handleDownloadedSyncFile(event: DownloadFileSyncSucceed) {
        event.responseBody.byteStream().saveToFile(externalCacheDir.toString() + File.separator + "FileBackup")
        val saveFile = File(externalCacheDir.toString() + File.separator + "FileBackup")
        viewModel.consumeSyncFile(event.jsonInfo, saveFile.readBytes())
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

    companion object {
        const val EXTRAS_LOGIN_HALF_TOKEN = "EXTRAS_LOGIN_HALF_TOKEN"
        const val EXTRAS_ENCRYPTED_DEVICE_ID = "EXTRAS_ENCRYPTED_DEVICE_ID"
        const val EXTRAS_BOTTOM_NAV_VIEW_POSITION = "EXTRAS_BOTTOM_NAV_VIEW_POSITION"

        fun start(activityContext: Context, loginHalfToken: String? = null, deviceId: String? = null, position: Int? = null) {
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
        fun createIntent(activityContext: Context, loginHalfToken: String? = null, deviceId: String? = null, @IdRes bottomNavViewPosition: Int? = null): Intent {
            return Intent(activityContext, MainActivity::class.java).apply {
                addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP)
                putExtra(EXTRAS_LOGIN_HALF_TOKEN, loginHalfToken)
                putExtra(EXTRAS_ENCRYPTED_DEVICE_ID, deviceId)
                putExtra(EXTRAS_BOTTOM_NAV_VIEW_POSITION, bottomNavViewPosition)
            }
        }
    }

}