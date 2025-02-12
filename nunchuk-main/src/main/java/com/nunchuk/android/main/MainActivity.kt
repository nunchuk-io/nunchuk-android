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

import android.app.Dialog
import android.content.Context
import android.content.Intent
import android.os.Bundle
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import androidx.annotation.IdRes
import androidx.core.view.WindowCompat
import androidx.lifecycle.lifecycleScope
import androidx.navigation.NavController
import androidx.navigation.fragment.NavHostFragment
import com.google.android.material.badge.BadgeDrawable
import com.google.android.material.bottomnavigation.BottomNavigationView
import com.nunchuk.android.contact.components.contacts.ContactsViewModel
import com.nunchuk.android.core.data.model.AppUpdateResponse
import com.nunchuk.android.core.matrix.MatrixEvenBus
import com.nunchuk.android.core.matrix.MatrixEvent
import com.nunchuk.android.core.matrix.MatrixEventListener
import com.nunchuk.android.core.matrix.SessionHolder
import com.nunchuk.android.core.nfc.BaseNfcActivity
import com.nunchuk.android.core.util.AppEvenBus
import com.nunchuk.android.core.util.AppEvent
import com.nunchuk.android.core.util.AppEventListener
import com.nunchuk.android.core.util.orFalse
import com.nunchuk.android.main.components.tabs.wallet.WalletsViewModel
import com.nunchuk.android.main.databinding.ActivityMainBinding
import com.nunchuk.android.main.di.MainAppEvent
import com.nunchuk.android.messages.components.list.RoomMessage
import com.nunchuk.android.messages.components.list.RoomsState
import com.nunchuk.android.messages.components.list.RoomsViewModel
import com.nunchuk.android.messages.components.list.shouldShow
import com.nunchuk.android.notifications.PushNotificationHelper
import com.nunchuk.android.widget.NCInfoDialog
import com.nunchuk.android.widget.NCToastMessage
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import javax.inject.Inject

@AndroidEntryPoint
class MainActivity : BaseNfcActivity<ActivityMainBinding>() {

    @Inject
    lateinit var pushNotificationHelper: PushNotificationHelper

    @Inject
    lateinit var sessionHolder: SessionHolder

    private lateinit var navController: NavController

    private val viewModel: MainActivityViewModel by viewModels()

    private val roomViewModel: RoomsViewModel by viewModels()

    private val contactsViewModel: ContactsViewModel by viewModels()

    private val syncRoomViewModel: SyncRoomViewModel by viewModels()

    private val walletViewModel: WalletsViewModel by viewModels()

    private val syncInfoViewModel: SyncInfoViewModel by viewModels()

    private val messages
        get() = intent.getStringArrayListExtra(EXTRAS_MESSAGE_LIST).orEmpty()

    private val bottomNavViewPosition: Int
        get() = intent.getIntExtra(EXTRAS_BOTTOM_NAV_VIEW_POSITION, 0)

    private val messageBadge: BadgeDrawable
        get() = binding.navView.getOrCreateBadge(R.id.navigation_messages)

    private val matrixEventListener: MatrixEventListener = {
        if (it is MatrixEvent.SignedInEvent) {
            roomViewModel.handleMatrixSignedIn()
            contactsViewModel.handleMatrixSignedIn()
        }
    }

    private val appEventListener: AppEventListener = {
        if (it is AppEvent.AppResumedEvent) {
            viewModel.checkAppUpdateRecommend(true)
        } else if (it is AppEvent.OpenServiceTabEvent) {
            lifecycleScope.launch {
                delay(300L)
                setBottomNavViewPosition(R.id.navigation_services)
            }
        }
    }
    private var dialogUpdateRecommend: Dialog? = null

    override fun initializeBinding() = ActivityMainBinding.inflate(layoutInflater).also {
        enableEdgeToEdge()
        WindowCompat.getInsetsController(window, window.decorView)
            .isAppearanceLightStatusBars = false
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        setupData()
        setupNavigationView()
        setBottomNavViewPosition(bottomNavViewPosition)
        subscribeEvents()
        MatrixEvenBus.instance.subscribe(matrixEventListener)
        AppEvenBus.instance.subscribe(appEventListener)
        viewModel.checkAppUpdateRecommend(false)
        syncInfoViewModel.init()
        walletViewModel.joinGroupWallet()

        messages.forEachIndexed { index, message ->
            NCToastMessage(this).showMessage(
                message = message, dismissTime = (index + 1) * DISMISS_TIME
            )
        }
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
        val (loginHalfToken, deviceId) = viewModel.getSetupData()
        if (loginHalfToken.isNotEmpty() && deviceId.isNotEmpty()) {
            syncRoomViewModel.setupMatrixIfNeeded(loginHalfToken, deviceId)
        }
        if (sessionHolder.getSafeActiveSession() != null) {
            pushNotificationHelper.retrieveFcmToken(
                onTokenRetrieved = ::onTokenRetrieved,
            )
            syncRoomViewModel.findSyncRoom()
        }
        viewModel.scheduleGetBTCConvertPrice()
    }

    private fun subscribeEvents() {
        viewModel.event.observe(this, ::handleEvent)
        syncRoomViewModel.event.observe(this, ::handleEvent)
        roomViewModel.state.observe(this, ::handleRoomState)
    }

    private fun handleRoomState(state: RoomsState) {
        val roomCount =
            state.rooms.filterIsInstance<RoomMessage.MatrixRoom>().map { it.data }.sumOf { if (it.shouldShow() && it.hasUnreadMessages) it.notificationCount else 0 }
        val groupWalletCount = state.rooms.filterIsInstance<RoomMessage.GroupWalletRoom>().sumOf { it.data.unreadCount }
        val count = roomCount + groupWalletCount
        messageBadge.apply {
            isVisible = count > 0
            number = count
            maxCharacterCount = 3
        }
    }

    private fun handleEvent(event: MainAppEvent) {
        when (event) {
            is MainAppEvent.UpdateAppRecommendEvent -> handleAppUpdateEvent(event.data)
            MainAppEvent.ConsumeSyncEventCompleted -> walletViewModel.retrieveData()
            MainAppEvent.ShowOnBoardEvent -> {
                finish()
                navigator.openOnBoardingScreen(this)
            }

            else -> {}
        }
    }

    private fun handleEvent(event: SyncRoomEvent) {
        when (event) {
            is SyncRoomEvent.FindSyncRoomSuccessEvent -> viewModel.syncData(event.syncRoomId)
            is SyncRoomEvent.CreateSyncRoomSucceedEvent -> viewModel.syncData(event.syncRoomId)
            is SyncRoomEvent.LoginMatrixSucceedEvent -> {
                pushNotificationHelper.retrieveFcmToken(
                    onTokenRetrieved = ::onTokenRetrieved,
                )
                syncRoomViewModel.findSyncRoom()
            }

            is SyncRoomEvent.FindSyncRoomFailedEvent -> if (event.syncRoomSize == 0) {
                syncRoomViewModel.createRoomWithTagSync()
            }
        }
    }

    private fun setupNavigationView() {
        val navView: BottomNavigationView = binding.navView
        val navHostFragment =
            supportFragmentManager.findFragmentById(R.id.nav_host_fragment_activity_main) as NavHostFragment
        navController = navHostFragment.navController
        NunchukBottomNavigationUtil.setupWithNavController(navView, navController)
    }

    private fun setBottomNavViewPosition(@IdRes id: Int) {
        if (id != 0) {
            binding.navView.selectedItemId = id
        }
    }

    private fun showUpdateRecommendedDialog(
        title: String,
        message: String,
        btnCTAText: String,
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
        private const val DISMISS_TIME = 2000L

        const val EXTRAS_BOTTOM_NAV_VIEW_POSITION = "EXTRAS_BOTTOM_NAV_VIEW_POSITION"
        const val EXTRAS_MESSAGE_LIST = "EXTRAS_MESSAGE_LIST"

        fun start(
            activityContext: Context,
            position: Int? = null,
            messages: ArrayList<String>? = null,
            isClearTask: Boolean = false,
        ) {
            activityContext.startActivity(
                createIntent(
                    activityContext = activityContext,
                    bottomNavViewPosition = position,
                    messages = messages,
                    isClearTask = isClearTask
                )
            )
        }

        // TODO replace with args
        fun createIntent(
            activityContext: Context,
            @IdRes bottomNavViewPosition: Int? = null,
            messages: ArrayList<String>? = null,
            isClearTask: Boolean = false,
        ): Intent {
            return Intent(activityContext, MainActivity::class.java).apply {
                if (isClearTask) {
                    addFlags(Intent.FLAG_ACTIVITY_CLEAR_TASK or Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP)
                } else {
                    addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP)
                }
                putExtra(EXTRAS_BOTTOM_NAV_VIEW_POSITION, bottomNavViewPosition)
                putExtra(EXTRAS_MESSAGE_LIST, messages)
            }
        }
    }

}